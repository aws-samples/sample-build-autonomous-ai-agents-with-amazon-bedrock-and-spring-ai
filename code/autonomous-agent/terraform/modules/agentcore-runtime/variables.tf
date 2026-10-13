variable "runtime_name" {
  description = "AgentCore Runtime name. Must be alphanumeric."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9]+$", var.runtime_name))
    error_message = "runtime_name must be alphanumeric (no hyphens or spaces)."
  }
}

variable "role_arn" {
  description = "Execution role ARN the runtime assumes."
  type        = string

  validation {
    condition     = can(regex("^arn:aws[a-z-]*:iam::[0-9]{12}:role/.+$", var.role_arn))
    error_message = "role_arn must be a valid IAM role ARN."
  }
}

variable "image_uri" {
  description = "Full ECR image URI (repo:tag) for the ARM64 agent image."
  type        = string

  validation {
    condition     = length(trimspace(var.image_uri)) > 0
    error_message = "image_uri must not be empty."
  }
}

variable "network_mode" {
  description = "Runtime network mode. PUBLIC exposes the runtime over the AgentCore managed network."
  type        = string
  default     = "PUBLIC"

  validation {
    condition     = contains(["PUBLIC"], var.network_mode)
    error_message = "network_mode must be PUBLIC."
  }
}

variable "environment_variables" {
  description = "Environment variables passed to the container at runtime."
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "Tags applied to the runtime (in addition to provider default_tags)."
  type        = map(string)
  default     = {}
}
