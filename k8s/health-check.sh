#!/bin/bash

# Gripday Platform - Kubernetes Health Check Script
# This script performs health checks for cluster and services

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="local"
SERVICE="all"
CHECK_TYPE="all"
VERBOSE=false
TIMEOUT=300

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_debug() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Perform health checks for Gripday Platform

OPTIONS:
    -e, --environment ENV    Environment (local|staging|production|all) [default: local]
    -s, --service SERVICE    Service to check (user|gateway|all) [default: all]
    -c, --check CHECK        Check type (cluster|pods|services|endpoints|all) [default: all]
    -t, --timeout SECONDS    Timeout for health checks [default: 300]
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

CHECK TYPES:
    cluster                 Check cluster connectivity and basic health
    pods                    Check pod status and readiness
    services                Check service availability and endpoints
    endpoints               Check service endpoints and connectivity
    all                     Perform all health checks

EXAMPLES:
    $0                                    # Full health check for local environment
    $0 -e production -s gateway          # Check gateway service in production
    $0 -c cluster                        # Check only cluster health
    $0 -e all                           # Check all environments

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -s|--service)
            SERVICE="$2"
            shift 2
            ;;
        -c|--check)
            CHECK_TYPE="$2"
            shift 2
            ;;
        -t|--timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate inputs
if [[ ! "$ENVIRONMENT" =~ ^(local|staging|production|all)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    exit 1
fi

if [[ ! "$SERVICE" =~ ^(user|gateway|all)$ ]]; then
    print_error "Invalid service: $SERVICE"
    exit 1
fi

if [[ ! "$CHECK_TYPE" =~ ^(cluster|pods|services|endpoints|all)$ ]]; then
    print_error "Invalid check type: $CHECK_TYPE"
    exit 1
fi

# Global health check results
HEALTH_RESULTS=()
FAILED_CHECKS=0
TOTAL_CHECKS=0

# Function to record health check result
record_result() {
    local check_name="$1"
    local status="$2"
    local message="$3"
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    if [[ "$status" == "PASS" ]]; then
        HEALTH_RESULTS+=("✓ $check_name: $message")
        print_success "✓ $check_name: $message"
    else
        HEALTH_RESULTS+=("✗ $check_name: $message")
        print_error "✗ $check_name: $message"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

# Function to get namespace suffix
get_namespace_suffix() {
    local env="$1"
    if [[ "$env" == "local" ]]; then
        echo ""
    else
        echo "-$env"
    fi
}

# Function to check cluster health
check_cluster_health() {
    print_status "Checking cluster health..."
    
    # Check kubectl connectivity
    if kubectl cluster-info &>/dev/null; then
        local cluster_context=$(kubectl config current-context)
        record_result "Cluster Connectivity" "PASS" "Connected to $cluster_context"
    else
        record_result "Cluster Connectivity" "FAIL" "Cannot connect to cluster"
        return 1
    fi
    
    # Check cluster nodes
    local node_count=$(kubectl get nodes --no-headers 2>/dev/null | wc -l)
    if [[ $node_count -gt 0 ]]; then
        record_result "Cluster Nodes" "PASS" "$node_count nodes available"
        
        # Check node readiness
        local ready_nodes=$(kubectl get nodes --no-headers 2>/dev/null | grep -c " Ready " || echo "0")
        if [[ $ready_nodes -eq $node_count ]]; then
            record_result "Node Readiness" "PASS" "All $ready_nodes nodes ready"
        else
            record_result "Node Readiness" "FAIL" "Only $ready_nodes/$node_count nodes ready"
        fi
    else
        record_result "Cluster Nodes" "FAIL" "No nodes found"
    fi
    
    # Check cluster resources
    local cpu_usage=$(kubectl top nodes --no-headers 2>/dev/null | awk '{sum+=$3} END {print sum}' || echo "N/A")
    local memory_usage=$(kubectl top nodes --no-headers 2>/dev/null | awk '{sum+=$5} END {print sum}' || echo "N/A")
    
    if [[ "$cpu_usage" != "N/A" ]]; then
        record_result "Cluster Resources" "PASS" "CPU: ${cpu_usage}%, Memory: ${memory_usage}%"
    else
        record_result "Cluster Resources" "WARN" "Metrics server not available"
    fi
}

# Function to check namespace health
check_namespace_health() {
    local env="$1"
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Checking namespace health for $env environment..."
    
    # Check auth namespace
    local auth_namespace="gripday-auth$namespace_suffix"
    if kubectl get namespace "$auth_namespace" &>/dev/null; then
        record_result "Auth Namespace ($env)" "PASS" "Namespace $auth_namespace exists"
    else
        record_result "Auth Namespace ($env)" "FAIL" "Namespace $auth_namespace not found"
    fi
    
    # Check gateway namespace
    local gateway_namespace="gripday-gateway$namespace_suffix"
    if kubectl get namespace "$gateway_namespace" &>/dev/null; then
        record_result "Gateway Namespace ($env)" "PASS" "Namespace $gateway_namespace exists"
    else
        record_result "Gateway Namespace ($env)" "FAIL" "Namespace $gateway_namespace not found"
    fi
}

# Function to check pod health
check_pod_health() {
    local service="$1"
    local env="$2"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Checking pod health for $service in $env environment..."
    
    case "$service" in
        "auth")
            check_service_pods "auth" "gripday-auth$namespace_suffix" "$env"
            ;;
        "gateway")
            check_service_pods "gateway" "gripday-gateway$namespace_suffix" "$env"
            ;;
        "all")
            check_service_pods "auth" "gripday-auth$namespace_suffix" "$env"
            check_service_pods "gateway" "gripday-gateway$namespace_suffix" "$env"
            ;;
    esac
}

# Function to check pods for a specific service
check_service_pods() {
    local service="$1"
    local namespace="$2"
    local env="$3"
    
    # Check if namespace exists
    if ! kubectl get namespace "$namespace" &>/dev/null; then
        record_result "$service Pods ($env)" "FAIL" "Namespace $namespace not found"
        return 1
    fi
    
    # Check main service deployment
    local deployment_name=""
    case "$service" in
        "auth")
            deployment_name="user-service"
            ;;
        "gateway")
            deployment_name="gateway-service"
            ;;
    esac
    
    if kubectl get deployment "$deployment_name" -n "$namespace" &>/dev/null; then
        local desired=$(kubectl get deployment "$deployment_name" -n "$namespace" -o jsonpath='{.spec.replicas}')
        local ready=$(kubectl get deployment "$deployment_name" -n "$namespace" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        
        if [[ "$ready" == "$desired" ]]; then
            record_result "$service Service Pods ($env)" "PASS" "$ready/$desired pods ready"
        else
            record_result "$service Service Pods ($env)" "FAIL" "Only $ready/$desired pods ready"
        fi
        
        # Check pod status details
        local pod_status=$(kubectl get pods -l app.kubernetes.io/name="$deployment_name" -n "$namespace" --no-headers 2>/dev/null)
        if [[ -n "$pod_status" ]]; then
            local running_pods=$(echo "$pod_status" | grep -c "Running" || echo "0")
            local total_pods=$(echo "$pod_status" | wc -l)
            
            if [[ "$running_pods" == "$total_pods" ]]; then
                record_result "$service Pod Status ($env)" "PASS" "All $running_pods pods running"
            else
                record_result "$service Pod Status ($env)" "FAIL" "Only $running_pods/$total_pods pods running"
            fi
        fi
    else
        record_result "$service Service Deployment ($env)" "FAIL" "Deployment $deployment_name not found"
    fi
    
    # Check database pods (for auth service)
    if [[ "$service" == "auth" ]]; then
        # Check PostgreSQL
        if kubectl get deployment "auth-postgres" -n "$namespace" &>/dev/null; then
            local postgres_ready=$(kubectl get deployment "auth-postgres" -n "$namespace" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
            if [[ "$postgres_ready" -gt 0 ]]; then
                record_result "$service PostgreSQL ($env)" "PASS" "PostgreSQL pod ready"
            else
                record_result "$service PostgreSQL ($env)" "FAIL" "PostgreSQL pod not ready"
            fi
        else
            record_result "$service PostgreSQL ($env)" "FAIL" "PostgreSQL deployment not found"
        fi
        
        # Check Redis
        if kubectl get deployment "auth-redis" -n "$namespace" &>/dev/null; then
            local redis_ready=$(kubectl get deployment "auth-redis" -n "$namespace" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
            if [[ "$redis_ready" -gt 0 ]]; then
                record_result "$service Redis ($env)" "PASS" "Redis pod ready"
            else
                record_result "$service Redis ($env)" "FAIL" "Redis pod not ready"
            fi
        else
            record_result "$service Redis ($env)" "FAIL" "Redis deployment not found"
        fi
    fi
    
    # Check Redis for gateway service
    if [[ "$service" == "gateway" ]]; then
        if kubectl get deployment "gateway-redis" -n "$namespace" &>/dev/null; then
            local redis_ready=$(kubectl get deployment "gateway-redis" -n "$namespace" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
            if [[ "$redis_ready" -gt 0 ]]; then
                record_result "$service Redis ($env)" "PASS" "Redis pod ready"
            else
                record_result "$service Redis ($env)" "FAIL" "Redis pod not ready"
            fi
        else
            record_result "$service Redis ($env)" "FAIL" "Redis deployment not found"
        fi
    fi
}

# Function to check service health
check_service_health() {
    local service="$1"
    local env="$2"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Checking service health for $service in $env environment..."
    
    case "$service" in
        "auth")
            check_k8s_service "user-service" "gripday-auth$namespace_suffix" "$env"
            ;;
        "gateway")
            check_k8s_service "gateway-service" "gripday-gateway$namespace_suffix" "$env"
            ;;
        "all")
            check_k8s_service "user-service" "gripday-auth$namespace_suffix" "$env"
            check_k8s_service "gateway-service" "gripday-gateway$namespace_suffix" "$env"
            ;;
    esac
}

# Function to check Kubernetes service
check_k8s_service() {
    local service_name="$1"
    local namespace="$2"
    local env="$3"
    
    if kubectl get service "$service_name" -n "$namespace" &>/dev/null; then
        local service_type=$(kubectl get service "$service_name" -n "$namespace" -o jsonpath='{.spec.type}')
        local cluster_ip=$(kubectl get service "$service_name" -n "$namespace" -o jsonpath='{.spec.clusterIP}')
        
        record_result "$service_name Service ($env)" "PASS" "Type: $service_type, IP: $cluster_ip"
        
        # Check service endpoints
        local endpoints=$(kubectl get endpoints "$service_name" -n "$namespace" -o jsonpath='{.subsets[*].addresses[*].ip}' 2>/dev/null || echo "")
        if [[ -n "$endpoints" ]]; then
            local endpoint_count=$(echo "$endpoints" | wc -w)
            record_result "$service_name Endpoints ($env)" "PASS" "$endpoint_count endpoints available"
        else
            record_result "$service_name Endpoints ($env)" "FAIL" "No endpoints available"
        fi
    else
        record_result "$service_name Service ($env)" "FAIL" "Service not found"
    fi
}

# Function to check endpoint connectivity
check_endpoint_connectivity() {
    local service="$1"
    local env="$2"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Checking endpoint connectivity for $service in $env environment..."
    
    case "$service" in
        "auth")
            check_service_endpoints "user-service" "gripday-auth$namespace_suffix" "$env" "8080"
            ;;
        "gateway")
            check_service_endpoints "gateway-service" "gripday-gateway$namespace_suffix" "$env" "8080"
            ;;
        "all")
            check_service_endpoints "user-service" "gripday-auth$namespace_suffix" "$env" "8080"
            check_service_endpoints "gateway-service" "gripday-gateway$namespace_suffix" "$env" "8080"
            ;;
    esac
}

# Function to check service endpoints
check_service_endpoints() {
    local service_name="$1"
    local namespace="$2"
    local env="$3"
    local port="$4"
    
    # Check health endpoint
    local health_check_cmd="kubectl exec -n $namespace deployment/$service_name -- curl -f http://localhost:$port/actuator/health"
    
    if eval "$health_check_cmd" &>/dev/null; then
        record_result "$service_name Health Endpoint ($env)" "PASS" "Health endpoint responding"
    else
        record_result "$service_name Health Endpoint ($env)" "FAIL" "Health endpoint not responding"
    fi
    
    # Check readiness endpoint
    local readiness_check_cmd="kubectl exec -n $namespace deployment/$service_name -- curl -f http://localhost:$port/actuator/health/readiness"
    
    if eval "$readiness_check_cmd" &>/dev/null; then
        record_result "$service_name Readiness Endpoint ($env)" "PASS" "Readiness endpoint responding"
    else
        record_result "$service_name Readiness Endpoint ($env)" "FAIL" "Readiness endpoint not responding"
    fi
}

# Function to perform health check
perform_health_check() {
    local env="$1"
    
    print_status "Starting health check for $env environment..."
    print_status ""
    
    # Check cluster health (only once for all environments)
    if [[ "$CHECK_TYPE" == "all" || "$CHECK_TYPE" == "cluster" ]]; then
        check_cluster_health
    fi
    
    # Check namespace health
    if [[ "$CHECK_TYPE" == "all" || "$CHECK_TYPE" == "cluster" ]]; then
        check_namespace_health "$env"
    fi
    
    # Check pod health
    if [[ "$CHECK_TYPE" == "all" || "$CHECK_TYPE" == "pods" ]]; then
        check_pod_health "$SERVICE" "$env"
    fi
    
    # Check service health
    if [[ "$CHECK_TYPE" == "all" || "$CHECK_TYPE" == "services" ]]; then
        check_service_health "$SERVICE" "$env"
    fi
    
    # Check endpoint connectivity
    if [[ "$CHECK_TYPE" == "all" || "$CHECK_TYPE" == "endpoints" ]]; then
        check_endpoint_connectivity "$SERVICE" "$env"
    fi
}

# Function to generate health report
generate_health_report() {
    print_status ""
    print_status "=========================================="
    print_status "         HEALTH CHECK REPORT"
    print_status "=========================================="
    print_status ""
    
    print_status "Environment(s): $ENVIRONMENT"
    print_status "Service(s): $SERVICE"
    print_status "Check Type: $CHECK_TYPE"
    print_status "Total Checks: $TOTAL_CHECKS"
    print_status "Failed Checks: $FAILED_CHECKS"
    print_status "Success Rate: $(( (TOTAL_CHECKS - FAILED_CHECKS) * 100 / TOTAL_CHECKS ))%"
    print_status ""
    
    print_status "Detailed Results:"
    for result in "${HEALTH_RESULTS[@]}"; do
        echo "  $result"
    done
    
    print_status ""
    
    if [[ $FAILED_CHECKS -eq 0 ]]; then
        print_success "🎉 All health checks passed!"
        return 0
    else
        print_error "❌ $FAILED_CHECKS health check(s) failed"
        print_error "Please investigate the failed checks and resolve any issues"
        return 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if curl is available (for endpoint checks)
    if ! command -v curl &> /dev/null; then
        print_warning "curl is not available - endpoint connectivity checks will be skipped"
    fi
    
    print_status "Prerequisites check completed"
}

# Main execution
main() {
    print_status "Gripday Platform - Kubernetes Health Check"
    print_status "Environment: $ENVIRONMENT"
    print_status "Service: $SERVICE"
    print_status "Check Type: $CHECK_TYPE"
    print_status "Timeout: ${TIMEOUT}s"
    print_status ""
    
    check_prerequisites
    
    # Perform health checks based on environment selection
    case "$ENVIRONMENT" in
        "local"|"staging"|"production")
            perform_health_check "$ENVIRONMENT"
            ;;
        "all")
            for env in local staging production; do
                print_status "Checking $env environment..."
                perform_health_check "$env"
                print_status ""
            done
            ;;
    esac
    
    # Generate final report
    generate_health_report
}

# Run main function
main "$@"