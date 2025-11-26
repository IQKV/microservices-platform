#!/bin/bash
set -e

ENV="${1:-local}"
FORCE="${2:-false}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$ENV" =~ ^(local|staging|production)$ ]] || err "Invalid environment: $ENV"
[[ "$FORCE" =~ ^(true|false)$ ]] || err "Invalid FORCE: $FORCE"

ENV_FILE="$PROJECT_DIR/.env.$ENV"
[ -f "$ENV_FILE" ] || err "Environment file not found: $ENV_FILE"

log "Setting up database for $ENV environment..."

set -a
source "$ENV_FILE"
set +a

command -v mvn > /dev/null || err "Maven not found"

if [ "$ENV" = "local" ]; then
    command -v docker > /dev/null || err "Docker not found"
    cd "$PROJECT_DIR"
    docker-compose up -d postgres
    
    log "Waiting for PostgreSQL..."
    for i in {1..30}; do
        docker-compose exec -T postgres pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1 && break
        sleep 2
    done
fi

cd "$PROJECT_DIR"
mvn liquibase:update \
    -Dspring.profiles.active=$ENV \
    -Dliquibase.url=$IQSCAFFOLD_DATABASE_URL \
    -Dliquibase.username=$IQSCAFFOLD_DATABASE_USERNAME \
    -Dliquibase.password=$IQSCAFFOLD_DATABASE_PASSWORD

log "Database setup complete for $ENV"
