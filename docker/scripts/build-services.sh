#!/bin/bash
set -e

echo "🔨 Building services..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

./mvnw clean package -DskipTests

docker build -f foundation-iam-service/Dockerfile     -t iqkv/foundation-iam-service:local     foundation-iam-service/
docker build -f foundation-billing-service/Dockerfile -t iqkv/foundation-billing-service:local foundation-billing-service/
docker build -f foundation-gateway-service/Dockerfile -t iqkv/foundation-gateway-service:local foundation-gateway-service/

echo "✅ Build complete"
