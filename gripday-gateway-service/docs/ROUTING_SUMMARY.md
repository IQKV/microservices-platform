# Gateway Routing Summary

## Overview

This document provides a quick reference for all configured routes in the Gateway Service.

## Route Configuration

### Authentication Routes (User Service)

| Route ID            | Path Pattern      | Target Service | Strip Prefix | Rate Limit |
| ------------------- | ----------------- | -------------- | ------------ | ---------- |
| `user-service-auth` | `/api/v1/auth/**` | User Service   | 0            | 60/min     |

**Examples:**

- `POST /api/v1/auth/login` → User Service
- `POST /api/v1/auth/signup` → User Service
- `POST /api/v1/auth/refresh` → User Service

### User Profile Routes (User Service)

| Route ID               | Path Pattern                  | Target Service | Strip Prefix | Rate Limit |
| ---------------------- | ----------------------------- | -------------- | ------------ | ---------- |
| `user-service-profile` | `/api/v1/me`, `/api/v1/me/**` | User Service   | 0            | 60/min     |

**Examples:**

- `GET /api/v1/me` → User Service (requires auth)
- `PUT /api/v1/me` → User Service (requires auth)
- `PATCH /api/v1/me/password` → User Service (requires auth)

### Admin Routes (User Service)

| Route ID             | Path Pattern       | Target Service | Strip Prefix | Rate Limit |
| -------------------- | ------------------ | -------------- | ------------ | ---------- |
| `user-service-admin` | `/api/v1/admin/**` | User Service   | 0            | 60/min     |

**Examples:**

- `GET /api/v1/admin/users` → User Service (requires ADMIN role)
- `POST /api/v1/admin/users` → User Service (requires ADMIN role)
- `DELETE /api/v1/admin/users/{id}` → User Service (requires ADMIN role)

### JWKS Route (User Service)

| Route ID            | Path Pattern             | Target Service | Strip Prefix | Rate Limit |
| ------------------- | ------------------------ | -------------- | ------------ | ---------- |
| `user-service-jwks` | `/.well-known/jwks.json` | User Service   | 0            | None       |

**Purpose:** Public key distribution for JWT validation

### Bookstore Routes

| Route ID            | Path Pattern           | Target Service    | Strip Prefix | Rate Limit | Circuit Breaker |
| ------------------- | ---------------------- | ----------------- | ------------ | ---------- | --------------- |
| `bookstore-service` | `/api/v1/bookstore/**` | Bookstore Service | 0            | 60/min     | Yes             |

**Examples:**

- `GET /api/v1/bookstore/books` → Bookstore Service
- `GET /api/v1/bookstore/books/{id}` → Bookstore Service
- `GET /api/v1/bookstore/inventory/{id}/availability` → Bookstore Service

### API Documentation Routes

| Route ID                  | Path Pattern                  | Target Service | Rewrite Rule                               |
| ------------------------- | ----------------------------- | -------------- | ------------------------------------------ |
| `user-service-api-docs`   | `/user-service/api-docs/**`   | User Service   | `/user-service/api-docs` → `/api-docs`     |
| `user-service-swagger-ui` | `/user-service/swagger-ui/**` | User Service   | `/user-service/swagger-ui` → `/swagger-ui` |

**Examples:**

- `GET /user-service/swagger-ui.html` → User Service Swagger UI
- `GET /user-service/api-docs` → User Service OpenAPI JSON

### Health Check Route

| Route ID       | Path Pattern       | Target Service | Strip Prefix |
| -------------- | ------------------ | -------------- | ------------ |
| `health-check` | `/actuator/health` | User Service   | 0            |

## Service URIs

### Local Development

```yaml
user-service:
  uri: http://localhost:8080

bookstore-service:
  uri: http://localhost:8081
```

### Environment Variables

Override service URIs using environment variables:

```bash
GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI=http://user-service:8080
GRIPDAY_GATEWAY_ROUTING_BOOKSTORE_SERVICE_URI=http://bookstore-service:8081
```

## Public vs Protected Endpoints

### Public Endpoints (No Authentication Required)

**Authentication:**

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/validate`

**Email Verification:**

- `GET /api/v1/auth/email/verify`
- `POST /api/v1/auth/email/resend`
- `GET /api/v1/auth/email/status`

**Password Reset:**

- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`

**Bookstore:**

- `GET /api/v1/bookstore/books`
- `GET /api/v1/bookstore/books/**`
- `GET /api/v1/bookstore/inventory/*/availability`

**Documentation:**

- `GET /api/v1/docs`
- `GET /swagger-ui/**`
- `GET /api-docs/**`
- `GET /user-service/swagger-ui/**`
- `GET /user-service/api-docs/**`

**Health:**

- `GET /actuator/health`
- `GET /actuator/info`

**JWKS:**

- `GET /.well-known/jwks.json`

### Protected Endpoints (Authentication Required)

**User Profile:**

- `GET /api/v1/me` - Get current user profile
- `PUT /api/v1/me` - Update current user profile
- `PATCH /api/v1/me/password` - Change password
- `POST /api/v1/auth/logout` - Logout current session
- `POST /api/v1/auth/logout-all` - Logout all sessions

**Admin Operations (ADMIN role required):**

- `GET /api/v1/admin/users` - List all users
- `POST /api/v1/admin/users` - Create new user
- `GET /api/v1/admin/users/{id}` - Get user details
- `PUT /api/v1/admin/users/{id}` - Update user
- `DELETE /api/v1/admin/users/{id}` - Delete user
- `PATCH /api/v1/admin/users/{id}/roles` - Update user roles

## Rate Limiting

### Default Policy

- **Requests per minute:** 60
- **Burst capacity:** 100

### Endpoint-Specific Policies

| Endpoint Pattern               | Requests/Min | Burst Capacity |
| ------------------------------ | ------------ | -------------- |
| `/api/v1/auth/login`           | 10           | 20             |
| `/api/v1/auth/signup`          | 5            | 10             |
| `/api/v1/auth/email/resend`    | 3            | 5              |
| `/api/v1/auth/password/forgot` | 3            | 5              |
| `/api/v1/admin/users/**`       | 100          | 150            |
| `/api/v1/me`                   | 100          | 150            |
| `/api/v1/bookstore/books`      | 100          | 200            |

### Local Development Overrides

In local development, rate limits are more lenient:

- Default: 120 requests/min (burst: 200)
- Login: 20 requests/min (burst: 40)
- Tenant quotas: Disabled

## Circuit Breaker Configuration

Circuit breaker is enabled for the Bookstore Service route:

```yaml
failure-rate-threshold: 50%
slow-call-rate-threshold: 50%
slow-call-duration-threshold: 2s
minimum-number-of-calls: 10
wait-duration-in-open-state: 30s
```

**Fallback:** `forward:/fallback/bookstore`

## CORS Configuration

### Allowed Origins (Local Development)

- `http://localhost:3000`
- `http://localhost:5173` (Vite default)
- `http://localhost:8080`
- `http://127.0.0.1:3000`
- `http://127.0.0.1:5173`

### Allowed Methods

- GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD

### Credentials

- Enabled (`allow-credentials: true`)

## Request/Response Transformation

### Request Headers Added

- `X-Gateway-Version: 1.0.0-local`
- `X-Environment: local`
- User context headers (when authenticated)
- Tenant context headers (when available)

### Request Headers Removed

- `Authorization-Internal`
- `X-Internal-Token`

### Response Headers Removed

- `X-Internal-Service`
- `X-Database-Query-Time`

## Testing Routes

### Test Authentication Flow

```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# 2. Get profile (use token from step 1)
curl http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer <token>"

# 3. List users (admin only)
curl http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer <token>"
```

### Test Public Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation
curl http://localhost:8080/api/v1/docs

# Bookstore (public)
curl http://localhost:8080/api/v1/bookstore/books
```

### Test API Documentation

```bash
# Gateway Swagger UI
open http://localhost:8080/swagger-ui.html

# User Service Swagger UI (through gateway)
open http://localhost:8080/user-service/swagger-ui.html

# OpenAPI JSON
curl http://localhost:8080/user-service/api-docs
```

## Monitoring Routes

### Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

### Health Status

```bash
curl http://localhost:8080/actuator/health
```

## Route Priority

Routes are evaluated in the order they are defined. More specific routes should be defined before generic ones:

1. JWKS route (most specific)
2. API documentation routes
3. Authentication routes
4. Profile routes
5. Admin routes
6. Bookstore routes (with circuit breaker)
7. Health check route

## Adding New Routes

To add a new service route:

1. Add service URI to `application.yml`:

```yaml
gripday:
  gateway:
    routing:
      services:
        new-service:
          uri: http://localhost:8082
          path: /v1/newservice/**
```

2. Add route definition:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: ${gripday.gateway.routing.services.new-service.uri}
          predicates:
            - Path=/api/v1/newservice/**
          filters:
            - StripPrefix=0
```

3. Configure public paths if needed:

```yaml
gripday:
  gateway:
    security:
      public-paths:
        - /api/v1/newservice/public/**
```

4. Add API documentation routes:

```yaml
- id: new-service-api-docs
  uri: ${gripday.gateway.routing.services.new-service.uri}
  predicates:
    - Path=/new-service/api-docs,/new-service/api-docs/**
  filters:
    - RewritePath=/new-service/api-docs(?<segment>/?.*), /api-docs$\{segment}
```
