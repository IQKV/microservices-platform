#!/bin/bash

# Docker Compose Platform Validation Script
# Validates the platform when running via Docker Compose

set -euo pipefail

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
VALIDATION_TIMEOUT="${VALIDATION_TIMEOUT:-300}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} $message"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
        "WARN")
            echo -e "${YELLOW}[WARN]${NC} $message"
            ;;
    esac
}

# Check if Docker Compose is available
check_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        log "ERROR" "Docker Compose not found. Please install Docker Compose."
        return 1
    fi
    
    log "INFO" "Using Docker Compose command: $DOCKER_COMPOSE_CMD"
}

# Start the platform using Docker Compose
start_platform() {
    log "INFO" "Starting platform with Docker Compose..."
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log "ERROR" "Docker Compose file not found: $COMPOSE_FILE"
        return 1
    fi
    
    # Start services
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d
    
    if [ $? -eq 0 ]; then
        log "SUCCESS" "Platform services started successfully"
        return 0
    else
        log "ERROR" "Failed to start platform services"
        return 1
    fi
}

# Wait for services to be healthy
wait_for_services() {
    log "INFO" "Waiting for services to be healthy..."
    
    local max_attempts=$((VALIDATION_TIMEOUT / 10))
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        local all_healthy=true
        
        # Check auth service
        if ! $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" ps gripday-auth-service | grep -q "Up (healthy)"; then
            all_healthy=false
        fi
        
        # Check gateway service
        if ! $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" ps gripday-gateway-service | grep -q "Up (healthy)"; then
            all_healthy=false
        fi
        
        if [ "$all_healthy" = true ]; then
            log "SUCCESS" "All services are healthy"
            return 0
        fi
        
        log "INFO" "Attempt $attempt/$max_attempts: Services not ready yet..."
        sleep 10
        ((attempt++))
    done
    
    log "ERROR" "Services failed to become healthy within timeout"
    return 1
}

# Run platform validation
run_validation() {
    log "INFO" "Running platform validation..."
    
    # Set environment variables for validation script
    export GATEWAY_URL="http://localhost:8080"
    export AUTH_URL="http://localhost:8080"
    export PROMETHEUS_URL="http://localhost:9090"
    export GRAFANA_URL="http://localhost:3000"
    
    # Run the main validation script
    if [ -f "scripts/validate-platform.sh" ]; then
        bash scripts/validate-platform.sh
        return $?
    else
        log "ERROR" "Platform validation script not found: scripts/validate-platform.sh"
        return 1
    fi
}

# Show service logs
show_logs() {
    log "INFO" "Showing service logs..."
    
    echo
    echo "=== Auth Service Logs ==="
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" logs --tail=20 gripday-auth-service
    
    echo
    echo "=== Gateway Service Logs ==="
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" logs --tail=20 gripday-gateway-service
}

# Stop the platform
stop_platform() {
    log "INFO" "Stopping platform services..."
    
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" down
    
    if [ $? -eq 0 ]; then
        log "SUCCESS" "Platform services stopped successfully"
    else
        log "WARN" "Some issues occurred while stopping services"
    fi
}

# Cleanup function
cleanup() {
    log "INFO" "Cleaning up..."
    stop_platform
}

# Main function
main() {
    local start_services=true
    local stop_after_validation=true
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --no-start)
                start_services=false
                shift
                ;;
            --no-stop)
                stop_after_validation=false
                shift
                ;;
            --compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            --timeout)
                VALIDATION_TIMEOUT="$2"
                shift 2
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --no-start              Don't start services (assume they're already running)"
                echo "  --no-stop               Don't stop services after validation"
                echo "  --compose-file FILE     Use specific Docker Compose file (default: docker-compose.yml)"
                echo "  --timeout SECONDS       Validation timeout in seconds (default: 300)"
                echo "  -h, --help              Show this help message"
                exit 0
                ;;
            *)
                log "ERROR" "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    echo "=== Docker Compose Platform Validation ==="
    echo "Compose file: $COMPOSE_FILE"
    echo "Timeout: ${VALIDATION_TIMEOUT}s"
    echo
    
    # Check Docker Compose availability
    check_docker_compose || exit 1
    
    # Set up cleanup trap
    if [ "$stop_after_validation" = true ]; then
        trap cleanup EXIT
    fi
    
    # Start services if requested
    if [ "$start_services" = true ]; then
        start_platform || exit 1
        wait_for_services || {
            show_logs
            exit 1
        }
    fi
    
    # Run validation
    if run_validation; then
        log "SUCCESS" "Docker Compose platform validation completed successfully!"
        exit 0
    else
        log "ERROR" "Docker Compose platform validation failed!"
        show_logs
        exit 1
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi