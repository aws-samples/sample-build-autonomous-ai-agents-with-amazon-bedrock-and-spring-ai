#!/bin/bash
# Builds the agent image for an EKS node architecture and pushes it to ECR.
# Usage: [TARGET_ARCH=amd64|arm64] ./scripts/build-and-push-eks.sh [region] [app-name] [environment]
set -euo pipefail

REGION="${1:-${AWS_REGION:-us-east-1}}"
APP_NAME="${2:-idp-agent}"
ENVIRONMENT="${3:-dev}"
TARGET_ARCH="${TARGET_ARCH:-amd64}"   # most EKS node groups are amd64; use arm64 for Graviton
REPO="$(echo "${APP_NAME}-${ENVIRONMENT}" | tr '[:upper:]' '[:lower:]')"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}"

echo "==> Building application jar"
mvn -q clean package -DskipTests

echo "==> Ensuring ECR repository exists"
aws ecr describe-repositories --repository-names "$REPO" --region "$REGION" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name "$REPO" --region "$REGION" >/dev/null

echo "==> Logging in to ECR"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "==> Building and pushing linux/${TARGET_ARCH} image: ${ECR_URI}:${IMAGE_TAG}"
# Only the timestamped tag is pushed, so this also works against an IMMUTABLE
# repository (such as the one the AgentCore Terraform path creates).
docker buildx build --platform "linux/${TARGET_ARCH}" \
  -t "${ECR_URI}:${IMAGE_TAG}" \
  --push .

# deploy-eks.sh reads the tag from here.
echo "$IMAGE_TAG" > terraform/examples/eks/image-version.txt

echo ""
echo "Image: ${ECR_URI}:${IMAGE_TAG}"
echo "Set 'ecr_repository_name=${REPO}' and 'image_tag=${IMAGE_TAG}' in"
echo "terraform/examples/eks (terraform.tfvars or environments/<env>.tfvars)."
echo "Note: the consolidated EKS example uses arm64 Graviton nodes; build with TARGET_ARCH=arm64."
