#!/bin/bash
# Provisions ECR, IAM, and the AgentCore Runtime for the IDP agent with Terraform.
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/deploy.sh [environment]
#   environment: dev (default) | prod | staging
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/../terraform"

VARFILE="environments/${ENVIRONMENT}.tfvars"
if [ ! -f "$VARFILE" ]; then
  echo "No tfvars for environment '${ENVIRONMENT}' at ${VARFILE}" >&2
  exit 1
fi

IMAGE_TAG="latest"
if [ -f image-version.txt ]; then
  IMAGE_TAG="$(cat image-version.txt)"
fi

terraform init -input=false
terraform workspace select "$ENVIRONMENT" 2>/dev/null || terraform workspace new "$ENVIRONMENT"
terraform apply -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}" \
  -var "image_tag=${IMAGE_TAG}"

echo ""
echo "Environment: ${ENVIRONMENT}"
echo "Runtime name: $(terraform output -raw runtime_name)"
echo "Runtime ARN:  $(terraform output -raw runtime_arn)"
