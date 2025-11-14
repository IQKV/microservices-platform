#!/bin/bash

# Docker Compose Validation Script for Gateway Service
# Validates Docker Compose configurations for different environments

set -euo pipefail

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

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"

# Docker Compose files
COMPOSE_LOCAL="$SERVICE_DIR/docker-compose.yml"
COMPOSE_STAGING="$SERVICE_DIR/docker-compose.staging.yml"
COMPOSE_PRODUCTION="$SERVICE_DIR/docker-compose.production.yml"

# Environment files
ENV_LOCAL="$SERVICE_DIR/.env.local"
ENV_STAGING="$SERVICE_DIR/.env.staging"
ENV_PRODUCTION="$SERVICE_DIR/.env.production"

# Validate Docker Compose file syntax
validate_compose_syntax() {
    local compose_file="$1"
    local env_name="$2"
    
    log_info "Validating $env_name Docker Compose syntax..."
    
    if [ ! -f "$compose_file" ]; then
        log_error "Docker Compose file not found: $compose_file"
        return 1
    fi
    
    if docker-compose -f "$compose_file" config > /dev/null 2>&1; then
        log_success "$env_name Docker Compose syntax is valid"
        return 0
    else
        log_error "$env_name Docker Compose syntax validation failed"
        docker-compose -f "$compose_file" config
        return 1
    fi
}

# Validate environment file
validate_env_file() {
    local env_file="$1"
    local env_name="$2"
    
    log_info "Validating $env_name environment file..."
    
    if [ ! -f "$env_file" ]; then
        log_error "Environment file not found: $env_file"
        return 1
    fi
    
    # Check for required variables
    local required_vars=(
        "SPRING_PROFILES_ACTIVE"
        "GRIPDAY_CACHE_REDIS_HOST"
        "GRIPDAY_CACHE_REDIS_PORT"
        "GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI"
    )
    
    local missing_vars=()
    for var in "${required_vars[@]}"; do
        if ! grep -q "^$var=" "$env_file"; then
            missing_vars+=("$var")
        fi
    done
    
    if [ ${#missing_vars[@]} -eq 0 ]; then
        log_success "$env_name environment file contains all required variables"
        return 0
    else
        log_error "$env_name environment file missing variables: ${missing_vars[*]}"
        return 1
    fi
}

# Check Docker Compose services
check_compose_services() {
    local compose_file="$1"
    local env_name="$2"
    
    log_info "Checking $env_name Docker Compose services..."
    
    # Check if gateway-service exists
    if docker-compose -f "$compose_file" config | grep -q "gateway-service:"; then
        log_success "$env_name: gateway-service defined"
    else
        log_error "$env_name: gateway-service not defined"
        return 1
    fi
    
    # Check if Redis service exists
    if docker-compose -f "$compose_file" config | grep -q "redis"; then
        log_success "$env_name: Redis service defined"
    else
        log_error "$env_name: Redis service not defined"
        return 1
    fi
    
    # Check for health checks
    if docker-compose -f "$compose_file" config | grep -q "healthcheck:"; then
        log_success "$env_name: Health checks configured"
    else
        log_warning "$env_name: No health checks found"
    fi
    
    # Check for networks
    if docker-compose -f "$compose_file" config | grep -q "networks:"; then
        log_success "$env_name: Networks configured"
    else
        log_warning "$env_name: No custom networks found"
    fi
    
    return 0
}

# Validate required files exist
validate_required_files() {
    log_info "Checking required files..."
    
    local required_files=(
        "$SERVICE_DIR/Dockerfile"
        "$SERVICE_DIR/pom.xml"
        "$SERVICE_DIR/.dockerignore"
    )
    
    local missing_files=()
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            missing_files+=("$file")
        fi
    done
    
    if [ ${#missing_files[@]} -eq 0 ]; then
        log_success "All required files exist"
        return 0
    else
        log_error "Missing required files: ${missing_files[*]}"
        return 1
    fi
}

# Main validation function
main() {
    log_info "Gateway Service Docker Compose Validation"
    echo
    
    local validation_failed=false
    
    # Validate required files
    if ! validate_required_files; then
        validation_failed=true
    fi
    
    echo
    
    # Validate local environment
    if ! validate_compose_syntax "$COMPOSE_LOCAL" "Local"; then
        validation_failed=true
    fi
    if ! validate_env_file "$ENV_LOCAL" "Local"; then
        validation_failed=true
    fi
    if ! check_compose_services "$COMPOSE_LOCAL" "Local"; then
        validation_failed=true
    fi
    
    echo
    
    # Validate staging environment
    if ! validate_compose_syntax "$COMPOSE_STAGING" "Staging"; then
        validation_failed=true
    fi
    if ! validate_env_file "$ENV_STAGING" "Staging"; then
        validation_failed=true
    fi
    if ! check_compose_services "$COMPOSE_STAGING" "Staging"; then
        validation_failed=true
    fi
    
    echo
    
    # Validate production environment
    if ! validate_compose_syntax "$COMPOSE_PRODUCTION" "Production"; then
        validation_failed=true
    fi
    if ! validate_env_file "$ENV_PRODUCTION" "Production"; then
        validation_failed=true
    fi
    if ! check_compose_services "$COMPOSE_PRODUCTION" "Production"; then
        validation_failed=true
    fi
    
    echo
    
    if [ "$validation_failed" = true ]; then
        log_error "Docker Compose validation failed"
        exit 1
    else
        log_success "All Docker Compose configurations are valid"
        exit 0
    fi
}

# Display usage information
usage() {
    echo "Usage: $0"
    echo
    echo "This script validates Docker Compose configurations for the Gateway Service"
    echo "across all environments (local, staging, production)."
    echo
    echo "The script checks:"
    echo "  - Docker Compose file syntax"
    echo "  - Environment file completeness"
    echo "  - Required services configuration"
    echo "  - Health checks and networking"
}

# Check if help is requested
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    log_error "docker-compose is required but not installed"
    exit 1
fi

# Execute main function
main "$@"