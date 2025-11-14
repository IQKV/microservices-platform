#!/bin/bash

# Gripday Platform - Local Kubernetes Deployment Script (Minikube)
# This script deploys the platform to a local Kubernetes cluster (minikube)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
DRY_RUN=false
VERBOSE=false
SKIP_BUILD=false
SKIP_NAMESPACE=false

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

Deploy Gripday Platform to local Kubernetes cluster (minikube)

OPTIONS:
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -s, --skip-build        Skip Docker image building
    -n, --skip-namespace    Skip namespace creation
    -h, --help              Show this help message

EXAMPLES:
    $0                      # Full deployment
    $0 --skip-build         # Deploy without building images
    $0 --dry-run            # Show what would be deployed

PREREQUISITES:
    - minikube running
    - kubectl configured for minikube
    - Docker images built (unless --skip-build)

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -n|--skip-namespace)
            SKIP_NAMESPACE=true
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

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if minikube is running
    if ! minikube status &>/dev/null; then
        print_error "Minikube is not running. Please start minikube first:"
        print_error "  minikube start"
        exit 1
    fi
    
    # Check if kubectl is configured for minikube
    local current_context=$(kubectl config current-context 2>/dev/null || echo "")
    if [[ "$current_context" != "minikube" ]]; then
        print_warning "Current kubectl context is not minikube: $current_context"
        print_warning "Switching to minikube context..."
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl config use-context minikube
        fi
    fi
    
    # Check if Docker images exist (unless skipping build)
    if [[ "$SKIP_BUILD" == "false" ]]; then
        print_status "Checking Docker images..."
        eval $(minikube docker-env)
        
        if ! docker image inspect gripday/user-service:latest &>/dev/null; then
            print_warning "Auth service image not found, will build..."
        fi
        
        if ! docker image inspect gripday/gateway-service:latest &>/dev/null; then
            print_warning "Gateway service image not found, will build..."
        fi
        
        if ! docker image inspect gripday/bookstore-service:latest &>/dev/null; then
            print_warning "Bookstore service image not found, will build..."
        fi
    fi
    
    print_status "Prerequisites check completed"
}

# Function to build Docker images
build_images() {
    if [[ "$SKIP_BUILD" == "true" ]]; then
        print_status "Skipping Docker image build"
        return 0
    fi
    
    print_status "Building Docker images for minikube..."
    
    # Use minikube's Docker daemon
    eval $(minikube docker-env)
    
    # Build auth service
    print_status "Building auth service image..."
    if [[ "$DRY_RUN" == "false" ]]; then
        cd gripday-user-service
        docker build -t gripday/user-service:latest .
        cd ..
    else
        print_warning "[DRY-RUN] Would build auth service image"
    fi
    
    # Build gateway service
    print_status "Building gateway service image..."
    if [[ "$DRY_RUN" == "false" ]]; then
        cd gripday-gateway-service
        docker build -t gripday/gateway-service:latest .
        cd ..
    else
        print_warning "[DRY-RUN] Would build gateway service image"
    fi
    
    # Build bookstore service
    print_status "Building bookstore service image..."
    if [[ "$DRY_RUN" == "false" ]]; then
        cd gripday-bookstore-service
        docker build -t gripday/bookstore-service:latest .
        cd ..
    else
        print_warning "[DRY-RUN] Would build bookstore service image"
    fi
    
    print_status "Docker images built successfully"
}

# Function to setup namespaces
setup_namespaces() {
    if [[ "$SKIP_NAMESPACE" == "true" ]]; then
        print_status "Skipping namespace setup"
        return 0
    fi
    
    print_status "Setting up namespaces..."
    
    execute_kubectl "apply -f user-service/namespace.yaml"
    execute_kubectl "apply -f gateway-service/namespace.yaml"
    execute_kubectl "apply -f bookstore-service/namespace.yaml"
    
    print_status "Namespaces created successfully"
}

# Function to deploy auth service
deploy_auth_service() {
    print_status "Deploying auth service..."
    
    # Apply configs and secrets
    execute_kubectl "apply -f user-service/configmap.yaml"
    execute_kubectl "apply -f user-service/secret.yaml"
    
    # Deploy PostgreSQL
    execute_kubectl "apply -f user-service/auth-postgres-deployment.yaml"
    execute_kubectl "apply -f user-service/auth-postgres-service.yaml"
    
    # Deploy Redis
    execute_kubectl "apply -f user-service/auth-redis-deployment.yaml"
    execute_kubectl "apply -f user-service/auth-redis-service.yaml"
    
    # Wait for databases to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for PostgreSQL to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=auth-postgres -n gripday-auth --timeout=300s
        
        print_status "Waiting for Redis to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=auth-redis -n gripday-auth --timeout=300s
    fi
    
    # Deploy auth service
    execute_kubectl "apply -f user-service/user-service-deployment.yaml"
    execute_kubectl "apply -f user-service/user-service-service.yaml"
    execute_kubectl "apply -f user-service/user-service-ingress.yaml"
    
    print_status "Auth service deployed successfully"
}

# Function to deploy gateway service
deploy_gateway_service() {
    print_status "Deploying gateway service..."
    
    # Apply configs and secrets
    execute_kubectl "apply -f gateway-service/configmap.yaml"
    execute_kubectl "apply -f gateway-service/secret.yaml"
    
    # Deploy Redis
    execute_kubectl "apply -f gateway-service/gateway-redis-deployment.yaml"
    execute_kubectl "apply -f gateway-service/gateway-redis-service.yaml"
    
    # Wait for Redis to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for Gateway Redis to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gateway-redis -n gripday-gateway --timeout=300s
    fi
    
    # Deploy gateway service
    execute_kubectl "apply -f gateway-service/gateway-service-deployment.yaml"
    execute_kubectl "apply -f gateway-service/gateway-service-service.yaml"
    execute_kubectl "apply -f gateway-service/gateway-service-hpa.yaml"
    execute_kubectl "apply -f gateway-service/network-policy.yaml"
    execute_kubectl "apply -f gateway-service/gateway-service-ingress.yaml"
    
    print_status "Gateway service deployed successfully"
}

# Function to deploy bookstore service
deploy_bookstore_service() {
    print_status "Deploying bookstore service..."
    
    # Apply configs and secrets
    execute_kubectl "apply -f bookstore-service/configmap.yaml"
    execute_kubectl "apply -f bookstore-service/secret.yaml"
    
    # Deploy PostgreSQL
    execute_kubectl "apply -f bookstore-service/bookstore-postgres-deployment.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-postgres-service.yaml"
    
    # Deploy Redis
    execute_kubectl "apply -f bookstore-service/bookstore-redis-deployment.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-redis-service.yaml"
    
    # Wait for databases to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for Bookstore PostgreSQL to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-postgres -n gripday-bookstore --timeout=300s
        
        print_status "Waiting for Bookstore Redis to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-redis -n gripday-bookstore --timeout=300s
    fi
    
    # Deploy bookstore service
    execute_kubectl "apply -f bookstore-service/bookstore-service-deployment.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-service-service.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-service-hpa.yaml"
    execute_kubectl "apply -f bookstore-service/network-policy.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-service-ingress.yaml"
    
    print_status "Bookstore service deployed successfully"
}

# Function to verify deployment
verify_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        print_warning "[DRY-RUN] Skipping deployment verification"
        return 0
    fi
    
    print_status "Verifying deployment..."
    
    # Wait for auth service to be ready
    print_status "Waiting for auth service to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-user-service -n gripday-auth --timeout=300s
    
    # Wait for gateway service to be ready
    print_status "Waiting for gateway service to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-gateway-service -n gripday-gateway --timeout=300s
    
    # Wait for bookstore service to be ready
    print_status "Waiting for bookstore service to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-bookstore-service -n gripday-bookstore --timeout=300s
    
    # Show service URLs
    print_status "Deployment verification completed!"
    print_status ""
    print_status "Service URLs (add to /etc/hosts):"
    print_status "  $(minikube ip) auth.gripday.site"
    print_status "  $(minikube ip) api.gripday.site"
    print_status ""
    print_status "Access services:"
    print_status "  User Service: http://auth.gripday.site"
    print_status "  Gateway Service: http://api.gripday.site"
    print_status "  Bookstore Service: http://localhost/api/v1/bookstore"
    print_status ""
    print_status "Swagger UI:"
    print_status "  User Service: http://auth.gripday.site/swagger-ui.html"
    print_status "  Bookstore Service: http://localhost/bookstore/swagger-ui.html"
}

# Main execution
main() {
    print_status "Gripday Platform - Local Kubernetes Deployment (Minikube)"
    print_status "Dry Run: $DRY_RUN"
    
    check_prerequisites
    build_images
    setup_namespaces
    deploy_auth_service
    deploy_gateway_service
    deploy_bookstore_service
    verify_deployment
    
    print_status "Local deployment completed successfully!"
}

# Run main function
main "$@"