# Complete example: AgentCore Runtime

Runs the full primary deployment (ECR + IAM + AgentCore Runtime) as a single,
standalone example. This mirrors the root configuration and is useful for trying
the stack in a scratch account or as a reference for wiring the modules together.

## What it creates

- An ECR repository (scan-on-push, AES256, lifecycle policy)
- A least-privilege IAM execution role for the AgentCore Runtime
- The Amazon Bedrock AgentCore Runtime running the containerized agent

## Prerequisites

- Terraform >= 1.6, AWS CLI v2
- Amazon Bedrock model access enabled in the target region
- An ARM64 agent image pushed to the ECR repository this example creates
  (build and push with `../../../scripts/build-and-push.sh`)

## Run

```bash
terraform init
terraform apply -var "app_name=idp-agent" -var "environment=dev" -var "image_tag=latest"
```

To validate only (no AWS calls, no apply):

```bash
terraform init -backend=false
terraform validate
```

## Inputs

| Name | Description | Default |
|------|-------------|---------|
| region | AWS region. | `us-east-1` |
| environment | dev, staging, or prod. | `dev` |
| app_name | Base name for ECR, IAM, runtime. | `idp-agent` |
| image_tag | Image tag to deploy. | `latest` |
| memory_id | Optional AgentCore Memory id. | `""` |
| tags | Extra tags. | `{}` |

## Outputs

| Name | Description |
|------|-------------|
| ecr_repository_url | Push the agent image here. |
| runtime_name | Runtime name to invoke. |
| runtime_arn | Runtime ARN. |
| runtime_role_arn | Execution role ARN. |

## Cleanup

```bash
terraform destroy -var "app_name=idp-agent" -var "environment=dev"
```
