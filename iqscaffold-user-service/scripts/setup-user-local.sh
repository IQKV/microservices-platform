#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env.local"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

log "Setting up local environment..."

for cmd in docker docker-compose mvn; do
    command -v $cmd > /dev/null || err "$cmd not found"
done

[ -f "$ENV_FILE" ] || { cp "$PROJECT_DIR/.env.example" "$ENV_FILE" && log "Created $ENV_FILE"; }

cd "$PROJECT_DIR"
docker-compose up -d postgres redis

log "Waiting for services..."
for i in {1..30}; do
    docker-compose exec -T postgres pg_isready -U iqscaffold_user -d iqscaffold_user_local > /dev/null 2>&1 && \
    docker-compose exec -T redis redis-cli ping > /dev/null 2>&1 && break
    sleep 2
done

mvn clean compile
mvn liquibase:update -Dspring.profiles.active=local

log "Setup complete. Start service: mvn spring-boot:run -Dspring.profiles.active=local"
