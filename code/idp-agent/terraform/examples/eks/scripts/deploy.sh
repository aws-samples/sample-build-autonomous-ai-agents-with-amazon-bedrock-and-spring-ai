#!/bin/bash
# One-command EKS deploy: VPC + EKS (Graviton) + IRSA + app.
# Run scripts/build-and-push.sh first so the image exists in ECR.
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/deploy.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/.."

VARFILE="environments/${ENVIRONMENT}.tfvars"
[ -f "$VARFILE" ] || { echo "Missing ${VARFILE}" >&2; exit 1; }

IMAGE_TAG="latest"
[ -f image-version.txt ] && IMAGE_TAG="$(cat image-version.txt)"

terraform init -input=false
terraform apply -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "image_tag=${IMAGE_TAG}" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}"

echo ""
echo "Configure kubectl:"
terraform output -raw configure_kubectl
echo ""
echo "External endpoint (allow ~2 min for the load balancer):"
terraform output -raw load_balancer_hostname || true
