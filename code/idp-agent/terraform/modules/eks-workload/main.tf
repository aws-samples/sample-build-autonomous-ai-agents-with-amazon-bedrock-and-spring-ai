terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
  }
}

locals {
  labels = {
    "app.kubernetes.io/name"       = var.app_name
    "app.kubernetes.io/managed-by" = "terraform"
  }

  selector_labels = {
    "app.kubernetes.io/name" = var.app_name
  }

  # Pod Security Standards: enforce the "restricted" profile on the namespace so
  # any pod that does not meet the hardened baseline is rejected at admission.
  pss_labels = {
    "pod-security.kubernetes.io/enforce" = "restricted"
    "pod-security.kubernetes.io/audit"   = "restricted"
    "pod-security.kubernetes.io/warn"    = "restricted"
  }

  # Default to an internal NLB so the service is not exposed to the public
  # internet in plaintext. Set load_balancer_internal = false only when you have
  # deliberately fronted it with TLS (for example an ALB Ingress + ACM cert).
  lb_annotations = var.service_type == "LoadBalancer" ? {
    "service.beta.kubernetes.io/aws-load-balancer-type"            = "external"
    "service.beta.kubernetes.io/aws-load-balancer-nlb-target-type" = "ip"
    "service.beta.kubernetes.io/aws-load-balancer-scheme"          = var.load_balancer_internal ? "internal" : "internet-facing"
  } : {}
}

resource "kubernetes_namespace_v1" "this" {
  metadata {
    name   = var.namespace
    labels = merge(local.labels, local.pss_labels)
  }
}

# Service account bound to the least-privilege IRSA role.
resource "kubernetes_service_account_v1" "this" {
  metadata {
    name      = var.service_account
    namespace = kubernetes_namespace_v1.this.metadata[0].name
    labels    = local.labels
    annotations = {
      "eks.amazonaws.com/role-arn" = var.irsa_role_arn
    }
  }

  # Only mount the projected SA token where a pod opts in; the deployment below
  # requests it explicitly for IRSA.
  automount_service_account_token = false
}

resource "kubernetes_deployment_v1" "this" {
  # The image reference is supplied by the caller (repo:tag or repo@digest). The
  # source ECR repo is IMMUTABLE-tagged, and production callers pass a pinned tag
  # or digest. checkov cannot evaluate the runtime value, so these two image
  # checks are documented, accepted exceptions rather than code defects.
  #checkov:skip=CKV_K8S_14:Image tag is caller-supplied; ECR is immutable-tagged and prod pins a version
  #checkov:skip=CKV_K8S_43:Digest pinning is optional; caller may pass repo@sha256 and ECR tags are immutable
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace_v1.this.metadata[0].name
    labels    = local.labels
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = local.selector_labels
    }

    template {
      metadata {
        labels = local.labels
      }

      spec {
        service_account_name            = kubernetes_service_account_v1.this.metadata[0].name
        automount_service_account_token = true

        # Pod-level hardening (Pod Security Standards "restricted").
        security_context {
          run_as_non_root = true
          run_as_user     = 1000
          run_as_group    = 1000
          fs_group        = 1000

          seccomp_profile {
            type = "RuntimeDefault"
          }
        }

        # Writable scratch space so the container root filesystem can stay
        # read-only. Spring Boot / the JVM write to /tmp (heap dumps, work dirs).
        volume {
          name = "tmp"
          empty_dir {}
        }

        container {
          name  = var.app_name
          image = var.image

          port {
            container_port = var.container_port
          }

          env {
            name  = "AWS_REGION"
            value = var.region
          }
          env {
            name  = "AGENTCORE_MEMORY_ID"
            value = var.memory_id
          }
          # Without a memory id the AgentCore Memory starters fail at start-up;
          # the "stateless" profile turns off only their auto-configuration.
          dynamic "env" {
            for_each = var.memory_id == "" ? [1] : []
            content {
              name  = "SPRING_PROFILES_ACTIVE"
              value = "stateless"
            }
          }
          env {
            name  = "LOG_FORMAT"
            value = "ecs"
          }
          env {
            name  = "CORS_ALLOWED_ORIGINS"
            value = var.cors_allowed_origins
          }

          volume_mount {
            name       = "tmp"
            mount_path = "/tmp"
          }

          resources {
            requests = {
              cpu    = var.resources.cpu_request
              memory = var.resources.memory_request
            }
            limits = {
              cpu    = var.resources.cpu_limit
              memory = var.resources.memory_limit
            }
          }

          # Spring Boot Actuator probes
          # (management.endpoint.health.probes.enabled=true).
          startup_probe {
            http_get {
              path = "/actuator/health"
              port = var.container_port
            }
            failure_threshold = 30
            period_seconds    = 5
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = var.container_port
            }
            period_seconds = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = var.container_port
            }
            period_seconds = 10
          }

          # Container-level hardening (Pod Security Standards "restricted").
          security_context {
            run_as_non_root            = true
            run_as_user                = 1000
            allow_privilege_escalation = false
            read_only_root_filesystem  = true
            privileged                 = false

            capabilities {
              drop = ["ALL"]
            }

            seccomp_profile {
              type = "RuntimeDefault"
            }
          }
        }
      }
    }
  }
}

# Default-deny ingress: only traffic to the app port from within the namespace
# and from the VPC (the in-VPC NLB / load balancer) is allowed. Everything else,
# including cross-namespace and public pod traffic, is denied.
resource "kubernetes_network_policy_v1" "this" {
  metadata {
    name      = "${var.app_name}-allow-lb"
    namespace = kubernetes_namespace_v1.this.metadata[0].name
    labels    = local.labels
  }

  spec {
    pod_selector {
      match_labels = local.selector_labels
    }

    policy_types = ["Ingress"]

    ingress {
      # Same-namespace callers.
      from {
        namespace_selector {
          match_labels = {
            "kubernetes.io/metadata.name" = kubernetes_namespace_v1.this.metadata[0].name
          }
        }
      }

      # In-VPC load balancer (NLB target ENIs live in the VPC CIDR).
      from {
        ip_block {
          cidr = var.allowed_ingress_cidr
        }
      }

      ports {
        port     = var.container_port
        protocol = "TCP"
      }
    }
  }
}

resource "kubernetes_service_v1" "this" {
  metadata {
    name        = "${var.app_name}-svc"
    namespace   = kubernetes_namespace_v1.this.metadata[0].name
    labels      = local.labels
    annotations = local.lb_annotations
  }

  spec {
    selector = local.selector_labels
    type     = var.service_type

    port {
      port        = var.service_port
      target_port = var.container_port
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler_v2" "this" {
  metadata {
    name      = "${var.app_name}-hpa"
    namespace = kubernetes_namespace_v1.this.metadata[0].name
    labels    = local.labels
  }

  spec {
    min_replicas = var.min_replicas
    max_replicas = var.max_replicas

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment_v1.this.metadata[0].name
    }

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = var.cpu_target_percent
        }
      }
    }
  }
}

# Keep at least one healthy replica during voluntary disruptions (node drains,
# rolling upgrades).
resource "kubernetes_pod_disruption_budget_v1" "this" {
  count = var.enable_pod_disruption_budget ? 1 : 0

  metadata {
    name      = "${var.app_name}-pdb"
    namespace = kubernetes_namespace_v1.this.metadata[0].name
    labels    = local.labels
  }

  spec {
    min_available = var.pdb_min_available
    selector {
      match_labels = local.selector_labels
    }
  }
}
