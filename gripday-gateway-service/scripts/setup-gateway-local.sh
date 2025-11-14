#!/bin/bash

# Setup script for Gripday Gateway Service - Local Development Environment
# This script sets up the local development environment for the gateway service

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env.local"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required tools are installed
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        missing_tools+=("docker-compose")
    fi
    
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("maven")
    fi
    
    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install the missing tools and try again."
        exit 1
    fi
    
    log_success "All prerequisites are installed"
}

# Create environment file if it doesn't exist
setup_environment_file() {
    log_info "Setting up environment file..."
    
    if [ ! -f "$ENV_FILE" ]; then
        if [ -f "$PROJECT_DIR/.env.example" ]; then
            cp "$PROJECT_DIR/.env.example" "$ENV_FILE"
            log_success "Created $ENV_FILE from template"
        else
            log_error ".env.example file not found"
            exit 1
        fi
    else
        log_info "Environment file already exists: $ENV_FILE"
    fi
}

# Start local infrastructure services
start_infrastructure() {
    log_info "Starting local infrastructure services..."
    
    cd "$PROJECT_DIR"
    
    # Start Redis using Docker Compose
    docker-compose -f docker-compose.yml up -d redis
    
    log_success "Infrastructure services started"
}

# Wait for services to be ready
wait_for_services() {
    log_info "Waiting for services to be ready..."
    
    # Wait for Redis
    log_info "Waiting for Redis..."
    local redis_ready=false
    local attempts=0
    local max_attempts=30
    
    while [ $attempts -lt $max_attempts ] && [ "$redis_ready" = false ]; do
        if docker-compose -f docker-compose.yml exec -T redis redis-cli ping &> /dev/null; then
            redis_ready=true
            log_success "Redis is ready"
        else
            ((attempts++))
            log_info "Waiting for Redis... (attempt $attempts/$max_attempts)"
            sleep 2
        fi
    done
    
    if [ "$redis_ready" = false ]; then
        log_error "Redis failed to start within expected time"
        exit 1
    fi
}

# Check user service availability
check_auth_service() {
    log_info "Checking user service availability..."
    
    local auth_service_url="http://localhost:8080"
    
    if curl -f -s "$auth_service_url/actuator/health" &> /dev/null; then
        log_success "User service is running and accessible"
    else
        log_warning "User service is not running at $auth_service_url"
        log_info "Please start the user service first or update the configuration"
    fi
}

# Build the application
build_application() {
    log_info "Building gateway service application..."
    
    cd "$PROJECT_DIR"
    
    # Clean and compile
    mvn clean compile
    
    log_success "Application built successfully"
}

# Display setup information
display_setup_info() {
    log_success "Local development environment setup completed!"
    echo
    log_info "Environment Details:"
    echo "  - Profile: local"
    echo "  - Gateway Service Port: 8080"
    echo "  - Redis Port: 6379"
    echo "  - Environment File: $ENV_FILE"
    echo
    log_info "Next Steps:"
    echo "  1. Ensure user service is running on port 8080"
    echo "  2. Start the gateway service: mvn spring-boot:run -Dspring.profiles.active=local"
    echo "  3. Access Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  4. Check health: http://localhost:8080/actuator/health"
    echo
    log_info "Useful Commands:"
    echo "  - View logs: docker-compose logs -f redis"
    echo "  - Stop services: docker-compose down"
    echo "  - Rebuild: mvn clean package"
    echo
    log_info "Service Dependencies:"
    echo "  - User Service: http://localhost:8080"
    echo "  - Redis: localhost:6379"
}

# Main execution
main() {
    log_info "Setting up Gripday Gateway Service - Local Development Environment"
    echo
    
    check_prerequisites
    setup_environment_file
    start_infrastructure
    wait_for_services
    check_auth_service
    build_application
    display_setup_info
}

# Execute main function
main "$@"