terraform {
  # Shared module: declare a minimum AWS provider version rather than a
  # pessimistic upper bound so it composes with either the 5.x line (used by the
  # EKS example, required by terraform-aws-modules/eks v20) and later majors.
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.40"
    }
  }
}

locals {
  # Strip the scheme so the OIDC URL can be used in IAM condition keys.
  oidc_host = replace(var.oidc_provider_url, "https://", "")
}

# Trust policy: only the specific Kubernetes ServiceAccount may assume this role
# through the cluster's OIDC provider (IAM Roles for Service Accounts).
data "aws_iam_policy_document" "assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:sub"
      values   = ["system:serviceaccount:${var.namespace}:${var.service_account}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name               = var.role_name
  assume_role_policy = data.aws_iam_policy_document.assume.json
  tags               = var.tags
}

# Least-privilege: only Bedrock model invocation and the AgentCore Memory actions
# the app uses, scoped to the configured memory resource.
data "aws_iam_policy_document" "permissions" {
  statement {
    sid    = "BedrockModelInvocation"
    effect = "Allow"
    actions = [
      "bedrock:InvokeModel",
      "bedrock:InvokeModelWithResponseStream"
    ]
    # Scoped to the deployment region and account (foundation models are
    # AWS-owned so carry no account segment; inference profiles are account-owned).
    resources = [
      "arn:aws:bedrock:${var.region}::foundation-model/*",
      "arn:aws:bedrock:${var.region}:${var.account_id}:inference-profile/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.region]
    }
  }

  statement {
    sid    = "AgentCoreMemoryAccess"
    effect = "Allow"
    actions = [
      "bedrock-agentcore:CreateEvent",
      "bedrock-agentcore:ListEvents",
      "bedrock-agentcore:DeleteEvent",
      "bedrock-agentcore:RetrieveMemoryRecords"
    ]
    resources = [var.memory_resource_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.region]
    }
  }
}

resource "aws_iam_role_policy" "this" {
  name   = "${var.role_name}-policy"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.permissions.json
}
