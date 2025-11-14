# Developer Onboarding Guide

Welcome to the Gripday microservices platform! This guide will help you get up and running quickly as a developer on the platform.

## 🎯 Overview

The Gripday platform is an extensible microservices foundation built with Spring Boot 3.5.6, Spring Cloud 2025.0.0, and Java 21. It provides:

- **Centralized Authentication** with JWT tokens
- **API Gateway** with intelligent routing and rate limiting
- **Multi-tenant Architecture** with complete isolation
- **Comprehensive Observability** with metrics and tracing
- **Modern Java Development** with Java 21 features

## 🚀 Quick Setup (5 minutes)

### 1. Prerequisites Check

```bash
# Verify Java 21
java -version
# Should show: openjdk version "21.0.x"

# Verify Maven
mvn -version
# Should show: Apache Maven 3.9.x

# Verify Docker
docker --version && docker-compose --version
# Should show: Docker version 20.x and docker-compose version 2.x
```

### 2. Clone and Start

```bash
# Clone repository
git clone <repository-url>
cd gripday

# Start infrastructure
docker compose up -d postgres redis

# Build and start services
mvn clean package
cd gripday-user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local &
cd ../gripday-gateway-service && mvn spring-boot:run -Dspring-boot.run.profiles=local &
```

### 3. Verify Setup

```bash
# Run validation script
chmod +x scripts/validate-platform.sh
./scripts/validate-platform.sh
```

✅ **Success**: You should see "All platform validation tests passed!"

## 🏗️ Architecture Deep Dive

### Service Architecture

```
┌─────────────────┐    ┌─────────────────┐
│  Gateway Service │    │   User Service  │
│   (Port 8080)   │◄──►│   (Port 8080)   │
└─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│      Redis      │    │   PostgreSQL    │
│   (Port 6379)   │    │   (Port 5432)   │
└─────────────────┘    └─────────────────┘
```

### Three-Tier Architecture Pattern

Each service follows a strict three-tier architecture:

```
src/main/java/org/gripday/{service}/
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

**Key Rules:**

- Controllers can only depend on domain services
- Domain services can depend on repositories
- No direct controller-to-repository dependencies
- Enforced by ArchUnit tests

## 🛠️ Development Workflow

### 1. Feature Development Process

#### Create Feature Branch

```bash
git checkout -b feature/user-profile-management
```

#### Follow Naming Conventions

- **Controllers**: `{Entity}Resource.java` (e.g., `UserManagementResource.java`)
- **Services**: `{Entity}Service.java` (e.g., `UserRegistrationService.java`)
- **Repositories**: `{Entity}Repository.java` (e.g., `UserRepository.java`)
- **DTOs**: Use records for immutable data transfer

#### Example Implementation

```java
// Controller (presentation/web)
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserManagementResource {

  private final UserManagementService userManagementService;

  @PostMapping
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    var user = userManagementService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }
}

// Service (domain/service)
@Service
@Transactional
public class UserManagementService {

  private final UserRepository userRepository;

  public UserResponse createUser(CreateUserRequest request) {
    var user = User.builder().username(request.username()).email(request.email()).build();

    var savedUser = userRepository.save(user);
    return UserResponse.from(savedUser);
  }
}

// Repository (infrastructure/repository)
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  @Query(
    """
    SELECT u FROM User u
    WHERE u.tenantId = :tenantId
    AND u.enabled = true
    """
  )
  List<User> findActiveUsersByTenant(@Param("tenantId") String tenantId);
}

// DTO (using records)
public record CreateUserRequest(@NotBlank @Size(min = 3, max = 50) String username, @Email @NotBlank String email, @NotBlank String firstName, @NotBlank String lastName) {}

public record UserResponse(Long userId, String username, String email, String firstName, String lastName, String tenantId, boolean enabled, Set<String> authorities) {
  public static UserResponse from(User user) {
    return new UserResponse(
      user.getId(),
      user.getUsername(),
      user.getEmail(),
      user.getFirstName(),
      user.getLastName(),
      user.getTenantId(),
      user.isEnabled(),
      user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet())
    );
  }
}
```

### 2. Java 21 Modern Features

#### Use var for Local Variables

```java
// Good
var userOptional = userRepository.findByUsername(username);
var authorities = user.getAuthorities().stream()
    .map(Authority::getName)
    .collect(Collectors.toSet());

// Avoid
Optional<User> userOptional = userRepository.findByUsername(username);
Set<String> authorities = user.getAuthorities().stream()...
```

#### Pattern Matching and Switch Expressions

```java
// Pattern matching for instanceof
public String processAuthenticationResult(AuthenticationResult result) {
  return switch (result) {
    case AuthenticationSuccess success -> "Login successful for user: " + success.username();
    case AuthenticationFailure failure -> "Login failed: " + failure.reason();
    case AccountLocked locked -> "Account locked until: " + locked.lockedUntil();
  };
}

// Enhanced switch with pattern matching
public ResponseEntity<?> handleUserAction(UserAction action) {
  return switch (action) {
    case CreateUser(var request) -> createUser(request);
    case UpdateUser(var id, var request) -> updateUser(id, request);
    case DeleteUser(var id) -> deleteUser(id);
  };
}
```

#### Text Blocks for SQL and JSON

```java
// SQL queries
private static final String FIND_USERS_WITH_AUTHORITIES = """
  SELECT u.*, a.name as authority_name
  FROM users u
  LEFT JOIN user_authorities ua ON u.id = ua.user_id
  LEFT JOIN authorities a ON ua.authority_id = a.id
  WHERE u.tenant_id = :tenantId
  AND u.enabled = true
  ORDER BY u.created_at DESC
  """;

// JSON templates
private static final String ERROR_RESPONSE_TEMPLATE = """
  {
      "error": {
          "code": "%s",
          "message": "%s",
          "timestamp": "%s",
          "correlationId": "%s"
      }
  }
  """;
```

#### Records for Immutable Data

```java
// User context propagation
public record UserContext(Long userId, String username, String email, Set<String> authorities, String tenantId, Map<String, Object> customClaims) {
  // Compact constructor for validation
  public UserContext {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(username, "username cannot be null");
    authorities = Set.copyOf(authorities); // Defensive copy
  }

  // Convenience methods
  public boolean hasAuthority(String authority) {
    return authorities.contains(authority);
  }

  public boolean isAdmin() {
    return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
  }
}
```

### 3. Testing Guidelines

#### Happy Path Testing Focus

```java
@Test
void shouldCreateUserSuccessfully() {
  // Given
  var request = new CreateUserRequest("johndoe", "john@example.com", "John", "Doe");

  // When
  var response = userManagementService.createUser(request);

  // Then
  assertThat(response.username()).isEqualTo("johndoe");
  assertThat(response.email()).isEqualTo("john@example.com");
  assertThat(response.enabled()).isTrue();
}

@Test
void shouldAuthenticateUserSuccessfully() {
  // Given
  var loginRequest = new LoginRequest("johndoe", "SecurePass123!");

  // When
  var tokenResponse = authenticationService.authenticate(loginRequest);

  // Then
  assertThat(tokenResponse.accessToken()).isNotBlank();
  assertThat(tokenResponse.user().username()).isEqualTo("johndoe");
}
```

#### Integration Testing with Testcontainers

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthenticationIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("gripday_auth_test").withUsername("test").withPassword("test");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Test
  void shouldCompleteAuthenticationFlow() {
    // Test complete signup -> login -> profile flow
    var signupRequest = new SignupRequest("testuser", "test@example.com", "TestPass123!", "Test", "User");

    // Signup
    var signupResponse = restTemplate.postForEntity("/api/v1/auth/signup", signupRequest, UserRegistrationResponse.class);
    assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // Login
    var loginRequest = new LoginRequest("testuser", "TestPass123!");
    var loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, TokenResponse.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Profile
    var headers = new HttpHeaders();
    headers.setBearerAuth(loginResponse.getBody().accessToken());
    var profileResponse = restTemplate.exchange("/api/v1/auth/profile", HttpMethod.GET, new HttpEntity<>(headers), UserProfile.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
```

#### Architectural Testing

```java
@AnalyzeClasses(packages = "org.gripday.authservice")
class ArchitectureTest {

  @ArchTest
  static final ArchRule controllers_should_be_in_web_package = classes()
    .that()
    .areAnnotatedWith(RestController.class)
    .should()
    .resideInAPackage("..presentation.web..")
    .andShould()
    .haveSimpleNameEndingWith("Resource");

  @ArchTest
  static final ArchRule services_should_be_in_service_package = classes().that().areAnnotatedWith(Service.class).should().resideInAPackage("..domain.service..");

  @ArchTest
  static final ArchRule repositories_should_be_in_repository_package = classes().that().areAnnotatedWith(Repository.class).should().resideInAPackage("..infrastructure.repository..");

  @ArchTest
  static final ArchRule controllers_should_not_access_repositories_directly = noClasses()
    .that()
    .resideInAPackage("..presentation.web..")
    .should()
    .dependOnClassesThat()
    .resideInAPackage("..infrastructure.repository..");
}
```

## 🔧 Configuration Management

### Environment Profiles

- **local**: Development with Docker Compose
- **staging**: Pre-production testing
- **production**: Live deployment
- **test**: Automated testing

### YAML Configuration Convention

All configuration uses YAML format with `gripday.` prefix:

```yaml
# application-local.yml
gripday:
  auth:
    jwt:
      secret: ${JWT_SECRET:local-development-secret-key}
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
    rate-limiting:
      login-attempts-per-minute: 5
      signup-attempts-per-minute: 3
  database:
    url: jdbc:postgresql://localhost:5432/gripday_auth
    username: ${DB_USERNAME:gripday}
    password: ${DB_PASSWORD:gripday}
  cache:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: PT2S
  observability:
    tracing:
      enabled: true
      sample-rate: 1.0
    metrics:
      enabled: true
      export-interval: PT30S

server:
  port: 8080

logging:
  level:
    org.gripday: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Configuration Properties Classes

```java
@ConfigurationProperties(prefix = "gripday.auth")
@Validated
public record AuthConfigurationProperties(@Valid JwtProperties jwt, @Valid RateLimitingProperties rateLimiting) {
  public record JwtProperties(@NotBlank String secret, @NotNull Duration accessTokenExpiry, @NotNull Duration refreshTokenExpiry) {}

  public record RateLimitingProperties(@Min(1) int loginAttemptsPerMinute, @Min(1) int signupAttemptsPerMinute) {}
}
```

## 🔐 Security Best Practices

### JWT Token Handling

```java
// Generate JWT with user context
public String generateAccessToken(UserContext userContext) {
  var now = Instant.now();
  var expiry = now.plus(jwtProperties.accessTokenExpiry());

  return Jwts.builder()
    .subject(userContext.username())
    .claim("userId", userContext.userId())
    .claim("email", userContext.email())
    .claim("tenantId", userContext.tenantId())
    .claim("authorities", userContext.authorities())
    .issuedAt(Date.from(now))
    .expiration(Date.from(expiry))
    .signWith(getSigningKey())
    .compact();
}

// Validate and extract user context
public UserContext extractUserContext(String token) {
  var claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

  return new UserContext(
    claims.get("userId", Long.class),
    claims.getSubject(),
    claims.get("email", String.class),
    Set.copyOf(claims.get("authorities", List.class)),
    claims.get("tenantId", String.class),
    Map.of()
  );
}
```

### Input Validation

```java
// Request validation with Bean Validation
public record CreateUserRequest(
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
  String username,

  @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

  @NotBlank(message = "Password is required") @ValidPassword String password
) {}

// Custom validation annotation
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
  String message() default "Password must contain at least 8 characters with uppercase, lowercase, number, and special character";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
```

### Multi-Tenant Security

```java
// Tenant context filter
@Component
public class TenantContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    var httpRequest = (HttpServletRequest) request;
    var tenantId = extractTenantId(httpRequest);

    try {
      TenantContext.setCurrentTenant(tenantId);
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private String extractTenantId(HttpServletRequest request) {
    // Try header first
    var tenantId = request.getHeader("X-Tenant-ID");
    if (tenantId != null) {
      return tenantId;
    }

    // Try JWT token
    var authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      var token = authHeader.substring(7);
      var userContext = jwtService.extractUserContext(token);
      return userContext.tenantId();
    }

    return "default";
  }
}
```

## 📊 Observability and Monitoring

### Structured Logging

```java
// Use SLF4J with structured logging
@Slf4j
@Service
public class AuthenticationService {

  public TokenResponse authenticate(LoginRequest request) {
    var correlationId = MDC.get("correlationId");

    log.info("Authentication attempt for user: {} [correlationId={}]", request.username(), correlationId);

    try {
      var user = userRepository.findByUsername(request.username()).orElseThrow(() -> new InvalidCredentialsException("User not found"));

      if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        log.warn("Authentication failed for user: {} - invalid password [correlationId={}]", request.username(), correlationId);
        throw new InvalidCredentialsException("Invalid password");
      }

      var tokenResponse = generateTokens(user);

      log.info("Authentication successful for user: {} [correlationId={}]", request.username(), correlationId);

      return tokenResponse;
    } catch (Exception e) {
      log.error("Authentication error for user: {} [correlationId={}]", request.username(), correlationId, e);
      throw e;
    }
  }
}
```

### Custom Metrics

```java
// Custom metrics with Micrometer
@Component
public class AuthenticationMetrics {

  private final Counter loginAttempts;
  private final Counter loginSuccesses;
  private final Counter loginFailures;
  private final Timer loginDuration;

  public AuthenticationMetrics(MeterRegistry meterRegistry) {
    this.loginAttempts = Counter.builder("auth.login.attempts").description("Total login attempts").register(meterRegistry);

    this.loginSuccesses = Counter.builder("auth.login.successes").description("Successful login attempts").register(meterRegistry);

    this.loginFailures = Counter.builder("auth.login.failures").description("Failed login attempts").register(meterRegistry);

    this.loginDuration = Timer.builder("auth.login.duration").description("Login processing time").register(meterRegistry);
  }

  public void recordLoginAttempt() {
    loginAttempts.increment();
  }

  public void recordLoginSuccess() {
    loginSuccesses.increment();
  }

  public void recordLoginFailure() {
    loginFailures.increment();
  }

  public Timer.Sample startLoginTimer() {
    return Timer.start();
  }

  public void recordLoginDuration(Timer.Sample sample) {
    sample.stop(loginDuration);
  }
}
```

## 🚀 Deployment

### Local Development

```bash
# Start infrastructure
docker compose up -d postgres redis

# Run services in development mode
cd gripday-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd ../gripday-gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker Compose

```bash
# Build and start all services
docker compose up --build

# Scale services
docker compose up --scale gateway-service=2

# View logs
docker-compose logs -f user-service
```

### Kubernetes

```bash
# Deploy to local cluster
./k8s/deploy-local.sh

# Deploy to staging
./k8s/deploy-staging.sh

# Check deployment status
kubectl get pods -n gripday
kubectl logs -f deployment/user-service -n gripday
```

## 🔍 Debugging and Troubleshooting

### Common Issues and Solutions

#### Service Won't Start

```bash
# Check port availability
lsof -i :8080
netstat -tulpn | grep 8080

# Check database connectivity
docker-compose exec postgres psql -U gripday -d gripday_auth -c "SELECT 1;"

# View detailed logs
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dlogging.level.org.gripday=DEBUG
```

#### Authentication Issues

```bash
# Verify JWT token
echo "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." | cut -d. -f2 | base64 -d | jq

# Check token validation
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/auth/validate

# Test authentication flow
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "TestPass123!"}'
```

#### Database Issues

```bash
# Check migrations
cd gripday-user-service
mvn liquibase:status -Dspring.profiles.active=local

# Run migrations manually
mvn liquibase:update -Dspring.profiles.active=local

# Reset database (development only)
mvn liquibase:dropAll -Dspring.profiles.active=local
mvn liquibase:update -Dspring.profiles.active=local
```

### Debugging Tools

#### Application Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Configuration properties
curl http://localhost:8080/actuator/configprops

# Environment variables
curl http://localhost:8080/actuator/env

# Metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/auth.login.attempts
```

#### Database Queries

```sql
-- Check user data
SELECT username, email, enabled, tenant_id, created_at FROM users ORDER BY created_at DESC LIMIT 10;

-- Check authorities
SELECT u.username, a.name as authority
FROM users u
JOIN user_authorities ua ON u.id = ua.user_id
JOIN authorities a ON ua.authority_id = a.id;

-- Check tenant isolation
SELECT tenant_id, COUNT(*) as user_count FROM users GROUP BY tenant_id;
```

## 📚 Additional Resources

### Documentation

- [Complete API Reference](docs/api/complete-api-reference.md)
- [Troubleshooting Guide](docs/troubleshooting/common-issues.md)
- [User Service README](gripday-user-service/README.md)
- [Gateway Service README](gripday-gateway-service/README.md)

### Interactive Tools

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Endpoints**: http://localhost:8080/actuator
- **Prometheus Metrics**: http://localhost:9090
- **Grafana Dashboards**: http://localhost:3000

### Development Tools

- **Validation Scripts**: `./scripts/validate-platform.sh`
- **Docker Compose**: `docker compose up -d`
- **Kubernetes**: `./k8s/deploy-local.sh`

### Community

- **GitHub Issues**: Report bugs and request features
- **GitHub Discussions**: Ask questions and share knowledge
- **Code Reviews**: Follow pull request guidelines
- **Contributing**: See CONTRIBUTING.md for guidelines

## 🎉 Next Steps

1. **Complete the Quick Setup** above
2. **Explore the API** using Swagger UI
3. **Run the validation scripts** to ensure everything works
4. **Create your first feature** following the development workflow
5. **Join the community** and contribute back!

Welcome to the Gripday platform development team! 🚀
