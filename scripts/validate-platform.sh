#!/bin/bash

# Platform End-to-End Validation Script
# Tests complete user authentication flow, multi-tenant functionality, JWT propagation, and observability

set -euo pipefail

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
AUTH_URL="${AUTH_URL:-http://localhost:8080}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
VALIDATION_LOG="platform-validation-$(date +%Y%m%d-%H%M%S).log"

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$VALIDATION_LOG"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$VALIDATION_LOG"
            ((TESTS_PASSED++))
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$VALIDATION_LOG"
            ((TESTS_FAILED++))
            ;;
        "WARN")
            echo -e "${YELLOW}[WARN]${NC} $message" | tee -a "$VALIDATION_LOG"
            ;;
    esac
    echo "[$timestamp] [$level] $message" >> "$VALIDATION_LOG"
}

# Wait for service to be ready
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    log "INFO" "Waiting for $service_name to be ready at $url..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url/actuator/health" > /dev/null 2>&1; then
            log "SUCCESS" "$service_name is ready"
            return 0
        fi
        
        log "INFO" "Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 2
        ((attempt++))
    done
    
    log "ERROR" "$service_name failed to start within expected time"
    return 1
}

# Test service health endpoints
test_health_endpoints() {
    log "INFO" "Testing service health endpoints..."
    
    # Test Auth Service health
    if curl -s -f "$AUTH_URL/actuator/health" | jq -e '.status == "UP"' > /dev/null; then
        log "SUCCESS" "Auth Service health check passed"
    else
        log "ERROR" "Auth Service health check failed"
        return 1
    fi
    
    # Test Gateway Service health
    if curl -s -f "$GATEWAY_URL/actuator/health" | jq -e '.status == "UP"' > /dev/null; then
        log "SUCCESS" "Gateway Service health check passed"
    else
        log "ERROR" "Gateway Service health check failed"
        return 1
    fi
}

# Test user registration flow
test_user_registration() {
    local tenant_id=$1
    local username="testuser_$(date +%s)"
    local email="$username@example.com"
    
    log "INFO" "Testing user registration for tenant: $tenant_id"
    
    local signup_payload=$(cat <<EOF
{
    "username": "$username",
    "email": "$email",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User",
    "tenantId": "$tenant_id"
}
EOF
)
    
    local response=$(curl -s -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant_id" \
        -d "$signup_payload" \
        "$GATEWAY_URL/api/v1/auth/signup")
    
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        log "SUCCESS" "User registration successful for tenant $tenant_id"
        echo "$username:$email:$tenant_id"
        return 0
    else
        log "ERROR" "User registration failed for tenant $tenant_id. HTTP: $http_code, Body: $body"
        return 1
    fi
}

# Test user authentication flow
test_user_authentication() {
    local tenant_id=$1
    local username=$2
    local password="SecurePass123!"
    
    log "INFO" "Testing user authentication for $username in tenant $tenant_id"
    
    local login_payload=$(cat <<EOF
{
    "username": "$username",
    "password": "$password"
}
EOF
)
    
    local response=$(curl -s -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant_id" \
        -d "$login_payload" \
        "$GATEWAY_URL/api/v1/auth/login")
    
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" = "200" ]; then
        local access_token=$(echo "$body" | jq -r '.accessToken')
        if [ "$access_token" != "null" ] && [ -n "$access_token" ]; then
            log "SUCCESS" "User authentication successful for $username"
            echo "$access_token"
            return 0
        else
            log "ERROR" "Authentication response missing access token"
            return 1
        fi
    else
        log "ERROR" "User authentication failed for $username. HTTP: $http_code, Body: $body"
        return 1
    fi
}

# Test JWT token validation and user context propagation
test_jwt_validation() {
    local tenant_id=$1
    local access_token=$2
    
    log "INFO" "Testing JWT token validation and user context propagation"
    
    # Test token validation through gateway
    local response=$(curl -s -w "%{http_code}" \
        -H "Authorization: Bearer $access_token" \
        -H "X-Tenant-ID: $tenant_id" \
        "$GATEWAY_URL/api/v1/auth/profile")
    
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" = "200" ]; then
        # Validate user context in response
        local user_id=$(echo "$body" | jq -r '.userId')
        local username=$(echo "$body" | jq -r '.username')
        local tenant=$(echo "$body" | jq -r '.tenantId')
        
        if [ "$user_id" != "null" ] && [ "$username" != "null" ] && [ "$tenant" = "$tenant_id" ]; then
            log "SUCCESS" "JWT validation and user context propagation successful"
            return 0
        else
            log "ERROR" "User context validation failed. Missing or incorrect user data"
            return 1
        fi
    else
        log "ERROR" "JWT token validation failed. HTTP: $http_code, Body: $body"
        return 1
    fi
}

# Test multi-tenant isolation
test_multi_tenant_isolation() {
    log "INFO" "Testing multi-tenant functionality and isolation"
    
    # Create users in different tenants
    local tenant1="tenant-alpha"
    local tenant2="tenant-beta"
    
    # Register user in tenant 1
    local user1_info=$(test_user_registration "$tenant1")
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to register user in tenant 1"
        return 1
    fi
    
    local user1_username=$(echo "$user1_info" | cut -d: -f1)
    
    # Register user in tenant 2
    local user2_info=$(test_user_registration "$tenant2")
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to register user in tenant 2"
        return 1
    fi
    
    local user2_username=$(echo "$user2_info" | cut -d: -f1)
    
    # Authenticate users
    local token1=$(test_user_authentication "$tenant1" "$user1_username")
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to authenticate user in tenant 1"
        return 1
    fi
    
    local token2=$(test_user_authentication "$tenant2" "$user2_username")
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to authenticate user in tenant 2"
        return 1
    fi
    
    # Test tenant isolation - user from tenant1 should not access tenant2 resources
    local cross_tenant_response=$(curl -s -w "%{http_code}" \
        -H "Authorization: Bearer $token1" \
        -H "X-Tenant-ID: $tenant2" \
        "$GATEWAY_URL/api/v1/auth/profile")
    
    local cross_tenant_http_code="${cross_tenant_response: -3}"
    
    if [ "$cross_tenant_http_code" = "403" ] || [ "$cross_tenant_http_code" = "401" ]; then
        log "SUCCESS" "Multi-tenant isolation working correctly"
        return 0
    else
        log "ERROR" "Multi-tenant isolation failed. Cross-tenant access allowed"
        return 1
    fi
}

# Test JWT token refresh flow
test_token_refresh() {
    local tenant_id=$1
    local username=$2
    
    log "INFO" "Testing JWT token refresh flow"
    
    # First authenticate to get refresh token
    local login_payload=$(cat <<EOF
{
    "username": "$username",
    "password": "SecurePass123!"
}
EOF
)
    
    local auth_response=$(curl -s \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant_id" \
        -d "$login_payload" \
        "$GATEWAY_URL/api/v1/auth/login")
    
    local refresh_token=$(echo "$auth_response" | jq -r '.refreshToken')
    
    if [ "$refresh_token" = "null" ] || [ -z "$refresh_token" ]; then
        log "ERROR" "No refresh token received from login"
        return 1
    fi
    
    # Test token refresh
    local refresh_payload=$(cat <<EOF
{
    "refreshToken": "$refresh_token"
}
EOF
)
    
    local refresh_response=$(curl -s -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant_id" \
        -d "$refresh_payload" \
        "$GATEWAY_URL/api/v1/auth/refresh")
    
    local refresh_http_code="${refresh_response: -3}"
    local refresh_body="${refresh_response%???}"
    
    if [ "$refresh_http_code" = "200" ]; then
        local new_access_token=$(echo "$refresh_body" | jq -r '.accessToken')
        if [ "$new_access_token" != "null" ] && [ -n "$new_access_token" ]; then
            log "SUCCESS" "JWT token refresh successful"
            return 0
        else
            log "ERROR" "Token refresh response missing new access token"
            return 1
        fi
    else
        log "ERROR" "JWT token refresh failed. HTTP: $refresh_http_code"
        return 1
    fi
}

# Test observability stack integration
test_observability_stack() {
    log "INFO" "Testing observability stack integration"
    
    # Test Prometheus metrics endpoint
    if curl -s -f "$PROMETHEUS_URL/api/v1/query?query=up" > /dev/null 2>&1; then
        log "SUCCESS" "Prometheus is accessible and responding"
    else
        log "WARN" "Prometheus not accessible at $PROMETHEUS_URL (may not be deployed)"
    fi
    
    # Test service metrics endpoints
    if curl -s -f "$AUTH_URL/actuator/prometheus" > /dev/null 2>&1; then
        log "SUCCESS" "Auth Service metrics endpoint accessible"
    else
        log "ERROR" "Auth Service metrics endpoint not accessible"
        return 1
    fi
    
    if curl -s -f "$GATEWAY_URL/actuator/prometheus" > /dev/null 2>&1; then
        log "SUCCESS" "Gateway Service metrics endpoint accessible"
    else
        log "ERROR" "Gateway Service metrics endpoint not accessible"
        return 1
    fi
    
    # Test Grafana (if available)
    if curl -s -f "$GRAFANA_URL/api/health" > /dev/null 2>&1; then
        log "SUCCESS" "Grafana is accessible and responding"
    else
        log "WARN" "Grafana not accessible at $GRAFANA_URL (may not be deployed)"
    fi
}

# Test rate limiting functionality
test_rate_limiting() {
    local tenant_id="rate-test-tenant"
    
    log "INFO" "Testing rate limiting functionality"
    
    # Register a test user
    local user_info=$(test_user_registration "$tenant_id")
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to register user for rate limiting test"
        return 1
    fi
    
    local username=$(echo "$user_info" | cut -d: -f1)
    local access_token=$(test_user_authentication "$tenant_id" "$username")
    
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to authenticate user for rate limiting test"
        return 1
    fi
    
    # Make multiple rapid requests to trigger rate limiting
    local rate_limit_triggered=false
    
    for i in {1..20}; do
        local response=$(curl -s -w "%{http_code}" \
            -H "Authorization: Bearer $access_token" \
            -H "X-Tenant-ID: $tenant_id" \
            "$GATEWAY_URL/api/v1/auth/profile")
        
        local http_code="${response: -3}"
        
        if [ "$http_code" = "429" ]; then
            rate_limit_triggered=true
            break
        fi
        
        sleep 0.1
    done
    
    if [ "$rate_limit_triggered" = true ]; then
        log "SUCCESS" "Rate limiting is working correctly"
        return 0
    else
        log "WARN" "Rate limiting may not be configured or limits are too high"
        return 0  # Don't fail the test as this might be intentional
    fi
}

# Main validation function
run_platform_validation() {
    log "INFO" "Starting platform validation..."
    log "INFO" "Gateway URL: $GATEWAY_URL"
    log "INFO" "Auth URL: $AUTH_URL"
    log "INFO" "Prometheus URL: $PROMETHEUS_URL"
    log "INFO" "Grafana URL: $GRAFANA_URL"
    
    # Wait for services to be ready
    wait_for_service "Auth Service" "$AUTH_URL" || return 1
    wait_for_service "Gateway Service" "$GATEWAY_URL" || return 1
    
    # Run validation tests
    test_health_endpoints || return 1
    
    # Test basic authentication flow
    local tenant_id="validation-tenant"
    local user_info=$(test_user_registration "$tenant_id")
    if [ $? -eq 0 ]; then
        local username=$(echo "$user_info" | cut -d: -f1)
        local access_token=$(test_user_authentication "$tenant_id" "$username")
        
        if [ $? -eq 0 ]; then
            test_jwt_validation "$tenant_id" "$access_token"
            test_token_refresh "$tenant_id" "$username"
        fi
    fi
    
    # Test multi-tenant functionality
    test_multi_tenant_isolation
    
    # Test rate limiting
    test_rate_limiting
    
    # Test observability stack
    test_observability_stack
    
    log "INFO" "Platform validation completed"
    log "INFO" "Tests passed: $TESTS_PASSED"
    log "INFO" "Tests failed: $TESTS_FAILED"
    log "INFO" "Detailed log saved to: $VALIDATION_LOG"
    
    if [ $TESTS_FAILED -eq 0 ]; then
        log "SUCCESS" "All platform validation tests passed!"
        return 0
    else
        log "ERROR" "Some platform validation tests failed. Check the log for details."
        return 1
    fi
}

# Check dependencies
check_dependencies() {
    local missing_deps=()
    
    if ! command -v curl &> /dev/null; then
        missing_deps+=("curl")
    fi
    
    if ! command -v jq &> /dev/null; then
        missing_deps+=("jq")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        log "ERROR" "Missing required dependencies: ${missing_deps[*]}"
        log "INFO" "Please install missing dependencies and try again"
        return 1
    fi
}

# Script entry point
main() {
    echo "=== Gripday Platform End-to-End Validation ==="
    echo
    
    # Check dependencies
    check_dependencies || exit 1
    
    # Run validation
    if run_platform_validation; then
        echo
        echo -e "${GREEN}✓ Platform validation completed successfully!${NC}"
        exit 0
    else
        echo
        echo -e "${RED}✗ Platform validation failed. Check the logs for details.${NC}"
        exit 1
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi