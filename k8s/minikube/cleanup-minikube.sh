#!/bin/bash
set -e

FORCE=false

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
ok() { echo "$(tput setaf 2)[OK]$(tput sgr0) $1"; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force) FORCE=true; shift ;;
        -h|--help) echo "Usage: $0 [-f|--force]"; exit 0 ;;
        *) err "Unknown option: $1" ;;
    esac
done

command -v kubectl > /dev/null || err "kubectl not found"
command -v minikube > /dev/null || err "minikube not found"
minikube status > /dev/null 2>&1 || { warn "minikube not running"; exit 0; }

kubectl get namespace iqscaffold-dev-env > /dev/null 2>&1 || { log "Namespace doesn't exist. Nothing to clean."; exit 0; }

log "Resources to delete:"
kubectl get all -n iqscaffold-dev-env 2>/dev/null || echo "  None"
echo ""

if [ "$FORCE" != true ]; then
    read -p "Delete all resources in iqscaffold-dev-env? (yes/no): " -r
    [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]] || { log "Cancelled"; exit 0; }
fi

log "Deleting namespace iqscaffold-dev-env..."
kubectl delete namespace iqscaffold-dev-env --timeout=60s

log "Waiting for termination..."
timeout=60
counter=0
while kubectl get namespace iqscaffold-dev-env > /dev/null 2>&1; do
    [ $counter -ge $timeout ] && { warn "Still terminating (normal)"; break; }
    sleep 2
    counter=$((counter + 2))
done

ok "Cleanup complete"
log "Redeploy with: ./deploy-minikube.sh"
