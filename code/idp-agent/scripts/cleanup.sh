#!/bin/bash
# Destroys all Terraform-managed resources for the IDP agent in an environment.
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/cleanup.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/../terraform"

VARFILE="environments/${ENVIRONMENT}.tfvars"
terraform workspace select "$ENVIRONMENT"
# verify_image=false: destroy must not depend on the image still being in ECR.
terraform destroy -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}" \
  -var "verify_image=false"

echo "Destroyed ${ENVIRONMENT} resources."
echo "If you created a memory resource, delete it with:"
echo "  aws bedrock-agentcore-control delete-memory --memory-id <id> --region <region>"
