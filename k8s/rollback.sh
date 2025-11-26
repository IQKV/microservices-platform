#!/bin/bash
set -e

ENV="${1:-local}"
SERVICE="${2:-all}"
REVISION="${3:-}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }

[[ "$ENV" =~ ^(local|staging|test|production)$ ]] || err "Invalid environment: $ENV"

command -v kubectl > /dev/null || err "kubectl not found"

NS="iqscaffold-dev-env"
[ "$ENV" = "staging" ] && NS="iqscaffold-staging-env"
[ "$ENV" = "test" ] && NS="iqscaffold-test-env"
[ "$ENV" = "production" ] && NS="iqscaffold-production-env"

if [ "$ENV" = "production" ]; then
    warn "⚠️  PRODUCTION ROLLBACK WARNING ⚠️"
    read -p "Type 'ROLLBACK' to confirm: " -r
    [[ "$REPLY" == "ROLLBACK" ]] || err "Rollback cancelled"
fi

rollback_service() {
    local svc=$1
    local ns=$2
    
    log "Rolling back $svc..."
    
    if [ -n "$REVISION" ]; then
        kubectl rollout undo deployment/${svc}-service -n $ns --to-revision=$REVISION
    else
        kubectl rollout undo deployment/${svc}-service -n $ns
    fi
    
    kubectl rollout status deployment/${svc}-service -n $ns --timeout=300s
}

case "$SERVICE" in
    user|gateway|bookstore) rollback_service "$SERVICE" "$NS" ;;
    all)
        for svc in user gateway bookstore; do
            rollback_service "$svc" "$NS"
        done
        ;;
esac

log "Rollback complete"
