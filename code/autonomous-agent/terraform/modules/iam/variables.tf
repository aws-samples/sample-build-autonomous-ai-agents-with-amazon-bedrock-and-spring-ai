variable "app_name" {
  description = "Application name used to name the execution role. Must be alphanumeric (AgentCore naming)."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9]+$", var.app_name))
    error_message = "app_name must be alphanumeric (no hyphens or spaces)."
  }
}

variable "region" {
  description = "AWS region used when scoping resource ARNs in the policies."
  type        = string

  validation {
    condition     = length(trimspace(var.region)) > 0
    error_message = "region must not be empty."
  }
}

variable "account_id" {
  description = "AWS account id used in trust and resource ARN conditions."
  type        = string

  validation {
    condition     = can(regex("^[0-9]{12}$", var.account_id))
    error_message = "account_id must be a 12 digit AWS account id."
  }
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository the runtime pulls its image from."
  type        = string

  validation {
    condition     = length(trimspace(var.ecr_repository_arn)) > 0
    error_message = "ecr_repository_arn must not be empty."
  }
}

variable "memory_resource_arn" {
  description = "ARN (or scoped wildcard) of the AgentCore Memory resource the runtime may use."
  type        = string

  validation {
    condition     = length(trimspace(var.memory_resource_arn)) > 0
    error_message = "memory_resource_arn must not be empty."
  }
}

variable "tags" {
  description = "Tags applied to the role (in addition to provider default_tags)."
  type        = map(string)
  default     = {}
}
