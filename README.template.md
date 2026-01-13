# 🏗️ IQ Scaffold Backend Microservices

> Production-ready microservices platform providing authentication, API gateway, and payment orchestration with multi-tenant architecture, distributed security, and comprehensive business operations. Reference implementation for building scalable SaaS backend systems.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Overview](#overview)
- [Microservices Architecture](#microservices-architecture)
- [What It Demonstrates](#what-it-demonstrates)
- [Architecture Patterns](#architecture-patterns)
- [Technical Highlights](#technical-highlights)
- [Learning Points](#learning-points)
- [Starting Point for SaaS Backend Development](#starting-point-for-saas-backend-development)
- [Adapting for Your Domain](#adapting-for-your-domain)
- [Integration & Communication](#integration--communication)

## Business Purpose

A comprehensive microservices platform that demonstrates:

- **Identity & Access Management** - Centralized authentication with JWT tokens, role-based access control, and multi-tenant user management
- **API Gateway & Security** - Intelligent routing, distributed rate limiting, circuit breaker patterns, and user context propagation
- **Payment Orchestration** - End-to-end payment processing, merchant onboarding, subscription management, and multi-gateway support
- **Multi-Tenant Architecture** - Schema-per-tenant isolation, tenant-specific configurations, and secure data segregation
- **Event-Driven Communication** - RabbitMQ messaging for asynchronous operations and cross-service coordination
- **Production Operations** - Comprehensive observability, health monitoring, and distributed tracing

Reference implementation for building enterprise-grade SaaS platforms with microservices architecture, demonstrating scalable patterns for authentication, payments, and multi-tenancy.

## Overview

The IQ Scaffold backend consists of three core microservices that work together to provide a complete SaaS platform foundation. Each service is independently deployable, scalable, and maintains its own database while communicating through well-defined APIs and event messaging.

Starting point for microservices backend development, demonstrating distributed system patterns, security best practices, and business domain separation with comprehensive integration examples.

## Microservices Architecture

### 🔐 User Service (Port 8080)

**Identity & Access Management Hub**

**Business Functions:**

- User authentication with JWT tokens (RSA256)
- User registration with email verification
- Password reset and account security
- Role-based access control (RBAC)
- Multi-tenant user management
- Organization and tenant provisioning
- User preferences and profile management

**Key Technologies:**

- Spring Boot 3.5.6 with Spring Security
- JWT (JwtEncoder/JwtDecoder) with RSA256
- PostgreSQL 15+ with Liquibase migrations
- Redis for token blacklisting and caching
- RabbitMQ for event messaging
- Spring Mail with Thymeleaf templates

**Core API Endpoints:**

- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - Authentication
- `POST /api/v1/auth/refresh` - Token refresh
- `GET /api/v1/users/me` - Current user context
- `GET /api/v1/admin/users` - User management (admin)
- `GET /api/v1/admin/organizations` - Organization management
- `POST /api/v1/public/signup` - Self-service tenant provisioning
- `GET /.well-known/jwks.json` - JWK Set for token validation

**Database Schema:**

- **Public Schema:** System-wide authorities and tenant registry
- **Tenant Schemas:** Per-tenant isolation with users, preferences, organizations, audit logs

**Security Features:**

- Account lockout after 5 failed attempts (15min duration)
- Password strength validation with custom annotations
- Email verification with UUID tokens (24h expiry)
- Token blacklisting with Redis TTL
- Security audit logging with correlation IDs

---

### 🌐 Gateway Service (Port 8081)

**Reactive API Gateway & Security Layer**

**Business Functions:**

- Intelligent request routing to downstream services
- JWT validation and user context propagation
- Distributed rate limiting with tenant-specific quotas
- Circuit breaker for fault tolerance
- Multi-tenant request routing
- Correlation ID generation and tracking

**Key Technologies:**

- Spring Cloud Gateway with WebFlux (reactive)
- Spring Security OAuth2 Resource Server
- Resilience4j for circuit breaker patterns
- Redis for distributed rate limiting (ZSET algorithm)
- Project Reactor (Mono/Flux) for reactive programming

**Core API Endpoints:**

- Routes to User Service: `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/admin/**`
- Routes to Billing Service: `/api/v1/billing/**`, `/api/v1/admin/billing/**`
- `GET /actuator/health` - Health check
- `GET /swagger-ui.html` - Aggregated API documentation

**Rate Limiting Strategy:**

- **Global IP-based:** 100 requests/minute per IP
- **Tenant-specific:** Configurable per tenant (default 60 req/min)
- **Endpoint-specific:** Different limits for different operations
- **Burst capacity:** 2x quota for traffic spikes

**Circuit Breaker Configuration:**

- Failure rate threshold: 50%
- Slow call threshold: 100ms
- Sliding window: 10 calls
- Half-open state: 3 calls before recovery decision

**Context Propagation:**

- `X-User-ID`, `X-Username`, `X-User-Email`
- `X-User-Authorities`, `X-User-Permissions`
- `X-Tenant-ID`, `X-Organization-ID`
- `X-Correlation-ID` for tracing

---

### 💰 Billing Service (Port 8082)

**Payment Orchestration & Merchant Management**

**Business Functions:**

- End-to-end payment orchestration with Stripe Payment Intents
- Automated merchant onboarding using Stripe Connect
- Subscription management with recurring billing
- Invoice generation and tracking
- Payout management and reconciliation
- Refund processing (full and partial)
- Multi-gateway support (Stripe, PayPal, Square, Braintree)
- Platform fee management and revenue sharing

**Key Technologies:**

- Spring Boot 3.5.6 with Spring Data JPA
- Stripe Java SDK for payment processing
- PostgreSQL 15+ with Liquibase
- Hibernate multi-tenancy (schema-per-tenant)
- Resilience4j for circuit breaker and time limiter
- RabbitMQ for event messaging
- AES-256-GCM encryption for gateway credentials

**Core API Endpoints:**

**Payment Operations:**

- `POST /api/v1/billing/payments/intent` - Create payment intent
- `GET /api/v1/billing/payments` - List payments (paginated)
- `POST /api/v1/billing/payments/{id}/refund` - Process refund

**Subscription Operations:**

- `POST /api/v1/billing/subscriptions` - Create subscription
- `GET /api/v1/billing/subscriptions/active` - Get active subscription
- `POST /api/v1/billing/subscriptions/{id}/cancel` - Cancel subscription

**Merchant Administration:**

- `POST /api/v1/admin/billing/merchants/onboard` - Initiate onboarding
- `GET /api/v1/admin/billing/merchants/status/{organizationId}` - Check status

**Gateway Configuration:**

- `POST /api/v1/admin/billing/gateway-config` - Create configuration
- `PUT /api/v1/admin/billing/gateway-config/{provider}` - Update configuration
- `POST /api/v1/admin/billing/gateway-config/{provider}/activate` - Activate gateway

**Webhook Endpoints:**

- `POST /api/v1/billing/webhooks/stripe` - Stripe webhook handler
- `POST /api/v1/billing/webhooks/paypal` - PayPal webhook handler (future)

**Payment State Machine:**

```
null → PENDING → PROCESSING → SUCCEEDED/FAILED → REFUNDED/PARTIALLY_REFUNDED
                                              ↘ CANCELED
```

**Database Schema:**

- **Public Schema:** Merchant payment config, encrypted gateway credentials
- **Tenant Schemas:** Payments, audit trails, payouts, customers, subscriptions, invoices

**Security Features:**

- JWT validation with user context extraction
- Webhook signature verification (Stripe-Signature header)
- AES-256-GCM encryption for gateway credentials
- Comprehensive audit logging for all state transitions

## What It Demonstrates

### 🔐 Distributed Authentication & Authorization

- JWT-based stateless authentication with RSA256 signatures
- Access tokens (15min) and refresh tokens (7 days) with configurable expiry
- Token rotation and Redis-backed blacklisting with TTL
- Authority-based access control (ABAC) with method-level @PreAuthorize
- User context extraction and propagation across services
- Multi-device session management with logout-all functionality

### 🏢 Multi-Tenant Architecture Patterns

- Schema-per-tenant isolation with Hibernate MultiTenantConnectionProvider
- Dynamic schema switching with tenant context propagation
- Tenant extraction from JWT claims and headers
- Per-tenant Liquibase migrations with automated schema creation
- Cross-tenant operations via TenantContext.executeInTenantContext()
- Tenant-scoped repositories without explicit tenant_id predicates

### 🌐 Reactive Gateway Patterns

- Spring Cloud Gateway with WebFlux for non-blocking I/O
- Reactive filter chains with ordered execution
- Backpressure handling for high-throughput scenarios
- Reactive Redis operations with ReactiveStringRedisTemplate
- Header sanitization to prevent spoofing attacks

### 💳 Payment Processing Patterns

- State machine-driven payment transitions with audit trails
- Idempotent webhook processing for external event synchronization
- Multi-gateway architecture with tenant-specific credentials
- Stripe Connect integration for marketplace/multi-tenant scenarios
- Platform fee management with automated commission calculation
- Comprehensive refund management with state synchronization

### 🔄 Event-Driven Architecture

- RabbitMQ messaging for asynchronous communication
- Event publishing for payment lifecycle (created, succeeded, failed, refunded)
- Cross-service event coordination for business processes
- Dead letter queues for failed message handling
- Transactional email notifications with event triggers

### 🛡️ Security & Compliance

- Account lockout protection with Redis-backed state management
- Password strength validation with custom annotations
- Input sanitization (SQL injection, XSS prevention)
- Webhook signature verification for external integrations
- Comprehensive security audit logging with correlation IDs
- Encrypted credential storage with tenant-specific key derivation

### 🎯 Observability & Monitoring

- Structured JSON logging with correlation IDs and MDC context
- OpenTelemetry distributed tracing across all services
- Prometheus metrics integration with custom business metrics
- Health checks and actuator endpoints for operational monitoring
- Comprehensive audit trails for compliance and debugging

## Architecture Patterns

### Communication Flow

```
Client Request
    ↓
Gateway Service (8081)
├─ JWT Validation
├─ Rate Limiting
├─ Circuit Breaker
└─ Route to Service
    ↓
┌─────────────────────────────────────┐
│                                     │
User Service (8080)    Billing Service (8082)
│                                     │
└─────────────────────────────────────┘
    ↓
PostgreSQL (User DB)   PostgreSQL (Billing DB)
    ↓
Redis (Caching, Rate Limiting, Token Blacklist)
    ↓
RabbitMQ (Event Messaging)
```

### Key Design Patterns

- **API Gateway Pattern** - Single entry point with routing and cross-cutting concerns
- **Database Per Service** - Each service owns its database
- **Schema-Per-Tenant** - Multi-tenant isolation at database level
- **Event-Driven Architecture** - RabbitMQ for asynchronous communication
- **Circuit Breaker** - Resilience4j for fault tolerance
- **Distributed Rate Limiting** - Redis-backed sliding window algorithm
- **JWT-Based Authentication** - Stateless, distributed authentication
- **Audit Logging** - Comprehensive tracking of all state changes

### Reactive Filter Chain (Gateway)

```
Request Flow:
1. CorrelationIdFilter        → Generate/extract correlation ID
2. TenantExtractionFilter     → Extract tenant context
3. JwtAuthenticationFilter    → Validate JWT and extract user context
4. ApiVersionRoutingFilter    → Handle API versioning
5. TenantRateLimitingFilter   → Apply rate limits
6. CircuitBreakerFilter       → Apply circuit breaker patterns
7. RouteToService             → Forward to downstream service
```

## Technical Highlights

### Technology Stack

| Component         | Technology           | Version                        |
| ----------------- | -------------------- | ------------------------------ |
| **Runtime**       | Java                 | 21                             |
| **Framework**     | Spring Boot          | 3.5.6                          |
| **Cloud**         | Spring Cloud         | 2025.0.0                       |
| **Gateway**       | Spring Cloud Gateway | WebFlux (Reactive)             |
| **Database**      | PostgreSQL           | 15+                            |
| **ORM**           | Hibernate            | 6.x with JPA                   |
| **Caching**       | Redis                | 7                              |
| **Messaging**     | RabbitMQ             | 3.13                           |
| **Security**      | Spring Security      | OAuth2 Resource Server         |
| **JWT**           | JJWT                 | RSA256                         |
| **Resilience**    | Resilience4j         | Circuit Breaker, Rate Limiting |
| **Migrations**    | Liquibase            | Schema management              |
| **Observability** | OpenTelemetry        | Distributed tracing            |
| **Metrics**       | Micrometer           | Prometheus                     |

### Performance Optimization

- Reactive programming with Project Reactor for non-blocking I/O
- Connection pooling with HikariCP for database efficiency
- Redis caching for frequently accessed data
- Query optimization with JPA criteria and native queries
- Lazy loading and pagination for large datasets
- Circuit breaker patterns for graceful degradation

### Security Features

- RSA256 JWT signatures with JWK Set rotation
- AES-256-GCM encryption for sensitive data at rest
- Webhook signature verification for external integrations
- Rate limiting with sliding window algorithms
- Input validation and sanitization
- Comprehensive audit logging

### Operational Features

- Docker containerization with multi-stage builds
- Health checks and readiness probes
- Prometheus metrics with custom business indicators
- Structured logging with correlation tracking
- Environment-specific configuration management
- Automated database migrations with Liquibase

## Learning Points

This implementation serves as a reference for:

- Building microservices with Spring Boot and Spring Cloud
- Implementing distributed authentication and authorization
- Managing multi-tenant architecture with schema isolation
- Creating reactive API gateways with Spring Cloud Gateway
- Handling payment processing with external providers (Stripe)
- Implementing event-driven architecture with RabbitMQ
- Managing distributed state with Redis
- Implementing circuit breaker and rate limiting patterns
- Creating comprehensive audit trails and observability
- Managing database migrations in multi-tenant environments
- Implementing webhook processing and external integrations
- Building resilient distributed systems

## Starting Point for SaaS Backend Development

Starting point for building microservices backends for SaaS applications:

### Foundation for SaaS Platforms

- **Multi-Tenant Architecture** - Schema-per-tenant isolation with tenant context propagation
- **Authentication Service** - Centralized identity management with JWT tokens
- **API Gateway** - Intelligent routing with security and rate limiting
- **Payment Processing** - Production-ready Stripe integration with multi-gateway support
- **Event-Driven Communication** - RabbitMQ messaging for asynchronous operations

### Reusable Patterns for Microservices

- **Service Communication** - HTTP APIs with JWT authentication and context propagation
- **Data Management** - JPA with multi-tenant connection providers
- **Security Patterns** - OAuth2 Resource Server with method-level authorization
- **Resilience Patterns** - Circuit breakers, rate limiting, and graceful degradation
- **Observability** - Distributed tracing, metrics, and structured logging

### Common SaaS Features

- **User Management** - Registration, authentication, and profile management
- **Organization Management** - Multi-tenant organization and user administration
- **Billing & Payments** - Payment processing, subscriptions, and merchant onboarding
- **Security & Compliance** - Audit trails, encryption, and access control
- **Operational Monitoring** - Health checks, metrics, and distributed tracing

## Adapting for Your Domain

Patterns applicable to various SaaS scenarios:

### SaaS Application Types

- Multi-tenant B2B SaaS platforms
- Enterprise management systems
- E-commerce and marketplace platforms
- Financial services and fintech applications
- Healthcare and compliance-heavy industries
- IoT and device management platforms

### Extending the Foundation

- Add domain-specific business services (inventory, CRM, analytics)
- Implement additional payment gateways (PayPal, Square, Braintree)
- Add notification services (email, SMS, push notifications)
- Integrate with external APIs and third-party services
- Add workflow and approval processes
- Implement advanced analytics and reporting

### Architecture Benefits

- Independent service scaling and deployment
- Technology diversity per service requirements
- Fault isolation and graceful degradation
- Team autonomy and parallel development
- Database technology optimization per service
- Comprehensive testing and quality assurance

## Integration & Communication

### Service-to-Service Communication

**Synchronous Communication (HTTP/REST):**

- Gateway routes requests to appropriate services
- JWT tokens validated and user context propagated
- Circuit breakers prevent cascade failures
- Rate limiting protects against abuse

**Asynchronous Communication (RabbitMQ):**

- User lifecycle events (created, updated, deleted)
- Payment events (succeeded, failed, refunded)
- Billing events (subscription created, canceled)
- Notification events (email, SMS)

### External Integrations

**Stripe Integration:**

- Payment Intent creation and processing
- Webhook event handling with signature verification
- Stripe Connect for merchant onboarding
- Subscription and invoice management

**Email Service Integration:**

- Transactional emails with Thymeleaf templates
- Multi-language support with i18n
- SMTP configuration with authentication

### Database Integration

**Multi-Tenant Data Access:**

- Schema-per-tenant isolation
- Dynamic connection routing
- Tenant context propagation
- Cross-tenant operations when needed

**Migration Management:**

- Liquibase for schema versioning
- Automated migrations across all tenant schemas
- Rollback capabilities for production safety

### Monitoring & Observability

**Distributed Tracing:**

- OpenTelemetry integration across all services
- Correlation ID propagation through request chains
- Performance monitoring and bottleneck identification

**Metrics & Monitoring:**

- Prometheus metrics for business and technical indicators
- Grafana dashboards for operational visibility
- Alert management for critical system events

---

**Use this as a blueprint** for building microservices backends with Spring Boot, demonstrating patterns for authentication, payments, multi-tenancy, and distributed system architecture. Provides comprehensive examples for SaaS platform development with production-ready patterns and practices.
