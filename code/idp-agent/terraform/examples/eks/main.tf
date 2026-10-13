# Complete EKS example: a self-contained, security-hardened deployment of the IDP
# agent to Amazon EKS in standalone mode (no AgentCore Runtime).
#
# This provisions everything: a VPC (private + public subnets, NAT, flow logs), an
# EKS cluster with private API access, control-plane logging, and secrets envelope
# encryption, Graviton (arm64) managed nodes with IMDSv2 and KMS-encrypted EBS, a
# least-privilege IRSA role (modules/eks-irsa), and the hardened Kubernetes
# workload (modules/eks-workload). Build and push the arm64 image with
# scripts/build-and-push.sh before apply.
#
# To deploy into an EXISTING cluster instead, drop the vpc and eks modules and
# wire modules/eks-irsa + modules/eks-workload to your cluster's OIDC provider
# and endpoint.

data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}

# The image is built and pushed by scripts/build-and-push.sh before apply.
data "aws_ecr_repository" "app" {
  name = var.ecr_repository_name
}

locals {
  account_id   = data.aws_caller_identity.current.account_id
  name         = "${var.app_name}-${var.environment}"
  cluster_name = "${var.app_name}-${var.environment}"
  azs          = slice(data.aws_availability_zones.available.names, 0, 2)
  namespace    = "idp"
  service_acct = "idp-agent"
  vpc_cidr     = "10.0.0.0/16"
  image_uri    = "${data.aws_ecr_repository.app.repository_url}:${var.image_tag}"

  memory_resource_arn = var.memory_id == "" ? "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/*" : "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:memory/${var.memory_id}"

  tags = merge({
    Application = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }, var.tags)
}

# ---------------------------------------------------------------------------
# KMS keys (customer-managed) for node EBS volumes and CloudWatch log groups
# ---------------------------------------------------------------------------

# Key used to encrypt worker node EBS volumes. The Auto Scaling service-linked
# role must be able to use the key so it can launch instances from KMS-encrypted
# snapshots.
#
# This is a KMS KEY policy attached directly to the key, not an IAM identity
# policy. In a key policy the Resource element must be "*", which resolves to
# "this key" only. Access is scoped by principal (account root and the Auto
# Scaling service-linked role) and, for grants, by kms:GrantIsForAWSResource.
resource "aws_kms_key" "ebs" {
  description             = "${local.name} EKS node EBS volume encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true
  tags                    = local.tags

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "EnableRootAccountAdmin"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:root" }
        Action    = "kms:*"
        Resource  = "*"
      },
      {
        Sid       = "AllowAutoScalingUseOfKey"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling" }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
      },
      {
        Sid       = "AllowAutoScalingCreateGrant"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling" }
        Action    = "kms:CreateGrant"
        Resource  = "*"
        Condition = {
          Bool = { "kms:GrantIsForAWSResource" = "true" }
        }
      }
    ]
  })
}

resource "aws_kms_alias" "ebs" {
  name          = "alias/${local.name}-ebs"
  target_key_id = aws_kms_key.ebs.key_id
}

# Key used to encrypt the VPC flow log CloudWatch log group.
#
# KMS KEY policy attached directly to the key (not an IAM identity policy).
# Resource "*" resolves to this key only; access is scoped by principal
# (account root and the CloudWatch Logs service) and by the aws:logs:arn
# encryption-context condition.
resource "aws_kms_key" "logs" {
  description             = "${local.name} CloudWatch log group encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true
  tags                    = local.tags

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "EnableRootAccountAdmin"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:root" }
        Action    = "kms:*"
        Resource  = "*"
      },
      {
        Sid       = "AllowCloudWatchLogs"
        Effect    = "Allow"
        Principal = { Service = "logs.${var.region}.amazonaws.com" }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
        Condition = {
          ArnLike = { "kms:EncryptionContext:aws:logs:arn" = "arn:aws:logs:${var.region}:${local.account_id}:log-group:*" }
        }
      }
    ]
  })
}

resource "aws_kms_alias" "logs" {
  name          = "alias/${local.name}-logs"
  target_key_id = aws_kms_key.logs.key_id
}

# ---------------------------------------------------------------------------
# Network: VPC with private + public subnets, single NAT, and flow logs
# ---------------------------------------------------------------------------
module "vpc" {
  # Pinned, signed Terraform Registry module. Registry modules are versioned by
  # tag, not by commit hash, so the commit-hash check does not apply.
  #checkov:skip=CKV_TF_1:Registry module pinned by semantic version, not a git commit hash
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.13"

  name = "${local.name}-vpc"
  cidr = local.vpc_cidr

  azs             = local.azs
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true

  # Do not auto-assign public IPs; nodes run in private subnets only.
  map_public_ip_on_launch = false

  # VPC flow logs to an encrypted CloudWatch log group for network audit.
  enable_flow_log                                 = true
  create_flow_log_cloudwatch_iam_role             = true
  create_flow_log_cloudwatch_log_group            = true
  flow_log_cloudwatch_log_group_retention_in_days = 365
  flow_log_cloudwatch_log_group_kms_key_id        = aws_kms_key.logs.arn
  flow_log_max_aggregation_interval               = 60

  # Tags required by the AWS Load Balancer Controller / EKS for subnet discovery.
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  tags = local.tags
}

# ---------------------------------------------------------------------------
# EKS cluster + Graviton (arm64) managed node group
# ---------------------------------------------------------------------------
module "eks" {
  # Pinned, signed Terraform Registry module. Registry modules are versioned by
  # tag, not by commit hash, so the commit-hash check does not apply.
  #checkov:skip=CKV_TF_1:Registry module pinned by semantic version, not a git commit hash
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.24"

  cluster_name    = local.cluster_name
  cluster_version = var.kubernetes_version

  # API endpoint: keep private access on, and restrict public access to an
  # explicit CIDR allowlist (never 0.0.0.0/0). Set the allowlist in tfvars.
  cluster_endpoint_private_access      = true
  cluster_endpoint_public_access       = true
  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs

  enable_irsa = true

  # Control-plane audit and diagnostics logging to CloudWatch.
  cluster_enabled_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

  # Envelope-encrypt Kubernetes secrets with a dedicated KMS key (created and
  # managed by the module) with rotation enabled.
  create_kms_key            = true
  enable_kms_key_rotation   = true
  cluster_encryption_config = { resources = ["secrets"] }

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  # The identity running terraform administers the cluster.
  enable_cluster_creator_admin_permissions = true

  eks_managed_node_groups = {
    default = {
      ami_type       = "AL2023_ARM_64_STANDARD"
      instance_types = var.node_instance_types
      desired_size   = var.node_desired_size
      min_size       = var.node_min_size
      max_size       = var.node_max_size

      # Private subnets only; no public IP.
      subnet_ids = module.vpc.private_subnets

      # Enforce IMDSv2 and block the pod hop to the node's instance metadata.
      metadata_options = {
        http_endpoint               = "enabled"
        http_tokens                 = "required"
        http_put_response_hop_limit = 1
        instance_metadata_tags      = "disabled"
      }

      # KMS-encrypted, right-sized root volume.
      block_device_mappings = {
        xvda = {
          device_name = "/dev/xvda"
          ebs = {
            volume_size           = 50
            volume_type           = "gp3"
            encrypted             = true
            kms_key_id            = aws_kms_key.ebs.arn
            delete_on_termination = true
          }
        }
      }
    }
  }

  tags = local.tags
}

# ---------------------------------------------------------------------------
# Least-privilege IAM role for the app's Kubernetes ServiceAccount (IRSA)
# ---------------------------------------------------------------------------
module "eks_irsa" {
  source = "../../modules/eks-irsa"

  role_name           = "${local.name}-irsa"
  oidc_provider_arn   = module.eks.oidc_provider_arn
  oidc_provider_url   = module.eks.oidc_provider
  namespace           = local.namespace
  service_account     = local.service_acct
  account_id          = local.account_id
  region              = var.region
  memory_resource_arn = local.memory_resource_arn
  tags                = local.tags
}

# ---------------------------------------------------------------------------
# Application: namespace, service account (IRSA), deployment, service, HPA
# ---------------------------------------------------------------------------
module "eks_workload" {
  source = "../../modules/eks-workload"

  app_name               = var.app_name
  region                 = var.region
  namespace              = local.namespace
  service_account        = local.service_acct
  irsa_role_arn          = module.eks_irsa.role_arn
  image                  = local.image_uri
  replicas               = var.replicas
  memory_id              = var.memory_id
  cors_allowed_origins   = var.cors_allowed_origins
  load_balancer_internal = var.load_balancer_internal
  allowed_ingress_cidr   = local.vpc_cidr

  depends_on = [module.eks]
}
