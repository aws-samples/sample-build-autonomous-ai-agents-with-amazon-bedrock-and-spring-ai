# Module: agentcore-runtime

Deploys the Amazon Bedrock AgentCore Runtime that runs the Spring Boot agent as
an ARM64 container image pulled from ECR.

## Purpose

- Provision an `aws_bedrockagentcore_agent_runtime` resource from a container
  image URI.
- Wire the execution role, network mode, and runtime environment variables the
  Spring Boot agent reads at startup.

## Requirements

| Name      | Version   |
| --------- | --------- |
| terraform | >= 1.6    |
| aws       | ~> 6.0    |

## Providers

| Name | Version |
| ---- | ------- |
| aws  | ~> 6.0  |

## Inputs

| Name                  | Description                                                              | Type        | Default    | Required |
| --------------------- | ------------------------------------------------------------------------ | ----------- | ---------- | :------: |
| runtime_name          | AgentCore Runtime name. Must be alphanumeric.                            | string      | n/a        |   yes    |
| role_arn              | Execution role ARN the runtime assumes.                                  | string      | n/a        |   yes    |
| image_uri             | Full ECR image URI (repo:tag) for the ARM64 agent image.                 | string      | n/a        |   yes    |
| network_mode          | Runtime network mode. PUBLIC exposes it over the managed network.        | string      | "PUBLIC"   |    no    |
| environment_variables | Environment variables passed to the container at runtime.                | map(string) | {}         |    no    |
| tags                  | Tags applied to the runtime (in addition to provider default_tags).      | map(string) | {}         |    no    |

## Outputs

| Name         | Description               |
| ------------ | ------------------------- |
| runtime_name | AgentCore Runtime name.   |
| runtime_arn  | AgentCore Runtime ARN.    |

## Usage

```hcl
module "runtime" {
  source       = "../../modules/agentcore-runtime"
  runtime_name = "autonomousagentDev"
  role_arn     = module.iam.role_arn
  image_uri    = "123456789012.dkr.ecr.us-east-1.amazonaws.com/autonomous-agent-dev:latest"

  environment_variables = {
    AWS_REGION             = "us-east-1"
    BEDROCK_MODEL          = "global.anthropic.claude-sonnet-4-5-20250929-v1:0"
    LOG_FORMAT             = "ecs"
    SPRING_PROFILES_ACTIVE = "dev"
  }

  tags = {
    Application = "autonomous-agent"
    Environment = "dev"
  }
}
```
