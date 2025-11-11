#!/bin/bash

# Start Gripday Platform - Local Development
# This script starts the entire platform with all services

set -e

echo "🚀 Starting Gripday Microservices Platform..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Copying from .env.example..."
    cp .env.example .env
    echo "📝 Please update .env file with your configuration before running again."
    exit 1
fi

# Build and start services
echo "🔨 Building Docker images..."
docker-compose build --parallel

echo "🏗️  Starting infrastructure services (PostgreSQL, Redis)..."
docker-compose up -d postgres-auth redis

echo "⏳ Waiting for infrastructure services to be healthy..."
docker-compose exec postgres-auth pg_isready -U gripday_user -d gripday_auth
docker-compose exec redis redis-cli ping

echo "🚀 Starting application services..."
docker-compose up -d auth-service gateway-service

echo "📊 Starting observability stack..."
docker-compose up -d prometheus grafana loki promtail

echo "✅ Platform started successfully!"
echo ""
echo "🌐 Service URLs:"
echo "   Gateway Service:  http://localhost:8080"
echo "   Auth Service:     http://localhost:8080"
echo "   Prometheus:       http://localhost:9090"
echo "   Grafana:          http://localhost:3000 (admin/admin)"
echo ""
echo "📋 To view logs: docker-compose logs -f [service-name]"
echo "🛑 To stop: docker-compose down"
echo "🧹 To clean up: docker-compose down -v --remove-orphans"