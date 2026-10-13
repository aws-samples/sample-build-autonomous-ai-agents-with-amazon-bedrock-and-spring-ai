resource "aws_ecr_repository" "this" {
  name = var.repository_name
  # Immutable tags by default so a pushed tag (for example a release version)
  # can never be silently overwritten with different content.
  image_tag_mutability = var.image_tag_mutability
  force_delete         = var.force_delete

  image_scanning_configuration {
    scan_on_push = true
  }

  # KMS encryption when a key is supplied, otherwise ECR-managed AES256.
  encryption_configuration {
    encryption_type = var.kms_key_arn == "" ? "AES256" : "KMS"
    kms_key         = var.kms_key_arn == "" ? null : var.kms_key_arn
  }

  tags = var.tags
}

# Optional resource-based repository policy (for example cross-account pull).
resource "aws_ecr_repository_policy" "this" {
  count      = var.repository_policy_json == "" ? 0 : 1
  repository = aws_ecr_repository.this.name
  policy     = var.repository_policy_json
}

# Keep the repository tidy: expire all but the most recent images.
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
