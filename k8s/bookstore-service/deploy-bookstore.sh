#!/bin/bash

# IQ Scaffold Bookstore Service - Kubernetes Deployment Script
# This script deploys only the bookstore service to Kubernetes

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
ENVIRONMENT="local"
NAMESPACE="iqscaffold-bookstore"

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

Deploy IQ Scaffold Bookstore Service to Kubernetes

OPTIONS:
    -e, --environment ENV   Target environment (local, staging, production) [default: local]
    -d, --dry-run          Show what would be done without executing
    -v, --verbose          Enable verbose output
    -s, --skip-build       Skip Docker image building
    -h, --help             Show this help message

EXAMPLES:
    $0                              # Deploy to local environment
    $0 -e staging                   # Deploy to staging environment
    $0 --skip-build                 # Deploy without building images
    $0 --dry-run -e production      # Show what would be deployed to production

PREREQUISITES:
    - kubectl configured for target cluster
    - Docker images built (unless --skip-build)
    - Target namespace exists

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
        -s|--skip-build)
            SKIP_BUILD=true
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

# Set namespace based on environment
case $ENVIRONMENT in
    local)
        NAMESPACE="iqscaffold-bookstore"
        ;;
    staging)
        NAMESPACE="iqscaffold-bookstore-staging"
        ;;
    production)
        NAMESPACE="iqscaffold-bookstore-production"
        ;;
    *)
        print_error "Invalid environment: $ENVIRONMENT"
        print_error "Valid environments: local, staging, production"
        exit 1
        ;;
esac

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
    print_status "Checking prerequisites for environment: $ENVIRONMENT"
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if cluster is accessible
    if ! kubectl cluster-info &>/dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        print_error "Please ensure kubectl is configured correctly"
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &>/dev/null; then
        print_warning "Namespace $NAMESPACE does not exist"
        print_status "Creating namespace..."
        execute_kubectl "apply -f namespace.yaml"
    fi
    
    print_status "Prerequisites check completed"
}

# Function to build Docker image
build_image() {
    if [[ "$SKIP_BUILD" == "true" ]]; then
        print_status "Skipping Docker image build"
        return 0
    fi
    
    print_status "Building bookstore service Docker image..."
    
    if [[ "$ENVIRONMENT" == "local" ]]; then
        # For local development with minikube
        if command -v minikube &> /dev/null && minikube status &>/dev/null; then
            print_status "Using minikube Docker daemon..."
            eval $(minikube docker-env)
        fi
    fi
    
    if [[ "$DRY_RUN" == "false" ]]; then
        cd ../../iqscaffold-bookstore-service
        docker build -t iqscaffold/bookstore-service:latest .
        cd ../k8s/bookstore-service
        print_status "Docker image built successfully"
    else
        print_warning "[DRY-RUN] Would build bookstore service image"
    fi
}

# Function to deploy PostgreSQL
deploy_postgres() {
    print_status "Deploying PostgreSQL for bookstore service..."
    
    execute_kubectl "apply -f bookstore-postgres-deployment.yaml"
    execute_kubectl "apply -f bookstore-postgres-service.yaml"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for PostgreSQL to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-postgres -n "$NAMESPACE" --timeout=300s
    fi
    
    print_status "PostgreSQL deployed successfully"
}

# Function to deploy Redis
deploy_redis() {
    print_status "Deploying Redis for bookstore service..."
    
    execute_kubectl "apply -f bookstore-redis-deployment.yaml"
    execute_kubectl "apply -f bookstore-redis-service.yaml"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for Redis to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-redis -n "$NAMESPACE" --timeout=300s
    fi
    
    print_status "Redis deployed successfully"
}

# Function to deploy bookstore service
deploy_service() {
    print_status "Deploying bookstore service application..."
    
    # Apply configuration and secrets
    execute_kubectl "apply -f configmap.yaml"
    execute_kubectl "apply -f secret.yaml"
    
    # Deploy the service
    execute_kubectl "apply -f bookstore-service-deployment.yaml"
    execute_kubectl "apply -f bookstore-service-service.yaml"
    
    # Apply HPA and network policies
    execute_kubectl "apply -f bookstore-service-hpa.yaml"
    execute_kubectl "apply -f network-policy.yaml"
    
    # Apply ingress
    execute_kubectl "apply -f bookstore-service-ingress.yaml"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for bookstore service to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=iqscaffold-bookstore-service -n "$NAMESPACE" --timeout=300s
    fi
    
    print_status "Bookstore service deployed successfully"
}

# Function to verify deployment
verify_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        print_warning "[DRY-RUN] Skipping deployment verification"
        return 0
    fi
    
    print_status "Verifying bookstore service deployment..."
    
    # Check pod status
    print_status "Pod status:"
    kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/part-of=iqscaffold
    
    # Check service status
    print_status "Service status:"
    kubectl get services -n "$NAMESPACE"
    
    # Check ingress status
    print_status "Ingress status:"
    kubectl get ingress -n "$NAMESPACE"
    
    # Show service endpoints
    print_status ""
    print_status "Bookstore service endpoints:"
    if [[ "$ENVIRONMENT" == "local" ]]; then
        print_status "  API: http://localhost/api/v1/bookstore"
        print_status "  Swagger UI: http://localhost/bookstore/swagger-ui.html"
        print_status "  Health Check: http://localhost/bookstore/actuator/health"
    elif [[ "$ENVIRONMENT" == "staging" ]]; then
        print_status "  API: https://api.iqscaffold.website/api/v1/bookstore"
    elif [[ "$ENVIRONMENT" == "production" ]]; then
        print_status "  API: https://api.iqscaffold.com/api/v1/bookstore"
    fi
    
    print_status "Deployment verification completed!"
}

# Function to show logs
show_logs() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return 0
    fi
    
    print_status "Recent logs from bookstore service:"
    kubectl logs -n "$NAMESPACE" -l app.kubernetes.io/name=iqscaffold-bookstore-service --tail=20 --prefix=true
}

# Main execution
main() {
    print_status "IQ Scaffold Bookstore Service - Kubernetes Deployment"
    print_status "Environment: $ENVIRONMENT"
    print_status "Namespace: $NAMESPACE"
    print_status "Dry Run: $DRY_RUN"
    print_status ""
    
    check_prerequisites
    build_image
    deploy_postgres
    deploy_redis
    deploy_service
    verify_deployment
    
    if [[ "$VERBOSE" == "true" ]]; then
        show_logs
    fi
    
    print_status "Bookstore service deployment completed successfully!"
}

# Run main function
main "$@"