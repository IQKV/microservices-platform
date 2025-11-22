# Repository Guidelines & Agent Instructions

## Overview

This document provides comprehensive guidelines for repository management, development workflows, and collaboration standards for the Gripday microservices platform. It serves as a reference for both human developers and AI agents working with this codebase.

The Gripday platform is a production-ready Spring Boot microservices ecosystem demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

## 🏛️ Repository Structure & Organization

### Microservices Architecture Layout

```
gripday/
├── .github/                          # GitHub workflows and automation
│   └── workflows/                    # CI/CD pipeline definitions
├── docker/                           # Docker configurations for infrastructure
│   ├── grafana/                      # Grafana dashboards and provisioning
│   ├── loki/                         # Loki logging configuration
│   ├── postgres/                     # PostgreSQL initialization scripts
│   ├── prometheus/                   # Prometheus metrics configuration
│   └── promtail/                     # Promtail log collection
├── e2e-tests/                        # End-to-end integration tests
├── helm/                             # Kubernetes Helm charts
├── k8s/                              # Kubernetes manifests
├── scripts/                          # Build, deployment, and utility scripts
├── gripday-user-service/             # Authentication & identity management
│   ├── src/main/java/                # Java source code
│   ├── src/main/resources/           # Configuration and migrations
│   ├── src/test/                     # Unit and integration tests
│   ├── scripts/                      # Service-specific scripts
│   ├── Dockerfile                    # Container image definition
│   ├── docker-compose.yml            # Local development setup
│   └── pom.xml                       # Maven build configuration
├── gripday-gateway-service/          # API Gateway with routing & rate limiting
│   ├── src/main/java/                # Reactive gateway implementation
│   ├── src/main/resources/           # Gateway routing configuration
│   ├── src/test/                     # Gateway tests
│   ├── scripts/                      # Gateway-specific scripts
│   ├── Dockerfile                    # Container image definition
│   ├── docker-compose.yml            # Local development setup
│   └── pom.xml                       # Maven build configuration
├── gripday-bookstore-service/        # Domain service (catalog & inventory)
│   ├── src/main/java/                # Domain implementation
│   ├── src/main/resources/           # Configuration and migrations
│   ├── src/test/                     # Domain tests
│   ├── scripts/                      # Service-specific scripts
│   ├── Dockerfile                    # Container image definition
│   ├── docker-compose.yml            # Local development setup
│   └── pom.xml                       # Maven build configuration
├── docker-compose.yml                # Full platform orchestration
├── compose.yaml                      # Development tools (SonarQube)
├── pom.xml                           # Parent POM with shared configuration
├── package.json                      # Node.js tooling (Prettier, Husky)
└── AGENTS.md                         # This file
```

### Service Architecture Pattern

Each microservice follows a consistent internal structure:

```
gripday-{service-name}/
├── src/main/java/org/gripday/{service}/
│   ├── config/                       # Spring configuration classes
│   ├── domain/                       # Domain entities and business logic
│   ├── repository/                   # Data access layer (JPA repositories)
│   ├── service/                      # Business service layer
│   ├── presentation/                 # REST controllers and DTOs
│   │   ├── web/                      # Public endpoints
│   │   └── admin/                    # Admin-only endpoints
│   ├── security/                     # Security configuration and filters
│   ├── exception/                    # Custom exceptions and handlers
│   └── {Service}Application.java    # Spring Boot main class
├── src/main/resources/
│   ├── application.yml               # Base configuration
│   ├── application-local.yml         # Local development profile
│   ├── application-staging.yml       # Staging environment profile
│   ├── application-production.yml    # Production environment profile
│   └── db/changelog/                 # Liquibase database migrations
└── src/test/java/                    # Unit, integration, and architecture tests
```

### Parent POM Configuration

```xml
<parent>
    <groupId>com.iqkv</groupId>
    <artifactId>boot-parent-pom</artifactId>
    <version>0.25.0-SNAPSHOT</version>
</parent>

<!-- Platform modules -->
<modules>
    <module>gripday-user-service</module>
    <module>gripday-gateway-service</module>
    <module>gripday-bookstore-service</module>
</modules>
```

## 🤖 AI Agent Guidelines

### Technology Stack Context

Before making recommendations, agents should understand the platform's technology stack:

**Runtime & Framework**

- Java 21 with modern features (records, pattern matching, text blocks, var)
- Spring Boot 3.5.6 with Spring Cloud 2025.0.0
- Parent POM: `com.iqkv:boot-parent-pom:0.25.0-SNAPSHOT`

**Data & Caching**

- PostgreSQL 15+ with Liquibase migrations
- Redis for distributed caching, rate limiting, and sessions
- Spring Data JPA with Hibernate

**Security**

- JWT with RSA256 (JJWT library)
- Spring Security OAuth2 Resource Server
- Spring Security OAuth2 Authorization Server (User Service)
- Method-level security with `@PreAuthorize`

**Reactive Programming**

- Spring Cloud Gateway with WebFlux (Gateway Service only)
- Project Reactor for reactive streams
- Reactive Redis operations

**Observability**

- OpenTelemetry for distributed tracing
- Prometheus for metrics collection
- Grafana for visualization
- Loki for log aggregation
- Promtail for log collection
- Logstash Logback Encoder for structured JSON logging

**Testing**

- JUnit 5 for unit tests
- Testcontainers for integration tests
- ArchUnit for architecture validation
- Spring Modulith for modularity testing
- H2 for test databases

**API Documentation**

- SpringDoc OpenAPI 3
- Swagger UI for interactive documentation

**Build & Deployment**

- Maven 3.9.0+ with multi-module structure
- Docker with service-specific Dockerfiles
- Docker Compose for local development
- Kubernetes with Helm charts
- Environment profiles: local, staging, production

### Agent Roles & Responsibilities

#### 1. Microservices Architecture Agent

**Purpose**: Guide microservices design, inter-service communication, and distributed system patterns

**Responsibilities**:

- Service decomposition and bounded context design
- API gateway pattern implementation
- Service-to-service communication patterns
- Database per service pattern
- Distributed transaction handling
- Service discovery and registration
- Circuit breaker and resilience patterns
- Multi-tenancy architecture

**Activation Triggers**:

- New service creation requests
- Inter-service communication design
- Gateway routing configuration
- Service mesh considerations
- Distributed system challenges

**Key Patterns in This Project**:

- Database per service (separate PostgreSQL instances)
- API Gateway pattern (Gateway Service)
- JWT-based user context propagation
- Correlation ID tracking across services
- Circuit breaker with Resilience4j
- Reactive gateway with Spring Cloud Gateway

#### 2. Spring Boot & Java Expert Agent

**Purpose**: Expert Java 21+ development with Spring Boot 3.x best practices

**Responsibilities**:

- Modern Java feature implementation (records, pattern matching, sealed classes)
- Spring Boot 3.x configuration and best practices
- Dependency injection and component design
- Configuration management with profiles
- Actuator and health check implementation
- Spring Data JPA optimization
- Transaction management

**Activation Triggers**:

- `*.java`, `pom.xml`, `application*.yml` modifications
- Spring Boot configuration questions
- Dependency management
- Performance optimization
- Code refactoring requests

**Key Patterns in This Project**:

- Java records for DTOs (immutable data carriers)
- `@ConfigurationProperties` with records for type-safe configuration
- Environment-specific profiles (local, staging, production)
- Liquibase for database migrations
- Repository and service layer separation
- DTO pattern for API boundaries

#### 3. Security & Authentication Agent

**Purpose**: Specialized in Spring Security, JWT, OAuth2, and authentication patterns

**Responsibilities**:

- JWT token generation and validation
- Spring Security configuration
- OAuth2 Resource Server setup
- Role-based access control (RBAC)
- Method-level security with `@PreAuthorize`
- Security audit logging
- Multi-tenant security isolation
- Token blacklisting and session management

**Activation Triggers**:

- `*Security*.java`, `*Auth*.java` file modifications
- JWT configuration changes
- Authentication and authorization implementation
- Security vulnerability assessments
- Access control requirements

**Key Patterns in This Project**:

- JWT-based stateless authentication (User Service)
- Token validation at Gateway with context propagation
- RSA256 key pair for JWT signing
- JWK Set endpoint for public key distribution
- Redis-backed token blacklist
- Account lockout protection
- Email verification workflows
- Multi-tenant data isolation with schema-per-tenant

#### 4. Reactive Programming Agent

**Purpose**: Expert in reactive programming with Spring WebFlux and Project Reactor

**Responsibilities**:

- Reactive filter chain implementation
- Non-blocking I/O patterns
- Backpressure handling
- Reactive Redis operations
- Reactive security configuration
- Error handling in reactive streams

**Activation Triggers**:

- Gateway Service modifications
- Reactive filter implementation
- WebFlux configuration
- Performance optimization for high-throughput scenarios

**Key Patterns in This Project**:

- Spring Cloud Gateway with reactive filters
- Reactive rate limiting with Redis
- Reactive JWT validation
- Filter chain pattern for request processing
- Mono and Flux for reactive streams

#### 5. Observability & Monitoring Agent

**Purpose**: Implement comprehensive observability with metrics, logging, and tracing

**Responsibilities**:

- Structured JSON logging configuration
- Correlation ID generation and propagation
- OpenTelemetry distributed tracing
- Prometheus metrics implementation
- Custom business metrics
- Health check configuration
- Grafana dashboard design

**Activation Triggers**:

- Logging configuration changes
- Metrics implementation
- Tracing setup
- Performance monitoring
- Debugging distributed system issues

**Key Patterns in This Project**:

- Correlation ID propagation across all services
- Structured JSON logging with Logstash Logback Encoder
- OpenTelemetry for distributed tracing
- Prometheus metrics with Micrometer
- Grafana dashboards for visualization
- Loki for log aggregation
- Health checks with Spring Boot Actuator

#### 6. Testing & Quality Assurance Agent

**Purpose**: Ensure comprehensive test coverage and code quality

**Responsibilities**:

- Unit test implementation with JUnit 5
- Integration tests with Testcontainers
- Architecture tests with ArchUnit
- Spring Modulith validation
- Test coverage analysis with JaCoCo
- Security testing
- Performance testing

**Activation Triggers**:

- Test file modifications
- Coverage improvement requests
- Architecture validation
- Quality gate configuration

**Key Patterns in This Project**:

- AAA pattern (Arrange-Act-Assert) for unit tests
- Testcontainers for PostgreSQL integration tests
- H2 for lightweight test databases
- ArchUnit for enforcing architectural rules
- Spring Modulith for modularity validation
- JaCoCo with minimum coverage thresholds (60-90% depending on service)
- `@SpringBootTest` for integration tests
- `@WebMvcTest` for controller tests

#### 7. DevOps & Deployment Agent

**Purpose**: Container orchestration, CI/CD, and deployment automation

**Responsibilities**:

- Dockerfile optimization
- Docker Compose orchestration
- Kubernetes manifest creation
- Helm chart development
- CI/CD pipeline configuration
- Environment configuration management
- Health check and readiness probe setup

**Activation Triggers**:

- Dockerfile modifications
- Docker Compose changes
- Kubernetes manifest updates
- CI/CD pipeline configuration
- Deployment issues

**Key Patterns in This Project**:

- Multi-stage Docker builds for optimized images
- Docker Compose for local development with health checks
- Service-specific docker-compose.yml files
- Environment-specific configuration (.env files)
- Kubernetes readiness and liveness probes
- Helm charts for Kubernetes deployment
- Graceful shutdown configuration

### Agent Interaction Protocols

#### Code Review Checklist

When reviewing code, agents should verify:

**Architecture & Design**

- [ ] Follows microservices patterns (database per service, API gateway)
- [ ] Proper separation of concerns (presentation, service, repository, domain)
- [ ] DTOs used for API boundaries (Java records preferred)
- [ ] Configuration externalized with profiles
- [ ] Multi-tenancy patterns applied where needed

**Security**

- [ ] JWT validation implemented correctly
- [ ] Role-based access control with `@PreAuthorize`
- [ ] Input validation with `@Valid` and Bean Validation
- [ ] No sensitive data in logs
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (proper output encoding)

**Performance**

- [ ] Database queries optimized with proper indexing
- [ ] Caching strategy implemented where appropriate
- [ ] Connection pooling configured
- [ ] Reactive patterns used in Gateway Service
- [ ] No N+1 query problems

**Observability**

- [ ] Correlation ID propagated in logs
- [ ] Structured JSON logging used
- [ ] Metrics exposed for critical operations
- [ ] Health checks implemented
- [ ] Error handling with proper logging

**Testing**

- [ ] Unit tests with AAA pattern
- [ ] Integration tests with Testcontainers
- [ ] Architecture tests with ArchUnit
- [ ] Test coverage meets minimum thresholds
- [ ] Security tests for protected endpoints

**Documentation**

- [ ] OpenAPI annotations on REST endpoints
- [ ] README updated with new features
- [ ] Complex business logic documented
- [ ] Configuration properties documented

#### Automated Quality Gates

```yaml
quality_gates:
  code_coverage:
    user_service: ">= 18% (instruction), >= 48% (line), >= 30% (branch)"
    gateway_service: ">= 27% (instruction)"
    bookstore_service: ">= 60% (instruction), >= 65% (line), >= 50% (branch)"

  code_style:
    tool: "Checkstyle"
    enforcement: "Maven build fails on violations"

  architecture:
    tool: "ArchUnit"
    rules:
      - "Services only accessed by controllers"
      - "No cyclic dependencies"
      - "Proper package structure"

  security:
    dependency_scanning: "OWASP Dependency Check"
    secret_detection: "No hardcoded secrets"

  commit_messages:
    format: "Conventional Commits"
    types: ["feat", "fix", "rfc", "docs", "style", "improvement", "enhancement", "refactor", "perf", "test", "chore", "build", "ci", "revert"]
    max_length: 220
    min_length: 6

## 📋 Development Standards

### Branch Strategy

The project uses a simplified Git workflow with the following branches:
```

dev (main development branch)
├── wip (work in progress)
├── feature/_ (new features)
├── bugfix/_ (bug fixes)
├── improvement/_ (enhancements)
├── library/_ (dependency updates)
├── prerelease/_ (release candidates)
├── hotfix/_ (production fixes)
└── rfc/\* (request for comments)

````

### Branch Naming Conventions

```bash
# Feature branches
feature/add-user-preferences
feature/implement-rate-limiting
feature/multi-tenant-support

# Bug fixes
bugfix/fix-jwt-expiration
bugfix/resolve-connection-leak

# Improvements
improvement/optimize-database-queries
improvement/enhance-error-messages

# Library updates
library/upgrade-spring-boot-3.5
library/update-testcontainers

# Hotfixes
hotfix/critical-security-patch
hotfix/production-data-fix

# RFC (Request for Comments)
rfc/new-authentication-flow
rfc/service-mesh-migration
````

### Commit Message Format (Conventional Commits)

The project enforces **Conventional Commits** with automated validation via GitHub Actions and Husky pre-commit hooks.

**Format:**

```
type(scope): subject

[optional body]

[optional footer]
```

**Allowed Types:**

- `feat`: New feature
- `fix`: Bug fix
- `rfc`: Request for comments / architectural proposal
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `improvement`: Enhancements to existing features
- `enhancement`: Similar to improvement
- `refactor`: Code refactoring without changing functionality
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks, dependency updates
- `build`: Build system or external dependency changes
- `ci`: CI/CD pipeline changes
- `revert`: Reverting previous commits

**Rules:**

- Subject line: 6-220 characters
- Use lowercase for type
- Use imperative mood ("add" not "added" or "adds")
- No period at the end of subject line
- Body and footer are optional

**Examples:**

```bash
# Simple feature
feat(user-service): add email verification endpoint

# Bug fix with details
fix(gateway): resolve rate limiting for tenant requests

The sliding window algorithm was not correctly resetting
the counter for tenant-specific quotas. Updated the Redis
key pattern to include tenant ID.

Closes #123

# Performance improvement
perf(bookstore): optimize book search query with indexes

Added composite index on (title, author, category) to improve
search performance by 80%.

# Documentation
docs(readme): update local development setup instructions

# Refactoring
refactor(user-service): extract JWT logic to separate service

# Breaking change
feat(auth)!: migrate to RSA256 JWT signing

BREAKING CHANGE: JWT tokens now use RSA256 instead of HS256.
Existing tokens will be invalidated. Clients must fetch new
tokens after deployment.
```

**Validation:**

- Automated via GitHub Actions on push and PR
- Pre-commit hook with Husky and commitlint
- PR title must follow the same format

### Code Quality Standards

#### Java Code Standards (Java 21)

**Use Modern Java Features:**

```java
// Records for immutable DTOs
public record UserCreateRequest(@NotBlank @Size(max = 100) String username, @Email String email, @NotBlank String password) {}

// Records for configuration properties
@ConfigurationProperties(prefix = "gripday.auth.jwt")
public record JwtProperties(String secret, long expiration, long refreshExpiration) {}

// Pattern matching with switch expressions
public String getUserRole(User user) {
  return switch (user.getRole()) {
    case ADMIN -> "Administrator";
    case USER -> "Regular User";
    case GUEST -> "Guest User";
    default -> "Unknown";
  };
}

// Text blocks for multi-line strings
var emailTemplate = """
  Dear %s,

  Please verify your email by clicking the link below:
  %s

  Best regards,
  Gripday Team
  """.formatted(user.getUsername(), verificationLink);

// var for local variables (when type is obvious)
var users = userRepository.findAll();

var response = ResponseEntity.ok(userDto);
```

**Service Layer Pattern:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserDto createUser(UserCreateRequest request) {
    log.info("Creating user with username: {}", request.username());

    // Business logic
    var user = User.builder().username(request.username()).email(request.email()).password(passwordEncoder.encode(request.password())).enabled(false).build();

    var savedUser = userRepository.save(user);
    return UserMapper.toDto(savedUser);
  }
}
```

**REST Controller Pattern:**

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations")
public class UserResource {

  private final UserService userService;

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "404", description = "User not found")
  public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    return userService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
}
```

**Security Annotations:**

```java
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
  userService.deleteUser(id);
  return ResponseEntity.noContent().build();
}
```

#### Testing Standards

**Unit Tests - AAA Pattern:**

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("Should create user with encoded password")
  void shouldCreateUserWithEncodedPassword() {
    // Arrange
    var request = new UserCreateRequest("john.doe", "john@example.com", "password123");
    var encodedPassword = "encoded_password";

    when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenAnswer((i) -> i.getArgument(0));

    // Act
    var result = userService.createUser(request);

    // Assert
    assertThat(result.username()).isEqualTo("john.doe");
    verify(passwordEncoder).encode("password123");
    verify(userRepository).save(any(User.class));
  }
}
```

**Integration Tests with Testcontainers:**

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("test_db").withUsername("test").withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserService userService;

  @Test
  void shouldCreateAndRetrieveUser() {
    // Test implementation
  }
}
```

**Architecture Tests with ArchUnit:**

```java
@AnalyzeClasses(packages = "org.gripday.userservice")
class ArchitectureTest {

  @ArchTest
  static final ArchRule servicesOnlyAccessedByControllersOrServices = classes()
    .that()
    .resideInAPackage("..service..")
    .should()
    .onlyBeAccessed()
    .byAnyPackage("..presentation..", "..service..", "..config..");

  @ArchTest
  static final ArchRule repositoriesShouldBeInterfaces = classes().that().resideInAPackage("..repository..").should().beInterfaces();

  @ArchTest
  static final ArchRule controllersShouldBeAnnotated = classes()
    .that()
    .resideInAPackage("..presentation..")
    .and()
    .haveSimpleNameEndingWith("Resource")
    .should()
    .beAnnotatedWith(RestController.class);
}
```

**Security Tests:**

```java
@WebMvcTest(UserResource.class)
@Import(SecurityConfiguration.class)
class UserResourceSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldAllowAdminToDeleteUser() throws Exception {
    mockMvc.perform(delete("/api/v1/admin/users/1")).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "USER")
  void shouldDenyUserToDeleteUser() throws Exception {
    mockMvc.perform(delete("/api/v1/admin/users/1")).andExpect(status().isForbidden());
  }
}
```

## 🔄 Workflow Management

### Pull Request Guidelines

#### PR Title Format

PR titles must follow Conventional Commits format (enforced by GitHub Actions):

```
type(scope): description

Examples:
feat(user-service): add email verification endpoint
fix(gateway): resolve rate limiting for tenant requests
docs(readme): update deployment instructions
refactor(bookstore): extract inventory logic to separate service
```

**Validation Rules:**

- Allowed types: `feat`, `fix`, `rfc`, `docs`, `style`, `improvement`, `enhancement`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`, `revert`
- Length: 6-220 characters
- Case-sensitive type prefix

#### PR Description Template

```markdown
## Description

Brief description of changes and motivation.

## Type of Change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Changes Made

- List specific changes
- Include affected services
- Mention configuration changes

## How Has This Been Tested?

- [ ] Unit tests added/updated
- [ ] Integration tests with Testcontainers
- [ ] Architecture tests with ArchUnit
- [ ] Manual testing performed
- [ ] Security testing (if applicable)

## Test Coverage

- Current coverage: X%
- Coverage change: +/-X%
- Meets minimum threshold: Yes/No

## Checklist

- [ ] Code follows Checkstyle rules
- [ ] Commit messages follow Conventional Commits
- [ ] Self-review completed
- [ ] Complex logic is documented
- [ ] Tests cover new/modified code
- [ ] All tests pass locally (`mvn clean verify`)
- [ ] No new Checkstyle violations
- [ ] OpenAPI documentation updated
- [ ] README updated (if needed)
- [ ] Breaking changes documented

## Database Changes

- [ ] Liquibase changelog added
- [ ] Migration tested locally
- [ ] Rollback script provided (if needed)

## Configuration Changes

- [ ] Environment variables documented
- [ ] application.yml updated
- [ ] .env.example updated

## Security Considerations

- [ ] No sensitive data in logs
- [ ] Input validation implemented
- [ ] Authorization checks in place
- [ ] No hardcoded secrets

## Performance Impact

- [ ] No N+1 query problems
- [ ] Caching strategy considered
- [ ] Database indexes added (if needed)
- [ ] Load testing performed (if applicable)

## Screenshots (if applicable)

## Additional Notes
```

#### Review Criteria

**Automated Checks (Must Pass):**

- ✅ All unit and integration tests pass
- ✅ Checkstyle validation passes
- ✅ JaCoCo coverage meets minimum thresholds
- ✅ Commit messages follow Conventional Commits
- ✅ PR title follows Conventional Commits
- ✅ No merge conflicts with target branch

**Manual Review Focus:**

```yaml
review_checklist:
  architecture:
    - Follows microservices patterns
    - Proper service boundaries
    - Database per service respected
    - No tight coupling between services

  code_quality:
    - Modern Java 21 features used appropriately
    - Records for DTOs
    - Proper exception handling
    - Logging with correlation IDs
    - No code duplication

  security:
    - JWT validation correct
    - Authorization with @PreAuthorize
    - Input validation with @Valid
    - No SQL injection vulnerabilities
    - No sensitive data exposure

  testing:
    - AAA pattern in unit tests
    - Testcontainers for integration tests
    - Architecture tests updated
    - Edge cases covered
    - Security tests for protected endpoints

  documentation:
    - OpenAPI annotations on endpoints
    - Complex logic documented
    - README updated
    - Configuration changes documented

  observability:
    - Correlation ID propagated
    - Structured logging used
    - Metrics exposed
    - Error handling with proper logging

  performance:
    - Database queries optimized
    - Proper indexing
    - Caching strategy
    - No performance regressions
```

**Blocking Conditions:**

- ❌ Failing tests
- ❌ Checkstyle violations
- ❌ Coverage below minimum threshold
- ❌ Security vulnerabilities
- ❌ Missing required documentation
- ❌ Merge conflicts

### Release Management

#### Versioning Strategy

The project uses **Semantic Versioning** with SNAPSHOT versions during development:

```
MAJOR.MINOR.PATCH[-SNAPSHOT]

Current version: 0.0.0-SNAPSHOT (pre-release development)

Examples:
0.0.0-SNAPSHOT  # Development version
0.1.0           # First minor release
0.1.1           # Patch release
1.0.0           # First major release
1.1.0           # Minor feature addition
2.0.0           # Breaking changes
```

**Version Increment Rules:**

- **MAJOR**: Breaking changes (API incompatibility, major architecture changes)
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

#### Release Process

**1. Preparation Phase**

```bash
# Create release branch
git checkout -b prerelease/v1.0.0 dev

# Update version in all POMs
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit

# Update CHANGELOG.md
# Document all changes since last release

# Run full test suite
mvn clean verify

# Run security scan
mvn org.owasp:dependency-check-maven:check

# Build Docker images
docker-compose build

# Test in staging environment
docker-compose -f docker-compose.staging.yml up
```

**2. Release Phase**

```bash
# Merge to main (or production branch)
git checkout main
git merge prerelease/v1.0.0

# Create Git tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Build and push Docker images
docker build -t gripday/user-service:1.0.0 -f gripday-user-service/Dockerfile .
docker build -t gripday/gateway-service:1.0.0 -f gripday-gateway-service/Dockerfile .
docker build -t gripday/bookstore-service:1.0.0 -f gripday-bookstore-service/Dockerfile .

docker push gripday/user-service:1.0.0
docker push gripday/gateway-service:1.0.0
docker push gripday/bookstore-service:1.0.0

# Deploy to production
kubectl apply -f k8s/production/
# or
helm upgrade gripday ./helm/gripday --namespace production
```

**3. Post-Release Phase**

```bash
# Merge back to dev
git checkout dev
git merge main

# Bump to next SNAPSHOT version
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
mvn versions:commit

# Update documentation
# - Update README with new version
# - Update API documentation
# - Create release notes

# Notify stakeholders
# - Send release announcement
# - Update status page
# - Notify dependent teams
```

#### Release Checklist

```yaml
pre_release:
  - [ ] All tests pass (unit, integration, architecture)
  - [ ] Code coverage meets thresholds
  - [ ] Checkstyle validation passes
  - [ ] Security scan clean (no critical vulnerabilities)
  - [ ] CHANGELOG.md updated
  - [ ] Version numbers updated in all POMs
  - [ ] Database migrations tested
  - [ ] Configuration changes documented
  - [ ] API documentation updated
  - [ ] README updated

staging_validation:
  - [ ] Deploy to staging environment
  - [ ] Smoke tests pass
  - [ ] Integration tests with real dependencies
  - [ ] Performance testing
  - [ ] Security testing
  - [ ] Load testing
  - [ ] Rollback procedure tested

production_deployment:
  - [ ] Backup databases
  - [ ] Deploy during maintenance window
  - [ ] Run database migrations
  - [ ] Deploy services in order (user → gateway → bookstore)
  - [ ] Verify health checks
  - [ ] Monitor logs and metrics
  - [ ] Verify critical user flows
  - [ ] Update monitoring dashboards

post_release:
  - [ ] Merge release branch back to dev
  - [ ] Create GitHub release with notes
  - [ ] Update documentation site
  - [ ] Notify stakeholders
  - [ ] Monitor production metrics
  - [ ] Address any issues immediately
```

## 🔒 Security Guidelines

### Security Architecture

**Authentication Flow:**

1. User authenticates with User Service (POST /api/v1/auth/login)
2. User Service generates JWT with RSA256 signature
3. Client includes JWT in Authorization header for subsequent requests
4. Gateway validates JWT using JWK Set from User Service
5. Gateway extracts user context and propagates to downstream services
6. Services validate JWT independently or trust Gateway headers

**Multi-Tenant Security:**

- Schema-per-tenant isolation in User Service
- Tenant context extracted from JWT claims
- Tenant ID propagated via X-Tenant-ID header
- Cross-tenant access prevention at service layer

### Security Checklist

**Authentication & Authorization:**

```yaml
authentication:
  - [ ] JWT tokens use RSA256 (not HS256)
  - [ ] Token expiration configured (15min access, 7 days refresh)
  - [ ] Refresh token rotation implemented
  - [ ] Token blacklist with Redis for logout
  - [ ] JWK Set endpoint exposed for public key distribution
  - [ ] Account lockout after 5 failed attempts (15min duration)
  - [ ] Email verification required for new accounts

authorization:
  - [ ] Role-based access control with @PreAuthorize
  - [ ] Method-level security enabled
  - [ ] Admin endpoints under /admin path
  - [ ] Public endpoints explicitly configured
  - [ ] Tenant isolation enforced
  - [ ] User can only access own data (unless admin)

input_validation:
  - [ ] Bean Validation annotations (@Valid, @NotBlank, @Email)
  - [ ] Input sanitization for SQL injection prevention
  - [ ] XSS prevention with proper output encoding
  - [ ] Path traversal prevention
  - [ ] File upload validation (if applicable)
  - [ ] Request size limits configured

data_protection:
  - [ ] Passwords hashed with BCrypt
  - [ ] Sensitive data not logged
  - [ ] PII encrypted at rest (if applicable)
  - [ ] HTTPS enforced in production
  - [ ] Secure headers configured (HSTS, CSP, X-Frame-Options)
  - [ ] CORS properly configured per environment
```

**Dependency Security:**

```yaml
dependency_management:
  - [ ] Regular dependency updates
  - [ ] OWASP Dependency Check in build
  - [ ] No known critical vulnerabilities
  - [ ] License compliance verified
  - [ ] Transitive dependencies reviewed

scanning:
  - [ ] Automated security scans in CI/CD
  - [ ] Container image scanning
  - [ ] Static code analysis
  - [ ] Secret detection in commits
```

**Infrastructure Security:**

```yaml
secrets_management:
  - [ ] No hardcoded secrets in code
  - [ ] Environment variables for configuration
  - [ ] .env files gitignored
  - [ ] Kubernetes secrets for production
  - [ ] Secret rotation procedures documented

network_security:
  - [ ] Service-to-service communication secured
  - [ ] Database connections encrypted
  - [ ] Redis connections secured
  - [ ] API Gateway as single entry point
  - [ ] Internal services not exposed publicly

logging_security:
  - [ ] No passwords in logs
  - [ ] No JWT tokens in logs
  - [ ] No PII in logs (or masked)
  - [ ] Correlation IDs for tracing
  - [ ] Security events logged (login, logout, failed attempts)
```

### Secrets Management

**Local Development:**

```bash
# Use .env files (gitignored)
# Example: gripday-user-service/.env.local

SPRING_DATASOURCE_PASSWORD=local_password
GRIPDAY_AUTH_JWT_SECRET=your-256-bit-secret-key-here
SPRING_DATA_REDIS_PASSWORD=redis_password
SPRING_MAIL_PASSWORD=mail_password
```

**Staging/Production:**

```bash
# Use Kubernetes secrets
kubectl create secret generic user-service-secrets \
  --from-literal=database-password='prod_password' \
  --from-literal=jwt-secret='prod_jwt_secret' \
  --from-literal=redis-password='prod_redis_password' \
  --namespace=production

# Reference in deployment
env:
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: user-service-secrets
        key: database-password
```

**Environment Variable Naming:**

```bash
# Spring Boot property mapping
SPRING_DATASOURCE_PASSWORD → spring.datasource.password
GRIPDAY_AUTH_JWT_SECRET → gripday.auth.jwt.secret
SPRING_DATA_REDIS_PASSWORD → spring.data.redis.password
```

### Security Testing

**Unit Tests:**

```java
@Test
void shouldRejectInvalidJwtToken() {
  var invalidToken = "invalid.jwt.token";
  assertThrows(JwtException.class, () -> jwtService.validateToken(invalidToken));
}

@Test
void shouldEnforcePasswordStrength() {
  var weakPassword = "123";
  assertThrows(ValidationException.class, () -> userService.createUser(request));
}
```

**Integration Tests:**

```java
@Test
@WithMockUser(roles = "USER")
void shouldDenyAccessToAdminEndpoint() throws Exception {
  mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
}

@Test
void shouldPreventSqlInjection() throws Exception {
  var maliciousInput = "'; DROP TABLE users; --";
  mockMvc.perform(get("/api/v1/users/search?username=" + maliciousInput)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
}
```

## 📊 Quality Assurance

### Code Quality Metrics

**Test Coverage Thresholds (JaCoCo):**

```yaml
user_service:
  instruction_coverage: ">= 18%"
  line_coverage: ">= 48%"
  branch_coverage: ">= 30%"
  note: "Lower thresholds due to complex multi-tenant logic"

gateway_service:
  instruction_coverage: ">= 27%"
  note: "Reactive code with extensive configuration classes"

bookstore_service:
  instruction_coverage: ">= 60%"
  line_coverage: ">= 65%"
  branch_coverage: ">= 50%"
  note: "Highest coverage as reference implementation"
```

**Code Style (Checkstyle):**

- Enforced via Maven build
- Build fails on violations
- Configuration inherited from parent POM
- Consistent across all services

**Architecture Validation (ArchUnit):**

- Services only accessed by controllers or other services
- Repositories must be interfaces
- Controllers annotated with @RestController
- No cyclic dependencies
- Proper package structure

**Code Quality Tools:**

```yaml
static_analysis:
  - checkstyle: "Code style enforcement"
  - archunit: "Architecture rules validation"
  - jacoco: "Test coverage measurement"
  - spotbugs: "Bug pattern detection (optional)"
  - sonarqube: "Comprehensive quality analysis (optional)"

testing_tools:
  - junit5: "Unit testing framework"
  - mockito: "Mocking framework"
  - testcontainers: "Integration testing with real dependencies"
  - spring_modulith: "Modularity validation"
  - assertj: "Fluent assertions"
```

### Automated Quality Checks

**Maven Build Configuration:**

```xml
<!-- JaCoCo for code coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>coverage-check</id>
            <phase>test</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum> <!-- Adjust per service -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Checkstyle for code style -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <!-- Configuration inherited from parent POM -->
</plugin>

<!-- Surefire for unit tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
            <exclude>**/*IT.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

**Quality Gates in CI/CD:**

```yaml
# GitHub Actions workflow (example)
quality_checks:
  - name: "Build and Test"
    run: mvn clean verify

  - name: "Code Coverage"
    run: mvn jacoco:report
    fail_if: "Coverage below threshold"

  - name: "Checkstyle"
    run: mvn checkstyle:check
    fail_if: "Style violations found"

  - name: "Architecture Tests"
    run: mvn test -Dtest=ArchitectureTest
    fail_if: "Architecture rules violated"

  - name: "Security Scan"
    run: mvn org.owasp:dependency-check-maven:check
    fail_if: "Critical vulnerabilities found"
```

**Local Quality Checks:**

```bash
# Run all quality checks locally before pushing
mvn clean verify

# Run only tests
mvn test

# Run only integration tests
mvn verify -DskipUnitTests

# Check code coverage
mvn jacoco:report
# View report: target/site/jacoco/index.html

# Check code style
mvn checkstyle:check

# Run architecture tests
mvn test -Dtest=ArchitectureTest

# Security scan
mvn org.owasp:dependency-check-maven:check
```

**Code Formatting:**

```bash
# Prettier for non-Java files (YAML, JSON, Markdown)
pnpm prettier:check
pnpm prettier:write

# Husky pre-commit hooks
# - Runs Prettier on staged files
# - Validates commit message format
```

## 🚀 CI/CD Pipeline

### Current GitHub Actions Workflows

**1. Node.js CI (build-nodejs-project.yml)**

- Triggers: Push to `dev`, `wip` branches; PRs to `dev`
- Node version: 22.x
- Package manager: pnpm 10.20.0
- Checks: Prettier formatting

**2. Commit Message Check (check-commit-message.yml)**

- Triggers: PRs and pushes to `dev`, `wip`, `rfc/*`, `feature/*`, `bugfix/*`, `improvement/*`, `library/*`, `prerelease/*`, `hotfix/*`
- Validates: Conventional Commits format
- Pattern: `^(feat|fix|rfc|docs|style|improvement|enhancement|refactor|perf|test|chore|build|ci|revert)(.+?)?: .+`
- Max length: 220 characters

**3. PR Title Check (check-pr-title.yml)**

- Triggers: PR opened, edited, synchronized, reopened
- Validates: PR title follows Conventional Commits
- Same pattern as commit messages

**4. Dependabot Auto-Approve (auto-approve-dependabot-pr.yml)**

- Automatically approves Dependabot PRs

### Recommended CI/CD Pipeline (To Implement)

```yaml
name: Gripday Platform CI/CD

on:
  push:
    branches: [dev, wip]
  pull_request:
    branches: [dev]

jobs:
  # Job 1: Build and Test Java Services
  test-java:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [user-service, gateway-service, bookstore-service]
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: "maven"

      - name: Build and Test ${{ matrix.service }}
        run: |
          cd gripday-${{ matrix.service }}
          mvn clean verify

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: gripday-${{ matrix.service }}/target/site/jacoco/jacoco.xml
          flags: ${{ matrix.service }}

      - name: Archive Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results-${{ matrix.service }}
          path: gripday-${{ matrix.service }}/target/surefire-reports/

  # Job 2: Code Quality Checks
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: "maven"

      - name: Checkstyle
        run: mvn checkstyle:check

      - name: Architecture Tests
        run: mvn test -Dtest=*ArchitectureTest

  # Job 3: Security Scan
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: "maven"

      - name: OWASP Dependency Check
        run: mvn org.owasp:dependency-check-maven:check

      - name: Upload Security Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: security-report
          path: target/dependency-check-report.html

  # Job 4: Build Docker Images
  build-images:
    needs: [test-java, code-quality, security]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/dev'
    strategy:
      matrix:
        service: [user-service, gateway-service, bookstore-service]
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Docker Image
        run: |
          docker build -t gripday/${{ matrix.service }}:${{ github.sha }} \
            -f gripday-${{ matrix.service }}/Dockerfile .

      - name: Save Docker Image
        run: |
          docker save gripday/${{ matrix.service }}:${{ github.sha }} \
            -o ${{ matrix.service }}.tar

      - name: Upload Image Artifact
        uses: actions/upload-artifact@v3
        with:
          name: ${{ matrix.service }}-image
          path: ${{ matrix.service }}.tar

  # Job 5: Integration Tests
  integration-tests:
    needs: build-images
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download Image Artifacts
        uses: actions/download-artifact@v3

      - name: Load Docker Images
        run: |
          docker load -i user-service-image/user-service.tar
          docker load -i gateway-service-image/gateway-service.tar
          docker load -i bookstore-service-image/bookstore-service.tar

      - name: Start Services
        run: docker-compose up -d

      - name: Wait for Services
        run: |
          timeout 300 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 5; done'

      - name: Run E2E Tests
        run: |
          cd e2e-tests
          npm install
          npm test

      - name: Collect Logs
        if: always()
        run: docker-compose logs > docker-logs.txt

      - name: Upload Logs
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: docker-logs
          path: docker-logs.txt
```

### Environment Management

**Local Development:**

```yaml
environment: local
database:
  - PostgreSQL via Docker Compose
  - H2 for tests
redis: Docker Compose
external_services: Mocked or local instances
logging: DEBUG level
configuration: .env.local files
```

**Staging:**

```yaml
environment: staging
database: PostgreSQL (dedicated staging instance)
redis: Redis cluster (staging)
external_services: Staging APIs
logging: INFO level
configuration:
  - application-staging.yml
  - .env.staging files
  - Kubernetes ConfigMaps and Secrets
deployment: Kubernetes with Helm
monitoring: Full observability stack
```

**Production:**

```yaml
environment: production
database: PostgreSQL (production cluster with replication)
redis: Redis cluster (production with persistence)
external_services: Production APIs
logging: WARN/ERROR level
configuration:
  - application-production.yml
  - .env.production files
  - Kubernetes Secrets
deployment: Kubernetes with Helm
monitoring: Full observability stack with alerting
backup: Automated database backups
scaling: Horizontal pod autoscaling
```

### Deployment Commands

**Local:**

```bash
# Start all services
docker-compose up

# Start specific service
cd gripday-user-service
docker-compose up
```

**Staging:**

```bash
# Deploy with Helm
helm upgrade --install gripday ./helm/gripday \
  --namespace staging \
  --values helm/gripday/values-staging.yaml

# Or with kubectl
kubectl apply -f k8s/staging/
```

**Production:**

```bash
# Deploy with Helm
helm upgrade --install gripday ./helm/gripday \
  --namespace production \
  --values helm/gripday/values-production.yaml \
  --wait \
  --timeout 10m

# Verify deployment
kubectl rollout status deployment/user-service -n production
kubectl rollout status deployment/gateway-service -n production
kubectl rollout status deployment/bookstore-service -n production
```

## 📚 Documentation Standards

### README Structure

Each service follows a consistent README structure:

```markdown
# 🔐 Service Name

> Brief tagline describing the service purpose

## Business Purpose

What business problems does this service solve?

- Bullet points of key capabilities
- Target use cases
- Value proposition

## Overview

High-level description of the service and its role in the platform.

## What It Demonstrates

### Key Pattern 1

- Implementation details
- Design decisions
- Best practices

### Key Pattern 2

- More patterns...

## Architecture Patterns

### Key Design Patterns

- Repository pattern
- Service layer
- DTO pattern
- Caching strategy

### API Design

- RESTful principles
- Versioning approach
- Error handling

## Technical Highlights

### Performance Optimization

### Data Management

### Testing Approach

### Operational Features

## Use Cases Implemented

### Use Case 1

- Detailed description
- API endpoints involved

## API Examples

### Public Endpoints

- List of endpoints with descriptions

### Protected Endpoints

- Authenticated endpoints

### Admin Endpoints

- Admin-only operations

### Monitoring Endpoints

- Health checks
- Metrics

## Learning Points

What can developers learn from this implementation?

## Adapting for Your Domain

How to adapt these patterns for other use cases.

## Integration with Other Services

How this service integrates with the platform.

---

**Use this as a blueprint** for similar implementations.
```

### API Documentation with OpenAPI

**Controller Annotations:**

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations and profile management")
public class UserResource {

  private final UserService userService;

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier. Requires authentication.", security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponses(
    {
      @ApiResponse(responseCode = "200", description = "User found successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
    }
  )
  public ResponseEntity<UserDto> getUser(@Parameter(description = "User ID", example = "1", required = true) @PathVariable Long id) {
    return userService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Create new user", description = "Creates a new user account. Requires ADMIN or SUPER_ADMIN role.", security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = UserDto.class)))
  @ApiResponse(responseCode = "400", description = "Invalid input data")
  @ApiResponse(responseCode = "409", description = "User already exists")
  public ResponseEntity<UserDto> createUser(@Parameter(description = "User creation request", required = true) @Valid @RequestBody UserCreateRequest request) {
    var user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }
}
```

**DTO Documentation:**

```java
@Schema(description = "User data transfer object")
public record UserDto(
  @Schema(description = "User unique identifier", example = "1") Long id,

  @Schema(description = "Username", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED) String username,

  @Schema(description = "Email address", example = "john.doe@example.com") String email,

  @Schema(description = "User roles", example = "[\"USER\", \"ADMIN\"]") List<String> roles,

  @Schema(description = "Account enabled status", example = "true") Boolean enabled,

  @Schema(description = "Account creation timestamp", example = "2024-01-15T10:30:00Z") Instant createdAt
) {}
```

**Security Scheme Configuration:**

```java
@Configuration
@OpenAPIDefinition(
  info = @Info(
    title = "Gripday User Service API",
    version = "1.0.0",
    description = "Authentication and user management microservice",
    contact = @Contact(name = "Gripday Team", email = "support@gripday.com"),
    license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
  ),
  servers = {
    @Server(url = "http://localhost:8080", description = "Local development"),
    @Server(url = "https://api-staging.gripday.com", description = "Staging"),
    @Server(url = "https://api.gripday.com", description = "Production"),
  },
  security = @SecurityRequirement(name = "bearer-jwt")
)
@SecurityScheme(
  name = "bearer-jwt",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT",
  description = "JWT authentication token obtained from /api/v1/auth/login endpoint"
)
public class OpenApiConfig {}
```

### Code Documentation

**When to Document:**

- Complex business logic
- Non-obvious algorithms
- Security-critical code
- Public APIs
- Configuration classes

**When NOT to Document:**

- Self-explanatory code
- Simple getters/setters
- Obvious implementations

**Examples:**

```java
/**
 * Validates JWT token and extracts user context.
 *
 * <p>This method performs the following validations:
 * <ul>
 *   <li>Token signature verification using RSA public key</li>
 *   <li>Token expiration check</li>
 *   <li>Token blacklist verification in Redis</li>
 *   <li>Required claims presence (sub, roles, tenantId)</li>
 * </ul>
 *
 * @param token JWT token string (without "Bearer " prefix)
 * @return UserContext containing user ID, roles, and tenant information
 * @throws JwtException if token is invalid, expired, or blacklisted
 * @throws IllegalArgumentException if token is null or empty
 */
public UserContext validateToken(String token) {
  // Implementation
}

/**
 * Implements sliding window rate limiting algorithm using Redis.
 *
 * <p>The algorithm maintains a sorted set in Redis with timestamps as scores.
 * Old entries outside the time window are automatically removed, and the
 * current count is compared against the configured limit.
 *
 * <p>Redis key pattern: rate_limit:{tenantId}:{endpoint}
 * <p>TTL: Automatically expires after window duration
 *
 * @param tenantId tenant identifier for rate limit isolation
 * @param endpoint API endpoint being accessed
 * @param limit maximum requests allowed in the time window
 * @param windowSeconds time window in seconds
 * @return true if request is allowed, false if rate limit exceeded
 */
public boolean checkRateLimit(String tenantId, String endpoint, int limit, int windowSeconds) {
  // Implementation
}
```

## 🎯 Agent Decision Framework

### User Confirmation Policy

**CRITICAL RULE: Always ask for user confirmation before applying changes.**

Agents must follow this approval workflow:

```yaml
before_making_changes:
  1. analyze: "Understand the request and identify required changes"
  2. explain: "Describe what changes will be made and why"
  3. assess_impact: "Evaluate impact, effort, and risk"
  4. present_options: "Offer alternatives if applicable"
  5. wait_for_approval: "STOP and wait for explicit user confirmation"
  6. apply_changes: "Only after user approves"
  7. verify: "Confirm changes work as expected"

exceptions:
  - read_only_operations: "Reading files, searching, analyzing code"
  - information_requests: "Answering questions, explaining concepts"
  - recommendations: "Suggesting approaches without implementing"

never_auto_apply:
  - code_changes: "Any modification to source files"
  - configuration_changes: "application.yml, pom.xml, Dockerfile, etc."
  - database_migrations: "Liquibase changelogs"
  - dependency_updates: "Adding or updating dependencies"
  - refactoring: "Code restructuring or optimization"
  - deletions: "Removing files, code, or configurations"
  - security_changes: "Authentication, authorization, secrets"
```

**Example Interaction:**

```
Agent: "I've analyzed the issue. To fix the JWT validation bug, I need to:

1. Update JwtService.java to check token expiration before validation
2. Add a test case for expired tokens
3. Update error handling to return 401 instead of 500

Impact: Low (bug fix, no breaking changes)
Effort: Small (< 1 hour)
Risk: Low (well-tested change, easy rollback)

Would you like me to proceed with these changes?"

User: "Yes, go ahead"

Agent: [Applies changes]
```

### When to Intervene

**High Priority (Immediate Action - Still Requires Approval):**

- 🚨 Security vulnerabilities (CVE alerts, exposed secrets)
- 🔴 Build failures blocking development
- 💥 Production incidents (service down, data corruption)
- ⚠️ Critical bugs affecting core functionality
- 🔒 Authentication/authorization failures

**Medium Priority (Plan and Execute - Requires Approval):**

- 📉 Code quality degradation (coverage drop, style violations)
- 🐌 Performance regressions (slow queries, memory leaks)
- ❌ Test failures (flaky tests, integration issues)
- 📦 Dependency updates (security patches, major versions)
- 🏗️ Architecture violations (ArchUnit failures)

**Low Priority (Continuous Improvement - Requires Approval):**

- 📝 Documentation gaps (missing API docs, outdated README)
- ♻️ Refactoring opportunities (code duplication, complexity)
- 🎨 Code style improvements (formatting, naming)
- 📊 Metrics and monitoring enhancements
- 🧪 Test coverage improvements

### Decision Matrix

**Impact Assessment:**

```yaml
user_impact:
  critical: "All users affected, service unavailable"
  high: "Major feature broken, workaround exists"
  medium: "Minor feature affected, limited users"
  low: "Internal improvement, no user-facing impact"

business_impact:
  critical: "Revenue loss, SLA breach, legal/compliance issue"
  high: "Customer complaints, reputation damage"
  medium: "Reduced efficiency, technical debt"
  low: "Code quality, maintainability"

technical_impact:
  critical: "System instability, data loss risk"
  high: "Performance degradation, scalability issues"
  medium: "Code maintainability, test reliability"
  low: "Code style, documentation"
```

**Effort Estimation:**

```yaml
development_effort:
  small: "< 1 day (simple fix, configuration change)"
  medium: "1-3 days (feature implementation, refactoring)"
  large: "1-2 weeks (major feature, architecture change)"
  xlarge: "> 2 weeks (platform upgrade, major refactoring)"

testing_effort:
  small: "Unit tests only"
  medium: "Unit + integration tests"
  large: "Full test suite + manual testing"
  xlarge: "Full test suite + performance + security testing"

deployment_effort:
  small: "Configuration change, no downtime"
  medium: "Service restart, brief downtime"
  large: "Database migration, coordinated deployment"
  xlarge: "Multi-service deployment, rollback plan required"
```

**Risk Evaluation:**

```yaml
implementation_risk:
  low: "Well-understood change, existing patterns"
  medium: "New pattern, requires research"
  high: "Complex change, multiple services affected"
  critical: "Breaking change, requires migration"

deployment_risk:
  low: "Backward compatible, easy rollback"
  medium: "Database migration, tested rollback"
  high: "Breaking change, complex rollback"
  critical: "Irreversible change, data migration"

rollback_capability:
  easy: "Configuration revert, no data changes"
  moderate: "Service rollback, database rollback script"
  difficult: "Multi-service coordination, data migration"
  impossible: "Irreversible data changes"
```

### Agent Recommendations

**For New Features (Always Get Approval First):**

1. **Analyze & Present Plan:**
   - Review service boundaries (does it belong in existing service?)
   - Check for similar implementations (reuse patterns)
   - Consider multi-tenancy implications
   - Assess impact, effort, and risk
   - **Present complete plan to user and wait for approval**

2. **After Approval, Implement:**
   - Plan database migrations (Liquibase changelog)
   - Design API with OpenAPI annotations
   - Implement with proper error handling
   - Add comprehensive tests (unit, integration, architecture)
   - Update documentation (README, API docs)
   - Consider observability (logging, metrics, tracing)

**For Bug Fixes (Always Get Approval First):**

1. **Analyze & Present Plan:**
   - Reproduce the issue with a failing test
   - Identify root cause (not just symptoms)
   - Assess impact and risk
   - **Present fix approach to user and wait for approval**

2. **After Approval, Fix:**
   - Fix with minimal changes
   - Add regression test
   - Verify fix doesn't break other functionality
   - Update documentation if behavior changed
   - Consider if similar bugs exist elsewhere

**For Refactoring (Always Get Approval First):**

1. **Analyze & Present Plan:**
   - Identify refactoring opportunity
   - Explain benefits and risks
   - Estimate effort
   - **Present refactoring plan to user and wait for approval**

2. **After Approval, Refactor:**
   - Ensure tests exist and pass
   - Make small, incremental changes
   - Run tests after each change
   - Verify no behavior changes
   - Update documentation if needed
   - Consider performance impact

**For Security Issues (Urgent but Still Requires Approval):**

1. **Immediate Assessment:**
   - Assess severity (CVSS score)
   - Check if exploitable in current configuration
   - Review affected services
   - **Present urgent fix plan to user and wait for approval**

2. **After Approval, Fix Urgently:**
   - Plan fix with minimal disruption
   - Test thoroughly (security tests)
   - Deploy urgently if critical
   - Document incident and resolution

### Common Patterns and Solutions

**Pattern: Adding a new REST endpoint**

```java
// 1. Define DTO (record)
public record BookCreateRequest(@NotBlank String title, @NotBlank String author) {}

// 2. Add service method
@Service
public class BookService {

  public BookDto createBook(BookCreateRequest request) {
    /* ... */
  }
}

// 3. Add controller endpoint
@RestController
@RequestMapping("/api/v1/bookstore/books")
public class BookResource {

  @PostMapping
  @Operation(summary = "Create new book")
  public ResponseEntity<BookDto> createBook(@Valid @RequestBody BookCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
  }
}

// 4. Add tests
@Test
void shouldCreateBook() {
  /* ... */
}
```

**Pattern: Adding database migration**

```xml
<!-- db/changelog/changes/001-create-books-table.xml -->
<changeSet id="001" author="developer">
    <createTable tableName="books">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="title" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
        <column name="author" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>
</changeSet>
```

**Pattern: Adding caching**

```java
@Service
public class BookService {

  @Cacheable(value = "books", key = "#id")
  public Optional<BookDto> findById(Long id) {
    return bookRepository.findById(id).map(BookMapper::toDto);
  }

  @CacheEvict(value = "books", key = "#id")
  public void deleteBook(Long id) {
    bookRepository.deleteById(id);
  }
}
```

---

This repository guidelines document serves as a comprehensive reference for maintaining high-quality, secure, and well-organized microservices while facilitating effective collaboration between human developers and AI agents. The patterns and practices documented here are derived from the actual Gripday platform implementation and represent production-ready approaches to building scalable distributed systems.
