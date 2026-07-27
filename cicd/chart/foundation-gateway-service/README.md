# iQ Key Value Gateway Service Helm Chart

API Gateway with intelligent routing, authentication, rate limiting, and comprehensive service orchestration for the iQ Key Value microservices platform.

## Overview

This Helm chart deploys the iQ Key Value Gateway Service, which provides:

- Centralized API gateway with intelligent routing
- JWT-based authentication and authorization
- Rate limiting and throttling protection
- Circuit breaker patterns for resilience
- CORS handling and security headers
- Request/response transformation
- Load balancing and service discovery
- Comprehensive observability and monitoring

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External Redis cache (for rate limiting and session storage)
- iQ Key Value User Service (for JWT validation)
- Downstream microservices (Billing, CRM services, etc.)

## Installation

### Basic Installation

```bash
helm install gateway-service ./foundation-gateway-service
```

### Production Installation with CI Pipeline

```bash
# Production deployment with all secrets passed via --set
helm upgrade --install gateway-service ./foundation-gateway-service \
  --values values-prd.yaml \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set config.gateway.security.jwt.secret="${JWT_SECRET_KEY}" \
  --namespace iqkv-prd-env \
  --create-namespace
```

## Configuration

### Critical Environment Variables for CI/CD

#### Essential Secrets (Required)

```bash
# JWT secret key (REQUIRED for authentication)
--set config.gateway.security.jwt.secret="${JWT_SECRET_KEY}"
```

#### Infrastructure Secrets (Optional)

```bash
# Redis password (if Redis auth is enabled)
--set infraServices.redis.password="${REDIS_PASSWORD}"
```

### Gateway Configuration

```yaml
config:
  gateway:
    routing:
      apiPrefix:
        enabled: true
        prefix: "/api"
        stripCount: 0
    security:
      jwt:
        secret: "override-via-set"
    cors:
      allowedOrigins:
        - "http://localhost:3000"
        - "https://app.example.com"
```

### Service Integration

```yaml
infraServices:
  # Required services
  userService:
    url: "http://foundation-iam-service"
    jwkUri: "http://foundation-iam-service/.well-known/jwks.json"

  # Optional services (enabled via profiles)
  billingService:
    url: "http://iqkv-billing-service"
    enabled: false

  crmServices:
    leadService:
      url: "http://iqkv-lead-service"
      enabled: true
    pipelineService:
      url: "http://iqkv-pipeline-service"
      enabled: true
    contactService:
      url: "http://iqkv-contact-service"
      enabled: true
```

## Key Features

### Intelligent Routing

- Path-based routing with configurable prefixes
- Service discovery and load balancing
- Health check integration
- Fallback and retry mechanisms

### Security

- JWT token validation and propagation
- Role-based access control (RBAC)
- Rate limiting per user/IP
- CORS policy enforcement
- Security headers injection

### Resilience

- Circuit breaker patterns
- Timeout and retry configuration
- Bulkhead isolation
- Graceful degradation

### Observability

- Request/response logging
- Metrics collection and export
- Distributed tracing
- Health check endpoints

## API Routing

The gateway routes requests to downstream services:

- `/api/v1/auth/**` → User Service
- `/api/v1/users/**` → User Service
- `/api/v1/billing/**` → Billing Service (if enabled)
- `/api/v1/leads/**` → Lead Service
- `/api/v1/contacts/**` → Contact Service
- `/api/v1/pipelines/**` → Pipeline Service

## Monitoring and Observability

### Key Metrics

- Request throughput and latency
- Error rates by service
- Circuit breaker status
- Rate limiting statistics
- Authentication success/failure rates

### Alerting Rules

- Gateway Service availability
- High error rates
- Circuit breaker open states
- High memory usage
- Authentication failures

## Security Considerations

1. **JWT Secret Management**
   - Use strong, randomly generated secrets
   - Rotate secrets regularly
   - Never commit secrets to version control

2. **Rate Limiting**
   - Configure appropriate limits per endpoint
   - Monitor for abuse patterns
   - Implement progressive penalties

3. **CORS Configuration**
   - Restrict origins to known domains
   - Avoid wildcard origins in production
   - Regularly review allowed origins

## Troubleshooting

### Common Issues

1. **JWT Validation Failures**
   - Verify JWT secret configuration
   - Check User Service connectivity
   - Ensure proper token format

2. **Service Routing Issues**
   - Verify downstream service URLs
   - Check service health endpoints
   - Monitor circuit breaker status

3. **Rate Limiting Problems**
   - Check Redis connectivity
   - Verify rate limit configuration
   - Monitor rate limit metrics

## Support

For issues and questions:

- **Documentation**: [iQ Key Value Documentation](https://docs.iqkv.site)
- **Issues**: [GitHub Issues](https://github.com/IQKV/foundation-gateway-service/issues)
- **Email**: team@iqkv.dev
