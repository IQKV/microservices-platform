#!/bin/bash
set -e

echo "🔨 Building services..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

./mvnw clean package -DskipTests

docker build -f gripday-user-service/Dockerfile -t gripday/user-service:latest .
docker build -f gripday-gateway-service/Dockerfile -t gripday/gateway-service:latest .

echo "✅ Build complete"