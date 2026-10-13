variable "app_name" {
  description = "Application name used to name the role"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "account_id" {
  description = "AWS account id"
  type        = string
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository the runtime pulls from"
  type        = string
}

variable "memory_resource_arn" {
  description = "ARN (or wildcard) of the AgentCore Memory resource the runtime may use"
  type        = string
}

variable "tags" {
  description = "Tags applied to the role"
  type        = map(string)
  default     = {}
}
