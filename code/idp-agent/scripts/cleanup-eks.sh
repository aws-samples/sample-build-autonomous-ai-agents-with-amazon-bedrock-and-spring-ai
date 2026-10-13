#!/bin/bash
# Tears down the entire EKS stack (app, cluster, node group, VPC, IRSA role)
# created by deploy-eks.sh via terraform/examples/eks.
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/cleanup-eks.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/../terraform/examples/eks"

VARFILE="environments/${ENVIRONMENT}.tfvars"

IMAGE_TAG="latest"
[ -f image-version.txt ] && IMAGE_TAG="$(cat image-version.txt)"

terraform destroy -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "image_tag=${IMAGE_TAG}" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}"

echo "EKS stack destroyed."
echo "Remove ECR images and any AgentCore Memory resource you created separately:"
echo "  aws ecr delete-repository --repository-name <repo> --force --region <region>"
echo "  aws bedrock-agentcore-control delete-memory --memory-id <id> --region <region>"
