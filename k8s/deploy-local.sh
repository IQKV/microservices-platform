#!/bin/bash
set -e

SKIP_BUILD="${1:-false}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

command -v kubectl > /dev/null || err "kubectl not found"
kubectl cluster-info > /dev/null 2>&1 || err "Cannot connect to cluster"

log "Deploying to local cluster..."

# Setup namespaces
kubectl apply -f user-service/namespace.yaml
kubectl apply -f gateway-service/namespace.yaml

# Apply priority classes
kubectl apply -f priority-classes.yaml

# Deploy services
log "Deploying services..."
kubectl apply -f user-service/
kubectl apply -f gateway-service/

# Wait for services to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=iqscaffold-user-service -n iqscaffold-dev-env --timeout=300s
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=iqscaffold-gateway-service -n iqscaffold-dev-env --timeout=300s

log "Local deployment complete"
log "Gateway: http://api.iqscaffold.local"
