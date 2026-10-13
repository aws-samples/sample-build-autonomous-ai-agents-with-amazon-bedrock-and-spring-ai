variable "role_name" {
  description = "Name of the IRSA IAM role."
  type        = string

  validation {
    condition     = length(trimspace(var.role_name)) > 0
    error_message = "role_name must not be empty."
  }
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS cluster's IAM OIDC provider."
  type        = string
}

variable "oidc_provider_url" {
  description = "EKS OIDC provider issuer URL (with or without the https:// scheme)."
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace of the service account that assumes this role."
  type        = string
}

variable "service_account" {
  description = "Kubernetes service account name bound to this role."
  type        = string
}

variable "account_id" {
  description = "AWS account id."
  type        = string
}

variable "region" {
  description = "AWS region."
  type        = string
}

variable "memory_resource_arn" {
  description = "ARN (or wildcard) of the AgentCore Memory resource the app may use."
  type        = string
}

variable "tags" {
  description = "Tags applied to the IAM role."
  type        = map(string)
  default     = {}
}
