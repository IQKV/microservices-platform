#!/bin/bash
set -e

echo "🚀 Starting platform..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }

[ -f .env ] || { cp .env.example .env; echo "📝 Update .env and run again"; exit 1; }

docker compose build --parallel
docker compose up -d postgres-iam postgres-billing rabbitmq redis
docker compose up -d iam-service
docker compose up -d billing-service gateway-service
docker compose up -d prometheus grafana loki promtail

echo "✅ Platform started"
echo "   Gateway:    http://localhost:80"
echo "   Traefik:    http://localhost:8888/dashboard/"
echo "   RabbitMQ:   http://rabbitmq.localhost"
echo "   Grafana:    http://grafana.localhost"
echo "   Prometheus: http://prometheus.localhost"
echo "   MailHog:    http://mailhog.localhost"
