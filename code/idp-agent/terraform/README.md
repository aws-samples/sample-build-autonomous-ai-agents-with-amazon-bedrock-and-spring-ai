# IDP Agent - Terraform

Infrastructure as Code for the IDP (Intelligent Document Processing) agent. This is
the single source of truth for deploying the agent.

There are two deployment paths:

- Primary: Amazon Bedrock AgentCore Runtime. The root configuration composes the
  `ecr`, `iam`, and `agentcore-runtime` modules to run the containerized agent on
  the fully managed AgentCore Runtime. This is what you get by running Terraform in
  this directory.
- Alternative: Amazon EKS (standalone). A complete, runnable example under
  `examples/eks/` provisions a VPC, an EKS cluster with Graviton nodes, a
  least-privilege IRSA role, and the Kubernetes workload. Use this when you want to
  run the agent on Kubernetes instead of AgentCore Runtime.

## Architecture

```
Primary (root)                         Alternative (examples/eks)
--------------                         --------------------------
ECR  (image registry)                  VPC  (subnets, NAT, tags)
IAM  (runtime execution role)          EKS  (Graviton managed nodes, IRSA/OIDC)
AgentCore Runtime (runs the image)     eks-irsa     (least-privilege IAM role)
                                       eks-workload (namespace, SA, deploy, svc, HPA)
```

Both paths use the same ARM64 container image and the same least-privilege IAM
model (Bedrock model invocation plus the AgentCore Memory actions the app uses).

Both paths are hardened to pass a strict AWS security review. See
[`SECURITY.md`](./SECURITY.md) for the full posture (IAM least privilege, ECR,
remote-state encryption, and the complete EKS hardening list), how to run `tflint`
and `checkov -d .`, and the table of intentionally accepted exceptions.

## Folder layout

```
terraform/
├── README.md                  # this file
├── SECURITY.md                # security posture + accepted exceptions (both paths)
├── backend.tf                 # remote state (S3 + DynamoDB) instructions
├── main.tf                    # primary: ECR + IAM + AgentCore Runtime
├── variables.tf  outputs.tf  versions.tf
├── terraform.tfvars.example   # documents every root variable
├── environments/
│   ├── dev.tfvars
│   └── prod.tfvars
├── modules/
│   ├── agentcore-runtime/     # Bedrock AgentCore Runtime
│   ├── ecr/                   # image repository + lifecycle policy
│   ├── iam/                   # runtime execution role (least privilege)
│   ├── eks-irsa/              # IRSA IAM role for the EKS path
│   └── eks-workload/          # Kubernetes Deployment/Service/HPA for the EKS path
└── examples/
    ├── complete/              # full AgentCore Runtime stack (standalone)
    └── eks/                   # full EKS stack (VPC + EKS + IRSA + workload)
```

Every module has its own `README.md` with Purpose, Requirements, Providers, Inputs,
Outputs, and a usage snippet.

## Requirements

| Name | Version | Used by |
|------|---------|---------|
| terraform | >= 1.6 | all |
| aws | ~> 6.0 | primary (root, examples/complete) |
| aws | ~> 5.95 | examples/eks (terraform-aws-modules/eks v20 targets the 5.x line) |
| kubernetes | ~> 2.30 | examples/eks |

The primary AgentCore Runtime path uses AWS provider 6.x (the
`aws_bedrockagentcore_agent_runtime` resource lives there). The EKS example pins
the 5.x line because the community EKS module v20 constrains `aws < 6.0.0`. The two
paths are independent stacks, so the different pins never collide.

## Run the primary deployment (AgentCore Runtime)

```bash
# Build and push the ARM64 image first (records the tag in image-version.txt)
../scripts/build-and-push.sh

# Initialize
terraform init

# Plan / apply per environment
terraform apply -var-file="environments/dev.tfvars"
terraform apply -var-file="environments/prod.tfvars"
```

Or use the helper script, which selects a workspace per environment:

```bash
AGENTCORE_MEMORY_ID=<optional> ../scripts/deploy.sh dev
```

Copy `terraform.tfvars.example` to `terraform.tfvars` to see and set every variable.

## Run the EKS alternative

See `examples/eks/README.md`. In short:

```bash
cd examples/eks
./scripts/build-and-push.sh
AGENTCORE_MEMORY_ID=<optional> ./scripts/deploy.sh dev
```

## Remote state (S3 + DynamoDB)

State is local by default so the project works out of the box. For team use, store
state in S3 with a DynamoDB lock table. See `backend.tf` for the one-time bootstrap
commands and the two ways to enable it (uncomment the block, or pass a partial
backend config at init). Use a distinct state `key` per environment so dev and prod
never share state.

## Quality tooling

Formatting and validation:

```bash
terraform fmt -recursive
terraform init -backend=false && terraform validate            # root
(cd examples/complete && terraform init -backend=false && terraform validate)
(cd examples/eks      && terraform init -backend=false && terraform validate)
```

Static analysis and security scanning (run from `terraform/`):

```bash
tflint --recursive                 # lint HCL and provider best practices
checkov -d .                       # security and compliance scanning
```

Pre-commit: add a `.pre-commit-config.yaml` with the
`antonbabenko/pre-commit-terraform` hooks (`terraform_fmt`, `terraform_validate`,
`terraform_tflint`, `terraform_checkov`) and run `pre-commit install` so `fmt`,
`validate`, `tflint`, and `checkov` run automatically on every commit.

## Notes

- AgentCore Runtime names must be alphanumeric; the root derives one from
  `app_name` + `environment` (for example `idpAgentDev`).
- The IAM policy scopes AgentCore Memory access to a specific memory id when
  `memory_id` is set, otherwise to the account's memory namespace. Every statement
  is scoped to the deployment account and region.
- ECR tags are IMMUTABLE by default, so push a distinct tag per release; `latest`
  cannot be overwritten once pushed.
- Restrict CORS origins (EKS path) to your real UI origin in production, and set
  `cluster_endpoint_public_access_cidrs` to your operator/CI egress ranges.
- Security posture and accepted exceptions are documented in
  [`SECURITY.md`](./SECURITY.md).
