variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"

  validation {
    condition     = length(trimspace(var.region)) > 0
    error_message = "region must not be empty."
  }
}

variable "environment" {
  description = "Deployment environment. One of dev, staging, or prod."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}

variable "app_name" {
  description = "Application / cluster base name."
  type        = string
  default     = "idp-agent"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,48}[a-z0-9]$", var.app_name))
    error_message = "app_name must be lowercase alphanumeric with hyphens, 3-50 characters, starting with a letter."
  }
}

variable "kubernetes_version" {
  description = "EKS control plane version. Pin to a currently supported release."
  type        = string
  default     = "1.33"
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDR allowlist for public access to the EKS API server endpoint. Defaults to a documentation placeholder (TEST-NET-3, RFC 5737) that reaches nothing; set this to your operator/CI egress ranges. Never use 0.0.0.0/0."
  type        = list(string)
  default     = ["203.0.113.0/24"]

  validation {
    condition     = !contains(var.cluster_endpoint_public_access_cidrs, "0.0.0.0/0")
    error_message = "cluster_endpoint_public_access_cidrs must not contain 0.0.0.0/0. Restrict the public API endpoint to specific ranges."
  }
}

variable "load_balancer_internal" {
  description = "Provision an internal (VPC-only) NLB for the app Service. Defaults to true so nothing is exposed to the public internet in plaintext. Front it with TLS (ALB Ingress + ACM) before setting to false."
  type        = bool
  default     = true
}

variable "node_instance_types" {
  description = "Graviton (arm64) instance types for the managed node group."
  type        = list(string)
  default     = ["t4g.large"]

  validation {
    condition     = length(var.node_instance_types) > 0
    error_message = "node_instance_types must contain at least one instance type."
  }
}

variable "node_desired_size" {
  description = "Desired worker node count."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum worker node count."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Maximum worker node count."
  type        = number
  default     = 4

  validation {
    condition     = var.node_max_size >= var.node_min_size
    error_message = "node_max_size must be greater than or equal to node_min_size."
  }
}

variable "ecr_repository_name" {
  description = "Existing ECR repository holding the arm64 image (created by scripts/build-and-push.sh)."
  type        = string
  default     = "idp-agent"
}

variable "image_tag" {
  description = "Image tag to deploy."
  type        = string
  default     = "latest"

  validation {
    condition     = length(trimspace(var.image_tag)) > 0
    error_message = "image_tag must not be empty."
  }
}

variable "memory_id" {
  description = "Optional AgentCore Memory id (empty to run without session memory)."
  type        = string
  default     = ""
}

variable "replicas" {
  description = "Number of application pods."
  type        = number
  default     = 2
}

variable "cors_allowed_origins" {
  description = "Comma-separated CORS origins for the React UI. Restrict in production."
  type        = string
  default     = "*"
}

variable "tags" {
  description = "Extra tags merged onto all AWS resources."
  type        = map(string)
  default     = {}
}
