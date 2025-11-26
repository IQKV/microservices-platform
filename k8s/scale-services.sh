#!/bin/bash
set -e

ENV="${1:-local}"
SERVICE="${2:-all}"
REPLICAS="${3:-}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$ENV" =~ ^(local|staging|test|production)$ ]] || err "Invalid environment: $ENV"
[ -n "$REPLICAS" ] || err "Replicas count required"

command -v kubectl > /dev/null || err "kubectl not found"

NS="iqscaffold-dev-env"
[ "$ENV" = "staging" ] && NS="iqscaffold-staging-env"
[ "$ENV" = "test" ] && NS="iqscaffold-test-env"
[ "$ENV" = "production" ] && NS="iqscaffold-production-env"

scale_service() {
    local svc=$1
    local ns=$2
    local replicas=$3
    
    log "Scaling $svc to $replicas replicas..."
    kubectl scale deployment/${svc}-service --replicas=$replicas -n $ns
    kubectl rollout status deployment/${svc}-service -n $ns --timeout=300s
}

case "$SERVICE" in
    user|gateway|bookstore) scale_service "$SERVICE" "$NS" "$REPLICAS" ;;
    all)
        for svc in user gateway bookstore; do
            scale_service "$svc" "$NS" "$REPLICAS"
        done
        ;;
esac

log "Scaling complete"
