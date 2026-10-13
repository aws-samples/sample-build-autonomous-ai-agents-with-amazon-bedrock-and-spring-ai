# eks-workload

## Purpose

Deploys the IDP agent onto an existing Amazon EKS cluster with a hardened security
posture. Creates:

- A namespace labeled for Pod Security Standards `restricted` (enforce/audit/warn).
- A service account annotated with the IRSA role ARN (token auto-mount off by
  default; the deployment opts in for IRSA).
- A Deployment whose pod and container security contexts run non-root
  (`runAsUser = 1000`), read-only root filesystem (with an `emptyDir` at `/tmp`),
  no privilege escalation, all capabilities dropped, and `seccompProfile`
  `RuntimeDefault`, plus Spring Boot Actuator startup/liveness/readiness probes and
  resource requests/limits.
- A NetworkPolicy with default-deny ingress, allowing the app port only from the
  same namespace and the in-VPC load balancer CIDR (`allowed_ingress_cidr`).
- A Service (internal NLB by default; set `load_balancer_internal = false` only
  behind TLS), a Horizontal Pod Autoscaler, and a PodDisruptionBudget.

Pair this with the `eks-irsa` module, which creates the least-privilege IAM role
whose ARN is passed in via `irsa_role_arn`.

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.6 |
| kubernetes | ~> 2.30 |

## Providers

| Name | Version |
|------|---------|
| kubernetes | ~> 2.30 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| app_name | Application / workload name. | `string` | n/a | yes |
| region | AWS region passed to the container as AWS_REGION. | `string` | n/a | yes |
| irsa_role_arn | IRSA role ARN annotated on the service account. | `string` | n/a | yes |
| image | Full container image reference (repo:tag). | `string` | n/a | yes |
| namespace | Kubernetes namespace to deploy into. | `string` | `"idp"` | no |
| service_account | Kubernetes service account name. | `string` | `"idp-agent"` | no |
| replicas | Initial replica count. | `number` | `2` | no |
| min_replicas | HPA minimum replicas. | `number` | `2` | no |
| max_replicas | HPA maximum replicas. | `number` | `10` | no |
| cpu_target_percent | HPA target average CPU utilization. | `number` | `70` | no |
| memory_id | Optional AgentCore Memory id. | `string` | `""` | no |
| cors_allowed_origins | Comma-separated CORS origins. | `string` | `"*"` | no |
| container_port | Container port. | `number` | `8080` | no |
| service_type | Service type: LoadBalancer, ClusterIP, or NodePort. | `string` | `"LoadBalancer"` | no |
| load_balancer_internal | Provision an internal (VPC-only) NLB when service_type is LoadBalancer. | `bool` | `true` | no |
| allowed_ingress_cidr | CIDR allowed to reach the app port by the NetworkPolicy (plus same-namespace). | `string` | `"10.0.0.0/16"` | no |
| enable_pod_disruption_budget | Create a PodDisruptionBudget. | `bool` | `true` | no |
| pdb_min_available | Minimum available pods for the PodDisruptionBudget. | `number` | `1` | no |
| service_port | Port the Service exposes. | `number` | `80` | no |
| resources | Container resource requests/limits. | `object` | see variables.tf | no |
| tags | Tags recorded for parity with AWS modules. | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| namespace | Namespace the workload runs in. |
| service_account | Service account name bound to the IRSA role. |
| service_name | In-cluster Kubernetes service name. |
| service_dns | In-cluster service DNS name. |
| load_balancer_hostname | External load balancer hostname (LoadBalancer only). |

## Usage

```hcl
module "eks_workload" {
  source          = "../../modules/eks-workload"
  app_name        = "idp-agent"
  region          = "us-east-1"
  irsa_role_arn   = module.eks_irsa.role_arn
  image           = "123456789012.dkr.ecr.us-east-1.amazonaws.com/idp-agent-dev:latest"
  namespace       = "idp"
  service_account = "idp-agent"
  replicas        = 2
  memory_id       = ""
}
```
