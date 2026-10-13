#!/bin/bash
# Builds the ARM64 agent image and pushes it to the environment's ECR repository.
# The repository name matches the one Terraform manages: <app_name>-<environment>.
# Usage: ./scripts/build-and-push.sh [environment] [region]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
REGION="${2:-${AWS_REGION:-us-east-1}}"
APP_NAME="autonomous-agent"
REPO="${APP_NAME}-${ENVIRONMENT}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}"

echo "==> Building application jar"
mvn -q clean package -DskipTests

echo "==> Logging in to ECR"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "==> Building and pushing linux/arm64 image: ${ECR_URI}:${IMAGE_TAG}"
docker buildx build --platform linux/arm64 \
  -t "${ECR_URI}:${IMAGE_TAG}" \
  -t "${ECR_URI}:latest" \
  --push .

echo "$IMAGE_TAG" > terraform/image-version.txt
echo "==> Pushed. Image tag recorded in terraform/image-version.txt"
