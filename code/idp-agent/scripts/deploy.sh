#!/bin/bash
# One-command deploy for an environment. Orchestrates the correct order:
#   1. create the ECR repository (so we have somewhere to push)
#   2. build + push the ARM64 image
#   3. provision the IAM role + AgentCore Runtime pointing at that image
#
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/deploy.sh [environment]
#   environment: dev (default) | prod | staging
set -euo pipefail

ENVIRONMENT="${1:-dev}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/terraform"

VARFILE="environments/${ENVIRONMENT}.tfvars"
if [ ! -f "$VARFILE" ]; then
  echo "No tfvars for environment '${ENVIRONMENT}' at ${VARFILE}" >&2
  exit 1
fi

MEMORY_ID="${AGENTCORE_MEMORY_ID:-}"
REGION="$(sed -n 's/^[[:space:]]*region[[:space:]]*=[[:space:]]*"\(.*\)".*/\1/p' "$VARFILE" | head -n1)"
REGION="${REGION:-${AWS_REGION:-us-east-1}}"

terraform init -input=false
terraform workspace select "$ENVIRONMENT" 2>/dev/null || terraform workspace new "$ENVIRONMENT"

echo "==> Phase 1/3: create ECR repository"
terraform apply -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "memory_id=${MEMORY_ID}" \
  -target=module.ecr

echo "==> Phase 2/3: build and push image"
"$ROOT/scripts/build-and-push.sh" "$ENVIRONMENT" "$REGION"

IMAGE_TAG="$(cat image-version.txt)"

echo "==> Phase 3/3: provision IAM role + AgentCore Runtime"
terraform apply -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "memory_id=${MEMORY_ID}" \
  -var "image_tag=${IMAGE_TAG}"

echo ""
echo "Environment:  ${ENVIRONMENT}"
echo "Runtime name: $(terraform output -raw runtime_name)"
echo "Runtime ARN:  $(terraform output -raw runtime_arn)"
