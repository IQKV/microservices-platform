# Gripday Microservices Authentication Architecture

## Overview

The Gripday platform implements a **centralized authentication** pattern with **JWT-based token propagation** across microservices. The User Service acts as the authentication authority, while the Gateway Service enforces authentication and propagates user context to downstream services.

**Key Architecture Principles:**

- **Single Source of Truth**: User Service is the sole authentication authority
- **Asymmetric Encryption**: RSA256 everywhere for enhanced security
- **Dynamic Key Distribution**: JWK endpoint for automatic public key fetching
- **Zero-Downtime Key Rotation**: Automated rotation with grace period support
- **Stateless Authentication**: JWT tokens carry all necessary user context

## Architecture Components

### 1. User Service (Authentication Authority)

**Port:** 8080  
**Role:** Centralized authentication and user management

#### Responsibilities

- User registration with email verification
- User authentication (login/logout)
- JWT token generation (access + refresh tokens)
- Token validation and refresh
- User lifecycle management (CRUD)
- Password management (reset/change)
- Role-based access control (RBAC)
- Multi-tenant user isolation

#### JWT Token Generation

**Algorithm:** RSA256 (asymmetric encryption)

- **Access Token:** 15 minutes expiry
- **Refresh Token:** 7 days expiry (30 days with "remember me")

**Token Structure:**

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

**JWT Header (includes key ID for rotation):**

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "unique-key-id"
}
```

#### Key Features

- **RSA256 Key Pair:** Managed by JwtKeyManagementService (2048-bit)
- **Automated Key Rotation:** Every 90 days with 7-day grace period
- **JWK Endpoint:** `/.well-known/jwks.json` for public key distribution
- **Token Blacklisting:** Redis-backed invalidation for logout
- **Refresh Token Revocation:** User-wide revocation on password change
- **Token Cleanup:** Scheduled daily cleanup of expired tokens
- **Account Lockout:** 5 failed attempts = 15-minute lockout
- **Email Verification:** Required before login
- **Security Audit Logging:** All auth events logged

#### Configuration

```yaml
gripday:
  auth:
    jwt:
      access-token-expiry: PT15M # 15 minutes
      refresh-token-expiry: P7D # 7 days
      issuer: gripday-user-service
# Key rotation is automatic via JwtKeyManagementService
# Cleanup runs daily at 2 AM via TokenCleanupService
```

#### Public Endpoints

- `GET /.well-known/jwks.json` - JWK Set for public key distribution (no auth required)
- `POST /api/v1/admin/keys/rotate` - Manual key rotation (SUPER_ADMIN only)
- `POST /api/v1/admin/keys/cleanup` - Manual token cleanup (SUPER_ADMIN only)

---

### 2. Gateway Service (BFF + Authentication Enforcer)

**Port:** 8080  
**Role:** API Gateway with JWT validation and context propagation

#### Responsibilities

- Route requests to downstream microservices
- Validate JWT tokens for protected endpoints using RSA256
- Extract and propagate user context via headers
- Extract and propagate tenant context
- Enforce rate limiting per tenant
- Circuit breaker patterns for resilience
- CORS handling for frontend applications

#### JWT Validation

**Algorithm:** RSA256 (asymmetric encryption)

- Uses OAuth2 Resource Server with JWK endpoint
- Fetches public keys from User Service dynamically
- Validates token signature, issuer, expiry
- Automatically refreshes keys on rotation
- No shared secrets required

**Configuration:**

```yaml
gripday:
  gateway:
    security:
      jwt:
        issuer: gripday-user-service
        algorithm: RS256
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
      authentication:
        enabled: true
        enable-user-context-propagation: true
      public-paths:
        - /.well-known/jwks.json # JWK endpoint
        - /api/v1/auth/login
        - /api/v1/auth/signup
        # ... other public paths
```

#### User Context Propagation

Gateway extracts JWT claims and propagates via HTTP headers:

```http
X-Correlation-ID: abc123-def456-ghi789
X-User-ID: 1
X-Username: johndoe
X-User-Roles: USER,ADMIN
X-Tenant-ID: default
```

#### Public Paths (No Authentication)

```yaml
public-paths:
  # JWK endpoint for public key distribution
  - /.well-known/jwks.json

  # Authentication endpoints
  - /api/v1/auth/login
  - /api/v1/auth/signup
  - /api/v1/auth/refresh
  - /api/v1/auth/validate

  # Email verification
  - /api/v1/auth/email/verify
  - /api/v1/auth/email/resend

  # Password management
  - /api/v1/password/forgot
  - /api/v1/password/reset

  # Public bookstore endpoints
  - /api/v1/bookstore/books
  - /api/v1/bookstore/books/**

  # Health checks
  - /actuator/health
  - /actuator/info
```

#### Tenant Context Extraction

Priority order:

1. `X-Tenant-ID` header
2. JWT `tenantId` claim
3. Subdomain extraction (e.g., `tenant1.api.gripday.com`)

---

### 3. Downstream Services (e.g., Bookstore Service)

**Port:** 8081  
**Role:** Business microservice consuming authentication

#### Responsibilities

- Validate JWT tokens from Gateway
- Extract user context from JWT claims
- Enforce method-level security with `@PreAuthorize`
- Implement business logic with user context

#### JWT Validation

**Algorithm:** RSA256 (public key validation via JWK endpoint)

- Fetches public keys from User Service's JWK endpoint
- Validates token signature using RSA public key
- Automatically handles key rotation (grace period support)
- Extracts user context from JWT claims
- No shared secrets or manual key distribution needed

#### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
      .oauth2ResourceServer((oauth2) -> oauth2.jwt())
      .authorizeHttpRequests((authz) ->
        authz
          // Public endpoints
          .requestMatchers(GET, "/api/v1/bookstore/books")
          .permitAll()
          // Authenticated endpoints
          .requestMatchers(GET, "/api/v1/bookstore/books/*")
          .authenticated()
          // Admin-only endpoints
          .requestMatchers(POST, "/api/v1/bookstore/books")
          .hasAnyRole("ADMIN", "SUPERADMIN")
          .requestMatchers(PUT, "/api/v1/bookstore/books/**")
          .hasAnyRole("ADMIN", "SUPERADMIN")
          .requestMatchers(DELETE, "/api/v1/bookstore/books/**")
          .hasAnyRole("ADMIN", "SUPERADMIN")
          .anyRequest()
          .authenticated()
      )
      .build();
  }
}
```

#### Application Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://user-service:8080
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

#### User Context Extraction

```java
@Component
public class UserContextExtractor {

  public UserContext extractFromJwt(Jwt jwt) {
    var claims = jwt.getClaims();

    return new UserContext(
      extractUserId(claims),
      extractUsername(claims),
      extractEmail(claims),
      extractRoles(claims),
      extractPermissions(claims),
      extractDepartment(claims),
      extractOrganizationId(claims),
      extractCustomClaims(claims)
    );
  }
}
```

---

## Authentication Flow

### 1. User Registration & Login Flow

```
┌─────────┐         ┌─────────┐         ┌──────────────┐
│ Client  │         │ Gateway │         │ User Service │
└────┬────┘         └────┬────┘         └──────┬───────┘
     │                   │                      │
     │ POST /auth/signup │                      │
     ├──────────────────>│                      │
     │                   │ Forward              │
     │                   ├─────────────────────>│
     │                   │                      │
     │                   │   201 Created        │
     │                   │<─────────────────────┤
     │   201 Created     │                      │
     │<──────────────────┤                      │
     │                   │                      │
     │ GET /email/verify?token=xxx              │
     ├──────────────────>│                      │
     │                   │ Forward              │
     │                   ├─────────────────────>│
     │                   │   200 OK             │
     │                   │<─────────────────────┤
     │   200 OK          │                      │
     │<──────────────────┤                      │
     │                   │                      │
     │ POST /auth/login  │                      │
     ├──────────────────>│                      │
     │                   │ Forward              │
     │                   ├─────────────────────>│
     │                   │                      │
     │                   │   JWT Tokens         │
     │                   │<─────────────────────┤
     │   JWT Tokens      │                      │
     │<──────────────────┤                      │
     │                   │                      │
```

### 2. Authenticated Request Flow

```
┌─────────┐         ┌─────────┐         ┌──────────────┐         ┌──────────────┐
│ Client  │         │ Gateway │         │ User Service │         │   Bookstore  │
└────┬────┘         └────┬────┘         └──────┬───────┘         └──────┬───────┘
     │                   │                      │                        │
     │ GET /bookstore/books                     │                        │
     │ Authorization: Bearer <JWT>              │                        │
     ├──────────────────>│                      │                        │
     │                   │                      │                        │
     │                   │ 1. Validate JWT      │                        │
     │                   │    (HMAC-SHA256)     │                        │
     │                   │                      │                        │
     │                   │ 2. Extract User Context                       │
     │                   │                      │                        │
     │                   │ 3. Add Headers:      │                        │
     │                   │    X-User-ID: 1      │                        │
     │                   │    X-Username: john  │                        │
     │                   │    X-User-Roles: USER│                        │
     │                   │    X-Tenant-ID: default                       │
     │                   │                      │                        │
     │                   │ 4. Forward Request   │                        │
     │                   ├────────────────────────────────────────────>│
     │                   │                      │                        │
     │                   │                      │   5. Validate JWT      │
     │                   │                      │      (RSA256)          │
     │                   │                      │                        │
     │                   │                      │   6. Extract Context   │
     │                   │                      │                        │
     │                   │                      │   7. Process Request   │
     │                   │                      │                        │
     │                   │                      │   200 OK + Data        │
     │                   │<────────────────────────────────────────────┤
     │   200 OK + Data   │                      │                        │
     │<──────────────────┤                      │                        │
     │                   │                      │                        │
```

### 3. Token Refresh Flow

```
┌─────────┐         ┌─────────┐         ┌──────────────┐
│ Client  │         │ Gateway │         │ User Service │
└────┬────┘         └────┬────┘         └──────┬───────┘
     │                   │                      │
     │ POST /auth/refresh                       │
     │ { refreshToken: "..." }                  │
     ├──────────────────>│                      │
     │                   │ Forward              │
     │                   ├─────────────────────>│
     │                   │                      │
     │                   │ 1. Validate Refresh Token
     │                   │ 2. Check Revocation  │
     │                   │ 3. Generate New Access Token
     │                   │                      │
     │                   │   New Access Token   │
     │                   │<─────────────────────┤
     │   New Access Token│                      │
     │<──────────────────┤                      │
     │                   │                      │
```

---

## Security Features

### 1. Multi-Tenant Isolation

- **Database Level:** Separate schemas per tenant
- **JWT Claims:** `tenantId` embedded in tokens
- **Header Propagation:** `X-Tenant-ID` header
- **Rate Limiting:** Per-tenant quotas

### 2. Token Security

- **Access Token:** Short-lived (15 min), RSA256 signed
- **Refresh Token:** Long-lived (7 days), stored in Redis
- **Token Blacklisting:** Immediate invalidation on logout
- **Refresh Revocation:** User-wide revocation on password change

### 3. Account Protection

- **Account Lockout:** 5 failed attempts = 15-minute lockout
- **Email Verification:** Required before login
- **Password Requirements:** Min 8 chars, uppercase, lowercase, number, special char
- **Rate Limiting:** Per-endpoint and per-tenant limits

### 4. Audit Logging

- **Authentication Events:** Login, logout, failed attempts
- **Token Events:** Generation, refresh, revocation
- **Security Events:** Account lockout, suspicious activity
- **Correlation IDs:** Request tracing across services

---

## Key Differences: User Service vs Gateway vs Downstream

| Aspect                 | User Service             | Gateway Service             | Downstream Services      |
| ---------------------- | ------------------------ | --------------------------- | ------------------------ |
| **JWT Algorithm**      | RSA256 (asymmetric)      | RSA256 (asymmetric)         | RSA256 (asymmetric)      |
| **Key Type**           | RSA key pair (private)   | RSA public key (via JWK)    | RSA public key (via JWK) |
| **Token Generation**   | ✅ Generates tokens      | ❌ Only validates           | ❌ Only validates        |
| **Token Validation**   | ✅ Validates own tokens  | ✅ Validates all tokens     | ✅ Validates all tokens  |
| **User Context**       | ✅ Creates from database | ✅ Extracts from JWT        | ✅ Extracts from JWT     |
| **Header Propagation** | ❌ Not needed            | ✅ Propagates to downstream | ❌ Receives from Gateway |
| **Key Management**     | ✅ Manages key rotation  | ❌ Fetches from JWK         | ❌ Fetches from JWK      |
| **JWK Endpoint**       | ✅ Exposes public keys   | ❌ Consumes JWK endpoint    | ❌ Consumes JWK endpoint |

### Why RSA256 Everywhere?

**Benefits of Unified RSA256 Architecture:**

1. **Enhanced Security:**
   - Asymmetric encryption (public/private key pair)
   - No shared secrets to manage or leak
   - Private key never leaves User Service

2. **Simplified Key Distribution:**
   - JWK endpoint provides automatic key distribution
   - No manual key configuration needed
   - Services automatically fetch latest keys

3. **Zero-Downtime Key Rotation:**
   - Automated rotation every 90 days
   - 7-day grace period supports old and new keys
   - No service restarts required

4. **Consistent Architecture:**
   - Same algorithm across all services
   - Easier to understand and maintain
   - Reduced configuration complexity

5. **Industry Standard:**
   - OAuth2/OIDC compliant
   - Follows JWT best practices
   - Compatible with external identity providers

---

## Configuration Summary

### User Service

```yaml
gripday:
  auth:
    jwt:
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
      issuer: gripday-user-service
# Key rotation and cleanup are automatic
# - Key rotation: Every 90 days (configurable via @Scheduled annotation)
# - Token cleanup: Daily at 2 AM
# - Grace period: 7 days for old keys
```

### Gateway Service

```yaml
gripday:
  gateway:
    security:
      jwt:
        issuer: gripday-user-service
        algorithm: RS256
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
      authentication:
        enabled: true
        enable-user-context-propagation: true
      public-paths:
        - /.well-known/jwks.json
        # ... other public paths
```

### Downstream Services

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://user-service:8080
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
# Spring Security automatically:
# - Fetches public keys from JWK endpoint
# - Caches keys (5 minutes default)
# - Refreshes on key rotation
# - Validates JWT signatures
```

---

## Best Practices

### 1. Token Management

- ✅ Use short-lived access tokens (15 min)
- ✅ Use long-lived refresh tokens (7 days)
- ✅ Implement token blacklisting for logout
- ✅ Revoke all tokens on password change
- ✅ Store refresh tokens securely in Redis
- ✅ Automated token cleanup (daily at 2 AM)
- ✅ Include unique token ID (jti) for tracking

### 2. Key Management

- ✅ Automated key rotation every 90 days
- ✅ 7-day grace period for old keys
- ✅ Unique key IDs (kid) in JWT header
- ✅ JWK endpoint for dynamic key distribution
- ✅ Manual rotation available for emergencies
- ✅ Comprehensive metrics and monitoring

### 3. Security Headers

- ✅ Propagate `X-Correlation-ID` for tracing
- ✅ Propagate `X-User-ID`, `X-Username`, `X-User-Roles`
- ✅ Propagate `X-Tenant-ID` for multi-tenancy
- ✅ Remove internal headers in responses

### 4. Error Handling

- ✅ Return RFC7807 Problem Details for errors
- ✅ Include correlation IDs in error responses
- ✅ Log security events with audit trail
- ✅ Rate limit authentication endpoints

### 5. Multi-Tenant Support

- ✅ Isolate data at database level
- ✅ Validate tenant context in every request
- ✅ Enforce tenant-specific rate limits
- ✅ Prevent cross-tenant access

### 6. Observability

- ✅ Metrics for token operations (generation, validation, cleanup)
- ✅ Metrics for key rotation events
- ✅ Distributed tracing with correlation IDs
- ✅ Security audit logging
- ✅ Redis memory monitoring

---

## Testing Authentication

### 1. Test JWK Endpoint

```bash
# Verify public keys are exposed
curl http://localhost:8080/.well-known/jwks.json | jq

# Expected response:
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "unique-key-id",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

### 2. Register User

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }' | jq -r '.accessToken'
```

### 4. Access Protected Resource

```bash
TOKEN="<access_token>"

curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/bookstore/books
```

### 5. Refresh Token

```bash
REFRESH_TOKEN="<refresh_token>"

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### 6. Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Test Key Rotation (Admin)

```bash
# Manual key rotation
curl -X POST http://localhost:8080/api/v1/admin/keys/rotate \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Verify new key ID in JWK endpoint
curl http://localhost:8080/.well-known/jwks.json | jq '.keys[].kid'
```

### 8. Test Token Cleanup (Admin)

```bash
# Manual cleanup trigger
curl -X POST http://localhost:8080/api/v1/admin/keys/cleanup \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep token_cleanup
```

---

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check token expiry
   - Verify token signature
   - Check token blacklist
   - Verify issuer matches configuration
   - Ensure JWK endpoint is accessible

2. **403 Forbidden**
   - Check user roles in JWT claims
   - Verify method-level security annotations
   - Check tenant context
   - Verify user has required permissions

3. **Token Validation Fails**
   - Verify JWK endpoint is accessible
   - Check network connectivity to User Service
   - Verify issuer URI matches configuration
   - Check if key rotation is in progress
   - Verify token format and structure

4. **JWK Endpoint Not Accessible**
   - Verify endpoint is in public paths
   - Check SecurityConfig allows public access
   - Verify User Service is running
   - Check network/firewall rules

5. **Key Rotation Issues**
   - Check grace period (7 days default)
   - Verify old tokens still work during grace period
   - Check JwtKeyManagementService logs
   - Verify multiple keys in JWK endpoint

6. **Cross-Tenant Access**
   - Verify `X-Tenant-ID` header
   - Check JWT `tenantId` claim
   - Validate tenant isolation logic
   - Check database-level isolation

7. **Redis Memory Growing**
   - Verify TokenCleanupService is running
   - Check cleanup job logs (runs at 2 AM)
   - Manually trigger cleanup if needed
   - Monitor Redis memory metrics

---

## Architecture Improvements (2024)

### What Changed

The authentication architecture was enhanced with the following improvements:

#### 1. JWK Endpoint for Dynamic Key Distribution

- **Before:** Manual public key configuration in each service
- **After:** Automatic key fetching from `/.well-known/jwks.json`
- **Benefit:** Zero-configuration key distribution, automatic updates

#### 2. Automated Key Rotation

- **Before:** Static keys generated at startup
- **After:** Automated rotation every 90 days with 7-day grace period
- **Benefit:** Enhanced security, zero-downtime rotation

#### 3. Unified RSA256 Architecture

- **Before:** Gateway used HMAC-SHA256, downstream used RSA256
- **After:** RSA256 everywhere with JWK endpoint
- **Benefit:** Simplified architecture, no shared secrets, better security

#### 4. Automated Token Cleanup

- **Before:** No cleanup, Redis memory growth
- **After:** Daily cleanup of expired tokens and sessions
- **Benefit:** Prevents memory leaks, maintains Redis performance

### Security Enhancements

| Feature               | Before             | After             | Impact |
| --------------------- | ------------------ | ----------------- | ------ |
| Key Distribution      | Manual             | Automatic (JWK)   | High   |
| Key Rotation          | None               | Every 90 days     | High   |
| Algorithm Consistency | Mixed (HMAC + RSA) | Unified (RSA256)  | Medium |
| Token Cleanup         | None               | Daily automated   | Medium |
| Grace Period          | N/A                | 7 days            | High   |
| Key Versioning        | None               | kid in JWT header | Medium |
| Shared Secrets        | Required           | None              | High   |

### Operational Improvements

- **Reduced Configuration:** No manual key distribution needed
- **Zero Downtime:** Key rotation without service restarts
- **Better Monitoring:** Comprehensive metrics for all operations
- **Easier Debugging:** Correlation IDs and audit logging
- **Memory Management:** Automated cleanup prevents Redis bloat
- **Admin Controls:** Manual rotation and cleanup endpoints

### Migration Path

For existing deployments, see:

- [AUTHENTICATION-MIGRATION-GUIDE.md](AUTHENTICATION-MIGRATION-GUIDE.md) - Step-by-step migration
- [AUTHENTICATION-FIXES-IMPLEMENTATION.md](AUTHENTICATION-FIXES-IMPLEMENTATION.md) - Implementation details

## Monitoring and Metrics

### Key Metrics to Monitor

#### Authentication Metrics

```prometheus
# Login attempts
http_server_requests_seconds_count{uri="/api/v1/auth/login"}

# Token generation
jwt_token_generation_total{type="access"}
jwt_token_generation_total{type="refresh"}

# Token validation
jwt_token_validation_total{result="success"}
jwt_token_validation_total{result="failure"}

# Account lockouts
auth_account_lockout_total
```

#### Key Management Metrics

```prometheus
# Active keys
jwt_active_keys_count

# Key rotation events
jwt_key_rotation_total
jwt_key_rotation_errors_total

# JWK endpoint requests
http_server_requests_seconds_count{uri="/.well-known/jwks.json"}
```

#### Token Cleanup Metrics

```prometheus
# Cleanup operations
token_cleanup_refresh_tokens_total{status="removed"}
token_cleanup_revocations_total{status="removed"}
token_cleanup_sessions_total{status="removed"}
token_cleanup_blacklist_total{status="removed"}

# Cleanup duration
token_cleanup_duration_seconds

# Cleanup errors
token_cleanup_errors_total
```

#### Redis Metrics

```prometheus
# Memory usage
redis_memory_used_bytes

# Key counts
redis_keys_count{pattern="refresh:token:*"}
redis_keys_count{pattern="blacklist:token:*"}
redis_keys_count{pattern="session:*"}
```

### Recommended Alerts

```yaml
# Alert if JWK endpoint fails
- alert: JWKEndpointDown
  expr: rate(http_server_requests_seconds_count{uri="/.well-known/jwks.json",status="5xx"}[5m]) > 0
  for: 5m
  severity: critical

# Alert if key rotation fails
- alert: KeyRotationFailed
  expr: rate(jwt_key_rotation_errors_total[1h]) > 0
  severity: critical

# Alert if token cleanup fails
- alert: TokenCleanupFailed
  expr: rate(token_cleanup_errors_total[1d]) > 0
  severity: warning

# Alert if Redis memory is high
- alert: RedisMemoryHigh
  expr: redis_memory_used_bytes > 1073741824 # 1GB
  severity: warning

# Alert if authentication failure rate is high
- alert: HighAuthenticationFailureRate
  expr: rate(jwt_token_validation_total{result="failure"}[5m]) > 10
  severity: warning
```

### Grafana Dashboard Panels

**Authentication Overview:**

- Login success/failure rate
- Active sessions count
- Token generation rate
- Average token validation time

**Key Management:**

- Active keys count
- Last key rotation timestamp
- Key rotation history
- JWK endpoint response time

**Token Cleanup:**

- Tokens cleaned per day
- Cleanup duration trend
- Redis memory usage
- Cleanup success rate

**Security:**

- Failed login attempts
- Account lockouts
- Suspicious activity alerts
- Token blacklist size

---

## References

- [User Service README](gripday-user-service/README.md)
- [Gateway Service README](gripday-gateway-service/README.md)
- [Authentication API Documentation](gripday-user-service/docs/api/authentication.md)
- [Spring Boot Best Practices](.kiro/steering/spring-boot-microservices-best-practices.md)
- [Authentication Gap Analysis](AUTHENTICATION-GAPS-ANALYSIS.md)
- [Authentication Fixes Implementation](AUTHENTICATION-FIXES-IMPLEMENTATION.md)
- [Authentication Migration Guide](AUTHENTICATION-MIGRATION-GUIDE.md)
