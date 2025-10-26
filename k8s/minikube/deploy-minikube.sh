#!/bin/bash

# Gripday Platform - Minikube Deployment Script
# Automates the deployment of all services to minikube

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BUILD_IMAGES=false
WAIT_FOR_READY=true
OPEN_SERVICES=false

# Function to print colored output
print_header() {
    echo ""
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo ""
}

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy Gripday Platform to Minikube

OPTIONS:
    -b, --build             Build Docker images before deploying
    -n, --no-wait          Don't wait for pods to be ready
    -o, --open             Open service URLs in browser after deployment
    -h, --help             Show this help message

EXAMPLES:
    $0                     # Deploy with existing images
    $0 --build             # Build images and deploy
    $0 --build --open      # Build, deploy, and open services

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build)
            BUILD_IMAGES=true
            shift
            ;;
        -n|--no-wait)
            WAIT_FOR_READY=false
            shift
            ;;
        -o|--open)
            OPEN_SERVICES=true
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

# Check prerequisites
print_header "Checking Prerequisites"

if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed"
    exit 1
fi
print_status "kubectl found"

if ! command -v minikube &> /dev/null; then
    print_error "minikube is not installed"
    exit 1
fi
print_status "minikube found"

if ! minikube status &> /dev/null; then
    print_error "minikube is not running. Start it with: minikube start"
    exit 1
fi
print_status "minikube is running"

# Get minikube info
MINIKUBE_IP=$(minikube ip)
print_info "Minikube IP: $MINIKUBE_IP"

# Build Docker images if requested
if [ "$BUILD_IMAGES" = true ]; then
    print_header "Building Docker Images"
    
    # Check if we can build from this directory
    PROJECT_ROOT="$(cd ../.. && pwd)"
    
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        print_error "Cannot find project root. Please run from k8s/minikube directory"
        exit 1
    fi
    
    print_info "Project root: $PROJECT_ROOT"
    print_info "Setting Docker environment to use minikube..."
    eval $(minikube docker-env)
    
    cd "$PROJECT_ROOT"
    
    # Build Auth Service
    print_info "Building Auth Service..."
    if docker build -t gripday/auth-service:latest -f gripday-auth-service/Dockerfile . ; then
        print_status "Auth Service image built"
    else
        print_error "Failed to build Auth Service image"
        exit 1
    fi
    
    # Build Gateway Service
    print_info "Building Gateway Service..."
    if docker build -t gripday/gateway-service:latest -f gripday-gateway-service/Dockerfile . ; then
        print_status "Gateway Service image built"
    else
        print_error "Failed to build Gateway Service image"
        exit 1
    fi
    
    # Build Bookstore Service
    print_info "Building Bookstore Service..."
    if docker build -t gripday/bookstore-service:latest -f gripday-bookstore-service/Dockerfile . ; then
        print_status "Bookstore Service image built"
    else
        print_error "Failed to build Bookstore Service image"
        exit 1
    fi
    
    cd - > /dev/null
    print_status "All images built successfully"
fi

# Deploy to Kubernetes
print_header "Deploying to Minikube"

print_info "Applying Kubernetes manifests..."
if kubectl apply -f all-in-one.yaml; then
    print_status "Manifests applied successfully"
else
    print_error "Failed to apply manifests"
    exit 1
fi

# Wait for pods to be ready
if [ "$WAIT_FOR_READY" = true ]; then
    print_header "Waiting for Pods to be Ready"
    
    print_info "Waiting for databases..."
    kubectl wait --for=condition=ready pod -l app=postgres-auth -n gripday --timeout=300s || print_warning "Postgres Auth not ready"
    kubectl wait --for=condition=ready pod -l app=postgres-bookstore -n gripday --timeout=300s || print_warning "Postgres Bookstore not ready"
    kubectl wait --for=condition=ready pod -l app=redis -n gripday --timeout=300s || print_warning "Redis not ready"
    
    print_info "Waiting for microservices..."
    kubectl wait --for=condition=ready pod -l app=auth-service -n gripday --timeout=300s || print_warning "Auth Service not ready"
    kubectl wait --for=condition=ready pod -l app=bookstore-service -n gripday --timeout=300s || print_warning "Bookstore Service not ready"
    kubectl wait --for=condition=ready pod -l app=gateway-service -n gripday --timeout=300s || print_warning "Gateway Service not ready"
    
    print_status "All pods are ready"
fi

# Display deployment status
print_header "Deployment Status"

echo ""
kubectl get pods -n gripday
echo ""
kubectl get services -n gripday
echo ""

# Get service URLs
print_header "Service Access Information"

GATEWAY_URL="http://${MINIKUBE_IP}:30080"
AUTH_URL="http://${MINIKUBE_IP}:30081"
BOOKSTORE_URL="http://${MINIKUBE_IP}:30082"

echo ""
echo -e "${GREEN}Gateway Service:${NC}   $GATEWAY_URL"
echo -e "${GREEN}Auth Service:${NC}      $AUTH_URL"
echo -e "${GREEN}Bookstore Service:${NC} $BOOKSTORE_URL"
echo ""

# Test health endpoints
print_header "Testing Health Endpoints"

sleep 5  # Give services a moment to fully start

test_health() {
    local name=$1
    local url=$2
    
    if curl -s -f "$url/actuator/health" > /dev/null 2>&1; then
        print_status "$name is healthy"
        return 0
    else
        print_warning "$name health check failed (may still be starting)"
        return 1
    fi
}

test_health "Gateway" "$GATEWAY_URL"
test_health "Auth" "$AUTH_URL"
test_health "Bookstore" "$BOOKSTORE_URL"

# Show example commands
print_header "Quick Start Commands"

cat << EOF
Test the API:

1. Register a user:
   curl -X POST $GATEWAY_URL/api/v1/auth/signup \\
     -H "Content-Type: application/json" \\
     -d '{
       "username": "testuser",
       "email": "test@example.com",
       "password": "TestPass123!",
       "firstName": "Test",
       "lastName": "User"
     }'

2. Login (after email verification):
   curl -X POST $GATEWAY_URL/api/v1/auth/login \\
     -H "Content-Type: application/json" \\
     -d '{
       "username": "testuser",
       "password": "TestPass123!"
     }'

3. View logs:
   kubectl logs -f deployment/gateway-service -n gripday

4. Access pods:
   kubectl get pods -n gripday

5. Port forward (alternative to NodePort):
   kubectl port-forward -n gripday svc/gateway-service 8080:8080

EOF

# Open services in browser if requested
if [ "$OPEN_SERVICES" = true ]; then
    print_header "Opening Services"
    
    if command -v xdg-open &> /dev/null; then
        xdg-open "$GATEWAY_URL/actuator/health" &
    elif command -v open &> /dev/null; then
        open "$GATEWAY_URL/actuator/health" &
    else
        print_warning "Could not detect browser command"
    fi
fi

print_header "Deployment Complete!"

print_status "Gripday Platform is running on minikube"
print_info "Use 'kubectl get all -n gripday' to see all resources"
print_info "Use './cleanup-minikube.sh' to remove everything"

echo ""
