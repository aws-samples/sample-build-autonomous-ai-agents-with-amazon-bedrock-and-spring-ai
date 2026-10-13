terraform {
  # Require a modern Terraform CLI. Features used (nested validation,
  # optional object attributes) are stable from 1.6 onward.
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source = "hashicorp/aws"
      # Pessimistic constraint: allow 6.x minor and patch updates, but do not
      # auto-upgrade across a major version (7.x) that may break. The 6.x line
      # is required for the aws_bedrockagentcore_agent_runtime resource.
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  # default_tags are inherited by every taggable resource created with this
  # provider, so individual resources do not need to repeat the common tags.
  # Resource-level tags with the same key override the value defined here.
  default_tags {
    tags = merge({
      Application = var.app_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }, var.tags)
  }
}
