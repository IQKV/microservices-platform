# Gripday Microservices Platform

An extensible microservices platform built with Spring Boot 3.5.6, Spring Cloud 2025.0.0, and Java 21, providing centralized authentication, intelligent API gateway, and comprehensive observability for scalable microservice architectures.

## Overview

The Gripday platform consists of two core services that work together to create a robust foundation for any microservices ecosystem:

- **Auth Service** - Centralized authentication, authorization, and user management with JWT tokens
- **Gateway Service** - Intelligent API gateway with routing, rate limiting, and circuit breaker functionality

### Key Features

- 🔐 **JWT-based Authentication** - Stateless authentication with user context propagation
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
- PostgreSQL 15+ (via Docker)
- Redis 7+ (via Docker)

### 1. Clone and Setup
```bash
git clone https://github.com/gripday/gripday-platform.git
cd gripday-platform

# Start infrastructure services
docker-compose up -d postgres redis
```

### 2. Build Services
```bash
# Build all services
mvn clean package

# Run database migrations
cd gripday-auth-service
mvn liquibase:update -Dspring.profiles.active=local
cd ..
```

### 3. Start Services
```bash
# Terminal 1 - Auth Service
cd gripday-auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 - Gateway Service  
cd gripday-gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Verify Installation
```bash
# Check service health
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8080/actuator/health  # Gateway Service

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

## Services

### Auth Service (Port 8081)
Centralized authentication and user management service.

**Key Endpoints:**
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User authentication  
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout

**Documentation:**
- [Auth Service README](gripday-auth-service/README.md)
- [API Documentation](gripday-auth-service/docs/api/authentication.md)
- Swagger UI: `http://localhost:8081/swagger-ui.html`

### Gateway Service (Port 8080)
API Gateway with intelligent routing and security.

**Features:**
- JWT authentication for all protected routes
- Redis-backed rate limiting
- Circuit breaker patterns with Resilience4j
- Multi-tenant request routing
- CORS handling

**Documentation:**
- [Gateway Service README](gripday-gateway-service/README.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Architecture

### Three-Tier Architecture
Each service follows a strict three-tier architecture pattern:

```
presentation.web/     # REST controllers (Resource suffix)
├── AuthenticationResource.java
└── UserManagementResource.java

domain.service/       # Business logic services  
├── AuthenticationService.java
└── UserRegistrationService.java

infrastructure.repository/  # Data access repositories
├── UserRepository.java
└── AuthorityRepository.java
```

### Multi-Tenant Support
Complete tenant isolation with multiple identification methods:

```bash
# Header-based tenant identification
curl -H "X-Tenant-ID: tenant-123" http://localhost:8080/api/v1/users

# Subdomain-based routing (when enabled)
curl http://tenant-123.localhost:8080/api/v1/users

# JWT token embedded tenant context
# Tenant information automatically extracted from JWT claims
```

### Technology Stack
- **Runtime**: Java 21 with modern language features
- **Framework**: Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Caching**: Redis 7+ for sessions and rate limiting
- **Security**: Spring Security with JWT (RS256/HS256)
- **Observability**: OpenTelemetry, Prometheus, Grafana
- **Testing**: JUnit 5, Testcontainers, ArchUnit, Spring Modulith

## Configuration

### Environment Profiles
- `local` - Development environment with Docker Compose
- `staging` - Pre-production testing environment  
- `production` - Live production deployment
- `test` - Automated testing with Testcontainers

### Key Configuration
```yaml
# Auth Service
gripday:
  auth:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
  database:
    url: ${DATABASE_URL}
  cache:
    redis:
      host: ${REDIS_HOST}

# Gateway Service  
gripday:
  gateway:
    auth-service-url: ${AUTH_SERVICE_URL}
    rate-limiting:
      default-requests-per-minute: 100
    circuit-breaker:
      failure-rate-threshold: 50
```

See [Environment Variables Guide](docs/configuration/environment-variables.md) for complete configuration options.

## Development

### Docker Compose Development
```bash
# Start all services with Docker
docker-compose up -d

# View logs
docker-compose logs -f auth-service
docker-compose logs -f gateway-service

# Stop services
docker-compose down
```

### Testing
```bash
# Run all tests
mvn test

# Integration tests
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
- Auth Service: `http://localhost:8081/swagger-ui.html`
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

**User Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

**Authenticated Request:**
```bash
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/users/me
```

## Monitoring and Observability

### Health Checks
```bash
# Service health
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health

# Component health  
curl http://localhost:8081/actuator/health/db
curl http://localhost:8081/actuator/health/redis
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8080/actuator/metrics/gateway.requests
```

### Structured Logging
- **Development**: Human-readable console output
- **Production**: JSON format with correlation IDs
- **Correlation**: Request tracing across all services

## Extending the Platform

### Adding New Microservices

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
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/newservice/**
          filters:
            - name: JwtAuthenticationFilter
```

3. **Follow architecture patterns:**
- Three-tier architecture (presentation/domain/infrastructure)
- JWT authentication integration
- Multi-tenant support
- Observability integration

### Service Integration Patterns
- **Authentication**: Validate JWT tokens via Auth Service
- **User Context**: Extract user information from JWT claims  
- **Tenant Context**: Support multi-tenant data isolation
- **Error Handling**: Consistent error response format
- **Observability**: OpenTelemetry tracing and metrics

## Documentation

### Comprehensive Guides
- [Local Development Setup](docs/deployment/local-development.md)
- [Environment Configuration](docs/configuration/environment-variables.md)
- [Authentication API](gripday-auth-service/docs/api/authentication.md)
- [Troubleshooting Guide](docs/troubleshooting/common-issues.md)

### Service Documentation
- [Auth Service README](gripday-auth-service/README.md)
- [Gateway Service README](gripday-gateway-service/README.md)

## Troubleshooting

### Common Issues
- **Database Connection**: Check PostgreSQL container and connection settings
- **JWT Token Issues**: Verify token format and secret configuration
- **Rate Limiting**: Check Redis connection and rate limit configuration
- **Circuit Breaker**: Monitor circuit breaker status and thresholds

See [Troubleshooting Guide](docs/troubleshooting/common-issues.md) for detailed solutions.

### Getting Help
```bash
# Collect diagnostic information
curl http://localhost:8081/actuator/info
curl http://localhost:8081/actuator/health
docker-compose logs --tail=50 auth-service
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