# User Service Gateway Routing Analysis

## Executive Summary

**Status**: ❌ **INCOMPLETE ROUTING** - Multiple user-service endpoints are NOT accessible through the gateway.

The gateway currently only routes `/api/v1/auth/**` paths, but user-service exposes 7 different endpoint groups across multiple base paths.

---

## User Service REST Endpoints Inventory

### 1. Authentication Endpoints ✅ ROUTED

**Base Path**: `/api/v1/auth`  
**Controller**: `AuthenticationResource`

| Method | Endpoint                  | Description             | Auth Required |
| ------ | ------------------------- | ----------------------- | ------------- |
| POST   | `/api/v1/auth/signup`     | User registration       | No (Public)   |
| POST   | `/api/v1/auth/login`      | User login              | No (Public)   |
| POST   | `/api/v1/auth/refresh`    | Refresh JWT token       | Yes           |
| POST   | `/api/v1/auth/logout`     | Logout user             | Yes           |
| POST   | `/api/v1/auth/logout-all` | Logout from all devices | Yes           |
| POST   | `/api/v1/auth/validate`   | Validate JWT token      | No (Public)   |
| GET    | `/api/v1/auth/health`     | Health check            | No (Public)   |

### 2. Email Verification Endpoints ✅ ROUTED

**Base Path**: `/api/v1/auth/email`  
**Controller**: `EmailVerificationResource`

| Method | Endpoint                    | Description               | Auth Required |
| ------ | --------------------------- | ------------------------- | ------------- |
| GET    | `/api/v1/auth/email/verify` | Verify email with token   | No (Public)   |
| POST   | `/api/v1/auth/email/resend` | Resend verification email | No (Public)   |
| GET    | `/api/v1/auth/email/status` | Get verification status   | No (Public)   |

### 3. Password Management Endpoints ❌ NOT ROUTED

**Base Path**: `/api/v1/password`  
**Controller**: `PasswordResetResource`

| Method | Endpoint                  | Description                     | Auth Required |
| ------ | ------------------------- | ------------------------------- | ------------- |
| POST   | `/api/v1/password/forgot` | Initiate password reset         | No (Public)   |
| POST   | `/api/v1/password/reset`  | Reset password with token       | No (Public)   |
| POST   | `/api/v1/password/change` | Change password (authenticated) | Yes           |

### 4. User Profile Endpoints ❌ NOT ROUTED

**Base Path**: `/api/v1/users`  
**Controller**: `UserProfileResource`

| Method | Endpoint           | Description              | Auth Required |
| ------ | ------------------ | ------------------------ | ------------- |
| GET    | `/api/v1/users/me` | Get current user profile | Yes           |

### 5. User Management Endpoints ❌ NOT ROUTED

**Base Path**: `/api/v1/users`  
**Controller**: `UserManagementResource`

| Method | Endpoint             | Description                | Auth Required | Role Required     |
| ------ | -------------------- | -------------------------- | ------------- | ----------------- |
| GET    | `/api/v1/users`      | List all users (paginated) | Yes           | ADMIN/SUPER_ADMIN |
| GET    | `/api/v1/users/{id}` | Get user by ID             | Yes           | ADMIN/SUPER_ADMIN |
| POST   | `/api/v1/users`      | Create new user            | Yes           | ADMIN/SUPER_ADMIN |
| PUT    | `/api/v1/users/{id}` | Update user                | Yes           | ADMIN/SUPER_ADMIN |
| DELETE | `/api/v1/users/{id}` | Delete user                | Yes           | ADMIN/SUPER_ADMIN |

### 6. Organization Management Endpoints ❌ NOT ROUTED

**Base Path**: `/api/v1/organizations`  
**Controller**: `OrganizationManagementResource`

| Method | Endpoint                     | Description            | Auth Required | Role Required     |
| ------ | ---------------------------- | ---------------------- | ------------- | ----------------- |
| GET    | `/api/v1/organizations`      | List all organizations | Yes           | ADMIN/SUPER_ADMIN |
| GET    | `/api/v1/organizations/{id}` | Get organization by ID | Yes           | ADMIN/SUPER_ADMIN |
| POST   | `/api/v1/organizations`      | Create organization    | Yes           | ADMIN/SUPER_ADMIN |
| PUT    | `/api/v1/organizations/{id}` | Update organization    | Yes           | ADMIN/SUPER_ADMIN |
| DELETE | `/api/v1/organizations/{id}` | Delete organization    | Yes           | ADMIN/SUPER_ADMIN |

### 7. Tenant Management Endpoints ❌ NOT ROUTED

**Base Path**: `/api/v1/admin/tenants`  
**Controller**: `TenantManagementResource`

| Method | Endpoint                                   | Description           | Auth Required | Role Required |
| ------ | ------------------------------------------ | --------------------- | ------------- | ------------- |
| GET    | `/api/v1/admin/tenants`                    | List all tenants      | Yes           | SUPER_ADMIN   |
| GET    | `/api/v1/admin/tenants/{tenantId}`         | Get tenant by ID      | Yes           | SUPER_ADMIN   |
| POST   | `/api/v1/admin/tenants`                    | Create tenant         | Yes           | SUPER_ADMIN   |
| PUT    | `/api/v1/admin/tenants/{tenantId}`         | Update tenant         | Yes           | SUPER_ADMIN   |
| PATCH  | `/api/v1/admin/tenants/{tenantId}/enabled` | Enable/disable tenant | Yes           | SUPER_ADMIN   |
| DELETE | `/api/v1/admin/tenants/{tenantId}`         | Delete tenant         | Yes           | SUPER_ADMIN   |
| GET    | `/api/v1/admin/tenants/statistics`         | Get tenant statistics | Yes           | SUPER_ADMIN   |

---

## Current Gateway Configuration Issues

### Issue 1: Single Route Pattern

**Location**: `gripday-gateway-service/src/main/java/org/gripday/gatewayservice/config/GatewayConfig.java`

```java
.route("user-service", r -> r
    .path("/api/v1/auth/**")  // ❌ Only matches /api/v1/auth/** paths
    .filters(f -> f
        .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
        .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
        .filter(loadBalancingFilter.apply(createLoadBalancingConfig("user-service")))
    )
    .uri(authServiceConfig.uri())
)
```

**Problem**: This route only matches paths starting with `/api/v1/auth/`, missing:

- `/api/v1/password/**` (Password management)
- `/api/v1/users/**` (User profile & management)
- `/api/v1/organizations/**` (Organization management)
- `/api/v1/admin/tenants/**` (Tenant management)

### Issue 2: Public Paths Configuration Incomplete

**Location**: `gripday-gateway-service/src/main/resources/application.yml`

The public paths list includes password endpoints that aren't even routed:

```yaml
public-paths:
  - /api/v1/auth/login
  - /api/v1/auth/signup
  # ... other auth paths
  # ❌ Missing password endpoints that should be public:
  # - /api/v1/password/forgot
  # - /api/v1/password/reset
```

---

## Recommended Solution

### Option 1: Expand Single Route (Simplest)

Change the path pattern to match all user-service endpoints:

```java
.route("user-service", r -> r
    .path("/api/v1/auth/**", "/api/v1/password/**", "/api/v1/users/**",
          "/api/v1/organizations/**", "/api/v1/admin/**")
    .filters(f -> f
        .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
        .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
        .filter(loadBalancingFilter.apply(createLoadBalancingConfig("user-service")))
    )
    .uri(authServiceConfig.uri())
)
```

### Option 2: Separate Routes by Concern (More Granular)

Create distinct routes for different endpoint groups:

```java
// Authentication & Email Verification
.route("auth-authentication", r -> r
    .path("/api/v1/auth/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
// Password Management
.route("auth-password", r -> r
    .path("/api/v1/password/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
// User Profile & Management
.route("auth-users", r -> r
    .path("/api/v1/users/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
// Organization Management
.route("auth-organizations", r -> r
    .path("/api/v1/organizations/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
// Tenant Management (Admin)
.route("auth-admin", r -> r
    .path("/api/v1/admin/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
```

### Option 3: Wildcard Route (Most Flexible)

Route all `/api/v1/**` to user-service (if it's the only service currently):

```java
.route("user-service", r -> r
    .path("/api/v1/**")
    .filters(f -> f.filter(...))
    .uri(authServiceConfig.uri())
)
```

---

## Public Paths to Add

Update the `public-paths` configuration to include:

```yaml
gripday:
  gateway:
    security:
      public-paths:
        # Existing auth paths
        - /api/v1/auth/login
        - /api/v1/auth/signup
        - /api/v1/auth/refresh
        - /api/v1/auth/email/verify
        - /api/v1/auth/email/resend
        - /api/v1/auth/email/status
        - /api/v1/auth/validate
        - /api/v1/auth/health

        # Password management (public endpoints)
        - /api/v1/password/forgot
        - /api/v1/password/reset

        # Health checks
        - /actuator/health
        - /actuator/info
```

---

## Rate Limiting Configuration to Add

Add rate limiting for new endpoints:

```yaml
gripday:
  gateway:
    rate-limiting:
      policies:
        endpoints:
          # Password management
          "/api/v1/password/forgot":
            requests-per-minute: 3
            burst-capacity: 5
          "/api/v1/password/reset":
            requests-per-minute: 5
            burst-capacity: 10
          "/api/v1/password/change":
            requests-per-minute: 10
            burst-capacity: 15

          # User management (admin)
          "/api/v1/users/**":
            requests-per-minute: 100
            burst-capacity: 150

          # Organization management (admin)
          "/api/v1/organizations/**":
            requests-per-minute: 100
            burst-capacity: 150

          # Tenant management (super admin)
          "/api/v1/admin/tenants/**":
            requests-per-minute: 50
            burst-capacity: 75
```

---

## Summary of Missing Routes

| Endpoint Group          | Base Path                  | Status        | Impact                                          |
| ----------------------- | -------------------------- | ------------- | ----------------------------------------------- |
| Authentication          | `/api/v1/auth/**`          | ✅ Routed     | Working                                         |
| Email Verification      | `/api/v1/auth/email/**`    | ✅ Routed     | Working                                         |
| Password Management     | `/api/v1/password/**`      | ❌ Not Routed | **CRITICAL** - Users cannot reset passwords     |
| User Profile            | `/api/v1/users/me`         | ❌ Not Routed | **HIGH** - Users cannot view their profile      |
| User Management         | `/api/v1/users/**`         | ❌ Not Routed | **HIGH** - Admins cannot manage users           |
| Organization Management | `/api/v1/organizations/**` | ❌ Not Routed | **MEDIUM** - Admins cannot manage orgs          |
| Tenant Management       | `/api/v1/admin/tenants/**` | ❌ Not Routed | **MEDIUM** - Super admins cannot manage tenants |

**Recommendation**: Implement **Option 1** (expand single route) as the quickest fix, then add missing public paths and rate limiting configurations.
