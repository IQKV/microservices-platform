#!/bin/bash

# Docker build script for Bookstore Service
# Usage: ./scripts/docker-build.sh [environment] [tag]

set -e

ENVIRONMENT=${1:-local}
TAG=${2:-latest}
IMAGE_NAME="gripday/bookstore-service"

echo "Building Docker image for environment: $ENVIRONMENT"
echo "Image name: $IMAGE_NAME:$TAG"

# Build the Docker image
docker build \
  --build-arg ENVIRONMENT=$ENVIRONMENT \
  --tag $IMAGE_NAME:$TAG \
  --tag $IMAGE_NAME:$ENVIRONMENT-$TAG \
  .

echo "Docker image built successfully!"
echo "Image: $IMAGE_NAME:$TAG"
echo "Tagged as: $IMAGE_NAME:$ENVIRONMENT-$TAG"

# Optional: Run security scan if trivy is available
if command -v trivy &> /dev/null; then
    echo "Running security scan..."
    trivy image $IMAGE_NAME:$TAG
fi

echo "Build completed!"