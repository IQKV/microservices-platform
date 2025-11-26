#!/bin/bash
set -e

BUILD_IMAGES=false
WAIT_FOR_READY=true

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
ok() { echo "$(tput setaf 2)[OK]$(tput sgr0) $1"; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build) BUILD_IMAGES=true; shift ;;
        -n|--no-wait) WAIT_FOR_READY=false; shift ;;
        -h|--help) echo "Usage: $0 [-b|--build] [-n|--no-wait]"; exit 0 ;;
        *) err "Unknown option: $1" ;;
    esac
done

command -v kubectl > /dev/null || err "kubectl not found"
command -v minikube > /dev/null || err "minikube not found"
minikube status > /dev/null 2>&1 || err "minikube not running. Start with: minikube start"

MINIKUBE_IP=$(minikube ip)
log "Minikube IP: $MINIKUBE_IP"

if [ "$BUILD_IMAGES" = true ]; then
    log "Building Docker images..."
    PROJECT_ROOT="$(cd ../.. && pwd)"
    [ -f "$PROJECT_ROOT/pom.xml" ] || err "Cannot find project root"
    
    eval $(minikube docker-env)
    cd "$PROJECT_ROOT"
    
    for svc in user gateway bookstore; do
        log "Building ${svc} service..."
        docker build -t iqscaffold/${svc}-service:latest -f iqscaffold-${svc}-service/Dockerfile .
    done
    
    cd - > /dev/null
    ok "Images built"
fi

log "Deploying to minikube..."
kubectl apply -f all-in-one.yaml
ok "Manifests applied"

if minikube addons list | grep -q "ingress.*enabled"; then
    log "Applying ingress..."
    kubectl apply -f ingress.yaml
    ok "Ingress applied"
    log "Add to /etc/hosts: $MINIKUBE_IP api.iqscaffold.site user.iqscaffold.site bookstore.iqscaffold.site"
else
    warn "Ingress addon not enabled. Enable with: minikube addons enable ingress"
fi

if [ "$WAIT_FOR_READY" = true ]; then
    log "Waiting for pods..."
    kubectl wait --for=condition=ready pod -l app=postgres-user -n iqscaffold-dev-env --timeout=300s || warn "Postgres user not ready"
    kubectl wait --for=condition=ready pod -l app=postgres-bookstore -n iqscaffold-dev-env --timeout=300s || warn "Postgres bookstore not ready"
    kubectl wait --for=condition=ready pod -l app=redis -n iqscaffold-dev-env --timeout=300s || warn "Redis not ready"
    kubectl wait --for=condition=ready pod -l app=user-service -n iqscaffold-dev-env --timeout=300s || warn "User service not ready"
    kubectl wait --for=condition=ready pod -l app=bookstore-service -n iqscaffold-dev-env --timeout=300s || warn "Bookstore service not ready"
    kubectl wait --for=condition=ready pod -l app=gateway-service -n iqscaffold-dev-env --timeout=300s || warn "Gateway service not ready"
    ok "All pods ready"
fi

echo ""
kubectl get pods -n iqscaffold-dev-env
echo ""

log "Service URLs:"
echo "  Gateway:   http://${MINIKUBE_IP}:30080"
echo "  User:      http://${MINIKUBE_IP}:30081"
echo "  Bookstore: http://${MINIKUBE_IP}:30082"
echo ""

if minikube addons list | grep -q "ingress.*enabled"; then
    echo "  API Gateway: http://api.iqscaffold.site"
    echo "  User:        http://user.iqscaffold.site"
    echo "  Bookstore:   http://bookstore.iqscaffold.site"
    echo ""
fi

ok "Deployment complete"
log "View resources: kubectl get all -n iqscaffold-dev-env"
log "Cleanup: ./cleanup-minikube.sh"
