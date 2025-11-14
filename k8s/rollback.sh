#!/bin/bash

# Gripday Platform - Kubernetes Rollback Script
# This script handles rollback operations for deployments

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
REVISION=""
ACTION="rollback"
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

Manage rollback operations for Gripday Platform services

OPTIONS:
    -e, --environment ENV    Environment (local|staging|production) [default: local]
    -s, --service SERVICE    Service to rollback (user|gateway|all) [default: all]
    -r, --revision REV       Revision number to rollback to (optional)
    -a, --action ACTION      Action (rollback|history|status) [default: rollback]
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

ACTIONS:
    rollback                Rollback to previous or specific revision
    history                 Show deployment history
    status                  Show current deployment status

EXAMPLES:
    $0 -s auth                          # Rollback user service to previous revision
    $0 -e production -s gateway -r 3    # Rollback gateway to revision 3 in prod
    $0 -a history                       # Show deployment history for all services
    $0 -a status -e staging             # Show deployment status in staging

SAFETY FEATURES:
    - Confirmation prompts for production
    - Deployment history verification
    - Health checks after rollback
    - Automatic rollout status monitoring

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
        -r|--revision)
            REVISION="$2"
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

if [[ ! "$SERVICE" =~ ^(user|gateway|all)$ ]]; then
    print_error "Invalid service: $SERVICE"
    exit 1
fi

if [[ ! "$ACTION" =~ ^(rollback|history|status)$ ]]; then
    print_error "Invalid action: $ACTION"
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

# Function to get namespace suffix
get_namespace_suffix() {
    local env="$1"
    if [[ "$env" == "local" ]]; then
        echo ""
    else
        echo "-$env"
    fi
}

# Function to confirm production rollback
confirm_production_rollback() {
    if [[ "$ENVIRONMENT" != "production" ]]; then
        return 0
    fi
    
    print_warning "⚠️  PRODUCTION ROLLBACK WARNING ⚠️"
    print_warning ""
    print_warning "You are about to rollback services in PRODUCTION!"
    print_warning "This will affect live users and services."
    print_warning ""
    print_warning "Rollback details:"
    print_warning "  Environment: $ENVIRONMENT"
    print_warning "  Service: $SERVICE"
    if [[ -n "$REVISION" ]]; then
        print_warning "  Target Revision: $REVISION"
    else
        print_warning "  Target Revision: Previous"
    fi
    print_warning ""
    
    read -p "Are you sure you want to continue? (type 'ROLLBACK' to confirm): " -r
    if [[ "$REPLY" != "ROLLBACK" ]]; then
        print_error "Rollback cancelled"
        exit 1
    fi
}

# Function to show deployment history
show_deployment_history() {
    local service="$1"
    local env="$2"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Deployment history for $service in $env environment:"
    print_status ""
    
    case "$service" in
        "user")
            local namespace="gripday-user$namespace_suffix"
            print_status "User Service History:"
            if [[ "$DRY_RUN" == "false" ]]; then
                kubectl rollout history deployment/user-service -n "$namespace" || print_warning "No history available"
            else
                print_warning "[DRY-RUN] Would show user service history"
            fi
            ;;
        "gateway")
            local namespace="gripday-gateway$namespace_suffix"
            print_status "Gateway Service History:"
            if [[ "$DRY_RUN" == "false" ]]; then
                kubectl rollout history deployment/gateway-service -n "$namespace" || print_warning "No history available"
            else
                print_warning "[DRY-RUN] Would show gateway service history"
            fi
            ;;
        "all")
            show_deployment_history "user" "$env"
            print_status ""
            show_deployment_history "gateway" "$env"
            ;;
    esac
}

# Function to show deployment status
show_deployment_status() {
    local service="$1"
    local env="$2"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    
    print_status "Deployment status for $service in $env environment:"
    print_status ""
    
    case "$service" in
        "user")
            local namespace="gripday-user$namespace_suffix"
            print_status "User Service Status:"
            if [[ "$DRY_RUN" == "false" ]]; then
                kubectl rollout status deployment/user-service -n "$namespace" --timeout=10s || print_warning "Status check timed out"
                kubectl get deployment user-service -n "$namespace" -o wide
            else
                print_warning "[DRY-RUN] Would show user service status"
            fi
            ;;
        "gateway")
            local namespace="gripday-gateway$namespace_suffix"
            print_status "Gateway Service Status:"
            if [[ "$DRY_RUN" == "false" ]]; then
                kubectl rollout status deployment/gateway-service -n "$namespace" --timeout=10s || print_warning "Status check timed out"
                kubectl get deployment gateway-service -n "$namespace" -o wide
            else
                print_warning "[DRY-RUN] Would show gateway service status"
            fi
            ;;
        "all")
            show_deployment_status "user" "$env"
            print_status ""
            show_deployment_status "gateway" "$env"
            ;;
    esac
}

# Function to rollback a specific service
rollback_service() {
    local service="$1"
    local env="$2"
    local revision="$3"
    
    local namespace_suffix=$(get_namespace_suffix "$env")
    local namespace=""
    local deployment=""
    
    case "$service" in
        "user")
            namespace="gripday-user$namespace_suffix"
            deployment="user-service"
            ;;
        "gateway")
            namespace="gripday-gateway$namespace_suffix"
            deployment="gateway-service"
            ;;
    esac
    
    print_status "Rolling back $service service in $env environment..."
    
    # Show current status before rollback
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Current deployment status:"
        kubectl get deployment "$deployment" -n "$namespace" -o wide
        print_status ""
    fi
    
    # Perform rollback
    local rollback_cmd="rollout undo deployment/$deployment -n $namespace"
    if [[ -n "$revision" ]]; then
        rollback_cmd="$rollback_cmd --to-revision=$revision"
        print_status "Rolling back to revision $revision..."
    else
        print_status "Rolling back to previous revision..."
    fi
    
    execute_kubectl "$rollback_cmd"
    
    # Wait for rollback to complete
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for rollback to complete..."
        kubectl rollout status deployment/"$deployment" -n "$namespace" --timeout=300s
        
        # Verify rollback success
        print_status "Verifying rollback success..."
        kubectl get deployment "$deployment" -n "$namespace" -o wide
        
        # Check pod health
        print_status "Checking pod health after rollback..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name="$deployment" -n "$namespace" --timeout=120s
        
        print_status "✓ $service service rollback completed successfully"
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
    
    # Check cluster connectivity
    if [[ "$DRY_RUN" == "false" ]]; then
        if ! kubectl cluster-info &>/dev/null; then
            print_error "Cannot connect to Kubernetes cluster"
            exit 1
        fi
        
        print_status "Connected to cluster: $(kubectl config current-context)"
        
        # Verify environment matches cluster context for production
        if [[ "$ENVIRONMENT" == "production" ]]; then
            local current_context=$(kubectl config current-context)
            if [[ ! "$current_context" =~ production|prod ]]; then
                print_warning "Current context doesn't appear to be production: $current_context"
                read -p "Continue anyway? (y/N): " -n 1 -r
                echo
                if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                    print_error "Rollback cancelled"
                    exit 1
                fi
            fi
        fi
    fi
    
    print_status "Prerequisites check completed"
}

# Main execution
main() {
    print_status "Gripday Platform - Rollback Management"
    print_status "Environment: $ENVIRONMENT"
    print_status "Service: $SERVICE"
    print_status "Action: $ACTION"
    if [[ -n "$REVISION" ]]; then
        print_status "Target Revision: $REVISION"
    fi
    print_status "Dry Run: $DRY_RUN"
    
    check_prerequisites
    
    case "$ACTION" in
        "rollback")
            confirm_production_rollback
            if [[ "$SERVICE" == "all" ]]; then
                rollback_service "user" "$ENVIRONMENT" "$REVISION"
                rollback_service "gateway" "$ENVIRONMENT" "$REVISION"
            else
                rollback_service "$SERVICE" "$ENVIRONMENT" "$REVISION"
            fi
            ;;
        "history")
            show_deployment_history "$SERVICE" "$ENVIRONMENT"
            ;;
        "status")
            show_deployment_status "$SERVICE" "$ENVIRONMENT"
            ;;
    esac
    
    print_status "Rollback operation completed successfully!"
}

# Run main function
main "$@"