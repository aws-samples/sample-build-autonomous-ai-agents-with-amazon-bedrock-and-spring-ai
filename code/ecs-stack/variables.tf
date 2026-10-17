variable "region" {
  description = "AWS Region to deploy to. Must have Amazon Bedrock and AgentCore available."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name used in resource names and tags."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "name" {
  description = "Base name for all resources."
  type        = string
  default     = "agentcore-web"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,20}$", var.name))
    error_message = "name must be 2-21 lowercase letters, digits, or hyphens, starting with a letter."
  }
}

variable "allowed_cidrs" {
  description = <<-EOT
    CIDR blocks allowed to reach the load balancer. The UIs have no real
    authentication (the IDP sign-in is a demo), so keep this to your own IP,
    for example ["203.0.113.10/32"]. There is deliberately no default.
  EOT
  type        = list(string)

  validation {
    condition     = length(var.allowed_cidrs) > 0 && alltrue([for c in var.allowed_cidrs : can(cidrnetmask(c))])
    error_message = "allowed_cidrs must be a non-empty list of valid IPv4 CIDR blocks."
  }

  validation {
    condition     = !contains(var.allowed_cidrs, "0.0.0.0/0")
    error_message = "0.0.0.0/0 is not allowed: the UIs are unauthenticated. Restrict access to known IPs."
  }
}

variable "autonomous_agent_image" {
  description = "Full image URI (repo:tag) of the autonomous-agent backend, for example the image pushed for AgentCore Runtime."
  type        = string
}

variable "idp_agent_image" {
  description = "Full image URI (repo:tag) of the idp-agent backend."
  type        = string
}

variable "ui_image_tag" {
  description = "Tag of the landing page and UI images in this stack's ECR repositories. Leave empty on the first apply (creates repos and the load balancer only)."
  type        = string
  default     = ""
}

variable "bedrock_model" {
  description = "Amazon Bedrock model or inference profile ID used by both backends."
  type        = string
  default     = "global.anthropic.claude-sonnet-4-5-20250929-v1:0"
}

variable "vpc_cidr" {
  description = "CIDR block for the stack's VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for all services."
  type        = number
  default     = 14
}

variable "tags" {
  description = "Extra tags applied to every resource."
  type        = map(string)
  default     = {}
}
