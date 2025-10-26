#!/bin/bash

# Gripday Platform - Minikube Cleanup Script
# Removes all deployed resources from minikube

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

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

Cleanup Gripday Platform from Minikube

OPTIONS:
    -f, --force            Force cleanup without confirmation
    -h, --help             Show this help message

EXAMPLES:
    $0                     # Cleanup with confirmation
    $0 --force             # Cleanup without confirmation

EOF
}

# Configuration
FORCE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE=true
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
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed"
    exit 1
fi

if ! command -v minikube &> /dev/null; then
    print_error "minikube is not installed"
    exit 1
fi

if ! minikube status &> /dev/null; then
    print_warning "minikube is not running"
    exit 0
fi

# Check if namespace exists
if ! kubectl get namespace gripday &> /dev/null; then
    print_info "Namespace 'gripday' does not exist. Nothing to clean up."
    exit 0
fi

# Show what will be deleted
print_header "Resources to be Deleted"

echo ""
echo -e "${YELLOW}Namespace:${NC} gripday"
echo ""
echo -e "${YELLOW}Resources in namespace:${NC}"
kubectl get all -n gripday 2>/dev/null || echo "  No resources found"
echo ""

# Confirm deletion
if [ "$FORCE" != true ]; then
    echo -e "${YELLOW}This will delete all resources in the 'gripday' namespace.${NC}"
    read -p "Are you sure you want to continue? (yes/no): " -r
    echo
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        print_info "Cleanup cancelled"
        exit 0
    fi
fi

# Delete resources
print_header "Cleaning Up Resources"

print_info "Deleting all resources in namespace 'gripday'..."

if kubectl delete namespace gripday --timeout=60s; then
    print_status "Namespace 'gripday' deleted successfully"
else
    print_error "Failed to delete namespace (it may take a moment to fully terminate)"
fi

# Wait for namespace deletion
print_info "Waiting for namespace termination..."
timeout=60
counter=0
while kubectl get namespace gripday &> /dev/null; do
    if [ $counter -ge $timeout ]; then
        print_warning "Namespace is still terminating (this is normal)"
        break
    fi
    sleep 2
    counter=$((counter + 2))
    echo -n "."
done
echo ""

if ! kubectl get namespace gripday &> /dev/null; then
    print_status "Namespace fully terminated"
fi

print_header "Cleanup Complete"

print_status "All Gripday Platform resources have been removed from minikube"
print_info "You can redeploy with: ./deploy-minikube.sh"

echo ""
