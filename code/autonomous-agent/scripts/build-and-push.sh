#!/bin/bash
# Builds the ARM64 agent image and pushes it to the environment's ECR repository.
# The repository name matches the one Terraform manages: <app_name>-<environment>.
#
# Uses Docker buildx when it is available (the Dockerfile). Without Docker it
# falls back to Jib through Maven, which builds the same linux/arm64 image on
# the same Amazon Linux 2023 Corretto base and needs no daemon.
# Force one with BUILDER=docker or BUILDER=jib.
#
# Usage: ./scripts/build-and-push.sh [environment] [region]
set -euo pipefail

ENVIRONMENT="${1:-dev}"
REGION="${2:-${AWS_REGION:-us-east-1}}"
APP_NAME="autonomous-agent"
REPO="${APP_NAME}-${ENVIRONMENT}"
BASE_IMAGE="public.ecr.aws/docker/library/amazoncorretto:21-al2023-headless"
JIB_VERSION="3.5.2"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
ECR_URI="${REGISTRY}/${REPO}"

BUILDER="${BUILDER:-}"
if [ -z "$BUILDER" ]; then
  if docker buildx version >/dev/null 2>&1; then BUILDER=docker; else BUILDER=jib; fi
fi

# Only the timestamped tag is pushed. The ECR repository is IMMUTABLE, so a
# moving tag such as :latest would fail on every push after the first one.
echo "==> Building and pushing linux/arm64 image with ${BUILDER}: ${ECR_URI}:${IMAGE_TAG}"
if [ "$BUILDER" = "docker" ]; then
  mvn -q clean package -DskipTests
  aws ecr get-login-password --region "$REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"
  docker buildx build --platform linux/arm64 -t "${ECR_URI}:${IMAGE_TAG}" --push .
else
  # The ECR token is short-lived (12 hours) and scoped to this registry.
  mvn -q -B -DskipTests compile "com.google.cloud.tools:jib-maven-plugin:${JIB_VERSION}:build" \
    -Djib.from.image="$BASE_IMAGE" \
    -Djib.from.platforms=linux/arm64 \
    -Djib.to.image="${ECR_URI}:${IMAGE_TAG}" \
    -Djib.to.auth.username=AWS \
    -Djib.to.auth.password="$(aws ecr get-login-password --region "$REGION")" \
    -Djib.container.ports=8080 \
    -Djib.container.user=1000 \
    -Djib.container.environment=PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    -Djib.container.creationTime=USE_CURRENT_TIMESTAMP
fi

echo "$IMAGE_TAG" > terraform/image-version.txt
echo "==> Pushed. Image tag recorded in terraform/image-version.txt"
