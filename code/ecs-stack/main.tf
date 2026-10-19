data "aws_caller_identity" "current" {}

locals {
  prefix         = "${var.name}-${var.environment}"
  account_id     = data.aws_caller_identity.current.account_id
  container_port = 8080

  tags = merge({
    Project     = "spring-ai-agentcore-samples"
    Stack       = "ecs-web"
    Environment = var.environment
    ManagedBy   = "terraform"
  }, var.tags)

  # Web apps behind the load balancer (one listener each).
  web_apps = {
    landing   = { short = "landing", repo = "landing-page" }
    travel_ui = { short = "travel-ui", repo = "autonomous-agent-ui" }
    idp_ui    = { short = "idp-ui", repo = "idp-agent-ui" }
  }

  listener_ports = {
    landing   = 443
    travel_ui = 8081
    idp_ui    = 8082
  }

  # Browser-facing host and HTTPS URLs (see alb.tf and certificate.tf).
  host = var.domain_name != "" ? var.domain_name : aws_lb.this.dns_name
  urls = {
    landing   = "https://${local.host}"
    travel_ui = "https://${local.host}:8081"
    idp_ui    = "https://${local.host}:8082"
  }

  # ECS services are created only once the UI images exist (second apply).
  deploy_services = var.ui_image_tag != ""
}

# --- ECR repositories for the three web images --------------------------------

resource "aws_ecr_repository" "web" {
  for_each = local.web_apps

  name                 = "${local.prefix}-${each.value.repo}"
  image_tag_mutability = "IMMUTABLE"
  # Demo stack: allow `terraform destroy` to remove repositories with images.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.this.arn
  }
}

resource "aws_ecr_lifecycle_policy" "web" {
  for_each = aws_ecr_repository.web

  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the 10 most recent images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

# --- Logs -----------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "service" {
  for_each = toset(["landing", "travel-ui", "idp-ui", "autonomous-agent", "idp-agent"])

  name              = "/ecs/${local.prefix}/${each.key}"
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.this.arn
}

# --- Cluster and Service Connect namespace ---------------------------------------

resource "aws_service_discovery_http_namespace" "this" {
  name        = "${local.prefix}.local"
  description = "Service Connect namespace for the agent web stack"
}

resource "aws_ecs_cluster" "this" {
  name = local.prefix

  service_connect_defaults {
    namespace = aws_service_discovery_http_namespace.this.arn
  }

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}
