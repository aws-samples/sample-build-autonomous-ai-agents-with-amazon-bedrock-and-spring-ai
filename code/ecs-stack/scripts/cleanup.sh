#!/bin/bash
# Destroys the ECS web stack (services, load balancer, VPC, ECR repositories and
# their images). The AgentCore Runtime stacks are not touched.
# Usage: ./scripts/cleanup.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/.."

terraform workspace select "$ENVIRONMENT"
TAG="$(cat image-version.txt 2>/dev/null || echo "")"
terraform destroy -input=false -auto-approve \
  -var-file="environments/${ENVIRONMENT}.tfvars" -var "ui_image_tag=${TAG}"
