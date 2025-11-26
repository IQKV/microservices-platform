#!/bin/bash
set -e

echo "🔍 Validating configuration..."

docker info > /dev/null 2>&1 || { echo "❌ Docker not running"; exit 1; }
command -v docker-compose > /dev/null || { echo "❌ docker-compose not found"; exit 1; }

validate() {
    docker-compose "$@" config > /dev/null 2>&1 && echo "✅ $1" || { echo "❌ $1 invalid"; exit 1; }
}

validate "main"
validate "-f docker-compose.yml -f docker-compose.staging.yml" "staging"
validate "-f docker-compose.yml -f docker-compose.production.yml" "production"
validate "-f iqscaffold-user-service/docker-compose.yml" "user-service"
validate "-f iqscaffold-gateway-service/docker-compose.yml" "gateway-service"

for file in docker/postgres/init-user.sql docker/prometheus/prometheus.yml \
    docker/loki/loki-config.yml docker/promtail/promtail-config.yml \
    docker/grafana/provisioning/datasources/datasources.yml .env.example \
    iqscaffold-user-service/Dockerfile iqscaffold-gateway-service/Dockerfile; do
    [ -f "$file" ] || { echo "❌ Missing: $file"; exit 1; }
done

echo "✅ All configurations valid"