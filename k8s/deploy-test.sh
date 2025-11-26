#!/bin/bash
set -e

TAG="${1:-test}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }

command -v kubectl > /dev/null || err "kubectl not found"
kubectl cluster-info > /dev/null 2>&1 || err "Cannot connect to cluster"

CONTEXT=$(kubectl config current-context)
[[ "$CONTEXT" =~ test ]] || { warn "Context doesn't appear to be test: $CONTEXT"; read -p "Continue? (y/N): " -n 1 -r; echo; [[ $REPLY =~ ^[Yy]$ ]] || exit 1; }

log "Deploying to test environment (tag: $TAG)..."

# Update image tags
for svc in user gateway bookstore; do
    sed "s|iqscaffold/${svc}-service:latest|iqscaffold/${svc}-service:$TAG|g" \
        ${svc}-service/${svc}-service-deployment.yaml | \
    sed 's/namespace: iqscaffold-dev-env/namespace: iqscaffold-test-env/g' | \
    kubectl apply -f -
done

# Apply other resources
kubectl apply -f user-service/namespace.yaml
kubectl apply -f gateway-service/namespace.yaml
kubectl apply -f bookstore-service/namespace.yaml
kubectl apply -f priority-classes.yaml

# Wait for rollout
for svc in user gateway bookstore; do
    kubectl rollout status deployment/${svc}-service -n iqscaffold-test-env --timeout=600s
done

log "Test deployment complete"
log "Gateway: https://api-test.iqscaffold.com"
