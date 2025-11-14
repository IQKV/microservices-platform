**Password Reset:**

```bash
# Initiate password reset (always returns 200)
curl -X POST http://localhost:8080/api/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com"}'

# Reset password using token from email
curl -X POST http://localhost:8080/api/v1/password/reset \
  -H "Content-Type: application/json" \
  -d '{"token":"<reset-token>","newPassword":"NewSecurePass123!"}'
```

**Logout from all devices:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout-all \
  -H "Authorization: Bearer $TOKEN"
```

# Gripday Microservices Platform

An extensible microservices platform built with Spring Boot 3.5.6, Spring Cloud 2025.0.0, and Java 21, providing centralized authentication, intelligent API gateway, and observability for scalable microservice architectures.

## Overview

The Gripday platform consists of three services that demonstrate a complete microservices ecosystem:

- **User Service** (Port 8080) - Centralized authentication, authorization, and user management with JWT tokens
- **Gateway Service** (Port 8080) - Intelligent API gateway with routing, rate limiting, and circuit breaker functionality
- **Bookstore Service** (Port 8080) - Example business service for book catalog and inventory management

### Key Features

- 🔐 **JWT-based Authentication** - Stateless authentication with user context propagation
- 📧 **Email Verification** - Secure account activation with HTML email templates
- 🌐 **API Gateway** - Intelligent routing, rate limiting, and circuit breaker patterns
- 🏢 **Multi-Tenant Architecture** - Complete tenant isolation and context management
- 📊 **Observability** - OpenTelemetry, Prometheus metrics, and structured logging
- 🐳 **Container-First** - Docker Compose for development, Kubernetes-ready
- ⚡ **Modern Java** - Java 21 features including records, pattern matching, and virtual threads
- 🏗️ **Three-Tier Architecture** - Enforced architectural boundaries with ArchUnit testing

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose

### 1. Clone and Setup

```bash
git clone <repository-url> gripday
cd gripday

# Start infrastructure services
docker compose up -d postgres redis
```

### 2. Build and Start Services

```bash
# Build all services
mvn clean package

# Run database migrations
cd gripday-user-service
mvn liquibase:update -Dspring.profiles.active=local
cd ..

# Start services (in separate terminals)
cd gripday-user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd gripday-gateway-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd gripday-bookstore-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Verify Installation

```bash
# Check service health
curl http://localhost:8080/actuator/health  # User Service
curl http://localhost:8080/actuator/health  # Gateway Service
curl http://localhost:8080/actuator/health  # Bookstore Service

# Test authentication flow
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

✅ **Success**: You should receive a 201 Created response with user details and a verification email sent.

**Note**: New users must verify their email address before they can log in. Check the application logs for the verification link in development mode.

### 4. Test Complete Flow

```bash
# Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "TestPass123!"}' | \
  jq -r '.accessToken')

# Test bookstore service through gateway
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/books
```

## Services

### User Service (Port 8080)

Centralized authentication and user management service providing JWT-based authentication, user lifecycle management, role-based access control, and email verification for account activation.

**Key Endpoints:**

- `POST /api/v1/auth/signup` - User registration with email verification
- `POST /api/v1/auth/login` - User authentication (requires verified email)
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/auth/logout-all` - Logout from all devices (revoke all refresh tokens and sessions)
- `GET /api/v1/auth/email/verify` - Email address verification
- `POST /api/v1/auth/email/resend` - Resend verification email
- `GET /api/v1/auth/email/status` - Check email verification status
- `POST /api/v1/password/forgot` - Initiate password reset (non-enumerating)
- `POST /api/v1/password/reset` - Complete password reset with token

**Documentation:**

- [User Service README](gripday-user-service/README.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Gateway Service (Port 8080)

API Gateway providing intelligent routing, security, and resilience patterns for all microservices.

**Features:**

- JWT authentication for protected routes
- Redis-backed rate limiting
- Circuit breaker patterns with Resilience4j
- Multi-tenant request routing
- CORS handling

**Documentation:**

- [Gateway Service README](gripday-gateway-service/README.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Bookstore Service (Port 8080)

Example business microservice demonstrating book catalog and inventory management with full three-tier architecture implementation.

**Features:**

- Book catalog management (CRUD operations)
- Inventory tracking and management
- JWT authentication integration
- Multi-tenant data isolation
- PostgreSQL with Liquibase migrations

**Documentation:**

- [Bookstore Service README](gripday-bookstore-service/README.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Architecture

### Service Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Gateway Service │    │   User Service  │    │ Bookstore Service│
│   (Port 8080)   │◄──►│   (Port 8080)   │    │   (Port 8080)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│      Redis      │    │   PostgreSQL    │    │   PostgreSQL    │
│   (Rate Limiting│    │ (Auth Database) │    │(Bookstore Database)│
│   & Caching)    │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Three-Tier Architecture

Each service follows a strict three-tier architecture pattern:

```
org.gripday.{servicename}/
├── presentation/web/          # REST controllers (Resource suffix)
│   ├── AuthenticationResource.java
│   └── UserManagementResource.java
├── domain/service/           # Business logic services
│   ├── AuthenticationService.java
│   └── UserRegistrationService.java
└── infrastructure/repository/ # Data access repositories
    ├── UserRepository.java
    └── AuthorityRepository.java
```

**Implemented Services:**

- `org.gripday.userservice` - Authentication and user management
- `org.gripday.gatewayservice` - API gateway and routing
- `org.gripday.bookstoreservice` - Book catalog and inventory management

### Multi-Tenant Support

Complete tenant isolation with multiple identification methods:

```bash
# Header-based tenant identification
curl -H "X-Tenant-ID: tenant-123" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/v1/books

# Subdomain-based routing (when enabled)
curl -H "Authorization: Bearer <token>" \
     http://tenant-123.localhost:8080/api/v1/books

# JWT token embedded tenant context
# Tenant information automatically extracted from JWT claims
```

### Technology Stack

- **Runtime**: Java 21 with modern language features
- **Framework**: Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Database**: PostgreSQL 15+ with Liquibase migrations (XML format)
- **Email**: SMTP integration with HTML templates
- **Caching**: Redis 7+ for sessions and rate limiting
- **Security**: Spring Security with JWT tokens and email verification
- **Observability**: OpenTelemetry, Prometheus, Grafana, Loki
- **Testing**: JUnit 5, Testcontainers, ArchUnit, Spring Modulith

## Configuration

### Environment Profiles

- `local` - Development environment with Docker Compose
- `staging` - Pre-production testing environment
- `production` - Live production deployment
- `test` - Automated testing with Testcontainers

### Key Configuration

```yaml
# User Service
gripday:
  auth:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
  email:
    smtp:
      host: ${SMTP_HOST:localhost}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
    sender:
      from-email: ${EMAIL_FROM_EMAIL:noreply@gripday.com}
      from-name: ${EMAIL_FROM_NAME:Gripday Platform}
      base-url: ${APP_BASE_URL:https://app.gripday.com}
    verification:
      token-expiry: PT24H
      rate-limit: 3
  database:
    url: ${DATABASE_URL}
  cache:
    redis:
      host: ${REDIS_HOST}

# Gateway Service
gripday:
  gateway:
    user-service-url: ${USER_SERVICE_URL:http://localhost:8080}
    rate-limiting:
      default-requests-per-minute: 100
    circuit-breaker:
      failure-rate-threshold: 50
```

### Email Configuration

```bash
# SMTP Settings
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@gripday.com
SMTP_PASSWORD=your-app-password

# Email Sender Configuration
EMAIL_FROM_EMAIL=noreply@gripday.com
EMAIL_FROM_NAME=Gripday Platform
APP_BASE_URL=https://app.gripday.com
```

See [Environment Variables Guide](docs/configuration/environment-variables.md) for complete configuration options.

## Development

### Docker Compose Development

```bash
# Start all services with Docker
docker compose up -d

# View logs
docker-compose logs -f user-service
docker-compose logs -f gateway-service
docker-compose logs -f bookstore-service

# Stop services
docker-compose down
```

### Testing

```bash
# Run all tests
mvn test

# Integration tests with Testcontainers
mvn verify

# Architectural tests
mvn test -Dtest=ArchitectureTest

# Code coverage
mvn clean test jacoco:report
```

### Code Quality

```bash
# Run code quality checks
mvn clean compile -Pcode-quality

# SpotBugs analysis
mvn spotbugs:check

# Checkstyle validation
mvn checkstyle:check
```

## API Documentation

### Interactive Documentation

- User Service: `http://localhost:8080/swagger-ui.html`
- Gateway Service: `http://localhost:8080/swagger-ui.html`

### API Examples

**User Registration:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Email Verification:**

```bash
# Check verification status
curl "http://localhost:8080/api/v1/auth/email/status?email=john@example.com"

# Verify email (use token from verification email)
curl "http://localhost:8080/api/v1/auth/email/verify?token=550e8400-e29b-41d4-a716-446655440000"

# Resend verification email
curl -X POST http://localhost:8080/api/v1/auth/email/resend \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com"}'
```

**User Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

**Note**: Users must verify their email address before they can successfully log in.

**Authenticated Request:**

```bash
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get user profile
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/auth/profile

# Access bookstore service
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/books
```

**Bookstore Service Examples:**

```bash
# Create a book (requires authentication)
curl -X POST http://localhost:8080/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot Microservices",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "price": 29.99,
    "quantity": 100
  }'

# Get all books
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/books

# Get book by ID
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/books/1
```

## Monitoring and Observability

### Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health

# Component health
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/redis
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/gateway.requests
```

### Structured Logging

- **Development**: Human-readable console output
- **Production**: JSON format with correlation IDs
- **Correlation**: Request tracing across all services

## Extending the Platform

### Adding New Microservices

The platform demonstrates extensibility with the included Bookstore Service. To add new services:

1. **Create new Maven module:**

```xml
<module>gripday-new-service</module>
```

2. **Configure gateway routing:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/newservice/**
          filters:
            - name: JwtAuthenticationFilter
```

3. **Follow architecture patterns:**

- Three-tier architecture (presentation/domain/infrastructure)
- JWT authentication integration
- Multi-tenant support
- Database per service pattern
- Observability integration

### Service Integration Patterns

- **Authentication**: Validate JWT tokens via User Service
- **User Context**: Extract user information from JWT claims
- **Tenant Context**: Support multi-tenant data isolation
- **Error Handling**: Consistent error response format
- **Observability**: OpenTelemetry tracing and metrics

## Documentation

### Getting Started

- [Developer Onboarding Guide](docs/developer-onboarding.md) - Complete setup and development guide
- [Platform Validation Scripts](scripts/README.md) - End-to-end testing and validation

### API Documentation

- [Complete API Reference](docs/api/complete-api-reference.md) - Comprehensive endpoint documentation
- [Authentication API](gripday-user-service/docs/api/authentication.md) - User service specific endpoints
- Interactive Swagger UI: [Gateway](http://localhost:8080/swagger-ui.html) | [Auth](http://localhost:8080/swagger-ui.html)

### Deployment and Operations

- [Local Development Setup](docs/deployment/local-development.md) - Development environment setup
- [Environment Configuration](docs/configuration/environment-variables.md) - Configuration management
- [Troubleshooting Guide](docs/troubleshooting/common-issues.md) - Common issues and solutions

### Service Documentation

- [User Service README](gripday-user-service/README.md) - Authentication service details
- [Gateway Service README](gripday-gateway-service/README.md) - API gateway service details
- [Bookstore Service README](gripday-bookstore-service/README.md) - Example business service implementation

## Troubleshooting

### Common Issues

- **Database Connection**: Check PostgreSQL container and connection settings
- **JWT Token Issues**: Verify token format and secret configuration
- **Email Verification Required**: New users must verify email before login
- **Email Sending Failed**: Check SMTP configuration and credentials
- **Rate Limiting**: Check Redis connection and rate limit configuration
- **Circuit Breaker**: Monitor circuit breaker status and thresholds

See [Troubleshooting Guide](docs/troubleshooting/common-issues.md) for detailed solutions.

### Getting Help

```bash
# Collect diagnostic information
curl http://localhost:8080/actuator/info  # User Service
curl http://localhost:8080/actuator/info  # Gateway Service
curl http://localhost:8080/actuator/info  # Bookstore Service

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health

# View service logs
docker-compose logs --tail=50 user-service
docker-compose logs --tail=50 gateway-service
docker-compose logs --tail=50 bookstore-service
```

## Contributing

### Development Workflow

1. Fork the repository
2. Create feature branch: `git checkout -b feature/new-feature`
3. Follow coding standards and architectural patterns
4. Write tests for new functionality
5. Ensure all tests pass: `mvn verify`
6. Submit pull request

### Coding Standards

- Java 21 modern features (var, records, pattern matching)
- Three-tier architecture with clear layer separation
- Comprehensive error handling and logging
- Security-first approach with input validation
- Performance optimization and resource management

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: Comprehensive guides in `/docs` directory
- **API Reference**: Interactive Swagger UI for each service
- **Issues**: GitHub Issues for bug reports and feature requests
- **Discussions**: GitHub Discussions for questions and community support
