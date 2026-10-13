# agentcore-runtime

## Purpose

Creates an Amazon Bedrock AgentCore Runtime that runs the containerized IDP agent.
The runtime pulls an ARM64 image from ECR, assumes the provided execution role,
and receives the container environment variables the agent needs at startup.

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.6 |
| aws | ~> 6.0 |

## Providers

| Name | Version |
|------|---------|
| aws | ~> 6.0 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| runtime_name | AgentCore Runtime name (must be alphanumeric). | `string` | n/a | yes |
| role_arn | Execution role ARN the runtime assumes. | `string` | n/a | yes |
| image_uri | Full ECR image URI (repo:tag) for the ARM64 agent image. | `string` | n/a | yes |
| network_mode | Runtime network mode. Currently PUBLIC. | `string` | `"PUBLIC"` | no |
| environment_variables | Environment variables passed to the container. | `map(string)` | `{}` | no |
| tags | Tags applied to the runtime. | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| runtime_name | AgentCore Runtime name used when invoking the agent. |
| runtime_arn | AgentCore Runtime ARN. |

## Usage

```hcl
module "runtime" {
  source       = "./modules/agentcore-runtime"
  runtime_name = "idpAgentDev"
  role_arn     = module.iam.role_arn
  image_uri    = "123456789012.dkr.ecr.us-east-1.amazonaws.com/idp-agent-dev:latest"

  environment_variables = {
    AWS_REGION             = "us-east-1"
    AGENTCORE_MEMORY_ID    = ""
    LOG_FORMAT             = "ecs"
    SPRING_PROFILES_ACTIVE = "dev"
  }

  tags = { Application = "idp-agent" }
}
```
