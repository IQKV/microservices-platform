#!/bin/bash
set -e

echo "🚀 Starting platform..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

[ -f .env ] || { cp .env.example .env; echo "📝 Update .env and run again"; exit 1; }

docker-compose build --parallel
docker-compose up -d postgres-iam redis
docker-compose up -d iam-service gateway-service
docker-compose up -d prometheus grafana loki promtail

echo "✅ Platform started"
echo "   Gateway:    http://localhost:8080"
echo "   Prometheus: http://localhost:9090"
echo "   Grafana:    http://localhost:3000 (admin/admin)"