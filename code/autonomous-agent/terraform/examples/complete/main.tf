terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = merge({
      Application = var.app_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }, var.tags)
  }
}

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  ecr_repo   = lower("${var.app_name}-${var.environment}")
  image_uri  = "${local.account_id}.dkr.ecr.${var.region}.amazonaws.com/${local.ecr_repo}:${var.image_tag}"

  memory_resource_arn = var.memory_id == "" ? "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/*" : "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/${var.memory_id}"

  runtime_name = replace("${var.app_name}${title(var.environment)}", "-", "")

  tags = merge({
    Application = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }, var.tags)
}

module "ecr" {
  source          = "../../modules/ecr"
  repository_name = local.ecr_repo

  # Demonstrate the hardened options: immutable tags, scan-on-push, and a scoped
  # repository policy that denies untrusted (cross-account) and non-TLS callers.
  # The policy is scoped to this account root to keep pull access inside the
  # account (avoids a module dependency cycle with the IAM role).
  account_id               = local.account_id
  image_tag_mutability     = "IMMUTABLE"
  scan_on_push             = true
  keep_last_images         = 15
  attach_repository_policy = true
  allowed_principal_arns   = ["arn:aws:iam::${local.account_id}:root"]

  # Set to a KMS key ARN to encrypt images with customer-managed KMS instead of
  # the default AWS-managed AES256.
  kms_key_arn = var.ecr_kms_key_arn

  tags = local.tags
}

module "iam" {
  source              = "../../modules/iam"
  app_name            = local.runtime_name
  region              = var.region
  account_id          = local.account_id
  ecr_repository_arn  = module.ecr.repository_arn
  memory_resource_arn = local.memory_resource_arn
  tags                = local.tags
}

module "runtime" {
  source       = "../../modules/agentcore-runtime"
  runtime_name = local.runtime_name
  role_arn     = module.iam.role_arn
  image_uri    = local.image_uri

  environment_variables = {
    AGENTCORE_MEMORY_ID = var.memory_id
    AWS_REGION          = var.region
    BEDROCK_MODEL       = var.bedrock_model
    LOG_FORMAT          = "ecs"
    # Add the "stateless" profile when no memory id is set (see ../../main.tf).
    SPRING_PROFILES_ACTIVE = var.memory_id == "" ? "${var.environment},stateless" : var.environment
  }

  tags = local.tags
}
