variable "region" {
  description = "AWS region to deploy into (for example us-east-1)."
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
  description = "Application name used as the base for the ECR repository, IAM role, and AgentCore Runtime names."
  type        = string
  default     = "idp-agent"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,48}[a-z0-9]$", var.app_name))
    error_message = "app_name must be lowercase alphanumeric with hyphens, 3-50 characters, starting with a letter."
  }
}

variable "image_tag" {
  description = "Container image tag pushed to ECR and deployed to the AgentCore Runtime."
  type        = string
  default     = "latest"

  validation {
    condition     = length(trimspace(var.image_tag)) > 0
    error_message = "image_tag must not be empty."
  }
}

variable "memory_id" {
  description = "Optional AgentCore Memory id. Leave empty to run without session memory (IAM access is then scoped to the account memory namespace)."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Extra tags merged onto all resources in addition to the provider default_tags."
  type        = map(string)
  default     = {}
}

variable "verify_image" {
  description = "Check at plan time that image_tag exists in ECR before creating or updating the runtime. scripts/cleanup.sh sets this to false so destroy never depends on an image."
  type        = bool
  default     = true
}
