#!/bin/bash
# Builds the ARM64 (Graviton) image and pushes it to ECR. Run this BEFORE
# terraform apply so the deployment can pull the image.
# Usage: ./scripts/build-and-push.sh [region] [repo-name]
set -euo pipefail

REGION="${1:-${AWS_REGION:-us-east-1}}"
REPO="${2:-idp-agent}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}"

# The app root is four levels up: examples/eks/scripts -> examples/eks ->
# examples -> terraform -> idp-agent.
APP_DIR="$(cd "$(dirname "$0")/../../../.." && pwd)"

echo "==> Building jar in ${APP_DIR}"
( cd "$APP_DIR" && mvn -q clean package -DskipTests )

echo "==> Ensuring ECR repository '${REPO}' exists"
aws ecr describe-repositories --repository-names "$REPO" --region "$REGION" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name "$REPO" --region "$REGION" >/dev/null

echo "==> Logging in to ECR"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "==> Building and pushing linux/arm64 image ${ECR_URI}:${IMAGE_TAG}"
# Only the timestamped tag is pushed, so this also works against an IMMUTABLE
# repository (such as the one the AgentCore Terraform path creates).
docker buildx build --platform linux/arm64 \
  -t "${ECR_URI}:${IMAGE_TAG}" \
  --push "$APP_DIR"

echo "$IMAGE_TAG" > "$(dirname "$0")/../image-version.txt"
echo "==> Done. Use image_tag=${IMAGE_TAG} (recorded in examples/eks/image-version.txt)"
