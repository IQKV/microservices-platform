# Gripday Microservices Platform

An extensible microservices platform with centralized authentication and API gateway built on Spring Boot 3.5.6 and Spring Cloud 2025.0.0.

## Architecture

The platform consists of two core services:

- **Auth Service** (`gripday-auth-service`): Centralized authentication, authorization, and user management
- **Gateway Service** (`gripday-gateway-service`): API gateway with routing, authentication, and rate limiting

## Prerequisites

- Java 21
- Maven 3.9.0+
- Docker and Docker Compose

## Quick Start

### Build the Platform

```bash
# Using Maven wrapper (recommended)
./mvnw clean compile

# Or using system Maven
mvn clean compile
```

### Run Tests

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Code quality checks
./mvnw clean compile -Pcode-quality
```

### Package Services

```bash
# Package all services
./mvnw package

# Package specific service
./mvnw package -pl gripday-auth-service
```

## Project Structure

```
gripday-platform/
├── pom.xml                          # Parent POM
├── gripday-auth-service/            # Authentication service
│   ├── src/main/java/org/gripday/authservice/
│   │   ├── presentation/web/        # REST controllers (Resource suffix)
│   │   ├── domain/service/          # Business logic
│   │   └── infrastructure/repository/ # Data access
│   └── src/test/java/org/gripday/authservice/
│       ├── architecture/            # ArchUnit tests
│       ├── integration/             # Integration tests
│       └── unit/                    # Unit tests
└── gripday-gateway-service/         # Gateway service
    ├── src/main/java/org/gripday/gatewayservice/
    │   ├── config/                  # Configuration
    │   ├── filter/                  # Gateway filters
    │   └── security/                # Security config
    └── src/test/java/org/gripday/gatewayservice/
        ├── architecture/            # ArchUnit tests
        ├── integration/             # Integration tests
        └── unit/                    # Unit tests
```

## Technology Stack

- **Java 21** with modern features (var, records, pattern matching, text blocks)
- **Spring Boot 3.5.6** for microservice framework
- **Spring Cloud 2025.0.0** for distributed system capabilities
- **PostgreSQL** for persistent data storage
- **Redis** for caching and session management
- **JWT** for stateless authentication
- **ArchUnit** and **Spring Modulith** for architectural testing
- **Docker** for containerization

## Development Guidelines

### Three-Tier Architecture

- **Presentation Layer**: REST controllers in `presentation.web` package with "Resource" suffix
- **Domain Layer**: Business logic services in `domain.service` package
- **Infrastructure Layer**: Data repositories in `infrastructure.repository` package

### Java 21 Features

- Use `var` for local variable type inference
- Implement records for immutable DTOs
- Apply text blocks for multi-line strings
- Use pattern matching and enhanced switch expressions

### Testing Philosophy

- Focus on happy path testing for core functionality
- Use ArchUnit to enforce architectural rules
- Implement Spring Modulith tests for module boundaries
- Prioritize clear, readable test implementations

## Services

### Auth Service (Port 8081)

Provides centralized authentication and user management:

- JWT token generation and validation
- User registration and authentication
- Role-based access control
- OAuth2 flows

### Gateway Service (Port 8080)

API gateway with intelligent routing:

- Request routing to microservices
- JWT authentication enforcement
- Rate limiting and circuit breaker
- CORS handling

## Next Steps

1. Implement auth service core functionality
2. Set up database schema and JPA entities
3. Configure Redis integration
4. Implement gateway routing and security
5. Add observability and monitoring
6. Create Docker containerization

## License

This project is licensed under the MIT License.