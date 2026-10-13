# Example: complete

A runnable, end-to-end example that provisions the full autonomous-agent stack
(ECR repository, IAM execution role, and Bedrock AgentCore Runtime) by composing
the three modules directly. It mirrors the root module but demonstrates the
hardened options in one place.

## What it demonstrates

- Pinned Terraform (>= 1.6) and AWS provider (~> 6.0) versions.
- Provider `default_tags` so every resource inherits the common tags.
- Immutable ECR image tags, scan-on-push, and a lifecycle policy that retains 15
  tagged images.
- A scoped ECR repository policy that denies untrusted cross-account and non-TLS
  callers (limited to this account root).
- Optional customer-managed KMS encryption for ECR via `ecr_kms_key_arn`.
- A least-privilege IAM execution role wired to the specific ECR repo and the
  AgentCore Memory resource.
- A production-style environment (`environment = "prod"`, `image_tag = "v1.0.0"`).

## Requirements

| Name      | Version   |
| --------- | --------- |
| terraform | >= 1.6    |
| aws       | ~> 6.0    |

## Usage

```bash
cd examples/complete

# Quick local check without contacting a backend
terraform init -backend=false
terraform validate

# Deploy (requires valid AWS credentials)
terraform init
terraform plan
terraform apply
```

Override any input on the command line or with a `.tfvars` file, for example:

```bash
terraform apply -var="environment=dev" -var="image_tag=latest"
```

## Inputs

| Name          | Description                                              | Type        | Default                                             |
| ------------- | -------------------------------------------------------- | ----------- | --------------------------------------------------- |
| region        | AWS region to deploy into.                               | string      | "us-east-1"                                         |
| environment   | Deployment environment: dev, staging, or prod.           | string      | "prod"                                              |
| app_name      | Base name for the ECR repo, IAM role, and runtime.       | string      | "autonomous-agent"                                  |
| image_tag     | Container image tag pushed to ECR and deployed.          | string      | "v1.0.0"                                            |
| memory_id     | Optional AgentCore Memory id.                            | string      | ""                                                  |
| bedrock_model | Bedrock model id (inference profile).                    | string      | "global.anthropic.claude-sonnet-4-5-20250929-v1:0" |
| ecr_kms_key_arn | Optional KMS key ARN for ECR encryption. Empty uses AES256. | string  | ""                                                  |
| tags          | Extra tags merged onto all resources.                    | map(string) | { CostCenter, Owner }                               |

## Outputs

| Name               | Description                                |
| ------------------ | ------------------------------------------ |
| ecr_repository_url | ECR repository to push the agent image to. |
| runtime_name       | AgentCore Runtime name.                    |
| runtime_arn        | AgentCore Runtime ARN.                     |
| runtime_role_arn   | IAM execution role assumed by the runtime. |
