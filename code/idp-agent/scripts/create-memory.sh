#!/bin/bash
# Optionally creates an AgentCore Memory resource for cross-document session context.
# Usage: ./scripts/create-memory.sh [memory-name] [region]
set -euo pipefail

MEMORY_NAME="${1:-idp_agent_memory}"
REGION="${2:-${AWS_REGION:-us-east-1}}"

if ! aws sts get-caller-identity --no-cli-pager >/dev/null 2>&1; then
  echo "AWS credentials not configured." >&2
  exit 1
fi

EXISTING=$(aws bedrock-agentcore-control list-memories --region "$REGION" --no-cli-pager \
  --query "memories[?starts_with(id, '${MEMORY_NAME}')].id | [0]" --output text 2>/dev/null || true)

if [ -n "${EXISTING}" ] && [ "${EXISTING}" != "None" ]; then
  echo "Found existing memory: ${EXISTING}"
  echo "export AGENTCORE_MEMORY_ID=${EXISTING}"
  exit 0
fi

echo "Creating memory ${MEMORY_NAME} in ${REGION}..."
MEMORY_ID=$(aws bedrock-agentcore-control create-memory \
  --region "$REGION" --no-cli-pager \
  --name "$MEMORY_NAME" \
  --event-expiry-duration 90 \
  --query "memory.id" --output text)

echo "Created memory: ${MEMORY_ID}"
echo "  export AGENTCORE_MEMORY_ID=${MEMORY_ID}"
