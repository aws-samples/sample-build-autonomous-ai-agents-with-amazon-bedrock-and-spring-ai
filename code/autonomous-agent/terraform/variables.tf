variable "region" {
  description = "AWS region to deploy all resources into (for example us-east-1)."
  type        = string
  default     = "us-east-1"

  validation {
    condition     = length(trimspace(var.region)) > 0
    error_message = "region must not be empty."
  }
}

variable "environment" {
  description = "Deployment environment. Drives naming, tags, and the Spring profile."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}

variable "app_name" {
  description = "Application name used as the base for the ECR repo, IAM role, and runtime names."
  type        = string
  default     = "autonomous-agent"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.app_name))
    error_message = "app_name must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "image_tag" {
  description = "Container image tag pushed to ECR and deployed to the runtime."
  type        = string
  default     = "latest"

  validation {
    condition     = length(trimspace(var.image_tag)) > 0
    error_message = "image_tag must not be empty."
  }
}

variable "memory_id" {
  description = "Optional AgentCore Memory id. Leave empty to run without session memory."
  type        = string
  default     = ""
}

variable "bedrock_model" {
  description = "Bedrock model id (inference profile) the agent invokes."
  type        = string
  default     = "global.anthropic.claude-sonnet-4-5-20250929-v1:0"

  validation {
    condition     = length(trimspace(var.bedrock_model)) > 0
    error_message = "bedrock_model must not be empty."
  }
}

variable "ecr_kms_key_arn" {
  description = "Optional KMS key ARN for ECR encryption at rest. Empty uses AWS-managed AES256; set a key ARN to use customer-managed KMS encryption."
  type        = string
  default     = ""

  validation {
    condition     = var.ecr_kms_key_arn == "" || can(regex("^arn:aws[a-z-]*:kms:[a-z0-9-]+:[0-9]{12}:key/.+$", var.ecr_kms_key_arn))
    error_message = "ecr_kms_key_arn must be empty or a valid KMS key ARN."
  }
}

variable "tags" {
  description = "Extra tags merged onto all resources in addition to the default tags."
  type        = map(string)
  default     = {}
}

variable "verify_image" {
  description = "Check at plan time that image_tag exists in ECR before creating or updating the runtime. scripts/cleanup.sh sets this to false so destroy never depends on an image."
  type        = bool
  default     = true
}
