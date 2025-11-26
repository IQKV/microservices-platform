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
docker-compose up -d redis

log "Waiting for Redis..."
for i in {1..30}; do
    docker-compose exec -T redis redis-cli ping > /dev/null 2>&1 && break
    sleep 2
done

mvn clean compile

log "Setup complete. Start gateway: mvn spring-boot:run -Dspring.profiles.active=local"
