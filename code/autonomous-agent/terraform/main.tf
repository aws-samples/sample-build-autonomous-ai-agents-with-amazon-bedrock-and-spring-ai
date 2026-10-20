data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  ecr_repo   = lower("${var.app_name}-${var.environment}")
  image_uri  = "${local.account_id}.dkr.ecr.${var.region}.amazonaws.com/${local.ecr_repo}:${var.image_tag}"

  # Scope memory access to the specific resource when provided, otherwise allow the
  # account's memory namespace (useful before a memory id exists).
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
  source          = "./modules/ecr"
  repository_name = local.ecr_repo
  account_id      = local.account_id

  # Immutable tags and scan-on-push are the secure module defaults. Set
  # kms_key_arn to a KMS key to switch encryption at rest from AES256 to KMS.
  kms_key_arn = var.ecr_kms_key_arn

  tags = local.tags
}

module "iam" {
  source              = "./modules/iam"
  app_name            = local.runtime_name
  region              = var.region
  account_id          = local.account_id
  ecr_repository_arn  = module.ecr.repository_arn
  memory_resource_arn = local.memory_resource_arn
  tags                = local.tags
}

# The runtime can only start from an image that is already in ECR. Reading it
# here makes `terraform plan` fail fast ("image not found") when image_tag was
# never pushed, instead of creating a runtime that cannot start. Use
# scripts/deploy.sh, which creates the repository, pushes a tagged image, and
# then applies with -var image_tag=<that tag>.
data "aws_ecr_image" "agent" {
  count = var.verify_image ? 1 : 0

  repository_name = module.ecr.repository_name
  image_tag       = var.image_tag
}

module "runtime" {
  source       = "./modules/agentcore-runtime"
  runtime_name = local.runtime_name
  role_arn     = module.iam.role_arn
  image_uri    = local.image_uri

  depends_on = [data.aws_ecr_image.agent]

  environment_variables = {
    AGENTCORE_MEMORY_ID = var.memory_id
    AWS_REGION          = var.region
    BEDROCK_MODEL       = var.bedrock_model
    LOG_FORMAT          = "ecs"
    # Without a memory id the AgentCore Memory starters fail at start-up, so add
    # the "stateless" profile, which turns off only the memory auto-configuration.
    SPRING_PROFILES_ACTIVE = var.memory_id == "" ? "${var.environment},stateless" : var.environment
  }

  tags = local.tags
}
