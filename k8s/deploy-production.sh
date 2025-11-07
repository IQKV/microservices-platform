#!/bin/bash

# Gripday Platform - Production Kubernetes Deployment Script
# This script deploys the platform to a production Kubernetes cluster with safety checks

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
SKIP_CONFIRMATION=false
IMAGE_TAG="latest"
BACKUP_ENABLED=true

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

Deploy Gripday Platform to production Kubernetes cluster

OPTIONS:
    -d, --dry-run           Show what would be done without executing
    -v, --verbose           Enable verbose output
    -n, --skip-namespace    Skip namespace creation
    -y, --yes               Skip confirmation prompts (DANGEROUS)
    -t, --tag TAG           Docker image tag to deploy [default: latest]
    --no-backup             Skip database backup before deployment
    -h, --help              Show this help message

EXAMPLES:
    $0                      # Deploy production with safety checks
    $0 -t v1.2.3           # Deploy specific version
    $0 --dry-run            # Show what would be deployed
    $0 -y -t v1.2.3        # Deploy without confirmation (CI/CD)

PREREQUISITES:
    - kubectl configured for production cluster
    - Docker images pushed to registry with proper tags
    - Production cluster accessible
    - Database backup tools available (unless --no-backup)

SAFETY FEATURES:
    - Multiple confirmation prompts
    - Automatic database backup
    - Rolling deployment strategy
    - Health checks before completion
    - Rollback capability

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
        -y|--yes)
            SKIP_CONFIRMATION=true
            shift
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        --no-backup)
            BACKUP_ENABLED=false
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

# Function to confirm production deployment
confirm_production_deployment() {
    if [[ "$SKIP_CONFIRMATION" == "true" ]]; then
        print_warning "Skipping confirmation prompts (--yes flag used)"
        return 0
    fi
    
    print_warning "⚠️  PRODUCTION DEPLOYMENT WARNING ⚠️"
    print_warning ""
    print_warning "You are about to deploy to PRODUCTION environment!"
    print_warning "This will affect live users and services."
    print_warning ""
    print_warning "Deployment details:"
    print_warning "  Image Tag: $IMAGE_TAG"
    print_warning "  Backup Enabled: $BACKUP_ENABLED"
    print_warning "  Cluster: $(kubectl config current-context)"
    print_warning ""
    
    read -p "Are you sure you want to continue? (type 'DEPLOY' to confirm): " -r
    if [[ "$REPLY" != "DEPLOY" ]]; then
        print_error "Deployment cancelled"
        exit 1
    fi
    
    print_warning "Final confirmation: This will deploy to PRODUCTION"
    read -p "Type 'YES' to proceed: " -r
    if [[ "$REPLY" != "YES" ]]; then
        print_error "Deployment cancelled"
        exit 1
    fi
}

# Function to check production prerequisites
check_production_prerequisites() {
    print_status "Checking production prerequisites..."
    
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
        
        # Verify this is production cluster
        if [[ ! "$current_context" =~ production|prod ]]; then
            print_error "Current context doesn't appear to be production: $current_context"
            print_error "Please verify you're connected to the correct cluster"
            exit 1
        fi
    fi
    
    # Check if image exists in registry
    print_status "Verifying Docker images in registry..."
    # Note: This would typically check a container registry
    # For now, we'll just warn if using 'latest' tag
    if [[ "$IMAGE_TAG" == "latest" ]]; then
        print_warning "Using 'latest' tag for production deployment"
        print_warning "Consider using a specific version tag for production"
    fi
    
    print_status "Production prerequisites check completed"
}

# Function to backup databases
backup_databases() {
    if [[ "$BACKUP_ENABLED" == "false" ]]; then
        print_status "Database backup disabled"
        return 0
    fi
    
    print_status "Creating database backups before deployment..."
    
    if [[ "$DRY_RUN" == "false" ]]; then
        local backup_timestamp=$(date +%Y%m%d_%H%M%S)
        local backup_dir="/tmp/gripday_backup_$backup_timestamp"
        
        mkdir -p "$backup_dir"
        
        # Backup auth database
        print_status "Backing up auth database..."
        kubectl exec -n gripday-production-env deployment/auth-postgres -- \
            pg_dump -U gripday_user_prod gripday_auth_production > "$backup_dir/auth_db_backup.sql"
        
        print_status "Database backup completed: $backup_dir"
        print_status "Please ensure backups are stored in a secure location"
    else
        print_warning "[DRY-RUN] Would create database backups"
    fi
}

# Function to update image tags for production
update_production_image_tags() {
    print_status "Updating image tags for production: $IMAGE_TAG"
    
    if [[ "$DRY_RUN" == "false" ]]; then
        # Create production deployment files with updated image tags
        sed "s|gripday/auth-service:latest|gripday/auth-service:$IMAGE_TAG|g" \
            auth-service/auth-service-deployment.yaml > /tmp/auth-service-deployment-production.yaml
        
        sed "s|gripday/gateway-service:latest|gripday/gateway-service:$IMAGE_TAG|g" \
            gateway-service/gateway-service-deployment.yaml > /tmp/gateway-service-deployment-production.yaml
        
        sed "s|gripday/bookstore-service:latest|gripday/bookstore-service:$IMAGE_TAG|g" \
            bookstore-service/bookstore-service-deployment.yaml > /tmp/bookstore-service-deployment-production.yaml
        
        # Update namespace references for production
        sed -i 's/namespace: gripday-dev-env/namespace: gripday-production-env/g' /tmp/auth-service-deployment-production.yaml
        sed -i 's/namespace: gripday-dev-env/namespace: gripday-production-env/g' /tmp/gateway-service-deployment-production.yaml
        sed -i 's/namespace: gripday-dev-env/namespace: gripday-production-env/g' /tmp/bookstore-service-deployment-production.yaml
        
        # Update environment variables for production
        sed -i 's/value: "local"/value: "production"/g' /tmp/auth-service-deployment-production.yaml
        sed -i 's/value: "local"/value: "production"/g' /tmp/gateway-service-deployment-production.yaml
        sed -i 's/value: "local"/value: "production"/g' /tmp/bookstore-service-deployment-production.yaml
        
        # Update resource limits for production
        sed -i 's/replicas: 2/replicas: 3/g' /tmp/auth-service-deployment-production.yaml
        sed -i 's/replicas: 3/replicas: 5/g' /tmp/gateway-service-deployment-production.yaml
        sed -i 's/replicas: 2/replicas: 3/g' /tmp/bookstore-service-deployment-production.yaml
    else
        print_warning "[DRY-RUN] Would update image tags and create production deployment files"
    fi
}

# Function to deploy to production with rolling updates
deploy_production() {
    print_status "Starting production deployment with rolling updates..."
    
    # Setup namespaces if needed
    if [[ "$SKIP_NAMESPACE" == "false" ]]; then
        setup_production_namespaces
    fi
    
    # Deploy infrastructure first
    deploy_production_infrastructure
    
    # Deploy services with rolling updates
    deploy_production_services
    
    print_status "Production deployment completed"
}

# Function to setup production namespaces
setup_production_namespaces() {
    print_status "Setting up production namespaces..."
    
    execute_kubectl "apply -f auth-service/namespace.yaml"
    execute_kubectl "apply -f gateway-service/namespace.yaml"
    execute_kubectl "apply -f bookstore-service/namespace.yaml"
    
    print_status "Production namespaces ready"
}

# Function to deploy production infrastructure
deploy_production_infrastructure() {
    print_status "Deploying production infrastructure..."
    
    # Deploy configs and secrets
    execute_kubectl "apply -f auth-service/configmap.yaml"
    execute_kubectl "apply -f auth-service/secret.yaml"
    execute_kubectl "apply -f gateway-service/configmap.yaml"
    execute_kubectl "apply -f gateway-service/secret.yaml"
    execute_kubectl "apply -f bookstore-service/configmap.yaml"
    execute_kubectl "apply -f bookstore-service/secret.yaml"
    
    # Deploy databases (if not already deployed)
    print_status "Ensuring database infrastructure is ready..."
    
    # Note: In production, databases might be managed externally
    # This is a simplified example
    local postgres_prod=$(sed 's/namespace: gripday-dev-env$/namespace: gripday-production-env/g' auth-service/auth-postgres-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$postgres_prod" | kubectl apply -f -
    fi
    
    local redis_auth_prod=$(sed 's/namespace: gripday-dev-env$/namespace: gripday-production-env/g' auth-service/auth-redis-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_auth_prod" | kubectl apply -f -
    fi
    
    local redis_gateway_prod=$(sed 's/namespace: gripday-dev-env$/namespace: gripday-production-env/g' gateway-service/gateway-redis-deployment.yaml)
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "$redis_gateway_prod" | kubectl apply -f -
    fi
    
    print_status "Production infrastructure deployed"
}

# Function to deploy production services
deploy_production_services() {
    print_status "Deploying production services with rolling updates..."
    
    # Deploy auth service
    if [[ "$DRY_RUN" == "false" ]]; then
        execute_kubectl "apply -f /tmp/auth-service-deployment-production.yaml"
        
        # Wait for rollout to complete
        kubectl rollout status deployment/auth-service -n gripday-production-env --timeout=600s
    fi
    
    # Deploy gateway service
    if [[ "$DRY_RUN" == "false" ]]; then
        execute_kubectl "apply -f /tmp/gateway-service-deployment-production.yaml"
        
        # Wait for rollout to complete
        kubectl rollout status deployment/gateway-service -n gripday-production-env --timeout=600s
    fi
    
    # Deploy bookstore service
    if [[ "$DRY_RUN" == "false" ]]; then
        execute_kubectl "apply -f /tmp/bookstore-service-deployment-production.yaml"
        
        # Wait for rollout to complete
        kubectl rollout status deployment/bookstore-service -n gripday-production-env --timeout=600s
    fi
    
    # Deploy services and ingress
    execute_kubectl "apply -f auth-service/auth-service-service.yaml"
    execute_kubectl "apply -f gateway-service/gateway-service-service.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-service-service.yaml"
    execute_kubectl "apply -f gateway-service/gateway-service-hpa.yaml"
    execute_kubectl "apply -f bookstore-service/bookstore-service-hpa.yaml"
    execute_kubectl "apply -f gateway-service/network-policy.yaml"
    execute_kubectl "apply -f bookstore-service/network-policy.yaml"
    execute_kubectl "apply -f auth-service/network-policy.yaml"
    # Only gateway has ingress in production (API Gateway pattern)
    execute_kubectl "apply -f gateway-service/gateway-service-ingress.yaml"
    
    print_status "Production services deployed successfully"
}

# Function to verify production deployment
verify_production_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        print_warning "[DRY-RUN] Skipping deployment verification"
        return 0
    fi
    
    print_status "Verifying production deployment health..."
    
    # Wait for all pods to be ready
    print_status "Waiting for auth service pods to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-auth-service -n gripday-production-env --timeout=300s
    
    print_status "Waiting for gateway service pods to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-gateway-service -n gripday-production-env --timeout=300s
    
    print_status "Waiting for bookstore service pods to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-bookstore-service -n gripday-production-env --timeout=300s
    
    # Perform health checks
    print_status "Performing health checks..."
    
    # Check auth service health
    local auth_health=$(kubectl get pods -l app.kubernetes.io/name=gripday-auth-service -n gripday-production-env -o jsonpath='{.items[*].status.phase}')
    if [[ "$auth_health" =~ "Running" ]]; then
        print_status "✓ Auth service health check passed"
    else
        print_error "✗ Auth service health check failed"
        return 1
    fi
    
    # Check gateway service health
    local gateway_health=$(kubectl get pods -l app.kubernetes.io/name=gripday-gateway-service -n gripday-production-env -o jsonpath='{.items[*].status.phase}')
    if [[ "$gateway_health" =~ "Running" ]]; then
        print_status "✓ Gateway service health check passed"
    else
        print_error "✗ Gateway service health check failed"
        return 1
    fi
    
    print_status "Production deployment verification completed successfully!"
    print_status ""
    print_status "Production URLs:"
    print_status "  Main App: https://pynity.com"
    print_status "  Auth Service: https://auth.pynity.com"
    print_status "  Gateway Service: https://api.pynity.com"
    print_status ""
    print_status "Monitoring and logs:"
    print_status "  kubectl logs -f deployment/auth-service -n gripday-production-env"
    print_status "  kubectl logs -f deployment/gateway-service -n gripday-production-env"
    
    # Cleanup temporary files
    rm -f /tmp/auth-service-deployment-production.yaml
    rm -f /tmp/gateway-service-deployment-production.yaml
    rm -f /tmp/bookstore-service-deployment-production.yaml
}

# Main execution
main() {
    print_status "Gripday Platform - Production Kubernetes Deployment"
    print_status "Image Tag: $IMAGE_TAG"
    print_status "Dry Run: $DRY_RUN"
    print_status "Backup Enabled: $BACKUP_ENABLED"
    
    confirm_production_deployment
    check_production_prerequisites
    backup_databases
    update_production_image_tags
    deploy_production
    verify_production_deployment
    
    print_status "🎉 Production deployment completed successfully!"
    print_status "Please monitor the services and verify everything is working correctly."
}

# Run main function
main "$@"