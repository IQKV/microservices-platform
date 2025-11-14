#!/bin/bash

# Database setup and migration script for Gripday Auth Service
# Handles database initialization, schema creation, and Liquibase migrations

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default configuration
DEFAULT_ENV="local"
DEFAULT_FORCE="false"

# Script parameters
ENVIRONMENT="${1:-$DEFAULT_ENV}"
FORCE="${2:-$DEFAULT_FORCE}"

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
    echo "Usage: $0 [ENVIRONMENT] [FORCE]"
    echo
    echo "Parameters:"
    echo "  ENVIRONMENT - Target environment (local|staging|production, default: $DEFAULT_ENV)"
    echo "  FORCE       - Force database recreation (true|false, default: $DEFAULT_FORCE)"
    echo
    echo "Examples:"
    echo "  $0 local           # Setup local database"
    echo "  $0 staging         # Setup staging database"
    echo "  $0 production true # Force setup production database"
    echo
    echo "This script initializes the database and runs Liquibase migrations"
    echo "for the specified environment."
}

# Check if help is requested
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

# Validate parameters
if [[ ! "$ENVIRONMENT" =~ ^(local|staging|production)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT"
    log_error "Valid environments: local, staging, production"
    exit 1
fi

if [[ ! "$FORCE" =~ ^(true|false)$ ]]; then
    log_error "Invalid FORCE value: $FORCE"
    log_error "Valid values: true, false"
    exit 1
fi

# Load environment variables
load_environment() {
    log_info "Loading environment configuration for $ENVIRONMENT..."
    
    local env_file="$PROJECT_DIR/.env.$ENVIRONMENT"
    
    if [ ! -f "$env_file" ]; then
        log_error "Environment file not found: $env_file"
        log_error "Please run setup script first or create the environment file"
        exit 1
    fi
    
    # Source environment file
    set -a
    source "$env_file"
    set +a
    
    log_success "Environment configuration loaded"
}

# Check database prerequisites
check_prerequisites() {
    log_info "Checking database prerequisites..."
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is required but not installed"
        exit 1
    fi
    
    # Check if PostgreSQL client is available (optional)
    if command -v psql &> /dev/null; then
        log_info "PostgreSQL client found"
    else
        log_warning "PostgreSQL client not found (psql command not available)"
    fi
    
    # Check if Docker is available for local environment
    if [ "$ENVIRONMENT" = "local" ] && ! command -v docker &> /dev/null; then
        log_error "Docker is required for local environment setup"
        exit 1
    fi
    
    log_success "Prerequisites check completed"
}

# Start database service for local environment
start_local_database() {
    if [ "$ENVIRONMENT" = "local" ]; then
        log_info "Starting local PostgreSQL database..."
        
        cd "$PROJECT_DIR"
        
        # Check if database is already running
        if docker-compose ps postgres | grep -q "Up"; then
            log_info "PostgreSQL is already running"
        else
            docker-compose up -d postgres
            log_success "PostgreSQL started"
        fi
        
        # Wait for database to be ready
        log_info "Waiting for PostgreSQL to be ready..."
        local attempts=0
        local max_attempts=30
        
        while [ $attempts -lt $max_attempts ]; do
            if docker-compose exec -T postgres pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" &> /dev/null; then
                log_success "PostgreSQL is ready"
                break
            else
                ((attempts++))
                log_info "Waiting for PostgreSQL... (attempt $attempts/$max_attempts)"
                sleep 2
            fi
        done
        
        if [ $attempts -eq $max_attempts ]; then
            log_error "PostgreSQL failed to start within expected time"
            exit 1
        fi
    fi
}

# Test database connection
test_database_connection() {
    log_info "Testing database connection..."
    
    # Extract database details from URL
    local db_host db_port db_name
    if [[ "$GRIPDAY_DATABASE_URL" =~ jdbc:postgresql://([^:]+):([0-9]+)/(.+) ]]; then
        db_host="${BASH_REMATCH[1]}"
        db_port="${BASH_REMATCH[2]}"
        db_name="${BASH_REMATCH[3]}"
    else
        log_error "Invalid database URL format: $GRIPDAY_DATABASE_URL"
        exit 1
    fi
    
    # Test connection using psql if available
    if command -v psql &> /dev/null; then
        if PGPASSWORD="$GRIPDAY_DATABASE_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$GRIPDAY_DATABASE_USERNAME" -d "$db_name" -c "SELECT 1;" &> /dev/null; then
            log_success "Database connection successful"
        else
            log_error "Failed to connect to database"
            log_error "Host: $db_host, Port: $db_port, Database: $db_name, User: $GRIPDAY_DATABASE_USERNAME"
            exit 1
        fi
    else
        log_info "Skipping direct database connection test (psql not available)"
    fi
}

# Create database if it doesn't exist
create_database_if_needed() {
    if [ "$FORCE" = "true" ] || [ "$ENVIRONMENT" = "local" ]; then
        log_info "Ensuring database exists..."
        
        # For local environment, database creation is handled by Docker Compose
        if [ "$ENVIRONMENT" = "local" ]; then
            log_info "Database creation handled by Docker Compose"
        else
            log_warning "Database creation for $ENVIRONMENT environment should be handled by infrastructure team"
        fi
    fi
}

# Run Liquibase migrations
run_migrations() {
    log_info "Running Liquibase database migrations..."
    
    cd "$PROJECT_DIR"
    
    # Set Maven properties for Liquibase
    local maven_props=""
    maven_props="$maven_props -Dspring.profiles.active=$ENVIRONMENT"
    maven_props="$maven_props -Dliquibase.url=$GRIPDAY_DATABASE_URL"
    maven_props="$maven_props -Dliquibase.username=$GRIPDAY_DATABASE_USERNAME"
    maven_props="$maven_props -Dliquibase.password=$GRIPDAY_DATABASE_PASSWORD"
    
    # Run Liquibase update
    if mvn liquibase:update $maven_props; then
        log_success "Database migrations completed successfully"
    else
        log_error "Database migrations failed"
        exit 1
    fi
}

# Validate database schema
validate_schema() {
    log_info "Validating database schema..."
    
    cd "$PROJECT_DIR"
    
    # Set Maven properties for Liquibase validation
    local maven_props=""
    maven_props="$maven_props -Dspring.profiles.active=$ENVIRONMENT"
    maven_props="$maven_props -Dliquibase.url=$GRIPDAY_DATABASE_URL"
    maven_props="$maven_props -Dliquibase.username=$GRIPDAY_DATABASE_USERNAME"
    maven_props="$maven_props -Dliquibase.password=$GRIPDAY_DATABASE_PASSWORD"
    
    # Validate database against changelog
    if mvn liquibase:validate $maven_props; then
        log_success "Database schema validation passed"
    else
        log_error "Database schema validation failed"
        exit 1
    fi
}

# Display database information
display_database_info() {
    log_info "Database Setup Summary:"
    echo "  - Environment: $ENVIRONMENT"
    echo "  - Database URL: $GRIPDAY_DATABASE_URL"
    echo "  - Database User: $GRIPDAY_DATABASE_USERNAME"
    echo "  - Migration Context: $ENVIRONMENT"
    echo "  - Force Recreation: $FORCE"
    
    if [ "$ENVIRONMENT" = "local" ]; then
        echo
        log_info "Local Database Access:"
        echo "  - Docker Container: Run 'docker-compose exec postgres psql -U $GRIPDAY_DATABASE_USERNAME -d ${POSTGRES_DB}'"
        echo "  - Direct Connection: psql -h localhost -p 5432 -U $GRIPDAY_DATABASE_USERNAME -d ${POSTGRES_DB}"
    fi
}

# Main execution
main() {
    log_info "Setting up Gripday Auth Service Database - $ENVIRONMENT Environment"
    echo
    
    load_environment
    check_prerequisites
    start_local_database
    test_database_connection
    create_database_if_needed
    run_migrations
    validate_schema
    
    echo
    display_database_info
    
    log_success "Database setup completed successfully!"
    
    echo
    log_info "Next Steps:"
    echo "  1. Start the auth service: mvn spring-boot:run -Dspring.profiles.active=$ENVIRONMENT"
    echo "  2. Verify database tables were created"
    echo "  3. Check application logs for any database-related issues"
}

# Handle script interruption
trap 'log_warning "Database setup interrupted"; exit 130' INT TERM

# Execute main function
main "$@"