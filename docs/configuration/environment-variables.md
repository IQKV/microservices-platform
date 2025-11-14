# Environment Variables Configuration

This document provides configuration examples for all environments and services in the Gripday microservices platform.

## Configuration Strategy

The platform uses Spring profiles for environment-specific configuration with the following hierarchy:

1. **Environment Variables** (highest priority)
2. **application-{profile}.yml** files
3. **application.yml** (default values)

All custom configuration properties use the `gripday.` prefix for clear namespace separation.

## Environment Profiles

| Profile      | Description             | Use Case                              |
| ------------ | ----------------------- | ------------------------------------- |
| `local`      | Development environment | Local development with Docker Compose |
| `staging`    | Staging environment     | Pre-production testing                |
| `production` | Production environment  | Live production deployment            |
| `test`       | Test environment        | Automated testing with Testcontainers |

## Common Environment Variables

### Database Configuration

```bash
# PostgreSQL Connection
GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/gripday_auth
GRIPDAY_DATABASE_USERNAME=gripday
GRIPDAY_DATABASE_PASSWORD=secure_password_123
GRIPDAY_DATABASE_DRIVER_CLASS_NAME=org.postgresql.Driver

# Connection Pool Settings
GRIPDAY_DATABASE_HIKARI_MAXIMUM_POOL_SIZE=20
GRIPDAY_DATABASE_HIKARI_MINIMUM_IDLE=5
GRIPDAY_DATABASE_HIKARI_CONNECTION_TIMEOUT=30000
GRIPDAY_DATABASE_HIKARI_IDLE_TIMEOUT=600000
GRIPDAY_DATABASE_HIKARI_MAX_LIFETIME=1800000
```

### Redis Configuration

```bash
# Redis Connection
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_PASSWORD=
GRIPDAY_CACHE_REDIS_DATABASE=0
GRIPDAY_CACHE_REDIS_TIMEOUT=2000ms

# Redis Pool Settings
GRIPDAY_CACHE_REDIS_LETTUCE_POOL_MAX_ACTIVE=8
GRIPDAY_CACHE_REDIS_LETTUCE_POOL_MAX_IDLE=8
GRIPDAY_CACHE_REDIS_LETTUCE_POOL_MIN_IDLE=0
```

### JWT Configuration

```bash
# JWT Settings
GRIPDAY_AUTH_JWT_SECRET=your_secret_key_minimum_256_bits_for_security
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D
GRIPDAY_AUTH_JWT_ISSUER=gripday-user-service
GRIPDAY_AUTH_JWT_ALGORITHM=RS256

# JWT Key Pair (for RS256)
GRIPDAY_AUTH_JWT_PRIVATE_KEY_PATH=/etc/ssl/private/jwt-private.pem
GRIPDAY_AUTH_JWT_PUBLIC_KEY_PATH=/etc/ssl/certs/jwt-public.pem
```

### Multi-Tenant Configuration

```bash
# Tenant Settings
GRIPDAY_TENANT_DEFAULT_ID=default
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID
GRIPDAY_TENANT_SUBDOMAIN_ENABLED=true
GRIPDAY_TENANT_ISOLATION_ENABLED=true
GRIPDAY_TENANT_CACHE_NAMESPACE_ENABLED=true
```

### Observability Configuration

```bash
# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
OTEL_SERVICE_NAME=gripday-user-service
OTEL_TRACES_SAMPLER=traceidratio
OTEL_TRACES_SAMPLER_ARG=0.1
OTEL_METRICS_EXPORTER=prometheus
OTEL_LOGS_EXPORTER=otlp

# Prometheus Metrics
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED=true
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

## Auth Service Configuration

### Local Development (.env.local)

```bash
# Service Configuration
GRIPDAY_AUTH_SERVICE_NAME=gripday-user-service
GRIPDAY_AUTH_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Database
GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/gripday_auth
GRIPDAY_DATABASE_USERNAME=gripday
GRIPDAY_DATABASE_PASSWORD=dev_password_123

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# JWT
GRIPDAY_AUTH_JWT_SECRET=dev_secret_key_change_in_production_minimum_256_bits
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D

# Security
GRIPDAY_AUTH_RATE_LIMITING_LOGIN_ATTEMPTS_PER_MINUTE=5
GRIPDAY_AUTH_RATE_LIMITING_SIGNUP_ATTEMPTS_PER_MINUTE=3
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_ATTEMPTS=5
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_DURATION=PT15M

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
LOGGING_PATTERN_CONSOLE=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# Liquibase
GRIPDAY_DATABASE_LIQUIBASE_CHANGE_LOG=classpath:db/changelog/db.changelog-master.xml
GRIPDAY_DATABASE_LIQUIBASE_ENABLED=true
```

### Staging Environment (.env.staging)

```bash
# Service Configuration
GRIPDAY_AUTH_SERVICE_NAME=gripday-user-service
GRIPDAY_AUTH_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Database
GRIPDAY_DATABASE_URL=jdbc:postgresql://postgres-staging:5432/gripday_auth
GRIPDAY_DATABASE_USERNAME=${DB_USERNAME}
GRIPDAY_DATABASE_PASSWORD=${DB_PASSWORD}

# Redis
GRIPDAY_CACHE_REDIS_HOST=redis-staging
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_PASSWORD=${REDIS_PASSWORD}

# JWT
GRIPDAY_AUTH_JWT_SECRET=${JWT_SECRET}
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D

# Security
GRIPDAY_AUTH_RATE_LIMITING_LOGIN_ATTEMPTS_PER_MINUTE=10
GRIPDAY_AUTH_RATE_LIMITING_SIGNUP_ATTEMPTS_PER_MINUTE=5
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_ATTEMPTS=5
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_DURATION=PT15M

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=INFO
LOGGING_LEVEL_ROOT=INFO
LOGGING_PATTERN_FILE=%d{ISO8601} [%thread] %-5level [%logger{36}] [%X{correlationId}] - %msg%n

# Observability
OTEL_SERVICE_NAME=gripday-user-service
OTEL_TRACES_SAMPLER_ARG=0.5
```

### Production Environment (.env.production)

```bash
# Service Configuration
GRIPDAY_AUTH_SERVICE_NAME=gripday-user-service
GRIPDAY_AUTH_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Database
GRIPDAY_DATABASE_URL=${DATABASE_URL}
GRIPDAY_DATABASE_USERNAME=${DB_USERNAME}
GRIPDAY_DATABASE_PASSWORD=${DB_PASSWORD}
GRIPDAY_DATABASE_HIKARI_MAXIMUM_POOL_SIZE=50
GRIPDAY_DATABASE_HIKARI_MINIMUM_IDLE=10

# Redis
GRIPDAY_CACHE_REDIS_HOST=${REDIS_HOST}
GRIPDAY_CACHE_REDIS_PORT=${REDIS_PORT}
GRIPDAY_CACHE_REDIS_PASSWORD=${REDIS_PASSWORD}
GRIPDAY_CACHE_REDIS_SSL=true

# JWT
GRIPDAY_AUTH_JWT_SECRET=${JWT_SECRET}
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D
GRIPDAY_AUTH_JWT_PRIVATE_KEY_PATH=${JWT_PRIVATE_KEY_PATH}
GRIPDAY_AUTH_JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}

# Security
GRIPDAY_AUTH_RATE_LIMITING_LOGIN_ATTEMPTS_PER_MINUTE=20
GRIPDAY_AUTH_RATE_LIMITING_SIGNUP_ATTEMPTS_PER_MINUTE=10
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_ATTEMPTS=3
GRIPDAY_AUTH_ACCOUNT_LOCKOUT_DURATION=PT30M

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID
GRIPDAY_TENANT_ISOLATION_ENABLED=true

# Logging (JSON format for production)
LOGGING_LEVEL_ORG_GRIPDAY=WARN
LOGGING_LEVEL_ROOT=WARN
LOGGING_CONFIG=classpath:logback-spring.xml

# Observability
OTEL_SERVICE_NAME=gripday-user-service
OTEL_TRACES_SAMPLER_ARG=1.0
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

## Gateway Service Configuration

### Local Development (.env.local)

```bash
# Service Configuration
GRIPDAY_GATEWAY_SERVICE_NAME=gripday-gateway-service
GRIPDAY_GATEWAY_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Gateway Routes
GRIPDAY_GATEWAY_AUTH_SERVICE_URL=http://localhost:8080
GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_URI=http://localhost:8080
GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_PREDICATES=Path=/api/v1/auth/**

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# Rate Limiting
GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE=100
GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY=20
GRIPDAY_GATEWAY_RATE_LIMITING_REPLENISH_RATE=10

# Circuit Breaker
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE=PT30S
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=10

# CORS
GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
GRIPDAY_GATEWAY_CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
GRIPDAY_GATEWAY_CORS_ALLOWED_HEADERS=*
GRIPDAY_GATEWAY_CORS_ALLOW_CREDENTIALS=true

# JWT
GRIPDAY_AUTH_JWT_SECRET=dev_secret_key_change_in_production_minimum_256_bits

# Multi-tenant
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID
GRIPDAY_TENANT_SUBDOMAIN_ENABLED=true

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB_REACTIVE=DEBUG
```

### Staging Environment (.env.staging)

```bash
# Service Configuration
GRIPDAY_GATEWAY_SERVICE_NAME=gripday-gateway-service
GRIPDAY_GATEWAY_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Gateway Routes
GRIPDAY_GATEWAY_AUTH_SERVICE_URL=http://auth-service-staging:8080
GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_URI=http://auth-service-staging:8080

# Redis
GRIPDAY_CACHE_REDIS_HOST=redis-staging
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_PASSWORD=${REDIS_PASSWORD}

# Rate Limiting
GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE=200
GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY=50

# Circuit Breaker
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=60
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE=PT60S

# CORS
GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS=https://gripday.website
GRIPDAY_GATEWAY_CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
GRIPDAY_GATEWAY_CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Tenant-ID

# JWT
GRIPDAY_AUTH_JWT_SECRET=${JWT_SECRET}

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=INFO
LOGGING_LEVEL_ROOT=INFO

# Observability
OTEL_SERVICE_NAME=gripday-gateway-service
OTEL_TRACES_SAMPLER_ARG=0.5
```

### Production Environment (.env.production)

```bash
# Service Configuration
GRIPDAY_GATEWAY_SERVICE_NAME=gripday-gateway-service
GRIPDAY_GATEWAY_SERVICE_VERSION=1.0.0
SERVER_PORT=8080

# Gateway Routes
GRIPDAY_GATEWAY_AUTH_SERVICE_URL=${AUTH_SERVICE_URL}
GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_URI=${AUTH_SERVICE_URL}

# Redis
GRIPDAY_CACHE_REDIS_HOST=${REDIS_HOST}
GRIPDAY_CACHE_REDIS_PORT=${REDIS_PORT}
GRIPDAY_CACHE_REDIS_PASSWORD=${REDIS_PASSWORD}
GRIPDAY_CACHE_REDIS_SSL=true

# Rate Limiting
GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE=500
GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY=100
GRIPDAY_GATEWAY_RATE_LIMITING_REPLENISH_RATE=50

# Circuit Breaker
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=70
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE=PT120S
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=20

# CORS
GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS=${ALLOWED_ORIGINS}
GRIPDAY_GATEWAY_CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
GRIPDAY_GATEWAY_CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Tenant-ID,X-Correlation-ID

# JWT
GRIPDAY_AUTH_JWT_SECRET=${JWT_SECRET}
GRIPDAY_AUTH_JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}

# Multi-tenant
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID
GRIPDAY_TENANT_SUBDOMAIN_ENABLED=true
GRIPDAY_TENANT_ISOLATION_ENABLED=true

# Logging (JSON format for production)
LOGGING_LEVEL_ORG_GRIPDAY=WARN
LOGGING_LEVEL_ROOT=WARN
LOGGING_CONFIG=classpath:logback-spring.xml

# Observability
OTEL_SERVICE_NAME=gripday-gateway-service
OTEL_TRACES_SAMPLER_ARG=1.0
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus,gateway
```

## YAML Configuration Examples

### Auth Service - application-local.yml

```yaml
gripday:
  auth:
    service:
      name: gripday-user-service
      version: 1.0.0
    jwt:
      secret: ${GRIPDAY_AUTH_JWT_SECRET:dev_secret_key_change_in_production_minimum_256_bits}
      access-token-expiry: ${GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY:PT15M}
      refresh-token-expiry: ${GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY:P7D}
      issuer: ${GRIPDAY_AUTH_JWT_ISSUER:gripday-user-service}
      algorithm: ${GRIPDAY_AUTH_JWT_ALGORITHM:HS256}
    rate-limiting:
      login-attempts-per-minute: ${GRIPDAY_AUTH_RATE_LIMITING_LOGIN_ATTEMPTS_PER_MINUTE:5}
      signup-attempts-per-minute: ${GRIPDAY_AUTH_RATE_LIMITING_SIGNUP_ATTEMPTS_PER_MINUTE:3}
    account-lockout:
      attempts: ${GRIPDAY_AUTH_ACCOUNT_LOCKOUT_ATTEMPTS:5}
      duration: ${GRIPDAY_AUTH_ACCOUNT_LOCKOUT_DURATION:PT15M}
  database:
    url: ${GRIPDAY_DATABASE_URL:jdbc:postgresql://localhost:5432/gripday_auth}
    username: ${GRIPDAY_DATABASE_USERNAME:gripday}
    password: ${GRIPDAY_DATABASE_PASSWORD:dev_password_123}
    driver-class-name: ${GRIPDAY_DATABASE_DRIVER_CLASS_NAME:org.postgresql.Driver}
    hikari:
      maximum-pool-size: ${GRIPDAY_DATABASE_HIKARI_MAXIMUM_POOL_SIZE:20}
      minimum-idle: ${GRIPDAY_DATABASE_HIKARI_MINIMUM_IDLE:5}
      connection-timeout: ${GRIPDAY_DATABASE_HIKARI_CONNECTION_TIMEOUT:30000}
    liquibase:
      change-log: ${GRIPDAY_DATABASE_LIQUIBASE_CHANGE_LOG:classpath:db/changelog/db.changelog-master.xml}
      enabled: ${GRIPDAY_DATABASE_LIQUIBASE_ENABLED:true}
  cache:
    redis:
      host: ${GRIPDAY_CACHE_REDIS_HOST:localhost}
      port: ${GRIPDAY_CACHE_REDIS_PORT:6379}
      password: ${GRIPDAY_CACHE_REDIS_PASSWORD:}
      database: ${GRIPDAY_CACHE_REDIS_DATABASE:0}
      timeout: ${GRIPDAY_CACHE_REDIS_TIMEOUT:2000ms}
  tenant:
    default-id: ${GRIPDAY_TENANT_DEFAULT_ID:default}
    header-name: ${GRIPDAY_TENANT_HEADER_NAME:X-Tenant-ID}
    subdomain-enabled: ${GRIPDAY_TENANT_SUBDOMAIN_ENABLED:false}
    isolation-enabled: ${GRIPDAY_TENANT_ISOLATION_ENABLED:true}

server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${gripday.auth.service.name}
  profiles:
    active: local
  datasource:
    url: ${gripday.database.url}
    username: ${gripday.database.username}
    password: ${gripday.database.password}
    driver-class-name: ${gripday.database.driver-class-name}
    hikari:
      maximum-pool-size: ${gripday.database.hikari.maximum-pool-size}
      minimum-idle: ${gripday.database.hikari.minimum-idle}
      connection-timeout: ${gripday.database.hikari.connection-timeout}
  data:
    redis:
      host: ${gripday.cache.redis.host}
      port: ${gripday.cache.redis.port}
      password: ${gripday.cache.redis.password}
      database: ${gripday.cache.redis.database}
      timeout: ${gripday.cache.redis.timeout}
  liquibase:
    change-log: ${gripday.database.liquibase.change-log}
    enabled: ${gripday.database.liquibase.enabled}

logging:
  level:
    org.gripday: ${LOGGING_LEVEL_ORG_GRIPDAY:DEBUG}
    org.springframework.security: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY:DEBUG}
    org.springframework.web: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB:DEBUG}
  pattern:
    console: ${LOGGING_PATTERN_CONSOLE:%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}

management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health,info,metrics}
  endpoint:
    health:
      show-details: always
```

### Gateway Service - application-local.yml

```yaml
gripday:
  gateway:
    service:
      name: gripday-gateway-service
      version: 1.0.0
    auth-service-url: ${GRIPDAY_GATEWAY_AUTH_SERVICE_URL:http://localhost:8080}
    rate-limiting:
      default-requests-per-minute: ${GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE:100}
      burst-capacity: ${GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY:20}
      replenish-rate: ${GRIPDAY_GATEWAY_RATE_LIMITING_REPLENISH_RATE:10}
    circuit-breaker:
      failure-rate-threshold: ${GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD:50}
      wait-duration-in-open-state: ${GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE:PT30S}
      sliding-window-size: ${GRIPDAY_GATEWAY_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE:10}
    cors:
      allowed-origins: ${GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
      allowed-methods: ${GRIPDAY_GATEWAY_CORS_ALLOWED_METHODS:GET,POST,PUT,PATCH,DELETE,OPTIONS}
      allowed-headers: ${GRIPDAY_GATEWAY_CORS_ALLOWED_HEADERS:*}
      allow-credentials: ${GRIPDAY_GATEWAY_CORS_ALLOW_CREDENTIALS:true}
    routes:
      auth-service:
        uri: ${GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_URI:http://localhost:8080}
        predicates: ${GRIPDAY_GATEWAY_ROUTES_AUTH_SERVICE_PREDICATES:Path=/api/v1/auth/**}
  cache:
    redis:
      host: ${GRIPDAY_CACHE_REDIS_HOST:localhost}
      port: ${GRIPDAY_CACHE_REDIS_PORT:6379}
      password: ${GRIPDAY_CACHE_REDIS_PASSWORD:}
      database: ${GRIPDAY_CACHE_REDIS_DATABASE:1}
  auth:
    jwt:
      secret: ${GRIPDAY_AUTH_JWT_SECRET:dev_secret_key_change_in_production_minimum_256_bits}
  tenant:
    header-name: ${GRIPDAY_TENANT_HEADER_NAME:X-Tenant-ID}
    subdomain-enabled: ${GRIPDAY_TENANT_SUBDOMAIN_ENABLED:false}

server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${gripday.gateway.service.name}
  profiles:
    active: local
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${gripday.gateway.routes.auth-service.uri}
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: ${gripday.gateway.rate-limiting.replenish-rate}
                redis-rate-limiter.burstCapacity: ${gripday.gateway.rate-limiting.burst-capacity}
      globalcors:
        cors-configurations:
          "[/**]":
            allowed-origins: ${gripday.gateway.cors.allowed-origins}
            allowed-methods: ${gripday.gateway.cors.allowed-methods}
            allowed-headers: ${gripday.gateway.cors.allowed-headers}
            allow-credentials: ${gripday.gateway.cors.allow-credentials}
  data:
    redis:
      host: ${gripday.cache.redis.host}
      port: ${gripday.cache.redis.port}
      password: ${gripday.cache.redis.password}
      database: ${gripday.cache.redis.database}

logging:
  level:
    org.gripday: ${LOGGING_LEVEL_ORG_GRIPDAY:DEBUG}
    org.springframework.cloud.gateway: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY:DEBUG}
    org.springframework.web.reactive: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB_REACTIVE:DEBUG}

management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health,info,metrics,gateway}
  endpoint:
    health:
      show-details: always
    gateway:
      enabled: true

resilience4j:
  circuitbreaker:
    instances:
      auth-service:
        failure-rate-threshold: ${gripday.gateway.circuit-breaker.failure-rate-threshold}
        wait-duration-in-open-state: ${gripday.gateway.circuit-breaker.wait-duration-in-open-state}
        sliding-window-size: ${gripday.gateway.circuit-breaker.sliding-window-size}
        minimum-number-of-calls: 5
```

## Security Configuration

### JWT Key Generation

```bash
# Generate RSA key pair for JWT signing (production)
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Set environment variables
export GRIPDAY_AUTH_JWT_PRIVATE_KEY_PATH=/etc/ssl/private/jwt-private.pem
export GRIPDAY_AUTH_JWT_PUBLIC_KEY_PATH=/etc/ssl/certs/jwt-public.pem
```

### Password Requirements

```bash
# Password validation configuration
GRIPDAY_AUTH_PASSWORD_MIN_LENGTH=8
GRIPDAY_AUTH_PASSWORD_REQUIRE_UPPERCASE=true
GRIPDAY_AUTH_PASSWORD_REQUIRE_LOWERCASE=true
GRIPDAY_AUTH_PASSWORD_REQUIRE_NUMBER=true
GRIPDAY_AUTH_PASSWORD_REQUIRE_SPECIAL_CHAR=true
```

## Validation and Testing

### Configuration Validation

```bash
# Validate configuration
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring.config.on-not-found=fail

# Test configuration loading
curl http://localhost:8080/actuator/configprops
curl http://localhost:8080/actuator/configprops
```

### Environment Variable Testing

```bash
# Test with environment variables
export GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/test_db
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Verify configuration
curl http://localhost:8080/actuator/env
```

This configuration provides a foundation for all deployment environments while maintaining security best practices and operational flexibility.
