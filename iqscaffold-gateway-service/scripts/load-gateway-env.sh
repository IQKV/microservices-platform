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

REQUIRED="SPRING_PROFILES_ACTIVE IQSCAFFOLD_CACHE_REDIS_HOST IQSCAFFOLD_GATEWAY_ROUTING_USER_SERVICE_URI IQSCAFFOLD_GATEWAY_SECURITY_JWT_SECRET"
for var in $REQUIRED; do
    [ -z "${!var:-}" ] && err "Missing required variable: $var"
done

[[ "$IQSCAFFOLD_GATEWAY_ROUTING_USER_SERVICE_URI" =~ ^https?:// ]] || err "Invalid user service URL"

[ "$ENV" = "production" ] && [ ${#IQSCAFFOLD_GATEWAY_SECURITY_JWT_SECRET} -lt 32 ] && err "JWT secret too short for production"

log "Environment loaded: $ENV (profile: $SPRING_PROFILES_ACTIVE)"