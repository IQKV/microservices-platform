#!/bin/bash

# Gripday Platform - Staging Kubernetes Deployment Script
# This script deploys the platform to a staging Kubernetes cluster

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
SKIP_NAMESPACE=false
IMAGE_TAG="staging"

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

Deploy Gripday Platform to staging Kubernetes cluster

OPTIONS:
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -n, --skip-namespace    Skip namespace creation
    -t, --tag TAG           Docker image tag to deploy [default: staging]
    -h, --help              Show this help message

EXAMPLES:
    $0                      # Deploy staging with default tag
    $0 -t v1.2.3           # Deploy specific version
    $0 --dry-run            # Show what would be deployed

PREREQUISITES:
    - kubectl configured for staging cluster
    - Docker images pushed to registry
    - Staging cluster accessible

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
        -n|--skip-namespace)
            SKIP_NAMESPACE=true
            shift
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
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
    print_status "Checking prerequisites for staging deployment..."
    
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
        
        local current_context=$(kubectl config current-context)
        print_status "Connected to cluster: $current_context"
        
        # Verify this is staging cluster (basic check)
        if [[ ! "$current_context" =~ staging ]]; then
            print_warning "Current context doesn't appear to be staging: $current_context"
            read -p "Continue anyway? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                print_error "Deployment cancelled"
                exit 1
            fi
        fi
    fi
    
    print_status "Prerequisites check completed"
}

# Function to setup namespaces
setup_namespaces() {
    if [[ "$SKIP_NAMESPACE" == "true" ]]; then
        print_status "Skipping namespace setup"
        return 0
    fi
    
    print_status "Setting up staging namespaces..."
    
    # Create staging namespaces
    execute_kubectl "apply -f user-service/namespace.yaml"
    execute_kubectl "apply -f gateway-service/namespace.yaml"
    
    print_status "Staging namespaces created successfully"
}

# Function to update image tags in deployment files
update_image_tags() {
    print_status "Updating image tags to: $IMAGE_TAG"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        # Create temporary deployment files with updated image tags
        sed "s|gripday/user-service:latest|gripday/user-service:$IMAGE_TAG|g" \
            user-service/user-service-deployment.yaml > /tmp/user-service-deployment-staging.yaml
        
        sed "s|gripday/gateway-service:latest|gripday/gateway-service:$IMAGE_TAG|g" \
            gateway-service/gateway-service-deployment.yaml > /tmp/gateway-service-deployment-staging.yaml
        
        # Update namespace references for staging
        sed -i 's/namespace: gripday-dev-env$/namespace: staging-env/g' /tmp/user-service-deployment-staging.yaml
        sed -i 's/namespace: gripday-dev-env$/namespace: staging-env/g' /tmp/gateway-service-deployment-staging.yaml
    else
        print_warning "[DRY-RUN] Would update image tags and create staging deployment files"
    fi
}

# Function to deploy user service to staging
deploy_auth_service_staging() {
    print_status "Deploying user service to staging..."
    
    # Apply staging configs and secrets
    execute_kubectl "apply -f user-service/configmap.yaml"
    execute_kubectl "apply -f user-service/secret.yaml"
    
    # Deploy PostgreSQL for staging
    local postgres_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' user-service/user-postgres-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$postgres_staging" | kubectl apply -f -
    else
        print_warning "[DRY-RUN] Would deploy PostgreSQL to staging"
    fi
    
    local postgres_svc_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' user-service/user-postgres-service.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$postgres_svc_staging" | kubectl apply -f -
    else
        print_warning "[DRY-RUN] Would deploy PostgreSQL service to staging"
    fi
    
    # Deploy Redis for staging
    local redis_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' user-service/user-redis-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_staging" | kubectl apply -f -
    else
        print_warning "[DRY-RUN] Would deploy Redis to staging"
    fi
    
    local redis_svc_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' user-service/user-redis-service.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_svc_staging" | kubectl apply -f -
    else
        print_warning "[DRY-RUN] Would deploy Redis service to staging"
    fi
    
    # Wait for databases to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for staging databases to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=user-postgres -n staging-env --timeout=300s
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=user-redis -n staging-env --timeout=300s
    fi
    
    # Deploy user service with updated image
    if [[ "$DRY_RUN" == "false" ]]; then
        execute_kubectl "apply -f /tmp/user-service-deployment-staging.yaml"
    else
        print_warning "[DRY-RUN] Would deploy user service to staging"
    fi
    
    # Deploy service and ingress
    local auth_svc_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' user-service/user-service-service.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$auth_svc_staging" | kubectl apply -f -
    fi
    
    # User service is only accessible through gateway in staging/production
    # execute_kubectl "apply -f user-service/user-service-ingress.yaml"
    
    print_status "User service deployed to staging successfully"
}

# Function to deploy gateway service to staging
deploy_gateway_service_staging() {
    print_status "Deploying gateway service to staging..."
    
    # Apply staging configs and secrets
    execute_kubectl "apply -f gateway-service/configmap.yaml"
    execute_kubectl "apply -f gateway-service/secret.yaml"
    
    # Deploy Redis for staging
    local redis_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' gateway-service/gateway-redis-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_staging" | kubectl apply -f -
    fi
    
    local redis_svc_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' gateway-service/gateway-redis-service.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_svc_staging" | kubectl apply -f -
    fi
    
    # Wait for Redis to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        print_status "Waiting for staging gateway Redis to be ready..."
        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gateway-redis -n staging-env --timeout=300s
    fi
    
    # Deploy gateway service with updated image
    if [[ "$DRY_RUN" == "false" ]]; then
        execute_kubectl "apply -f /tmp/gateway-service-deployment-staging.yaml"
    fi
    
    # Deploy service, HPA, network policy, and ingress
    local gateway_svc_staging=$(sed 's/namespace: gripday-dev-env$/namespace: staging-env/g' gateway-service/gateway-service-service.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$gateway_svc_staging" | kubectl apply -f -
    fi
    
    execute_kubectl "apply -f gateway-service/gateway-service-hpa.yaml"
    execute_kubectl "apply -f gateway-service/network-policy.yaml"
    execute_kubectl "apply -f user-service/network-policy.yaml"
    # Only gateway has ingress in staging (API Gateway pattern)
    execute_kubectl "apply -f gateway-service/gateway-service-ingress.yaml"
    
    print_status "Gateway service deployed to staging successfully"
}

# Function to verify staging deployment
verify_staging_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        print_warning "[DRY-RUN] Skipping deployment verification"
        return 0
    fi
    
    print_status "Verifying staging deployment..."
    
    # Wait for services to be ready
    print_status "Waiting for user service to be ready in staging..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-user-service -n staging-env --timeout=300s
    
    print_status "Waiting for gateway service to be ready in staging..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-gateway-service -n staging-env --timeout=300s
    
    # Show deployment status
    print_status "Staging deployment verification completed!"
    print_status ""
    print_status "Staging URLs:"
    print_status "  Main App: https://gripday.website"
    print_status "  User Service: https://auth.gripday.website"
    print_status "  Gateway Service: https://api.gripday.website"
    print_status "  Auth Swagger UI: https://auth.gripday.website/swagger-ui.html"
    
    # Cleanup temporary files
    rm -f /tmp/user-service-deployment-staging.yaml
    rm -f /tmp/gateway-service-deployment-staging.yaml
}

# Main execution
main() {
    print_status "Gripday Platform - Staging Kubernetes Deployment"
    print_status "Image Tag: $IMAGE_TAG"
    print_status "Dry Run: $DRY_RUN"
    
    check_prerequisites
    setup_namespaces
    update_image_tags
    deploy_auth_service_staging
    deploy_gateway_service_staging
    verify_staging_deployment
    
    print_status "Staging deployment completed successfully!"
}

# Run main function
main "$@"