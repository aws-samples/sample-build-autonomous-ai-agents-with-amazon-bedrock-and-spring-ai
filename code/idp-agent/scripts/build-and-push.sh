#!/bin/bash
# Builds the ARM64 IDP agent image and pushes it to ECR.
# Usage: ./scripts/build-and-push.sh [region] [app-name]
set -euo pipefail

REGION="${1:-${AWS_REGION:-us-east-1}}"
APP_NAME="${2:-IdpAgent}"
REPO="$(echo "$APP_NAME" | tr '[:upper:]' '[:lower:]')"
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

echo "==> Building and pushing linux/arm64 image: ${ECR_URI}:${IMAGE_TAG}"
docker buildx build --platform linux/arm64 \
  -t "${ECR_URI}:${IMAGE_TAG}" \
  -t "${ECR_URI}:latest" \
  --push .

echo "$IMAGE_TAG" > terraform/image-version.txt
echo "==> Pushed. Image tag recorded in terraform/image-version.txt"
