#!/bin/bash

# Gripday Platform - Kubernetes Namespace Setup Script
# This script sets up namespaces for different environments

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="local"
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

Setup Kubernetes namespaces for Gripday Platform

OPTIONS:
    -e, --environment ENV    Environment to setup (local|staging|production|all) [default: local]
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

EXAMPLES:
    $0                                    # Setup local environment
    $0 -e staging                         # Setup staging environment
    $0 -e all                            # Setup all environments
    $0 -e production --dry-run           # Show what would be done for production

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
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
    print_error "Valid environments: local, staging, production, all"
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

# Function to setup namespaces for a specific environment
setup_environment_namespaces() {
    local env="$1"
    
    print_status "Setting up namespaces for $env environment..."
    
    # User service namespaces
    if [[ "$env" == "local" ]]; then
        execute_kubectl "apply -f user-service/namespace.yaml"
    else
        execute_kubectl "apply -f user-service/namespace.yaml"
    fi
    
    # Gateway service namespaces
    if [[ "$env" == "local" ]]; then
        execute_kubectl "apply -f gateway-service/namespace.yaml"
    else
        execute_kubectl "apply -f gateway-service/namespace.yaml"
    fi
    
    print_status "Namespaces for $env environment created successfully"
}

# Function to verify namespace creation
verify_namespaces() {
    local env="$1"
    
    print_status "Verifying namespaces for $env environment..."
    
    local namespaces=()
    if [[ "$env" == "local" ]]; then
        namespaces=("gripday-user" "gripday-gateway")
    elif [[ "$env" == "staging" ]]; then
        namespaces=("gripday-user-staging" "gripday-gateway-staging")
    elif [[ "$env" == "production" ]]; then
        namespaces=("gripday-user-production" "gripday-gateway-production")
    fi
    
    for ns in "${namespaces[@]}"; do
        if [[ "$DRY_RUN" == "false" ]]; then
            if kubectl get namespace "$ns" &>/dev/null; then
                print_status "✓ Namespace $ns exists"
            else
                print_error "✗ Namespace $ns not found"
                return 1
            fi
        else
            print_warning "[DRY-RUN] Would verify namespace: $ns"
        fi
    done
}

# Main execution
main() {
    print_status "Gripday Platform - Kubernetes Namespace Setup"
    print_status "Environment: $ENVIRONMENT"
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
            print_error "Please check your kubeconfig and cluster connectivity"
            exit 1
        fi
        
        print_status "Connected to Kubernetes cluster: $(kubectl config current-context)"
    fi
    
    # Setup namespaces based on environment
    case "$ENVIRONMENT" in
        "local"|"staging"|"production")
            setup_environment_namespaces "$ENVIRONMENT"
            verify_namespaces "$ENVIRONMENT"
            ;;
        "all")
            for env in local staging production; do
                setup_environment_namespaces "$env"
                verify_namespaces "$env"
            done
            ;;
    esac
    
    print_status "Namespace setup completed successfully!"
}

# Run main function
main "$@"