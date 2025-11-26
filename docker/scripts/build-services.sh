#!/bin/bash
set -e

echo "🔨 Building services..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

./mvnw clean package -DskipTests

docker build -f iqscaffold-user-service/Dockerfile -t iqscaffold/iqscaffold-user-service:latest .
docker build -f iqscaffold-gateway-service/Dockerfile -t iqscaffold/iqscaffold-gateway-service:latest .

echo "✅ Build complete"
