# Auth Service Configuration Guide

## Overview

The Gripday Auth Service uses YAML-only configuration with environment-specific profiles and the `gripday.` prefix convention for all custom properties.

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

### Database Configuration (`gripday.database`)

```yaml
gripday:
  database:
    url: ${GRIPDAY_DATABASE_URL}
    username: ${GRIPDAY_DATABASE_USERNAME}
    password: ${GRIPDAY_DATABASE_PASSWORD}
    pool:
      maximum-size: 20
      minimum-idle: 5
      connection-timeout: PT30S
      idle-timeout: PT10M
      max-lifetime: PT30M
    migration:
      enabled: true
      contexts: staging
      validate-on-migrate: true
```

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
      key-prefix: "gripday:auth:staging"
      default-ttl: PT30M
      enable-statistics: true
```

### Authentication Configuration (`gripday.auth`)

```yaml
gripday:
  auth:
    jwt:
      secret-key: ${GRIPDAY_AUTH_JWT_SECRET}
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
      issuer: gripday-auth-service
      audience: gripday-services
      algorithm: RS256
    security:
      password:
        encoder-strength: 12
        require-special-chars: true
        min-length: 8
      rate-limiting:
        login-attempts: 5
        lockout-duration: PT15M
      session:
        timeout: PT4H
        concurrent-sessions: 3
    oauth2:
      enabled: true
      providers:
        google:
          client-id: ${GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID}
          client-secret: ${GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_SECRET}
          enabled: ${GRIPDAY_AUTH_OAUTH2_GOOGLE_ENABLED:false}
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
      prefix: gripday_auth_staging
      include-host-tag: true
      include-application-tag: true
      include-environment-tag: true
      custom-tags:
        environment: staging
        service: auth-service
    logging:
      level: INFO
      format: json
      include-correlation-id: true
      include-trace-id: true
      include-span-id: true
      include-user-id: true
      include-tenant-id: true
      correlation-id-header: X-Correlation-ID
      request-id-header: X-Request-ID
      tenant-id-header: X-Tenant-ID
      enable-sql-logging: false
      enable-security-events: true
      enable-performance-logging: true
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

| Variable                    | Description               | Example                                         |
| --------------------------- | ------------------------- | ----------------------------------------------- |
| `GRIPDAY_DATABASE_URL`      | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/gripday_auth` |
| `GRIPDAY_DATABASE_USERNAME` | Database username         | `gripday_user`                                  |
| `GRIPDAY_DATABASE_PASSWORD` | Database password         | `secure_password`                               |
| `GRIPDAY_CACHE_REDIS_HOST`  | Redis host                | `localhost`                                     |
| `GRIPDAY_AUTH_JWT_SECRET`   | JWT signing secret        | `your-secure-secret-key`                        |

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
java -Dspring.profiles.active=staging -jar auth-service.jar

# Docker environment
docker run -e SPRING_PROFILES_ACTIVE=production auth-service
```
