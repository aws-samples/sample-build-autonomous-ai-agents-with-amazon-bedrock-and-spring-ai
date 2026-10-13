# Module: iam

Creates the least-privilege IAM execution role assumed by the Amazon Bedrock
AgentCore Runtime service, plus the inline execution policy that grants only the
permissions the runtime needs.

## Purpose

- Provision the execution role trusted by the `bedrock-agentcore.amazonaws.com`
  service principal, scoped with `aws:SourceAccount` and `aws:SourceArn`
  conditions so only this account's AgentCore resources can assume it.
- Attach a least-privilege inline policy that allows:
  - Pulling the agent image from the specific ECR repository.
  - Writing logs to the AgentCore log groups (scoped to this account/region).
  - Sending X-Ray traces and `bedrock-agentcore` CloudWatch metrics. These APIs
    require `Resource = "*"`, so they are constrained with `aws:RequestedRegion`
    and (for metrics) a `cloudwatch:namespace` condition.
  - Invoking Bedrock foundation models and inference profiles. Inference
    profiles are scoped to this region and account; foundation-model ARNs use a
    region wildcard so global (cross-region) inference profiles keep working.
  - Accessing only the provided AgentCore Memory resource.

See `../../SECURITY.md` for the full statement-by-statement breakdown and the
list of intentionally accepted wildcards.

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

| Name                | Description                                                                     | Type        | Default | Required |
| ------------------- | ------------------------------------------------------------------------------- | ----------- | ------- | :------: |
| app_name            | Application name used to name the role. Must be alphanumeric (AgentCore naming). | string      | n/a     |   yes    |
| region              | AWS region used when scoping resource ARNs in the policies.                     | string      | n/a     |   yes    |
| account_id          | AWS account id used in trust and resource ARN conditions.                       | string      | n/a     |   yes    |
| ecr_repository_arn  | ARN of the ECR repository the runtime pulls its image from.                     | string      | n/a     |   yes    |
| memory_resource_arn | ARN (or scoped wildcard) of the AgentCore Memory resource the runtime may use.  | string      | n/a     |   yes    |
| tags                | Tags applied to the role (in addition to provider default_tags).                | map(string) | {}      |    no    |

## Outputs

| Name      | Description             |
| --------- | ----------------------- |
| role_arn  | Execution role ARN.     |
| role_name | Execution role name.    |

## Usage

```hcl
module "iam" {
  source              = "../../modules/iam"
  app_name            = "autonomousagentDev"
  region              = "us-east-1"
  account_id          = "123456789012"
  ecr_repository_arn  = module.ecr.repository_arn
  memory_resource_arn = "arn:aws:bedrock-agentcore:us-east-1:123456789012:memory/*"

  tags = {
    Application = "autonomous-agent"
    Environment = "dev"
  }
}
```
