# 🌐 IQ Scaffold Gateway Service

> Reactive API gateway providing intelligent routing, JWT authentication, Redis-backed rate limiting, circuit breaker patterns, and user context propagation across microservices.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Overview](#overview)
- [What It Demonstrates](#what-it-demonstrates)
- [Architecture Patterns](#architecture-patterns)
- [Technical Highlights](#technical-highlights)
- [Use Cases Implemented](#use-cases-implemented)
- [API Examples](#api-examples)
- [Learning Points](#learning-points)
- [Adapting for Your Domain](#adapting-for-your-domain)
- [Integration with Downstream Services](#integration-with-downstream-services)

## Business Purpose

A centralized entry point for the IQ Scaffold microservices platform that handles:

- **Intelligent Routing** - Dynamic request routing to downstream services with path-based and header-based versioning
- **Authentication Gateway** - JWT validation and user context propagation to all protected services
- **Rate Limiting** - Redis-backed distributed rate limiting with tenant-specific quotas and burst capacity
- **Circuit Breaker** - Fault tolerance with automatic failure detection and graceful degradation
- **Multi-Tenancy** - Tenant context extraction from headers, JWT claims, or subdomain routing
- **Request Transformation** - Header enrichment, correlation ID generation, and context propagation

## Overview

This is the front door to the IQ Scaffold microservices ecosystem. Built on Spring Cloud Gateway with reactive programming, it provides a single entry point for all client requests while
handling cross-cutting concerns like authentication, rate limiting, and observability.

## What It Demonstrates

### 🌐 Reactive Gateway Patterns

- Spring Cloud Gateway with WebFlux for non-blocking I/O
- Reactive filter chains with ordered execution (GlobalFilter + Ordered)
- Backpressure handling for high-throughput scenarios
- Reactive Redis operations with ReactiveStringRedisTemplate
- Reactive JWT validation with OAuth2 Resource Server (RSA256)

### 🔐 Authentication & Authorization

- JWT validation using RSA256 with JWK Set endpoint
- User context extraction (userId, username, email, authorities, permissions, organizationId)
- Authority propagation via headers (X-User-Authorities, X-User-Email, X-User-Permissions, X-Organization-ID)
- Header sanitization to prevent spoofing attacks (removes all user/tenant context headers from incoming requests)
- Public path pattern matching (exact and wildcard /\*\*)
- Configurable user context propagation toggle
- MDC logging with user and tenant context

### 🚦 Rate Limiting Patterns

- Redis-backed sliding window log algorithm with sorted sets (ZSET)
- Dual-layer rate limiting (global IP-based + tenant-specific)
- Endpoint-specific rate limit policies with pattern matching
- Burst capacity handling (2x quota) for traffic spikes
- Configurable quotas per tenant via IqScaffoldProperties
- Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After)
- Automatic cleanup of expired entries with TTL

### 🔄 Circuit Breaker Implementation

- Resilience4j with reactive CircuitBreakerOperator
- Path-based circuit breaker selection (per service)
- Configurable failure rate and slow call thresholds
- Automatic state transitions (closed → open → half-open)
- Fallback responses with retry-after headers
- Sliding window for failure tracking (count-based or time-based)

### 🏢 Multi-Tenancy Support

- Priority-based tenant extraction (JWT claims → X-Tenant-ID header)
- Tenant-specific rate limit quotas with default fallback
- Tenant context stored in exchange attributes
- Tenant ID propagation via X-Tenant-ID header
- Tenant-scoped Redis keys for isolation
- Tenant quota monitoring service for analytics

### 🎯 Observability & Monitoring

- Correlation ID generation and propagation
- OpenTelemetry distributed tracing
- Prometheus metrics for gateway operations
- Structured JSON logging with MDC context
- Request/response logging with correlation tracking

## Architecture Patterns

### Reactive Filter Chain

```
Request Flow:
1. CorrelationIdFilter        → Generate/extract correlation ID
2. TenantExtractionFilter     → Extract tenant context
3. JwtAuthenticationFilter    → Validate JWT and extract user context
4. ApiVersionRoutingFilter    → Handle API versioning
5. TenantRateLimitingFilter   → Apply rate limits
6. CircuitBreakerFilter       → Fault tolerance
7. RequestTransformationFilter → Enrich headers
8. Route to downstream service
9. ResponseTransformationFilter → Clean response headers
```

### Gateway Patterns

- Gateway Aggregation pattern for unified API entry
- Filter Chain pattern for request processing
- Circuit Breaker pattern for fault tolerance
- Rate Limiting with sliding window algorithm
- Context Propagation via headers
- Service Discovery ready (configurable)

### API Design

- Centralized routing configuration in YAML
- Path-based and header-based API versioning
- Public vs protected endpoint segregation
- OpenAPI documentation aggregation
- Consistent error responses with Problem Details (RFC 7807)

## Technical Highlights

### Reactive Programming

- Non-blocking I/O with Project Reactor (Mono/Flux)
- Reactive Redis operations with ReactiveStringRedisTemplate
- ReactiveSecurityContextHolder for JWT validation
- Backpressure support for high load
- Efficient resource utilization (no thread blocking)
- Reactive filter chains with transformDeferred and flatMap

### Performance Optimization

- Redis connection pooling
- Reactive filter chains (no thread blocking)
- Efficient JWT validation with caching
- Sliding window rate limiting algorithm
- Circuit breaker prevents cascading failures

### Security Features

- JWT validation with RSA256 public key
- JWK Set endpoint integration
- CORS configuration per environment
- Security header injection
- Internal header removal from responses

### Configuration Management

- Type-safe configuration with Java records (IqScaffoldProperties)
- Nested record structure for organized config hierarchy
- Bean Validation annotations (@Min, @Max, @NotBlank, @Pattern)
- Environment-specific profiles (local, staging, production)
- Externalized service routing configuration
- Redis-backed distributed state with connection pooling

### Operational Features

- Docker containerization
- Health checks and actuator endpoints
- Graceful shutdown handling
- Structured JSON logging
- Prometheus metrics export

## Use Cases Implemented

### Request Routing

- Route requests to user-service (/api/v1/auth/**, /api/v1/users/me, /api/v1/admin/**)
- Route requests to billing-service (/api/v1/billing/**, /api/v1/admin/billing/**)
- Dynamic service registration support
- Load balancing across service instances
- Health check-based routing

### Billing Service Integration

The gateway provides comprehensive routing and rate limiting for the billing service:

#### Payment Operations

- `POST /api/v1/billing/payments/intent` - Create payment intents (30 req/min)
- `GET /api/v1/billing/payments/**` - Payment queries (60 req/min)
- `POST /api/v1/billing/payments/*/refund` - Process refunds (10 req/min)

#### Subscription Management

- `POST /api/v1/billing/subscriptions` - Create subscriptions (40 req/min)
- `GET /api/v1/billing/subscriptions/**` - Subscription queries (50 req/min)
- `POST /api/v1/billing/subscriptions/*/cancel` - Cancel subscriptions (10 req/min)
- `POST /api/v1/billing/subscriptions/*/pause` - Pause subscriptions (10 req/min)
- `POST /api/v1/billing/subscriptions/*/resume` - Resume subscriptions (10 req/min)

#### Subscription Plans (Public Access)

- `GET /api/v1/billing/subscription-plans` - List all plans (public)
- `GET /api/v1/billing/subscription-plans/active` - List active plans (public)

#### Invoice Operations

- `GET /api/v1/billing/invoices/**` - Invoice queries (50 req/min)

#### Payout Operations

- `GET /api/v1/billing/payouts/**` - Payout queries (40 req/min)

#### Admin Operations

- `POST /api/v1/admin/billing/merchants/**` - Merchant onboarding (20 req/min)
- `GET/POST/PUT/DELETE /api/v1/admin/billing/gateway-config/**` - Gateway configuration (20 req/min)

#### Webhook Processing (Public Access)

- `POST /api/v1/billing/webhooks/**` - Payment provider webhooks (200 req/min, no auth required)

### Authentication Flow

- Validate JWT tokens from Authorization header
- Extract user context (userId, username, email, authorities, permissions, organizationId)
- Sanitize incoming headers to prevent spoofing (removes X-User-\*, X-Tenant-ID, X-Organization-ID)
- Propagate user context to downstream services via headers:
  - `X-User-ID`: User identifier
  - `X-Username`: Username
  - `X-User-Email`: User email address
  - `X-User-Authorities`: Comma-separated list of authorities (e.g., "ADMIN,USER")
  - `X-User-Permissions`: Comma-separated list of permissions
  - `X-Tenant-ID`: Tenant identifier
  - `X-Organization-ID`: Organization identifier
- Skip authentication for public paths
- Support for both access and refresh tokens

### Rate Limiting

- Global rate limiting per IP address
- Tenant-specific rate limit quotas
- Endpoint-specific policies (login: 10/min, signup: 5/min, books: 100/min)
- Burst capacity for traffic spikes
- Rate limit exceeded responses with retry headers

### Circuit Breaking

- Automatic failure detection
- Open circuit after threshold breaches
- Half-open state for recovery testing
- Fallback responses during outages
- Service health monitoring

### Multi-Tenancy

- Extract tenant from X-Tenant-ID header
- Extract tenant from JWT claims
- Extract tenant from subdomain (tenant1.api.iqscaffold.com)
- Tenant-scoped rate limiting
- Tenant context propagation

### Request Transformation

- Add correlation ID to all requests
- Sanitize incoming headers (remove X-User-\*, X-Tenant-ID, X-Organization-ID to prevent spoofing)
- Propagate user context headers:
  - `X-User-ID`: User identifier
  - `X-Username`: Username
  - `X-User-Email`: User email address
  - `X-User-Authorities`: Comma-separated authorities
  - `X-User-Permissions`: Comma-separated permissions
  - `X-Organization-ID`: Organization identifier
- Propagate tenant context (X-Tenant-ID)
- Add gateway version header
- Remove internal headers from requests

### Response Transformation

- Add security headers
- Add correlation headers for tracing
- Remove internal service headers
- Consistent error response format
- CORS headers injection

## API Examples

### Routing Configuration

Routes are defined in `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: http://user-service:8080
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 60
                redis-rate-limiter.burst-capacity: 100
```

### Public Endpoints (No Authentication)

#### User Service

- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/auth/email/verify` - Verify email
- `POST /api/v1/auth/password/forgot` - Password reset request
- `GET /.well-known/jwks.json` - JWK Set for token validation

#### Billing Service

- `GET /api/v1/billing/subscription-plans` - List all subscription plans
- `GET /api/v1/billing/subscription-plans/active` - List active subscription plans
- `POST /api/v1/billing/webhooks/**` - Payment provider webhooks (Stripe, PayPal, etc.)

### Protected Endpoints (Requires JWT)

#### User Service

- `GET /api/v1/users/me` - Get current user
- `POST /api/v1/auth/logout` - Logout current session
- `GET /api/v1/admin/users` - List users (admin only)

#### Billing Service

- `POST /api/v1/billing/payments/intent` - Create payment intent (USER role)
- `GET /api/v1/billing/payments/{id}` - Get payment details (USER role)
- `GET /api/v1/billing/payments` - List payments (BILLING_ADMIN+ role)
- `POST /api/v1/billing/payments/{id}/refund` - Process refund (BILLING_ADMIN+ role)
- `POST /api/v1/billing/subscriptions` - Create subscription (BILLING_ADMIN+ role)
- `GET /api/v1/billing/subscriptions/**` - Subscription operations (USER+ role)
- `GET /api/v1/billing/invoices/**` - Invoice operations (BILLING_ADMIN+ role)
- `GET /api/v1/billing/payouts/**` - Payout operations (BILLING_ADMIN+ role)
- `POST /api/v1/admin/billing/merchants/onboard` - Merchant onboarding (BILLING_ADMIN+ role)
- `GET /api/v1/admin/billing/merchants/status/{orgId}` - Merchant status (BILLING_ADMIN+ role)
- `GET/POST/PUT/DELETE /api/v1/admin/billing/gateway-config/**` - Gateway configuration (BILLING_ADMIN+ role)

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - Aggregated API documentation

### Rate Limit Response

When rate limit is exceeded:

```json
{
  "type": "/problems/rate_limit_exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Tenant rate limit exceeded",
  "instance": "/api/v1/auth/login",
  "code": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2024-01-15T10:30:00Z",
  "retryAfter": 60,
  "correlationId": "1634567890-abc12345",
  "tenantId": "tenant-123"
}
```

Headers:

- `X-RateLimit-Limit: 60`
- `X-RateLimit-Remaining: 0`
- `X-RateLimit-Reset: 1634568790`
- `Retry-After: 60`

## Learning Points

This implementation serves as a reference for:

- Building reactive API gateways with Spring Cloud Gateway
- Implementing distributed rate limiting with Redis
- JWT validation and user context propagation
- Circuit breaker patterns for fault tolerance
- Multi-tenant request routing and isolation
- Correlation ID tracking across services
- API versioning strategies (path and header-based)
- Request/response transformation patterns
- Reactive programming with Project Reactor
- Observability in distributed systems

## Adapting for Your Domain

This gateway service demonstrates patterns applicable to various scenarios:

### API Gateway Patterns

- E-commerce platforms with multiple backend services
- SaaS applications with tenant isolation
- Microservices architectures requiring unified entry point
- Mobile app backends with rate limiting needs

### Rate Limiting Strategies

- Public API protection from abuse
- Tenant-based quota enforcement
- Endpoint-specific throttling policies
- DDoS mitigation at gateway level

### Authentication Gateway

- Centralized authentication for microservices
- Token validation and context propagation
- Multi-tenant access control
- Public vs protected endpoint segregation

### Circuit Breaker Patterns

- Fault tolerance for downstream service failures
- Graceful degradation during outages
- Automatic recovery detection
- Fallback response strategies

### Request Transformation

- Header enrichment for downstream services
- Correlation ID generation for tracing
- User context propagation
- Tenant context extraction and forwarding

The patterns demonstrated here apply to any domain requiring centralized API management, distributed rate limiting, fault tolerance, and multi-tenant request routing.

## Integration with Downstream Services

### Consuming Gateway Context

Downstream services receive enriched headers from the gateway:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(
  @RequestHeader("X-User-ID") Long userId,
  @RequestHeader("X-Username") String username,
  @RequestHeader("X-User-Email") String email,
  @RequestHeader("X-User-Authorities") String authorities,
  @RequestHeader("X-User-Permissions") String permissions,
  @RequestHeader("X-Tenant-ID") String tenantId,
  @RequestHeader("X-Organization-ID") Long organizationId,
  @RequestHeader("X-Correlation-ID") String correlationId
) {
  // Parse authorities
  List<String> authorityList = Arrays.asList(authorities.split(","));

  // Use context for business logic
  logger.info("Request from user {} (tenant: {}) with authorities: {} and correlation: {}", username, tenantId, authorityList, correlationId);

  return ResponseEntity.ok(
    /* response */
  );
}
```

**Security Note**: The gateway sanitizes all incoming user/tenant context headers before processing the request. This prevents clients from spoofing user identity by injecting malicious
headers. Only the gateway can set these headers after JWT validation.

### JWT Validation Configuration

Services can validate JWTs independently using the same JWK Set:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

### Rate Limit Headers

Services can check rate limit status from gateway responses:

```java
var rateLimitLimit = response.getHeaders().getFirst("X-RateLimit-Limit");

var rateLimitRemaining = response.getHeaders().getFirst("X-RateLimit-Remaining");

var rateLimitReset = response.getHeaders().getFirst("X-RateLimit-Reset");
```

### Circuit Breaker Integration

Services should implement health checks for gateway monitoring:

```java
@GetMapping("/actuator/health")
public ResponseEntity<Map<String, String>> health() {
  return ResponseEntity.ok(Map.of("status", "UP"));
}
```

---

**Use this as a blueprint** for building reactive API gateways with intelligent routing, distributed rate limiting, circuit breaker patterns, and multi-tenant support in your microservices
architecture.
