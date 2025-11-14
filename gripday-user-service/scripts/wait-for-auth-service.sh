#!/bin/bash

# Health check script for Gripday Auth Service
# Waits for the auth service to be healthy and ready to accept requests

set -euo pipefail

# Default configuration
DEFAULT_HOST="localhost"
DEFAULT_PORT="8080"
DEFAULT_TIMEOUT="300"
DEFAULT_INTERVAL="5"

# Script parameters
HOST="${1:-$DEFAULT_HOST}"
PORT="${2:-$DEFAULT_PORT}"
TIMEOUT="${3:-$DEFAULT_TIMEOUT}"
INTERVAL="${4:-$DEFAULT_INTERVAL}"

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
    echo "Usage: $0 [HOST] [PORT] [TIMEOUT] [INTERVAL]"
    echo
    echo "Parameters:"
    echo "  HOST     - Auth service host (default: $DEFAULT_HOST)"
    echo "  PORT     - Auth service port (default: $DEFAULT_PORT)"
    echo "  TIMEOUT  - Maximum wait time in seconds (default: $DEFAULT_TIMEOUT)"
    echo "  INTERVAL - Check interval in seconds (default: $DEFAULT_INTERVAL)"
    echo
    echo "Examples:"
    echo "  $0                                    # Use all defaults"
    echo "  $0 auth-service 8080                 # Custom host and port"
    echo "  $0 localhost 8080 60 2              # All parameters"
    echo
    echo "Environment Variables:"
    echo "  AUTH_SERVICE_HOST - Override default host"
    echo "  AUTH_SERVICE_PORT - Override default port"
}

# Check if help is requested
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

# Override with environment variables if set
HOST="${AUTH_SERVICE_HOST:-$HOST}"
PORT="${AUTH_SERVICE_PORT:-$PORT}"

# Validate parameters
if ! [[ "$PORT" =~ ^[0-9]+$ ]] || [ "$PORT" -lt 1 ] || [ "$PORT" -gt 65535 ]; then
    log_error "Invalid port number: $PORT"
    exit 1
fi

if ! [[ "$TIMEOUT" =~ ^[0-9]+$ ]] || [ "$TIMEOUT" -lt 1 ]; then
    log_error "Invalid timeout value: $TIMEOUT"
    exit 1
fi

if ! [[ "$INTERVAL" =~ ^[0-9]+$ ]] || [ "$INTERVAL" -lt 1 ]; then
    log_error "Invalid interval value: $INTERVAL"
    exit 1
fi

# Service URLs
BASE_URL="http://$HOST:$PORT"
HEALTH_URL="$BASE_URL/actuator/health"
READY_URL="$BASE_URL/actuator/health/readiness"

# Check if curl is available
if ! command -v curl &> /dev/null; then
    log_error "curl is required but not installed"
    exit 1
fi

# Wait for service to be healthy
wait_for_health() {
    log_info "Waiting for auth service at $HOST:$PORT to be healthy..."
    log_info "Health check URL: $HEALTH_URL"
    log_info "Timeout: ${TIMEOUT}s, Check interval: ${INTERVAL}s"
    
    local start_time=$(date +%s)
    local end_time=$((start_time + TIMEOUT))
    local attempts=0
    
    while [ $(date +%s) -lt $end_time ]; do
        ((attempts++))
        
        # Check if service is responding
        if curl -f -s -m 10 "$HEALTH_URL" > /dev/null 2>&1; then
            log_success "Auth service is responding to health checks"
            
            # Check detailed health status
            local health_response
            health_response=$(curl -s -m 10 "$HEALTH_URL" 2>/dev/null || echo '{"status":"UNKNOWN"}')
            
            local status
            status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")
            
            if [ "$status" = "UP" ]; then
                log_success "Auth service is healthy (status: UP)"
                
                # Check readiness if endpoint exists
                if curl -f -s -m 5 "$READY_URL" > /dev/null 2>&1; then
                    local ready_response
                    ready_response=$(curl -s -m 5 "$READY_URL" 2>/dev/null || echo '{"status":"UNKNOWN"}')
                    
                    local ready_status
                    ready_status=$(echo "$ready_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")
                    
                    if [ "$ready_status" = "UP" ]; then
                        log_success "Auth service is ready to accept requests"
                        return 0
                    else
                        log_info "Auth service is healthy but not ready (readiness: $ready_status)"
                    fi
                else
                    log_success "Auth service is healthy and ready"
                    return 0
                fi
            else
                log_info "Auth service is responding but not healthy (status: $status)"
            fi
        else
            log_info "Attempt $attempts: Auth service not responding yet..."
        fi
        
        sleep "$INTERVAL"
    done
    
    log_error "Timeout reached after ${TIMEOUT}s. Auth service is not healthy."
    return 1
}

# Test basic connectivity
test_connectivity() {
    log_info "Testing basic connectivity to $HOST:$PORT..."
    
    if command -v nc &> /dev/null; then
        if nc -z "$HOST" "$PORT" 2>/dev/null; then
            log_success "Port $PORT is open on $HOST"
        else
            log_warning "Port $PORT is not accessible on $HOST"
        fi
    elif command -v telnet &> /dev/null; then
        if timeout 5 telnet "$HOST" "$PORT" </dev/null &>/dev/null; then
            log_success "Port $PORT is open on $HOST"
        else
            log_warning "Port $PORT is not accessible on $HOST"
        fi
    else
        log_info "Neither nc nor telnet available for port testing"
    fi
}

# Display service information
display_service_info() {
    log_info "Auth Service Information:"
    echo "  - Host: $HOST"
    echo "  - Port: $PORT"
    echo "  - Base URL: $BASE_URL"
    echo "  - Health URL: $HEALTH_URL"
    echo "  - Swagger UI: $BASE_URL/swagger-ui.html"
    echo "  - API Docs: $BASE_URL/api-docs"
}

# Main execution
main() {
    log_info "Gripday Auth Service Health Check"
    echo
    
    test_connectivity
    
    if wait_for_health; then
        echo
        display_service_info
        log_success "Auth service is healthy and ready!"
        exit 0
    else
        log_error "Auth service health check failed"
        exit 1
    fi
}

# Handle script interruption
trap 'log_warning "Health check interrupted"; exit 130' INT TERM

# Execute main function
main "$@"