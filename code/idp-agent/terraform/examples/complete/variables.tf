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
  description = "Application name used as the base for the ECR repository, IAM role, and runtime."
  type        = string
  default     = "idp-agent"
}

variable "image_tag" {
  description = "Container image tag pushed to ECR."
  type        = string
  default     = "latest"

  validation {
    condition     = length(trimspace(var.image_tag)) > 0
    error_message = "image_tag must not be empty."
  }
}

variable "memory_id" {
  description = "Optional AgentCore Memory id. Empty to run without session memory."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Extra tags merged onto all resources."
  type        = map(string)
  default     = {}
}
