variable "app_name" {
  description = "Application / workload name (used for the deployment, service, and HPA)."
  type        = string

  validation {
    condition     = length(trimspace(var.app_name)) > 0
    error_message = "app_name must not be empty."
  }
}

variable "region" {
  description = "AWS region passed to the container as AWS_REGION."
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace to deploy into."
  type        = string
  default     = "idp"
}

variable "service_account" {
  description = "Kubernetes service account name bound to the IRSA role."
  type        = string
  default     = "idp-agent"
}

variable "irsa_role_arn" {
  description = "IRSA role ARN annotated on the service account."
  type        = string
}

variable "image" {
  description = "Full container image reference (repo:tag)."
  type        = string
}

variable "replicas" {
  description = "Initial replica count for the deployment."
  type        = number
  default     = 2

  validation {
    condition     = var.replicas >= 1
    error_message = "replicas must be at least 1."
  }
}

variable "min_replicas" {
  description = "HPA minimum replicas."
  type        = number
  default     = 2
}

variable "max_replicas" {
  description = "HPA maximum replicas."
  type        = number
  default     = 10

  validation {
    condition     = var.max_replicas >= var.min_replicas
    error_message = "max_replicas must be greater than or equal to min_replicas."
  }
}

variable "cpu_target_percent" {
  description = "HPA target average CPU utilization percentage."
  type        = number
  default     = 70

  validation {
    condition     = var.cpu_target_percent > 0 && var.cpu_target_percent <= 100
    error_message = "cpu_target_percent must be between 1 and 100."
  }
}

variable "memory_id" {
  description = "Optional AgentCore Memory id passed to the container."
  type        = string
  default     = ""
}

variable "cors_allowed_origins" {
  description = "Comma-separated CORS origins the API accepts. Restrict in production."
  type        = string
  default     = "*"
}

variable "container_port" {
  description = "Container port the application listens on."
  type        = number
  default     = 8080
}

variable "service_type" {
  description = "Kubernetes Service type. LoadBalancer provisions an AWS load balancer; ClusterIP keeps it in-cluster."
  type        = string
  default     = "LoadBalancer"

  validation {
    condition     = contains(["LoadBalancer", "ClusterIP", "NodePort"], var.service_type)
    error_message = "service_type must be one of: LoadBalancer, ClusterIP, NodePort."
  }
}

variable "load_balancer_internal" {
  description = "When service_type is LoadBalancer, provision an internal (VPC-only) NLB. Defaults to true so the service is not exposed to the public internet in plaintext. Set to false only when fronted by TLS (for example an ALB Ingress with an ACM certificate)."
  type        = bool
  default     = true
}

variable "allowed_ingress_cidr" {
  description = "CIDR allowed to reach the app port by the NetworkPolicy, in addition to same-namespace traffic. Defaults to the example VPC CIDR so the in-VPC load balancer can reach pods. Narrow this to your VPC/subnet range."
  type        = string
  default     = "10.0.0.0/16"
}

variable "enable_pod_disruption_budget" {
  description = "Create a PodDisruptionBudget to keep replicas available during voluntary disruptions."
  type        = bool
  default     = true
}

variable "pdb_min_available" {
  description = "Minimum available pods for the PodDisruptionBudget."
  type        = number
  default     = 1
}

variable "service_port" {
  description = "Port the Kubernetes Service exposes."
  type        = number
  default     = 80
}

variable "resources" {
  description = "Container resource requests and limits."
  type = object({
    cpu_request    = string
    memory_request = string
    cpu_limit      = string
    memory_limit   = string
  })
  default = {
    cpu_request    = "500m"
    memory_request = "768Mi"
    cpu_limit      = "1"
    memory_limit   = "1536Mi"
  }
}

variable "tags" {
  description = "Tags recorded on the module (Kubernetes resources use labels; kept for parity with AWS modules)."
  type        = map(string)
  default     = {}
}
