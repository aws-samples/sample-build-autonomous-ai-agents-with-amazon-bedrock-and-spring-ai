variable "repository_name" {
  description = "Name of the ECR repository"
  type        = string
}

variable "image_tag_mutability" {
  description = "Tag mutability setting for the repository. One of MUTABLE or IMMUTABLE. Defaults to IMMUTABLE so pushed tags cannot be overwritten."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability must be MUTABLE or IMMUTABLE."
  }
}

variable "kms_key_arn" {
  description = "Optional KMS key ARN for encryption at rest. When empty, ECR-managed AES256 is used; when set, the repository is encrypted with the supplied customer-managed KMS key."
  type        = string
  default     = ""
}

variable "repository_policy_json" {
  description = "Optional JSON resource-based policy document attached to the repository (for example to allow cross-account pulls). Empty means no repository policy."
  type        = string
  default     = ""
}

variable "force_delete" {
  description = "Allow deleting the repository even if it still contains images."
  type        = bool
  default     = true
}

variable "keep_last_images" {
  description = "Number of tagged images to retain via the lifecycle policy."
  type        = number
  default     = 10

  validation {
    condition     = var.keep_last_images > 0
    error_message = "keep_last_images must be greater than 0."
  }
}

variable "tags" {
  description = "Tags applied to the repository"
  type        = map(string)
  default     = {}
}
