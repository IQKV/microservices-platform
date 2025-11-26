#!/bin/bash
set -e

ENV="${1:-local}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env.$ENV"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$ENV" =~ ^(local|staging|production)$ ]] || err "Invalid environment: $ENV"
[ -f "$ENV_FILE" ] || err "Environment file not found: $ENV_FILE"

log "Loading environment: $ENV"

set -a
source "$ENV_FILE"
set +a

REQUIRED="SPRING_PROFILES_ACTIVE IQSCAFFOLD_DATABASE_URL IQSCAFFOLD_DATABASE_USERNAME IQSCAFFOLD_DATABASE_PASSWORD IQSCAFFOLD_CACHE_REDIS_HOST IQSCAFFOLD_AUTH_JWT_SECRET"
for var in $REQUIRED; do
    [ -z "${!var:-}" ] && err "Missing required variable: $var"
done

[[ "$IQSCAFFOLD_DATABASE_URL" =~ ^jdbc:postgresql:// ]] || err "Invalid database URL"

[ "$ENV" = "production" ] && [ ${#IQSCAFFOLD_AUTH_JWT_SECRET} -lt 32 ] && err "JWT secret too short for production"

log "Environment loaded: $ENV (profile: $SPRING_PROFILES_ACTIVE)"
