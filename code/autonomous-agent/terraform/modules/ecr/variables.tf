variable "repository_name" {
  description = "Name of the ECR repository."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9._/-]*$", var.repository_name))
    error_message = "repository_name must start with a lowercase letter or number and contain only lowercase letters, numbers, and the characters . _ / -."
  }
}

variable "image_tag_mutability" {
  description = "Whether image tags can be overwritten. IMMUTABLE by default to prevent tag reuse and ensure image provenance."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability must be either MUTABLE or IMMUTABLE."
  }
}

variable "scan_on_push" {
  description = "Scan images for vulnerabilities automatically on push. Enabled by default."
  type        = bool
  default     = true
}

variable "kms_key_arn" {
  description = "Optional KMS key ARN for repository encryption at rest. When empty, AWS-managed AES256 encryption is used; when set, encryption_type is KMS with this key."
  type        = string
  default     = ""

  validation {
    condition     = var.kms_key_arn == "" || can(regex("^arn:aws[a-z-]*:kms:[a-z0-9-]+:[0-9]{12}:key/.+$", var.kms_key_arn))
    error_message = "kms_key_arn must be empty or a valid KMS key ARN (arn:aws:kms:<region>:<account>:key/<id>)."
  }
}

variable "force_delete" {
  description = "Allow deleting the repository even if it still contains images. Set to false in production to prevent accidental image loss."
  type        = bool
  default     = true
}

variable "account_id" {
  description = "AWS account id used to deny untrusted cross-account principals in the optional repository policy. Leave empty to skip the cross-account deny."
  type        = string
  default     = ""

  validation {
    condition     = var.account_id == "" || can(regex("^[0-9]{12}$", var.account_id))
    error_message = "account_id must be empty or a 12 digit AWS account id."
  }
}

variable "keep_last_images" {
  description = "Number of tagged images to retain before the lifecycle policy expires older ones."
  type        = number
  default     = 10

  validation {
    condition     = var.keep_last_images >= 1
    error_message = "keep_last_images must be at least 1."
  }
}

variable "attach_repository_policy" {
  description = "When true, attach a repository policy that restricts pull access to allowed_principal_arns. Defaults off to preserve default account access."
  type        = bool
  default     = false
}

variable "allowed_principal_arns" {
  description = "IAM principal ARNs allowed to pull images when attach_repository_policy is true."
  type        = list(string)
  default     = []

  validation {
    condition     = alltrue([for arn in var.allowed_principal_arns : can(regex("^arn:aws[a-z-]*:iam::[0-9]{12}:(root|(user|role|group)/.+)$", arn))])
    error_message = "allowed_principal_arns must all be valid IAM principal ARNs (root, user, role, or group)."
  }
}

variable "tags" {
  description = "Tags applied to the repository (in addition to provider default_tags)."
  type        = map(string)
  default     = {}
}
