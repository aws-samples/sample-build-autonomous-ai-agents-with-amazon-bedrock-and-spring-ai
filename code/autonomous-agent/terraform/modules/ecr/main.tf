resource "aws_ecr_repository" "this" {
  name                 = var.repository_name
  image_tag_mutability = var.image_tag_mutability
  force_delete         = var.force_delete

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  # Encrypt images at rest. When a KMS key ARN is supplied, use customer-managed
  # (or specified) KMS encryption; otherwise fall back to AWS-managed AES256.
  encryption_configuration {
    encryption_type = var.kms_key_arn == "" ? "AES256" : "KMS"
    kms_key         = var.kms_key_arn == "" ? null : var.kms_key_arn
  }

  tags = var.tags
}

# Keep the repository tidy: expire untagged images and cap retained tagged images.
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after 3 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 3
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Keep only the last ${var.keep_last_images} tagged images"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["v", "latest", "20"]
          countType     = "imageCountMoreThan"
          countNumber   = var.keep_last_images
        }
        action = { type = "expire" }
      }
    ]
  })
}

# Optional repository policy that limits access to an explicit set of principals
# and actively denies untrusted callers. Off by default so the repository keeps
# standard account access. Enable by setting attach_repository_policy = true and
# providing allowed_principal_arns (for example the runtime execution role ARN).
#
# When enabled the policy:
#   - Allows the scoped pull actions only to allowed_principal_arns.
#   - Denies every ECR action that is not sent over TLS (aws:SecureTransport).
#   - Denies every ECR action from principals outside this account when
#     account_id is provided, blocking untrusted cross-account access.
resource "aws_ecr_repository_policy" "this" {
  count      = var.attach_repository_policy && length(var.allowed_principal_arns) > 0 ? 1 : 0
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Sid       = "AllowScopedPull"
          Effect    = "Allow"
          Principal = { AWS = var.allowed_principal_arns }
          Action = [
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer",
            "ecr:BatchCheckLayerAvailability"
          ]
        },
        {
          Sid       = "DenyInsecureTransport"
          Effect    = "Deny"
          Principal = { AWS = "*" }
          Action    = "ecr:*"
          Condition = {
            Bool = { "aws:SecureTransport" = "false" }
          }
        }
      ],
      var.account_id == "" ? [] : [
        {
          Sid       = "DenyUntrustedAccounts"
          Effect    = "Deny"
          Principal = { AWS = "*" }
          Action    = "ecr:*"
          Condition = {
            StringNotEquals = { "aws:PrincipalAccount" = var.account_id }
          }
        }
      ]
    )
  })
}
