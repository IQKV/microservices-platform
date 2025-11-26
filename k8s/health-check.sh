#!/bin/bash
set -e

ENV="${1:-local}"
SERVICE="${2:-all}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }
ok() { echo "$(tput setaf 2)[OK]$(tput sgr0) $1"; }

[[ "$ENV" =~ ^(local|staging|test|production)$ ]] || err "Invalid environment: $ENV"

command -v kubectl > /dev/null || err "kubectl not found"

NS="iqscaffold-dev-env"
[ "$ENV" = "staging" ] && NS="iqscaffold-staging-env"
[ "$ENV" = "test" ] && NS="iqscaffold-test-env"
[ "$ENV" = "production" ] && NS="iqscaffold-production-env"

check_service() {
    local svc=$1
    local ns=$2
    
    if kubectl get deployment ${svc}-service -n $ns > /dev/null 2>&1; then
        local ready=$(kubectl get deployment ${svc}-service -n $ns -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        local desired=$(kubectl get deployment ${svc}-service -n $ns -o jsonpath='{.spec.replicas}')
        
        if [ "$ready" = "$desired" ]; then
            ok "$svc: $ready/$desired pods ready"
        else
            err "$svc: only $ready/$desired pods ready"
        fi
    else
        err "$svc: deployment not found"
    fi
}

log "Health check for $ENV environment..."

case "$SERVICE" in
    user|gateway|bookstore) check_service "$SERVICE" "$NS" ;;
    all)
        check_service "user" "$NS"
        check_service "gateway" "$NS"
        check_service "bookstore" "$NS"
        ;;
esac

log "Health check complete"
