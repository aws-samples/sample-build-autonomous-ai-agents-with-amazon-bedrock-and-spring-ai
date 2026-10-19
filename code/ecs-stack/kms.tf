# Customer-managed KMS key for the stack's ECR repositories and CloudWatch log
# groups (service logs and VPC flow logs). Rotation is on.

# In a key policy, Resource "*" means this key only. The account-root statement
# is the AWS default that lets IAM policies govern access to the key.
data "aws_iam_policy_document" "kms" {
  #checkov:skip=CKV_AWS_109:Key policy; "*" refers to this key only (AWS default root statement)
  #checkov:skip=CKV_AWS_111:Key policy; "*" refers to this key only (AWS default root statement)
  #checkov:skip=CKV_AWS_356:Key policy; "*" refers to this key only (AWS default root statement)
  statement {
    sid       = "AccountAdministration"
    actions   = ["kms:*"]
    resources = ["*"]
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.account_id}:root"]
    }
  }

  # CloudWatch Logs encrypts log groups in this account and Region only.
  statement {
    sid = "CloudWatchLogs"
    actions = [
      "kms:Encrypt*",
      "kms:Decrypt*",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:Describe*"
    ]
    resources = ["*"]
    principals {
      type        = "Service"
      identifiers = ["logs.${var.region}.amazonaws.com"]
    }
    condition {
      test     = "ArnLike"
      variable = "kms:EncryptionContext:aws:logs:arn"
      values   = ["arn:aws:logs:${var.region}:${local.account_id}:log-group:*"]
    }
  }
}

resource "aws_kms_key" "this" {
  description             = "${local.prefix}: ECR images and CloudWatch logs"
  enable_key_rotation     = true
  deletion_window_in_days = 7
  policy                  = data.aws_iam_policy_document.kms.json
}

resource "aws_kms_alias" "this" {
  name          = "alias/${local.prefix}"
  target_key_id = aws_kms_key.this.key_id
}
