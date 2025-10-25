# Platform Validation Scripts

This directory contains comprehensive validation scripts for the Gripday microservices platform. These scripts test the complete user authentication flow, multi-tenant functionality, JWT token propagation, and observability stack integration.

## Scripts Overview

### 1. validate-platform.sh
Core validation script that performs end-to-end testing of the platform functionality.

**Features:**
- Complete user authentication flow testing
- Multi-tenant functionality validation
- JWT token propagation and user context testing
- Rate limiting functionality verification
- Observability stack integration testing
- Comprehensive logging and reporting

**Usage:**
```bash
# Make executable (if needed)
chmod +x scripts/validate-platform.sh

# Run with default settings
./scripts/validate-platform.sh

# Run with custom URLs
GATEWAY_URL=http://localhost:8080 \
AUTH_URL=http://localhost:8081 \
PROMETHEUS_URL=http://localhost:9090 \
GRAFANA_URL=http://localhost:3000 \
./scripts/validate-platform.sh
```

### 2. validate-docker-compose.sh
Docker Compose specific validation that manages the platform lifecycle and runs validation.

**Features:**
- Automatic platform startup using Docker Compose
- Service health monitoring
- Complete validation suite execution
- Automatic cleanup and log collection
- Configurable timeout and compose file selection

**Usage:**
```bash
# Make executable (if needed)
chmod +x scripts/validate-docker-compose.sh

# Run full validation (start, test, stop)
./scripts/validate-docker-compose.sh

# Run validation on already running services
./scripts/validate-docker-compose.sh --no-start --no-stop

# Use custom compose file
./scripts/validate-docker-compose.sh --compose-file docker-compose.production.yml

# Set custom timeout
./scripts/validate-docker-compose.sh --timeout 600
```

**Options:**
- `--no-start`: Don't start services (assume they're already running)
- `--no-stop`: Don't stop services after validation
- `--compose-file FILE`: Use specific Docker Compose file
- `--timeout SECONDS`: Validation timeout in seconds
- `-h, --help`: Show help message

### 3. validate-kubernetes.sh
Kubernetes specific validation for deployed platform instances.

**Features:**
- Kubernetes deployment readiness verification
- Pod health monitoring
- Service URL discovery (LoadBalancer, NodePort, port-forward)
- Complete validation suite execution
- Kubernetes-specific logging and troubleshooting

**Usage:**
```bash
# Make executable (if needed)
chmod +x scripts/validate-kubernetes.sh

# Run validation in default namespace
./scripts/validate-kubernetes.sh

# Run validation in specific namespace
./scripts/validate-kubernetes.sh --namespace production

# Set custom timeout
./scripts/validate-kubernetes.sh --timeout 900

# Skip log collection on failure
./scripts/validate-kubernetes.sh --no-logs
```

**Options:**
- `--namespace NAME`: Kubernetes namespace (default: gripday)
- `--timeout SECONDS`: Validation timeout in seconds (default: 600)
- `--no-logs`: Don't show logs on failure
- `-h, --help`: Show help message

## Validation Test Coverage

### Authentication Flow Tests
- User registration with tenant isolation and email verification
- Email verification token validation and account activation
- User authentication with username/email (requires verified email)
- JWT token generation and validation
- Token refresh functionality
- User logout and token invalidation

### Multi-Tenant Functionality
- Tenant-specific user registration
- Cross-tenant access prevention
- Tenant context propagation
- Tenant isolation verification

### JWT Token and User Context
- JWT token structure validation
- User context claims verification
- Token expiration and refresh
- Cross-service token propagation

### Rate Limiting
- Rate limit trigger testing
- Tenant-specific rate limiting
- Rate limit response validation

### Observability Stack
- Prometheus metrics endpoint testing
- Grafana accessibility verification
- Service metrics collection validation
- Health check endpoint testing

## Environment Variables

### Core Service URLs
- `GATEWAY_URL`: Gateway service URL (default: http://localhost:8080)
- `AUTH_URL`: Auth service URL (default: http://localhost:8081)
- `PROMETHEUS_URL`: Prometheus URL (default: http://localhost:9090)
- `GRAFANA_URL`: Grafana URL (default: http://localhost:3000)

### Docker Compose Configuration
- `COMPOSE_FILE`: Docker Compose file path (default: docker-compose.yml)
- `VALIDATION_TIMEOUT`: Validation timeout in seconds (default: 300)

### Kubernetes Configuration
- `NAMESPACE`: Kubernetes namespace (default: gripday)
- `KUBECTL_CMD`: kubectl command (default: kubectl)

## Dependencies

### Required Tools
- `curl`: HTTP client for API testing
- `jq`: JSON processor for response parsing
- `docker-compose` or `docker compose`: For Docker Compose validation
- `kubectl`: For Kubernetes validation

### Installation Examples

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install curl jq
```

**macOS:**
```bash
brew install curl jq
```

**CentOS/RHEL:**
```bash
sudo yum install curl jq
```

## Output and Logging

### Console Output
- Color-coded log messages (INFO, SUCCESS, ERROR, WARN)
- Real-time validation progress
- Test result summary

### Log Files
- Detailed validation logs saved to `platform-validation-YYYYMMDD-HHMMSS.log`
- Complete request/response details
- Timestamp and correlation information

### Exit Codes
- `0`: All validations passed successfully
- `1`: One or more validations failed
- `1`: Missing dependencies or configuration errors

## Troubleshooting

### Common Issues

**Services Not Ready:**
- Increase timeout with `--timeout` option
- Check service logs for startup errors
- Verify network connectivity and port availability

**Authentication Failures:**
- Verify JWT configuration and secrets
- Check database connectivity and schema
- Validate tenant configuration
- Ensure email verification is completed for new users

**Rate Limiting Issues:**
- Adjust rate limiting configuration
- Check Redis connectivity
- Verify tenant-specific rate limits

**Observability Stack Issues:**
- Verify Prometheus and Grafana deployment
- Check metrics endpoint accessibility
- Validate observability configuration

### Debug Mode
Enable verbose logging by setting environment variables:
```bash
export DEBUG=1
export VERBOSE=1
./scripts/validate-platform.sh
```

### Manual Testing
For manual testing and debugging, you can run individual test functions:
```bash
# Source the script to access functions
source scripts/validate-platform.sh

# Run specific tests
test_health_endpoints
test_user_registration "test-tenant"
test_observability_stack
```

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Validate Platform
  run: |
    chmod +x scripts/validate-docker-compose.sh
    ./scripts/validate-docker-compose.sh --timeout 600
```

### Jenkins Pipeline Example
```groovy
stage('Platform Validation') {
    steps {
        sh 'chmod +x scripts/validate-docker-compose.sh'
        sh './scripts/validate-docker-compose.sh --timeout 600'
    }
}
```

### GitLab CI Example
```yaml
validate_platform:
  script:
    - chmod +x scripts/validate-docker-compose.sh
    - ./scripts/validate-docker-compose.sh --timeout 600
```

## Security Considerations

- Validation scripts create temporary test users and data
- Test data is isolated using unique tenant IDs and timestamps
- No production data is modified during validation
- JWT tokens are short-lived and automatically expire
- All test credentials use secure, randomly generated values

## Performance Impact

- Validation scripts are designed for minimal performance impact
- Test operations use lightweight requests and small data sets
- Rate limiting tests are controlled to avoid service disruption
- Observability tests only query metrics endpoints without heavy operations