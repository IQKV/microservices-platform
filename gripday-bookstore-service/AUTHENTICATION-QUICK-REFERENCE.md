# Authentication Quick Reference - Bookstore Service

## Architecture Overview

The Bookstore Service is a **downstream microservice** in the Gripday platform that validates JWT tokens issued by the User Service using **RSA256 asymmetric encryption** via the **JWK endpoint**.

```
Client → Gateway → Bookstore Service
           ↓            ↓
      JWK Endpoint  JWK Endpoint
      (public keys) (public keys)
```

## Configuration

### Application Properties

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080
          jwk-set-uri: http://localhost:8080/.well-known/jwks.json
```

### Environment Variables

```bash
JWT_ISSUER_URI=http://user-service:8080
JWT_JWK_SET_URI=http://user-service:8080/.well-known/jwks.json
```

## Key Changes

### Use `hasAnyAuthority` instead of `hasAnyRole`

**Before:**

```java
.requestMatchers(HttpMethod.POST, "/api/v1/bookstore/books")
    .hasAnyRole("ADMIN", "SUPERADMIN")
```

**After:**

```java
.requestMatchers(HttpMethod.POST, "/api/v1/bookstore/books")
    .hasAnyAuthority("ADMIN", "SUPERADMIN")
```

### Use `@PreAuthorize` on Service Methods

**Before:**

```java
public BookDto createBook(CreateBookRequest request, UserContext userContext) {
  if (!userContext.isAdmin()) {
    throw new UnauthorizedOperationException("create book", "ADMIN or SUPERADMIN");
  }
  // business logic
}
```

**After:**

```java
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
public BookDto createBook(CreateBookRequest request, UserContext userContext) {
  // business logic only
}
```

## JWT Token Format (from User Service)

The User Service issues JWT tokens with the following structure:

```json
{
  "iss": "gripday-user-service",
  "sub": "1",
  "iat": 1642248000,
  "exp": 1642248900,
  "jti": "unique-token-id",
  "type": "access",
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

### Supported Authority Claim Formats

The service supports multiple claim formats for maximum compatibility:

```json
{
  "roles": ["ADMIN", "USER"], // Gripday standard
  // OR
  "authorities": ["ADMIN", "USER"], // Alternative
  // OR
  "realm_access": {
    // Keycloak
    "roles": ["ADMIN", "USER"]
  }
}
```

**Important:** Use exact authority names without "ROLE\_" prefix:

- ✅ `"ADMIN"`
- ✅ `"SUPERADMIN"`
- ✅ `"USER"`
- ❌ `"ROLE_ADMIN"` (not used)

## Testing with Different Users

### Admin User

```java
@WithMockUser(authorities = { "ADMIN" })
@Test
void testCreateBook() {
  // test admin operation
}
```

### Super Admin User

```java
@WithMockUser(authorities = { "SUPERADMIN" })
@Test
void testDeleteBook() {
  // test super admin operation
}
```

### Regular User (No Admin)

```java
@WithMockUser(authorities = { "USER" })
@Test
void testCreateBook_shouldFail() {
  // expect 403 Forbidden
}
```

## Common Authorization Expressions

```java
// Single authority
@PreAuthorize("hasAuthority('ADMIN')")

// Multiple authorities (OR)
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")

// Multiple authorities (AND)
@PreAuthorize("hasAuthority('ADMIN') and hasAuthority('MANAGER')")

// Complex expression
@PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('USER') and #userContext.userId == #id)")

// Check user context
@PreAuthorize("@userContextService.canAccessResource(#userContext, #resourceId)")
```

## Error Responses

### 401 Unauthorized

No JWT token or invalid token:

```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### 403 Forbidden

Valid token but insufficient authorities:

```json
{
  "error": "Forbidden",
  "message": "Access Denied"
}
```

## Debugging Tips

### Enable Security Debug Logging

```yaml
logging:
  level:
    org.springframework.security: DEBUG
```

### Check Extracted Authorities

Look for log entries like:

```
Authorities: [ADMIN, USER]
```

### Verify JWT Claims

Use jwt.io to decode your token and verify claims structure.

## Architecture Alignment Checklist

- [x] Use JWK endpoint for JWT validation (RSA256)
- [x] Replace `hasAnyRole()` with `hasAnyAuthority()`
- [x] Add `@PreAuthorize` annotations to service methods
- [x] Remove manual `userContext.isAdmin()` checks
- [x] Add custom `JwtAuthenticationConverter`
- [x] Support multiple authority claim formats
- [x] Implement correlation ID propagation
- [x] Add comprehensive audit logging
- [x] Configure issuer-uri and jwk-set-uri
- [x] Document architecture alignment
- [ ] Test with User Service JWT tokens
- [ ] Test key rotation scenario
- [ ] Update integration tests
- [ ] Deploy with environment-specific configuration

## Testing with User Service

### 1. Start User Service

```bash
cd gripday-user-service
./mvnw spring-boot:run
```

### 2. Register and Login

```bash
# Register user
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Verify email (check logs for token)
curl http://localhost:8080/api/v1/auth/email/verify?token=<token>

# Login
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' \
  | jq -r '.accessToken')
```

### 3. Test Bookstore Endpoints

```bash
# Public endpoint (no auth)
curl http://localhost:8081/api/v1/bookstore/books

# Authenticated endpoint
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/v1/bookstore/books/1

# Admin endpoint (requires ADMIN role)
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

## References

- [Authentication Architecture](../../docs/auth/authentication-architecture.md)
- [Architecture Alignment](AUTHENTICATION-ARCHITECTURE-ALIGNMENT.md)
- [Refactoring Details](AUTHENTICATION-REFACTORING.md)
