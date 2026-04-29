#!/bin/bash
set -e

echo "🔨 Building services..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

./mvnw clean package -DskipTests

docker build -f foundation-iam-service/Dockerfile -t foundation/foundation-iam-service:latest .
docker build -f foundation-gateway-service/Dockerfile -t foundation/foundation-gateway-service:latest .

echo "✅ Build complete"
