# eks-irsa

## Purpose

Creates a least-privilege IAM role for use with IAM Roles for Service Accounts
(IRSA) on Amazon EKS. The trust policy restricts assumption to one exact
Kubernetes service account (`system:serviceaccount:<namespace>:<service_account>`)
through the cluster's OIDC provider (validating both the `sub` and `aud` claims).
The permission policy grants only Bedrock model invocation and the four AgentCore
Memory actions the agent uses, scoped to the deployment region and account and to
the configured memory resource ARN, each gated by an `aws:RequestedRegion`
condition.

Pair this with the `eks-workload` module, which annotates the service account with
the role ARN this module outputs.

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.6 |
| aws | >= 5.40 |

This is a shared module, so it declares a minimum AWS provider version rather than
a pessimistic upper bound. The EKS example that consumes it pins `aws ~> 5.95`
(required by the terraform-aws-modules/eks v20 module).

## Providers

| Name | Version |
|------|---------|
| aws | >= 5.40 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| role_name | Name of the IRSA IAM role. | `string` | n/a | yes |
| oidc_provider_arn | ARN of the EKS cluster's IAM OIDC provider. | `string` | n/a | yes |
| oidc_provider_url | EKS OIDC provider issuer URL (scheme optional). | `string` | n/a | yes |
| namespace | Kubernetes namespace of the service account. | `string` | n/a | yes |
| service_account | Kubernetes service account name bound to this role. | `string` | n/a | yes |
| account_id | AWS account id. | `string` | n/a | yes |
| region | AWS region. | `string` | n/a | yes |
| memory_resource_arn | ARN (or wildcard) of the AgentCore Memory resource. | `string` | n/a | yes |
| tags | Tags applied to the IAM role. | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| role_arn | IRSA role ARN to annotate on the Kubernetes service account. |
| role_name | IRSA role name. |

## Usage

```hcl
module "eks_irsa" {
  source              = "../../modules/eks-irsa"
  role_name           = "idp-agent-dev-irsa"
  oidc_provider_arn   = module.eks.oidc_provider_arn
  oidc_provider_url   = module.eks.oidc_provider
  namespace           = "idp"
  service_account     = "idp-agent"
  account_id          = data.aws_caller_identity.current.account_id
  region              = "us-east-1"
  memory_resource_arn = "arn:aws:bedrock-agentcore:us-east-1:123456789012:memory/*"
  tags                = { Application = "idp-agent" }
}
```
