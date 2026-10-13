# Complete example: EKS (standalone alternative)

A self-contained, production-grade deployment of the IDP agent to Amazon EKS using
the Spring AI AgentCore SDK in standalone mode (no AgentCore Runtime). This is the
consolidated EKS path: it replaces the former `deploy-eks/` and `terraform/eks/`
trees.

Terraform provisions a VPC (private + public subnets, NAT, KMS-encrypted flow
logs), an EKS cluster with private API access, control-plane logging, and secrets
envelope encryption, Graviton (arm64) managed nodes with IMDSv2 and KMS-encrypted
EBS, a least-privilege IRSA role for the app's service account (`modules/eks-irsa`),
and the hardened Kubernetes Deployment, Service (internal NLB by default), HPA, and
NetworkPolicy (`modules/eks-workload`).

See `../../SECURITY.md` for the full posture and the accepted-exceptions table.

## Layout

```
examples/eks/
├── main.tf            # VPC + EKS (registry modules) + eks-irsa + eks-workload
├── variables.tf  outputs.tf  versions.tf
├── terraform.tfvars.example
├── environments/{dev,prod}.tfvars
└── scripts/{build-and-push,deploy,cleanup}.sh
```

Supporting modules live under `terraform/modules/`:

- `eks-irsa` - least-privilege IAM role bound to the K8s service account (OIDC/IRSA)
- `eks-workload` - namespace, service account, deployment, service, HPA

## Prerequisites

- AWS CLI v2, `kubectl`, Terraform >= 1.6, Docker (with `buildx`)
- AWS account with Amazon Bedrock model access enabled
- IAM permissions to create VPC, EKS, IAM, and ECR resources

## Deploy (two commands)

```bash
# 1. Build + push the arm64 image to ECR (creates the repo if needed)
./scripts/build-and-push.sh

# 2. Provision everything and deploy the app (dev | prod)
AGENTCORE_MEMORY_ID=<optional> ./scripts/deploy.sh dev
```

Terraform prints the kubeconfig command and the external endpoint. Then:

```bash
aws eks update-kubeconfig --name idp-agent-dev --region us-east-1
kubectl -n idp get pods
URL=$(kubectl -n idp get svc idp-agent-svc -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

curl -X POST "http://$URL/api/documents" \
  -F "agentType=BANK_CHECK" \
  -F "question=Is this check signed?" \
  -F "file=@../../../samples/insurance-policy.txt;type=text/plain"
```

## Validate only (no AWS calls, no apply)

```bash
terraform init -backend=false
terraform validate
```

`init` downloads the VPC and EKS registry modules and the providers, which needs
network access but makes no AWS API calls.

## Why Graviton

The node group uses arm64 Graviton instances (`t4g`/`m7g`), so the same arm64 image
used for AgentCore Runtime runs unchanged on EKS: better price/performance and one
image to maintain.

## Least-privilege IAM (IRSA)

The app pod assumes an IAM role through IRSA. The trust policy restricts assumption
to the exact `system:serviceaccount:idp:idp-agent` identity via the cluster OIDC
provider, and the permission policy grants only `bedrock:InvokeModel*` and the four
AgentCore Memory actions, scoped to your memory resource ARN. No node-level
credentials, no broad managed policies.

## Deploy into an existing cluster

If you already run a central EKS cluster, skip the `vpc` and `eks` modules in
`main.tf` and wire `modules/eks-irsa` and `modules/eks-workload` to your cluster's
OIDC provider ARN/URL and API endpoint (configure the `kubernetes` provider with
your cluster's `host` and CA). The two supporting modules are cluster-agnostic.

## Cleanup (AWS cost-hygiene)

Always tear down what you created to avoid ongoing charges:

```bash
./scripts/cleanup.sh dev
```

This runs `terraform destroy`, removing the app, the load balancer, the node group,
the EKS control plane, the VPC/NAT gateway, and the IRSA role. Then remove
leftovers:

```bash
# ECR images / repository
aws ecr batch-delete-image --repository-name idp-agent --image-ids imageTag=latest --region us-east-1
aws ecr delete-repository --repository-name idp-agent --force --region us-east-1

# AgentCore Memory (if you created one)
aws bedrock-agentcore-control delete-memory --memory-id <id> --region us-east-1
```

Verify in the console that the EKS cluster, NAT gateway, and load balancer are gone;
these are the main cost drivers.

## Security hardening

This example is tuned to pass a strict AWS security review. Highlights:

- Control plane: `api`/`audit`/`authenticator`/`controllerManager`/`scheduler`
  logging, KMS secrets envelope encryption, private endpoint access, and a public
  endpoint restricted to `cluster_endpoint_public_access_cidrs` (validated to
  reject `0.0.0.0/0`). Kubernetes pinned to `1.33`.
- Nodes: IMDSv2 required (hop limit 1), KMS-encrypted `gp3` EBS, Graviton/arm64,
  private subnets only, no public IP.
- Network: KMS-encrypted VPC flow logs (365-day retention), no `0.0.0.0/0` ingress,
  internal NLB by default.
- Workload: Pod Security Standards `restricted`, non-root, read-only root FS,
  dropped capabilities, `seccompProfile RuntimeDefault`, NetworkPolicy default-deny
  ingress, and a PodDisruptionBudget.

Set `cluster_endpoint_public_access_cidrs` to your operator/CI egress ranges before
apply; the default is an RFC 5737 placeholder that reaches nothing.

### Switching to public HTTPS

The Service is an internal NLB by default (`load_balancer_internal = true`). To
expose the agent publicly over TLS, install the AWS Load Balancer Controller, add
an Ingress backed by an ACM certificate (HTTPS listener), and set
`load_balancer_internal = false`. Do not expose the plaintext NLB to the internet.

## Notes

- Restrict `cors_allowed_origins` to your real UI origin in production.
- The two supporting modules (`eks-irsa`, `eks-workload`) are cluster-agnostic and
  can be wired to an existing cluster.
