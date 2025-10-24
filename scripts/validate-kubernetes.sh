#!/bin/bash

# Kubernetes Platform Validation Script
# Validates the platform when deployed to Kubernetes

set -euo pipefail

# Configuration
NAMESPACE="${NAMESPACE:-gripday}"
VALIDATION_TIMEOUT="${VALIDATION_TIMEOUT:-600}"
KUBECTL_CMD="${KUBECTL_CMD:-kubectl}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} $message"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
        "WARN")
            echo -e "${YELLOW}[WARN]${NC} $message"
            ;;
    esac
}

# Check if kubectl is available and cluster is accessible
check_kubernetes() {
    if ! command -v "$KUBECTL_CMD" &> /dev/null; then
        log "ERROR" "kubectl not found. Please install kubectl."
        return 1
    fi
    
    if ! $KUBECTL_CMD cluster-info &> /dev/null; then
        log "ERROR" "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        return 1
    fi
    
    log "INFO" "Kubernetes cluster is accessible"
    
    # Check if namespace exists
    if ! $KUBECTL_CMD get namespace "$NAMESPACE" &> /dev/null; then
        log "ERROR" "Namespace '$NAMESPACE' does not exist"
        return 1
    fi
    
    log "INFO" "Using namespace: $NAMESPACE"
}

# Wait for deployments to be ready
wait_for_deployments() {
    log "INFO" "Waiting for deployments to be ready..."
    
    local deployments=("auth-service" "gateway-service" "auth-postgres" "auth-redis" "gateway-redis")
    local max_attempts=$((VALIDATION_TIMEOUT / 10))
    
    for deployment in "${deployments[@]}"; do
        log "INFO" "Waiting for deployment: $deployment"
        
        if ! $KUBECTL_CMD wait --for=condition=available \
            --timeout="${VALIDATION_TIMEOUT}s" \
            deployment/"$deployment" \
            -n "$NAMESPACE"; then
            log "ERROR" "Deployment $deployment failed to become ready"
            return 1
        fi
        
        log "SUCCESS" "Deployment $deployment is ready"
    done
    
    log "SUCCESS" "All deployments are ready"
}

# Wait for pods to be ready
wait_for_pods() {
    log "INFO" "Waiting for pods to be ready..."
    
    if ! $KUBECTL_CMD wait --for=condition=ready \
        --timeout="${VALIDATION_TIMEOUT}s" \
        pods --all \
        -n "$NAMESPACE"; then
        log "ERROR" "Some pods failed to become ready"
        return 1
    fi
    
    log "SUCCESS" "All pods are ready"
}

# Get service URLs
get_service_urls() {
    log "INFO" "Getting service URLs..."
    
    # Try to get LoadBalancer or NodePort URLs
    local gateway_service_type=$($KUBECTL_CMD get service gateway-service -n "$NAMESPACE" -o jsonpath='{.spec.type}')
    local auth_service_type=$($KUBECTL_CMD get service auth-service -n "$NAMESPACE" -o jsonpath='{.spec.type}')
    
    if [ "$gateway_service_type" = "LoadBalancer" ]; then
        GATEWAY_URL=$($KUBECTL_CMD get service gateway-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
        if [ -z "$GATEWAY_URL" ] || [ "$GATEWAY_URL" = "null" ]; then
            GATEWAY_URL=$($KUBECTL_CMD get service gateway-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
        fi
        GATEWAY_URL="http://$GATEWAY_URL:8080"
    elif [ "$gateway_service_type" = "NodePort" ]; then
        local node_ip=$($KUBECTL_CMD get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
        if [ -z "$node_ip" ]; then
            node_ip=$($KUBECTL_CMD get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
        fi
        local node_port=$($KUBECTL_CMD get service gateway-service -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}')
        GATEWAY_URL="http://$node_ip:$node_port"
    else
        # Use port-forward for ClusterIP
        log "INFO" "Using port-forward for gateway service access"
        $KUBECTL_CMD port-forward service/gateway-service 8080:8080 -n "$NAMESPACE" &
        GATEWAY_PORT_FORWARD_PID=$!
        GATEWAY_URL="http://localhost:8080"
        sleep 5
    fi
    
    if [ "$auth_service_type" = "LoadBalancer" ]; then
        AUTH_URL=$($KUBECTL_CMD get service auth-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
        if [ -z "$AUTH_URL" ] || [ "$AUTH_URL" = "null" ]; then
            AUTH_URL=$($KUBECTL_CMD get service auth-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
        fi
        AUTH_URL="http://$AUTH_URL:8081"
    elif [ "$auth_service_type" = "NodePort" ]; then
        local node_ip=$($KUBECTL_CMD get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
        if [ -z "$node_ip" ]; then
            node_ip=$($KUBECTL_CMD get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
        fi
        local node_port=$($KUBECTL_CMD get service auth-service -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}')
        AUTH_URL="http://$node_ip:$node_port"
    else
        # Use port-forward for ClusterIP
        log "INFO" "Using port-forward for auth service access"
        $KUBECTL_CMD port-forward service/auth-service 8081:8081 -n "$NAMESPACE" &
        AUTH_PORT_FORWARD_PID=$!
        AUTH_URL="http://localhost:8081"
        sleep 5
    fi
    
    log "INFO" "Gateway URL: $GATEWAY_URL"
    log "INFO" "Auth URL: $AUTH_URL"
    
    export GATEWAY_URL
    export AUTH_URL
}

# Check pod health and logs
check_pod_health() {
    log "INFO" "Checking pod health..."
    
    # Get pod status
    log "INFO" "Pod status:"
    $KUBECTL_CMD get pods -n "$NAMESPACE" -o wide
    
    # Check for any pods in error state
    local error_pods=$($KUBECTL_CMD get pods -n "$NAMESPACE" --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)
    
    if [ "$error_pods" -gt 0 ]; then
        log "WARN" "Found $error_pods pods not in Running state"
        $KUBECTL_CMD get pods -n "$NAMESPACE" --field-selector=status.phase!=Running
    else
        log "SUCCESS" "All pods are in Running state"
    fi
}

# Run platform validation
run_validation() {
    log "INFO" "Running platform validation..."
    
    # Set default Prometheus and Grafana URLs (may not be available in K8s)
    export PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
    export GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
    
    # Run the main validation script
    if [ -f "scripts/validate-platform.sh" ]; then
        bash scripts/validate-platform.sh
        return $?
    else
        log "ERROR" "Platform validation script not found: scripts/validate-platform.sh"
        return 1
    fi
}

# Show pod logs
show_logs() {
    log "INFO" "Showing recent pod logs..."
    
    echo
    echo "=== Auth Service Logs ==="
    $KUBECTL_CMD logs -l app=auth-service --tail=20 -n "$NAMESPACE"
    
    echo
    echo "=== Gateway Service Logs ==="
    $KUBECTL_CMD logs -l app=gateway-service --tail=20 -n "$NAMESPACE"
    
    echo
    echo "=== Auth PostgreSQL Logs ==="
    $KUBECTL_CMD logs -l app=auth-postgres --tail=10 -n "$NAMESPACE"
    
    echo
    echo "=== Redis Logs ==="
    $KUBECTL_CMD logs -l app=auth-redis --tail=10 -n "$NAMESPACE"
    $KUBECTL_CMD logs -l app=gateway-redis --tail=10 -n "$NAMESPACE"
}

# Cleanup function
cleanup() {
    log "INFO" "Cleaning up port-forwards..."
    
    if [ -n "${GATEWAY_PORT_FORWARD_PID:-}" ]; then
        kill "$GATEWAY_PORT_FORWARD_PID" 2>/dev/null || true
    fi
    
    if [ -n "${AUTH_PORT_FORWARD_PID:-}" ]; then
        kill "$AUTH_PORT_FORWARD_PID" 2>/dev/null || true
    fi
}

# Main function
main() {
    local show_logs_on_failure=true
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            --timeout)
                VALIDATION_TIMEOUT="$2"
                shift 2
                ;;
            --no-logs)
                show_logs_on_failure=false
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --namespace NAME        Kubernetes namespace (default: gripday)"
                echo "  --timeout SECONDS       Validation timeout in seconds (default: 600)"
                echo "  --no-logs               Don't show logs on failure"
                echo "  -h, --help              Show this help message"
                exit 0
                ;;
            *)
                log "ERROR" "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    echo "=== Kubernetes Platform Validation ==="
    echo "Namespace: $NAMESPACE"
    echo "Timeout: ${VALIDATION_TIMEOUT}s"
    echo
    
    # Set up cleanup trap
    trap cleanup EXIT
    
    # Check Kubernetes availability
    check_kubernetes || exit 1
    
    # Wait for deployments and pods
    wait_for_deployments || {
        if [ "$show_logs_on_failure" = true ]; then
            show_logs
        fi
        exit 1
    }
    
    wait_for_pods || {
        if [ "$show_logs_on_failure" = true ]; then
            show_logs
        fi
        exit 1
    }
    
    # Check pod health
    check_pod_health
    
    # Get service URLs
    get_service_urls || exit 1
    
    # Run validation
    if run_validation; then
        log "SUCCESS" "Kubernetes platform validation completed successfully!"
        exit 0
    else
        log "ERROR" "Kubernetes platform validation failed!"
        if [ "$show_logs_on_failure" = true ]; then
            show_logs
        fi
        exit 1
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi