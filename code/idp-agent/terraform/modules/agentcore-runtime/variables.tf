variable "runtime_name" {
  description = "AgentCore Runtime name"
  type        = string
}

variable "role_arn" {
  description = "Execution role ARN the runtime assumes"
  type        = string
}

variable "image_uri" {
  description = "Full ECR image URI (repo:tag) for the ARM64 agent image"
  type        = string
}

variable "network_mode" {
  description = "Runtime network mode. AgentCore Runtime currently supports PUBLIC."
  type        = string
  default     = "PUBLIC"

  validation {
    condition     = contains(["PUBLIC"], var.network_mode)
    error_message = "network_mode must be PUBLIC."
  }
}

variable "environment_variables" {
  description = "Environment variables passed to the container"
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "Tags applied to the runtime"
  type        = map(string)
  default     = {}
}
