# Gripday Platform - Design Document

> **Version:** 1.0.0  
> **Last Updated:** November 25, 2025  
> **Status:** Ready for Kiro AI Development

## 📋 Document Purpose

This design document provides architectural decisions, design patterns, technical implementation details, and rationale for the Gripday platform. It serves as a reference for developers and AI assistants (like Kiro) to understand the "why" behind implementation choices.

---

## 🎯 Design Goals & Principles

### Primary Goals

1. **Scalability** - Support horizontal scaling for millions of users
2. **Security** - Enterprise-grade security with multi-tenant isolation
3. **Maintainability** - Clean architecture with clear boundaries
4. **Performance** - Sub-second response times for 95% of requests
5. **Observability** - Complete visibility into system behavior
6. **Developer Experience** - Easy to understand, extend, and test

### Design Principles

**Backend Principles:**

- Microservices with clear bounded contexts
- Database per service for data isolation
- Stateless services for horizontal scaling
- API-first design with OpenAPI documentation
- Fail-fast with circuit breakers
- Eventual consistency where appropriate

**Frontend Principles:**

- Feature-Sliced Design for scalability
- Type-safety with TypeScript strict mode
- Component composition over inheritance
- Declarative over imperative code
- Accessibility-first development
- Performance by default

---

## 🏗️ Architectural Decisions

### ADR-001: Microservices Architecture

**Status:** Accepted  
**Date:** 2024-01-15

**Context:**
Need to build a scalable platform that can grow independently in different business domains (authentication, catalog, inventory, etc.).

**Decision:**
Adopt microservices architecture with:

- Service decomposition by business capability
- Database per service pattern
- API Gateway as single entry point
- Event-driven communication (future)

**Rationale:**

- **Independent Scaling:** Each service scales based on its load
- **Technology Flexibility:** Can use different tech stacks per service
- **Team Autonomy:** Teams can work independently
- **Fault Isolation:** Failure in one service doesn't cascade
- **Deployment Independence:** Deploy services separately

**Consequences:**

- ✅ Better scalability and resilience
- ✅ Faster development cycles
- ❌ Increased operational complexity
- ❌ Distributed system challenges (consistency, tracing)
- ❌ More infrastructure overhead

**Alternatives Considered:**

- Monolithic architecture (rejected: doesn't scale well)
- Modular monolith (considered for future simplification)

### ADR-002: JWT-Based Authentication with RSA256

**Status:** Accepted  
**Date:** 2024-01-15

**Context:**
Need stateless authentication that works across multiple services without session storage.

**Decision:**
Use JWT tokens with RSA256 asymmetric signing:

- User Service generates tokens with private key
- Other services validate with public key (JWK Set endpoint)
- Access tokens: 15 minutes
- Refresh tokens: 7 days
- Token blacklisting with Redis for logout

**Rationale:**

- **Stateless:** No session storage needed
- **Scalable:** Services validate independently
- **Secure:** RSA256 prevents token forgery
- **Standard:** Industry-standard approach
- **Distributed:** Works across service boundaries

**Consequences:**

- ✅ Horizontal scaling without session replication
- ✅ No database lookup for every request
- ✅ Services can validate tokens independently
- ❌ Cannot revoke tokens immediately (use blacklist)
- ❌ Token size larger than session IDs
- ❌ Key management complexity

**Implementation Details:**

```java
// Token generation (User Service)
JwtClaimsSet claims = JwtClaimsSet.builder()
    .subject(userId.toString())
    .claim("username", user.getUsername())
    .claim("email", user.getEmail())
    .claim("roles", user.getRoles())
    .claim("tenantId", user.getTenantId())
    .claim("type", "access")
    .issuer("gripday-user-service")
    .issuedAt(Instant.now())
    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
    .id(UUID.randomUUID().toString()) // JTI for blacklisting
    .build();

return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
```

**Alternatives Considered:**

- HS256 (rejected: shared secret across services)
- OAuth2 with external provider (future enhancement)
- Session-based auth (rejected: doesn't scale)

---

### ADR-003: Schema-Per-Tenant Multi-Tenancy

**Status:** Accepted  
**Date:** 2024-01-20

**Context:**
Need strong data isolation for multi-tenant SaaS application with compliance requirements.

**Decision:**
Implement schema-per-tenant strategy:

- Each tenant gets dedicated PostgreSQL schema
- Hibernate MultiTenantConnectionProvider for schema switching
- TenantContext for thread-local tenant management
- Tenant ID in JWT claims and headers

**Rationale:**

- **Strong Isolation:** Complete data separation
- **Security:** No risk of cross-tenant queries
- **Compliance:** Meets regulatory requirements
- **Performance:** Better than row-level security
- **Backup/Restore:** Per-tenant operations possible

**Consequences:**

- ✅ Strongest data isolation
- ✅ No tenant_id in every query
- ✅ Easier compliance audits
- ❌ More complex migrations (per-tenant)
- ❌ Cannot do cross-tenant queries easily
- ❌ More database connections

**Implementation Details:**

```java
// TenantContext for thread-local management
public class TenantContext {

  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

  public static void setTenantId(String tenantId) {
    currentTenant.set(tenantId);
  }

  public static String getTenantId() {
    return currentTenant.get();
  }

  public static <T> T executeInTenantContext(String tenantId, Supplier<T> operation) {
    String previousTenant = getTenantId();
    try {
      setTenantId(tenantId);
      return operation.get();
    } finally {
      setTenantId(previousTenant);
    }
  }
}

// Hibernate connection provider
@Override
public Connection getConnection(String tenantIdentifier) {
  Connection connection = dataSource.getConnection();
  connection.createStatement().execute("SET search_path = " + tenantIdentifier);
  return connection;
}
```

**Alternatives Considered:**

- Row-level security with tenant_id column (rejected: performance, complexity)
- Database-per-tenant (rejected: too many databases)
- Shared schema (rejected: weak isolation)

### ADR-004: Reactive API Gateway with Spring Cloud Gateway

**Status:** Accepted  
**Date:** 2024-01-22

**Context:**
Need high-throughput API gateway that can handle thousands of concurrent requests without blocking.

**Decision:**
Use Spring Cloud Gateway with reactive WebFlux:

- Non-blocking I/O with Project Reactor
- Reactive filter chains
- Reactive Redis for rate limiting
- Circuit breaker with Resilience4j

**Rationale:**

- **Performance:** Non-blocking handles more concurrent requests
- **Efficiency:** Better resource utilization
- **Backpressure:** Built-in flow control
- **Modern:** Aligns with reactive programming trends
- **Integration:** Works well with Spring ecosystem

**Consequences:**

- ✅ Higher throughput with fewer threads
- ✅ Better resource utilization
- ✅ Handles traffic spikes better
- ❌ Steeper learning curve (reactive programming)
- ❌ Debugging more complex
- ❌ Must use reactive libraries (Redis, etc.)

**Implementation Details:**

```java
// Reactive filter chain
@Component
@Order(1)
public class TenantRateLimitingFilter implements GlobalFilter {

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String tenantId = extractTenantId(exchange);
    String key = "rate_limit:" + tenantId;

    return redisTemplate
      .opsForZSet()
      .add(key, UUID.randomUUID().toString(), System.currentTimeMillis())
      .flatMap((added) -> redisTemplate.opsForZSet().count(key, System.currentTimeMillis() - 60000, System.currentTimeMillis()))
      .flatMap((count) -> {
        if (count > getQuota(tenantId)) {
          return rateLimitExceeded(exchange);
        }
        return chain.filter(exchange);
      });
  }
}
```

**Alternatives Considered:**

- Zuul (rejected: older, blocking)
- Nginx (rejected: less Spring integration)
- Custom gateway (rejected: reinventing wheel)

---

### ADR-005: Feature-Sliced Design for Frontend

**Status:** Accepted  
**Date:** 2024-01-25

**Context:**
Need scalable frontend architecture that prevents spaghetti code as application grows.

**Decision:**
Adopt Feature-Sliced Design (FSD) methodology:

- Strict layer hierarchy (app → processes → pages → widgets → features → entities → shared)
- Public API pattern (index.ts exports)
- Cross-feature isolation
- Automated architecture tests

**Rationale:**

- **Scalability:** Grows linearly with features
- **Maintainability:** Clear boundaries and dependencies
- **Team Collaboration:** Multiple teams can work independently
- **Onboarding:** New developers understand structure quickly
- **Refactoring:** Easy to move/rename features

**Consequences:**

- ✅ Prevents circular dependencies
- ✅ Clear import rules
- ✅ Easy to find code
- ✅ Testable architecture
- ❌ Initial learning curve
- ❌ More boilerplate (index.ts files)
- ❌ Strict rules can feel restrictive

**Implementation Details:**

```typescript
// Layer structure
src/
├── app/          // Can import from all layers
├── processes/    // Can import: features, entities, shared
├── pages/        // Can import: widgets, features, entities, shared
├── widgets/      // Can import: features, entities, shared
├── features/     // Can import: entities, shared (NOT other features)
├── entities/     // Can import: shared
└── shared/       // Cannot import from upper layers

// Public API pattern
// features/user-form/index.ts
export { UserFormFeature } from './ui/user-form-feature';
export type { FormValues } from './model/types';

// Usage (correct)
import { UserFormFeature } from '@/features/user-form';

// Usage (incorrect - will fail architecture tests)
import { UserFormFeature } from '@/features/user-form/ui/user-form-feature';
```

**Alternatives Considered:**

- Atomic Design (rejected: not suitable for business logic)
- Feature folders (rejected: no strict rules)
- Domain-Driven Design frontend (rejected: too complex)

### ADR-006: RFC 9457 Problem Details for Error Handling

**Status:** Accepted  
**Date:** 2024-01-28

**Context:**
Need standardized error format that works across all services and provides actionable information to clients.

**Decision:**
Implement RFC 9457 Problem Details for HTTP APIs:

- Standardized error response format
- Field-level validation errors
- Correlation IDs for tracing
- Retryable error indication
- Frontend automatic field mapping

**Rationale:**

- **Standardization:** Industry standard (RFC 9457)
- **Consistency:** Same format across all services
- **Actionable:** Clients know how to handle errors
- **Debugging:** Correlation IDs for tracing
- **User Experience:** Field-level errors for forms

**Consequences:**

- ✅ Consistent error handling
- ✅ Better debugging with correlation IDs
- ✅ Automatic form field error mapping
- ✅ Clear error types for client logic
- ❌ More complex error handling code
- ❌ Larger response payloads

**Implementation Details:**

```java
// Backend error response
@Data
@Builder
public class ProblemDetail {
    private String type;           // "/problems/validation-error"
    private String title;          // "Validation Failed"
    private int status;            // 400
    private String detail;         // "One or more fields are invalid"
    private String instance;       // "/api/v1/users"
    private String code;           // "VALIDATION_ERROR"
    private String correlationId;  // "abc-123"
    private String requestId;      // "req-456"
    private Instant timestamp;
    private List<FieldError> fields;
}

@Data
public class FieldError {
    private String field;          // "email"
    private String message;        // "Email is already registered"
    private Object rejectedValue;  // "user@example.com"
}

// Frontend automatic mapping
const mutation = useFormMutation(form, apiCall, {
  // Automatically maps field errors to form
  // form.errors = { email: 'Email is already registered' }
});
```

**Alternatives Considered:**

- Custom error format (rejected: not standard)
- Simple error messages (rejected: not actionable)
- GraphQL errors (rejected: REST API)

---

### ADR-007: Redis for Distributed Caching and Rate Limiting

**Status:** Accepted  
**Date:** 2024-02-01

**Context:**
Need distributed state management for caching, rate limiting, and token blacklisting across multiple service instances.

**Decision:**
Use Redis for:

- Distributed caching (cache-aside pattern)
- Rate limiting (sliding window with ZSET)
- Token blacklisting (with TTL)
- Session management (future)

**Rationale:**

- **Performance:** In-memory, sub-millisecond latency
- **Distributed:** Shared state across instances
- **Data Structures:** ZSET perfect for sliding window
- **TTL:** Automatic expiration for tokens
- **Proven:** Battle-tested in production

**Consequences:**

- ✅ Fast distributed state
- ✅ Horizontal scaling support
- ✅ Automatic cleanup with TTL
- ✅ Rich data structures
- ❌ Additional infrastructure dependency
- ❌ Memory constraints
- ❌ Persistence considerations

**Implementation Details:**

```java
// Rate limiting with sliding window
public boolean isRateLimitExceeded(String tenantId, int quota) {
  String key = "rate_limit:" + tenantId;
  long now = System.currentTimeMillis();
  long windowStart = now - 60000; // 1 minute window

  // Add current request
  redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

  // Remove expired entries
  redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

  // Count requests in window
  Long count = redisTemplate.opsForZSet().count(key, windowStart, now);

  // Set expiration
  redisTemplate.expire(key, Duration.ofMinutes(2));

  return count > quota;
}

// Token blacklisting
public void blacklistToken(String jti, Duration ttl) {
  String key = "blacklist:" + jti;
  redisTemplate.opsForValue().set(key, "true", ttl);
}

public boolean isTokenBlacklisted(String jti) {
  return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
}
```

**Alternatives Considered:**

- Hazelcast (rejected: more complex)
- Memcached (rejected: limited data structures)
- Database (rejected: too slow)

### ADR-008: Domain-Driven Design for Bookstore Service

**Status:** Accepted  
**Date:** 2024-02-05

**Context:**
Need rich domain model for catalog and inventory management that encapsulates business logic.

**Decision:**
Implement tactical DDD patterns:

- Value Objects (ISBN, Money, BookId)
- Aggregate Roots (Book, Inventory, Category)
- Domain Services (DuplicateIsbnChecker)
- Factory Methods (Book.create())
- Repository pattern for persistence

**Rationale:**

- **Business Logic in Domain:** Not in services
- **Type Safety:** Value objects prevent primitive obsession
- **Invariants:** Enforced by aggregates
- **Testability:** Domain logic testable without infrastructure
- **Maintainability:** Clear business rules

**Consequences:**

- ✅ Rich domain model
- ✅ Business logic in one place
- ✅ Easy to test
- ✅ Clear boundaries
- ❌ More classes/files
- ❌ Learning curve for team
- ❌ Can be over-engineered for simple domains

**Implementation Details:**

```java
// Value Object - ISBN
public record ISBN(String value) {
  public ISBN {
    if (!isValid(value)) {
      throw new IllegalArgumentException("Invalid ISBN: " + value);
    }
  }

  private static boolean isValid(String isbn) {
    String cleaned = isbn.replaceAll("[^0-9X]", "");
    return cleaned.length() == 10 || cleaned.length() == 13;
  }

  public String formatted() {
    return value.length() == 13 ? value.replaceAll("(\\d{3})(\\d{1})(\\d{3})(\\d{5})(\\d{1})", "$1-$2-$3-$4-$5") : value.replaceAll("(\\d{1})(\\d{3})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
  }
}

// Aggregate Root - Book
@Entity
public class Book extends AggregateRoot {

  @EmbeddedId
  private BookId id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Embedded
  private ISBN isbn;

  @Embedded
  private Money price;

  @ManyToOne
  private Category category;

  private boolean available;

  // Factory method
  public static Book create(String title, String author, ISBN isbn, Money price, Category category) {
    Book book = new Book();
    book.id = BookId.generate();
    book.title = requireNonNull(title, "Title is required");
    book.author = requireNonNull(author, "Author is required");
    book.isbn = requireNonNull(isbn, "ISBN is required");
    book.price = requireNonNull(price, "Price is required");
    book.category = requireNonNull(category, "Category is required");
    book.available = true;
    book.createdAt = Instant.now();
    return book;
  }

  // Business methods
  public boolean canBeSold() {
    return available && hasStock();
  }

  public void markAsAvailable() {
    this.available = true;
    this.updatedAt = Instant.now();
  }

  public void markAsUnavailable() {
    this.available = false;
    this.updatedAt = Instant.now();
  }

  public boolean isInPriceRange(Money min, Money max) {
    return price.isGreaterThanOrEqual(min) && price.isLessThanOrEqual(max);
  }
}

// Domain Service
@Service
public class DuplicateIsbnChecker {

  private final BookRepository bookRepository;

  public void checkDuplicate(ISBN isbn) {
    if (bookRepository.existsByIsbn(isbn)) {
      throw new DuplicateIsbnException(isbn);
    }
  }
}
```

**Alternatives Considered:**

- Anemic domain model (rejected: business logic scattered)
- Transaction script (rejected: doesn't scale)
- CRUD-based (rejected: no business logic encapsulation)

### ADR-009: OpenTelemetry for Distributed Tracing

**Status:** Accepted  
**Date:** 2024-02-10

**Context:**
Need to trace requests across multiple services for debugging and performance analysis.

**Decision:**
Implement OpenTelemetry with:

- Correlation ID generation at gateway
- Automatic span creation for HTTP requests
- Manual spans for critical operations
- Propagation via headers
- Export to Prometheus/Grafana

**Rationale:**

- **Vendor Neutral:** Not locked to specific vendor
- **Standard:** Industry standard for observability
- **Automatic:** Spring Boot auto-configuration
- **Complete:** Traces, metrics, logs in one
- **Ecosystem:** Wide tool support

**Consequences:**

- ✅ End-to-end request tracing
- ✅ Performance bottleneck identification
- ✅ Vendor-neutral approach
- ✅ Rich ecosystem
- ❌ Performance overhead (minimal)
- ❌ Storage requirements for traces
- ❌ Learning curve for team

**Implementation Details:**

```java
// Automatic span creation (Spring Boot)
@Configuration
public class ObservabilityConfig {

  @Bean
  public ObservationRegistry observationRegistry() {
    return ObservationRegistry.create();
  }
}

// Manual span creation for critical operations
@Service
public class UserService {

  private final Tracer tracer;

  public User createUser(CreateUserRequest request) {
    Span span = tracer.spanBuilder("createUser").setAttribute("username", request.username()).setAttribute("tenantId", TenantContext.getTenantId()).startSpan();

    try (Scope scope = span.makeCurrent()) {
      // Business logic
      User user = userRepository.save(newUser);
      span.setAttribute("userId", user.getId().toString());
      span.setStatus(StatusCode.OK);
      return user;
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }
}

// Correlation ID propagation
@Component
@Order(1)
public class CorrelationIdFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");

    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }

    ServerHttpRequest request = exchange.getRequest().mutate().header("X-Correlation-ID", correlationId).build();

    return chain.filter(exchange.mutate().request(request).build());
  }
}
```

**Alternatives Considered:**

- Zipkin (rejected: less feature-rich)
- Jaeger (rejected: OpenTelemetry is successor)
- Custom tracing (rejected: reinventing wheel)

---

### ADR-010: TanStack Query for Server State Management

**Status:** Accepted  
**Date:** 2024-02-12

**Context:**
Need efficient server state management with caching, automatic refetching, and optimistic updates.

**Decision:**
Use TanStack Query v5 for all server state:

- Automatic caching with stale-while-revalidate
- Query invalidation on mutations
- Optimistic updates for better UX
- Retry logic with exponential backoff
- Integration with useFormMutation hook

**Rationale:**

- **Caching:** Reduces unnecessary API calls
- **Automatic Refetching:** Keeps data fresh
- **Optimistic Updates:** Better perceived performance
- **DevTools:** Excellent debugging experience
- **Type-Safe:** Full TypeScript support

**Consequences:**

- ✅ Reduced API calls
- ✅ Better user experience
- ✅ Automatic loading/error states
- ✅ Cache invalidation handled
- ❌ Learning curve for team
- ❌ More complex state management
- ❌ Cache can get stale if not managed

**Implementation Details:**

```typescript
// Query client configuration
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minute
      refetchOnWindowFocus: false,
      retry: (failureCount, error: any) => {
        // Don't retry on 4xx errors except 408, 429
        if (error?.response?.status >= 400 && error?.response?.status < 500) {
          return error?.response?.status === 408 || error?.response?.status === 429;
        }
        return failureCount < 3;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

// Query keys pattern
export const usersKeys = {
  all: ["users"] as const,
  lists: () => [...usersKeys.all, "list"] as const,
  list: (params: UsersQueryParams) => [...usersKeys.lists(), params] as const,
  details: () => [...usersKeys.all, "detail"] as const,
  detail: (id: string) => [...usersKeys.details(), id] as const,
};

// Query hook
export function useUsersQuery(params: UsersQueryParams) {
  return useQuery({
    queryKey: usersKeys.list(params),
    queryFn: () => api.get("/api/v1/admin/users", { params }).then((r) => r.data),
  });
}

// Mutation with cache invalidation
export function useCreateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateUserRequest) => api.post("/api/v1/admin/users", data).then((r) => r.data),
    onSuccess: () => {
      // Invalidate all user lists
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() });
    },
  });
}

// Optimistic update
export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) => api.put(`/api/v1/admin/users/${id}`, data).then((r) => r.data),
    onMutate: async ({ id, data }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: usersKeys.detail(id) });

      // Snapshot previous value
      const previous = queryClient.getQueryData(usersKeys.detail(id));

      // Optimistically update
      queryClient.setQueryData(usersKeys.detail(id), (old: any) => ({
        ...old,
        ...data,
      }));

      return { previous };
    },
    onError: (err, variables, context) => {
      // Rollback on error
      if (context?.previous) {
        queryClient.setQueryData(usersKeys.detail(variables.id), context.previous);
      }
    },
    onSettled: (data, error, variables) => {
      // Refetch after mutation
      queryClient.invalidateQueries({ queryKey: usersKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() });
    },
  });
}
```

**Alternatives Considered:**

- Redux Toolkit Query (rejected: more boilerplate)
- SWR (rejected: less feature-rich)
- Apollo Client (rejected: GraphQL-focused)

---

## 🎨 Design Patterns

### Backend Patterns

#### 1. Repository Pattern

**Purpose:** Abstract data access logic from business logic

**Implementation:**

```java
// Repository interface
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
  Optional<User> findByEmail(String email);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.enabled = true")
  List<User> findAllEnabled();
}

// Service uses repository
@Service
@Transactional
public class UserService {

  private final UserRepository userRepository;

  public User findByUsername(String username) {
    return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
  }
}
```

**Benefits:**

- Separation of concerns
- Easy to test (mock repository)
- Database-agnostic business logic
- Centralized query logic

---

#### 2. DTO Pattern with Java Records

**Purpose:** Transfer data between layers without exposing domain entities

**Implementation:**

```java
// Request DTO
public record CreateUserRequest(@NotBlank String username, @Email String email, @ValidPassword String password, String firstName, String lastName, Set<String> roles) {}

// Response DTO
public record UserResponse(Long id, String username, String email, String firstName, String lastName, Set<String> roles, boolean enabled, Instant createdAt) {}

// Mapper
public class UserMapper {

  public static UserResponse toResponse(User user) {
    return new UserResponse(
      user.getId(),
      user.getUsername(),
      user.getEmail(),
      user.getFirstName(),
      user.getLastName(),
      user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
      user.isEnabled(),
      user.getCreatedAt()
    );
  }
}
```

**Benefits:**

- Immutable data transfer
- API contract stability
- No accidental entity exposure
- Clean separation of concerns

---

#### 3. Factory Method Pattern

**Purpose:** Encapsulate object creation logic with validation

**Implementation:**

```java
@Entity
public class Book {

  // Factory method
  public static Book create(String title, String author, ISBN isbn, Money price, Category category) {
    // Validation
    requireNonNull(title, "Title is required");
    requireNonNull(author, "Author is required");
    requireNonNull(isbn, "ISBN is required");
    requireNonNull(price, "Price is required");
    requireNonNull(category, "Category is required");

    if (title.isBlank()) {
      throw new IllegalArgumentException("Title cannot be blank");
    }

    // Creation
    Book book = new Book();
    book.id = BookId.generate();
    book.title = title;
    book.author = author;
    book.isbn = isbn;
    book.price = price;
    book.category = category;
    book.available = true;
    book.createdAt = Instant.now();

    return book;
  }

  // Private constructor
  private Book() {}
}
```

**Benefits:**

- Centralized validation
- Consistent object creation
- Enforces invariants
- Clear creation intent

---

#### 4. Circuit Breaker Pattern

**Purpose:** Prevent cascading failures in distributed systems

**Implementation:**

```java
// Configuration
@Configuration
public class CircuitBreakerConfig {

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
      .failureRateThreshold(50)
      .waitDurationInOpenState(Duration.ofSeconds(60))
      .slidingWindowSize(10)
      .minimumNumberOfCalls(5)
      .build();

    return CircuitBreakerRegistry.of(config);
  }
}

// Usage in Gateway
@Component
public class CircuitBreakerFilter implements GlobalFilter {

  private final CircuitBreakerRegistry registry;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String serviceName = extractServiceName(exchange);
    CircuitBreaker circuitBreaker = registry.circuitBreaker(serviceName);

    return Mono.from(circuitBreaker.executeSupplier(() -> chain.filter(exchange))).onErrorResume(CallNotPermittedException.class, (e) -> {
      exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
      return exchange.getResponse().setComplete();
    });
  }
}
```

**Benefits:**

- Prevents cascading failures
- Fast failure detection
- Automatic recovery
- System stability

---

#### 5. Sliding Window Rate Limiting

**Purpose:** Smooth rate limiting without burst issues

**Implementation:**

```java
@Service
public class RateLimitingService {

  private final RedisTemplate<String, String> redisTemplate;

  public boolean isRateLimitExceeded(String identifier, int quota, Duration window) {
    String key = "rate_limit:" + identifier;
    long now = System.currentTimeMillis();
    long windowStart = now - window.toMillis();

    // Add current request with timestamp as score
    redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

    // Remove entries outside window
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

    // Count requests in window
    Long count = redisTemplate.opsForZSet().count(key, windowStart, now);

    // Set expiration (2x window for safety)
    redisTemplate.expire(key, window.multipliedBy(2));

    return count > quota;
  }
}
```

**Benefits:**

- Smooth rate limiting
- No burst issues
- Accurate counting
- Automatic cleanup

### Frontend Patterns

#### 1. Custom Hook Pattern (useFormMutation)

**Purpose:** Encapsulate form submission logic with error handling

**Implementation:**

```typescript
// Hook implementation
export function useFormMutation<TData, TVariables>(form: UseFormReturnType<any>, mutationFn: (variables: TVariables) => Promise<TData>, options?: FormMutationOptions<TData, TVariables>) {
  const { notifySuccess, notifyError, clearOnSuccess, focusErrorField } = options ?? {};

  return useMutation({
    mutationFn,
    onSuccess: (data, variables, context) => {
      // Clear form errors
      form.setErrors({});

      // Clear form if requested
      if (clearOnSuccess) {
        form.reset();
      }

      // Show success notification
      if (notifySuccess) {
        notificationService.success({
          title: notifySuccess.title,
          message: notifySuccess.message,
        });
      }

      options?.onSuccess?.(data, variables, context);
    },
    onError: (error, variables, context) => {
      const normalized = normalizeAxiosError(error);

      // Map field errors to form
      const fieldErrors = toMantineErrors(normalized);
      if (Object.keys(fieldErrors).length) {
        form.setErrors(fieldErrors);

        // Focus first error field
        if (focusErrorField) {
          const firstErrorField = Object.keys(fieldErrors)[0];
          document.querySelector(`[name="${firstErrorField}"]`)?.focus();
        }
      }

      // Show error notification
      if (notifyError !== false) {
        notificationService.errorFromAxios(error, {
          title: notifyError?.title,
          fallback: notifyError?.fallback,
        });
      }

      options?.onError?.(normalized, variables, context);
    },
  });
}

// Usage
const mutation = useFormMutation(form, async (values) => api.post("/api/v1/users", values), {
  notifySuccess: { title: t`Success`, message: t`User created` },
  notifyError: { title: t`Error`, fallback: t`Failed to create user` },
  clearOnSuccess: true,
  focusErrorField: true,
});
```

**Benefits:**

- Consistent form handling
- Automatic error mapping
- Reduced boilerplate
- Better UX (notifications, focus)

---

#### 2. Public API Pattern (FSD)

**Purpose:** Enforce encapsulation and prevent internal imports

**Implementation:**

```typescript
// features/user-form/index.ts (Public API)
export { UserFormFeature } from "./ui/user-form-feature";
export { useUserForm } from "./model/use-user-form";
export type { FormValues, UserFormProps } from "./model/types";

// Internal files (not exported)
// features/user-form/ui/user-form-feature.tsx
// features/user-form/model/validation.ts
// features/user-form/model/types.ts

// Correct usage
import { UserFormFeature } from "@/features/user-form";

// Incorrect usage (will fail architecture tests)
import { UserFormFeature } from "@/features/user-form/ui/user-form-feature";
```

**Benefits:**

- Clear API boundaries
- Prevents tight coupling
- Easy refactoring
- Enforced by tests

---

#### 3. Compound Component Pattern

**Purpose:** Create flexible, composable components

**Implementation:**

```typescript
// Compound component
interface StatisticsCardProps {
  children: React.ReactNode;
}

interface StatisticsCardTitleProps {
  children: React.ReactNode;
  icon?: React.ReactNode;
}

interface StatisticsCardValueProps {
  value: string | number;
  trend?: number;
}

export function StatisticsCard({ children }: StatisticsCardProps) {
  return <Card>{children}</Card>;
}

StatisticsCard.Title = function StatisticsCardTitle({
  children,
  icon
}: StatisticsCardTitleProps) {
  return (
    <Group>
      {icon}
      <Text size="sm" c="dimmed">{children}</Text>
    </Group>
  );
};

StatisticsCard.Value = function StatisticsCardValue({
  value,
  trend
}: StatisticsCardValueProps) {
  return (
    <Group>
      <Text size="xl" fw={700}>{value}</Text>
      {trend && (
        <Badge color={trend > 0 ? 'green' : 'red'}>
          {trend > 0 ? '+' : ''}{trend}%
        </Badge>
      )}
    </Group>
  );
};

// Usage
<StatisticsCard>
  <StatisticsCard.Title icon={<IconUsers />}>
    Total Users
  </StatisticsCard.Title>
  <StatisticsCard.Value value={1234} trend={12} />
</StatisticsCard>
```

**Benefits:**

- Flexible composition
- Clear component relationships
- Reduced prop drilling
- Better readability

---

#### 4. Error Boundary Pattern

**Purpose:** Catch and handle React errors gracefully

**Implementation:**

```typescript
// Error boundary component
export class ErrorBoundary extends React.Component<
  { children: React.ReactNode; fallback?: React.ReactNode },
  { hasError: boolean; error?: Error }
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    // Log to error tracking service
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <Container>
          <Title order={2}>Something went wrong</Title>
          <Text c="dimmed">{this.state.error?.message}</Text>
          <Button onClick={() => this.setState({ hasError: false })}>
            Try again
          </Button>
        </Container>
      );
    }

    return this.props.children;
  }
}

// Usage
<ErrorBoundary fallback={<ErrorFallback />}>
  <UserFormFeature />
</ErrorBoundary>
```

**Benefits:**

- Prevents app crashes
- Better error handling
- User-friendly error messages
- Error logging integration

---

#### 5. Render Props Pattern (for Flexibility)

**Purpose:** Share logic between components with maximum flexibility

**Implementation:**

```typescript
// Render props component
interface DataFetcherProps<T> {
  queryKey: QueryKey;
  queryFn: () => Promise<T>;
  children: (data: {
    data?: T;
    isLoading: boolean;
    error?: Error;
    refetch: () => void;
  }) => React.ReactNode;
}

export function DataFetcher<T>({
  queryKey,
  queryFn,
  children
}: DataFetcherProps<T>) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey,
    queryFn,
  });

  return <>{children({ data, isLoading, error, refetch })}</>;
}

// Usage
<DataFetcher
  queryKey={['users']}
  queryFn={() => api.get('/api/v1/users').then(r => r.data)}
>
  {({ data, isLoading, error, refetch }) => {
    if (isLoading) return <Loader />;
    if (error) return <ErrorMessage error={error} />;
    return <UserList users={data} onRefresh={refetch} />;
  }}
</DataFetcher>
```

**Benefits:**

- Maximum flexibility
- Logic reuse
- Clear data flow
- Type-safe

---

## 🔐 Security Design

### Authentication Flow Design

```
┌─────────────────────────────────────────────────────────────────┐
│                     Authentication Flow                         │
└─────────────────────────────────────────────────────────────────┘

1. User Login Request
   ↓
2. Gateway → User Service (POST /api/v1/auth/login)
   ↓
3. User Service validates credentials
   ↓
4. Generate JWT tokens (access + refresh)
   - Access token: 15 minutes, RSA256 signed
   - Refresh token: 7 days, stored in database
   - JTI (JWT ID) for blacklisting
   ↓
5. Return tokens to client
   - Set HTTP-only cookie (secure, sameSite)
   - Return tokens in response body
   ↓
6. Client stores tokens
   - Cookie: automatic with requests
   - LocalStorage: for SPA state
   ↓
7. Subsequent requests include token
   ↓
8. Gateway validates token
   - Check signature with public key
   - Check expiration
   - Check blacklist (Redis)
   - Extract user context
   ↓
9. Forward to service with user context headers
   - X-User-ID
   - X-Username
   - X-User-Roles
   - X-Tenant-ID
```

### Token Structure Design

**Access Token Claims:**

```json
{
  "sub": "1", // User ID
  "username": "john.doe",
  "email": "john.doe@example.com",
  "roles": ["USER", "ADMIN"],
  "permissions": ["READ_USERS", "WRITE_USERS"],
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "tenant-123",
  "type": "access",
  "iss": "gripday-user-service",
  "iat": 1634567890,
  "exp": 1634568790,
  "jti": "unique-token-id" // For blacklisting
}
```

**Design Decisions:**

- **Rich Claims:** Include user context to avoid database lookups
- **JTI:** Enables token blacklisting for logout
- **Type:** Distinguish access vs refresh tokens
- **Tenant ID:** Multi-tenant context propagation
- **Expiration:** Short-lived for security, refresh for UX

### Multi-Tenant Security Design

**Tenant Isolation Layers:**

1. **Network Layer:** Separate VPCs per tenant (future)
2. **Application Layer:** Tenant context validation
3. **Database Layer:** Schema-per-tenant isolation
4. **Cache Layer:** Tenant-scoped Redis keys

**Tenant Context Flow:**

```
Request → Gateway → Extract Tenant ID → Validate Tenant → Set Context → Service
                    (Header/JWT/Subdomain)  (Exists/Enabled)  (ThreadLocal)
```

**Tenant Validation:**

```java
@Component
public class TenantValidationFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String tenantId = extractTenantId(exchange);

    return tenantRepository
      .findById(tenantId)
      .filter(Tenant::isEnabled)
      .switchIfEmpty(Mono.error(new TenantNotFoundException(tenantId)))
      .flatMap((tenant) -> {
        // Add tenant to exchange attributes
        exchange.getAttributes().put("tenantId", tenantId);

        // Add tenant header for downstream services
        ServerHttpRequest request = exchange.getRequest().mutate().header("X-Tenant-ID", tenantId).build();

        return chain.filter(exchange.mutate().request(request).build());
      });
  }
}
```

### Rate Limiting Design

**Multi-Level Rate Limiting:**

1. **Global IP-based:** 1000 requests/minute per IP
2. **Tenant-based:** Configurable per tenant (default: 100/minute)
3. **Endpoint-specific:** Different limits per endpoint
4. **User-based:** Per-user limits (future)

**Rate Limit Algorithm:**

```
Sliding Window Log with Redis ZSET

Key: rate_limit:{identifier}
Value: ZSET with timestamp as score

Algorithm:
1. Add current request: ZADD key timestamp uuid
2. Remove old entries: ZREMRANGEBYSCORE key 0 (now - window)
3. Count requests: ZCOUNT key (now - window) now
4. Check limit: count > quota
5. Set expiration: EXPIRE key (window * 2)
```

**Benefits:**

- Accurate counting
- No burst issues
- Automatic cleanup
- Distributed across instances

### Account Security Design

**Account Lockout Mechanism:**

```java
@Service
public class AccountLockoutService {

  private final RedisTemplate<String, String> redisTemplate;

  public void recordFailedAttempt(String username) {
    String key = "failed_attempts:" + username;
    Long attempts = redisTemplate.opsForValue().increment(key);

    // Set expiration on first attempt
    if (attempts == 1) {
      redisTemplate.expire(key, Duration.ofMinutes(30));
    }

    // Lock account after 5 attempts
    if (attempts >= 5) {
      lockAccount(username, Duration.ofMinutes(15));
    }
  }

  private void lockAccount(String username, Duration duration) {
    String lockKey = "account_locked:" + username;
    redisTemplate.opsForValue().set(lockKey, "true", duration);
  }

  public boolean isAccountLocked(String username) {
    return Boolean.TRUE.equals(redisTemplate.hasKey("account_locked:" + username));
  }

  public void clearFailedAttempts(String username) {
    redisTemplate.delete("failed_attempts:" + username);
  }
}
```

**Design Features:**

- Sliding window (30 minutes)
- Progressive lockout (15 minutes)
- Automatic unlock
- Redis-backed for distribution

---

## 📊 Data Design

### Database Schema Design

#### User Service Schema

**Core Tables:**

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT username_length CHECK (LENGTH(username) >= 3)
);

-- Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User-Role junction table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Permissions table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Role-Permission junction table
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Organizations table
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT REFERENCES users(id),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT org_name_unique UNIQUE (name)
);

-- Tenants table (in public schema)
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    max_users INTEGER DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Email verification tokens
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token UUID UNIQUE NOT NULL,
    used BOOLEAN DEFAULT false,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token UUID UNIQUE NOT NULL,
    used BOOLEAN DEFAULT false,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User audit log
CREATE TABLE user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    details JSONB,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User preferences
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    locale VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    theme VARCHAR(20) DEFAULT 'light',
    notifications_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_user_audit_log_user_id ON user_audit_log(user_id);
CREATE INDEX idx_user_audit_log_created_at ON user_audit_log(created_at);
CREATE INDEX idx_user_audit_log_correlation_id ON user_audit_log(correlation_id);
```

**Design Decisions:**

1. **BIGSERIAL for IDs:** Supports large-scale growth
2. **Timestamps:** Track creation and updates
3. **Soft Deletes:** Use `enabled` flag instead of DELETE
4. **Audit Trail:** Comprehensive logging with correlation IDs
5. **Indexes:** Optimize common queries (username, email, tokens)
6. **Constraints:** Enforce data integrity at database level
7. **JSONB for Details:** Flexible audit log details

#### Bookstore Service Schema

```sql
-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Books table
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(17) UNIQUE NOT NULL,
    price_amount DECIMAL(10, 2) NOT NULL,
    price_currency VARCHAR(3) DEFAULT 'USD',
    category_id BIGINT REFERENCES categories(id),
    available BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT price_positive CHECK (price_amount > 0)
);

-- Inventory table
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT UNIQUE NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT reserved_non_negative CHECK (reserved >= 0),
    CONSTRAINT reserved_not_exceed_quantity CHECK (reserved <= quantity)
);

-- Indexes
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_books_available ON books(available);
CREATE INDEX idx_inventory_book_id ON inventory(book_id);
CREATE INDEX idx_inventory_low_stock ON inventory(quantity) WHERE quantity <= low_stock_threshold;
```

**Design Decisions:**

1. **Separate Inventory:** Inventory is separate aggregate
2. **Constraints:** Enforce business rules (positive price, quantity)
3. **Partial Index:** Optimize low stock queries
4. **Money Pattern:** Store amount and currency separately
5. **ISBN Uniqueness:** Enforce at database level

### Caching Strategy Design

**Multi-Level Caching:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Caching Layers                           │
└─────────────────────────────────────────────────────────────┘

L1: Application Cache (Caffeine)
    - JVM heap-based
    - Fastest access (nanoseconds)
    - Limited size (1000 entries)
    - Per-instance cache
    - Use for: Frequently accessed, rarely changed data

L2: Distributed Cache (Redis)
    - Network-based
    - Fast access (milliseconds)
    - Larger size (GBs)
    - Shared across instances
    - Use for: Session data, rate limiting, token blacklist

L3: Database
    - Disk-based
    - Slower access (10-100ms)
    - Unlimited size
    - Source of truth
    - Use for: All persistent data
```

**Cache-Aside Pattern:**

```java
@Service
public class UserService {

  private final UserRepository userRepository;
  private final RedisTemplate<String, User> redisTemplate;

  public User findById(Long id) {
    String cacheKey = "user:" + id;

    // Try L2 cache (Redis)
    User cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      return cached;
    }

    // Cache miss - fetch from database
    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

    // Store in cache with TTL
    redisTemplate.opsForValue().set(cacheKey, user, Duration.ofMinutes(15));

    return user;
  }

  public User updateUser(Long id, UpdateUserRequest request) {
    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

    // Update user
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user = userRepository.save(user);

    // Invalidate cache
    String cacheKey = "user:" + id;
    redisTemplate.delete(cacheKey);

    return user;
  }
}
```

**Cache Invalidation Strategies:**

1. **TTL-based:** Automatic expiration (15 minutes for user data)
2. **Event-based:** Invalidate on updates/deletes
3. **Pattern-based:** Invalidate multiple keys (e.g., all user lists)
4. **Manual:** Explicit cache clear for critical operations

---

## 🚀 Performance Design

### Response Time Targets

**Target Response Times (95th percentile):**

| Operation Type | Target   | Rationale                       |
| -------------- | -------- | ------------------------------- |
| Authentication | < 200ms  | Critical path, user-facing      |
| User CRUD      | < 300ms  | Interactive operations          |
| Search/List    | < 500ms  | May involve complex queries     |
| Dashboard      | < 1000ms | Multiple data sources           |
| Reports        | < 3000ms | Complex aggregations acceptable |

### Database Query Optimization

**Indexing Strategy:**

```sql
-- Composite index for common query patterns
CREATE INDEX idx_users_enabled_created ON users(enabled, created_at DESC);

-- Partial index for active users only
CREATE INDEX idx_users_active ON users(username, email) WHERE enabled = true;

-- Covering index (includes all needed columns)
CREATE INDEX idx_users_list ON users(id, username, email, created_at)
    WHERE enabled = true;

-- Full-text search index
CREATE INDEX idx_books_search ON books USING gin(
    to_tsvector('english', title || ' ' || author)
);
```

**Query Optimization Patterns:**

```java
// Bad: N+1 query problem
List<User> users = userRepository.findAll();
for (User user : users) {
    Set<Role> roles = user.getRoles(); // Lazy load - N queries
}

// Good: Fetch join
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.enabled = true")
List<User> findAllWithRoles();

// Good: Pagination with count query optimization
@Query(value = "SELECT u FROM User u WHERE u.enabled = true",
       countQuery = "SELECT COUNT(u) FROM User u WHERE u.enabled = true")
Page<User> findAllEnabled(Pageable pageable);
```

### Connection Pool Tuning

**HikariCP Configuration:**

```yaml
spring:
  datasource:
    hikari:
      # Pool sizing formula: connections = ((core_count * 2) + effective_spindle_count)
      maximum-pool-size: 20
      minimum-idle: 5

      # Connection timeout
      connection-timeout: 30000 # 30 seconds

      # Idle timeout
      idle-timeout: 600000 # 10 minutes

      # Max lifetime
      max-lifetime: 1800000 # 30 minutes

      # Leak detection
      leak-detection-threshold: 60000 # 1 minute

      # Validation
      connection-test-query: SELECT 1
      validation-timeout: 5000
```

**Design Rationale:**

- **Pool Size:** Based on CPU cores and disk spindles
- **Timeouts:** Balance between resource usage and availability
- **Leak Detection:** Catch connection leaks early
- **Validation:** Ensure connections are healthy

### Reactive Programming Benefits

**Blocking vs Non-Blocking:**

```java
// Blocking approach (traditional)
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
  // Thread blocked waiting for DB
  User user = userRepository.findById(id).orElseThrow();

  // Thread blocked waiting for external API
  UserDetails details = externalApi.getDetails(id);

  return combineUserAndDetails(user, details);
}

// Requires: 1 thread per request
// Throughput: Limited by thread pool size

// Non-blocking approach (reactive)
@GetMapping("/users/{id}")
public Mono<User> getUser(@PathVariable Long id) {
  return userRepository
    .findById(id)
    .zipWith(externalApi.getDetails(id))
    .map((tuple) -> combineUserAndDetails(tuple.getT1(), tuple.getT2()));
}
// Requires: Few threads (event loop)
// Throughput: Much higher, limited by CPU/network
```

**Gateway Performance:**

```
Traditional Gateway (Blocking):
- 200 threads
- Each request holds thread
- Max throughput: ~200 concurrent requests

Reactive Gateway (Non-Blocking):
- 8 threads (CPU cores)
- Threads never block
- Max throughput: ~10,000+ concurrent requests
```

### Frontend Performance Optimization

**Code Splitting Strategy:**

```typescript
// Route-based code splitting (automatic with TanStack Router)
const DashboardRoute = createFileRoute('/dashboard')({
  component: lazy(() => import('./dashboard')),
});

// Component-based code splitting
const HeavyChart = lazy(() => import('./heavy-chart'));

// Usage with Suspense
<Suspense fallback={<Loader />}>
  <HeavyChart data={data} />
</Suspense>
```

**Memoization Strategy:**

```typescript
// Expensive computation
const ExpensiveComponent = ({ data, filter }: Props) => {
  // Memoize filtered data
  const filteredData = useMemo(
    () => data.filter(item => item.category === filter),
    [data, filter]
  );

  // Memoize callback
  const handleClick = useCallback(
    (id: string) => {
      console.log('Clicked:', id);
    },
    []
  );

  return <DataTable data={filteredData} onClick={handleClick} />;
};

// Memoize component
export default React.memo(ExpensiveComponent);
```

**Bundle Size Optimization:**

```typescript
// Tree shaking - import only what you need
import { Button } from '@mantine/core';  // ✅ Good
import * as Mantine from '@mantine/core';  // ❌ Bad

// Dynamic imports for large libraries
const loadPDF = async () => {
  const pdfjs = await import('pdfjs-dist');
  return pdfjs;
};

// Lazy load images
<img
  src={thumbnail}
  loading="lazy"
  alt="Product"
/>
```

### Caching Headers Design

**HTTP Caching Strategy:**

```java
@GetMapping("/api/v1/books/{id}")
public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
  Book book = bookService.findById(id);

  return ResponseEntity.ok()
    // Cache for 1 hour
    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
    // ETag for conditional requests
    .eTag(String.valueOf(book.getVersion()))
    // Last modified
    .lastModified(book.getUpdatedAt().toEpochMilli())
    .body(BookMapper.toResponse(book));
}
```

**Frontend Caching:**

```typescript
// TanStack Query caching
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minute
      cacheTime: 1000 * 60 * 5, // 5 minutes
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
    },
  },
});
```

---

## 🧪 Testing Design

### Testing Pyramid

```
                    ▲
                   / \
                  /   \
                 /  E2E \          10% - End-to-End Tests
                /_______\          - Full user flows
               /         \         - Browser automation
              /Integration\        - Slow, expensive
             /_____________\
            /               \      30% - Integration Tests
           /   Unit Tests    \     - API endpoints
          /___________________\    - Database integration
                                   - External services

                                   60% - Unit Tests
                                   - Business logic
                                   - Domain models
                                   - Utilities
                                   - Fast, isolated
```

### Backend Testing Strategy

**Unit Tests (60%):**

```java
// Domain logic testing
@Test
void book_canBeSold_whenAvailableAndInStock() {
  // Arrange
  Book book = Book.create("Title", "Author", isbn, price, category);
  Inventory inventory = Inventory.create(book.getId(), 10);

  // Act
  boolean canBeSold = book.canBeSold();

  // Assert
  assertTrue(canBeSold);
}

// Service logic testing
@Test
void createUser_withValidData_createsUser() {
  // Arrange
  CreateUserRequest request = new CreateUserRequest("testuser", "test@example.com", "password", "Test", "User", Set.of("USER"));
  when(userRepository.existsByUsername(anyString())).thenReturn(false);
  when(userRepository.save(any())).thenAnswer((i) -> i.getArgument(0));

  // Act
  UserResponse response = userService.createUser(request);

  // Assert
  assertNotNull(response);
  assertEquals("testuser", response.username());
  verify(userRepository).save(any(User.class));
}
```

**Integration Tests (30%):**

```java
// API endpoint testing with Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("test").withUsername("test").withPassword("test");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void createUser_withValidRequest_returns201() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest("newuser", "new@example.com", "SecurePass123!", "New", "User", Set.of("USER"));

    // Act
    ResponseEntity<UserResponse> response = restTemplate.withBasicAuth("admin", "admin").postForEntity("/api/v1/admin/users", request, UserResponse.class);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("newuser", response.getBody().username());
  }
}
```

**Architecture Tests:**

```java
// ArchUnit tests for architecture compliance
@AnalyzeClasses(packages = "org.gripday.user")
class ArchitectureTest {

  @ArchTest
  static final ArchRule servicesOnlyAccessedByControllers = classes().that().resideInAPackage("..service..").should().onlyBeAccessed().byAnyPackage("..presentation..", "..service..");

  @ArchTest
  static final ArchRule repositoriesOnlyAccessedByServices = classes().that().resideInAPackage("..repository..").should().onlyBeAccessed().byAnyPackage("..service..", "..repository..");

  @ArchTest
  static final ArchRule noCyclicDependencies = slices().matching("org.gripday.user.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule controllersAnnotatedWithRestController = classes().that().resideInAPackage("..presentation.web..").should().beAnnotatedWith(RestController.class);
}
```

### Frontend Testing Strategy

**Unit Tests (60%):**

```typescript
// Component testing
describe('UserFormFeature', () => {
  it('renders form fields', () => {
    render(<UserFormFeature />);

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('validates required fields', async () => {
    const user = userEvent.setup();
    render(<UserFormFeature />);

    const submitButton = screen.getByRole('button', { name: /submit/i });
    await user.click(submitButton);

    expect(await screen.findByText(/username is required/i)).toBeInTheDocument();
  });

  it('submits form with valid data', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<UserFormFeature onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'SecurePass123!');
    await user.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        username: 'testuser',
        email: 'test@example.com',
        password: 'SecurePass123!',
      });
    });
  });
});

// Hook testing
describe('useFormMutation', () => {
  it('maps field errors to form', async () => {
    const form = useForm({ initialValues: { email: '' } });
    const apiCall = vi.fn().mockRejectedValue({
      response: {
        data: {
          fields: [{ field: 'email', message: 'Email already exists' }],
        },
      },
    });

    const { result } = renderHook(() => useFormMutation(form, apiCall));

    await act(async () => {
      await result.current.mutateAsync({ email: 'test@example.com' });
    });

    expect(form.errors.email).toBe('Email already exists');
  });
});
```

**E2E Tests (10%):**

```typescript
// Playwright E2E tests
test.describe("User Authentication", () => {
  test("user can sign up and login", async ({ page }) => {
    // Sign up
    await page.goto("/register");
    await page.fill('[name="username"]', "testuser");
    await page.fill('[name="email"]', "test@example.com");
    await page.fill('[name="password"]', "SecurePass123!");
    await page.click('button[type="submit"]');

    // Should redirect to login
    await expect(page).toHaveURL("/login");

    // Login
    await page.fill('[name="username"]', "testuser");
    await page.fill('[name="password"]', "SecurePass123!");
    await page.click('button[type="submit"]');

    // Should redirect to dashboard
    await expect(page).toHaveURL("/dashboard");
    await expect(page.locator("h1")).toContainText("Dashboard");
  });

  test("shows error for invalid credentials", async ({ page }) => {
    await page.goto("/login");
    await page.fill('[name="username"]', "wronguser");
    await page.fill('[name="password"]', "wrongpass");
    await page.click('button[type="submit"]');

    await expect(page.locator('[role="alert"]')).toContainText("Invalid credentials");
  });
});
```

**Architecture Tests:**

```typescript
// FSD architecture compliance tests
describe("Architecture Tests", () => {
  it("all FSD layers exist", () => {
    const layers = ["app", "processes", "pages", "widgets", "features", "entities", "shared"];

    layers.forEach((layer) => {
      expect(fs.existsSync(`src/${layer}`)).toBe(true);
    });
  });

  it("all features have public API (index.ts)", () => {
    const features = fs.readdirSync("src/features");

    features.forEach((feature) => {
      const indexPath = `src/features/${feature}/index.ts`;
      expect(fs.existsSync(indexPath)).toBe(true);
    });
  });

  it("features have required segments (ui, model)", () => {
    const features = fs.readdirSync("src/features");

    features.forEach((feature) => {
      const uiPath = `src/features/${feature}/ui`;
      const modelPath = `src/features/${feature}/model`;

      expect(fs.existsSync(uiPath) || fs.existsSync(modelPath)).toBe(true);
    });
  });
});
```

### Test Data Management

**Test Fixtures:**

```java
// Test data builders
public class UserTestBuilder {

  private String username = "testuser";
  private String email = "test@example.com";
  private String password = "password";
  private Set<Role> roles = Set.of(Role.USER);

  public UserTestBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  public UserTestBuilder withEmail(String email) {
    this.email = email;
    return this;
  }

  public UserTestBuilder withRoles(Role... roles) {
    this.roles = Set.of(roles);
    return this;
  }

  public User build() {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRoles(roles);
    user.setEnabled(true);
    return user;
  }
}

// Usage
User admin = new UserTestBuilder().withUsername("admin").withRoles(Role.ADMIN, Role.USER).build();
```

**Mock Service Worker (MSW):**

```typescript
// API mocking for frontend tests
export const handlers = [
  http.post("/api/v1/auth/login", async ({ request }) => {
    const body = await request.json();

    if (body.username === "testuser" && body.password === "password") {
      return HttpResponse.json({
        accessToken: "mock-token",
        user: { id: 1, username: "testuser" },
      });
    }

    return HttpResponse.json({ type: "auth-error", message: "Invalid credentials" }, { status: 401 });
  }),

  http.get("/api/v1/users/me", () => {
    return HttpResponse.json({
      id: 1,
      username: "testuser",
      email: "test@example.com",
    });
  }),
];

// Setup in tests
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

---

## 🔄 CI/CD Design

### Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD Pipeline                           │
└─────────────────────────────────────────────────────────────┘

Trigger: Push to branch or Pull Request

┌──────────────┐
│   Checkout   │
└──────┬───────┘
       │
┌──────▼───────┐
│  Validation  │  - Commit message format
│              │  - Branch naming
└──────┬───────┘
       │
┌──────▼───────┐
│    Build     │  - Maven/PNPM install
│              │  - Compile code
└──────┬───────┘
       │
┌──────▼───────┐
│  Code Quality│  - Checkstyle
│              │  - ESLint/Prettier
│              │  - SonarQube scan
└──────┬───────┘
       │
┌──────▼───────┐
│  Unit Tests  │  - JUnit/Vitest
│              │  - Coverage report
│              │  - Fail if < threshold
└──────┬───────┘
       │
┌──────▼───────┐
│ Integration  │  - Testcontainers
│    Tests     │  - API tests
└──────┬───────┘
       │
┌──────▼───────┐
│  E2E Tests   │  - Playwright
│              │  - Full user flows
└──────┬───────┘
       │
┌──────▼───────┐
│Architecture  │  - ArchUnit
│   Tests      │  - FSD compliance
└──────┬───────┘
       │
┌──────▼───────┐
│   Security   │  - Dependency scan
│    Scan      │  - OWASP check
└──────┬───────┘
       │
┌──────▼───────┐
│Docker Build  │  - Build images
│              │  - Tag with version
└──────┬───────┘
       │
┌──────▼───────┐
│   Deploy     │  - Push to registry
│              │  - Deploy to env
└──────────────┘
```

### GitHub Actions Workflow

**Backend CI:**

```yaml
name: Backend CI

on:
  push:
    branches: [dev, main]
    paths:
      - "backend/**"
  pull_request:
    branches: [dev, main]
    paths:
      - "backend/**"

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: "maven"

      - name: Validate commit messages
        run: |
          npx commitlint --from HEAD~1 --to HEAD

      - name: Build with Maven
        run: |
          cd backend
          mvn clean install -DskipTests

      - name: Run unit tests
        run: |
          cd backend
          mvn test -Dcheckstyle.skip=true

      - name: Run integration tests
        run: |
          cd backend
          mvn verify -DskipUnitTests -Dcheckstyle.skip=true
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test
          SPRING_DATA_REDIS_HOST: localhost

      - name: Run Checkstyle
        run: |
          cd backend
          mvn checkstyle:check

      - name: Generate coverage report
        run: |
          cd backend
          mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./backend/target/site/jacoco/jacoco.xml

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          cd backend
          mvn sonar:sonar \
            -Dsonar.projectKey=gripday-backend \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }}

      - name: Build Docker images
        if: github.ref == 'refs/heads/main'
        run: |
          cd backend
          docker build -t gripday/user-service:${{ github.sha }} ./gripday-user-service
          docker build -t gripday/gateway-service:${{ github.sha }} ./gripday-gateway-service
          docker build -t gripday/bookstore-service:${{ github.sha }} ./gripday-bookstore-service

      - name: Push to registry
        if: github.ref == 'refs/heads/main'
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push gripday/user-service:${{ github.sha }}
          docker push gripday/gateway-service:${{ github.sha }}
          docker push gripday/bookstore-service:${{ github.sha }}
```

**Frontend CI:**

```yaml
name: Frontend CI

on:
  push:
    branches: [dev, main]
    paths:
      - "auth.gripday.com/**"
      - "app.gripday.com/**"
  pull_request:
    branches: [dev, main]
    paths:
      - "auth.gripday.com/**"
      - "app.gripday.com/**"

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        app: [auth.gripday.com, app.gripday.com]

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "22"

      - name: Setup PNPM
        uses: pnpm/action-setup@v4
        with:
          version: 10.20.0

      - name: Get PNPM store directory
        id: pnpm-cache
        run: echo "STORE_PATH=$(pnpm store path)" >> $GITHUB_OUTPUT

      - name: Setup PNPM cache
        uses: actions/cache@v4
        with:
          path: ${{ steps.pnpm-cache.outputs.STORE_PATH }}
          key: ${{ runner.os }}-pnpm-store-${{ hashFiles('**/pnpm-lock.yaml') }}
          restore-keys: |
            ${{ runner.os }}-pnpm-store-

      - name: Install dependencies
        run: |
          cd ${{ matrix.app }}
          pnpm install --frozen-lockfile

      - name: Lint
        run: |
          cd ${{ matrix.app }}
          pnpm lint

      - name: Type check
        run: |
          cd ${{ matrix.app }}
          pnpm type-check

      - name: Run unit tests
        run: |
          cd ${{ matrix.app }}
          pnpm test:coverage

      - name: Run architecture tests
        run: |
          cd ${{ matrix.app }}
          pnpm test:arch

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./${{ matrix.app }}/coverage/coverage-final.json

      - name: Install Playwright browsers
        run: |
          cd ${{ matrix.app }}
          pnpm playwright:install --with-deps

      - name: Run E2E tests
        run: |
          cd ${{ matrix.app }}
          pnpm e2e

      - name: Upload Playwright report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report-${{ matrix.app }}
          path: ${{ matrix.app }}/playwright-report/
          retention-days: 30

      - name: Build
        run: |
          cd ${{ matrix.app }}
          pnpm build

      - name: Build Docker image
        if: github.ref == 'refs/heads/main'
        run: |
          cd ${{ matrix.app }}
          docker build -t gripday/${{ matrix.app }}:${{ github.sha }} .

      - name: Push to registry
        if: github.ref == 'refs/heads/main'
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push gripday/${{ matrix.app }}:${{ github.sha }}
```

### Deployment Strategy

**Environment Progression:**

```
Development → Staging → Production

Development:
- Auto-deploy on push to dev branch
- Latest features
- May be unstable
- Full logging enabled

Staging:
- Auto-deploy on push to main branch
- Production-like environment
- Final testing before production
- Performance testing

Production:
- Manual approval required
- Tagged releases only
- Blue-green deployment
- Rollback capability
```

**Kubernetes Deployment:**

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: gripday
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        version: v1.0.0
    spec:
      containers:
        - name: user-service
          image: gripday/user-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: database-secret
                  key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

---

## 📚 Design Trade-offs & Decisions

### Microservices vs Monolith

**Decision:** Microservices  
**Trade-off:**

| Aspect      | Microservices (Chosen)   | Monolith            |
| ----------- | ------------------------ | ------------------- |
| Scalability | ✅ Independent scaling   | ❌ Scale entire app |
| Deployment  | ✅ Independent deploys   | ❌ All-or-nothing   |
| Technology  | ✅ Polyglot possible     | ❌ Single stack     |
| Complexity  | ❌ Higher operational    | ✅ Simpler ops      |
| Development | ❌ Distributed debugging | ✅ Easier debugging |
| Performance | ❌ Network overhead      | ✅ In-process calls |

**Rationale:** Platform designed for growth and team autonomy. Operational complexity acceptable for long-term benefits.

---

### JWT vs Session-Based Auth

**Decision:** JWT with RSA256  
**Trade-off:**

| Aspect      | JWT (Chosen)             | Session-Based           |
| ----------- | ------------------------ | ----------------------- |
| Scalability | ✅ Stateless             | ❌ Session storage      |
| Revocation  | ❌ Requires blacklist    | ✅ Immediate            |
| Token Size  | ❌ Larger (1-2KB)        | ✅ Small (session ID)   |
| Security    | ✅ Signed, verifiable    | ❌ Requires lookup      |
| Distributed | ✅ Works across services | ❌ Shared session store |

**Rationale:** Stateless authentication critical for horizontal scaling. Blacklist acceptable trade-off for revocation.

---

### Schema-Per-Tenant vs Row-Level Security

**Decision:** Schema-per-tenant  
**Trade-off:**

| Aspect      | Schema-Per-Tenant (Chosen)  | Row-Level Security          |
| ----------- | --------------------------- | --------------------------- |
| Isolation   | ✅ Complete separation      | ❌ Logical only             |
| Security    | ✅ No cross-tenant queries  | ❌ Requires careful coding  |
| Performance | ✅ Better query performance | ❌ tenant_id in every query |
| Migrations  | ❌ Per-tenant complexity    | ✅ Single migration         |
| Backup      | ✅ Per-tenant possible      | ❌ All-or-nothing           |
| Compliance  | ✅ Easier audits            | ❌ More complex             |

**Rationale:** Strong isolation requirements for SaaS. Migration complexity acceptable for security benefits.

---

### Reactive vs Blocking Gateway

**Decision:** Reactive (Spring Cloud Gateway)  
**Trade-off:**

| Aspect     | Reactive (Chosen)         | Blocking (Zuul)       |
| ---------- | ------------------------- | --------------------- |
| Throughput | ✅ 10,000+ req/s          | ❌ 200-500 req/s      |
| Resources  | ✅ Fewer threads          | ❌ Thread per request |
| Complexity | ❌ Steeper learning curve | ✅ Simpler code       |
| Debugging  | ❌ More difficult         | ✅ Easier             |
| Ecosystem  | ✅ Modern, active         | ❌ Maintenance mode   |

**Rationale:** Gateway is critical path. Performance benefits outweigh complexity for high-traffic scenarios.

---

### Feature-Sliced Design vs Other Architectures

**Decision:** Feature-Sliced Design  
**Trade-off:**

| Aspect             | FSD (Chosen)         | Atomic Design       | Feature Folders     |
| ------------------ | -------------------- | ------------------- | ------------------- |
| Scalability        | ✅ Linear growth     | ❌ UI-focused       | ⚠️ Can become messy |
| Business Logic     | ✅ Clear location    | ❌ Not addressed    | ⚠️ Scattered        |
| Learning Curve     | ❌ Requires training | ✅ Intuitive        | ✅ Simple           |
| Enforcement        | ✅ Testable rules    | ❌ No rules         | ❌ No rules         |
| Team Collaboration | ✅ Clear boundaries  | ⚠️ Overlap possible | ❌ Conflicts likely |

**Rationale:** Long-term maintainability critical. Initial learning curve acceptable for scalable architecture.

---

### TanStack Query vs Redux

**Decision:** TanStack Query  
**Trade-off:**

| Aspect         | TanStack Query (Chosen) | Redux Toolkit            |
| -------------- | ----------------------- | ------------------------ |
| Server State   | ✅ Built-in caching     | ❌ Manual implementation |
| Boilerplate    | ✅ Minimal              | ❌ More code             |
| DevTools       | ✅ Excellent            | ✅ Excellent             |
| Learning Curve | ✅ Easier               | ❌ Steeper               |
| Flexibility    | ⚠️ Opinionated          | ✅ Very flexible         |

**Rationale:** Most state is server state. TanStack Query optimized for this use case with less code.

---

### PostgreSQL vs NoSQL

**Decision:** PostgreSQL  
**Trade-off:**

| Aspect        | PostgreSQL (Chosen) | MongoDB       | DynamoDB        |
| ------------- | ------------------- | ------------- | --------------- |
| ACID          | ✅ Full support     | ⚠️ Limited    | ⚠️ Limited      |
| Relationships | ✅ Native joins     | ❌ Manual     | ❌ Manual       |
| Schema        | ✅ Enforced         | ❌ Flexible   | ❌ Flexible     |
| Scalability   | ⚠️ Vertical first   | ✅ Horizontal | ✅ Horizontal   |
| Maturity      | ✅ 30+ years        | ⚠️ 15 years   | ⚠️ AWS-specific |

**Rationale:** Relational data model fits domain. ACID guarantees important for financial/user data.

---

### Docker vs Kubernetes Native

**Decision:** Docker + Kubernetes  
**Trade-off:**

| Aspect       | Docker + K8s (Chosen) | K8s Native           |
| ------------ | --------------------- | -------------------- |
| Portability  | ✅ Run anywhere       | ❌ K8s only          |
| Local Dev    | ✅ Docker Compose     | ❌ Minikube required |
| Simplicity   | ✅ Familiar           | ❌ More complex      |
| K8s Features | ⚠️ Some unused        | ✅ Full power        |

**Rationale:** Docker provides flexibility for local development and non-K8s deployments.

---

## 🎓 Lessons Learned & Best Practices

### What Worked Well

1. **Feature-Sliced Design**
   - Clear boundaries prevented spaghetti code
   - Architecture tests caught violations early
   - New developers onboarded quickly

2. **Testcontainers**
   - Real database in tests caught issues
   - No mocking of infrastructure
   - Tests closer to production

3. **RFC 9457 Error Handling**
   - Consistent errors across services
   - Frontend automatic field mapping
   - Better debugging with correlation IDs

4. **OpenTelemetry**
   - End-to-end request tracing
   - Performance bottleneck identification
   - Vendor-neutral approach

5. **TanStack Query**
   - Reduced API calls significantly
   - Better UX with optimistic updates
   - Less boilerplate than Redux

### What Could Be Improved

1. **Reactive Programming Complexity**
   - Steeper learning curve than expected
   - Debugging more difficult
   - Consider: Provide more training/examples

2. **Multi-Tenant Migrations**
   - Per-tenant migrations slow
   - Consider: Parallel execution
   - Consider: Migration service

3. **Test Data Management**
   - Test data setup repetitive
   - Consider: Test data builders
   - Consider: Shared fixtures

4. **Documentation**
   - API docs sometimes outdated
   - Consider: Generate from code
   - Consider: API contract testing

5. **Monitoring Alerts**
   - Too many false positives initially
   - Consider: Tune thresholds
   - Consider: Alert fatigue prevention

### Recommendations for Future Projects

1. **Start Simple**
   - Begin with modular monolith
   - Extract microservices when needed
   - Don't over-engineer early

2. **Invest in Observability Early**
   - Logging, metrics, tracing from day one
   - Harder to add later
   - Critical for debugging distributed systems

3. **Automate Everything**
   - CI/CD from the start
   - Automated testing
   - Automated deployments

4. **Document Decisions**
   - ADRs for major decisions
   - Rationale important for future
   - Prevents repeating mistakes

5. **Focus on Developer Experience**
   - Fast feedback loops
   - Good local development setup
   - Clear error messages

---

## 📖 References & Resources

### Architecture Patterns

- [Microservices Patterns](https://microservices.io/patterns/)
- [Feature-Sliced Design](https://feature-sliced.design/)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)

### Technology Documentation

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [React](https://react.dev/)
- [TanStack Query](https://tanstack.com/query/latest)
- [Mantine UI](https://mantine.dev/)

### Best Practices

- [12-Factor App](https://12factor.net/)
- [REST API Design](https://restfulapi.net/)
- [OpenAPI Specification](https://swagger.io/specification/)

---

## 🎯 Conclusion

This design document captures the architectural decisions, patterns, and rationale behind the Gripday platform. It serves as a reference for:

- **Developers:** Understanding why things are built this way
- **AI Assistants (Kiro):** Context for making informed suggestions
- **Architects:** Evaluating design decisions
- **New Team Members:** Onboarding and learning

The design prioritizes:

- ✅ Scalability for growth
- ✅ Security for enterprise use
- ✅ Maintainability for long-term success
- ✅ Developer experience for productivity

**Remember:** These are design decisions for this specific platform. Different contexts may require different choices. Always evaluate trade-offs for your specific requirements.

---

**Document Status:** Ready for Kiro AI Development ✨
