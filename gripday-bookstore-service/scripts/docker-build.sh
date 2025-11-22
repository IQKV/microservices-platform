#!/bin/bash
set -e

ENV=${1:-local}
TAG=${2:-latest}
IMAGE="gripday/bookstore-service"

echo "🔨 Building $IMAGE:$TAG (env: $ENV)"

docker build --build-arg ENVIRONMENT=$ENV -t $IMAGE:$TAG -t $IMAGE:$ENV-$TAG .

command -v trivy > /dev/null 2>&1 && trivy image $IMAGE:$TAG || true

echo "✅ Built: $IMAGE:$TAG, $IMAGE:$ENV-$TAG"