# 🔐 Gripday User Service

> Centralized authentication and user management microservice providing JWT-based authentication, user lifecycle management, role-based access control, and email verification.

## Business Purpose

An identity and access management service that handles:

- **User Authentication** - Secure login/logout with JWT tokens, refresh token rotation, and session management
- **User Registration** - Self-service account creation with email verification and strong password enforcement
- **Account Security** - Password reset flows, account lockout protection, and multi-device session management
- **User Management** - Admin-controlled user CRUD operations with role-based permissions
- **Multi-Tenancy** - Tenant isolation ensuring data segregation across organizations
- **Email Verification** - Token-based email verification with rate limiting and expiration handling

## Overview

This is the authentication hub for the Gripday microservices platform. It centralizes identity management, enabling other services to delegate authentication and authorization concerns while maintaining consistent security policies across the ecosystem.

## What It Demonstrates

### 🔐 Authentication & Authorization

- JWT-based stateless authentication
- Access tokens (15min) and refresh tokens (7 days)
- Token rotation and blacklisting
- Role-based access control (RBAC)
- Method-level security with @PreAuthorize
- User context extraction and propagation

### 📧 Email Verification Patterns

- Token-based email verification (24h expiry)
- Single-use token enforcement
- Rate limiting (3 emails/hour per user)
- Transactional email templates with Thymeleaf
- Multi-language support with i18n

### 🛡️ Security Implementation

- Account lockout after 5 failed attempts (15min duration)
- Password strength validation
- Input sanitization (SQL injection, XSS prevention)
- IP-based rate limiting (5 attempts/min)
- Security audit logging with correlation IDs
- Redis-backed token blacklist

### 🏢 Multi-Tenancy Patterns

- Tenant context extraction from JWT
- Tenant-scoped data isolation
- Tenant ID propagation to downstream services
- Organization-level user management
- Cross-tenant access prevention

### 🎯 Observability & Monitoring

- Structured JSON logging with correlation IDs
- OpenTelemetry distributed tracing
- Prometheus metrics integration
- Security audit trail
- Health checks and actuator endpoints

## Architecture Patterns

### Key Design Patterns

- Repository pattern for data access
- Service layer for business logic
- DTO pattern with Java records
- Token blacklist with Redis TTL
- Email verification workflow
- Password reset flow with secure tokens

### API Design

- RESTful endpoints with proper HTTP methods
- Versioning support (URL-based)
- OpenAPI/Swagger documentation
- Problem Details (RFC 7807) for errors
- Consistent error response format

## Technical Highlights

### Security Features

- Strong password requirements (8+ chars, mixed case, numbers, special chars)
- Progressive account lockout protection
- Token-based email verification
- Secure password reset flow
- Session management across devices
- Audit logging for security events

### Performance Optimization

- Redis caching for token blacklist
- Connection pooling for database
- Efficient JWT validation
- Optimized database queries with indexes
- Async email sending

### Data Management

- Liquibase for database migrations
- PostgreSQL with proper indexing
- Transaction management
- Token cleanup scheduled tasks
- Soft deletes for audit trail

### Testing Approach

- Unit tests with JUnit 5
- Integration tests with Testcontainers
- Security testing
- Architecture tests with ArchUnit
- Spring Modulith validation

### Operational Features

- Docker containerization
- Environment-specific profiles (local, staging, production)
- Graceful shutdown
- Structured JSON logging
- Health checks and metrics

## Use Cases Implemented

### User Authentication

- User signup with email verification
- Login with username or email
- JWT token generation (access + refresh)
- Token refresh and rotation
- Logout (single device)
- Logout from all devices
- Token validation for downstream services

### Email Verification

- Send verification email on signup
- Verify email with token link
- Resend verification email (rate limited)
- Check verification status
- Single-use token enforcement

### Password Management

- Forgot password flow
- Password reset with secure token
- Token validation (HEAD request)
- Change password for authenticated users
- Password strength validation

### User Profile

- Get current user context
- Update own password
- User context propagation via JWT

### Administrative Functions

- List users (paginated, tenant-scoped)
- Get user by ID
- Create new user
- Update user details
- Delete user (with self-deletion prevention)
- Role assignment and management
- Audit logging for admin actions

## API Examples

### Public Endpoints

- `POST /api/v1/auth/signup` - Register new user
- `POST /api/v1/auth/login` - Authenticate user
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/validate` - Validate JWT token
- `GET /api/v1/auth/email/verify` - Verify email address
- `POST /api/v1/auth/email/resend` - Resend verification email
- `POST /api/v1/auth/password/forgot` - Initiate password reset
- `POST /api/v1/auth/password/reset` - Reset password

### Protected Endpoints (Requires Authentication)

- `GET /api/v1/users/me` - Get current user
- `PATCH /api/v1/users/me/password` - Change password
- `POST /api/v1/auth/logout` - Logout current session
- `POST /api/v1/auth/logout-all` - Logout all sessions

### Admin Endpoints (Requires ADMIN/SUPER_ADMIN Role)

- `GET /api/v1/admin/users` - List users
- `POST /api/v1/admin/users` - Create user
- `PUT /api/v1/admin/users/{id}` - Update user
- `DELETE /api/v1/admin/users/{id}` - Delete user

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - API documentation
- `/api/v1/auth/.well-known/jwks.json` - JWK Set for token validation

## Learning Points

This implementation serves as a reference for:

- Building authentication services with Spring Security
- Implementing JWT-based stateless authentication
- Designing secure password reset flows
- Email verification with token-based workflows
- Multi-tenant data isolation patterns
- Account security (lockout, rate limiting)
- Audit logging for security events
- Token blacklisting with Redis
- User context propagation across services
- Role-based access control implementation

## Adapting for Your Domain

This authentication service demonstrates patterns applicable to various scenarios:

### Identity Management Systems

- Employee authentication portals
- Customer identity platforms
- Partner access management
- Multi-tenant SaaS authentication

### Token-Based Workflows

- Email verification for any registration flow
- Document approval workflows
- Order confirmation systems
- Subscription activation processes

### Security Patterns

- Account lockout mechanisms
- Rate limiting strategies
- Audit logging for compliance
- Multi-device session management

### Multi-Tenancy

- Organization-level data isolation
- Tenant context propagation
- Cross-tenant access prevention
- Tenant-scoped administrative operations

The patterns demonstrated here apply to any domain requiring centralized authentication, user management, email verification workflows, and multi-tenant isolation.

## Integration with Other Services

### Consuming Authentication

Other microservices validate JWT tokens using the JWK Set endpoint:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/api/v1/auth/.well-known/jwks.json
```

Extract user context from JWT claims:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(Authentication auth) {
  if (auth instanceof JwtAuthenticationToken token) {
    var userId = token.getToken().getSubject();
    var tenantId = token.getToken().getClaimAsString("tenantId");
    var roles = token.getToken().getClaimAsStringList("roles");
    // Use context for business logic
  }
}
```

### JWT Token Structure

Access tokens carry comprehensive user context:

```json
{
  "sub": "1",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "roles": ["USER"],
  "permissions": ["READ_PROFILE"],
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "tenant-123",
  "type": "access",
  "iss": "gripday-user-service",
  "iat": 1634567890,
  "exp": 1634568790
}
```

---

**Use this as a blueprint** for building authentication services and implementing security patterns in your microservices architecture. The code demonstrates production-ready patterns for identity management, multi-tenancy, and secure token-based authentication.
