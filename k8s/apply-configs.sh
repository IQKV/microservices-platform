#!/bin/bash

# Gripday Platform - Kubernetes ConfigMap and Secret Management Script
# This script manages ConfigMaps and Secrets for different environments

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="local"
ACTION="apply"
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

Manage Kubernetes ConfigMaps and Secrets for Gripday Platform

OPTIONS:
    -e, --environment ENV    Environment (local|staging|production|all) [default: local]
    -a, --action ACTION      Action to perform (apply|delete|update) [default: apply]
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

ACTIONS:
    apply                   Create or update ConfigMaps and Secrets
    delete                  Delete ConfigMaps and Secrets
    update                  Update existing ConfigMaps and Secrets

EXAMPLES:
    $0                                    # Apply configs for local environment
    $0 -e staging -a apply               # Apply configs for staging
    $0 -e production -a update           # Update production configs
    $0 -e all -a delete --dry-run        # Show what would be deleted

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
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

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(local|staging|production|all)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    exit 1
fi

# Validate action
if [[ ! "$ACTION" =~ ^(apply|delete|update)$ ]]; then
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

# Function to apply configs for a specific environment
apply_configs() {
    local env="$1"
    
    print_status "Applying ConfigMaps and Secrets for $env environment..."
    
    # Apply auth service configs
    execute_kubectl "$ACTION -f auth-service/configmap.yaml"
    execute_kubectl "$ACTION -f auth-service/secret.yaml"
    
    # Apply gateway service configs
    execute_kubectl "$ACTION -f gateway-service/configmap.yaml"
    execute_kubectl "$ACTION -f gateway-service/secret.yaml"
    
    # Apply bookstore service configs
    execute_kubectl "$ACTION -f bookstore-service/configmap.yaml"
    execute_kubectl "$ACTION -f bookstore-service/secret.yaml"
    
    print_status "ConfigMaps and Secrets for $env environment processed successfully"
}

# Function to verify configs
verify_configs() {
    local env="$1"
    
    print_status "Verifying ConfigMaps and Secrets for $env environment..."
    
    local auth_namespace="gripday-auth"
    local gateway_namespace="gripday-gateway"
    local bookstore_namespace="gripday-bookstore"
    
    if [[ "$env" == "staging" ]]; then
        auth_namespace="staging-env"
        gateway_namespace="staging-env"
        bookstore_namespace="staging-env"
    elif [[ "$env" == "production" ]]; then
        auth_namespace="production-env"
        gateway_namespace="production-env"
        bookstore_namespace="production-env"
    fi
    
    if [[ "$DRY_RUN" == "false" && "$ACTION" == "apply" ]]; then
        # Verify auth service configs
        if kubectl get configmap auth-service-config -n "$auth_namespace" &>/dev/null; then
            print_status "✓ Auth service ConfigMap exists in $auth_namespace"
        else
            print_error "✗ Auth service ConfigMap not found in $auth_namespace"
        fi
        
        if kubectl get secret auth-service-secrets -n "$auth_namespace" &>/dev/null; then
            print_status "✓ Auth service Secret exists in $auth_namespace"
        else
            print_error "✗ Auth service Secret not found in $auth_namespace"
        fi
        
        # Verify gateway service configs
        if kubectl get configmap gateway-service-config -n "$gateway_namespace" &>/dev/null; then
            print_status "✓ Gateway service ConfigMap exists in $gateway_namespace"
        else
            print_error "✗ Gateway service ConfigMap not found in $gateway_namespace"
        fi
        
        if kubectl get secret gateway-service-secrets -n "$gateway_namespace" &>/dev/null; then
            print_status "✓ Gateway service Secret exists in $gateway_namespace"
        else
            print_error "✗ Gateway service Secret not found in $gateway_namespace"
        fi
        
        # Verify bookstore service configs
        if kubectl get configmap bookstore-service-config -n "$bookstore_namespace" &>/dev/null; then
            print_status "✓ Bookstore service ConfigMap exists in $bookstore_namespace"
        else
            print_error "✗ Bookstore service ConfigMap not found in $bookstore_namespace"
        fi
        
        if kubectl get secret bookstore-service-secrets -n "$bookstore_namespace" &>/dev/null; then
            print_status "✓ Bookstore service Secret exists in $bookstore_namespace"
        else
            print_error "✗ Bookstore service Secret not found in $bookstore_namespace"
        fi
    fi
}

# Main execution
main() {
    print_status "Gripday Platform - ConfigMap and Secret Management"
    print_status "Environment: $ENVIRONMENT"
    print_status "Action: $ACTION"
    print_status "Dry Run: $DRY_RUN"
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if we can connect to Kubernetes cluster
    if [[ "$DRY_RUN" == "false" ]]; then
        if ! kubectl cluster-info &>/dev/null; then
            print_error "Cannot connect to Kubernetes cluster"
            exit 1
        fi
        
        print_status "Connected to Kubernetes cluster: $(kubectl config current-context)"
    fi
    
    # Process configs based on environment
    case "$ENVIRONMENT" in
        "local"|"staging"|"production")
            apply_configs "$ENVIRONMENT"
            if [[ "$ACTION" == "apply" ]]; then
                verify_configs "$ENVIRONMENT"
            fi
            ;;
        "all")
            for env in local staging production; do
                apply_configs "$env"
                if [[ "$ACTION" == "apply" ]]; then
                    verify_configs "$env"
                fi
            done
            ;;
    esac
    
    print_status "ConfigMap and Secret management completed successfully!"
}

# Run main function
main "$@"