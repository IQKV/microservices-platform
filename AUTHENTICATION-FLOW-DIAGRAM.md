# Gripday Authentication Flow Diagrams

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Gripday Platform                                 │
│                                                                          │
│  ┌──────────┐         ┌──────────┐         ┌──────────────────────┐   │
│  │  Client  │         │ Gateway  │         │   User Service       │   │
│  │ (React)  │◄───────►│ Service  │◄───────►│  (Auth Authority)    │   │
│  │          │         │  (BFF)   │         │                      │   │
│  └──────────┘         └────┬─────┘         └──────────────────────┘   │
│                            │                                            │
│                            │                                            │
│                            ▼                                            │
│                  ┌──────────────────┐                                   │
│                  │    Bookstore     │                                   │
│                  │     Service      │                                   │
│                  │  (Business Logic)│                                   │
│                  └──────────────────┘                                   │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │                    Shared Infrastructure                        │    │
│  │                                                                 │    │
│  │  ┌──────────┐    ┌──────────┐    ┌──────────┐                │    │
│  │  │PostgreSQL│    │  Redis   │    │  Loki    │                │    │
│  │  │(Per Svc) │    │ (Cache)  │    │ (Logs)   │                │    │
│  │  └──────────┘    └──────────┘    └──────────┘                │    │
│  └────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

## JWT Token Generation (User Service)

```
┌─────────────────────────────────────────────────────────────────────┐
│                      User Service - Token Generation                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ POST /login  │
│ {username,   │
│  password}   │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 1. Validate Credentials              │
│    - Check username/email exists     │
│    - Verify password (BCrypt)        │
│    - Check account enabled           │
│    - Check email verified            │
│    - Check account not locked        │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 2. Load User Context                 │
│    - User ID, username, email        │
│    - Roles (USER, ADMIN, etc.)       │
│    - Permissions                     │
│    - Tenant ID                       │
│    - Department, Organization        │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 3. Generate Access Token (RSA256)    │
│    ┌──────────────────────────────┐  │
│    │ Header:                      │  │
│    │   alg: RS256                 │  │
│    │   typ: JWT                   │  │
│    ├──────────────────────────────┤  │
│    │ Payload:                     │  │
│    │   iss: gripday-user-service  │  │
│    │   sub: 1                     │  │
│    │   exp: now + 15min           │  │
│    │   userId: 1                  │  │
│    │   username: johndoe          │  │
│    │   roles: [USER, ADMIN]       │  │
│    │   tenantId: default          │  │
│    ├──────────────────────────────┤  │
│    │ Signature:                   │  │
│    │   RS256(header + payload,    │  │
│    │         privateKey)          │  │
│    └──────────────────────────────┘  │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 4. Generate Refresh Token (RSA256)   │
│    - Expiry: 7 days                  │
│    - Type: refresh                   │
│    - Minimal claims                  │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 5. Store Session in Redis            │
│    Key: session:{sessionId}          │
│    Value: {userId, username,         │
│            tenantId, ipAddress}      │
│    TTL: 30 minutes                   │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 6. Return Token Response             │
│    {                                 │
│      accessToken: "eyJ...",          │
│      refreshToken: "eyJ...",         │
│      expiresIn: 900,                 │
│      userContext: {...}              │
│    }                                 │
└──────────────────────────────────────┘
```

## JWT Validation (Gateway Service)

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Gateway Service - JWT Validation                    │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│ Incoming Request     │
│ Authorization:       │
│   Bearer eyJ...      │
│ X-Tenant-ID: default │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 1. Check Public Path                 │
│    - /api/v1/auth/login ✓            │
│    - /api/v1/auth/signup ✓           │
│    - /api/v1/bookstore/books ✓       │
│    - Others require auth             │
└──────┬───────────────────────────────┘
       │
       │ Protected Path
       ▼
┌──────────────────────────────────────┐
│ 2. Extract JWT Token                 │
│    - Get Authorization header        │
│    - Remove "Bearer " prefix         │
│    - Validate token format           │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 3. Validate JWT (HMAC-SHA256)        │
│    ┌──────────────────────────────┐  │
│    │ Parse JWT                    │  │
│    │   ├─ Verify signature        │  │
│    │   │  (HMAC with secret key)  │  │
│    │   ├─ Check expiry            │  │
│    │   ├─ Verify issuer           │  │
│    │   ├─ Verify audience         │  │
│    │   └─ Check blacklist (Redis) │  │
│    └──────────────────────────────┘  │
└──────┬───────────────────────────────┘
       │
       │ Valid Token
       ▼
┌──────────────────────────────────────┐
│ 4. Extract User Context              │
│    - userId: 1                       │
│    - username: johndoe               │
│    - email: john@example.com         │
│    - roles: [USER, ADMIN]            │
│    - tenantId: default               │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 5. Extract Tenant Context            │
│    Priority:                         │
│    1. X-Tenant-ID header             │
│    2. JWT tenantId claim             │
│    3. Subdomain extraction           │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 6. Propagate Context Headers         │
│    X-Correlation-ID: abc-123         │
│    X-User-ID: 1                      │
│    X-Username: johndoe               │
│    X-User-Roles: USER,ADMIN          │
│    X-Tenant-ID: default              │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 7. Forward to Downstream Service     │
│    - Keep Authorization header       │
│    - Add context headers             │
│    - Apply rate limiting             │
│    - Apply circuit breaker           │
└──────────────────────────────────────┘
```

## JWT Validation (Downstream Service)

```
┌─────────────────────────────────────────────────────────────────────┐
│              Bookstore Service - JWT Validation                      │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│ Request from Gateway │
│ Authorization:       │
│   Bearer eyJ...      │
│ X-User-ID: 1         │
│ X-Username: johndoe  │
│ X-User-Roles: USER   │
│ X-Tenant-ID: default │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 1. Spring Security Filter Chain      │
│    - Extract JWT from header         │
│    - Pass to OAuth2 Resource Server  │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 2. Validate JWT (RSA256)             │
│    ┌──────────────────────────────┐  │
│    │ Decode JWT                   │  │
│    │   ├─ Verify signature        │  │
│    │   │  (RSA with public key)   │  │
│    │   ├─ Check expiry            │  │
│    │   ├─ Verify issuer           │  │
│    │   └─ Parse claims            │  │
│    └──────────────────────────────┘  │
└──────┬───────────────────────────────┘
       │
       │ Valid Token
       ▼
┌──────────────────────────────────────┐
│ 3. Extract User Context from JWT     │
│    UserContextExtractor:             │
│    - userId from sub or userId claim │
│    - username from username claim    │
│    - email from email claim          │
│    - roles from roles claim          │
│    - permissions from permissions    │
│    - tenantId from tenantId claim    │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 4. Create Authentication Object      │
│    JwtAuthenticationToken:           │
│    - Principal: UserContext          │
│    - Authorities: Roles              │
│    - Authenticated: true             │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 5. Store in SecurityContext          │
│    SecurityContextHolder             │
│      .getContext()                   │
│      .setAuthentication(token)       │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 6. Authorize Request                 │
│    @PreAuthorize("hasRole('ADMIN')") │
│    - Check method-level security     │
│    - Verify user has required role   │
│    - Allow or deny access            │
└──────┬───────────────────────────────┘
       │
       │ Authorized
       ▼
┌──────────────────────────────────────┐
│ 7. Execute Business Logic            │
│    - Access UserContext              │
│    - Apply tenant filtering          │
│    - Process request                 │
│    - Return response                 │
└──────────────────────────────────────┘
```

## Complete Request Flow

```
┌────────┐     ┌─────────┐     ┌──────────┐     ┌──────────┐
│ Client │     │ Gateway │     │   User   │     │Bookstore │
│        │     │ Service │     │ Service  │     │ Service  │
└───┬────┘     └────┬────┘     └────┬─────┘     └────┬─────┘
    │               │               │                │
    │ 1. Login      │               │                │
    ├──────────────>│               │                │
    │               │ 2. Forward    │                │
    │               ├──────────────>│                │
    │               │               │                │
    │               │ 3. Validate   │                │
    │               │    Credentials│                │
    │               │               │                │
    │               │ 4. Generate   │                │
    │               │    JWT Tokens │                │
    │               │               │                │
    │               │ 5. Return     │                │
    │               │<──────────────┤                │
    │ 6. Tokens     │               │                │
    │<──────────────┤               │                │
    │               │               │                │
    │ 7. GET /books │               │                │
    │    + JWT      │               │                │
    ├──────────────>│               │                │
    │               │               │                │
    │               │ 8. Validate   │                │
    │               │    JWT        │                │
    │               │    (HMAC)     │                │
    │               │               │                │
    │               │ 9. Extract    │                │
    │               │    Context    │                │
    │               │               │                │
    │               │ 10. Forward   │                │
    │               │     + Headers │                │
    │               ├───────────────┼───────────────>│
    │               │               │                │
    │               │               │ 11. Validate   │
    │               │               │     JWT (RSA)  │
    │               │               │                │
    │               │               │ 12. Extract    │
    │               │               │     Context    │
    │               │               │                │
    │               │               │ 13. Authorize  │
    │               │               │                │
    │               │               │ 14. Process    │
    │               │               │                │
    │               │ 15. Response  │                │
    │               │<──────────────┼────────────────┤
    │ 16. Response  │               │                │
    │<──────────────┤               │                │
    │               │               │                │
```

## Token Refresh Flow

```
┌────────┐     ┌─────────┐     ┌──────────┐
│ Client │     │ Gateway │     │   User   │
│        │     │ Service │     │ Service  │
└───┬────┘     └────┬────┘     └────┬─────┘
    │               │               │
    │ Access Token  │               │
    │ Expired       │               │
    │               │               │
    │ 1. POST       │               │
    │    /refresh   │               │
    │    {refresh}  │               │
    ├──────────────>│               │
    │               │               │
    │               │ 2. Forward    │
    │               ├──────────────>│
    │               │               │
    │               │ 3. Validate   │
    │               │    Refresh    │
    │               │    Token      │
    │               │               │
    │               │ 4. Check      │
    │               │    Revocation │
    │               │    (Redis)    │
    │               │               │
    │               │ 5. Load User  │
    │               │    Context    │
    │               │               │
    │               │ 6. Generate   │
    │               │    New Access │
    │               │    Token      │
    │               │               │
    │               │ 7. Return     │
    │               │<──────────────┤
    │ 8. New Token  │               │
    │<──────────────┤               │
    │               │               │
```

## Logout Flow

```
┌────────┐     ┌─────────┐     ┌──────────┐     ┌───────┐
│ Client │     │ Gateway │     │   User   │     │ Redis │
│        │     │ Service │     │ Service  │     │       │
└───┬────┘     └────┬────┘     └────┬─────┘     └───┬───┘
    │               │               │               │
    │ 1. POST       │               │               │
    │    /logout    │               │               │
    │    + JWT      │               │               │
    ├──────────────>│               │               │
    │               │               │               │
    │               │ 2. Forward    │               │
    │               ├──────────────>│               │
    │               │               │               │
    │               │ 3. Extract    │               │
    │               │    Token ID   │               │
    │               │               │               │
    │               │ 4. Blacklist  │               │
    │               │    Token      │               │
    │               │               ├──────────────>│
    │               │               │ SET blacklist:│
    │               │               │ token:{jti}   │
    │               │               │               │
    │               │ 5. Delete     │               │
    │               │    Session    │               │
    │               │               ├──────────────>│
    │               │               │ DEL session:  │
    │               │               │ {sessionId}   │
    │               │               │               │
    │               │ 6. Success    │               │
    │               │<──────────────┤               │
    │ 7. 204 No     │               │               │
    │    Content    │               │               │
    │<──────────────┤               │               │
    │               │               │               │
```

## Multi-Tenant Context Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    Tenant Context Extraction                    │
└────────────────────────────────────────────────────────────────┘

Request arrives at Gateway:
┌──────────────────────────────────────┐
│ GET /api/v1/bookstore/books          │
│ Authorization: Bearer eyJ...         │
│ X-Tenant-ID: acme-corp               │
│ Host: acme.api.gripday.com           │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Priority 1: Check X-Tenant-ID Header │
│ ✓ Found: acme-corp                   │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Priority 2: Check JWT Claim          │
│ (Skip if header found)               │
│ JWT.tenantId = "acme-corp"           │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Priority 3: Extract from Subdomain   │
│ (Skip if header or JWT found)        │
│ Host: acme.api.gripday.com           │
│ Subdomain: acme                      │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Tenant Context Established           │
│ tenantId: acme-corp                  │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Add to MDC for Logging               │
│ MDC.put("tenantId", "acme-corp")     │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Propagate to Downstream              │
│ X-Tenant-ID: acme-corp               │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Downstream Service Receives          │
│ - Validates tenant context           │
│ - Applies tenant filtering           │
│ - Ensures data isolation             │
└──────────────────────────────────────┘
```

## Security Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                      Security Layers                             │
└─────────────────────────────────────────────────────────────────┘

Layer 1: Gateway Service
┌──────────────────────────────────────┐
│ ✓ JWT Validation (HMAC-SHA256)       │
│ ✓ Token Blacklist Check              │
│ ✓ Rate Limiting (Per Tenant)         │
│ ✓ CORS Enforcement                   │
│ ✓ Public Path Filtering              │
└──────┬───────────────────────────────┘
       │
       │ Valid Request
       ▼
Layer 2: Downstream Service
┌──────────────────────────────────────┐
│ ✓ JWT Validation (RSA256)            │
│ ✓ User Context Extraction            │
│ ✓ Method-Level Security               │
│   (@PreAuthorize)                    │
│ ✓ Tenant Isolation                   │
└──────┬───────────────────────────────┘
       │
       │ Authorized
       ▼
Layer 3: Business Logic
┌──────────────────────────────────────┐
│ ✓ Tenant Filtering (WHERE clause)    │
│ ✓ User Context Validation            │
│ ✓ Permission Checks                  │
│ ✓ Audit Logging                      │
└──────────────────────────────────────┘
```

## Error Handling Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    Authentication Error Flow                    │
└────────────────────────────────────────────────────────────────┘

Request with Invalid/Expired Token
┌──────────────────────┐
│ GET /api/v1/books    │
│ Authorization:       │
│   Bearer <expired>   │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Gateway: Validate JWT                │
│ ✗ Token Expired                      │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Return 401 Unauthorized              │
│ Content-Type:                        │
│   application/problem+json           │
│                                      │
│ {                                    │
│   "type": "/problems/auth_invalid",  │
│   "title": "Unauthorized",           │
│   "status": 401,                     │
│   "detail": "Invalid token",         │
│   "instance": "/api/v1/books",       │
│   "code": "AUTH_TOKEN_INVALID",      │
│   "correlationId": "abc-123",        │
│   "timestamp": "2024-01-15T10:30:00" │
│ }                                    │
└──────────────────────────────────────┘

Request with Insufficient Permissions
┌──────────────────────┐
│ DELETE /api/v1/books │
│ Authorization:       │
│   Bearer <user-jwt>  │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Bookstore: Check Authorization       │
│ @PreAuthorize("hasRole('ADMIN')")    │
│ User Roles: [USER]                   │
│ ✗ Insufficient Permissions           │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Return 403 Forbidden                 │
│ {                                    │
│   "type": "/problems/forbidden",     │
│   "title": "Forbidden",              │
│   "status": 403,                     │
│   "detail": "Insufficient perms",    │
│   "code": "AUTH_FORBIDDEN",          │
│   "correlationId": "abc-123"         │
│ }                                    │
└──────────────────────────────────────┘
```
