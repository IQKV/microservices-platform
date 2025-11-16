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
for svc in user bookstore; do
    kubectl exec -n gripday-production-env deployment/${svc}-postgres -- \
        pg_dump -U gripday_${svc}_prod gripday_${svc}_production > /tmp/${svc}_backup_$(date +%Y%m%d_%H%M%S).sql || warn "Backup failed for $svc"
done

# Update image tags
for svc in user gateway bookstore; do
    sed "s|gripday/${svc}-service:latest|gripday/${svc}-service:$TAG|g" \
        ${svc}-service/${svc}-service-deployment.yaml | \
    sed 's/namespace: gripday-dev-env/namespace: gripday-production-env/g' | \
    sed 's/replicas: 2/replicas: 3/g' | \
    kubectl apply -f -
done

# Apply resources
kubectl apply -f priority-classes.yaml

# Wait for rollout
for svc in user gateway bookstore; do
    kubectl rollout status deployment/${svc}-service -n gripday-production-env --timeout=600s
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-${svc}-service -n gripday-production-env --timeout=300s
done

log "Production deployment complete"
log "Gateway: https://api.gripday.com"
