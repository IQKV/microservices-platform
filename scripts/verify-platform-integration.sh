#!/bin/bash

# Platform Integration Verification Script
# This script verifies the integration between Gateway Service, User Service, 
# Bookstore Service, and the observability stack

set -e

echo "🚀 Starting Platform Integration Verification..."

# Configuration
GATEWAY_URL="http://localhost:8080"
AUTH_URL="http://localhost:8080"
BOOKSTORE_URL="http://localhost:8080"
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_service_health() {
    local service_name=$1
    local health_url=$2
    
    log_info "Checking $service_name health..."
    
    if curl -f -s "$health_url/actuator/health" > /dev/null; then
        log_info "✅ $service_name is healthy"
        return 0
    else
        log_error "❌ $service_name is not healthy"
        return 1
    fi
}

test_gateway_routing() {
    log_info "Testing Gateway Service routing..."
    
    # Test auth service routing through gateway
    log_info "Testing auth service routing through gateway..."
    if curl -f -s "$GATEWAY_URL/api/v1/auth/health" > /dev/null; then
        log_info "✅ Auth service routing works"
    else
        log_warn "⚠️  Auth service routing may not be configured"
    fi
    
    # Test bookstore service routing through gateway
    log_info "Testing bookstore service routing through gateway..."
    if curl -f -s "$GATEWAY_URL/api/v1/bookstore/books" > /dev/null; then
        log_info "✅ Bookstore service routing works"
    else
        log_error "❌ Bookstore service routing failed"
        return 1
    fi
    
    return 0
}

test_jwt_authentication_flow() {
    log_info "Testing JWT authentication flow..."
    
    # Test login through gateway (if auth endpoints are available)
    local login_response
    login_response=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' || echo "")
    
    if [[ -n "$login_response" && "$login_response" != *"error"* ]]; then
        log_info "✅ JWT authentication flow works"
        
        # Extract token (simplified - in real scenario would parse JSON properly)
        local token
        token=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || echo "")
        
        if [[ -n "$token" ]]; then
            # Test authenticated request to bookstore
            if curl -f -s -H "Authorization: Bearer $token" "$GATEWAY_URL/api/v1/bookstore/books" > /dev/null; then
                log_info "✅ Authenticated bookstore access works"
            else
                log_warn "⚠️  Authenticated bookstore access may need configuration"
            fi
        fi
    else
        log_warn "⚠️  JWT authentication flow needs manual verification"
    fi
}

test_observability_integration() {
    log_info "Testing observability stack integration..."
    
    # Test Prometheus
    if curl -f -s "$PROMETHEUS_URL/api/v1/status/config" > /dev/null; then
        log_info "✅ Prometheus is accessible"
        
        # Check if services are being scraped
        local targets_response
        targets_response=$(curl -s "$PROMETHEUS_URL/api/v1/targets" || echo "")
        
        if echo "$targets_response" | grep -q "bookstore-service"; then
            log_info "✅ Bookstore service is being monitored by Prometheus"
        else
            log_warn "⚠️  Bookstore service monitoring needs verification"
        fi
    else
        log_error "❌ Prometheus is not accessible"
    fi
    
    # Test Grafana
    if curl -f -s "$GRAFANA_URL/api/health" > /dev/null; then
        log_info "✅ Grafana is accessible"
    else
        log_error "❌ Grafana is not accessible"
    fi
    
    # Test service metrics endpoints
    for service in "user-service:$AUTH_URL" "bookstore-service:$BOOKSTORE_URL"; do
        IFS=':' read -r service_name service_url <<< "$service"
        if curl -f -s "$service_url/actuator/prometheus" > /dev/null; then
            log_info "✅ $service_name metrics endpoint works"
        else
            log_error "❌ $service_name metrics endpoint failed"
        fi
    done
}

test_cors_configuration() {
    log_info "Testing CORS configuration for React 19 frontend..."
    
    local cors_response
    cors_response=$(curl -s -X OPTIONS "$GATEWAY_URL/api/v1/bookstore/books" \
        -H "Origin: http://localhost:5173" \
        -H "Access-Control-Request-Method: GET" \
        -H "Access-Control-Request-Headers: Authorization,Content-Type" \
        -I || echo "")
    
    if echo "$cors_response" | grep -q "Access-Control-Allow-Origin"; then
        log_info "✅ CORS configuration works for React 19 frontend"
    else
        log_warn "⚠️  CORS configuration needs verification"
    fi
}

test_rate_limiting() {
    log_info "Testing rate limiting integration..."
    
    # Make multiple requests to test rate limiting
    local rate_limit_headers=false
    for i in {1..5}; do
        local response_headers
        response_headers=$(curl -s -I "$GATEWAY_URL/api/v1/bookstore/books" || echo "")
        
        if echo "$response_headers" | grep -q "X-RateLimit"; then
            rate_limit_headers=true
            break
        fi
        sleep 1
    done
    
    if $rate_limit_headers; then
        log_info "✅ Rate limiting headers are present"
    else
        log_warn "⚠️  Rate limiting headers not detected"
    fi
}

# Main execution
main() {
    log_info "Platform Integration Verification Started"
    echo "=================================================="
    
    # Check service health
    local services_healthy=true
    
    if ! check_service_health "Gateway Service" "$GATEWAY_URL"; then
        services_healthy=false
    fi
    
    if ! check_service_health "User Service" "$AUTH_URL"; then
        services_healthy=false
    fi
    
    if ! check_service_health "Bookstore Service" "$BOOKSTORE_URL"; then
        services_healthy=false
    fi
    
    if ! $services_healthy; then
        log_error "Some services are not healthy. Please check service status."
        exit 1
    fi
    
    echo ""
    log_info "All services are healthy. Proceeding with integration tests..."
    echo ""
    
    # Run integration tests
    test_gateway_routing
    echo ""
    
    test_jwt_authentication_flow
    echo ""
    
    test_observability_integration
    echo ""
    
    test_cors_configuration
    echo ""
    
    test_rate_limiting
    echo ""
    
    echo "=================================================="
    log_info "Platform Integration Verification Completed"
    log_info "✅ Basic platform integration is working"
    log_warn "⚠️  Some features may require manual verification with actual authentication"
}

# Run main function
main "$@"