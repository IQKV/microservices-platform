#!/bin/bash

# Build Gripday Services
# This script builds Docker images for all services

set -e

echo "🔨 Building Gripday Microservices..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Build Maven projects first
echo "📦 Building Maven projects..."
./mvnw clean package -DskipTests

# Build Docker images
echo "🐳 Building Docker images..."

echo "  📦 Building Auth Service..."
docker build -f gripday-auth-service/Dockerfile -t gripday/auth-service:latest .

echo "  📦 Building Gateway Service..."
docker build -f gripday-gateway-service/Dockerfile -t gripday/gateway-service:latest .

echo "✅ All services built successfully!"
echo ""
echo "🏷️  Built images:"
echo "   gripday/auth-service:latest"
echo "   gripday/gateway-service:latest"