# IQ Scaffold Platform — Coding Guidelines

> Consolidated conventions, design rules, and clean coding principles extracted from all platform microservices.
> Every developer building a new service or feature must follow this document.

---

## Table of Contents

1. [Project Setup & Build](#1-project-setup--build)
2. [Package & Module Structure](#2-package--module-structure)
3. [Naming Conventions](#3-naming-conventions)
4. [Domain / Entity Layer](#4-domain--entity-layer)
5. [Repository Layer](#5-repository-layer)
6. [Service Layer](#6-service-layer)
7. [REST Controller Layer](#7-rest-controller-layer)
8. [DTOs & Mappers](#8-dtos--mappers)
9. [Exception Handling](#9-exception-handling)
10. [Security & Authentication](#10-security--authentication)
11. [Multi-Tenancy](#11-multi-tenancy)
12. [Database Migrations (Liquibase)](#12-database-migrations-liquibase)
13. [Configuration Management](#13-configuration-management)
14. [Messaging & Events (RabbitMQ)](#14-messaging--events-rabbitmq)
15. [Caching Strategy](#15-caching-strategy)
16. [Observability](#16-observability)
17. [Testing](#17-testing)
18. [General Clean Code Rules](#18-general-clean-code-rules)
19. [KISS — Keep It Simple, Stupid](#19-kiss--keep-it-simple-stupid)
20. [DRY — Don't Repeat Yourself](#20-dry--dont-repeat-yourself)
21. [Constant Usage](#21-constant-usage)
22. [Java 21+ Language Features](#22-java-21-language-features)
23. [Internationalization (i18n)](#23-internationalization-i18n)
24. [Inter-Service Communication & Resilience](#24-inter-service-communication--resilience)
25. [Reactive Programming (Gateway Service)](#25-reactive-programming-gateway-service)
26. [Consistency & Event Publishing (The Dual-Write Problem)](#26-consistency--event-publishing-the-dual-write-problem)
27. [Idempotency](#27-idempotency)
28. [Database Isolation & Autonomy](#28-database-isolation--autonomy)
29. [Schema & API Evolution](#29-schema--api-evolution)
30. [Kubernetes Lifecycle & Graceful Shutdown](#30-kubernetes-lifecycle--graceful-shutdown)
31. [CI/CD Pipeline & Deployment Configuration](#31-cicd-pipeline--deployment-configuration)
32. [Java Import Rules, Final Variables & Strict Code Hygiene](#32-java-import-rules-final-variables--strict-code-hygiene)

---

## 1. Project Setup & Build

### Parent POM

All services inherit from the shared parent:

```xml
<parent>
  <groupId>com.iqkv</groupId>
  <artifactId>boot-parent-pom</artifactId>
  <version>0.25.0-SNAPSHOT</version>
</parent>
```

Never override dependency versions managed by the parent. If a version override is genuinely required, it must be documented with a comment explaining why and tracked as technical debt.

### Maven Properties

Every service `pom.xml` must declare:

```xml
<properties>
  <spring-boot.run.jvmArguments>-Duser.timezone=UTC</spring-boot.run.jvmArguments>
  <checkstyle.skip>false</checkstyle.skip>
  <maven.gitcommitid.skip>false</maven.gitcommitid.skip>
  <jacoco.skip>false</jacoco.skip>
</properties>
```

### Required Dependencies

| Category      | Artifact                                                                                                                                                      |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Web           | `spring-boot-starter-web`                                                                                                                                     |
| Persistence   | `spring-boot-starter-data-jpa`, `postgresql`, `liquibase-core`                                                                                                |
| Security      | `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`                                                                                  |
| Validation    | `spring-boot-starter-validation`                                                                                                                              |
| Messaging     | `spring-boot-starter-amqp`                                                                                                                                    |
| Cache         | `spring-boot-starter-data-redis`, `hibernate-jcache`, `ehcache (jakarta)`                                                                                     |
| Observability | `spring-boot-starter-actuator`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `micrometer-registry-prometheus`, `logstash-logback-encoder` |
| API Docs      | `springdoc-openapi-starter-webmvc-ui`                                                                                                                         |
| JWT           | `jjwt-api`, `jjwt-impl`, `jjwt-jackson`                                                                                                                       |
| Testing       | `spring-boot-starter-test`, `spring-security-test`, `testcontainers:postgresql`, `archunit-junit5`                                                            |

### JaCoCo Coverage

Minimum thresholds enforced at build time:

- Bundle instruction coverage: **50%**
- Per-class line coverage: **40%**
- Per-class branch coverage: **40%**

Excluded from coverage (do not add coverage for these):

- `*Application`, `*Config`, `*Configuration`, `*Properties`
- `*RestResource`, `*Filter`, `*Listener`, `*EventListener`
- `*Exception`, `*Constants`, `*ClaimNames`
- DTOs: `*Request`, `*Response`, `*Dto`, `*Detail`
- Infrastructure: `*Runner`, `*Bootstrap`, `*Initializer`

---

## 2. Package & Module Structure

### Base Package

```
com.iqscaffold.{service-name}
```

Example: `com.iqscaffold.contactservice`

### Top-Level Package Layout

```
com.iqscaffold.{service}/
├── {DomainEntity}/               # Feature module (vertical slice)
│   ├── {Entity}.java             # JPA entity
│   ├── {Entity}Repository.java   # Spring Data repository
│   ├── {Entity}Service.java      # Service interface
│   ├── {Entity}ServiceImpl.java  # Service implementation
│   ├── {Entity}RestResource.java # REST controller
│   ├── {Entity}Status.java       # Enum (if applicable)
│   └── dto/
│       ├── {Entity}Dtos.java     # All DTOs as nested records
│       └── {Entity}Mapper.java   # Manual mapper (static methods)
├── config/                       # All @Configuration classes
│   ├── SecurityConfig.java
│   ├── RabbitMQConfig.java
│   ├── OpenApiConfig.java
│   ├── GlobalExceptionHandler.java
│   ├── IqScaffoldProperties.java
│   └── ...
├── event/                        # Event publishers
├── security/                     # JWT filter, security utilities
├── tenancy/                      # Multi-tenant infrastructure
├── shared/                       # Cross-cutting utilities
│   ├── exception/                # Custom exceptions
│   ├── {Service}Constants.java
│   └── ...
└── {ServiceName}Application.java
```

### Rules

- Organize by **feature/domain** (vertical slices), not by layer.
- Each domain package is self-contained: entity, repo, service, controller, DTOs all together.
- Infrastructure concerns (config, security, tenancy) live in their own top-level packages.
- The `shared/` package holds only truly cross-cutting utilities — not domain logic.

---

## 3. Naming Conventions

### Classes

| Type                   | Convention                      | Example                            |
| ---------------------- | ------------------------------- | ---------------------------------- |
| Entity                 | `{Entity}`                      | `Contact`, `User`                  |
| Repository             | `{Entity}Repository`            | `ContactRepository`                |
| Service interface      | `{Entity}Service`               | `ContactService`                   |
| Service implementation | `{Entity}ServiceImpl`           | `ContactServiceImpl`               |
| REST controller        | `{Entity}RestResource`          | `ContactRestResource`              |
| DTO container          | `{Entity}Dtos`                  | `ContactDtos`                      |
| Mapper                 | `{Entity}Mapper`                | `ContactMapper`                    |
| Event publisher        | `{Entity}EventPublisher`        | `ContactEventPublisher`            |
| Config class           | `{Concern}Config`               | `SecurityConfig`, `RabbitMQConfig` |
| Properties record      | `{Service/Concern}Properties`   | `IqScaffoldProperties`             |
| Constants class        | `{Service}Constants`            | `UserServiceConstants`             |
| Custom exception       | `{Concept}Exception`            | `ContactNotFoundException`         |
| Enum                   | `{Entity}Status` or descriptive | `ContactStatus`                    |

### Methods

| Operation       | Convention                                                |
| --------------- | --------------------------------------------------------- |
| Create          | `create{Entity}(...)`                                     |
| Read single     | `get{Entity}ById(...)`                                    |
| Read list/page  | `getAll{Entities}(...)`, `get{Entities}By{Criteria}(...)` |
| Update          | `update{Entity}(...)`                                     |
| Delete          | `delete{Entity}(...)`                                     |
| Check existence | `existsBy{Field}(...)`                                    |
| Count           | `countBy{Field}(...)`                                     |
| Search          | `search{Entities}(...)`                                   |
| Publish event   | `publish{Entity}{Action}(...)`                            |

### Database

- Table names: `snake_case`, plural — `contacts`, `user_authorities`
- Column names: `snake_case` — `first_name`, `created_at`, `tenant_id`
- Index names: `idx_{table}_{column(s)}` — `idx_contacts_email`
- Foreign key columns: `{referenced_table_singular}_id` — `company_id`

### Liquibase Changesets

- File naming: `YYYYMMDDhhmmss-description.xml` — `20251110167000-create-contact-tables.xml`
- Changeset ID: matches filename without extension — `20251110167000-create-contacts-table`
- Author: `iqscaffold`

---

## 4. Domain / Entity Layer

### Required Annotations

```java
@Entity
@Table(name = "contacts")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({ ... })
public class Contact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ContactStatus status = ContactStatus.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  @Column(name = "updated_by", nullable = false)
  private String updatedBy;
}
```

### Rules

- Always use `GenerationType.IDENTITY` for primary keys (PostgreSQL `BIGSERIAL`).
- Use `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate) for audit timestamps — never set them manually.
- All temporal fields use `LocalDateTime`.
- Enums stored as `@Enumerated(EnumType.STRING)` — never `ORDINAL`.
- Lazy-load all `@ManyToOne` and `@OneToMany` relationships: `fetch = FetchType.LAZY`.
- Use `@NamedEntityGraphs` to define named fetch strategies; use them in repository queries.
- Apply `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)` on all entities for L2 cache.
- Every entity must have `createdAt`, `updatedAt`, `createdBy`, `updatedBy` audit fields.
- Multi-tenant entities extend `TenantAware` base class — never add `tenant_id` manually.
- Provide a protected no-arg constructor for JPA; use a constructor with required fields for application code.
- Implement `equals()` and `hashCode()` based on `id` and natural key fields only.
- `toString()` must never expose sensitive data (passwords, tokens).
- Validation annotations (`@NotBlank`, `@Size`, `@Email`) are placed on entity fields as database-level guards. Primary validation is on DTOs — both layers must be consistent with each other.

### TenantAware Base Class

All multi-tenant entities must extend `TenantAware`:

```java
public class Contact extends TenantAware {
  // tenant_id is inherited and auto-injected via @PrePersist
}
```

`TenantAware` automatically injects `tenant_id` from `TenantContext` on persist and validates it on update.

---

## 5. Repository Layer

### Interface Declaration

```java
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
  Optional<Contact> findByEmail(String email);

  Page<Contact> findByStatus(ContactStatus status, Pageable pageable);

  boolean existsByEmail(String email);

  long countByStatus(ContactStatus status);

  @Query("SELECT c FROM Contact c WHERE " + "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  Page<Contact> searchContacts(@Param("searchTerm") String searchTerm, Pageable pageable);

  @EntityGraph("contact-with-company")
  @Query("SELECT c FROM Contact c WHERE c.id = :id")
  Optional<Contact> findByIdWithCompany(@Param("id") Long id);

  @Query("SELECT c FROM Contact c WHERE c.id IN :ids")
  List<Contact> findAllByIdIn(@Param("ids") List<Long> ids);
}
```

### Rules

- Always extend `JpaRepository<Entity, Long>`.
- Use Spring Data derived query methods for simple lookups (`findBy{Field}`).
- Use `@Query` with JPQL for complex queries; use native SQL only when JPQL cannot express it.
- Always use `@Param` for named parameters in `@Query`.
- Use `Page<Entity>` + `Pageable` for all list endpoints — never return unbounded `List` from user-facing queries.
- Define `@EntityGraph` variants for queries that need associations — prevents N+1 problems.
- Bulk fetch with `findAllByIdIn(List<Long> ids)` — never loop individual `findById` calls.
- Add `@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))` on all read-only repository methods that are called on hot paths (any method called per-request in a controller).
- Document every non-obvious query method with a Javadoc comment explaining when to use it.

---

## 6. Service Layer

### Interface + Implementation Pattern

```java
// Interface
public interface ContactService {
  Contact createContact(Contact contact);
  Optional<Contact> getContactById(Long id);
  Page<Contact> getAllContacts(Pageable pageable);
  Contact updateContact(Long id, Contact contact);
  void deleteContact(Long id);
  boolean existsByEmail(String email);
}

// Implementation
@Service
@Transactional
public class ContactServiceImpl implements ContactService {

  private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

  private final ContactRepository contactRepository;
  private final ContactEventPublisher eventPublisher;

  public ContactServiceImpl(final ContactRepository contactRepository, final ContactEventPublisher eventPublisher) {
    this.contactRepository = contactRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Contact createContact(Contact contact) {
    Contact saved = contactRepository.save(contact);
    eventPublisher.publishContactCreated(saved);
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Contact> getContactById(Long id) {
    return contactRepository.findById(id);
  }
}
```

### Rules

- Every service **must** have an interface. Inject the interface, not the implementation.
- Class-level `@Transactional` covers all write methods by default.
- Override with `@Transactional(readOnly = true)` on every read-only method — this is mandatory.
- Use constructor injection with `final` fields — never `@Autowired` on fields.
- Throw custom `RuntimeException` subclasses for domain errors; never catch and swallow them.
- Services must not return entities directly to controllers — return entities to the controller layer which maps to DTOs.
- After state-changing operations, publish domain events via the event publisher.
- Event publishing failures must be logged but must **not** fail the main transaction:

```java
try {
  eventPublisher.publishContactCreated(saved);
} catch (Exception e) {
  log.error("Failed to publish event for contactId={}", saved.getId(), e);
  // Do not rethrow — event failure is non-critical
}
```

- Use SLF4J `LoggerFactory.getLogger(ClassName.class)` — never `System.out.println`.
- Log at `INFO` for significant state changes, `DEBUG` for internal flow, `WARN` for recoverable issues, `ERROR` for failures.

---

## 7. REST Controller Layer

### Structure

```java
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Contact management operations")
@SecurityRequirement(name = "bearerAuth")
public class ContactRestResource {

  private final ContactService contactService;

  public ContactRestResource(final ContactService contactService) {
    this.contactService = contactService;
  }

  @Operation(summary = "Create contact", description = "Creates a new contact")
  @ApiResponses(
    value = {
      @ApiResponse(responseCode = "201", description = "Contact created successfully"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "409", description = "Duplicate resource"),
    }
  )
  @PostMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ContactDtos.ContactResponse> createContact(@Valid @RequestBody ContactDtos.CreateContactRequest request) {
    String userId = getCurrentUserId();
    Contact contact = ContactMapper.toEntity(request, userId);
    Contact saved = contactService.createContact(contact);
    return ResponseEntity.status(HttpStatus.CREATED).body(ContactMapper.toResponse(saved));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
    contactService.deleteContact(id);
    return ResponseEntity.noContent().build();
  }
}
```

### HTTP Status Conventions

| Operation              | Success Status   |
| ---------------------- | ---------------- |
| POST (create)          | `201 Created`    |
| GET (read)             | `200 OK`         |
| PUT (full update)      | `200 OK`         |
| PATCH (partial update) | `200 OK`         |
| DELETE                 | `204 No Content` |

### Rules

- URL pattern: `/api/v1/{resource}` — always versioned.
- `PUT` must fully replace the entity; `PATCH` must apply partial updates (ignoring nulls in the request).
- Do not expose `Page<T>` directly. Wrap paginated responses in a standard `PagedResponse<T>` DTO to avoid leaking internal Spring Data fields.
- Every endpoint must have `@Operation`, `@ApiResponses`, and `@Tag` annotations.
- Every controller class must have `@SecurityRequirement(name = "bearerAuth")`.
- Every endpoint must have `@PreAuthorize` with explicit authority list.
- Use `@Valid` on all `@RequestBody` parameters.
- Controllers must not contain business logic — delegate everything to the service layer.
- Mapping between DTOs and entities happens in the controller, using the mapper.
- Extract the current user ID from the JWT in the controller and pass it to the service. Always use `JwtClaimNames` constants — never raw strings:

```java
private String getCurrentUserId() {
  var auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth instanceof JwtAuthenticationToken jwtAuth) {
    return jwtAuth.getToken().getClaimAsString(JwtClaimNames.USER_ID);
  }
  return auth.getName();
}
```

- Duplicate-check logic (e.g., email uniqueness) belongs in the service layer — not in the controller.

---

## 8. DTOs & Mappers

### DTO Container Pattern

All DTOs for a domain are grouped in a single `{Entity}Dtos.java` class as nested records:

```java
public final class ContactDtos {

  private ContactDtos() {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateContactRequest(
    @NotBlank(message = "First name is required") @Size(max = 100, message = "First name must not exceed 100 characters") String firstName,
    @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
    @Email(message = "Email must be a valid email address") @Size(max = 255) String email,
    ContactStatus status,
    @Min(0) @Max(100) Integer leadScore
  ) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateContactRequest(@NotBlank String firstName, @NotBlank String lastName, @Email String email, ContactStatus status) {}

  public record ContactResponse(Long id, String firstName, String lastName, String email, ContactStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy) {}
}
```

### Mapper Pattern

```java
public final class ContactMapper {

  private ContactMapper() {}

  public static ContactDtos.ContactResponse toResponse(final Contact contact) {
    if (contact == null) return null;
    return new ContactDtos.ContactResponse(
        contact.getId(), contact.getFirstName(), ...);
  }

  public static Contact toEntity(
      final ContactDtos.CreateContactRequest request,
      final String createdBy) {
    if (request == null) return null;
    Contact contact = new Contact();
    contact.setFirstName(request.firstName());
    contact.setCreatedBy(createdBy);
    contact.setUpdatedBy(createdBy);
    return contact;
  }

  public static void updateEntity(
      final Contact contact,
      final ContactDtos.UpdateContactRequest request,
      final String updatedBy) {
    if (contact == null || request == null) return;
    contact.setFirstName(request.firstName());
    contact.setUpdatedBy(updatedBy);
  }
}
```

### Rules

- Use Java **records** for all DTOs — immutable by design.
- Group all DTOs for one domain in a single `{Entity}Dtos.java` container class.
- Standard record names: `Create{Entity}Request`, `Update{Entity}Request`, `{Entity}Response`, `Bulk{Entity}Request`, `BulkOperationResponse`.
- Apply `@JsonInclude(JsonInclude.Include.NON_NULL)` on request records to omit null fields in serialization.
- All validation annotations go on DTO records, not on entities (entities have them as DB-level guards only).
- Validation messages must be explicit strings, not just annotation defaults.
- Mappers are **final utility classes** with a private constructor and **static methods only** — no MapStruct, no Spring beans.
- Mapper method signatures: `toResponse(Entity)`, `toEntity(Request, userId)`, `updateEntity(Entity, Request, userId)`.
- Always null-check inputs in mapper methods. When input is null, throw `IllegalArgumentException` rather than returning null — callers must not pass null to mappers.
- Never expose entity objects directly in API responses.

---

## 9. Exception Handling

### Custom Exceptions

```java
// Extend RuntimeException — no checked exceptions
public class ContactNotFoundException extends RuntimeException {

  public ContactNotFoundException(String message) {
    super(message);
  }
}

// With context fields for security-critical exceptions
public class TenantContextMismatchException extends RuntimeException {

  private final String currentTenantId;
  private final String entityTenantId;

  public TenantContextMismatchException(String message, String currentTenantId, String entityTenantId) {
    super(message);
    this.currentTenantId = currentTenantId;
    this.entityTenantId = entityTenantId;
  }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setType(URI.create("https://api.iqscaffold.com/errors/validation-error"));
    pd.setTitle("Validation Error");
    pd.setInstance(URI.create(request.getRequestURI()));
    Map<String, String> errors = new HashMap<>();
    ex
      .getBindingResult()
      .getFieldErrors()
      .forEach((e) -> errors.put(e.getField(), e.getDefaultMessage()));
    pd.setProperty("errors", errors);
    logger.warn("Validation error on {}: {}", request.getRequestURI(), errors);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(ContactNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ContactNotFoundException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setType(URI.create("https://api.iqscaffold.com/errors/not-found"));
    pd.setTitle("Not Found");
    pd.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }
}
```

### Rules

- All custom exceptions extend `RuntimeException` — no checked exceptions.
- Place custom exceptions in `shared/exception/` package.
- One `GlobalExceptionHandler` per service annotated with `@RestControllerAdvice`.
- All error responses use Spring's `ProblemDetail` (RFC 7807 Problem Details).
- Every `ProblemDetail` must set: `type` (URI), `title`, `detail`, `instance` (request URI).
- Error type URIs follow the pattern: `https://api.iqscaffold.com/errors/{error-slug}`.
- Log `WARN` for client errors (4xx), `ERROR` for server errors (5xx).
- Never catch and swallow exceptions silently — always log at minimum.
- `AccessDeniedException` → 403, `AuthenticationException` → 401, handled in the global handler.

---

## 10. Security & Authentication

### 10.1 Security Filter Chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .sessionManagement((s) -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .csrf((csrf) -> csrf.disable())
      .authorizeHttpRequests((authz) ->
        authz.requestMatchers("/actuator/**").permitAll().requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().anyRequest().authenticated()
      )
      .oauth2ResourceServer((oauth2) -> oauth2.jwt((jwt) -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter())))
      .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix(""); // No ROLE_ prefix — ever (see 10.10)
    authoritiesConverter.setAuthoritiesClaimName("authorities");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
```

Rules:

- All services are stateless — `SessionCreationPolicy.STATELESS` always.
- CSRF disabled for all services (stateless JWT-based auth).
- Actuator (`/actuator/**`) and API docs (`/swagger-ui/**`, `/api-docs/**`) are always public.
- Every controller method must have `@PreAuthorize` — no implicit authorization.
- Use `hasAnyAuthority(...)` not `hasRole(...)`.
- `JwtGrantedAuthoritiesConverter` must always set `setAuthorityPrefix("")` and `setAuthoritiesClaimName("authorities")`.
- RS256 is the only supported signing algorithm — no HS256, no profile-based switching, no `isSymmetricConfigured()` guard. See section 10.5.
- Never log JWT tokens or passwords at any log level.

---

### 10.2 Standard JWT Token Structure

This is the canonical token structure. Every service that issues or consumes tokens must conform to it exactly.

#### Access Token

```json
{
  "sub": "42",
  "iss": "iqscaffold-user-service",
  "iat": 1700000000,
  "exp": 1700000900,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "type": "access",

  "userId": 42,
  "username": "john.doe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "preferred_locale": "en",

  "tenant_id": "tenant-acme",
  "organizationId": 7,

  "authorities": ["USER", "CRM_ACCESS"],
  "permissions": []
}
```

#### Refresh Token

```json
{
  "sub": "42",
  "iss": "iqscaffold-user-service",
  "iat": 1700000000,
  "exp": 1700604800,
  "jti": "660e9500-f30c-52e5-b827-557766551111",
  "type": "refresh",
  "username": "john.doe",
  "tenant_id": "tenant-acme"
}
```

Refresh tokens carry only the minimum claims needed to issue a new access token. They must never carry authorities or profile data.

---

### 10.3 Claim Reference

All claim names are defined in `JwtClaimNames` (one copy per service in `security/JwtClaimNames.java`). Never use raw string literals to read or write claims.

| Constant           | Wire Key           | Type           | Access | Refresh | Notes                            |
| ------------------ | ------------------ | -------------- | ------ | ------- | -------------------------------- |
| `SUBJECT`          | `sub`              | `String`       | ✅     | ✅      | User ID as string (RFC 7519)     |
| `ISSUER`           | `iss`              | `String`       | ✅     | ✅      | Always `iqscaffold-user-service` |
| `ISSUED_AT`        | `iat`              | `Instant`      | ✅     | ✅      | RFC 7519                         |
| `EXPIRATION`       | `exp`              | `Instant`      | ✅     | ✅      | RFC 7519                         |
| `JWT_ID`           | `jti`              | `String`       | ✅     | ✅      | UUID, required for revocation    |
| `TYPE`             | `type`             | `String`       | ✅     | ✅      | `"access"` or `"refresh"`        |
| `USER_ID`          | `userId`           | `Long`         | ✅     | ❌      | Explicit numeric ID for frontend |
| `USERNAME`         | `username`         | `String`       | ✅     | ✅      |                                  |
| `EMAIL`            | `email`            | `String`       | ✅     | ❌      |                                  |
| `FIRST_NAME`       | `firstName`        | `String`       | ✅     | ❌      | camelCase                        |
| `LAST_NAME`        | `lastName`         | `String`       | ✅     | ❌      | camelCase                        |
| `PREFERRED_LOCALE` | `preferred_locale` | `String`       | ✅     | ❌      | snake_case (i18n convention)     |
| `TENANT_ID`        | `tenant_id`        | `String`       | ✅     | ✅      | snake_case (multi-tenancy)       |
| `ORGANIZATION_ID`  | `organizationId`   | `Long`         | ✅     | ❌      | camelCase                        |
| `AUTHORITIES`      | `authorities`      | `List<String>` | ✅     | ❌      | No `ROLE_` prefix                |
| `PERMISSIONS`      | `permissions`      | `List<String>` | ✅     | ❌      | Fine-grained permissions         |

#### Naming convention rationale

Mixed casing is intentional and must not be "fixed":

- `sub`, `iss`, `iat`, `exp`, `jti` — RFC 7519 standard names, always lowercase.
- `tenant_id`, `preferred_locale` — snake_case, matching database column names and i18n conventions.
- `userId`, `firstName`, `lastName`, `organizationId` — camelCase, matching Java field names and frontend JSON conventions.

---

### 10.4 Token Lifetimes

| Token         | Default    | Config key                                 |
| ------------- | ---------- | ------------------------------------------ |
| Access token  | 15 minutes | `iqscaffold.auth.jwt.access-token-expiry`  |
| Refresh token | 7 days     | `iqscaffold.auth.jwt.refresh-token-expiry` |

Access tokens are short-lived by design. Do not increase the default without a documented security justification.

---

### 10.5 Token Signing

**RS256 is the only supported algorithm across all environments** — local, dev, staging, and production.

HS256 is not used. Symmetric signing requires every consumer service to hold the same secret, creating a secret-distribution problem and meaning a single leaked key compromises the entire platform. RS256 eliminates this: the user-service holds the private key, every other service validates via the public JWK Set URI, and rotating the key pair does not require touching any consumer service.

#### User Service (token issuer) — all environments

```yaml
iqscaffold:
  auth:
    jwt:
      algorithm: RS256
      issuer: iqscaffold-user-service
      # Path to RSA private key (PEM). Use a mounted secret in k8s, a local file in dev.
      private-key-path: ${JWT_PRIVATE_KEY_PATH}
```

#### Consumer Services (token validators) — all environments

```yaml
iqscaffold:
  crm:
    security:
      jwt:
        jwk-set-uri: ${USER_SERVICE_URL}/.well-known/jwks.json
```

Consumer services never hold a private key or a shared secret. They only need the JWK Set URI.

#### Generating a local key pair for development

```bash
# Generate 2048-bit RSA private key
openssl genrsa -out jwt-private.pem 2048

# Extract the public key (served at /.well-known/jwks.json by the user service)
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

Point `JWT_PRIVATE_KEY_PATH` at `jwt-private.pem` in your local `.env` or IDE run config. The user service exposes the corresponding public key automatically at `/.well-known/jwks.json`.

#### Rules

- RS256 everywhere — no exceptions, no profile-based algorithm switching.
- The private key is held exclusively by the user service. No other service ever sees it.
- Private keys are never committed to source control. Use a mounted Kubernetes secret in deployed environments and a local file path in development.
- Key rotation is handled by the `JwtKeyManagementService` — add the new key to the JWK Set before retiring the old one so in-flight tokens remain valid during rollover.
- `jti` (JWT ID) is mandatory on every token — it is the revocation handle stored in Redis.
- Remove the `isSymmetricConfigured()` guard and all HS256 code paths from `JwtConfiguration` — dead code that creates confusion.

---

### 10.6 Token Revocation

Revocation uses Redis with TTL matching the token's remaining lifetime:

```java
// Blacklist a specific token by jti
var ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
redisTemplate.opsForValue().set("blacklist:token:" + jti, "true", ttl, TimeUnit.SECONDS);

// Revoke all refresh tokens for a user (logout from all devices)
redisTemplate.opsForValue().set("revoked:refresh:" + userId,
    String.valueOf(Instant.now().getEpochSecond()));
```

Redis key conventions:

- Single token blacklist: `blacklist:token:{jti}`
- User-level refresh revocation: `revoked:refresh:{userId}`
- Per-device refresh token: `refresh:token:{userId}:{deviceId}`

---

### 10.7 JwtClaimNames — One Copy Per Service

Each service has its own `JwtClaimNames` in its `security/` package. All copies must be identical. This is a known duplication accepted until a shared library is introduced.

```java
package com.iqscaffold.{service}.security;

public final class JwtClaimNames {
  private JwtClaimNames() {
    throw new UnsupportedOperationException("Utility class");
  }

  // RFC 7519 standard claims
  public static final String SUBJECT        = "sub";
  public static final String ISSUER         = "iss";
  public static final String ISSUED_AT      = "iat";
  public static final String EXPIRATION     = "exp";
  public static final String JWT_ID         = "jti";

  // Platform claims
  public static final String TYPE           = "type";
  public static final String USER_ID        = "userId";
  public static final String USERNAME       = "username";
  public static final String EMAIL          = "email";
  public static final String FIRST_NAME     = "firstName";
  public static final String LAST_NAME      = "lastName";
  public static final String PREFERRED_LOCALE = "preferred_locale";
  public static final String TENANT_ID      = "tenant_id";
  public static final String ORGANIZATION_ID = "organizationId";
  public static final String AUTHORITIES    = "authorities";
  public static final String PERMISSIONS    = "permissions";

  // Token type values
  public static final String TOKEN_TYPE_ACCESS  = "access";
  public static final String TOKEN_TYPE_REFRESH = "refresh";
}
```

Never read claims with raw string literals. Always use the constant:

```java
// Correct
String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);

String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);

// Wrong — raw strings bypass the single source of truth
String tenantId = jwt.getClaimAsString("tenant_id");

String userId = jwt.getClaimAsString("userId");
```

---

### 10.8 UserContext Record

`UserContext` is the in-memory representation of the authenticated user, populated by `JwtAuthenticationFilter` from JWT claims. Each service has its own copy in `security/UserContext.java`.

#### Standard structure (all services)

```java
public record UserContext(Long userId, String username, String email, Set<String> authorities, String tenantId, Long organizationId, String firstName, String lastName) {
  public boolean hasAuthority(String authority) {
    return authorities != null && authorities.contains(authority);
  }

  public boolean isAdmin() {
    return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
  }

  public boolean isSuperAdmin() {
    return hasAuthority("SUPER_ADMIN");
  }
}
```

Rules:

- Field order must match the constructor call in `JwtAuthenticationFilter.extractUserContext()` — keep them in sync.
- Authority strings are always bare — `"ADMIN"`, `"USER"`, `"SUPER_ADMIN"`. Never `"ROLE_ADMIN"`, never `"ROLE_USER"`. See section 10.10.
- Service-specific helper methods (e.g., `hasBillingAccess()` in billing service) are allowed as additions, not replacements.
- The user-service `UserContext` additionally carries `permissions` and `customClaims` — consumer services omit these for simplicity.
- `userId` is extracted from `sub` first, then `userId` claim as fallback (backward compatibility). Once all tokens carry `userId`, the `sub` fallback can be removed.

---

### 10.9 JwtAuthenticationFilter — Standard Implementation

Every service has a `JwtAuthenticationFilter extends OncePerRequestFilter` in its `security/` package. The implementation must follow this pattern exactly:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    var correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId != null) {
      MDC.put("correlationId", correlationId);
    }

    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth instanceof JwtAuthenticationToken jwtAuth) {
        var jwt = jwtAuth.getToken();
        var userContext = extractUserContext(jwt);
        var tenantId = resolveTenantId(request, userContext);

        if (tenantId != null) {
          TenantContext.setCurrentTenantId(tenantId);
        }
        if (userContext.userId() != null) {
          MDC.put("userId", userContext.userId().toString());
        }
        request.setAttribute("userContext", userContext);
      }
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      MDC.clear();
    }
  }

  // Tenant resolution: X-Tenant-ID header takes priority over JWT claim
  private String resolveTenantId(HttpServletRequest request, UserContext userContext) {
    var header = request.getHeader(TENANT_ID_HEADER);
    if (StringUtils.hasText(header)) return header.trim();
    return userContext.tenantId();
  }

  private UserContext extractUserContext(Jwt jwt) {
    // sub first, userId claim as fallback
    var userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
    if (userId == null) userId = extractLong(jwt.getClaim(JwtClaimNames.USER_ID));

    return new UserContext(
      userId,
      jwt.getClaim(JwtClaimNames.USERNAME),
      jwt.getClaim(JwtClaimNames.EMAIL),
      extractAuthorities(jwt.getClaim(JwtClaimNames.AUTHORITIES)),
      jwt.getClaim(JwtClaimNames.TENANT_ID),
      extractLong(jwt.getClaim(JwtClaimNames.ORGANIZATION_ID)),
      jwt.getClaim(JwtClaimNames.FIRST_NAME),
      jwt.getClaim(JwtClaimNames.LAST_NAME)
    );
  }

  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> list.stream().map(Object::toString).collect(Collectors.toSet());
      case Set<?> set -> set.stream().map(Object::toString).collect(Collectors.toSet());
      case null, default -> Collections.emptySet();
    };
  }
}
```

Rules:

- Tenant resolution order: `X-Tenant-ID` header first, JWT `tenant_id` claim second.
- Always clear `TenantContext` and `MDC` in the `finally` block — never skip this.
- Do not log tenant IDs or user IDs at `INFO` level inside the filter — use `DEBUG` only. The lead service has `INFO`-level tenant logging that must be corrected.
- Store `UserContext` as a request attribute (`"userContext"`) for downstream access.
- Use `JwtClaimNames` constants — never raw strings.

---

### 10.10 Authority Naming

**Authorities never carry a `ROLE_` prefix. This is a hard rule with no exceptions.**

Spring Security's default behaviour prepends `ROLE_` when using `hasRole(...)`. This platform disables that by setting `setAuthorityPrefix("")` on `JwtGrantedAuthoritiesConverter` (see 10.1). As a result, authorities in the JWT, in `UserContext`, in `@PreAuthorize`, and in any `authorities.contains(...)` call are always bare strings.

```
# Core
USER
ADMIN
SUPER_ADMIN
TENANT_OWNER

# CRM module
CRM_ACCESS
CRM_ADMIN
CRM_CONTACT_MANAGER

# Billing module
BILLING_ACCESS
BILLING_ADMIN
FINANCE_VIEWER

# API / integration
API_ACCESS
```

#### Correct usage

```java
// @PreAuthorize — always hasAnyAuthority, never hasRole
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")

// UserContext helper methods — bare strings only
public boolean isAdmin() {
  return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
}

public boolean isUser() {
  return hasAuthority("USER");
}

// authorities.contains — bare strings only
authorities.contains("BILLING_ADMIN")
```

#### Wrong — never do this

```java
// Wrong — ROLE_ prefix
@PreAuthorize("hasRole('ADMIN')")
authorities.contains("ROLE_ADMIN")
hasAuthority("ROLE_USER")
```

#### Existing violations to fix

The following `ROLE_` references are legacy artifacts and must be removed:

| Location                                                             | What to fix                                                       |
| -------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `UserContext.isAdmin()` in pipeline, lead, contact, billing services | Remove `authorities.contains("ROLE_ADMIN")` — keep only `"ADMIN"` |
| `UserContext.isUser()` in pipeline, lead, contact services           | Remove `authorities.contains("ROLE_USER")` — keep only `"USER"`   |
| `UserContext` tests in billing service                               | Replace `"ROLE_ADMIN"` with `"ADMIN"` in test data                |
| `JwtServiceTest` in user service                                     | Replace `"ROLE_USER"` with `"USER"` in mock claims                |
| `AuthenticationServiceTest` in user service                          | Replace `"ROLE_USER"` with `"USER"` in test assertions            |
| `Authority` seed data in user service                                | Rename `"ROLE_USER"` → `"USER"` in DB migration and initializer   |

---

## 11. Multi-Tenancy

### Architecture

Schema-per-tenant strategy: each tenant gets its own PostgreSQL schema (`tenant_{id}`). The `public` schema holds system-level data (tenant registry, etc.).

### TenantContext

```java
// Set in filter, cleared after request
TenantContext.setCurrentTenant(tenantId);
try {
  // handle request
} finally {
  TenantContext.clear();
}
```

### Tenant Resolution Order

1. `X-Tenant-ID` request header (set by the Gateway — takes priority)
2. JWT claim `tenant_id`
3. Subdomain extraction (fallback only)

### Rules

- All tenant-specific entities extend `TenantAware` — `tenant_id` is auto-injected via `@PrePersist`.
- Never manually set `tenant_id` on entities — let `TenantAware` handle it.
- `TenantContext` is a `ThreadLocal` holder — always clear it in a `finally` block.
- Liquibase migrations are split: `db/changelog/system/` for public schema, `db/changelog/tenant/` for tenant schemas.
- The `TenantLiquibaseRunner` applies tenant migrations to every tenant schema on startup.
- Cross-tenant data access is a security violation — `TenantContextMismatchException` is thrown automatically on `@PreUpdate`.

---

## 12. Database Migrations (Liquibase)

### Directory Structure

```
src/main/resources/db/changelog/
├── system/
│   ├── master.xml          # Includes all system changesets
│   └── YYYYMMDD-description.xml
└── tenant/
    ├── master.xml          # Includes all tenant changesets
    ├── demo/               # Demo/seed data
    └── YYYYMMDD-description.xml
```

### Changeset Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

  <changeSet id="20251110167000-create-contacts-table" author="iqscaffold">
    <createTable tableName="contacts">
      <column name="id" type="BIGSERIAL">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="first_name" type="VARCHAR(100)">
        <constraints nullable="false"/>
      </column>
      <column name="status" type="VARCHAR(20)" defaultValue="ACTIVE">
        <constraints nullable="false"/>
      </column>
      <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
      <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
      <column name="created_by" type="VARCHAR(100)">
        <constraints nullable="false"/>
      </column>
      <column name="updated_by" type="VARCHAR(100)">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <createIndex tableName="contacts" indexName="idx_contacts_email">
      <column name="email"/>
    </createIndex>

    <rollback>
      <dropIndex tableName="contacts" indexName="idx_contacts_email"/>
      <dropTable tableName="contacts"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```

### Rules

- Use XML format for all changesets — no SQL scripts, no YAML.
- File naming: `YYYYMMDDhhmmss-description.xml` (timestamp + kebab-case description).
- Changeset ID matches the filename without extension.
- Author is always `iqscaffold`.
- Every changeset **must** include a `<rollback>` section.
- Treat destructive changes (e.g., `dropColumn`, `dropTable`) with extreme caution. In production, destructive changes should often have empty `<rollback>` blocks, or be performed in a multi-phase deployment (deprecate -> remove usage -> drop).
- Every table must have: `id` (BIGSERIAL PK), `created_at`, `updated_at`, `created_by`, `updated_by`.
- Create indexes for: all foreign key columns, `status` columns, frequently filtered columns, and composite search columns.
- `ddl-auto` is always `none` — Hibernate never manages schema.
- Never modify an existing changeset — add a new one instead.
- Enum columns use `VARCHAR` with a `defaultValue` matching the default enum value.

---

## 13. Configuration Management

### Properties Record Pattern

```java
@ConfigurationProperties(prefix = "iqscaffold")
public record IqScaffoldProperties(String tenantIdHeader, String userServiceUrl, @NestedConfigurationProperty CrmProperties crm) {
  public record CrmProperties(@NestedConfigurationProperty SecurityProperties security) {
    public record SecurityProperties(@NestedConfigurationProperty JwtProperties jwt) {
      public record JwtProperties(
        String jwkSetUri, // RS256 only — no secretKey, no algorithm field
        String issuer
      ) {}
    }
  }
}
```

### Application YAML Structure

```yaml
server:
  port: 8080

management:
  server:
    port: 8081 # Actuator on separate port
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,liquibase

spring:
  application:
    name: iqscaffold-{service}-service
  datasource:
    url: ${iqscaffold.database.url}
    username: ${iqscaffold.database.username}
    password: ${iqscaffold.database.password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true

iqscaffold:
  tenant-id-header: X-Tenant-ID
```

### Rules

- Use `@ConfigurationProperties` records — never `@Value` for structured config.
- Scan config properties with `@ConfigurationPropertiesScan` on the main application class.
- All sensitive values (passwords, secrets) come from environment variables: `${ENV_VAR:default}`.
- Default values in YAML must be safe for local development but clearly wrong for production (e.g., `change-me-in-production`).
- Actuator runs on port `8081`, application on `8080` — always separate.
- Profile-specific overrides go in `application-{profile}.yml` (profiles: `local`, `dev`, `staging`, `production`).
- HikariCP pool: `maximum-pool-size: 10`, `minimum-idle: 2` as the baseline. Do not reduce below these values. Increase only with a measured justification — connection pools are not free.
- JDBC batch inserts/updates always enabled: `batch_size: 20`, `order_inserts: true`, `order_updates: true`.
- `show-sql: false` always — in every profile including local. Use a query profiler or P6Spy if SQL inspection is needed during development.

---

## 14. Messaging & Events (RabbitMQ)

### Exchange & Routing Key Conventions

```java
public class RabbitMQConfig {

  public static final String EXCHANGE_NAME = "iqscaffold.events"; // shared topic exchange
  public static final String DLX_EXCHANGE = "iqscaffold.dlx";
  public static final String DLQ = "iqscaffold.dlq";

  // Routing keys: {entity}.{action}
  public static final String CONTACT_CREATED_ROUTING_KEY = "contact.created";
  public static final String CONTACT_UPDATED_ROUTING_KEY = "contact.updated";
  public static final String CONTACT_DELETED_ROUTING_KEY = "contact.deleted";
}
```

### Event Publisher Pattern

```java
@Component
public class ContactEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(ContactEventPublisher.class);
  private final RabbitTemplate rabbitTemplate;

  public void publishContactCreated(final Contact contact) {
    String tenantId = TenantContext.getCurrentTenant();
    ContactEvent event = new ContactEvent("CONTACT_CREATED", contact.getId(), tenantId, metadata);
    try {
      rabbitTemplate.convertAndSend(EXCHANGE_NAME, CONTACT_CREATED_ROUTING_KEY, event);
      log.info("Published contact.created: contactId={}, tenantId={}", contact.getId(), tenantId);
    } catch (Exception e) {
      log.error("Failed to publish contact.created: contactId={}", contact.getId(), e);
      // Never rethrow — event failure must not break the main flow
    }
  }
}
```

### Queue Declaration

```java
@Bean
public Queue tenantEventsQueue() {
  return new Queue(
    "iqscaffold.{service}.tenant.events",
    true,
    false,
    false,
    Map.of(
      "x-dead-letter-exchange",
      DLX_EXCHANGE,
      "x-message-ttl",
      86400000 // 24 hours
    )
  );
}
```

### Rules

- One shared topic exchange: `iqscaffold.events`.
- Routing key pattern: `{entity}.{action}` — e.g., `contact.created`, `user.updated`, `tenant.deleted`.
- Queue naming: `iqscaffold.{service}.{entity}.{purpose}` — e.g., `iqscaffold.contact.tenant.events`.
- All queues must configure a dead-letter exchange (`x-dead-letter-exchange`) and TTL.
- Always use `Jackson2JsonMessageConverter` — never Java serialization.
- Event publishing is **fire-and-forget** — catch all exceptions, log them, never rethrow.
- Event classes are plain POJOs with: `eventType`, `entityId`, `tenantId`, `metadata` map, `eventId` (UUID), `timestamp`.
- Include `tenantId` from `TenantContext` in every published event.
- Routing key constants live in the `RabbitMQConfig` class as `public static final String`.

---

## 15. Caching Strategy

### Hibernate L2 Cache

```java
// On entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Contact { ... }

// On collection
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "User.authorities")
private Set<Authority> authorities = new HashSet<>();
```

### application.yml

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        javax:
          cache:
            provider: org.ehcache.jsr107.EhcacheCachingProvider
            uri: classpath:ehcache.xml
```

### Rules

- Apply `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)` on all entities.
- Cache regions are named by fully-qualified class name for entities.
- Configure TTL and heap size per region in `ehcache.xml`.
- Use Redis for distributed caching and session storage across instances.
- Cache names are defined as constants in `{Service}Constants.CacheNames`.
- Never cache mutable shared state without proper eviction strategy.

---

## 16. Observability

### Structured Logging with MDC

```java
// Set in filters before request processing
MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);
MDC.put(UserServiceConstants.MDC.TENANT_ID, tenantId);
MDC.put(UserServiceConstants.MDC.USER_ID, userId);
// Always clear in finally block
MDC.clear();
```

### Log Format

Use `logstash-logback-encoder` for JSON structured logs in non-local environments.

### Rules

- Every service exposes: `health`, `info`, `metrics`, `prometheus`, `liquibase` actuator endpoints.
- Actuator on port `8081`, never on the main application port.
- Propagate `X-Correlation-ID` header through all inter-service calls.
- Set MDC keys at request entry (filter level): `correlationId`, `tenant.id`, `userId`.
- Always clear MDC in a `finally` block after request processing.
- Tracing sampling probability: `1.0` (100%) in all environments. Do not reduce it without a documented, measured justification — reduced sampling hides production issues.
- Prometheus metrics endpoint enabled for scraping.
- HTTP request percentile histograms configured: `0.5`, `0.95`, `0.99`.
- MDC key constants live in `{Service}Constants.MDC` — never use raw strings.
- Header constants live in `{Service}Constants.Headers`.

---

## 17. Testing

### Test Stack

- JUnit 5 + Spring Boot Test
- Mockito for unit test mocking
- TestContainers for PostgreSQL, Redis, RabbitMQ integration tests
- ArchUnit for architecture rule enforcement
- Spring Modulith for module boundary tests

### Architecture Tests (ArchUnit)

Enforce layer dependencies with ArchUnit — controllers must not access repositories directly, services must not depend on controllers, etc.

### Rules

- All unit tests must follow the **Arrange-Act-Assert (AAA)** structure with clear blank lines separating the sections.
- Prefer `BDDMockito` (`given()` and `then()`) over classic Mockito (`when()` and `verify()`) for natural alignment with the AAA pattern.
- Unit tests: mock all dependencies with Mockito, test service logic in isolation.
- Integration tests: use TestContainers — never mock the database in integration tests.
- Be precise with Spring Boot test slicing:
  - `@WebMvcTest` for controller-only tests (mocking the service layer).
  - `@DataJpaTest` strictly for repository-only tests.
  - `@SpringBootTest` exclusively for full integration tests using Testcontainers.
- Use `@Transactional` on integration tests to roll back after each test.
- Test class naming: `{ClassName}Test` for unit tests, `{ClassName}IT` for integration tests.
- Do not write tests for: config classes, DTOs/records, exception classes, REST resources, filters, listeners (excluded from JaCoCo).
- Focus test coverage on: service implementations, validators, mappers, utility classes.
- Property-based tests are required for any method that processes unbounded input (parsers, validators, formatters, numeric conversions). Use `junit-quickcheck` or equivalent.

---

## 18. General Clean Code Rules

### Dependency Injection

Always use constructor injection with `final` fields:

```java
// Correct
public class ContactServiceImpl implements ContactService {

  private final ContactRepository contactRepository;
  private final ContactEventPublisher eventPublisher;

  public ContactServiceImpl(final ContactRepository contactRepository, final ContactEventPublisher eventPublisher) {
    this.contactRepository = contactRepository;
    this.eventPublisher = eventPublisher;
  }
}

// Wrong — never do this
@Autowired
private ContactRepository contactRepository;
```

### Constants Classes

```java
public final class UserServiceConstants {

  private UserServiceConstants() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static final class Headers {

    private Headers() {
      throw new UnsupportedOperationException("Utility class");
    }

    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_TENANT_ID = "X-Tenant-ID";
  }
}
```

### Utility Classes

```java
public final class ContactMapper {

  private ContactMapper() {
    // Utility class — not instantiable
  }
  // static methods only
}
```

### General Rules

- Returning collections (`List`, `Set`, `Map`) from service boundaries or domain entities must be immutable. Use `List.copyOf()`, `Set.copyOf()`, or `Collections.unmodifiableList()` to prevent accidental state mutation by callers.
- Constructor injection only — no field injection, no setter injection.
- All injected fields are `final`.
- Utility classes: `final` class, private constructor that throws `UnsupportedOperationException`.
- Constants classes: nested static inner classes grouped by concern (Headers, MDC, SecurityEvents, etc.).
- Use `var` for local variables where the type is obvious from the right-hand side.
- Use `String.formatted(...)` instead of `String.format(...)`.
- Use `Objects.equals()` and `Objects.hash()` in `equals()`/`hashCode()`.
- `toString()` must never expose passwords, tokens, or other sensitive data.
- Use `Optional` as the return type for all service methods that may return no result — never return `null` from a service method.
- Never use `@SuppressWarnings` without a comment explaining the exact reason. An uncommented `@SuppressWarnings` is a code smell and must be flagged in review.
- All public methods and classes must have Javadoc — at minimum a one-line summary.
- Entity Javadoc should document: purpose, key fields, relationships, security considerations, and usage examples.
- Avoid magic numbers and strings — define them as named constants.
- Enums over boolean flags when a field can have more than two meaningful states.
- Default enum values must be set at field declaration: `private ContactStatus status = ContactStatus.ACTIVE;`.
- Use `HashSet` for `@ManyToMany` collections — never `ArrayList`.
- Provide domain utility methods on entities where appropriate (`isActive()`, `hasAuthority()`, `getFullName()`).
- Application main class annotations: `@SpringBootApplication`, `@EnableJpaAuditing`, `@EnableTransactionManagement`, `@ConfigurationPropertiesScan`.
- JVM timezone: always `-Duser.timezone=UTC`.

---

## 19. KISS — Keep It Simple, Stupid

The goal is working, readable code — not clever code.

- Solve the problem in front of you. Do not build for hypothetical future requirements.
- If a method needs a comment to explain what it does, it is probably too complex — simplify or extract.
- Prefer a straightforward `if/else` over a stream pipeline when the logic is not naturally collection-oriented.
- One method, one responsibility. If you find yourself writing "and" in a method name, split it.
- Avoid deep nesting (more than 2–3 levels). Use early returns to flatten logic:

```java
// Avoid
public void process(Contact contact) {
  if (contact != null) {
    if (contact.getEmail() != null) {
      if (!contact.getEmail().isBlank()) {
        // actual logic
      }
    }
  }
}

// Prefer
public void process(Contact contact) {
  if (contact == null) return;
  if (contact.getEmail() == null || contact.getEmail().isBlank()) return;
  // actual logic
}
```

- Do not over-engineer abstractions. Add an interface when there is a real reason (multiple implementations, testability) — not by default for every class.
- Configuration classes, filters, and infrastructure wiring are allowed to be verbose — clarity matters more than brevity there.
- When in doubt between two approaches, pick the one a new team member can understand without asking questions.

---

## 20. DRY — Don't Repeat Yourself

Duplication is a maintenance liability. Every piece of knowledge should have a single authoritative home.

### Where DRY applies

- **Validation rules** — define constraints once on the DTO record, not again in the service and again in the entity.
- **Error messages** — define in `i18n/messages.properties`, not as inline strings scattered across handlers.
- **Routing keys and queue names** — defined once as `public static final String` in `RabbitMQConfig`, referenced everywhere else.
- **JWT claim names** — defined once in `JwtClaimNames`, used in token generation and token parsing.
- **HTTP header names** — defined once in `{Service}Constants.Headers`.
- **MDC keys** — defined once in `{Service}Constants.MDC`.
- **Audit fields** (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) — inherited from `TenantAware` or added once per entity, never copy-pasted.
- **Tenant ID injection** — handled once in `TenantAware.@PrePersist`, never repeated in service code.
- **Security filter chain** — one `SecurityConfig` per service, not duplicated across controllers.

### How to avoid duplication

- Extract repeated logic into a private method before it appears a third time.
- Shared mapper logic (e.g., mapping audit fields) belongs in a base mapper method, not copy-pasted into every `toEntity`.
- If two services need the same utility (e.g., a date formatter, a slug generator), it belongs in a shared library — not duplicated.
- Do not copy-paste Liquibase changeset blocks — each changeset is unique and versioned; reuse is done via `include` in `master.xml`.

### Where DRY does NOT mean forced abstraction

- Two pieces of code that look similar but represent different domain concepts should stay separate. Accidental similarity is not duplication.
- Do not create a shared base class just to avoid two lines of identical boilerplate — the coupling cost often outweighs the benefit.

---

## 21. Constant Usage

Every string or number used more than once, or that carries domain meaning, must be a named constant.

### Where constants live

| What                                 | Where                                              |
| ------------------------------------ | -------------------------------------------------- |
| JWT claim keys                       | `JwtClaimNames` (shared across services)           |
| HTTP headers                         | `{Service}Constants.Headers`                       |
| MDC keys                             | `{Service}Constants.MDC`                           |
| Security event types                 | `{Service}Constants.SecurityEvents`                |
| Cache region names                   | `{Service}Constants.CacheNames`                    |
| RabbitMQ exchange/queue/routing keys | `RabbitMQConfig` (as `public static final String`) |
| Tenant/schema defaults               | `{Service}Constants.Tenant`                        |
| Filter ordering                      | `{Service}Constants.FilterOrder`                   |
| Regex patterns                       | `{Service}Constants.Patterns`                      |

### Rules

- No raw string literals for header names, claim names, queue names, routing keys, or MDC keys anywhere outside their defining constant class.
- No magic numbers — max field lengths, score bounds, pool sizes, TTLs must all be named constants or externalized to config.
- Constants classes use nested static inner classes grouped by concern — never one flat class with 50 unrelated constants.
- Every inner constants class has a private constructor throwing `UnsupportedOperationException`.
- Prefer constants over enums when the value is used as a plain string (e.g., header names, claim keys). Use enums when the value participates in type-safe logic.

```java
// Correct — constant for a header name used in multiple filters
MDC.put(UserServiceConstants.MDC.TENANT_ID, tenantId);
rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.CONTACT_CREATED_ROUTING_KEY, event);

// Wrong — raw strings
MDC.put("tenant_id", tenantId);
rabbitTemplate.convertAndSend("iqscaffold.events", "contact.created", event);
```

---

## 22. Java 21+ Language Features

The platform targets **Java 21**. Modern language features are not optional style preferences — they are the expected baseline. Reviewers should flag code that ignores these patterns in favour of older equivalents.

---

### `var` — Local Variable Type Inference

Use `var` for local variables when the type is obvious from the right-hand side. It reduces noise without sacrificing readability.

```java
// Correct
var users = userManagementService.getAllUsers(pageable, currentUser);

var tenantClaim = jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID);

var sb = new StringBuilder();

// Wrong — type is already clear, explicit declaration adds nothing
List<UserDto> users = userManagementService.getAllUsers(pageable, currentUser);
```

Do NOT use `var` when:

- The right-hand side is a method call whose return type is not obvious from the name alone.
- The variable is a field (not allowed by the language anyway).
- It would hide a meaningful type distinction (e.g., `var x = someFactory.create()` where the factory returns different subtypes).

---

### Pattern Matching `instanceof`

Always use pattern matching `instanceof` — never cast separately after a type check.

```java
// Correct
if (authentication instanceof JwtAuthenticationToken token) {
  var subject = token.getToken().getSubject();
}

// Negated guard pattern — use for early return
if (!(authentication instanceof JwtAuthenticationToken token)) {
  return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}

// Inline conditional
return value instanceof String s ? s : null;

// Wrong — old two-step cast
if (authentication instanceof JwtAuthenticationToken) {
  JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
  ...
}
```

This applies everywhere: filters, services, event listeners, exception handlers.

---

### Switch Expressions

Use switch expressions (not switch statements) for exhaustive branching on enums or sealed types. Prefer arrow-case syntax.

```java
// Correct — switch expression with arrow cases
String label = switch (status) {
  case ACTIVE   -> "Active";
  case INACTIVE -> "Inactive";
  case ARCHIVED -> "Archived";
};

// Correct — with yield for multi-line branches
HttpStatus httpStatus = switch (errorCode) {
  case NOT_FOUND -> HttpStatus.NOT_FOUND;
  case FORBIDDEN -> HttpStatus.FORBIDDEN;
  default -> {
    log.warn("Unmapped error code: {}", errorCode);
    yield HttpStatus.INTERNAL_SERVER_ERROR;
  }
};

// Wrong — old switch statement with fall-through
switch (status) {
  case ACTIVE:
    label = "Active";
    break;
  ...
}
```

Rules:

- Switch expressions must be exhaustive — always include a `default` branch unless the type is a sealed class or enum where all cases are covered.
- Never use fall-through (`case X: case Y:` without a break) — use comma-separated cases instead: `case X, Y ->`.
- Prefer switch expressions over long `if/else if` chains on the same variable.

---

### Text Blocks

Use text blocks for any multi-line string: SQL queries, JSON templates, OpenAPI descriptions, email bodies, log messages with structure.

````java
// Correct — OpenAPI description
.description("""
    CRM contact management service for IQ Scaffold platform.

    ## Authentication
    All endpoints require JWT authentication:
    ```
    Authorization: Bearer <token>
    ```
    """)

// Correct — JPQL query
@Query("""
    SELECT c FROM Contact c
    WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
       OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :term, '%'))
    """)
Page<Contact> searchContacts(@Param("term") String term, Pageable pageable);

// Correct — structured log / message
var nextSteps = """
    1. Verify your email at %s
    2. Log in with username: %s
    3. Invite your team members
    """.formatted(email, username);

// Wrong — string concatenation for multi-line content
String desc = "CRM contact management service.\n" +
              "## Authentication\n" +
              "All endpoints require JWT.\n";
````

Rules:

- The closing `"""` goes on its own line to control trailing newline.
- Combine with `.formatted(...)` for interpolation — never concatenate into a text block.
- Use text blocks for any string that spans more than one logical line.

---

### `String.formatted()`

Use `String.formatted(...)` as an instance method instead of the static `String.format(...)`.

```java
// Correct
return "Tenant context mismatch [current=%s, entity=%s]".formatted(currentTenantId, entityTenantId);
return "User{id=%d, username='%s'}".formatted(id, username);

// Wrong
return String.format("Tenant context mismatch [current=%s, entity=%s]", currentTenantId, entityTenantId);
```

---

### Records for DTOs

All DTOs are Java records — immutable, concise, with compiler-generated `equals`, `hashCode`, and `toString`.

```java
// Correct
public record CreateContactRequest(@NotBlank String firstName, @Email String email, ContactStatus status) {}

// Wrong — class-based DTO
public class CreateContactRequest {

  private String firstName;
  // getters, setters, equals, hashCode, toString...
}
```

Records are the only acceptable DTO form. Do not use Lombok `@Data` or manual POJO classes for DTOs.

---

### Sealed Classes and Interfaces

Use sealed types when a domain concept has a fixed, known set of subtypes — particularly for result types, event variants, or error hierarchies.

```java
// Sealed interface for operation results
public sealed interface OperationResult<T>
    permits OperationResult.Success, OperationResult.Failure {

  record Success<T>(T value) implements OperationResult<T> {}
  record Failure<T>(String reason, Throwable cause) implements OperationResult<T> {}
}

// Caller uses pattern matching switch — exhaustive, no default needed
return switch (result) {
  case OperationResult.Success<Contact> s -> ResponseEntity.ok(ContactMapper.toResponse(s.value()));
  case OperationResult.Failure<Contact> f -> ResponseEntity.internalServerError().build();
};
```

Use sealed types when:

- A type hierarchy is intentionally closed (all subtypes are known at compile time).
- You want the compiler to enforce exhaustive handling in switch expressions.
- Modelling discriminated unions (success/failure, event types).

Do not use sealed types just to restrict extension — use `final` for that.

---

### Enhanced `instanceof` with Numeric Patterns

When extracting numeric values from untyped maps (e.g., RabbitMQ event metadata deserialized as `Object`), use pattern matching to handle both `Integer` and `Long` safely:

```java
// Correct — handles JSON deserializing numbers as Integer or Long
Long leadId = switch (convertedFromLeadIdObj) {
  case Integer i -> i.longValue();
  case Long l -> l;
  case null -> null;
  default -> Long.parseLong(String.valueOf(convertedFromLeadIdObj));
};

// Also acceptable for simple two-case scenarios
Long userId = (userIdObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(userIdObj));

// Wrong — unsafe cast
Long leadId = (Long) convertedFromLeadIdObj;
```

---

### Virtual Threads (Project Loom)

Spring Boot 3.2+ on Java 21 supports virtual threads. Enable them in all services — every platform service is I/O-bound by nature (database, HTTP, messaging):

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Rules:

- Virtual threads must be enabled in every service — this is not optional.
- Do not use `synchronized` blocks in code that runs on virtual threads — use `ReentrantLock` instead (synchronized pins the carrier thread).
- Do not pool virtual threads — they are cheap to create; thread pools for virtual threads defeat the purpose.

---

### What to Avoid (Pre-Java 21 Patterns)

| Avoid                                                 | Use instead                                      |
| ----------------------------------------------------- | ------------------------------------------------ |
| `if (x instanceof Foo) { Foo f = (Foo) x; }`          | `if (x instanceof Foo f)`                        |
| `String.format(...)`                                  | `"...".formatted(...)`                           |
| `"line1\n" + "line2\n"`                               | Text block `"""..."""`                           |
| Class-based DTOs with Lombok `@Data`                  | `record`                                         |
| `switch` statement with `break`                       | Switch expression with `->`                      |
| Explicit type on obvious local variables              | `var`                                            |
| Raw `(Long)` cast on untyped map values               | Pattern matching switch or `instanceof Number n` |
| `Thread.sleep` / blocking in platform threads for I/O | Virtual threads + non-blocking where possible    |

---

## 23. Internationalization (i18n)

i18n is the mechanism for serving user-facing text in the user's preferred language. The infrastructure is wired in every service. The user-service is the primary consumer (email content, validation messages). All other services must follow the same pattern when they produce user-facing text.

---

### 23.1 Message Files

All user-facing strings live in `.properties` files under `src/main/resources/i18n/`. Never hardcode user-facing text in Java source.

```
src/main/resources/i18n/
├── messages.properties        ← default (English, authoritative)
├── messages_en.properties     ← explicit English override
├── messages_es.properties     ← Spanish
└── messages_fr.properties     ← French
```

`messages.properties` is the authoritative fallback. Every key that exists in a locale-specific file must also exist in `messages.properties`. A missing key in the default file is a bug.

#### Key naming convention

Keys follow a dot-separated hierarchy: `{domain}.{sub-domain}.{action-or-field}`

```properties
# Authentication
auth.login.failed=Invalid username or password
auth.account.locked=Account is locked
auth.token.invalid=Invalid or expired token

# User management
user.not.found=User not found
user.email.already.exists=Email address already in use

# Validation (positional args: {0} = field name, {1} = bound)
validation.required={0} is required
validation.min.length={0} must be at least {1} characters
validation.email.invalid=Invalid email format

# Email content
email.verification.subject=Verify your IQ Scaffold account
email.verification.greeting=Hello {0}
email.password.reset.subject=Reset your IQ Scaffold password
```

Rules:

- Keys are lowercase, dot-separated — no camelCase, no underscores.
- Positional arguments use `{0}`, `{1}` (MessageFormat syntax).
- Group related keys under the same prefix — all email keys under `email.`, all auth keys under `auth.`.
- Every key added to any locale file must be added to `messages.properties` first.
- Never delete a key without checking all locale files and all call sites.

---

### 23.2 Configuration

```yaml
spring:
  messages:
    basename: i18n/messages # loads i18n/messages*.properties from classpath
    encoding: UTF-8
    cache-duration: PT1H

iqscaffold:
  i18n:
    supported-locales: [en, es, fr]
    default-locale: en
    message-basename: i18n/messages
    message-cache-duration: PT1H
    fallback-to-system-locale: false
    use-code-as-default-message: true
```

`use-code-as-default-message: true` means a missing translation returns the key itself (e.g., `"email.verification.subject"`) instead of throwing. This prevents runtime exceptions from missing keys but missing keys must still be treated as bugs and fixed.

`fallback-to-system-locale: false` — the platform controls locale explicitly; the JVM system locale is irrelevant.

---

### 23.3 IqScaffoldProperties — i18n Record

```java
@ConfigurationProperties(prefix = "iqscaffold")
public record IqScaffoldProperties(@NestedConfigurationProperty I18n i18n) {
  public record I18n(
    List<String> supportedLocales,
    String defaultLocale,
    String messageBasename,
    Duration messageCacheDuration,
    boolean fallbackToSystemLocale,
    boolean useCodeAsDefaultMessage
  ) {
    public Locale getDefaultLocaleObject() {
      return Locale.forLanguageTag(defaultLocale);
    }

    public List<Locale> getSupportedLocaleObjects() {
      return supportedLocales.stream().map(Locale::forLanguageTag).toList();
    }
  }
}
```

---

### 23.4 I18nConfig

Each service has an `I18nConfig` in its `config/` package that wires `MessageSource` and `LocaleResolver`:

```java
@Configuration
public class I18nConfig {

  private final IqScaffoldProperties properties;

  public I18nConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public MessageSource messageSource() {
    var i18n = properties.i18n();
    var source = new ResourceBundleMessageSource();
    source.setBasename(i18n.messageBasename());
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setUseCodeAsDefaultMessage(i18n.useCodeAsDefaultMessage());
    source.setFallbackToSystemLocale(i18n.fallbackToSystemLocale());
    source.setCacheSeconds((int) i18n.messageCacheDuration().toSeconds());
    return source;
  }

  @Bean
  public LocaleResolver localeResolver() {
    var i18n = properties.i18n();
    var resolver = new UserPreferenceLocaleResolver();
    resolver.setDefaultLocale(i18n.getDefaultLocaleObject());
    resolver.setSupportedLocales(i18n.getSupportedLocaleObjects());
    return resolver;
  }
}
```

---

### 23.5 Locale Resolution Order

Per request, locale is resolved in this priority order:

1. `X-User-Locale` header (forwarded by the gateway from user preferences)
2. `Accept-Language` header (standard HTTP negotiation)
3. Default locale (`en`)

`UserPreferenceLocaleResolver` implements this:

```java
public class UserPreferenceLocaleResolver extends AcceptHeaderLocaleResolver {

  private static final String USER_LOCALE_HEADER = "X-User-Locale";

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    String header = request.getHeader(USER_LOCALE_HEADER);
    if (StringUtils.hasText(header)) {
      Locale locale = Locale.forLanguageTag(header);
      if (isSupportedLocale(locale)) return locale;
    }
    return super.resolveLocale(request); // falls back to Accept-Language
  }

  @Override
  public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    // Read-only — locale changes go through user preferences, not this resolver
    throw new UnsupportedOperationException("Cannot change locale via UserPreferenceLocaleResolver — update user preferences instead");
  }
}
```

Rules:

- `setLocale` always throws — locale is not changed via the resolver. To change a user's locale, update `User.preferredLocale` in the database.
- Unsupported locales fall through to `Accept-Language` — never reject a request because of an unknown locale header.

---

### 23.6 MessageService

Each service has a `MessageService` in its `shared/` package. It is the only way to retrieve translated strings — never call `MessageSource` directly from business code.

```java
@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(final MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  // Uses locale from current request context (set by LocaleResolver)
  public String getMessage(String code) {
    return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
  }

  // With positional arguments
  public String getMessage(String code, Object[] args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }

  // Explicit locale (use when sending emails — locale comes from user preference, not request)
  public String getMessage(String code, Locale locale) {
    return messageSource.getMessage(code, null, locale);
  }

  public String getMessage(String code, Object[] args, Locale locale) {
    return messageSource.getMessage(code, args, locale);
  }

  // Resolves locale from User.preferredLocale, falls back to request context, then English
  public String getMessage(String code, User user) {
    return messageSource.getMessage(code, null, getUserLocale(user));
  }

  public String getMessage(String code, Object[] args, User user) {
    return messageSource.getMessage(code, args, getUserLocale(user));
  }

  // Locale priority: User.preferredLocale → LocaleContextHolder → Locale.ENGLISH
  public Locale getUserLocale(User user) {
    if (user != null && StringUtils.hasText(user.getPreferredLocale())) {
      return Locale.forLanguageTag(user.getPreferredLocale());
    }
    var contextLocale = LocaleContextHolder.getLocale();
    return contextLocale != null ? contextLocale : Locale.ENGLISH;
  }
}
```

Rules:

- Always inject `MessageService` — never inject `MessageSource` directly in business code.
- Use `getMessage(code, user)` when sending emails or notifications — the user's stored preference takes priority over the current request locale.
- Use `getMessage(code)` for request-scoped messages (validation errors, API responses) — locale comes from the request automatically.
- Never pass `null` as a message code — it will throw. Use a constant.

---

### 23.7 The `preferred_locale` JWT Claim

`preferred_locale` is carried in the access token (see section 10.3). Consumer services that need to send a message to a user (e.g., billing sends an invoice email) read this claim and pass it directly to `MessageService` — no database lookup required:

```java
// In a consumer service — get locale from JWT claim, not from DB
String preferredLocale = jwt.getClaim(JwtClaimNames.PREFERRED_LOCALE);

Locale locale = StringUtils.hasText(preferredLocale) ? Locale.forLanguageTag(preferredLocale) : Locale.ENGLISH;

String subject = messageService.getMessage("email.invoice.subject", locale);
```

---

### 23.8 Usage Example — Email Service

```java
// Correct — all text from MessageService, locale from user preference
helper.setSubject(messageService.getMessage("email.verification.subject", userLocale));
context.setVariable("greeting",
    messageService.getMessage("email.verification.greeting",
        new Object[]{ user.getFirstName() }, userLocale));
context.setVariable("body",
    messageService.getMessage("email.verification.body", userLocale));

// Wrong — hardcoded text
helper.setSubject("Verify your IQ Scaffold account");
context.setVariable("greeting", "Hello " + user.getFirstName());
```

---

### 23.9 Rules Summary

- All user-facing text lives in `i18n/messages*.properties` — no inline strings in Java.
- `messages.properties` (English) is the authoritative fallback — every key must exist there.
- Key format: `{domain}.{sub-domain}.{action}` — lowercase, dot-separated.
- `MessageService` is the only access point — never call `MessageSource` directly from business code.
- Use `getMessage(code, user)` for async/email flows; `getMessage(code)` for request-scoped flows.
- `preferred_locale` in the JWT eliminates the need for a DB lookup in consumer services.
- Supported locales are configured in `iqscaffold.i18n.supported-locales` — adding a new language requires a new `messages_{lang}.properties` file and a config update.
- Missing translations are bugs — `use-code-as-default-message: true` prevents crashes but does not excuse missing keys.

---

## 24. Inter-Service Communication & Resilience

### HTTP Clients

Use modern HTTP clients for synchronous inter-service calls instead of legacy options:

```java
// Prefer Spring's RestClient (blocking, Spring Boot 3.2+) or WebClient (reactive)
@Bean
public RestClient {service}RestClient(RestClient.Builder builder) {
    return builder.baseUrl("http://iqscaffold-{service}-service").build();
}
```

Never use `RestTemplate` or `OpenFeign` for new integrations.

### Context Propagation

Outgoing HTTP requests must propagate contextual headers explicitly to maintain observability and multi-tenancy limits:

- `X-Correlation-ID`: Ensure tracing matches across services.
- `X-Tenant-ID`: Ensure downstream operations apply to the appropriate tenant data.

Configure `RestClient` or `WebClient` with default interceptors/filters to extract these from `MDC` (or Reactor `Context`) and inject them into request headers.

### Circuit Breaking and Retry

Synchronous cascading failures must be prevented. Apply Resilience4j on all inter-service HTTP calls:

```java
@CircuitBreaker(name = "{service}Client", fallbackMethod = "fallbackFor{Operation}")
@Retry(name = "{service}Client")
public Response callDownstreamService(...) {
    // ...
}
```

Rules:

- Non-critical read operations should define sensible fallbacks (e.g., empty lists or cached state).
- Write operations typically cannot have fallbacks and should throw a custom exception representing a downstream failure.
- Configure thresholds, timeouts, and half-open states in `application-production.yml` to prevent aggressive retries during system degradation.

---

## 25. Reactive Programming (Gateway Service)

The API Gateway is built on Spring Cloud Gateway and WebFlux (Project Reactor). This implies strict reactive programming rules.

### Rule of Avoidance: Blocking Calls

**Blocking the event loop is strictly forbidden.** A single blocked thread in WebFlux can cause total service degradation.

```java
// WRONG: Blocking calls are forbidden in WebFlux
// RestTemplate, standard JDBC, Thread.sleep(), InputStream holding thread...
```

Use `WebClient` for API calls from the gateway, and non-blocking alternatives (like `ReactiveRedisTemplate`) for caching or rate-limiting data access.

### Context Propagation in Reactor

In a reactive application, `ThreadLocal` (e.g., `MDC`, `SecurityContextHolder`, `TenantContext`) cannot be used reliably because multiple stages of a single processing pipeline may execute on different threads.

```java
// Use Reactor Context for context propagation
return chain.filter(exchange)
    .contextWrite(ctx -> ctx.put("tenantId", resolvedTenantId));

// Extracting from Context later
return Mono.deferContextual(ctx -> {
    String tenantId = ctx.get("tenantId");
    // ...
});
```

Always rely on Reactor `Context` to thread contextual values down the reactive chain. Logging correlation IDs requires explicit Micrometer Tracing context configuration that bridges to SLF4J MDC using the `io.micrometer:context-propagation` library.

---

## 26. Consistency & Event Publishing (The Dual-Write Problem)

Publishing an event to RabbitMQ directly from within a database transaction is a major architectural anti-pattern. If the message broker throws an exception, you might catch it, but if the database transaction subsequently rolls back (e.g., due to a constraint violation), the event has already been broadcasted. Downstream services will react to state that does not exist.

### Strict Guidelines

- **Never publish directly from a `@Transactional` business method.**
- **Use `@TransactionalEventListener`:** For non-critical events, publish only after the transaction commits successfully.
- **Use the Outbox Pattern:** For mission-critical state changes, save the event to an outbox table within the same database transaction. A separate process/job should read from the outbox table and publish to RabbitMQ.

```java
// Example: Safe Event Publishing
@Service
public class ContactServiceImpl implements ContactService {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public Contact createContact(Contact contact) {
    Contact saved = repository.save(contact);
    // Publish internal Spring application event
    applicationEventPublisher.publishEvent(new ContactCreatedSpringEvent(saved));
    return saved;
  }
}

@Component
public class ContactEventRabbitPublisher {

  // Listen for the internal event, trigger only AFTER DB COMMIT
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleContactCreated(ContactCreatedSpringEvent event) {
    try {
      rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.CONTACT_CREATED_ROUTING_KEY, event.getPayload());
    } catch (Exception e) {
      log.error("Failed to publish event after commit", e);
    }
  }
}
```

---

## 27. Idempotency

In a distributed system, network failures and retries are guaranteed. Both HTTP requests (via Resilience4j retries) and RabbitMQ messages (via NACKs or broker redeliveries) will be processed at-least-once.

### Strict Guidelines

- **All State-Mutating APIs Must Be Idempotent:** `PUT`, `PATCH`, and `DELETE` endpoints must be naturally idempotent. For `POST` endpoints creating resources, require an `X-Idempotency-Key` header or use a natural unique composite key constraint in the database.
- **All Event Listeners Must Be Idempotent:** Every `@RabbitListener` must check if it has already processed the specific `eventId` (by checking an `idempotency_keys` table or using database constraints) before mutating state.

---

## 28. Database Isolation & Autonomy

Microservices must be completely decoupled at the data layer to ensure autonomous deployments and scaling.

### Strict Guidelines

- **Database-Per-Service:** No service may ever read or write to the database or schema of another service.
- **No Shared Tables:** If two services need the same reference data, they must either query the owning service via REST or replicate the data locally by listening to RabbitMQ events.
- **Foreign Keys:** You can never have a database foreign key pointing to a table owned by another microservice. Relational integrity across services must be handled at the application level via eventual consistency.

---

## 29. Schema & API Evolution

Deployments in a microservice ecosystem happen independently. You cannot guarantee that the client (Consumer) and the server (Provider) will be deployed at the exact same moment.

### Strict Guidelines

- **Additive Changes Only:** Never remove, rename, or change the type of an API field or database column in a single deployment.
- **Multi-Phase Rollouts for Breaking Changes:**
  1. Add the new field/column. Write to both, read from old.
  2. Implement the new behavior in clients.
  3. Change the server to read from the new field.
  4. (Months later) Remove the old field/column.
- **Never Change Event Schemas:** Events in RabbitMQ might stay in queues or Dead Letter Exchanges (DLXs) for days. If you change the shape of an event that you publish, the consumer might crash when trying to deserialize older events. Add new fields instead of modifying existing ones, or publish to an entirely new routing key (e.g., `contact.created.v2`).

---

## 30. Kubernetes Lifecycle & Graceful Shutdown

Services must respect the orchestration lifecycle to achieve zero-downtime deployments.

### Strict Guidelines

- **Graceful Shutdown:** Must be enabled in Spring Boot (`server.shutdown: graceful`). When Kubernetes sends a `SIGTERM`, the service must stop accepting new HTTP connections and stop pulling new RabbitMQ messages, but finish processing currently active requests (allow up to 30 seconds).
- **Probes:**
  - `livenessProbe` must check if the application is fundamentally deadlocked (e.g., Spring Boot Actuator `/actuator/health/liveness`).
  - `readinessProbe` must check if the service can handle traffic, meaning its database connections and essential downstream dependencies are reachable (`/actuator/health/readiness`).

---

## 31. CI/CD Pipeline & Deployment Configuration

All platform microservices use Drone CI pipelines for end-to-end CI/CD and Helm for Kubernetes deployments.

### CI/CD Pipeline Flow

- **VerifyCode:** Runs unit and integration tests (with TestContainers), Jacoco code coverage, and static analysis (SonarQube, PMD, SpotBugs) on all branches.
- **PublishArtifacts:** Publishes Maven artifacts to the Nexus repository for specific branches and tags.
- **PublishDockerImage:** Packages the JAR and builds a Docker container image securely published to the registry.
- **Deploy / Promote:** Automated Helm deployments for WIP and feature branches; manual promotion steps for test, staging, and production environments.

### Helm Chart Configuration & Infrastructure

Microservices rely on common infrastructure deployed via the `iqscaffold-infra` Helm chart (PostgreSQL, Redis, RabbitMQ, MinIO). Connection details to these services must not be hardcoded in application code.

### Strict Security Guidelines for `helm --set`

- **Never hardcode secrets in `values.yaml`:** Passwords, JWT secret keys, OAuth client IDs/secrets, and SMTP credentials must be set to empty strings or safe placeholder values (like `"iqscaffold_dev_password"` ONLY for local dev overrides) in the `values.yaml` files.
- **Dynamic Secret Injection via `--set`:** All sensitive configuration is managed in CI/CD secrets (e.g., Drone CI `from_secret`) and injected at deploy time using `--set`.

```bash
# Example from Drone Pipeline
helm upgrade --install --atomic --wait --timeout 5m ${DRONE_REPO_NAME} ./ \
  --values ./values.yaml \
  --values ./values-dev.yaml \
  --set image.tag=${DRONE_BRANCH} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set infraServices.objectstorage.accessKey=${INFRA_S3_ACCESS_KEY} \
  --set infraServices.objectstorage.secretKey=${INFRA_S3_SECRET_KEY} \
  --set config.jwt.secretKey=${JWT_SECRET_KEY}
```

This guarantees that source code repositories never contain actionable credentials and that separate environments (dev, test, staging, production) can securely provision their own isolation.

---

## 32. Java Import Rules, Final Variables & Strict Code Hygiene

These rules are enforced by Checkstyle (`maven-project-common-checkstyle.xml`) and the project `.editorconfig`. Violations fail the build — treat them as compilation errors.

---

### 32.1 Import Ordering

Imports must follow this exact order, with a blank line between each group:

```
1. Static imports          (e.g., import static org.assertj.core.api.Assertions.assertThat;)
2. Standard Java packages  (java.**, javax.**, jakarta.**)
3. Special platform imports (tech.**, expert.**, android.**, dev.**, build.**)
4. Third-party imports     (everything else — Spring, Hibernate, Jackson, etc.)
```

Checkstyle rule: `CustomImportOrder` with `STATIC###STANDARD_JAVA_PACKAGE###SPECIAL_IMPORTS###THIRD_PARTY_PACKAGE`.

IntelliJ layout (`.editorconfig`):

```
ij_java_imports_layout = $*, |, jakarta.**, java.**, javax.**, |, *
```

#### Rules

- No wildcard imports — ever. `AvoidStarImport` is enforced. Set `class_count_to_use_import_on_demand = 999` and `names_count_to_use_import_on_demand = 999` in IntelliJ to prevent auto-collapsing to `*`.
- No unused imports — `UnusedImports` check is active.
- Imports must not be line-wrapped — `NoLineWrap` is enforced on `IMPORT` and `STATIC_IMPORT` tokens.
- Alphabetical ordering within each group is required.
- One blank line between each import group — no blank lines within a group.
- Static imports come first, before all other imports.

#### Correct example

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.contact.ContactRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

#### Wrong — never do this

```java
import static org.assertj.core.api.Assertions.assertThat; // static not first

import com.iqscaffold.contactservice.contact.Contact;
import java.time.LocalDateTime;
import java.util.*; // wildcard — forbidden
import java.util.List;
import org.springframework.stereotype.*; // wildcard — forbidden // java.** mixed with third-party
```

---

### 32.2 Final Variables

#### Injected fields — always `final`

All constructor-injected fields must be `final`. This is already required by section 18, but it is also a Checkstyle-enforced rule.

```java
// Correct
public class ContactServiceImpl implements ContactService {

  private final ContactRepository contactRepository;
  private final ContactEventPublisher eventPublisher;

  public ContactServiceImpl(final ContactRepository contactRepository, final ContactEventPublisher eventPublisher) {
    this.contactRepository = contactRepository;
    this.eventPublisher = eventPublisher;
  }
}

// Wrong — mutable field, field injection
@Autowired
private ContactRepository contactRepository;
```

#### Constructor, catch, and for-each parameters — `final`

The `FinalParameters` Checkstyle rule enforces `final` on:

- Constructor parameters (`CTOR_DEF`)
- For-each clause variables (`FOR_EACH_CLAUSE`)
- Catch block parameters (`LITERAL_CATCH`)

Primitive types are exempt (`ignorePrimitiveTypes = true`). Unnamed parameters (Java 21 `_`) are exempt.

```java
// Correct
public ContactServiceImpl(final ContactRepository repo, final ContactEventPublisher publisher) { ... }

for (final Contact contact : contacts) { ... }

try {
  // ...
} catch (final ContactNotFoundException ex) {
  log.warn("Contact not found", ex);
}

// Wrong — missing final on constructor/catch/for-each parameters
public ContactServiceImpl(ContactRepository repo) { ... }
for (Contact contact : contacts) { ... }
catch (ContactNotFoundException ex) { ... }
```

> Note: `final` on regular method parameters is not enforced by Checkstyle here (only `CTOR_DEF`, `FOR_EACH_CLAUSE`, `LITERAL_CATCH` are in scope), but it is encouraged for clarity on non-trivial methods.

#### Local variables — declare close to use

`VariableDeclarationUsageDistance` is set to `allowedDistance = 20`. Declare local variables as close as possible to their first use — do not hoist declarations to the top of a method.

```java
// Correct — declared at point of use
public Contact createContact(final Contact contact) {
  Contact saved = contactRepository.save(contact);
  eventPublisher.publishContactCreated(saved);
  return saved;
}

// Wrong — unnecessary hoisting
public Contact createContact(final Contact contact) {
  Contact saved;
  // ... unrelated code ...
  saved = contactRepository.save(contact);
  return saved;
}
```

#### Static constants — `public static final`

All constants must be `public static final` (or package-private `static final` when intentionally scoped). The modifier order is enforced by `ModifierOrder`:

```
public / protected / private → abstract → static → final → transient → volatile → synchronized → native → strictfp
```

```java
// Correct
public static final String EXCHANGE_NAME = "iqscaffold.events";

private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

// Wrong — wrong modifier order
public static final String EXCHANGE_NAME = "iqscaffold.events";

static final String EXCHANGE_NAME = "iqscaffold.events";
```

---

### 32.3 One Declaration Per Line

`MultipleVariableDeclarations` is enforced — never declare multiple variables on one line.

```java
// Correct
String firstName = request.firstName();

String lastName = request.lastName();

// Wrong
String firstName = request.firstName(),
  lastName = request.lastName();
```

`OneStatementPerLine` is also enforced — one statement per line, always.

```java
// Correct
contact.setFirstName(request.firstName());
contact.setLastName(request.lastName());

// Wrong
contact.setFirstName(request.firstName()); contact.setLastName(request.lastName());
```

---

### 32.4 No Finalizers

`NoFinalizer` is enforced. Never override `Object.finalize()`. Use `try-with-resources` or explicit `close()` calls for resource cleanup.

```java
// Wrong — never do this
@Override
protected void finalize() throws Throwable {
  cleanup();
  super.finalize();
}

// Correct — use try-with-resources
try (var stream = Files.newInputStream(path)) {
  // use stream
}
```

---

### 32.5 Switch Statements

- `MissingSwitchDefault` is enforced — every `switch` statement must have a `default` branch.
- `FallThrough` is enforced — fall-through between `case` blocks is forbidden unless the case body is empty (grouping).

```java
// Correct — default present, no fall-through
return switch (status) {
  case ACTIVE   -> processActive(contact);
  case INACTIVE -> processInactive(contact);
  default       -> throw new IllegalArgumentException("Unknown status: " + status);
};

// Wrong — missing default
return switch (status) {
  case ACTIVE   -> processActive(contact);
  case INACTIVE -> processInactive(contact);
  // no default — Checkstyle violation
};
```

---

### 32.6 Array Style

`ArrayTypeStyle` is enforced — use Java-style array declarations, not C-style.

```java
// Correct
String[] names;

byte[] data;

// Wrong — C-style
String names[];

byte data[];
```

---

### 32.7 Long Literal Suffix

`UpperEll` is enforced — always use uppercase `L` for long literals, never lowercase `l` (visually ambiguous with `1`).

```java
// Correct
long timeout = 86400L;

long maxSize = 1_000_000L;

// Wrong
long timeout = 86400l; // 'l' looks like '1'
```

---

### 32.8 Abbreviations in Names

`AbbreviationAsWordInName` is enforced with `allowedAbbreviationLength = 5` and `ignoreFinal = false`.

- Abbreviations longer than 5 consecutive uppercase letters are forbidden in class, method, variable, and parameter names.
- This applies to `final` fields too — `ignoreFinal = false`.

```java
// Correct — abbreviation ≤ 5 chars
String userId;

String jwtToken;

String httpUrl;

class RabbitMQConfig {} // "AMQP" is 4 chars — OK

class JwtClaimNames {} // "JWT" is 3 chars — OK

// Wrong — abbreviation > 5 chars
String userIDENTIFIER;

class HTTPSConnectionManager {} // "HTTPS" is 5 — borderline; "HTTPS" itself is fine, but "HTTPSConnection" has 5 consecutive caps — check with Checkstyle
```

---

### 32.9 Overloaded Methods — Declaration Order

`OverloadMethodsDeclarationOrder` is enforced. All overloads of the same method must be declared consecutively — do not interleave overloads with unrelated methods.

```java
// Correct — overloads together
public Optional<Contact> getContactById(Long id) { ... }
public Optional<Contact> getContactById(Long id, String tenantId) { ... }

public Page<Contact> getAllContacts(Pageable pageable) { ... }

// Wrong — overloads separated by unrelated method
public Optional<Contact> getContactById(Long id) { ... }
public Page<Contact> getAllContacts(Pageable pageable) { ... }
public Optional<Contact> getContactById(Long id, String tenantId) { ... }  // violation
```

---

### 32.10 Empty Blocks & Braces

- `NeedBraces` is enforced on `do`, `else`, `for`, `if`, `while` — always use braces, even for single-line bodies.
- `EmptyBlock` is enforced — empty `try`, `finally`, `if`, `else`, `switch` blocks must contain at least a comment.
- `EmptyCatchBlock` allows only catch blocks where the variable is named `expected` (test idiom).

```java
// Correct — braces always
if (contact == null) {
  return;
}

// Wrong — no braces
if (contact == null) return;

// Correct — empty catch with explanation
try {
  cache.evict(key);
} catch (final CacheException expected) {
  // eviction failure is non-critical; cache will expire naturally
}

// Wrong — silent swallow
try {
  cache.evict(key);
} catch (CacheException e) {}
```

---

### 32.11 Checkstyle Suppression Policy

Suppressions are allowed only via `checkstyle-suppressions.xml` or `@SuppressWarnings` with a mandatory comment. Inline `// CHECKSTYLE:OFF` blocks are forbidden except in generated code.

```java
// Correct — suppression with explanation
@SuppressWarnings("checkstyle:MagicNumber") // port 8080 is a well-known default, not a magic number
private static final int DEFAULT_PORT = 8080;

// Wrong — unexplained suppression
@SuppressWarnings("checkstyle:MagicNumber")
private static final int DEFAULT_PORT = 8080;
```

---

### 32.12 Summary Checklist

| Rule                                                       | Enforced by                                   | Severity         |
| ---------------------------------------------------------- | --------------------------------------------- | ---------------- |
| No wildcard imports                                        | Checkstyle `AvoidStarImport`                  | Build failure    |
| No unused imports                                          | Checkstyle `UnusedImports`                    | Build failure    |
| Import group order (static → java → special → third-party) | Checkstyle `CustomImportOrder`                | Build failure    |
| Alphabetical order within import groups                    | Checkstyle `CustomImportOrder`                | Build failure    |
| `final` on constructor / catch / for-each parameters       | Checkstyle `FinalParameters`                  | Build failure    |
| `final` on all injected fields                             | Checkstyle + code review                      | Build failure    |
| One variable declaration per line                          | Checkstyle `MultipleVariableDeclarations`     | Build failure    |
| One statement per line                                     | Checkstyle `OneStatementPerLine`              | Build failure    |
| No `finalize()` override                                   | Checkstyle `NoFinalizer`                      | Build failure    |
| `default` in every `switch`                                | Checkstyle `MissingSwitchDefault`             | Build failure    |
| No fall-through in `switch`                                | Checkstyle `FallThrough`                      | Build failure    |
| Java-style array declarations                              | Checkstyle `ArrayTypeStyle`                   | Build failure    |
| Uppercase `L` for long literals                            | Checkstyle `UpperEll`                         | Build failure    |
| Abbreviations ≤ 5 chars in names                           | Checkstyle `AbbreviationAsWordInName`         | Build failure    |
| Overloads declared consecutively                           | Checkstyle `OverloadMethodsDeclarationOrder`  | Build failure    |
| Braces on all control flow blocks                          | Checkstyle `NeedBraces`                       | Build failure    |
| No silent empty catch blocks                               | Checkstyle `EmptyCatchBlock`                  | Build failure    |
| Correct modifier order                                     | Checkstyle `ModifierOrder`                    | Build failure    |
| Variable declared close to use (≤ 20 lines)                | Checkstyle `VariableDeclarationUsageDistance` | Build failure    |
| No unexplained `@SuppressWarnings`                         | Code review policy                            | Review rejection |
