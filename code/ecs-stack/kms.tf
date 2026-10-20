# Customer-managed KMS key for the stack's ECR repositories and CloudWatch log
# groups (service logs and VPC flow logs). Rotation is on.
#
# The key policy is inline on the key. In a key policy, Resource "*" means this
# key only. The account-root statement is the AWS default that lets IAM
# policies in this account govern access to the key; CloudWatch Logs may use
# the key only for log groups in this account and Region.

resource "aws_kms_key" "this" {
  description             = "${local.prefix}: ECR images and CloudWatch logs"
  enable_key_rotation     = true
  deletion_window_in_days = 7

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AccountAdministration"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:root" }
        Action    = "kms:*"
        Resource  = "*"
      },
      {
        Sid       = "CloudWatchLogs"
        Effect    = "Allow"
        Principal = { Service = "logs.${var.region}.amazonaws.com" }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncryptFrom",
          "kms:ReEncryptTo",
          "kms:GenerateDataKey",
          "kms:GenerateDataKeyWithoutPlaintext",
          "kms:DescribeKey"
        ]
        Resource = "*"
        Condition = {
          ArnLike = {
            "kms:EncryptionContext:aws:logs:arn" = "arn:aws:logs:${var.region}:${local.account_id}:log-group:*"
          }
        }
      }
    ]
  })
}

resource "aws_kms_alias" "this" {
  name          = "alias/${local.prefix}"
  target_key_id = aws_kms_key.this.key_id
}
