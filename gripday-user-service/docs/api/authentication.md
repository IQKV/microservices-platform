# Authentication API Documentation

This document provides documentation for the Gripday User Service REST API endpoints.

## Base URL

- Local Development: `http://localhost:8080`
- Staging: `https://auth.gripday.website`
- Production: `https://user.gripday.com`

## API Versioning

All endpoints are versioned using URL path: `/api/v1/`

## Authentication

Most endpoints require JWT authentication via the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

## Content Type

Successful responses use `application/json`.
Errors use RFC7807 Problem Details with `application/problem+json`.

## Multi-Tenant Support

Include tenant context via header:

```
X-Tenant-ID: <tenant_id>
```

---

## User Registration

### POST /api/v1/auth/signup

Register a new user account.

**Request:**

```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "default"
}
```

**Request Schema:**
| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| username | string | Yes | 3-50 chars, alphanumeric + underscore |
| email | string | Yes | Valid email format |
| password | string | Yes | Min 8 chars, uppercase, lowercase, number, special char |
| firstName | string | Yes | 1-100 chars |
| lastName | string | Yes | 1-100 chars |
| tenantId | string | No | Defaults to "default" |

**Success Response (201 Created):**

```json
{
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": false,
  "tenantId": "default",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Responses:**

**409 Conflict - Username/Email Already Exists:**

```json
{
  "type": "https://problems.gripday.com/user-registration",
  "title": "User registration failed",
  "status": 409,
  "detail": "Username or email is already registered",
  "instance": "/api/v1/auth/signup",
  "code": "USER_ALREADY_EXISTS",
  "path": "/api/v1/auth/signup",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024",
  "fields": [
    {
      "field": "username",
      "code": "Duplicate",
      "message": "Username 'johndoe' is already taken",
      "rejectedValue": "johndoe"
    }
  ]
}
```

**400 Bad Request - Validation Error:**

```json
{
  "type": "https://problems.gripday.com/validation-error",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields contain invalid values",
  "instance": "/api/v1/auth/signup",
  "code": "VALIDATION_ERROR",
  "path": "/api/v1/auth/signup",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024",
  "fields": [
    {
      "field": "password",
      "code": "Pattern",
      "message": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character",
      "rejectedValue": "***"
    },
    {
      "field": "email",
      "code": "Email",
      "message": "Invalid email format",
      "rejectedValue": "invalid"
    }
  ]
}
```

---

## User Login

### POST /api/v1/auth/login

Authenticate user and obtain JWT tokens.

**Request:**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!",
  "rememberMe": false
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| username | string | Yes | Username or email address |
| password | string | Yes | User password |
| rememberMe | boolean | No | Extend refresh token expiry (default: false) |

**Success Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNjQyMjQ4MDAwLCJleHAiOjE2NDIyNDg5MDAsInJvbGVzIjpbIlVTRVIiXSwidGVuYW50SWQiOiJkZWZhdWx0In0...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNjQyMjQ4MDAwLCJleHAiOjE2NDI4NTI4MDAsInR5cGUiOiJyZWZyZXNoIn0...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800,
  "userContext": {
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "permissions": ["read:profile", "update:profile"],
    "tenantId": "default",
    "department": "Engineering",
    "customClaims": {}
  }
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| accessToken | string | JWT access token (15 minutes) |
| refreshToken | string | JWT refresh token (7 days or 30 days if rememberMe) |
| tokenType | string | Always "Bearer" |
| expiresIn | number | Access token expiry in seconds |
| refreshExpiresIn | number | Refresh token expiry in seconds |
| userContext | object | User information and context |

**Error Responses:**

**401 Unauthorized - Invalid Credentials:**

```json
{
  "type": "https://problems.gripday.com/authentication-error",
  "title": "Authentication failed",
  "status": 401,
  "detail": "Invalid username or password",
  "instance": "/api/v1/auth/login",
  "code": "AUTH_INVALID_CREDENTIALS",
  "path": "/api/v1/auth/login",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024"
}
```

**423 Locked - Account Locked:**

```json
{
  "type": "https://problems.gripday.com/account-locked",
  "title": "Account temporarily locked",
  "status": 423,
  "detail": "Account locked due to multiple failed login attempts. Try again in 15 minutes.",
  "instance": "/api/v1/auth/login",
  "code": "AUTH_ACCOUNT_LOCKED",
  "path": "/api/v1/auth/login",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024"
}
```

---

## Token Refresh

### POST /api/v1/auth/refresh

Refresh JWT access token using refresh token.

**Request:**

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refreshToken | string | Yes | Valid JWT refresh token |

**Success Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800,
  "userContext": {
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"],
    "tenantId": "default"
  }
}
```

**Error Responses:**

**401 Unauthorized - Invalid Refresh Token:**

```json
{
  "type": "https://problems.gripday.com/authentication-error",
  "title": "Invalid refresh token",
  "status": 401,
  "detail": "Refresh token is expired, invalid, or has been revoked",
  "instance": "/api/v1/auth/refresh",
  "code": "AUTH_INVALID_TOKEN",
  "path": "/api/v1/auth/refresh",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024"
}
```

---

## User Logout

### POST /api/v1/auth/logout

Invalidate current JWT tokens and logout user.

**Request:**

```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Success Response (200 OK):**

```json
{
  "message": "Successfully logged out",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Error Responses:**

**401 Unauthorized - Invalid Token:**

```json
{
  "type": "https://problems.gripday.com/authentication-error",
  "title": "Invalid or expired token",
  "status": 401,
  "detail": "JWT token is invalid, expired, or malformed",
  "instance": "/api/v1/auth/logout",
  "code": "AUTH_INVALID_TOKEN",
  "path": "/api/v1/auth/logout",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024"
}
```

---

## Token Validation (Internal)

### POST /api/v1/auth/validate

Validate JWT token (used by other microservices).

**Request:**

```http
POST /api/v1/auth/validate
Content-Type: application/json

{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200 OK):**

```json
{
  "valid": true,
  "userContext": {
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"],
    "permissions": ["read:profile", "update:profile"],
    "tenantId": "default",
    "department": "Engineering"
  },
  "expiresAt": "2024-01-15T10:45:00Z"
}
```

**Error Response (401 Unauthorized):**

```json
{
  "type": "https://problems.gripday.com/authentication-error",
  "title": "Token validation failed",
  "status": 401,
  "detail": "JWT token is invalid, expired, or malformed"
}
```

---

## Error Codes Reference

| Code                         | HTTP Status | Description                     |
| ---------------------------- | ----------- | ------------------------------- |
| VALIDATION_ERROR             | 400         | Request validation failed       |
| AUTH_INVALID_CREDENTIALS     | 401         | Invalid username/password       |
| AUTH_INVALID_TOKEN           | 401         | Invalid or expired JWT token    |
| AUTH_INSUFFICIENT_PRIVILEGES | 403         | User lacks required permissions |
| AUTH_ACCOUNT_LOCKED          | 423         | Account temporarily locked      |
| RESOURCE_NOT_FOUND           | 404         | Requested resource not found    |
| RATE_LIMIT_EXCEEDED          | 429         | Too many requests               |
| SYSTEM_ERROR                 | 500         | Internal server error           |

---

## Rate Limiting

Authentication endpoints are rate limited:

- **Login**: 5 attempts per minute per IP
- **Signup**: 3 attempts per minute per IP
- **Refresh**: 10 attempts per minute per user
- **Logout**: 20 attempts per minute per user

Rate limit headers are included in responses:

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
X-RateLimit-Reset: 1642248060
```

---

## OpenAPI/Swagger UI

Interactive API documentation is available at:

- Local: `http://localhost:8080/swagger-ui.html`
- Staging: `https://auth.gripday.website/swagger-ui.html`
- Production: `https://user.gripday.com/swagger-ui.html`

Download OpenAPI specification:

- JSON: `/v3/api-docs`
- YAML: `/v3/api-docs.yaml`

---

## cURL Examples

### Complete Authentication Flow

1. **Register new user:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User",
    "tenantId": "default"
  }'
```

2. **Login and get tokens:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }' | jq -r '.accessToken'
```

3. **Use access token:**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' | jq -r '.accessToken')

curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/me
```

4. **Refresh token:**

```bash
REFRESH_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' | jq -r '.refreshToken')

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

5. **Logout:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Password Reset

### POST /api/v1/auth/password/forgot

Initiate password reset flow by sending a reset email.

**Request:**

```http
POST /api/v1/auth/password/forgot
Content-Type: application/json

{
  "email": "john@example.com"
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | string | Yes | User's email address |

**Success Response (202 Accepted):**

Always returns 202 Accepted to prevent user enumeration, regardless of whether the email exists.

```http
HTTP/1.1 202 Accepted
```

**Security Features:**

- Non-enumerating response (always 202)
- Reset token expires in 30 minutes
- Rate limiting per IP address
- Audit logging of reset attempts

---

### HEAD /api/v1/auth/password/reset

Validate if a password reset token is valid and not expired.

**Request:**

```http
HEAD /api/v1/auth/password/reset?token=550e8400-e29b-41d4-a716-446655440000
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | string | Yes | Password reset token from email |

**Success Response (200 OK):**

```http
HTTP/1.1 200 OK
```

Token is valid and can be used for password reset.

**Error Response (404 Not Found):**

```http
HTTP/1.1 404 Not Found
```

Token is invalid, expired, or doesn't exist.

**Use Case:**

This endpoint is designed for UI applications to validate the reset token before displaying the password reset form. If the token is invalid or expired, the UI can show a 404 error page instead of the reset form.

**Example Usage:**

```javascript
// Check token validity before showing reset form
const response = await fetch(`/api/v1/auth/password/reset?token=${token}`, {
  method: "HEAD",
});

if (response.status === 404) {
  // Show 404 page - invalid or expired token
  showErrorPage("Invalid or expired reset link");
} else {
  // Show password reset form
  showResetForm(token);
}
```

---

### POST /api/v1/auth/password/reset

Complete password reset using a valid reset token.

**Request:**

```http
POST /api/v1/auth/password/reset
Content-Type: application/json

{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NewSecurePass123!"
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| token | string | Yes | Password reset token from email |
| newPassword | string | Yes | New password (min 8 chars, must include uppercase, lowercase, number, special char) |

**Success Response (204 No Content):**

```http
HTTP/1.1 204 No Content
```

Password has been reset successfully.

**Error Response (400 Bad Request):**

```json
{
  "type": "https://problems.gripday.com/password-reset-error",
  "title": "Password reset failed",
  "status": 400,
  "detail": "Invalid or expired reset token",
  "instance": "/api/v1/auth/password/reset",
  "code": "AUTH_INVALID_TOKEN",
  "path": "/api/v1/auth/password/reset",
  "method": "POST",
  "correlationId": "abc123-def456-ghi789",
  "requestId": "req-001-2024"
}
```

**Security Actions on Success:**

- User password is updated
- All refresh tokens are revoked
- All active sessions are invalidated
- Reset token is deleted (single-use)
- Confirmation email is sent to user

---
