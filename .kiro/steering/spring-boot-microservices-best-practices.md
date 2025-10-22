# Spring Boot Microservices Development Best Practices

## Architecture Standards

### Three-Tier Architecture Pattern
- **Presentation Layer**: REST controllers in `presentation.web` package with "Resource" suffix
- **Domain Layer**: Business logic services in `domain.service` package  
- **Infrastructure Layer**: Data repositories in `infrastructure.repository` package
- Enforce layer separation with ArchUnit and Spring Modulith testing
- Controllers can only depend on domain services, never infrastructure directly

### Package Structure Convention
```
org.gripday.{servicename}/
├── presentation/web/          # REST controllers (Resource suffix required)
├── domain/service/           # Business logic services
├── infrastructure/repository/ # Data access repositories
└── {ServiceName}Application.java
```

### Database Per Service Pattern
- Each microservice owns its dedicated database instance
- Services communicate via APIs only, never direct database access
- Use PostgreSQL 15+ with Flyway migrations (XML format only)
- Independent scaling and technology choices per service

## Java 21 Modern Development

### Active Use of Modern Features
- Use `var` for local variable type inference throughout codebase
- Implement records for immutable DTOs and value objects
- Apply text blocks for multi-line strings (SQL, JSON templates)
- Use pattern matching for cleaner type checking and control flow
- Leverage enhanced switch expressions for improved logic flow
- Apply virtual threads for improved concurrency where beneficial

### Example Implementation Patterns
```java
// Use var for improved readability
var userOptional = userRepository.findByUsername(username);
var authorities = user.getAuthorities().stream()
    .map(Authority::getName)
    .collect(Collectors.toSet());

// Records for DTOs
public record UserContext(
    Long userId,
    String username,
    Set<String> roles,
    Map<String, Object> customClaims
) {}

// Text blocks for SQL
private static final String FIND_USERS_QUERY = """
    SELECT u.*, a.name as authority_name
    FROM users u
    LEFT JOIN user_authorities ua ON u.id = ua.user_id
    WHERE u.enabled = true
    ORDER BY u.created_at DESC
    """;
```

## REST API Standards

### Controller Naming and Structure
- All @RestController classes must be in `presentation.web` package
- All @RestController classes must have "Resource" suffix
- Example: `AuthenticationResource`, `UserManagementResource`

### HTTP Standards Implementation
- Use standard HTTP methods consistently (GET, POST, PUT, PATCH, DELETE)
- Implement RFC-compliant HTTP status codes (2xx, 4xx, 5xx)
- Provide consistent error response format across all services
- Include correlation IDs for distributed request tracing

### API Versioning Strategy
- Support URL path-based versioning (`/api/v1/users`, `/api/v2/users`)
- Support header-based versioning with Accept and API-Version headers
- Maintain backward compatibility for at least 2 previous major versions
- Provide clear deprecation warnings and migration guidance

## Security and Authentication

### JWT-Based Authentication
- Centralized authentication through Auth Service
- JWT tokens carry user context across all microservices
- User context propagation via standardized JWT claims
- Role-based access control (RBAC) enforcement

### User Context Propagation
```java
public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> roles,
    Set<String> permissions,
    String department,
    String organizationId,
    Map<String, Object> customClaims
) {}
```

### CORS Configuration
- Environment-specific CORS policies (permissive for development, restrictive for production)
- Support for Vite 6 + React 19 development (localhost:5173 default)
- Proper credentials handling for authentication flows

## Documentation Standards

### OpenAPI Documentation
- Comprehensive SpringDoc OpenAPI integration with interactive Swagger UI
- Automatic schema generation from code annotations
- Version-specific documentation with deprecation notices
- Include security schemes, examples, and error responses

### Concise Documentation Approach
- Provide essential information in minimal, focused content
- Use bullet points, code examples, and brief descriptions
- Focus on actionable information and practical usage examples
- Avoid verbose explanations and redundant content

## Testing Philosophy

### Happy Path Testing Focus
- Focus exclusively on successful execution scenarios for core functionality
- Prioritize clear, readable test implementations over comprehensive edge case coverage
- Use straightforward assertions and minimal test data setup
- Avoid testing multiple failure scenarios unless critical to core functionality

### Architectural Testing
- Use ArchUnit to enforce layer separation and naming conventions
- Validate package structure compliance and dependency rules
- Use Spring Modulith for module boundary validation

## Configuration Management

### Environment-Specific Configuration
- Use Spring profiles for environment settings (local, staging, production)
- Minimize Maven profile usage, rely primarily on Spring profiles
- Externalize configuration using environment variables and property files
- Separate application-{profile}.yml files per environment

### Technology Stack Standards
- Spring Boot 3.5.6 for all microservices
- Spring Cloud 2025.0.0 for distributed system capabilities
- Java 21 as target runtime version
- PostgreSQL 15+ with Flyway migrations
- Redis for caching and session management

## Observability and Monitoring

### Structured Logging
- JSON format for staging and production environments
- Human-readable format for local development
- Environment-specific log levels (DEBUG for dev, INFO for staging, WARN for production)
- Correlation ID propagation through all microservice requests
- Security audit logging for authentication and authorization operations

### Observability Stack Integration
- OpenTelemetry for distributed tracing
- Prometheus for metrics collection
- Grafana for dashboards and visualization
- Loki and Promtail for log aggregation

## Error Handling Standards

### Consistent Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": "One or more fields contain invalid values",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/users",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789",
    "requestId": "req-001-2024",
    "fields": [...]
  }
}
```

### Error Code Categories
- AUTH_* for authentication and authorization errors
- VALIDATION_* for input validation errors
- RESOURCE_* for resource-related errors
- DOMAIN_* for business logic violations
- SYSTEM_* for technical system errors
- RATE_* for rate limiting errors

## Development Workflow

### Container-First Development
- Individual Docker containers with dedicated Dockerfiles per microservice
- Separate Docker Compose configurations per microservice
- Environment-specific Docker Compose setups
- Include PostgreSQL, Redis, and observability stack in development environment

### POSIX Environment Assumption
- All development, deployment, and operational environments assume POSIX-compliant systems
- Use Unix/Linux/macOS compatible scripts and commands
- Provide POSIX-specific examples in documentation

## Service Integration Patterns

### Gateway Service Responsibilities
- Reactive programming model for intelligent routing
- JWT authentication enforcement for protected endpoints
- Redis-backed distributed rate limiting
- Circuit breaker patterns with Resilience4j
- CORS handling and request/response transformation

### Auth Service Responsibilities
- Centralized authentication and authorization for all microservices
- JWT token generation and validation
- User lifecycle management (CRUD operations)
- Role-based access control with admin-only user management
- OAuth2 flows and multi-factor authentication support

### Extensible Platform Design
- Support dynamic service registry registration of new microservices
- Centralized endpoint security for any connected microservice
- Standardized user context propagation across all services
- Consistent three-tier architecture and API standards for all services