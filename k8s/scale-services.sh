#!/bin/bash

# Gripday Platform - Kubernetes Service Scaling Script
# This script manages horizontal scaling of services

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
REPLICAS=""
ACTION="scale"
DRY_RUN=false
VERBOSE=false

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

print_debug() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Manage horizontal scaling of Gripday Platform services

OPTIONS:
    -e, --environment ENV    Environment (local|staging|production) [default: local]
    -s, --service SERVICE    Service to scale (user|gateway|bookstore|all) [default: all]
    -r, --replicas COUNT     Number of replicas to scale to
    -a, --action ACTION      Action (scale|status|auto) [default: scale]
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

ACTIONS:
    scale                   Scale service to specific replica count
    status                  Show current scaling status
    auto                    Enable/configure auto-scaling

EXAMPLES:
    $0 -s user -r 3                    # Scale user service to 3 replicas
    $0 -e production -s gateway -r 10  # Scale gateway to 10 replicas in prod
    $0 -a status                       # Show scaling status for all services
    $0 -a auto -s gateway              # Configure auto-scaling for gateway

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
        -r|--replicas)
            REPLICAS="$2"
            shift 2
            ;;
        -a|--action)
            ACTION="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
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
if [[ ! "$ENVIRONMENT" =~ ^(local|staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    exit 1
fi

if [[ ! "$SERVICE" =~ ^(user|gateway|bookstore|all)$ ]]; then
    print_error "Invalid service: $SERVICE"
    exit 1
fi

if [[ ! "$ACTION" =~ ^(scale|status|auto)$ ]]; then
    print_error "Invalid action: $ACTION"
    exit 1
fi

if [[ "$ACTION" == "scale" && -z "$REPLICAS" ]]; then
    print_error "Replicas count required for scale action"
    exit 1
fi

# Function to execute kubectl command
execute_kubectl() {
    local cmd="$1"
    print_debug "Executing: kubectl $cmd"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        print_warning "[DRY-RUN] Would execute: kubectl $cmd"
    else
        if ! kubectl $cmd; then
            print_error "Failed to execute: kubectl $cmd"
            return 1
        fi
    fi
}

# Function to get namespace for environment
get_namespace() {
    local env="$1"
    case "$env" in
        "local")
            echo "gripday"
            ;;
        "staging")
            echo "staging-env"
            ;;
        "production")
            echo "production-env"
            ;;
    esac
}

# Function to scale a specific service
scale_service() {
    local service="$1"
    local replicas="$2"
    local env="$3"
    
    local namespace=$(get_namespace "$env")
    local deployment=""
    
    case "$service" in
        "user")
            deployment="user-service"
            ;;
        "gateway")
            deployment="gateway-service"
            ;;
        "bookstore")
            deployment="bookstore-service"
            ;;
    esac
    
    print_status "Scaling $service service to $replicas replicas in $env environment..."
    
    execute_kubectl "scale deployment/$deployment --replicas=$replicas -n $namespace"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for scaling to complete..."
        kubectl rollout status deployment/$deployment -n $namespace --timeout=300s
        print_status "✓ $service service scaled successfully"
    fi
}

# Function to show scaling status
show_scaling_status() {
    local service="$1"
    local env="$2"
    
    local namespace=$(get_namespace "$env")
    
    print_status "Scaling status for $env environment:"
    print_status ""
    
    if [[ "$service" == "all" || "$service" == "user" ]]; then
        local user_namespace="$namespace"
        print_status "User Service ($user_namespace):"
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl get deployment user-service -n "$user_namespace" -o wide 2>/dev/null || print_warning "User service not found"
            kubectl get hpa -n "$user_namespace" 2>/dev/null || print_debug "No HPA configured for user service"
        else
            print_warning "[DRY-RUN] Would show user service status"
        fi
        print_status ""
    fi
    
    if [[ "$service" == "all" || "$service" == "gateway" ]]; then
        local gateway_namespace="$namespace"
        print_status "Gateway Service ($gateway_namespace):"
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl get deployment gateway-service -n "$gateway_namespace" -o wide 2>/dev/null || print_warning "Gateway service not found"
            kubectl get hpa -n "$gateway_namespace" 2>/dev/null || print_debug "No HPA configured for gateway service"
        else
            print_warning "[DRY-RUN] Would show gateway service status"
        fi
        print_status ""
    fi
    
    if [[ "$service" == "all" || "$service" == "bookstore" ]]; then
        local bookstore_namespace="$namespace"
        print_status "Bookstore Service ($bookstore_namespace):"
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl get deployment bookstore-service -n "$bookstore_namespace" -o wide 2>/dev/null || print_warning "Bookstore service not found"
            kubectl get hpa -n "$bookstore_namespace" 2>/dev/null || print_debug "No HPA configured for bookstore service"
        else
            print_warning "[DRY-RUN] Would show bookstore service status"
        fi
        print_status ""
    fi
}

# Function to configure auto-scaling
configure_autoscaling() {
    local service="$1"
    local env="$2"
    
    local namespace=$(get_namespace "$env")
    
    print_status "Configuring auto-scaling for $service in $env environment..."
    
    case "$service" in
        "auth")
            print_status "Applying HPA for user service..."
            # User service typically doesn't need aggressive auto-scaling
            if [[ "$DRY_RUN" == "false" ]]; then
                kubectl autoscale deployment user-service --cpu-percent=70 --min=2 --max=10 -n "$namespace"
            else
                print_warning "[DRY-RUN] Would configure user service HPA"
            fi
            ;;
        "gateway")
            print_status "Applying HPA for gateway service..."
            execute_kubectl "apply -f gateway-service/gateway-service-hpa.yaml"
            ;;
        "bookstore")
            print_status "Applying HPA for bookstore service..."
            execute_kubectl "apply -f bookstore-service/bookstore-service-hpa.yaml"
            ;;
        "all")
            configure_autoscaling "user" "$env"
            configure_autoscaling "gateway" "$env"
            configure_autoscaling "bookstore" "$env"
            ;;
    esac
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check cluster connectivity
    if [[ "$DRY_RUN" == "false" ]]; then
        if ! kubectl cluster-info &>/dev/null; then
            print_error "Cannot connect to Kubernetes cluster"
            exit 1
        fi
        
        print_status "Connected to cluster: $(kubectl config current-context)"
    fi
    
    print_status "Prerequisites check completed"
}

# Main execution
main() {
    print_status "Gripday Platform - Service Scaling Management"
    print_status "Environment: $ENVIRONMENT"
    print_status "Service: $SERVICE"
    print_status "Action: $ACTION"
    if [[ -n "$REPLICAS" ]]; then
        print_status "Replicas: $REPLICAS"
    fi
    print_status "Dry Run: $DRY_RUN"
    
    check_prerequisites
    
    case "$ACTION" in
        "scale")
            if [[ "$SERVICE" == "all" ]]; then
                scale_service "user" "$REPLICAS" "$ENVIRONMENT"
                scale_service "gateway" "$REPLICAS" "$ENVIRONMENT"
                scale_service "bookstore" "$REPLICAS" "$ENVIRONMENT"
            else
                scale_service "$SERVICE" "$REPLICAS" "$ENVIRONMENT"
            fi
            ;;
        "status")
            show_scaling_status "$SERVICE" "$ENVIRONMENT"
            ;;
        "auto")
            configure_autoscaling "$SERVICE" "$ENVIRONMENT"
            ;;
    esac
    
    print_status "Service scaling operation completed successfully!"
}

# Run main function
main "$@"