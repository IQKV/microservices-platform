#!/bin/bash
set -e

ENV="${1:-local}"
ACTION="${2:-apply}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$ENV" =~ ^(local|staging|test|production|all)$ ]] || err "Invalid environment: $ENV"
[[ "$ACTION" =~ ^(apply|delete)$ ]] || err "Invalid action: $ACTION"

command -v kubectl > /dev/null || err "kubectl not found"
kubectl cluster-info > /dev/null 2>&1 || err "Cannot connect to cluster"

apply_configs() {
    local env=$1
    log "Applying configs for $env..."
    
    for svc in user-service gateway-service; do
        kubectl $ACTION -f $svc/configmap.yaml
        kubectl $ACTION -f $svc/secret.yaml
    done
}

case "$ENV" in
    local|staging|test|production) apply_configs "$ENV" ;;
    all) for e in local staging test production; do apply_configs "$e"; done ;;
esac

log "Config management complete"
