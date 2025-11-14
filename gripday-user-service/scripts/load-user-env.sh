#!/bin/bash

# Environment variable loading and validation script for Gripday User Service
# Loads and validates environment variables for different deployment environments

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default environment
DEFAULT_ENV="local"
ENVIRONMENT="${1:-$DEFAULT_ENV}"

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

# Display usage information
usage() {
    echo "Usage: $0 [ENVIRONMENT]"
    echo
    echo "Parameters:"
    echo "  ENVIRONMENT - Target environment (local|staging|production, default: $DEFAULT_ENV)"
    echo
    echo "Examples:"
    echo "  $0 local      # Load local environment"
    echo "  $0 staging    # Load staging environment"
    echo "  $0 production # Load production environment"
    echo
    echo "This script loads environment variables from .env.[ENVIRONMENT] file"
    echo "and validates required variables for the user service."
}

# Check if help is requested
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

# Validate environment parameter
if [[ ! "$ENVIRONMENT" =~ ^(local|staging|production)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT"
    log_error "Valid environments: local, staging, production"
    exit 1
fi

# Environment file path
ENV_FILE="$PROJECT_DIR/.env.$ENVIRONMENT"

# Load environment file
load_environment_file() {
    log_info "Loading environment file: $ENV_FILE"
    
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Environment file not found: $ENV_FILE"
        log_error "Please create the environment file or run setup script first"
        exit 1
    fi
    
    # Source the environment file
    set -a  # Automatically export all variables
    source "$ENV_FILE"
    set +a  # Stop automatically exporting
    
    log_success "Environment file loaded successfully"
}

# Define required environment variables for each environment
get_required_variables() {
    local env="$1"
    
    case "$env" in
        "local")
            echo "SPRING_PROFILES_ACTIVE GRIPDAY_DATABASE_URL GRIPDAY_DATABASE_USERNAME GRIPDAY_DATABASE_PASSWORD GRIPDAY_CACHE_REDIS_HOST GRIPDAY_AUTH_JWT_SECRET"
            ;;
        "staging")
            echo "SPRING_PROFILES_ACTIVE GRIPDAY_DATABASE_URL GRIPDAY_DATABASE_USERNAME GRIPDAY_DATABASE_PASSWORD GRIPDAY_CACHE_REDIS_HOST GRIPDAY_CACHE_REDIS_PASSWORD GRIPDAY_AUTH_JWT_SECRET JWT_SECRET POSTGRES_PASSWORD REDIS_PASSWORD"
            ;;
        "production")
            echo "SPRING_PROFILES_ACTIVE GRIPDAY_DATABASE_URL GRIPDAY_DATABASE_USERNAME GRIPDAY_DATABASE_PASSWORD GRIPDAY_CACHE_REDIS_HOST GRIPDAY_CACHE_REDIS_PASSWORD GRIPDAY_AUTH_JWT_SECRET DB_HOST DB_USERNAME DB_PASSWORD REDIS_HOST JWT_SECRET GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET"
            ;;
    esac
}

# Validate required environment variables
validate_required_variables() {
    log_info "Validating required environment variables for $ENVIRONMENT environment..."
    
    local required_vars
    required_vars=$(get_required_variables "$ENVIRONMENT")
    
    local missing_vars=()
    local empty_vars=()
    
    for var in $required_vars; do
        if [ -z "${!var:-}" ]; then
            if [ -z "${!var+x}" ]; then
                missing_vars+=("$var")
            else
                empty_vars+=("$var")
            fi
        fi
    done
    
    # Report missing variables
    if [ ${#missing_vars[@]} -ne 0 ]; then
        log_error "Missing required environment variables:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
    fi
    
    # Report empty variables
    if [ ${#empty_vars[@]} -ne 0 ]; then
        log_error "Empty required environment variables:"
        for var in "${empty_vars[@]}"; do
            echo "  - $var"
        done
    fi
    
    # Exit if any required variables are missing or empty
    if [ ${#missing_vars[@]} -ne 0 ] || [ ${#empty_vars[@]} -ne 0 ]; then
        log_error "Please set all required environment variables in $ENV_FILE"
        exit 1
    fi
    
    log_success "All required environment variables are set"
}

# Validate environment-specific configurations
validate_environment_config() {
    log_info "Validating environment-specific configuration..."
    
    # Validate Spring profile matches environment
    if [ "$SPRING_PROFILES_ACTIVE" != "$ENVIRONMENT" ]; then
        log_warning "Spring profile ($SPRING_PROFILES_ACTIVE) does not match environment ($ENVIRONMENT)"
    fi
    
    # Validate database URL format
    if [[ ! "$GRIPDAY_DATABASE_URL" =~ ^jdbc:postgresql:// ]]; then
        log_error "Invalid database URL format: $GRIPDAY_DATABASE_URL"
        log_error "Expected format: jdbc:postgresql://host:port/database"
        exit 1
    fi
    
    # Validate JWT secret strength for production
    if [ "$ENVIRONMENT" = "production" ]; then
        if [ ${#GRIPDAY_AUTH_JWT_SECRET} -lt 32 ]; then
            log_error "JWT secret is too short for production (minimum 32 characters)"
            exit 1
        fi
        
        if [[ "$GRIPDAY_AUTH_JWT_SECRET" == *"development"* ]] || [[ "$GRIPDAY_AUTH_JWT_SECRET" == *"local"* ]]; then
            log_error "JWT secret appears to be a development key, not suitable for production"
            exit 1
        fi
    fi
    
    log_success "Environment-specific configuration is valid"
}

# Display loaded configuration summary
display_configuration_summary() {
    log_info "Configuration Summary for $ENVIRONMENT environment:"
    echo "  - Spring Profile: $SPRING_PROFILES_ACTIVE"
    echo "  - Database URL: $GRIPDAY_DATABASE_URL"
    echo "  - Database User: $GRIPDAY_DATABASE_USERNAME"
    echo "  - Redis Host: $GRIPDAY_CACHE_REDIS_HOST"
    echo "  - Redis Port: ${GRIPDAY_CACHE_REDIS_PORT:-6379}"
    echo "  - Redis Database: ${GRIPDAY_CACHE_REDIS_DATABASE:-0}"
    
    if [ -n "${GRIPDAY_OBSERVABILITY_TRACING_ENDPOINT:-}" ]; then
        echo "  - Tracing Endpoint: $GRIPDAY_OBSERVABILITY_TRACING_ENDPOINT"
    fi
    
    # Show OAuth2 configuration if enabled
    if [ "${GRIPDAY_AUTH_OAUTH2_GOOGLE_ENABLED:-false}" = "true" ]; then
        echo "  - Google OAuth2: Enabled"
        if [ -n "${GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID:-}" ]; then
            echo "    - Client ID: ${GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID:0:10}..."
        fi
    else
        echo "  - Google OAuth2: Disabled"
    fi
}

# Export environment variables for use by other scripts
export_variables() {
    log_info "Exporting environment variables..."
    
    # Export all GRIPDAY_ prefixed variables
    while IFS='=' read -r name value; do
        if [[ "$name" =~ ^GRIPDAY_ ]]; then
            export "$name=$value"
        fi
    done < "$ENV_FILE"
    
    # Export other important variables
    export SPRING_PROFILES_ACTIVE
    export POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
    export REDIS_PASSWORD
    
    log_success "Environment variables exported"
}

# Main execution
main() {
    log_info "Loading Gripday User Service environment: $ENVIRONMENT"
    echo
    
    load_environment_file
    validate_required_variables
    validate_environment_config
    export_variables
    
    echo
    display_configuration_summary
    
    log_success "Environment loaded and validated successfully!"
    
    # Provide usage instructions
    echo
    log_info "To use these environment variables in your current shell:"
    echo "  source $0 $ENVIRONMENT"
    echo
    log_info "To start the user service with this environment:"
    echo "  mvn spring-boot:run -Dspring.profiles.active=$ENVIRONMENT"
}

# Execute main function
main "$@"