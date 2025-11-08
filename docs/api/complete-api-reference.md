# Complete API Reference

This document provides API documentation for all Gripday platform endpoints with complete examples, request/response formats, and error handling.

## Base URLs

- **Gateway Service**: `http://localhost:8080` (Production: `https://api.gripday.com`)
- **Auth Service**: `http://localhost:8081` (Internal service, accessed via Gateway)

## Authentication

All protected endpoints require JWT authentication via the `Authorization` header:

```
Authorization: Bearer <jwt-token>
```

Multi-tenant requests also require the tenant identifier:

```
X-Tenant-ID: <tenant-id>
```

## Auth Service API

### Authentication Endpoints

#### POST /api/v1/auth/signup

Register a new user account.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "default"
  }'
```

**Request Body:**

```json
{
  "username": "string (3-50 chars, alphanumeric + underscore)",
  "email": "string (valid email format)",
  "password": "string (8+ chars, uppercase, lowercase, number, special char)",
  "firstName": "string (1-50 chars)",
  "lastName": "string (1-50 chars)",
  "tenantId": "string (optional, defaults to header value)"
}
```

**Success Response (201 Created):**

```json
{
  "userId": 123,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "default",
  "enabled": true,
  "emailVerified": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "authorities": ["USER"]
}
```

**Error Responses:**

```json
// 409 Conflict - Username/Email already exists
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Username already exists",
    "details": "A user with username 'johndoe' already exists",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/auth/signup",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789"
  }
}

// 400 Bad Request - Validation errors
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": "One or more fields contain invalid values",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/auth/signup",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789",
    "fields": [
      {
        "field": "password",
        "message": "Password must contain at least one uppercase letter"
      },
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  }
}
```

#### POST /api/v1/auth/login

Authenticate user and receive JWT tokens.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!",
    "rememberMe": true
  }'
```

**Request Body:**

```json
{
  "username": "string (username or email)",
  "password": "string",
  "rememberMe": "boolean (optional, extends refresh token expiry)"
}
```

**Success Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800,
  "user": {
    "userId": 123,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "default",
    "authorities": ["USER"],
    "lastLoginAt": "2024-01-15T10:30:00Z"
  }
}
```

**Error Responses:**

```json
// 401 Unauthorized - Invalid credentials
{
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Invalid username or password",
    "details": "Authentication failed for user 'johndoe'",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/auth/login",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789"
  }
}

// 423 Locked - Account locked
{
  "error": {
    "code": "AUTH_ACCOUNT_LOCKED",
    "message": "Account is temporarily locked",
    "details": "Account locked due to multiple failed login attempts. Try again in 15 minutes.",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/auth/login",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789",
    "retryAfter": "2024-01-15T10:45:00Z"
  }
}
```

#### POST /api/v1/auth/refresh

Refresh JWT access token using refresh token.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Request Body:**

```json
{
  "refreshToken": "string (valid JWT refresh token)"
}
```

**Success Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800
}
```

**Error Responses:**

```json
// 401 Unauthorized - Invalid refresh token
{
  "error": {
    "code": "AUTH_INVALID_TOKEN",
    "message": "Invalid or expired refresh token",
    "details": "The provided refresh token is invalid or has expired",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/auth/refresh",
    "method": "POST",
    "correlationId": "abc123-def456-ghi789"
  }
}
```

#### POST /api/v1/auth/logout

Logout user and invalidate tokens.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Success Response (200 OK):**

```json
{
  "message": "Successfully logged out",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### GET /api/v1/auth/profile

Get current user profile information.

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/auth/profile \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Success Response (200 OK):**

```json
{
  "userId": 123,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "default",
  "enabled": true,
  "emailVerified": false,
  "authorities": ["USER"],
  "createdAt": "2024-01-15T10:30:00Z",
  "lastLoginAt": "2024-01-15T10:30:00Z"
}
```

### User Management Endpoints (Admin Only)

#### GET /api/v1/users

List users with pagination and filtering (Admin/Super Admin only).

**Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20&sort=createdAt,desc&search=john" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Query Parameters:**

- `page`: Page number (0-based, default: 0)
- `size`: Page size (1-100, default: 20)
- `sort`: Sort criteria (field,direction)
- `search`: Search term for username, email, firstName, lastName
- `enabled`: Filter by enabled status (true/false)
- `authority`: Filter by authority/role

**Success Response (200 OK):**

```json
{
  "content": [
    {
      "userId": 123,
      "username": "johndoe",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "tenantId": "default",
      "enabled": true,
      "emailVerified": false,
      "authorities": ["USER"],
      "createdAt": "2024-01-15T10:30:00Z",
      "lastLoginAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false
  }
}
```

#### GET /api/v1/users/{userId}

Get specific user by ID (Admin/Super Admin only).

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/users/123 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Success Response (200 OK):**

```json
{
  "userId": 123,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "tenantId": "default",
  "enabled": true,
  "emailVerified": false,
  "authorities": ["USER"],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "lastLoginAt": "2024-01-15T10:30:00Z",
  "loginAttempts": 0,
  "lockedUntil": null
}
```

#### PUT /api/v1/users/{userId}

Update user information (Admin/Super Admin only).

**Request:**

```bash
curl -X PUT http://localhost:8080/api/v1/users/123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default" \
  -d '{
    "firstName": "John Updated",
    "lastName": "Doe Updated",
    "email": "john.updated@example.com",
    "enabled": true
  }'
```

**Request Body:**

```json
{
  "firstName": "string (optional)",
  "lastName": "string (optional)",
  "email": "string (optional, valid email)",
  "enabled": "boolean (optional)"
}
```

**Success Response (200 OK):**

```json
{
  "userId": 123,
  "username": "johndoe",
  "email": "john.updated@example.com",
  "firstName": "John Updated",
  "lastName": "Doe Updated",
  "tenantId": "default",
  "enabled": true,
  "emailVerified": false,
  "authorities": ["USER"],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T11:00:00Z"
}
```

#### DELETE /api/v1/users/{userId}

Delete user account (Super Admin only).

**Request:**

```bash
curl -X DELETE http://localhost:8080/api/v1/users/123 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Success Response (204 No Content):**

```
(Empty response body)
```

#### POST /api/v1/users/{userId}/authorities

Assign authority/role to user (Super Admin only).

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/users/123/authorities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default" \
  -d '{
    "authorityName": "ADMIN"
  }'
```

**Request Body:**

```json
{
  "authorityName": "string (USER, ADMIN, SUPER_ADMIN)"
}
```

**Success Response (200 OK):**

```json
{
  "message": "Authority assigned successfully",
  "userId": 123,
  "authorityName": "ADMIN",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### DELETE /api/v1/users/{userId}/authorities/{authorityName}

Remove authority/role from user (Super Admin only).

**Request:**

```bash
curl -X DELETE http://localhost:8080/api/v1/users/123/authorities/ADMIN \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-ID: default"
```

**Success Response (200 OK):**

```json
{
  "message": "Authority removed successfully",
  "userId": 123,
  "authorityName": "ADMIN",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Gateway Service API

### Health and Status Endpoints

#### GET /actuator/health

Get service health status.

**Request:**

```bash
curl -X GET http://localhost:8080/actuator/health
```

**Success Response (200 OK):**

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 91943432192,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.5"
      }
    }
  }
}
```

#### GET /actuator/metrics

Get available metrics.

**Request:**

```bash
curl -X GET http://localhost:8080/actuator/metrics
```

**Success Response (200 OK):**

```json
{
  "names": [
    "gateway.requests",
    "gateway.requests.duration",
    "gateway.rate.limit.exceeded",
    "circuit.breaker.calls",
    "circuit.breaker.state",
    "jvm.memory.used",
    "jvm.threads.live",
    "http.server.requests"
  ]
}
```

#### GET /actuator/metrics/{metricName}

Get specific metric details.

**Request:**

```bash
curl -X GET http://localhost:8080/actuator/metrics/gateway.requests
```

**Success Response (200 OK):**

```json
{
  "name": "gateway.requests",
  "description": "Gateway request count",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1247.0
    }
  ],
  "availableTags": [
    {
      "tag": "method",
      "values": ["GET", "POST", "PUT", "DELETE"]
    },
    {
      "tag": "status",
      "values": ["200", "401", "404", "429", "500"]
    },
    {
      "tag": "uri",
      "values": ["/api/v1/auth/login", "/api/v1/auth/signup", "/api/v1/users"]
    }
  ]
}
```

## Error Response Format

All API endpoints use a consistent error response format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": "Additional error details",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/endpoint",
    "method": "HTTP_METHOD",
    "correlationId": "unique-correlation-id",
    "requestId": "unique-request-id",
    "fields": [
      {
        "field": "fieldName",
        "message": "Field-specific error message"
      }
    ]
  }
}
```

### Error Codes

#### Authentication Errors (AUTH\_\*)

- `AUTH_INVALID_CREDENTIALS`: Invalid username or password
- `AUTH_INVALID_TOKEN`: Invalid or expired JWT token
- `AUTH_TOKEN_EXPIRED`: JWT token has expired
- `AUTH_ACCOUNT_LOCKED`: Account is temporarily locked
- `AUTH_ACCOUNT_DISABLED`: Account is disabled
- `AUTH_INSUFFICIENT_PRIVILEGES`: Insufficient permissions for operation

#### Validation Errors (VALIDATION\_\*)

- `VALIDATION_ERROR`: General validation error
- `VALIDATION_REQUIRED_FIELD`: Required field is missing
- `VALIDATION_INVALID_FORMAT`: Field format is invalid
- `VALIDATION_DUPLICATE_VALUE`: Value already exists

#### Resource Errors (RESOURCE\_\*)

- `RESOURCE_NOT_FOUND`: Requested resource not found
- `RESOURCE_CONFLICT`: Resource conflict (e.g., duplicate)
- `RESOURCE_FORBIDDEN`: Access to resource is forbidden

#### Rate Limiting Errors (RATE\_\*)

- `RATE_LIMIT_EXCEEDED`: Rate limit exceeded
- `RATE_LIMIT_QUOTA_EXCEEDED`: Quota limit exceeded

#### System Errors (SYSTEM\_\*)

- `SYSTEM_ERROR`: Internal system error
- `SYSTEM_UNAVAILABLE`: System temporarily unavailable
- `SYSTEM_TIMEOUT`: Operation timed out

## HTTP Status Codes

### Success Codes (2xx)

- `200 OK`: Request successful
- `201 Created`: Resource created successfully
- `204 No Content`: Request successful, no content to return

### Client Error Codes (4xx)

- `400 Bad Request`: Invalid request format or validation error
- `401 Unauthorized`: Authentication required or invalid
- `403 Forbidden`: Access denied due to insufficient permissions
- `404 Not Found`: Requested resource not found
- `409 Conflict`: Resource conflict (e.g., duplicate username)
- `423 Locked`: Account or resource is locked
- `429 Too Many Requests`: Rate limit exceeded

### Server Error Codes (5xx)

- `500 Internal Server Error`: Unexpected server error
- `502 Bad Gateway`: Gateway received invalid response from upstream
- `503 Service Unavailable`: Service temporarily unavailable
- `504 Gateway Timeout`: Gateway timeout waiting for upstream response

## Rate Limiting

### Rate Limit Headers

All responses include rate limiting information:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248600
X-RateLimit-Retry-After: 60
```

### Rate Limit Configuration

Default rate limits per tenant:

- **Authentication endpoints**: 10 requests per minute
- **User management endpoints**: 50 requests per minute
- **General API endpoints**: 100 requests per minute

## Multi-Tenant Support

### Tenant Identification

Tenants can be identified through multiple methods:

1. **Header-based** (Recommended):

```bash
curl -H "X-Tenant-ID: tenant-123" http://localhost:8080/api/v1/users
```

2. **JWT token claims** (Automatic):

```json
{
  "sub": "johndoe",
  "tenantId": "tenant-123",
  "iat": 1642248600,
  "exp": 1642249500
}
```

3. **Subdomain-based** (When configured):

```bash
curl http://tenant-123.api.gripday.com/api/v1/users
```

### Tenant Isolation

- All data is automatically filtered by tenant context
- Cross-tenant access is prevented at the database level
- JWT tokens include tenant claims for context propagation
- Rate limiting is applied per tenant

## SDK and Client Libraries

### JavaScript/TypeScript

```javascript
import { GripdayClient } from "@gripday/client-js";

const client = new GripdayClient({
  baseUrl: "http://localhost:8080",
  tenantId: "default",
});

// Authenticate
const { accessToken } = await client.auth.login({
  username: "johndoe",
  password: "SecurePass123!",
});

// Set token for subsequent requests
client.setAccessToken(accessToken);

// Get user profile
const profile = await client.auth.getProfile();
```

### Java

```java
import com.gripday.client.GripdayClient;
import com.gripday.client.auth.AuthService;

GripdayClient client = GripdayClient.builder()
    .baseUrl("http://localhost:8080")
    .tenantId("default")
    .build();

// Authenticate
LoginResponse response = client.auth().login(LoginRequest.builder()
    .username("johndoe")
    .password("SecurePass123!")
    .build());

// Set token
client.setAccessToken(response.getAccessToken());

// Get profile
UserProfile profile = client.auth().getProfile();
```

### Python

```python
from gripday_client import GripdayClient

client = GripdayClient(
    base_url='http://localhost:8080',
    tenant_id='default'
)

# Authenticate
response = client.auth.login(
    username='johndoe',
    password='SecurePass123!'
)

# Set token
client.set_access_token(response.access_token)

# Get profile
profile = client.auth.get_profile()
```

## Postman Collection

A Postman collection is available with:

- Pre-configured environments (local, staging, production)
- Authentication flow automation
- Variable management for tokens and tenant IDs
- Test scripts for response validation

Download: [Gripday Platform Postman Collection](postman/gripday-platform.postman_collection.json)

## OpenAPI Specifications

Interactive API documentation is available via Swagger UI:

- **Gateway Service**: http://localhost:8080/swagger-ui.html
- **Auth Service**: http://localhost:8081/swagger-ui.html

Download OpenAPI specifications:

- **Gateway Service**: http://localhost:8080/v3/api-docs
- **Auth Service**: http://localhost:8081/v3/api-docs
