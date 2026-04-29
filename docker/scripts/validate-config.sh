#!/bin/bash
set -e

echo "🔍 Validating configuration..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }
command -v docker > /dev/null || { echo "❌ docker not found"; exit 1; }

docker compose config > /dev/null 2>&1 && echo "✅ compose.yaml valid" || { echo "❌ compose.yaml invalid"; exit 1; }

for file in \
    docker/postgres/init-iam.sql \
    docker/postgres/init-billing.sql \
    docker/prometheus/prometheus.yml \
    docker/loki/loki-config.yml \
    docker/promtail/promtail-config.yml \
    docker/rabbitmq/rabbitmq.conf \
    docker/rabbitmq/definitions.json \
    docker/traefik/traefik.yml \
    .env.example \
    foundation-iam-service/Dockerfile \
    foundation-billing-service/Dockerfile \
    foundation-gateway-service/Dockerfile; do
    [ -f "$file" ] && echo "✅ $file" || { echo "❌ Missing: $file"; exit 1; }
done

echo "✅ All configurations valid"
