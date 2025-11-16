# 🚀 Gripday Microservices Platform

> Production-ready Spring Boot microservices platform demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

## Business Purpose

A comprehensive microservices ecosystem that provides:

- **Identity & Access Management** - Centralized authentication with JWT tokens, user lifecycle management, email verification, and role-based access control
- **API Gateway** - Intelligent request routing with rate limiting, circuit breakers, and multi-tenant support
- **Catalog & Inventory** - Book management system demonstrating domain-driven design and inventory tracking patterns
- **Extensible Platform** - Foundation for adding new microservices with standardized security, observability, and integration patterns

This platform serves as a reference implementation for organizations building microservices architectures, showcasing production-ready patterns for authentication, API management, and business domain services.

## Platform Services

### 🔐 [User Service](gripday-user-service/README.md)

Centralized authentication and identity management hub.

**Core Capabilities:**

- JWT-based authentication with token rotation
- User registration with email verification
- Password reset and account security
- Role-based access control (RBAC)
- Multi-tenant data isolation
- Admin user management

**Key Patterns:**

- Token blacklisting with Redis
- Account lockout protection
- Email verification workflows
- Security audit logging
- User context propagation

### 🌐 [Gateway Service](gripday-gateway-service/README.md)

Reactive API gateway providing unified entry point for all services.

**Core Capabilities:**

- Intelligent routing to downstream services
- JWT validation and context propagation
- Redis-backed distributed rate limiting
- Circuit breaker fault tolerance
- Multi-tenant request routing
- Correlation ID tracking

**Key Patterns:**

- Reactive programming with WebFlux
- Sliding window rate limiting
- Request/response transformation
- API versioning (path and header-based)
- Graceful degradation

### 📚 [Bookstore Service](gripday-bookstore-service/README.md)

Domain service demonstrating catalog and inventory management.

**Core Capabilities:**

- Book catalog management
- Inventory tracking with reservations
- Multi-criteria search and filtering
- Stock level monitoring
- Admin operations with audit trail

**Key Patterns:**

- Three-tier architecture
- Repository and service layers
- Cache-aside pattern with Redis
- Optimistic locking for concurrency
- Domain-driven design

## Architecture Overview

### Microservices Architecture

```
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│      Gateway Service (Port 8081)    │
│  • Routing & Rate Limiting          │
│  • JWT Validation                   │
│  • Circuit Breaker                  │
└──────┬──────────────────┬───────────┘
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐
│ User Service │   │   Bookstore  │
│  (Port 8080) │   │    Service   │
│              │   │  (Port 8082) │
│ • Auth/JWT   │   │              │
│ • Users      │   │ • Books      │
│ • Roles      │   │ • Inventory  │
└──────┬───────┘   └──────┬───────┘
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐
│  PostgreSQL  │   │  PostgreSQL  │
│  (User DB)   │   │ (Bookstore)  │
└──────────────┘   └──────────────┘

       Shared Infrastructure:
┌──────────────┐   ┌──────────────┐
│    Redis     │   │ Observability│
│  (Caching &  │   │   Stack      │
│ Rate Limit)  │   │ (Prometheus, │
└──────────────┘   │ Grafana,Loki)│
                   └──────────────┘
```

### Technology Stack

- **Runtime:** Java 21 with modern features (records, var, text blocks, pattern matching)
- **Framework:** Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Database:** PostgreSQL 15+ with Liquibase migrations
- **Caching:** Redis for distributed caching and rate limiting
- **Security:** JWT with RSA256, Spring Security OAuth2 Resource Server
- **Observability:** OpenTelemetry, Prometheus, Grafana, Loki
- **API Documentation:** SpringDoc OpenAPI with Swagger UI
- **Testing:** JUnit 5, Testcontainers, ArchUnit, Spring Modulith
- **Containerization:** Docker with service-specific Dockerfiles

## Key Features

### Security & Authentication

- Centralized JWT-based authentication through User Service
- Token validation at Gateway with context propagation
- Role-based access control across all services
- Account security (lockout, rate limiting, password policies)
- Email verification and password reset flows
- Security audit logging with correlation IDs

### Operational Excellence

- Structured JSON logging for production environments
- Distributed tracing with correlation ID propagation
- Prometheus metrics and Grafana dashboards
- Health checks and actuator endpoints
- Graceful shutdown and error handling
- Environment-specific configuration (local, staging, production)

### Performance & Scalability

- Reactive programming for high-throughput scenarios
- Multi-level caching with Redis
- Database query optimization and connection pooling
- Circuit breaker patterns for fault tolerance
- Distributed rate limiting for API protection
- Independent service scaling

### Developer Experience

- Comprehensive OpenAPI documentation
- Docker Compose for local development
- Consistent error response format (RFC 7807)
- Architecture validation with ArchUnit
- Integration tests with Testcontainers
- Clear separation of concerns with three-tier architecture

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
# Start User Service with dependencies
cd gripday-user-service
docker-compose up

# Start Gateway Service
cd gripday-gateway-service
docker-compose up

# Start Bookstore Service
cd gripday-bookstore-service
docker-compose up
```

### API Documentation

Once services are running, access Swagger UI:

- User Service: http://user-service:8080/swagger-ui.html
- Gateway Service: http://gateway-service:8081/swagger-ui.html
- Bookstore Service: http://bookstore-service:8082/swagger-ui.html

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

- Java 21 features (records, var, text blocks, pattern matching)
- Reactive programming with WebFlux
- Spring Boot 3.x best practices
- Clean architecture principles
- Test-driven development

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

- Catalog and inventory management
- Order processing systems
- Asset management platforms
- Any CRUD-based business domain

The patterns demonstrated here apply to any organization building microservices architectures requiring centralized authentication, API management, and scalable domain services.

---

**Use this as a foundation** for building production-ready microservices with modern Spring Boot, demonstrating security, observability, and operational excellence patterns that scale.
