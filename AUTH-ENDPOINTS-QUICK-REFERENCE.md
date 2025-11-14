# User Service Endpoints - Quick Reference

## Base URL

- **Local Development**: `http://localhost:8080`
- **Through Gateway**: All endpoints accessible via gateway

---

## 🔓 Public Endpoints (No Authentication Required)

### Authentication

```bash
POST /api/v1/auth/signup          # Register new user
POST /api/v1/auth/login           # User login
POST /api/v1/auth/refresh         # Refresh JWT token
POST /api/v1/auth/validate        # Validate JWT token
GET  /api/v1/auth/health          # Health check
```

### Email Verification

```bash
GET  /api/v1/auth/email/verify    # Verify email (query param: token)
POST /api/v1/auth/email/resend    # Resend verification email
GET  /api/v1/auth/email/status    # Check verification status (query param: email)
```

### Password Management

```bash
POST /api/v1/password/forgot      # Initiate password reset
POST /api/v1/password/reset       # Complete password reset
```

---

## 🔒 Authenticated Endpoints (JWT Required)

### User Session

```bash
POST /api/v1/auth/logout          # Logout current session
POST /api/v1/auth/logout-all      # Logout all sessions
```

### User Profile

```bash
GET  /api/v1/users/me             # Get current user profile
```

### Password Management

```bash
POST /api/v1/password/change      # Change password (requires current password)
```

---

## 👤 Admin Endpoints (ADMIN or SUPER_ADMIN Role)

### User Management

```bash
GET    /api/v1/users              # List all users (paginated)
GET    /api/v1/users/{id}         # Get user by ID
POST   /api/v1/users              # Create new user
PUT    /api/v1/users/{id}         # Update user
DELETE /api/v1/users/{id}         # Delete user
```

### Organization Management

```bash
GET    /api/v1/organizations              # List all organizations (paginated)
GET    /api/v1/organizations/{id}         # Get organization by ID
POST   /api/v1/organizations              # Create organization
PUT    /api/v1/organizations/{id}         # Update organization
DELETE /api/v1/organizations/{id}         # Delete organization
```

---

## 👑 Super Admin Endpoints (SUPER_ADMIN Role Only)

### Tenant Management

```bash
GET    /api/v1/admin/tenants                      # List all tenants
GET    /api/v1/admin/tenants/{tenantId}           # Get tenant by ID
POST   /api/v1/admin/tenants                      # Create tenant
PUT    /api/v1/admin/tenants/{tenantId}           # Update tenant
PATCH  /api/v1/admin/tenants/{tenantId}/enabled   # Enable/disable tenant
DELETE /api/v1/admin/tenants/{tenantId}           # Delete tenant
GET    /api/v1/admin/tenants/statistics           # Get tenant statistics
```

---

## Rate Limits (Production)

| Endpoint             | Requests/Min | Burst |
| -------------------- | ------------ | ----- |
| `/auth/login`        | 10           | 20    |
| `/auth/signup`       | 5            | 10    |
| `/auth/email/resend` | 3            | 5     |
| `/auth/email/verify` | 10           | 15    |
| `/password/forgot`   | 3            | 5     |
| `/password/reset`    | 5            | 10    |
| `/password/change`   | 10           | 15    |
| `/users/**`          | 100          | 150   |
| `/organizations/**`  | 100          | 150   |
| `/admin/tenants/**`  | 50           | 75    |
| Default              | 60           | 100   |

---

## Example Requests

### Register User

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "SecurePass123!"
  }'
```

### Get Current User Profile

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Forgot Password

```bash
curl -X POST http://localhost:8080/api/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

### Reset Password

```bash
curl -X POST http://localhost:8080/api/v1/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "newPassword": "NewSecurePass123!"
  }'
```

### Change Password (Authenticated)

```bash
curl -X POST http://localhost:8080/api/v1/password/change \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "OldPass123!",
    "newPassword": "NewPass123!"
  }'
```

### List Users (Admin)

```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### Create User (Admin)

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "SecurePass123!",
    "firstName": "New",
    "lastName": "User",
    "roles": ["USER"]
  }'
```

### List Tenants (Super Admin)

```bash
curl -X GET http://localhost:8080/api/v1/admin/tenants \
  -H "Authorization: Bearer SUPER_ADMIN_JWT_TOKEN"
```

### Create Tenant (Super Admin)

```bash
curl -X POST http://localhost:8080/api/v1/admin/tenants \
  -H "Authorization: Bearer SUPER_ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-corp",
    "name": "ACME Corporation",
    "domain": "acme.com",
    "subdomain": "acme",
    "enabled": true
  }'
```

---

## Response Codes

| Code | Meaning                              |
| ---- | ------------------------------------ |
| 200  | Success                              |
| 201  | Created                              |
| 204  | No Content (Success)                 |
| 400  | Bad Request / Validation Error       |
| 401  | Unauthorized / Invalid Credentials   |
| 403  | Forbidden / Insufficient Permissions |
| 404  | Not Found                            |
| 409  | Conflict (e.g., username exists)     |
| 423  | Locked (e.g., account locked)        |
| 429  | Too Many Requests (Rate Limited)     |
| 500  | Internal Server Error                |

---

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "1",
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "permissions": ["READ_PROFILE"],
  "tenantId": "tenant-123",
  "type": "access",
  "iat": 1234567890,
  "exp": 1234568790
}
```

### Refresh Token Claims

```json
{
  "sub": "1",
  "type": "refresh",
  "iat": 1234567890,
  "exp": 1235172690
}
```

---

## Error Response Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": "One or more fields contain invalid values",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/users",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789",
    "requestId": "req-001-2024",
    "fields": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  }
}
```
