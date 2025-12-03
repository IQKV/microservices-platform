#!/bin/bash
set -e

ENV="${1:-local}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$ENV" =~ ^(local|staging|test|production|all)$ ]] || err "Invalid environment: $ENV"

command -v kubectl > /dev/null || err "kubectl not found"
kubectl cluster-info > /dev/null 2>&1 || err "Cannot connect to cluster"

setup_namespaces() {
    local env=$1
    log "Setting up namespaces for $env..."
    
    kubectl apply -f user-service/namespace.yaml
    kubectl apply -f gateway-service/namespace.yaml
    kubectl apply -f priority-classes.yaml
}

case "$ENV" in
    local|staging|test|production) setup_namespaces "$ENV" ;;
    all) for e in local staging test production; do setup_namespaces "$e"; done ;;
esac

log "Namespace setup complete"
