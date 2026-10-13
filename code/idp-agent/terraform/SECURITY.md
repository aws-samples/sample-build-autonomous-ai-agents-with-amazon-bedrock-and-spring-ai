# Security posture

This document summarizes the security controls applied to the IDP Agent Terraform
for both deployment paths, how to run the static analysis tooling, and the list of
intentionally accepted exceptions.

The configuration covers two independent stacks:

- Primary: Amazon Bedrock AgentCore Runtime (root, `examples/complete`) on AWS
  provider `~> 6.0`.
- Alternative: Amazon EKS (`examples/eks`) on AWS provider `~> 5.95` plus the
  `kubernetes`, `helm`, and `tls` providers.

## Shared / AgentCore Runtime path

### IAM least privilege (`modules/iam`)

- The execution role trust policy allows only the
  `bedrock-agentcore.amazonaws.com` service principal, guarded against the confused
  deputy problem with both `aws:SourceAccount` (this account) and `aws:SourceArn`
  (`arn:aws:bedrock-agentcore:<region>:<account>:*`).
- Every permission statement is scoped to the deployment account and region.
- ECR image pull is scoped to the single repository ARN.
- Bedrock invocation is scoped to `arn:aws:bedrock:<region>::foundation-model/*`
  and `arn:aws:bedrock:<region>:<account>:inference-profile/*`, gated by an
  `aws:RequestedRegion` condition.
- AgentCore Memory access is limited to four actions (`CreateEvent`, `ListEvents`,
  `DeleteEvent`, `RetrieveMemoryRecords`) on the specific memory resource ARN (or
  the account memory namespace when no memory id is set), gated by
  `aws:RequestedRegion`.
- Actions that do not support resource-level permissions
  (`ecr:GetAuthorizationToken`, the `xray:*` telemetry actions,
  `cloudwatch:PutMetricData`) use `"*"` constrained by an `aws:RequestedRegion`
  condition, and metrics are additionally constrained to the `bedrock-agentcore`
  namespace. See the accepted exceptions table.

### ECR (`modules/ecr`)

- `image_tag_mutability` defaults to `IMMUTABLE`, so a pushed tag can never be
  silently overwritten.
- `scan_on_push` is enabled.
- Encryption at rest defaults to ECR-managed AES256, and switches to a
  customer-managed KMS key when `kms_key_arn` is set.
- A lifecycle policy expires untagged images after 3 days and keeps only the most
  recent tagged images.
- An optional resource-based repository policy can be supplied via
  `repository_policy_json` (for example, cross-account pull).

### Remote state (`backend.tf`)

State is local by default so the project runs out of the box. `backend.tf`
documents a hardened S3 + DynamoDB backend:

- SSE-KMS with a customer-managed key and an S3 Bucket Key.
- Bucket versioning to recover from accidental or malicious state changes.
- Full public access block (all four settings).
- A bucket policy that denies all non-TLS (`aws:SecureTransport=false`) access.
- A DynamoDB lock table with a `LockID` partition key.
- A distinct state `key` per environment so dev and prod never share state.

## EKS path (`examples/eks`, `modules/eks-irsa`, `modules/eks-workload`)

### Control plane

- Control-plane logging enabled for `api`, `audit`, `authenticator`,
  `controllerManager`, and `scheduler`.
- Kubernetes secrets are envelope-encrypted with a KMS key (key rotation enabled).
- The public API endpoint is restricted to an explicit CIDR allowlist
  (`cluster_endpoint_public_access_cidrs`, default is an RFC 5737 placeholder that
  reaches nothing) and validated to reject `0.0.0.0/0`; private endpoint access is
  enabled.
- Kubernetes version is pinned to a currently supported release (`1.33`).

### Node groups

- IMDSv2 is enforced through launch-template metadata options
  (`http_tokens = required`, `http_put_response_hop_limit = 1`), which also blocks
  pods from reaching the node instance metadata service.
- Node root EBS volumes are encrypted with a customer-managed KMS key (`gp3`).
- Nodes are Graviton/arm64 (`AL2023_ARM_64_STANDARD`), placed in private subnets
  only, with no auto-assigned public IP.

### Networking

- VPC with two private and two public subnets across two AZs, and a NAT gateway
  for private egress.
- VPC flow logs delivered to a KMS-encrypted CloudWatch log group with 365-day
  retention.
- Security groups are the tightened defaults from the EKS and VPC modules; no
  `0.0.0.0/0` ingress is opened. The application load balancer is internal by
  default.

### IRSA least privilege (`modules/eks-irsa`)

- The trust policy binds the role to exactly one service account
  (`system:serviceaccount:<namespace>:<service_account>`) via the cluster OIDC
  provider `sub` and `aud` conditions. No node-wide credentials.
- Permissions grant only `bedrock:InvokeModel*` (scoped to region and account) and
  the four AgentCore Memory actions on the specific memory resource ARN, each gated
  by an `aws:RequestedRegion` condition.

### Kubernetes workload (`modules/eks-workload`)

- Namespace carries Pod Security Standards `restricted` labels (enforce, audit,
  warn).
- Pod and container security contexts: `runAsNonRoot = true`, `runAsUser = 1000`,
  `readOnlyRootFilesystem = true` (with an `emptyDir` mounted at `/tmp` for the
  JVM), `allowPrivilegeEscalation = false`, `privileged = false`, all capabilities
  dropped, and `seccompProfile = RuntimeDefault`.
- Resource requests and limits are set on the container.
- Startup, liveness, and readiness probes target the Spring Boot Actuator
  endpoints.
- A NetworkPolicy applies default-deny ingress, allowing the app port only from the
  same namespace and the in-VPC load balancer CIDR.
- A PodDisruptionBudget keeps at least one replica available during voluntary
  disruptions.
- The service account does not auto-mount its token except where the deployment
  opts in for IRSA.

### Service exposure

- The Service defaults to an internal NLB (`load_balancer_internal = true`), so
  nothing is exposed to the public internet in plaintext.
- For public HTTPS, front the app with the AWS Load Balancer Controller and an
  ALB Ingress terminating TLS with an ACM certificate, then set
  `load_balancer_internal = false`. See `examples/eks/README.md`.

## Running the tooling

From the `terraform/` directory:

```bash
terraform fmt -recursive

terraform init -backend=false && terraform validate            # root
(cd examples/complete && terraform init -backend=false && terraform validate)
(cd examples/eks      && terraform init -backend=false && terraform validate)

tflint --recursive        # lint HCL and provider best practices
checkov -d . --compact    # security and compliance scanning
```

### tflint

`tflint` was not installed on the machine used to harden this configuration, so it
was not run here. Install it (`https://github.com/terraform-linters/tflint`) and
run `tflint --recursive` from `terraform/` to lint the HCL.

### checkov

`checkov -d . --compact` was run from `terraform/`. Latest result:

```
Passed checks: 101, Failed checks: 0, Skipped checks: 10
```

The 10 skipped checks are the intentionally accepted exceptions below, each
inline-suppressed with `#checkov:skip=CKV_...:reason`. Registry modules
(`terraform-aws-modules/vpc`, `terraform-aws-modules/eks`) are not downloaded
during the scan; run with `--download-external-modules true` to also scan their
internals.

## Accepted exceptions

| Check | Location | Justification |
|-------|----------|---------------|
| CKV_TF_1 | `examples/eks/main.tf` `module "vpc"`, `module "eks"` | Signed Terraform Registry modules are pinned by semantic version (`~> 5.13`, `~> 20.24`), not by git commit hash. The commit-hash policy does not apply to registry sources. |
| CKV_AWS_109, CKV_AWS_111, CKV_AWS_356 | `examples/eks/main.tf` `aws_iam_policy_document.ebs_kms` | KMS key policy, not an identity policy. In a key policy the `Resource` element must be `"*"`, which resolves to this key only. Access is scoped by principal (account root and the Auto Scaling service-linked role) and a `kms:GrantIsForAWSResource` condition. |
| CKV_AWS_109, CKV_AWS_111, CKV_AWS_356 | `examples/eks/main.tf` `aws_iam_policy_document.logs_kms` | KMS key policy. `Resource "*"` means this key only; access is scoped by principal (account root and the CloudWatch Logs service) and the `kms:EncryptionContext:aws:logs:arn` condition. |
| CKV_K8S_14 | `modules/eks-workload/main.tf` deployment | The image reference is caller-supplied. The source ECR repository is IMMUTABLE-tagged and production callers pass a pinned version tag. checkov cannot evaluate the runtime value. |
| CKV_K8S_43 | `modules/eks-workload/main.tf` deployment | Digest pinning is optional; callers may pass `repo@sha256:...`. ECR immutable tags provide equivalent content-integrity guarantees for the default tag-based workflow. |

### Residual IAM wildcards (by design, not flagged)

Some IAM actions do not support resource-level permissions and therefore use
`Resource: "*"`, constrained by conditions:

- `ecr:GetAuthorizationToken` - constrained by `aws:RequestedRegion`.
- `xray:PutTraceSegments`, `xray:PutTelemetryRecords`, `xray:GetSamplingRules`,
  `xray:GetSamplingTargets` - constrained by `aws:RequestedRegion`.
- `cloudwatch:PutMetricData` - constrained by `aws:RequestedRegion` and the
  `bedrock-agentcore` metric namespace.

These are the minimum wildcards AWS requires for the actions and are scoped as
tightly as the actions allow.
