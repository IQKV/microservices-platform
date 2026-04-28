# 🚀 IQ Scaffold Microservices Platform

> Spring Boot microservices platform demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Platform Services](#platform-services)
- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Architecture Patterns](#architecture-patterns)
- [Getting Started](#getting-started)
- [Monitoring](#monitoring)
- [Learning Objectives](#learning-objectives)
- [Adapting for Your Domain](#adapting-for-your-domain)

## Business Purpose

A microservices ecosystem that provides:

- **Identity & Access Management** - Centralized authentication with JWT tokens, user lifecycle management, email verification, and role-based access control
- **API Gateway** - Intelligent request routing with rate limiting, circuit breakers, and multi-tenant support
- **Billing & Payments** - Multi-tenant payment orchestration with Stripe Connect, subscription management, and automated financial operations
- **CRM Operations** - Complete lead-to-customer pipeline with lead management, contact tracking, and sales pipeline automation
- **Extensible Platform** - Foundation for adding new microservices with standardized security, observability, and integration patterns

This platform serves as a reference implementation for organizations building microservices architectures, showcasing production-ready patterns for authentication, API management, and business domain services.

## Platform Services

### 🔐 [IAM Service](foundation-iam-service/README.md)

Centralized authentication and identity management hub.

**Core Capabilities:**

- JWT-based authentication with RSA256 (JwtEncoder/JwtDecoder)
- User registration with email verification (UUID tokens, 24h expiry)
- Password reset and account security
- Role-based access control (RBAC) with @PreAuthorize
- Schema-per-tenant isolation with Hibernate MultiTenantConnectionProvider
- Admin user management with organization preferences

**Key Patterns:**

- JTI-based token blacklisting with Redis TTL
- Account lockout (5 attempts, 15min) with sliding window
- Email verification with rate limiting (3/hour)
- Security audit logging with UserAuditLog entity
- User context propagation with full JWT claims (userId, username, email, roles, permissions, firstName, lastName, tenantId)
- Pattern matching for claim extraction (Java 21)

### 🌐 [Gateway Service](foundation-gateway-service/README.md)

Reactive API gateway providing unified entry point for all services.

**Core Capabilities:**

- Intelligent routing to downstream services
- JWT validation with ReactiveSecurityContextHolder
- Redis-backed distributed rate limiting with ZSET
- Circuit breaker with Resilience4j (per-service)
- Multi-tenant request routing with priority-based extraction
- Correlation ID generation and tracking

**Key Patterns:**

- Reactive programming with WebFlux (Mono/Flux)
- Sliding window log algorithm with Redis sorted sets
- Dual-layer rate limiting (global IP + tenant-specific)
- Request/response transformation with GlobalFilter chain
- API versioning (path and header-based)
- Type-safe configuration with Java records (IqScaffoldProperties)

### 💰 [Billing Service](foundation-billing-service/README.md)

Multi-tenant payment orchestration and financial operations service.

**Core Capabilities:**

- Payment orchestration with Stripe Payment Intents and multi-gateway support
- Subscription management with automated billing, trial periods, and plan changes
- Merchant onboarding using Stripe Connect (Standard/Express)
- Payout management and revenue sharing with platform fees
- Refund processing with full and partial refund support
- Invoice management with automated generation and tracking
- Multi-gateway configuration (Stripe) with encrypted credentials

**Key Patterns:**

- State machine-driven payment and subscription lifecycle
- Cryptographic webhook signature verification
- AES-256-GCM encryption for gateway credentials
- Schema-per-tenant financial data isolation
- Event-driven notifications with RabbitMQ integration
- Comprehensive audit logging for compliance

### 🎯 [Lead Service](foundation-lead-service/README.md)

CRM lead management service for lead capture, qualification, and conversion.

**Core Capabilities:**

- Lead management with comprehensive tracking and scoring (0-100)
- Lead qualification with automatic and manual processes
- Lead assignment to sales representatives
- Lead conversion to contacts with cross-service integration
- Activity tracking and lead notes management
- Multi-source lead tracking (Website, Referral, Cold Call, etc.)

**Key Patterns:**

- Lead-to-contact conversion with REST API integration
- Event publishing for lead lifecycle events
- Redis caching for performance optimization
- Multi-tenant schema isolation
- Role-based access control with JWT authentication

### 👥 [Contact Service](foundation-contact-service/README.md)

CRM contact management service with lead scoring and bulk operations.

**Core Capabilities:**

- Contact management with comprehensive CRUD operations
- Bulk operations for efficient contact processing (max 100 per request)
- Lead conversion tracking with timestamps
- Company association and contact status management
- Lead scoring maintenance and bulk score updates
- Event publishing for contact lifecycle events

**Key Patterns:**

- Bulk operation support with detailed success/failure reporting
- Event-driven integration with Lead and Pipeline services
- Ehcache second-level caching for performance
- Multi-tenant schema isolation
- Conversion tracking from leads

### 📊 [Pipeline Service](foundation-pipeline-service/README.md)

CRM pipeline management service for sales process tracking and analytics.

**Core Capabilities:**

- Pipeline stage management with customizable stages (New → Contacted → Qualified → Proposal → Won/Lost)
- Lead tracking through pipeline stages
- Follow-up scheduling with due dates and priorities
- Activity logging for all lead interactions and stage transitions
- Dashboard analytics with conversion metrics and pipeline statistics
- Velocity tracking and conversion rate analysis

**Key Patterns:**

- Event-driven architecture consuming contact/lead events
- Activity logging for comprehensive audit trails
- Dashboard service for real-time analytics
- Multi-tenant schema isolation
- Follow-up priority and status management

## Architecture Overview

### Microservices Architecture

```text
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────────┐
│        Gateway Service (Port 8081)       │
│  • Routing & Rate Limiting               │
│  • JWT Validation                        │
│  • Circuit Breaker                       │
└──────┬────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
│            IAM Service (Port 8080)      │   │          Billing Service (Port 8080)     │
│  • Auth/JWT                              │   │  • Payment Intents                       │
│  • Users                                 │   │  • Subscriptions                         │
│  • Roles                                 │   │  • Stripe Connect                        │
└──────┬───────────────────────────────────┘   └──────┬───────────────────────────────────┘
       │                                              │
       ▼                                              ▼
┌──────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
│            PostgreSQL (User DB)          │   │          PostgreSQL (Billing DB)         │
└──────────────────────────────────────────┘   └──────────────────────────────────────────┘

CRM Services
┌──────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
│          Contact Service (Port 8080)     │   │           Lead Service (Port 8080)       │
│  • Contact Management                    │   │  • Lead Management                       │
│  • Bulk Operations                       │   │  • Lead Scoring                          │
│  • Lead Conversion                       │   │  • Lead Assignment                       │
└──────┬───────────────────────────────────┘   └──────┬───────────────────────────────────┘
       │                                              │
       ▼                                              ▼
┌──────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
│          PostgreSQL (Contact DB)         │   │           PostgreSQL (Lead DB)           │
└──────────────────────────────────────────┘   └──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│        Pipeline Service (Port 8080)      │
│  • Pipeline Management                   │
│  • Follow-up Scheduling                  │
│  • Analytics Dashboard                   │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         PostgreSQL (Pipeline DB)         │
└──────────────────────────────────────────┘

Shared Infrastructure
┌──────────────────────────────┐   ┌─────────────────────────────┐   ┌─────────────────────────────┐
│            Redis             │   │          RabbitMQ           │   │        Observability        │
│  • Caching                   │   │  • Event Publishing         │   │  • Prometheus / Grafana     │
│  • Rate Limiting             │   │  • Cross-Service Messaging  │   │  • Loki / OpenTelemetry     │
└──────────────────────────────┘   └─────────────────────────────┘   └─────────────────────────────┘
```

### Technology Stack

- **Runtime:** Java 25 with modern features (records, var, text blocks, pattern matching, switch expressions)
- **Framework:** Spring Boot 4.0, Spring Cloud 2025.0.0, Spring Cloud Gateway (reactive)
- **Database:** PostgreSQL 15+ with Liquibase migrations, Hibernate multi-tenancy
- **Caching:** Redis for distributed caching, rate limiting (ZSET), and token blacklisting
- **Messaging:** RabbitMQ for event-driven communication and cross-service integration
- **Security:** JWT with RSA256 (JwtEncoder/JwtDecoder), Spring Security OAuth2 Resource Server
- **Resilience:** Resilience4j for circuit breaker, rate limiting, and fault tolerance
- **Observability:** OpenTelemetry, Prometheus, Grafana, Loki, structured JSON logging
- **API Documentation:** SpringDoc OpenAPI with Swagger UI
- **Testing:** JUnit 5, Testcontainers, ArchUnit, Spring Modulith, Reactor Test
- **Containerization:** Docker with multi-stage builds, Docker Compose for local development

## Key Features

### Security & Authentication

- Centralized JWT-based authentication with RSA256 through IAM Service
- JTI-based token blacklisting with Redis TTL for logout
- Token validation at Gateway with ReactiveSecurityContextHolder
- User context propagation via headers (X-User-ID, X-Username, X-User-Roles)
- Role-based access control with @PreAuthorize across all services
- Account security (5 failed attempts → 15min lockout with sliding window)
- Email verification with UUID tokens and rate limiting (3/hour)
- Password reset flows with secure time-limited tokens
- Security audit logging with UserAuditLog entity and correlation IDs

### Operational Excellence

- Structured JSON logging for production environments
- Distributed tracing with correlation ID propagation
- Prometheus metrics and Grafana dashboards
- Health checks and actuator endpoints
- Graceful shutdown and error handling
- Environment-specific configuration (local, staging, production)

### Performance & Scalability

- Reactive programming with WebFlux (Mono/Flux) for high-throughput scenarios
- Multi-level caching with Redis (cache-aside pattern)
- Sliding window log algorithm with Redis ZSET for rate limiting
- Database query optimization with proper indexing and connection pooling
- Circuit breaker with Resilience4j (per-service, configurable thresholds)
- Distributed rate limiting with dual-layer (global IP + tenant-specific)
- Schema-per-tenant isolation for multi-tenancy
- Independent service scaling with stateless design

### Developer Experience

- OpenAPI documentation with Swagger UI
- Docker Compose for local development
- Consistent error response format (RFC 7807)
- Architecture validation with ArchUnit
- Integration tests with Testcontainers
- Clear separation of concerns

## Architecture Patterns

### Cross-Cutting Patterns

- **Database Per Service** - Each microservice owns its dedicated database
- **API Gateway** - Single entry point with centralized concerns
- **Service Discovery Ready** - Configurable for dynamic service registration
- **Circuit Breaker** - Fault tolerance with Resilience4j
- **Distributed Tracing** - Correlation IDs across all services
- **Centralized Authentication** - JWT validation and context propagation

### Communication Patterns

- Synchronous REST APIs with proper HTTP semantics
- JWT-based user context propagation via headers
- Event-driven messaging with RabbitMQ for cross-service communication
- Lead-to-contact conversion with REST API integration
- Correlation ID tracking for distributed requests
- Standardized error responses across services

## Getting Started

### Prerequisites

- Java 21+
- Docker and Docker Compose
- PostgreSQL 15+ (or use Docker Compose)
- Redis (or use Docker Compose)

### Local Development

Each service can be run independently with Docker Compose:

```bash
# Start IAM Service with dependencies
cd foundation-iam-service
docker-compose up

# Start Billing Service with dependencies
cd foundation-billing-service
docker-compose up

# Start Gateway Service
cd foundation-gateway-service
docker-compose up

# Start CRM Services
cd foundation-lead-service
docker-compose up

cd foundation-contact-service
docker-compose up

cd foundation-pipeline-service
docker-compose up

```

### API Documentation

Once services are running, access Swagger UI:

- IAM Service: http://iam-service:8080/swagger-ui.html
- Gateway Service: http://gateway-service:8080/swagger-ui.html
- Billing Service: http://billing-service:8080/swagger-ui.html

### Monitoring

Access observability tools:

- Prometheus: http://prometheus:9090
- Grafana: http://grafana:3000
- Health Checks: http://{service-name}:808x/actuator/health

## Learning Objectives

This platform demonstrates:

### Microservices Architecture

- Service decomposition and bounded contexts
- Database per service pattern
- API gateway pattern
- Service-to-service communication
- Distributed system challenges and solutions

### Security Implementation

- JWT-based stateless authentication
- Token validation and propagation
- Role-based access control
- Multi-tenant data isolation
- Security audit logging

### Operational Patterns

- Structured logging and correlation IDs
- Distributed tracing with OpenTelemetry
- Metrics collection with Prometheus
- Health checks and graceful shutdown
- Circuit breaker and rate limiting

### Modern Java Development

- Java 21 features (records, var, text blocks, pattern matching, switch expressions)
- Value objects and immutable DTOs with records
- Pattern matching for claim extraction and type handling
- Reactive programming with WebFlux and Project Reactor
- Spring Boot 3.x best practices with type-safe configuration
- Domain-Driven Design with tactical patterns (aggregates, value objects, factories)
- Clean architecture with clear layer separation
- Test-driven development with unit, integration, and architecture tests

## Adapting for Your Domain

This platform provides reusable patterns for:

### Authentication & Authorization

- Employee portals and customer platforms
- Multi-tenant SaaS applications
- Partner access management systems
- Identity and access management (IAM)

### API Gateway Patterns

- E-commerce platforms with multiple services
- Mobile app backends with rate limiting
- Public API protection and management
- Multi-tenant request routing

### Domain Services

- CRM and lead management systems
- Sales pipeline and conversion tracking
- Customer relationship management platforms
- Order processing systems
- Asset management platforms
- Any CRUD-based business domain

The patterns demonstrated here apply to any organization building microservices architectures requiring centralized authentication, API management, and scalable domain services.

---

**Use this as a foundation** for building production-ready microservices with modern Spring Boot, demonstrating security, observability, and operational excellence patterns that scale.
