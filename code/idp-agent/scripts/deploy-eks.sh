#!/bin/bash
# One-command EKS deploy: provisions a VPC + EKS (Graviton) cluster + IRSA +
# Deployment + Service + HPA via the consolidated example under
# terraform/examples/eks. Configure environments/<env>.tfvars first.
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/deploy-eks.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/../terraform/examples/eks"

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
echo "Namespace:   $(terraform output -raw namespace)"
echo "Service DNS: $(terraform output -raw service_dns)"
echo "IRSA role:   $(terraform output -raw irsa_role_arn)"
echo ""
echo "External endpoint (allow ~2 min for the load balancer):"
terraform output -raw load_balancer_hostname || true
