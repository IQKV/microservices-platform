#!/bin/bash

# Validate Docker Configuration
# This script validates the Docker setup without starting services

set -e

echo "🔍 Validating Gripday Platform Docker Configuration..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed or not in PATH."
    exit 1
fi

# Validate main docker-compose.yml
echo "📋 Validating main docker-compose.yml..."
if docker-compose config > /dev/null 2>&1; then
    echo "✅ Main docker-compose.yml is valid"
else
    echo "❌ Main docker-compose.yml has errors"
    docker-compose config
    exit 1
fi

# Validate staging configuration
echo "📋 Validating staging configuration..."
if docker-compose -f docker-compose.yml -f docker-compose.staging.yml config > /dev/null 2>&1; then
    echo "✅ Staging configuration is valid"
else
    echo "❌ Staging configuration has errors"
    exit 1
fi

# Validate production configuration
echo "📋 Validating production configuration..."
if docker-compose -f docker-compose.yml -f docker-compose.production.yml config > /dev/null 2>&1; then
    echo "✅ Production configuration is valid"
else
    echo "❌ Production configuration has errors"
    exit 1
fi

# Validate service-specific configurations
echo "📋 Validating user service configuration..."
if docker-compose -f gripday-user-service/docker-compose.yml config > /dev/null 2>&1; then
    echo "✅ User service configuration is valid"
else
    echo "❌ User service configuration has errors"
    exit 1
fi

echo "📋 Validating gateway service configuration..."
if docker-compose -f gripday-gateway-service/docker-compose.yml config > /dev/null 2>&1; then
    echo "✅ Gateway service configuration is valid"
else
    echo "❌ Gateway service configuration has errors"
    exit 1
fi

# Check if required files exist
echo "📁 Checking required configuration files..."
required_files=(
    "docker/postgres/init-user.sql"
    "docker/prometheus/prometheus.yml"
    "docker/loki/loki-config.yml"
    "docker/promtail/promtail-config.yml"
    "docker/grafana/provisioning/datasources/datasources.yml"
    ".env.example"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file is missing"
        exit 1
    fi
done

# Check Dockerfiles
echo "🐳 Checking Dockerfiles..."
dockerfiles=(
    "gripday-user-service/Dockerfile"
    "gripday-gateway-service/Dockerfile"
)

for dockerfile in "${dockerfiles[@]}"; do
    if [ -f "$dockerfile" ]; then
        echo "✅ $dockerfile exists"
    else
        echo "❌ $dockerfile is missing"
        exit 1
    fi
done

echo ""
echo "🎉 All Docker configurations are valid!"
echo ""
echo "📝 Next steps:"
echo "   1. Copy .env.example to .env and update values"
echo "   2. Run: docker-compose up -d --build"
echo "   3. Check health: docker-compose ps"