# 🔐 IQ Scaffold User Service

> Centralized authentication and user management microservice providing JWT-based authentication, user lifecycle management, role-based access control, and email verification.

## Business Purpose

An identity and access management service that handles:

- **User Authentication** - Secure login/logout with JWT tokens, refresh token rotation, and session management
- **User Registration** - Self-service account creation with email verification and strong password enforcement
- **Account Security** - Password reset flows, account lockout protection, and multi-device session management
- **User Management** - Admin-controlled user CRUD operations with role-based permissions
- **User Preferences** - Self-service preference management for personalization (locale, theme, notifications, profile)
- **Organization Management** - Organization CRUD with owner assignment and tenant isolation
- **Organization Preferences** - Organization-wide settings for security policies, defaults, and configurations
- **Multi-Tenancy** - Tenant isolation ensuring data segregation across organizations with schema-per-tenant strategy
- **Email Verification** - Token-based email verification with rate limiting and expiration handling

## Overview

This is the authentication hub for the IQ Scaffold microservices platform. It centralizes identity management, enabling other services to delegate authentication and authorization concerns while maintaining consistent security policies across the ecosystem.

## What It Demonstrates

### 🔐 Authentication & Authorization

- JWT-based stateless authentication with RSA256 (JwtEncoder/JwtDecoder)
- Access tokens (15min) and refresh tokens (7 days) with configurable expiry
- Token rotation and Redis-backed blacklisting with TTL
- JTI (JWT ID) for unique token identification
- Role-based access control (RBAC) with method-level @PreAuthorize
- User context extraction with pattern matching (Java 21)
- Comprehensive JWT claims (userId, username, email, roles, permissions, firstName, lastName, tenantId)
- Self-service user preference management
- Admin-controlled organization settings

### 📧 Email Verification Patterns

- UUID-based verification tokens (24h expiry, 48h cleanup)
- Single-use token enforcement with database flag
- Rate limiting (3 emails/hour per user) with sliding window
- Token invalidation on new generation
- Transactional email templates with Thymeleaf
- Multi-language support with i18n
- Scheduled cleanup with @Scheduled (daily at 2 AM)
- Metrics tracking for verification success/failure

### 🛡️ Security Implementation

- Account lockout after 5 failed attempts (15min duration) with Redis state
- Failed attempts counter with 30min sliding window
- Password strength validation with custom annotations (@ValidPassword)
- Username validation with custom annotations (@ValidUsername)
- Input sanitization (SQL injection, XSS prevention)
- IP-based rate limiting (5 attempts/min)
- Security audit logging with correlation IDs and UserAuditLog entity
- Redis-backed token blacklist with automatic TTL expiration
- Fail-open strategy for Redis unavailability (availability over strict security)

### 🏢 Multi-Tenancy Patterns

- Schema-per-tenant isolation with Hibernate MultiTenantConnectionProvider
- SchemaPerTenantConnectionProvider with dynamic schema switching
- TenantContext for thread-local tenant management
- Tenant extraction from JWT claims and headers via TenantExtractionFilter
- Per-tenant Liquibase migrations with TenantLiquibaseRunner
- H2 test support with automatic schema creation
- Cross-tenant operations via TenantContext.executeInTenantContext()
- Tenant-scoped repositories (no tenant_id predicates needed)
- Organization-level user management within tenant boundaries

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
- Progressive account lockout with Redis-backed state (5 attempts, 15min lockout)
- Failed attempts tracking with 30min sliding window
- Token-based email verification with UUID generation
- Secure password reset flow with time-limited tokens
- Session management across devices with refresh token revocation
- Audit logging for security events (UserAuditLog entity)
- JTI-based token blacklisting with automatic cleanup

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

### Tenancy Implementation

- Strategy: Hibernate schema-per-tenant; each tenant's data isolated in its own schema
- Context: `TenantContext` controls current tenant; repositories are tenant-agnostic
- Service layer: cross-tenant operations run by iterating tenants and executing in context
- No cross-tenant joins: removed `tenant_id`-based predicates from repositories and queries
- H2 tests: service counting uses schema detection and a PUBLIC fallback to ensure test reliability
- Liquibase: per-tenant constraints and indexes (e.g., `organizations` name unique per tenant; one-to-one `users.organization_id`)
- Usage pattern:

```java
// Per-tenant operation
var count = TenantContext.executeInTenantContext(tenantId, () -> userRepository.countByEnabledTrue());

// Cross-tenant aggregation (service layer)
var stats = tenantRepository
  .findByEnabledTrue()
  .stream()
  .map((t) ->
    new TenantStatistics(
      t.getTenantId(),
      t.getName(),
      t.getEnabled(),
      TenantContext.executeInTenantContext(t.getTenantId(), () -> userRepository.countByEnabledTrue()),
      t.getMaxUsers(),
      TenantStatistics.calculateUtilization(TenantContext.executeInTenantContext(t.getTenantId(), () -> userRepository.countByEnabledTrue()), t.getMaxUsers()),
      t.getCreatedAt()
    )
  )
  .toList();
```

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

### User Profile & Preferences

- Get current user context
- Update own password
- User context propagation via JWT
- Manage personal preferences (locale, timezone, theme, notifications)
- Profile customization (photo, phone, bio)
- Personal security settings (2FA preferences)

### Administrative Functions

#### User Management

- List users (paginated, tenant-scoped)
- Get user by ID
- Create new user
- Update user details
- Delete user (with self-deletion prevention)
- Role assignment and management
- Audit logging for admin actions

#### Organization Management

- List organizations (paginated, tenant-scoped)
- Get organization by ID
- Create new organization
- Update organization details
- Delete organization
- Assign organization owner

#### Organization Preference Management

- Configure organization-wide settings
- Set password policies (min length, complexity)
- Define security settings (session timeout, login attempts, lockout)
- Configure two-factor authentication policies
- Set localization defaults (locale, timezone, currency)
- Manage registration and verification policies

#### Tenant Management

- List tenants with statistics
- Create new tenant with schema provisioning
- Update tenant configuration
- Enable/disable tenants
- View tenant utilization and user counts
- Tenant-scoped data isolation

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

#### User Management

- `GET /api/v1/admin/users` - List users
- `GET /api/v1/admin/users/{id}` - Get user by ID
- `POST /api/v1/admin/users` - Create user
- `PUT /api/v1/admin/users/{id}` - Update user
- `DELETE /api/v1/admin/users/{id}` - Delete user

#### Organization Management

- `GET /api/v1/admin/organizations` - List organizations
- `GET /api/v1/admin/organizations/{id}` - Get organization by ID
- `POST /api/v1/admin/organizations` - Create organization
- `PUT /api/v1/admin/organizations/{id}` - Update organization
- `DELETE /api/v1/admin/organizations/{id}` - Delete organization

#### Organization Preference Management

- `GET /api/v1/admin/organization-preferences` - List organization preferences
- `GET /api/v1/admin/organization-preferences/{id}` - Get preference by ID
- `GET /api/v1/admin/organization-preferences/organization/{orgId}` - Get preference by organization
- `POST /api/v1/admin/organization-preferences` - Create organization preference
- `PUT /api/v1/admin/organization-preferences/{id}` - Update organization preference
- `DELETE /api/v1/admin/organization-preferences/{id}` - Delete organization preference

#### Tenant Management

- `GET /api/v1/admin/tenants` - List tenants
- `GET /api/v1/admin/tenants/{id}` - Get tenant by ID
- `POST /api/v1/admin/tenants` - Create tenant
- `PUT /api/v1/admin/tenants/{id}` - Update tenant
- `DELETE /api/v1/admin/tenants/{id}` - Delete tenant
- `GET /api/v1/admin/tenants/statistics` - Get tenant statistics

### User Preference Endpoints (Self-Service)

- `GET /api/v1/users/me/preferences` - Get my preferences
- `PATCH /api/v1/users/me/preferences` - Update my preferences
- `DELETE /api/v1/users/me/preferences` - Delete my preferences (reset to defaults)

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - API documentation
- `/.well-known/jwks.json` - JWK Set for token validation

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
  "iss": "iqscaffold-user-service",
  "iat": 1634567890,
  "exp": 1634568790
}
```

---

**Use this as a blueprint** for building authentication services and implementing security patterns in your microservices architecture. The code demonstrates production-ready patterns for identity management, multi-tenancy, and secure token-based authentication.
