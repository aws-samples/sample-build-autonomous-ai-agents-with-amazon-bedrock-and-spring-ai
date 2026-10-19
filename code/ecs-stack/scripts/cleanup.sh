#!/bin/bash
# Destroys the ECS web stack (services, load balancer, NAT gateway, VPC, ECR
# repositories and their images, KMS key scheduled for deletion). The AgentCore
# Runtime stacks are not touched.
# Usage: ./scripts/cleanup.sh [environment]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
cd "$(dirname "$0")/.."

terraform workspace select "$ENVIRONMENT"
TAG="$(cat image-version.txt 2>/dev/null || echo "")"
VARS=(-var-file="environments/${ENVIRONMENT}.tfvars" -var "ui_image_tag=${TAG}")

# The load balancer has deletion protection on; switch it off first.
terraform apply -input=false -auto-approve "${VARS[@]}" \
  -var "alb_deletion_protection=false" -target=aws_lb.this
terraform destroy -input=false -auto-approve "${VARS[@]}" \
  -var "alb_deletion_protection=false"
