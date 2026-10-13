#!/bin/bash
# Tears down the entire EKS stack (app, cluster, node group, VPC, IRSA role).
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/cleanup.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/.."

VARFILE="environments/${ENVIRONMENT}.tfvars"

IMAGE_TAG="latest"
[ -f image-version.txt ] && IMAGE_TAG="$(cat image-version.txt)"

# Destroying the Service removes the AWS load balancer it created; destroy in order.
terraform destroy -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "image_tag=${IMAGE_TAG}" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}"

echo "EKS stack destroyed."
echo "Remember to remove ECR images and any AgentCore Memory resource you created:"
echo "  aws ecr batch-delete-image / delete-repository"
echo "  aws bedrock-agentcore-control delete-memory --memory-id <id> --region <region>"
