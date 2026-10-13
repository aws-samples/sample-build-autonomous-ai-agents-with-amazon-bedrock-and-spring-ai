# Autonomous Agent Terraform

Infrastructure as code for deploying the autonomous-agent Java Spring Boot
application to Amazon Bedrock AgentCore Runtime as an ARM64 container.

## Architecture

The root module composes three child modules:

```
                        +-------------------+
                        |   root module     |
                        |  (main.tf, etc.)  |
                        +---------+---------+
                                  |
          +-----------------------+-----------------------+
          |                       |                       |
   +------v------+        +-------v-------+       +--------v---------+
   |   ecr       |        |     iam       |       | agentcore-runtime|
   | repository  |        | execution role|       | agent runtime    |
   | + lifecycle |        | + policy      |       | (ARM64 container)|
   +-------------+        +---------------+       +------------------+
```

- `modules/ecr` provisions the private ECR repository (immutable tags,
  scan-on-push, AES256 or optional KMS encryption, lifecycle policy, and an
  optional deny-by-default repository policy) that stores the agent image.
- `modules/iam` provisions the least-privilege execution role the runtime
  assumes, scoped to the specific ECR repo, log groups, Bedrock models, and the
  AgentCore Memory resource.
- `modules/agentcore-runtime` provisions the Bedrock AgentCore Runtime from the
  container image and wires its environment variables.

The root module derives names and ARNs (for example the ECR repo name, the
image URI, and the memory resource ARN) and passes them between the modules.

## Folder layout

```
terraform/
  versions.tf              Terraform and provider version constraints, default_tags
  backend.tf               Remote state backend docs + hardening guidance (commented, opt-in)
  SECURITY.md              Security posture, accepted wildcards, tooling commands
  main.tf                  Root composition of the three modules
  variables.tf             Root input variables with validation
  outputs.tf               Root outputs (repo url, runtime name/arn, role arn)
  terraform.tfvars.example Documented sample values for every variable
  environments/
    dev.tfvars             Dev environment values
    prod.tfvars            Prod environment values
  modules/
    ecr/                   ECR repository module
    iam/                   IAM execution role module
    agentcore-runtime/     AgentCore Runtime module
  examples/
    complete/              Runnable end-to-end example
```

## Requirements

| Name      | Version   |
| --------- | --------- |
| terraform | >= 1.6    |
| aws       | ~> 6.0    |

## Running per environment

Initialize once, then plan and apply with the matching environment file from
`environments/`.

```bash
# Initialize providers (backend disabled for a quick local check)
terraform init -backend=false

# Dev
terraform plan  -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars

# Prod
terraform plan  -var-file=environments/prod.tfvars
terraform apply -var-file=environments/prod.tfvars
```

On Windows PowerShell, keep each command on a single line and chain with `;`
rather than `&&` if needed.

## Remote state (S3 + DynamoDB lock)

State is local by default. For team use, enable the S3 backend documented in
`backend.tf`. The bucket and DynamoDB lock table are created once per account
outside this configuration. Because their names are user specific, the backend
block stays commented; either uncomment it or pass values at init time:

```bash
terraform init \
  -backend-config="bucket=my-tf-state-bucket" \
  -backend-config="key=autonomous-agent/dev/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -backend-config="dynamodb_table=my-tf-locks" \
  -backend-config="encrypt=true"
```

Use a distinct `key` per environment so dev and prod never share state. See
`backend.tf` for the one-time bootstrap commands.

## Quality checks

Format, validate, and lint before committing:

```bash
terraform fmt -recursive
terraform init -backend=false
terraform validate
```

Recommended additional tooling:

- `tflint` for provider-aware linting of Terraform code (`tflint --recursive`).
- `checkov` for static security and compliance scanning (`checkov -d .` or
  `checkov -d . --compact`). The configuration currently reports 0 failed
  checks.
- `pre-commit` with hooks for `terraform fmt`, `terraform validate`, `tflint`,
  and `checkov` so the checks run automatically on every commit.

A minimal `.pre-commit-config.yaml` can wire the
`antonbabenko/pre-commit-terraform` hooks (`terraform_fmt`,
`terraform_validate`, `terraform_tflint`, `terraform_checkov`).

See `SECURITY.md` for the full security posture, the least-privilege IAM
breakdown, ECR hardening, remote-state requirements, and the table of
intentionally accepted wildcards.

## Inputs

| Name          | Description                                                    | Type        | Default                                             | Required |
| ------------- | -------------------------------------------------------------- | ----------- | --------------------------------------------------- | :------: |
| region        | AWS region to deploy into.                                     | string      | "us-east-1"                                         |    no    |
| environment   | Deployment environment: dev, staging, or prod.                 | string      | "dev"                                               |    no    |
| app_name      | Base name for the ECR repo, IAM role, and runtime.             | string      | "autonomous-agent"                                  |    no    |
| image_tag     | Container image tag pushed to ECR and deployed.                | string      | "latest"                                            |    no    |
| memory_id     | Optional AgentCore Memory id. Empty runs without memory.       | string      | ""                                                  |    no    |
| bedrock_model | Bedrock model id (inference profile) the agent invokes.        | string      | "global.anthropic.claude-sonnet-4-5-20250929-v1:0" |    no    |
| ecr_kms_key_arn | Optional KMS key ARN for ECR encryption. Empty uses AES256.  | string      | ""                                                  |    no    |
| tags          | Extra tags merged onto all resources.                          | map(string) | {}                                                  |    no    |

## Outputs

| Name               | Description                                       |
| ------------------ | ------------------------------------------------- |
| ecr_repository_url | ECR repository to push the agent image to.        |
| runtime_name       | AgentCore Runtime name used when invoking.         |
| runtime_arn        | AgentCore Runtime ARN.                             |
| runtime_role_arn   | IAM execution role assumed by the runtime.         |
