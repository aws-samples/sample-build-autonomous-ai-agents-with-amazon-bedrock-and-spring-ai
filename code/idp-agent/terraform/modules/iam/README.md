# iam

## Purpose

Creates the least-privilege IAM execution role assumed by the AgentCore Runtime
service principal. The inline policy grants only what the agent needs: pulling the
image from ECR, writing runtime logs, X-Ray tracing, publishing CloudWatch metrics
in the `bedrock-agentcore` namespace, invoking Bedrock models, and using the
configured AgentCore Memory resource.

The trust policy restricts assumption to the `bedrock-agentcore.amazonaws.com`
service principal with confused-deputy guards (`aws:SourceAccount` and
`aws:SourceArn`). Every permission statement is scoped to the deployment account
and region: resource ARNs are region- and account-qualified, and each statement
carries an `aws:RequestedRegion` condition. Actions that do not support
resource-level permissions (`ecr:GetAuthorizationToken`, `xray:*`,
`cloudwatch:PutMetricData`) use `"*"` constrained by that condition (and, for
metrics, the `bedrock-agentcore` namespace). See `../../SECURITY.md`.

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
| app_name | Application name used to name the role. | `string` | n/a | yes |
| region | AWS region. | `string` | n/a | yes |
| account_id | AWS account id. | `string` | n/a | yes |
| ecr_repository_arn | ARN of the ECR repository the runtime pulls from. | `string` | n/a | yes |
| memory_resource_arn | ARN (or wildcard) of the AgentCore Memory resource the runtime may use. | `string` | n/a | yes |
| tags | Tags applied to the role. | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| role_arn | Execution role ARN. |
| role_name | Execution role name. |

## Usage

```hcl
module "iam" {
  source              = "./modules/iam"
  app_name            = "idpAgentDev"
  region              = "us-east-1"
  account_id          = "123456789012"
  ecr_repository_arn  = module.ecr.repository_arn
  memory_resource_arn = "arn:aws:bedrock-agentcore:us-east-1:123456789012:memory/*"
  tags                = { Application = "idp-agent" }
}
```
