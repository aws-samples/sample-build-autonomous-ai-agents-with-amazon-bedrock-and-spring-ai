# Complete AgentCore Runtime example.
#
# Composes the ecr, iam, and agentcore-runtime modules exactly as the primary
# root configuration does, so you can run the full stack from a single, standalone
# example. Provide an image_tag that already exists in the created ECR repository
# (build and push it with ../../../scripts/build-and-push.sh before apply).

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  ecr_repo   = lower("${var.app_name}-${var.environment}")
  image_uri  = "${local.account_id}.dkr.ecr.${var.region}.amazonaws.com/${local.ecr_repo}:${var.image_tag}"

  # Scope memory access to the specific resource when provided, otherwise allow
  # the account's memory namespace (useful before a memory id exists).
  memory_resource_arn = var.memory_id == "" ? "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/*" : "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/${var.memory_id}"

  # AgentCore Runtime names must be alphanumeric.
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
  tags            = local.tags
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
    AGENTCORE_MEMORY_ID    = var.memory_id
    AWS_REGION             = var.region
    LOG_FORMAT             = "ecs"
    SPRING_PROFILES_ACTIVE = var.environment
  }

  tags = local.tags
}
