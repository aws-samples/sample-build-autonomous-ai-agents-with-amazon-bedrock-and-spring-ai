#!/bin/bash
# Destroys all Terraform-managed resources for the agent in an environment.
# The AgentCore Memory resource is NOT deleted here (it may hold data you want to keep).
# Usage: [AGENTCORE_MEMORY_ID=<id>] ./scripts/cleanup.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/../terraform"

VARFILE="environments/${ENVIRONMENT}.tfvars"
terraform workspace select "$ENVIRONMENT"
terraform destroy -input=false -auto-approve \
  -var-file="$VARFILE" \
  -var "memory_id=${AGENTCORE_MEMORY_ID:-}"

echo "Destroyed ${ENVIRONMENT} resources."
echo "If you created an AgentCore Memory resource, delete it separately with:"
echo "  aws bedrock-agentcore-control delete-memory --memory-id <id> --region <region>"
