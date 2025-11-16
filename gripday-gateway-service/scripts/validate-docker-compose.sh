#!/bin/bash
set -e

SERVICE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

command -v docker-compose > /dev/null || err "docker-compose not found"

log "Validating configurations..."

validate() {
    docker-compose -f "$SERVICE_DIR/$1" config > /dev/null 2>&1 && log "✓ $2" || { err "✗ $2 invalid"; }
}

validate "docker-compose.yml" "local"
validate "docker-compose.staging.yml" "staging"
validate "docker-compose.production.yml" "production"

for file in Dockerfile pom.xml .dockerignore .env.local .env.staging .env.production; do
    [ -f "$SERVICE_DIR/$file" ] || err "Missing: $file"
done

log "All configurations valid"
