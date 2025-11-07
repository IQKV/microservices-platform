# Gateway Service Configuration Guide

## Overview

The Gripday Gateway Service uses YAML-only configuration with environment-specific profiles and the `gripday.` prefix convention for all custom properties.

## Configuration Structure

### Environment Profiles

- **local**: Development environment with debug logging and relaxed security
- **staging**: Staging environment with moderate logging and security
- **production**: Production environment with minimal logging and maximum security

### Configuration Files

```
src/main/resources/
├── application.yml              # Base configuration
├── application-local.yml        # Local development
├── application-staging.yml      # Staging environment
└── application-production.yml   # Production environment
```

## Gripday Configuration Properties

All custom configuration properties use the `gripday.` prefix for clear namespace separation:

### Cache Configuration (`gripday.cache.redis`)

```yaml
gripday:
  cache:
    redis:
      host: ${GRIPDAY_CACHE_REDIS_HOST}
      port: ${GRIPDAY_CACHE_REDIS_PORT:6379}
      password: ${GRIPDAY_CACHE_REDIS_PASSWORD}
      database: ${GRIPDAY_CACHE_REDIS_DATABASE:1}
      timeout: PT3S
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
        max-wait: PT5S
      key-prefix: "gripday:gateway:staging"
      default-ttl: PT30M
      enable-statistics: true
```

### Gateway Configuration (`gripday.gateway`)

```yaml
gripday:
  gateway:
    routing:
      services:
        auth-service:
          uri: ${GRIPDAY_GATEWAY_ROUTING_AUTH_SERVICE_URI}
          path: /api/v1/auth/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
      enable-service-discovery: false
      load-balancing:
        strategy: round-robin
        enable-health-check: true
        health-check-interval: PT30S
    
    security:
      jwt:
        secret-key: ${GRIPDAY_GATEWAY_SECURITY_JWT_SECRET}
        access-token-expiry: PT15M
        refresh-token-expiry: P7D
        issuer: gripday-platform-staging
        audience: gripday-services-staging
        algorithm: RS256
      authentication:
        enabled: true
        auth-service-url: ${GRIPDAY_GATEWAY_SECURITY_AUTH_SERVICE_URL}
        token-validation-timeout: PT5S
        enable-user-context-propagation: true
      public-paths:
        - /api/v1/auth/login
        - /api/v1/auth/signup
        - /api/v1/auth/refresh
        - /api/v1/auth/email/verify
        - /api/v1/auth/email/resend
        - /api/v1/auth/email/status
        - /actuator/health
    
    rate-limiting:
      enabled: true
      redis:
        key-prefix: "gripday:gateway:rate-limit:staging"
        key-expiry: PT1M
      policies:
        default-requests-per-minute: 60
        default-burst-capacity: 100
        endpoints:
          "/api/v1/auth/login":
            requests-per-minute: 10
            burst-capacity: 20
            enable-tenant-quotas: true
      tenant-quotas:
        enabled: true
        default-tenant-requests-per-minute: 1000
    
    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50
      slow-call-rate-threshold: 50
      slow-call-duration-threshold: PT2S
      minimum-number-of-calls: 10
      wait-duration-in-open-state: PT30S
      sliding-window-size: 100
      sliding-window-type: COUNT_BASED
    
    cors:
      enabled: true
      allowed-origins:
        - ${GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS}
      allowed-methods:
        - GET
        - POST
        - PUT
        - PATCH
        - DELETE
        - OPTIONS
      allowed-headers:
        - Authorization
        - Content-Type
        - X-Correlation-ID
      allow-credentials: true
      max-age: 3600
```

### Observability Configuration (`gripday.observability`)

```yaml
gripday:
  observability:
    tracing:
      enabled: true
      service-name: ${spring.application.name}
      sampling-rate: 0.1
      endpoint: ${GRIPDAY_OBSERVABILITY_TRACING_ENDPOINT}
      timeout: PT10S
      export-timeout: PT30S
      batch-size: 512
    metrics:
      enabled: true
      path: /actuator/prometheus
      prefix: gripday_gateway_staging
      include-host-tag: true
      include-application-tag: true
      include-environment-tag: true
      custom-tags:
        environment: staging
        service: gateway-service
      enabled-metrics:
        - gateway.requests
        - gateway.response.time
        - gateway.circuit.breaker
        - gateway.rate.limit
    logging:
      level: INFO
      format: json
      enable-request-logging: true
      enable-response-logging: false
      enable-correlation-id: true
      include-trace-id: true
      include-span-id: true
      include-user-id: true
      include-tenant-id: true
      correlation-id-header: X-Correlation-ID
      request-id-header: X-Request-ID
      tenant-id-header: X-Tenant-ID
```

## Environment Variables

Use the provided `.env.example` file as a template:

```bash
# Copy and configure environment variables
cp .env.example .env.local
cp .env.example .env.staging
cp .env.example .env.production
```

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `GRIPDAY_CACHE_REDIS_HOST` | Redis host | `localhost` |
| `GRIPDAY_GATEWAY_ROUTING_AUTH_SERVICE_URI` | Auth service URL | `http://localhost:8081` |
| `GRIPDAY_GATEWAY_SECURITY_JWT_SECRET` | JWT signing secret | `your-secure-secret-key` |
| `GRIPDAY_GATEWAY_SECURITY_AUTH_SERVICE_URL` | Auth service URL | `http://localhost:8081` |
| `GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `https://app.pynity.com` |

## Configuration Validation

The service automatically validates:

- All custom properties use `gripday.` prefix
- YAML format compliance
- No `.properties` files are used
- Configuration property constraints

## Profile Activation

Set the active profile using:

```bash
# Environment variable
export SPRING_PROFILES_ACTIVE=local

# JVM argument
java -Dspring.profiles.active=staging -jar gateway-service.jar

# Docker environment
docker run -e SPRING_PROFILES_ACTIVE=production gateway-service
```