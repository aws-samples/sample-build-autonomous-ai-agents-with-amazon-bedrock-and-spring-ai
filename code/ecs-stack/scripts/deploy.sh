#!/bin/bash
# Deploys the browser-facing web stack on Amazon ECS Fargate:
#   HTTPS load balancer -> landing page (:443), travel UI (:8081), IDP console (:8082)
#   ECS tasks run in private subnets (no public IPs) behind a NAT gateway
#   UIs -> Service Connect -> autonomous-agent / idp-agent backends (not public)
#
# Prerequisite: the backend images exist in ECR. Run the AgentCore deploys first
# (code/autonomous-agent/scripts/deploy.sh and code/idp-agent/scripts/deploy.sh);
# this stack reuses the exact images they pushed.
#
# Usage: ALLOWED_CIDR=203.0.113.10/32 ./scripts/deploy.sh [environment]
#   ALLOWED_CIDR  who may reach the load balancer (default: your current public IP)
set -euo pipefail

ENVIRONMENT="${1:-dev}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CODE="$(cd "$ROOT/.." && pwd)"
cd "$ROOT"

VARFILE="environments/${ENVIRONMENT}.tfvars"
[ -f "$VARFILE" ] || { echo "No tfvars for environment '${ENVIRONMENT}' at ${VARFILE}" >&2; exit 1; }
# Use the same Region Terraform deploys to.
REGION="$(sed -n 's/^[[:space:]]*region[[:space:]]*=[[:space:]]*"\(.*\)".*/\1/p' "$VARFILE" | head -n1)"
REGION="${REGION:-${AWS_REGION:-us-east-1}}"
export AWS_REGION="$REGION"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
ALLOWED_CIDR="${ALLOWED_CIDR:-$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')/32}"

AUTONOMOUS_TAG="$(cat "$CODE/autonomous-agent/terraform/image-version.txt")"
IDP_TAG="$(cat "$CODE/idp-agent/terraform/image-version.txt")"

cat > terraform.tfvars <<EOF
allowed_cidrs          = ["${ALLOWED_CIDR}"]
autonomous_agent_image = "${REGISTRY}/autonomous-agent-${ENVIRONMENT}:${AUTONOMOUS_TAG}"
idp_agent_image        = "${REGISTRY}/idp-agent-${ENVIRONMENT}:${IDP_TAG}"
EOF

terraform init -input=false
terraform workspace select "$ENVIRONMENT" 2>/dev/null || terraform workspace new "$ENVIRONMENT"

# On a re-deploy, keep the running services on their current images during
# phase 1 (an empty ui_image_tag would remove them until phase 3).
CURRENT_TAG="$(cat image-version.txt 2>/dev/null || echo "")"

echo "==> Phase 1/3: network, load balancer, ECR repositories, cluster"
terraform apply -input=false -auto-approve -var-file="$VARFILE" -var "ui_image_tag=${CURRENT_TAG}"
LANDING_URL="$(terraform output -raw landing_url)/"

echo "==> Phase 2/3: build and push the landing page and UI images (linux/arm64)"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
# Docker buildx (the Dockerfiles) when available, otherwise Jib CLI with each
# folder's jib.yaml (no Docker daemon needed). Force with BUILDER=docker|jib.
BUILDER="${BUILDER:-}"
if [ -z "$BUILDER" ]; then
  if docker buildx version >/dev/null 2>&1; then BUILDER=docker
  elif command -v jib >/dev/null 2>&1; then BUILDER=jib
  else
    echo "Neither Docker buildx nor Jib CLI found. Install one of them:" >&2
    echo "  Docker Desktop, or https://github.com/GoogleContainerTools/jib/tree/master/jib-cli" >&2
    exit 1
  fi
fi
echo "    builder: ${BUILDER}"

if [ "$BUILDER" = "docker" ]; then
  aws ecr get-login-password --region "$REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"
fi
for app in landing-page autonomous-agent-ui idp-agent-ui; do
  target="${REGISTRY}/agentcore-web-${ENVIRONMENT}-${app}:${IMAGE_TAG}"
  if [ "$BUILDER" = "docker" ]; then
    docker buildx build --platform linux/arm64 \
      --build-arg "VITE_LANDING_URL=${LANDING_URL}" \
      -t "$target" --push "$CODE/$app"
  else
    (
      cd "$CODE/$app"
      npm ci --no-audit --no-fund
      VITE_LANDING_URL="$LANDING_URL" npm run build
      jib build --build-file=jib.yaml \
        --parameter="CREATION_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        --target="$target" \
        --to-username=AWS --to-password="$(aws ecr get-login-password --region "$REGION")"
    )
  fi
done
echo "$IMAGE_TAG" > image-version.txt

echo "==> Phase 3/3: ECS services"
terraform apply -input=false -auto-approve -var-file="$VARFILE" -var "ui_image_tag=${IMAGE_TAG}"

echo ""
echo "Landing page: $(terraform output -raw landing_url)"
echo "Travel UI:    $(terraform output -raw travel_ui_url)"
echo "IDP console:  $(terraform output -raw idp_ui_url)"
echo "Allowed from: ${ALLOWED_CIDR}"
