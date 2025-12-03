#!/bin/bash
set -e

TAG="${1:-latest}"
SKIP_CONFIRM="${2:-false}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }

command -v kubectl > /dev/null || err "kubectl not found"
kubectl cluster-info > /dev/null 2>&1 || err "Cannot connect to cluster"

CONTEXT=$(kubectl config current-context)
[[ "$CONTEXT" =~ production|prod ]] || err "Context doesn't appear to be production: $CONTEXT"

if [ "$SKIP_CONFIRM" != "true" ]; then
    warn "⚠️  PRODUCTION DEPLOYMENT WARNING ⚠️"
    warn "Tag: $TAG | Cluster: $CONTEXT"
    read -p "Type 'DEPLOY' to confirm: " -r
    [[ "$REPLY" == "DEPLOY" ]] || err "Deployment cancelled"
fi

log "Deploying to production (tag: $TAG)..."

# Backup databases
log "Creating database backups..."
for svc in user; do
    kubectl exec -n iqscaffold-production-env deployment/${svc}-postgres -- \
        pg_dump -U iqscaffold_${svc}_prod iqscaffold_${svc}_production > /tmp/${svc}_backup_$(date +%Y%m%d_%H%M%S).sql || warn "Backup failed for $svc"
done

# Update image tags
for svc in user gateway; do
    sed "s|iqscaffold/${svc}-service:latest|iqscaffold/${svc}-service:$TAG|g" \
        ${svc}-service/${svc}-service-deployment.yaml | \
    sed 's/namespace: iqscaffold-dev-env/namespace: iqscaffold-production-env/g' | \
    sed 's/replicas: 2/replicas: 3/g' | \
    kubectl apply -f -
done

# Apply resources
kubectl apply -f priority-classes.yaml

# Wait for rollout
for svc in user gateway; do
    kubectl rollout status deployment/${svc}-service -n iqscaffold-production-env --timeout=600s
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=iqscaffold-${svc}-service -n iqscaffold-production-env --timeout=300s
done

log "Production deployment complete"
log "Gateway: https://api.iqscaffold.com"
