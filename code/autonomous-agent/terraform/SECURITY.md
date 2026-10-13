# Security Posture

This document summarizes the security controls applied to the autonomous-agent
Terraform and how to verify them locally. The configuration is designed to pass
a strict AWS security review with least-privilege IAM and no unresolved static
analysis findings.

## Least-privilege IAM (modules/iam)

The execution role is assumed only by the `bedrock-agentcore.amazonaws.com`
service principal and is guarded against the confused-deputy problem:

- Trust policy requires `aws:SourceAccount` equal to this account and
  `aws:SourceArn` matching `arn:aws:bedrock-agentcore:<region>:<account>:*`, so
  only this account's AgentCore resources can assume the role.

The inline execution policy grants only what the runtime needs, scoped as
tightly as each action allows:

| Statement              | Actions                                                                | Resource scope                                                             |
| ---------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| ECRImageAccess         | ecr:BatchGetImage, ecr:GetDownloadUrlForLayer                          | The specific ECR repository ARN only.                                      |
| ECRTokenAccess         | ecr:GetAuthorizationToken                                              | `*` (required by the API, see accepted wildcards below).                   |
| Logs                   | logs:CreateLogGroup/Stream, PutLogEvents, DescribeLogStreams          | AgentCore runtime log-group prefix in this account and region.             |
| LogsDescribe           | logs:DescribeLogGroups                                                 | This account and region, constrained by `aws:RequestedRegion`.             |
| XRay                   | xray:PutTraceSegments/PutTelemetryRecords/GetSampling*                 | `*` (required by the API), constrained by `aws:RequestedRegion`.           |
| CloudWatchMetrics      | cloudwatch:PutMetricData                                               | `*` (required), constrained to namespace `bedrock-agentcore` + region.     |
| BedrockModelInvocation | bedrock:InvokeModel, bedrock:InvokeModelWithResponseStream            | Inference profiles scoped to this region and account; foundation models by region wildcard (global inference profiles route cross-region). |
| AgentCoreMemoryAccess  | bedrock-agentcore:CreateEvent/ListEvents/DeleteEvent/RetrieveMemoryRecords | The specific AgentCore Memory resource ARN (or the account memory namespace before a memory id exists). |

## ECR (modules/ecr)

- `image_tag_mutability` defaults to `IMMUTABLE` to prevent tag reuse and
  guarantee image provenance.
- `scan_on_push` defaults to `true` so images are scanned for vulnerabilities on
  every push.
- Encryption at rest is always on: AWS-managed `AES256` by default, or
  customer-managed `KMS` when `kms_key_arn` is set.
- A lifecycle policy expires untagged images after 3 days and caps retained
  tagged images.
- An optional repository policy (`attach_repository_policy = true`) allows pull
  only to `allowed_principal_arns`, denies any non-TLS request
  (`aws:SecureTransport = false`), and denies untrusted cross-account principals
  when `account_id` is provided (`aws:PrincipalAccount` mismatch).

## Runtime, IMDS, and network notes (modules/agentcore-runtime)

- The AgentCore Runtime runs as an AWS-managed service; there is no customer EC2
  instance and therefore no IMDS endpoint to harden. The container obtains
  credentials from the scoped execution role rather than instance metadata.
- `network_mode` is `PUBLIC`, which exposes the runtime over the AgentCore
  managed network (the current supported mode for this resource). No inbound
  security groups or public IPs are created by this configuration.
- Least-privilege credentials flow only through the execution role above, so a
  compromised container cannot exceed the role's scoped permissions.

## Remote-state encryption (backend.tf)

State is local by default for out-of-the-box use. For team use, the S3 backend
must be hardened (documented in `backend.tf`): SSE-KMS encryption with bucket
keys, versioning, a full public access block, a TLS-only bucket policy, and a
DynamoDB lock table with encryption enabled. Bootstrap commands are provided as
commented guidance in `backend.tf`.

## Running security tooling locally

Format, validate, lint, and scan before committing. On Windows PowerShell keep
each command on a single line and chain with `;` rather than `&&`.

```bash
terraform fmt -recursive
terraform init -backend=false
terraform validate

# Provider-aware linting (install: https://github.com/terraform-linters/tflint)
tflint --recursive

# Static security and compliance scanning (install: pip install checkov)
checkov -d .
checkov -d . --compact   # condensed output
```

At the time of writing, `checkov -d .` reports 16 passed and 0 failed checks and
`terraform validate` succeeds for both the root module and `examples/complete`.
`tflint` was not installed in the authoring environment; run it locally for
provider-aware linting. No inline `#checkov:skip` suppressions are required
because there are no failing findings.

## Intentionally accepted wildcards

These `Resource = "*"` entries cannot be scoped further because the underlying
AWS APIs do not support resource-level permissions. Each is constrained by
condition keys where the API allows it.

| Permission                 | Why the wildcard is required                                                                 | Additional constraint                                  |
| -------------------------- | -------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| ecr:GetAuthorizationToken  | Returns an account-level auth token; the API does not accept a repository ARN.               | None available; action is read-only and low risk.      |
| xray:PutTraceSegments, PutTelemetryRecords, GetSamplingRules, GetSamplingTargets | X-Ray trace ingestion and sampling APIs do not support resource-level permissions. | `aws:RequestedRegion` = deployment region.             |
| cloudwatch:PutMetricData   | PutMetricData does not support resource-level permissions.                                   | `cloudwatch:namespace` = `bedrock-agentcore` and `aws:RequestedRegion` = deployment region. |
| bedrock:InvokeModel* (foundation-model ARNs) | Global (cross-region) inference profiles invoke foundation models in multiple regions; foundation-model ARNs carry no account id. | Inference-profile ARNs are scoped to this region and account. |
