# Authentication Architecture Alignment

## Overview

The Bookstore Service has been aligned with the Gripday platform's centralized authentication architecture as a **downstream microservice**. This document explains how the service implements the architecture patterns defined in `docs/auth/authentication-architecture.md`.

## Architecture Role

**Service Type:** Downstream Microservice (Business Service)  
**Authentication Role:** JWT Token Consumer  
**Port:** 8081 (configurable)

### Position in Architecture

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│ Client  │────────>│   Gateway   │────────>│ User Service │         │  Bookstore   │
│         │         │  (Port 8080)│         │  (Port 8080) │         │ (Port 8081)  │
└─────────┘         └─────────────┘         └──────────────┘         └──────────────┘
                           │                        │                        │
                           │                        │                        │
                           │    Validates JWT       │    Validates JWT       │
                           │    using RSA256        │    using RSA256        │
                           │         ↓              │         ↓              │
                           │    JWK Endpoint        │    JWK Endpoint        │
                           └───────────────────────>│<───────────────────────┘
                                                    │
                                    /.well-known/jwks.json
                                    (Public Keys)
```

## Implementation Details

### 1. JWT Validation (RSA256 via JWK)

**Configuration:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080
          jwk-set-uri: http://localhost:8080/.well-known/jwks.json
```

**Key Features:**

- ✅ Uses RSA256 asymmetric encryption
- ✅ Fetches public keys from User Service's JWK endpoint
- ✅ Automatic key rotation support (90-day rotation, 7-day grace period)
- ✅ No shared secrets required
- ✅ Spring Security caches keys (5 minutes default)
- ✅ Validates token signature, issuer, and expiry

**Implementation:**

```java
@Bean
public JwtDecoder jwtDecoder() {
  return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
}
```

### 2. User Context Extraction

**JWT Claims Structure (from User Service):**

```json
{
  "iss": "gripday-user-service",
  "sub": "1",
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER", "ADMIN"],
  "permissions": ["read:profile", "update:profile"],
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "default",
  "department": "Engineering",
  "organizationId": "org-123"
}
```

**Extracted UserContext:**

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

**Extraction Process:**

1. `JwtAuthenticationFilter` intercepts authenticated requests
2. Extracts JWT from Spring Security context
3. `UserContextExtractor` parses JWT claims
4. UserContext stored in request attribute
5. Controllers access via `@RequestAttribute("userContext")`

### 3. Authority Extraction

**Supported Claim Formats:**

- `roles` - Gripday User Service standard
- `authorities` - Alternative format
- `realm_access.roles` - Keycloak compatibility

**Custom Converter:**

```java
@Bean
public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
  return (jwt) -> {
    // Extract from multiple claim sources
    // Convert to GrantedAuthority without ROLE_ prefix
    // Allows using hasAnyAuthority("ADMIN")
  };
}
```

### 4. Security Configuration

**Endpoint Security Levels:**

| Endpoint Pattern                        | Security Level | Required Authority  |
| --------------------------------------- | -------------- | ------------------- |
| `GET /api/v1/bookstore/books`           | Public         | None                |
| `GET /api/v1/bookstore/books/search/**` | Public         | None                |
| `GET /api/v1/bookstore/books/{id}`      | Authenticated  | Valid JWT           |
| `GET /api/v1/bookstore/inventory/**`    | Authenticated  | Valid JWT           |
| `POST /api/v1/bookstore/books`          | Admin          | ADMIN or SUPERADMIN |
| `PUT /api/v1/bookstore/books/**`        | Admin          | ADMIN or SUPERADMIN |
| `DELETE /api/v1/bookstore/books/**`     | Admin          | ADMIN or SUPERADMIN |
| `PUT /api/v1/bookstore/inventory/**`    | Admin          | ADMIN or SUPERADMIN |

**Method-Level Security:**

```java
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
public BookDto createBook(CreateBookRequest request, UserContext userContext) {
  // Business logic
}
```

### 5. Correlation ID Propagation

**Flow:**

1. Client sends request (optionally with `X-Correlation-ID` header)
2. Gateway generates or forwards correlation ID
3. Bookstore receives and logs with correlation ID
4. All logs include correlation ID via MDC
5. Response includes `X-Correlation-ID` header

**Implementation:**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        var correlationId = generateCorrelationId(request);
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        // ... extract user context
        MDC.put("userId", String.valueOf(userContext.userId()));
        MDC.put("username", userContext.username());
    }
}
```

### 6. Audit Logging

**Security Events Logged:**

- Admin operations (create, update, delete)
- Inventory changes
- Unauthorized access attempts (handled by Spring Security)

**Audit Log Format:**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "operation": "CREATE",
  "resourceType": "BOOK",
  "resourceId": "123",
  "userId": 1,
  "username": "johndoe",
  "userRoles": ["ADMIN"],
  "department": "Engineering",
  "organizationId": "org-123",
  "correlationId": "abc123-def456-ghi789"
}
```

## Alignment with Architecture Document

### ✅ Implemented Features

| Feature                 | Architecture Requirement                  | Implementation Status |
| ----------------------- | ----------------------------------------- | --------------------- |
| RSA256 Validation       | Use RSA256 via JWK endpoint               | ✅ Implemented        |
| JWK Endpoint            | Fetch keys from `/.well-known/jwks.json`  | ✅ Implemented        |
| Key Rotation Support    | Support 90-day rotation with grace period | ✅ Automatic          |
| User Context Extraction | Extract all standard claims               | ✅ Implemented        |
| Authority Extraction    | Support multiple claim formats            | ✅ Implemented        |
| Method-Level Security   | Use @PreAuthorize annotations             | ✅ Implemented        |
| Correlation ID          | Propagate and log correlation IDs         | ✅ Implemented        |
| Audit Logging           | Log security events                       | ✅ Implemented        |
| Stateless Sessions      | No server-side sessions                   | ✅ Implemented        |
| CORS Configuration      | Environment-specific CORS                 | ✅ Implemented        |
| Security Headers        | HSTS, frame options, etc.                 | ✅ Implemented        |

### 📋 Configuration Alignment

**Architecture Standard:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://user-service:8080
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

**Bookstore Implementation:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8080}
          jwk-set-uri: ${JWT_JWK_SET_URI:http://localhost:8080/.well-known/jwks.json}
```

✅ **Aligned** - Uses environment variables for flexibility

### 🔄 Authentication Flow

**Architecture Pattern:**

```
Client → Gateway (validates JWT) → Bookstore (validates JWT)
              ↓                           ↓
         JWK Endpoint                JWK Endpoint
         (public keys)               (public keys)
```

**Bookstore Implementation:**

1. ✅ Receives request with JWT in Authorization header
2. ✅ Validates JWT using RSA256 public key from JWK endpoint
3. ✅ Extracts user context from JWT claims
4. ✅ Stores user context in request attribute
5. ✅ Enforces authorization via @PreAuthorize
6. ✅ Executes business logic with user context
7. ✅ Logs operations with correlation ID

## Differences from Architecture

### Minor Deviations

1. **Port Number**
   - Architecture: Port 8081 for downstream services
   - Bookstore: Port 8080 (configurable via environment)
   - **Impact:** None - port is configurable

2. **Issuer URI**
   - Architecture: `http://user-service:8080`
   - Bookstore: `http://localhost:8080` (default)
   - **Impact:** None - uses environment variables for deployment

3. **Additional Features**
   - Bookstore includes Redis caching (not in base architecture)
   - Bookstore includes comprehensive metrics
   - **Impact:** Positive - enhanced functionality

### No Breaking Deviations

All core authentication patterns match the architecture:

- ✅ RSA256 validation via JWK endpoint
- ✅ User context extraction
- ✅ Authority-based authorization
- ✅ Method-level security
- ✅ Correlation ID propagation
- ✅ Audit logging

## Testing Authentication

### 1. Verify JWK Endpoint Connectivity

```bash
# Test from bookstore service
curl http://localhost:8080/.well-known/jwks.json | jq
```

### 2. Test Public Endpoints (No Auth)

```bash
# Should work without JWT
curl http://localhost:8081/api/v1/bookstore/books
```

### 3. Test Authenticated Endpoints

```bash
# Get JWT from User Service
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' \
  | jq -r '.accessToken')

# Access authenticated endpoint
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/v1/bookstore/books/1
```

### 4. Test Admin Endpoints

```bash
# Get admin JWT
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"AdminPass123!"}' \
  | jq -r '.accessToken')

# Create book (admin only)
curl -X POST http://localhost:8081/api/v1/bookstore/books \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Book",
    "author": "Test Author",
    "isbn": "978-1234567890",
    "price": 29.99,
    "categoryId": 1,
    "initialQuantity": 10
  }'
```

### 5. Test Key Rotation

```bash
# Trigger key rotation on User Service (admin)
curl -X POST http://localhost:8080/api/v1/admin/keys/rotate \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Verify bookstore still works with old token (grace period)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/v1/bookstore/books/1

# Get new token with new key
NEW_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' \
  | jq -r '.accessToken')

# Verify new token works
curl -H "Authorization: Bearer $NEW_TOKEN" \
     http://localhost:8081/api/v1/bookstore/books/1
```

## Monitoring and Observability

### Metrics to Monitor

```prometheus
# JWT validation
http_server_requests_seconds_count{uri="/api/v1/bookstore/**",status="401"}
http_server_requests_seconds_count{uri="/api/v1/bookstore/**",status="403"}

# Admin operations
bookstore_books_created_total
bookstore_books_updated_total
bookstore_books_deleted_total
bookstore_inventory_updated_total

# Correlation ID propagation
http_server_requests_seconds_count{correlationId!=""}
```

### Log Patterns

```
# Successful authentication
2024-01-15 10:30:00 [http-nio-8081-exec-1] INFO [abc123] [1:johndoe] BookService - Creating book with title: Test Book

# Unauthorized access (Spring Security)
2024-01-15 10:30:00 [http-nio-8081-exec-1] WARN [abc123] [] SecurityFilterChain - Access Denied

# Admin operation audit
2024-01-15 10:30:00 [http-nio-8081-exec-1] INFO [abc123] [1:johndoe] AUDIT - Admin operation performed: CREATE BOOK
```

## Best Practices Followed

### 1. Security

- ✅ Stateless authentication (no sessions)
- ✅ RSA256 asymmetric encryption
- ✅ No shared secrets
- ✅ Automatic key rotation support
- ✅ Method-level security with @PreAuthorize
- ✅ Security headers (HSTS, frame options)

### 2. Observability

- ✅ Correlation ID propagation
- ✅ MDC logging context
- ✅ Comprehensive audit logging
- ✅ Metrics for all operations
- ✅ Structured logging

### 3. Architecture

- ✅ Three-tier architecture (presentation, domain, infrastructure)
- ✅ Dependency injection
- ✅ Configuration externalization
- ✅ Environment-specific settings

### 4. Code Quality

- ✅ Comprehensive documentation
- ✅ Java 21 modern features (records, var, text blocks)
- ✅ Clean code principles
- ✅ Single responsibility

## Migration from Previous Implementation

### Changes Made

1. **JWK Endpoint Configuration**
   - Before: `issuer-uri` only
   - After: Both `issuer-uri` and `jwk-set-uri`

2. **JWT Decoder**
   - Before: `NimbusJwtDecoder.withIssuerLocation(issuerUri)`
   - After: `NimbusJwtDecoder.withJwkSetUri(jwkSetUri)`

3. **Authority Handling**
   - Before: `hasAnyRole("ADMIN")`
   - After: `hasAnyAuthority("ADMIN")`

4. **Service Authorization**
   - Before: Manual `userContext.isAdmin()` checks
   - After: `@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")`

### Backward Compatibility

✅ **Fully Compatible** - No breaking changes to:

- API endpoints
- Request/response formats
- JWT token structure
- User context structure
- Database schema

## References

- [Authentication Architecture](../../docs/auth/authentication-architecture.md)
- [Authentication Refactoring](AUTHENTICATION-REFACTORING.md)
- [Authentication Quick Reference](AUTHENTICATION-QUICK-REFERENCE.md)
- [Spring Boot Best Practices](../.kiro/steering/spring-boot-microservices-best-practices.md)

## Summary

The Bookstore Service is **fully aligned** with the Gripday platform's authentication architecture as a downstream microservice. It correctly implements:

- ✅ RSA256 JWT validation via JWK endpoint
- ✅ Automatic key rotation support
- ✅ User context extraction and propagation
- ✅ Authority-based authorization
- ✅ Method-level security
- ✅ Correlation ID tracking
- ✅ Comprehensive audit logging
- ✅ Stateless authentication

The service serves as a **reference implementation** for other downstream microservices in the Gripday platform.
