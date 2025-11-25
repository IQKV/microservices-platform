# 🚀 Gripday Platform - Complete Development Specification

> **Generated:** November 25, 2025  
> **Version:** 1.0.0  
> **Status:** Ready for Development with Kiro

## 📋 Executive Summary

Gripday is a production-ready, full-stack microservices platform demonstrating modern architecture patterns for building scalable SaaS applications. The platform consists of:

- **Backend**: Spring Boot 3.5.6 microservices (Java 21) with JWT authentication, API gateway, and domain services
- **Frontend**: Two React 19 applications using Feature-Sliced Design architecture
  - **Auth Portal** (`auth.gripday.com`): Dedicated authentication gateway
  - **Main Application** (`app.gripday.com`): User management, dashboard, and business features

**Key Differentiators:**

- Multi-tenant architecture with schema-per-tenant isolation
- RFC 9457 Problem Details compliant error handling
- Comprehensive observability (OpenTelemetry, Prometheus, Grafana, Loki)
- Production-ready with Docker, Kubernetes, and CI/CD pipelines
- AI-assisted development guidelines (AGENTS.md)

---

## 🏗️ Architecture Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Layer                            │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │  Auth Portal     │         │  Main App        │         │
│  │  (React 19)      │         │  (React 19)      │         │
│  │  Port: 5173      │         │  Port: 5173      │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
└───────────┼──────────────────────────────┼──────────────────┘
            │                              │
            └──────────────┬───────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│                   API Gateway Layer                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Gateway Service (Spring Cloud Gateway - Reactive)    │ │
│  │  Port: 8081                                            │ │
│  │  • JWT Validation                                      │ │
│  │  • Rate Limiting (Redis)                               │ │
│  │  • Circuit Breaker                                     │ │
│  │  • Request Routing                                     │ │
│  │  • Tenant Context Extraction                           │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────┘
                           │
           ┌──────────────┼──────────────┬──────────────┐
           │              │              │              │
┌──────────▼────────┐ ┌──▼────────┐ ┌──▼────────────┐ │
│  User Service     │ │ Bookstore │ │ Future        │ │
│  Port: 8080       │ │ Service   │ │ Services      │ │
│  • Authentication │ │ Port: 8082│ │               │ │
│  • User Mgmt      │ │ • Catalog │ │               │ │
│  • JWT Tokens     │ │ • Inventory│ │               │ │
│  • Multi-Tenancy  │ │ • DDD     │ │               │ │
└──────────┬────────┘ └──┬────────┘ └───────────────┘ │
           │              │                             │
┌──────────▼──────────────▼─────────────────────────────┐
│              Data & Infrastructure Layer              │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ PostgreSQL   │  │    Redis     │  │ Observ.    │ │
│  │ (Per Service)│  │ • Cache      │  │ Stack      │ │
│  │              │  │ • Rate Limit │  │ • Prom     │ │
│  │              │  │ • Sessions   │  │ • Grafana  │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
└───────────────────────────────────────────────────────┘
```

### Technology Stack

#### Backend Services

- **Runtime**: Java 21 (records, pattern matching, text blocks, sealed classes)
- **Framework**: Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Gateway**: Spring Cloud Gateway (Reactive WebFlux)
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Caching**: Redis (distributed cache, rate limiting, token blacklist)
- **Security**: JWT RSA256, Spring Security OAuth2 Resource Server
- **Resilience**: Resilience4j (circuit breaker, rate limiter)
- **Observability**: OpenTelemetry, Prometheus, Grafana, Loki
- **API Docs**: SpringDoc OpenAPI 3 with Swagger UI
- **Testing**: JUnit 5, Testcontainers, ArchUnit, Spring Modulith

#### Frontend Applications

- **Runtime**: React 19 with concurrent features
- **Language**: TypeScript 5.9 (strict mode)
- **Build Tool**: Vite 7 with SWC compiler
- **Package Manager**: PNPM 10.20
- **UI Library**: Mantine UI v8 with extensions
- **Routing**: TanStack Router v1 (type-safe, file-based)
- **State Management**: TanStack Query v5 (server), Zustand (client)
- **Forms**: Mantine Form with Zod validation
- **i18n**: Lingui v5 with macro support
- **Testing**: Vitest 3, Playwright 1.56, Testing Library
- **Quality**: ESLint 9, Prettier, Stylelint, Husky, Commitlint

---

## 🔐 Backend Services Specification

### 1. User Service (Port 8080)

**Purpose**: Centralized authentication and identity management hub

#### Core Capabilities

**Authentication & Authorization**

- JWT-based stateless authentication with RSA256 signing
- Access tokens (15min) and refresh tokens (7 days)
- Token rotation and Redis-backed blacklisting with TTL
- JTI (JWT ID) for unique token identification
- Role-based access control (RBAC) with @PreAuthorize
- Permission-based access control for granular operations
- User context extraction with pattern matching (Java 21)

**User Management**

- User registration with email verification (UUID tokens, 24h expiry)
- Password reset flows with secure time-limited tokens
- Account lockout after 5 failed attempts (15min duration)
- Failed attempts counter with 30min sliding window
- Admin-controlled user CRUD operations
- Role assignment and management
- Security audit logging with UserAuditLog entity

**Email Verification**

- UUID-based verification tokens (24h expiry, 48h cleanup)
- Single-use token enforcement with database flag
- Rate limiting (3 emails/hour per user) with sliding window
- Token invalidation on new generation
- Transactional email templates with Thymeleaf
- Multi-language support with i18n
- Scheduled cleanup with @Scheduled (daily at 2 AM)

**Multi-Tenancy**

- Schema-per-tenant isolation with Hibernate MultiTenantConnectionProvider
- TenantContext for thread-local tenant management
- Tenant extraction from JWT claims and headers
- Per-tenant Liquibase migrations
- Cross-tenant operations via TenantContext.executeInTenantContext()
- Organization management with owner assignment
- Organization preferences for security policies

**User Preferences (Self-Service)**

- Locale and timezone settings
- Theme preferences (light/dark)
- Notification preferences
- Profile customization (photo, phone, bio)
- Personal security settings (2FA preferences)

#### API Endpoints

**Public Endpoints**

```
POST   /api/v1/auth/signup              - Register new user
POST   /api/v1/auth/login               - Authenticate user
POST   /api/v1/auth/refresh             - Refresh access token
POST   /api/v1/auth/validate            - Validate JWT token
GET    /api/v1/auth/email/verify        - Verify email address
POST   /api/v1/auth/email/resend        - Resend verification email
POST   /api/v1/auth/password/forgot     - Initiate password reset
POST   /api/v1/auth/password/reset      - Reset password with token
GET    /.well-known/jwks.json           - JWK Set for token validation
```

**Protected Endpoints (Requires Authentication)**

```
GET    /api/v1/users/me                 - Get current user profile
PATCH  /api/v1/users/me/password        - Change password
POST   /api/v1/auth/logout              - Logout current session
POST   /api/v1/auth/logout-all          - Logout all sessions
GET    /api/v1/auth/email/status        - Get email verification status
GET    /api/v1/users/me/preferences     - Get user preferences
PATCH  /api/v1/users/me/preferences     - Update user preferences
DELETE /api/v1/users/me/preferences     - Reset to defaults
```

**Admin Endpoints (Requires ADMIN/SUPER_ADMIN Role)**

```
# User Management
GET    /api/v1/admin/users              - List users (paginated)
GET    /api/v1/admin/users/{id}         - Get user by ID
POST   /api/v1/admin/users              - Create new user
PUT    /api/v1/admin/users/{id}         - Update user
DELETE /api/v1/admin/users/{id}         - Delete user

# Organization Management
GET    /api/v1/admin/organizations      - List organizations
GET    /api/v1/admin/organizations/{id} - Get organization by ID
POST   /api/v1/admin/organizations      - Create organization
PUT    /api/v1/admin/organizations/{id} - Update organization
DELETE /api/v1/admin/organizations/{id} - Delete organization

# Tenant Management
GET    /api/v1/admin/tenants            - List tenants with statistics
GET    /api/v1/admin/tenants/{id}       - Get tenant by ID
POST   /api/v1/admin/tenants            - Create tenant
PUT    /api/v1/admin/tenants/{id}       - Update tenant
DELETE /api/v1/admin/tenants/{id}       - Delete tenant
GET    /api/v1/admin/tenants/statistics - Get tenant statistics
```

#### JWT Token Structure

**Access Token Claims**

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
  "exp": 1634568790,
  "jti": "unique-token-id"
}
```

#### Database Schema

**Core Tables**

- `users` - User accounts with credentials
- `roles` - Role definitions (USER, ADMIN, SUPER_ADMIN)
- `permissions` - Granular permissions
- `user_roles` - User-role associations
- `role_permissions` - Role-permission associations
- `organizations` - Organization entities
- `tenants` - Tenant configurations
- `user_preferences` - User-specific settings
- `organization_preferences` - Organization-wide settings
- `user_audit_log` - Security audit trail
- `email_verification_tokens` - Email verification tokens
- `password_reset_tokens` - Password reset tokens

**Multi-Tenancy**

- Schema-per-tenant strategy
- Each tenant has isolated schema
- Tenant context propagated via TenantContext
- No cross-tenant queries possible

#### Configuration

**Environment Variables**

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gripday_user
SPRING_DATASOURCE_USERNAME=gripday_user
SPRING_DATASOURCE_PASSWORD=secure_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT
JWT_PRIVATE_KEY_PATH=/path/to/private-key.pem
JWT_PUBLIC_KEY_PATH=/path/to/public-key.pem
JWT_ACCESS_TOKEN_EXPIRATION=900000    # 15 minutes
JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7 days

# Email
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=noreply@gripday.com
SPRING_MAIL_PASSWORD=email_password

# Observability
MANAGEMENT_TRACING_ENABLED=true
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

### 2. Gateway Service (Port 8081)

**Purpose**: Reactive API gateway providing intelligent routing, authentication, rate limiting, and circuit breaker patterns

#### Core Capabilities

**Intelligent Routing**

- Dynamic request routing to downstream services
- Path-based and header-based API versioning
- Service discovery ready (configurable)
- Load balancing across service instances
- Health check-based routing

**Authentication Gateway**

- JWT validation using RSA256 with JWK Set endpoint
- User context extraction (userId, username, email, roles, permissions)
- Context propagation via headers (X-User-ID, X-Username, X-User-Roles)
- Public path pattern matching (exact and wildcard /\*\*)
- Configurable user context propagation toggle

**Rate Limiting**

- Redis-backed sliding window log algorithm with sorted sets (ZSET)
- Dual-layer rate limiting (global IP-based + tenant-specific)
- Endpoint-specific rate limit policies with pattern matching
- Burst capacity handling (2x quota) for traffic spikes
- Configurable quotas per tenant
- Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After)
- Automatic cleanup of expired entries with TTL

**Circuit Breaker**

- Resilience4j with reactive CircuitBreakerOperator
- Path-based circuit breaker selection (per service)
- Configurable failure rate and slow call thresholds
- Automatic state transitions (closed → open → half-open)
- Fallback responses with retry-after headers
- Sliding window for failure tracking

**Multi-Tenancy Support**

- Priority-based tenant extraction (JWT claims → X-Tenant-ID header)
- Tenant-specific rate limit quotas with default fallback
- Tenant context stored in exchange attributes
- Tenant ID propagation via X-Tenant-ID header
- Tenant-scoped Redis keys for isolation

**Observability**

- Correlation ID generation and propagation
- OpenTelemetry distributed tracing
- Prometheus metrics for gateway operations
- Structured JSON logging with MDC context
- Request/response logging with correlation tracking

#### Reactive Filter Chain

**Request Flow**

```
1. CorrelationIdFilter        → Generate/extract correlation ID
2. TenantExtractionFilter     → Extract tenant context
3. JwtAuthenticationFilter    → Validate JWT and extract user context
4. ApiVersionRoutingFilter    → Handle API versioning
5. TenantRateLimitingFilter   → Apply rate limits
6. CircuitBreakerFilter       → Fault tolerance
7. RequestTransformationFilter → Enrich headers
8. Route to downstream service
9. ResponseTransformationFilter → Clean response headers
```

#### Routing Configuration

**Routes Defined in application.yml**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: http://user-service:8080
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 60
                redis-rate-limiter.burst-capacity: 100

        - id: user-service-users
          uri: http://user-service:8080
          predicates:
            - Path=/api/v1/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: user-service
                fallbackUri: forward:/fallback/user-service

        - id: bookstore-service
          uri: http://bookstore-service:8082
          predicates:
            - Path=/api/v1/bookstore/**
```

#### Rate Limit Policies

**Endpoint-Specific Quotas**

```yaml
rate-limiting:
  policies:
    - path: /api/v1/auth/login
      quota: 10
      window: 60s
      burst: 15

    - path: /api/v1/auth/signup
      quota: 5
      window: 60s
      burst: 7

    - path: /api/v1/bookstore/**
      quota: 100
      window: 60s
      burst: 200
```

#### Configuration

**Environment Variables**

```bash
# Gateway
SERVER_PORT=8081

# User Service
USER_SERVICE_URL=http://user-service:8080

# Bookstore Service
BOOKSTORE_SERVICE_URL=http://bookstore-service:8082

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT Validation
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://user-service:8080/.well-known/jwks.json

# Circuit Breaker
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_USER_SERVICE_FAILURE_RATE_THRESHOLD=50
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_USER_SERVICE_WAIT_DURATION_IN_OPEN_STATE=60000
```

### 3. Bookstore Service (Port 8082)

**Purpose**: Reference implementation demonstrating Domain-Driven Design, tactical patterns, and production-ready microservice architecture

#### Core Capabilities

**Book Catalog Management**

- Maintain searchable collection of books (title, author, ISBN, price, category)
- Category organization and management
- ISBN uniqueness validation (ISBN-10/ISBN-13)
- Availability status management
- Rich domain models with business methods

**Inventory Tracking**

- Monitor stock levels and reserved quantities
- Real-time availability calculation
- Reservation system for pending orders
- Release reserved inventory on cancellation
- Low stock threshold alerts
- Out-of-stock detection

**Search & Discovery**

- Multi-criteria search (title, author, category, price range, availability)
- Dynamic filtering with composable filters
- Pagination with Spring Data
- Query optimization with strategic caching

**Domain-Driven Design Patterns**

- Value Objects (ISBN, Money, BookId)
- Aggregate Roots (Book, Category, Inventory)
- Domain Services (DuplicateIsbnChecker, BookAvailabilityChecker)
- Factory Methods (Book.create(), Inventory.create())
- Business Logic in Domain (canBeSold(), isInPriceRange())

#### API Endpoints

**Public Endpoints (No Authentication)**

```
# Book Catalog
GET    /api/v1/bookstore/books                    - Browse catalog with filters
GET    /api/v1/bookstore/books/{id}               - Get book details
GET    /api/v1/bookstore/books/isbn/{isbn}        - Get book by ISBN
GET    /api/v1/bookstore/books/available          - List available books
GET    /api/v1/bookstore/books/in-stock           - List books in stock

# Search
GET    /api/v1/bookstore/books/search             - Advanced search
GET    /api/v1/bookstore/books/search/title       - Search by title
GET    /api/v1/bookstore/books/search/author      - Search by author
GET    /api/v1/bookstore/books/search/category    - Search by category
GET    /api/v1/bookstore/books/search/price-range - Search by price

# Inventory
GET    /api/v1/bookstore/inventory/{bookId}       - Get inventory details
GET    /api/v1/bookstore/inventory/{bookId}/availability - Check availability
GET    /api/v1/bookstore/inventory/low-stock      - List low stock items
GET    /api/v1/bookstore/inventory/out-of-stock   - List out of stock items
GET    /api/v1/bookstore/inventory/stats/*        - Inventory statistics
```

**Admin Endpoints (Requires ADMIN/SUPER_ADMIN)**

```
# Book Management
POST   /api/v1/bookstore/admin/books              - Create new book
PUT    /api/v1/bookstore/admin/books/{id}         - Update book
DELETE /api/v1/bookstore/admin/books/{id}         - Delete book

# Inventory Management
PUT    /api/v1/bookstore/admin/inventory/{bookId} - Update inventory
POST   /api/v1/bookstore/admin/inventory/bulk-update - Bulk update
POST   /api/v1/bookstore/admin/inventory/{bookId}/reserve - Reserve inventory
POST   /api/v1/bookstore/admin/inventory/{bookId}/release - Release inventory
POST   /api/v1/bookstore/admin/inventory/{bookId}/adjust  - Adjust inventory
```

#### Domain Model

**Value Objects**

```java
// ISBN - Self-validating ISBN-10/ISBN-13
public record ISBN(String value) {
  public ISBN {
    if (!isValid(value)) {
      throw new IllegalArgumentException("Invalid ISBN");
    }
  }

  private static boolean isValid(String isbn) {
    // ISBN-10 or ISBN-13 validation logic
  }
}

// Money - Immutable monetary amounts
public record Money(BigDecimal amount, Currency currency) {
  public Money add(Money other) {
    /* ... */
  }

  public Money multiply(BigDecimal factor) {
    /* ... */
  }
}

// BookId - Type-safe identifier
public record BookId(Long value) {}
```

**Aggregate Roots**

```java
@Entity
public class Book extends AggregateRoot {

  private BookId id;
  private String title;
  private String author;
  private ISBN isbn;
  private Money price;
  private Category category;
  private boolean available;

  // Factory method
  public static Book create(String title, String author, ISBN isbn, Money price) {
    // Validation and creation logic
  }

  // Business methods
  public boolean canBeSold() {
    return available && hasStock();
  }

  public void markAsAvailable() {
    /* ... */
  }

  public void updateDetails(String title, String author) {
    /* ... */
  }
}

@Entity
public class Inventory extends AggregateRoot {

  private BookId bookId;
  private int quantity;
  private int reserved;
  private int lowStockThreshold;

  public int getAvailableQuantity() {
    return quantity - reserved;
  }

  public void reserve(int amount) {
    if (getAvailableQuantity() < amount) {
      throw new InsufficientStockException();
    }
    reserved += amount;
  }

  public void release(int amount) {
    /* ... */
  }
}
```

#### Configuration

**Environment Variables**

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gripday_bookstore
SPRING_DATASOURCE_USERNAME=gripday_bookstore
SPRING_DATASOURCE_PASSWORD=secure_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT Validation
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://user-service:8080/.well-known/jwks.json

# Observability
MANAGEMENT_TRACING_ENABLED=true
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

---

## 🎨 Frontend Applications Specification

### Architecture: Feature-Sliced Design (FSD)

Both frontend applications follow Feature-Sliced Design methodology with strict layer hierarchy:

```
src/
├── app/          # Application layer (providers, routing, config, theme)
├── processes/    # Process layer (cross-feature: auth, tenant management)
├── pages/        # Page layer (route components)
├── widgets/      # Widget layer (complex UI blocks: header, sidebar)
├── features/     # Feature layer (user scenarios, business logic)
├── entities/     # Entity layer (business entities, data models)
└── shared/       # Shared layer (reusable: API, UI, utils, mocks)
```

**FSD Layer Rules (ENFORCED BY ARCHITECTURE TESTS)**

1. Higher layers can ONLY import from lower layers
2. Each slice MUST expose functionality through `index.ts` (public API)
3. Features cannot depend on each other (use shared/entities/processes)
4. Architecture tests verify compliance: `pnpm test:arch`

### Common Frontend Patterns

**State Management Strategy**

- **Server State**: TanStack Query v5 (users, dashboard data, API calls)
- **Client State**: Zustand with Immer (auth, UI preferences, theme)
- **URL State**: nuqs (search params, filters, pagination)
- **Form State**: Mantine Form with Zod validation

**Error Handling (RFC 9457 Compliant)**

```typescript
// AppError interface
interface AppError {
  type: "auth" | "validation" | "network" | "timeout" | "server" | "client" | "rate-limit";
  message: string;
  status?: number;
  code?: string;
  details?: any;
  requestId?: string;
  retryable: boolean;
  fieldErrors?: Record<string, string[]>;
  // RFC 9457 fields
  type: string;
  title: string;
  detail: string;
  instance?: string;
}

// Usage with useFormMutation
const mutation = useFormMutation<ResponseType, FormValues>(form, async (values) => api.post("/endpoint", values), {
  notifySuccess: { title: t`Success!`, message: t`Form submitted` },
  notifyError: { title: t`Error`, fallback: t`Failed to submit` },
  clearOnSuccess: true,
  focusErrorField: true,
});
```

**API Client Configuration**

```typescript
// shared/lib/client.ts
import axios from "axios";
import { getConfig } from "@/app/config";
import { resolveTenantId } from "./tenant-utils";

const BASE_URL = getConfig("VITE_API_URL_SERVER");

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // Cookie-based auth
});

// Tenant header interceptor
api.interceptors.request.use((config) => {
  const tenantId = resolveTenantId();
  if (tenantId && !config.headers["X-Tenant-ID"]) {
    config.headers["X-Tenant-ID"] = tenantId;
  }
  return config;
});

// Error handling interceptor
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const normalized = normalizeAxiosError(error);
    if (normalized.type === "server") {
      notificationService.error({
        title: "Server error",
        message: normalized.message,
      });
    }
    return Promise.reject(normalized);
  }
);
```

### 1. Auth Portal (auth.gripday.com)

**Purpose**: Dedicated authentication gateway handling user registration, login, password management, and redirects to main application

#### Features Implemented

**Authentication Flows**

- User registration with validation (username, email, password, names)
- User login with username/email and password
- Remember me functionality for extended sessions
- Forgot password flow with email input
- Reset password with token validation
- JWT token reception and storage
- Automatic redirect to main application domain

**Form Features**

- Real-time validation with Zod schemas
- Password strength indicators
- Character counting and validation status
- Accessible form controls with Mantine UI
- Error handling with user-friendly messages
- Loading states and success notifications

**UI Components**

- Auth layout with centered forms
- Tenant information display
- Theme toggle (light/dark mode)
- Responsive design for all devices
- Lingui i18n integration

#### Pages & Routes

```
/                    - Home (redirects to /login)
/login               - Sign in form
/register            - Sign up form
/forgot-password     - Password reset request
/reset-password      - Password reset with token
/404                 - Not found page
```

#### Features Structure

```
features/
├── signin-form/
│   ├── ui/
│   │   └── signin-form-feature.tsx
│   ├── model/
│   │   ├── types.ts
│   │   └── validation.ts
│   └── index.ts
├── signup-form/
│   ├── ui/
│   │   └── signup-form-feature.tsx
│   ├── model/
│   │   ├── types.ts
│   │   └── validation.ts
│   └── index.ts
├── forgot-password-form/
│   └── [similar structure]
└── reset-password-form/
    └── [similar structure]
```

#### Validation Schemas

```typescript
// Centralized validation with Lingui i18n
export const createValidationSchemas = () => ({
  email: z
    .string()
    .min(1, t`Email is required`)
    .email(t`Please enter a valid email address`),

  password: z
    .string()
    .min(8, t`Password must be at least 8 characters`)
    .regex(/(?=.*[a-z])/, t`Must include lowercase letter`)
    .regex(/(?=.*[A-Z])/, t`Must include uppercase letter`)
    .regex(/(?=.*\d)/, t`Must include number`)
    .regex(/(?=.*[@$!%*?&])/, t`Must include special character`),

  username: z
    .string()
    .min(3, t`Username must be at least 3 characters`)
    .max(20, t`Username must not exceed 20 characters`)
    .regex(/^[a-zA-Z0-9_-]+$/, t`Only letters, numbers, underscore, hyphen`),
});
```

#### Environment Configuration

```bash
# API Configuration
VITE_API_URL_SERVER=http://localhost:8080

# Auth Domain Configuration
VITE_AUTH_DOMAIN_AUTH=https://auth.gripday.com
VITE_AUTH_DOMAIN_APP=https://app.gripday.com

# Redirect Configuration (optional)
VITE_AUTH_REDIRECT_AFTER_LOGIN=https://app.gripday.com
VITE_AUTH_REDIRECT_AFTER_LOGOUT=https://auth.gripday.com/login
VITE_AUTH_REDIRECT_AFTER_SIGNUP=https://auth.gripday.com/login

# MSW Configuration
VITE_ENABLE_MSW=true
VITE_LOG_LEVEL=info
```

### 2. Main Application (app.gripday.com)

**Purpose**: Main application frontend providing authenticated user experience, user management, dashboard analytics, and security settings

#### Features Implemented

**Dashboard & Analytics**

- Statistics overview with key metrics (users, orders, revenue, growth)
- Trend indicators (positive/negative changes)
- Real-time data visualization
- Business KPIs display
- Responsive grid layout
- Icon-based visual indicators

**User Management (Admin)**

- User listing with pagination (page, limit)
- User search by username, email, or name
- User creation with role assignment
- User editing with validation
- User deletion with confirmation
- Email verification status display
- Role-based access control enforcement

**Security Management**

- Password change with validation
- Current password verification
- Password strength requirements
- Multi-device session management
- Logout from all devices
- Security recommendations display

**Email Verification**

- Email verification status checking
- Verification workflow integration
- Resend verification email
- Email verification confirmation
- Status display in user profile

**User Preferences**

- Current user profile display
- User information viewing
- Account settings access
- Role and permission display
- Tenant information display
- Locale and timezone settings
- Theme preferences
- Notification preferences

**Route Protection**

- Public routes (redirected to auth portal)
- Protected routes (requires authentication)
- Admin routes (requires ADMIN/SUPER_ADMIN role)
- Permission-based routes
- Automatic redirect for unauthorized access
- Email verification requirement enforcement

#### Pages & Routes

```
/                    - Home (public, redirects to auth if not logged in)
/dashboard           - Dashboard with analytics (protected)
/users               - User management (admin only)
/user-preferences    - User preferences (protected)
/about               - About page (public)
/examples            - Component examples (protected)
/msw-demo            - MSW demo page (development)
/404                 - Not found page
```

#### Features Structure

```
features/
├── dashboard/
│   ├── ui/
│   │   ├── dashboard-feature.tsx
│   │   └── statistics-card.tsx
│   ├── model/
│   │   └── types.ts
│   └── index.ts
├── users/
│   ├── ui/
│   │   ├── users-list-feature.tsx
│   │   ├── user-form-feature.tsx
│   │   └── user-table.tsx
│   ├── model/
│   │   ├── types.ts
│   │   ├── validation.ts
│   │   └── queries.ts
│   └── index.ts
├── security-settings/
│   ├── ui/
│   │   ├── security-settings-feature.tsx
│   │   ├── password-change-form.tsx
│   │   └── session-management.tsx
│   ├── model/
│   │   ├── types.ts
│   │   └── validation.ts
│   └── index.ts
├── user-preferences/
│   ├── ui/
│   │   └── user-preferences-feature.tsx
│   ├── model/
│   │   ├── types.ts
│   │   └── validation.ts
│   └── index.ts
└── email-status-checker/
    ├── ui/
    │   └── email-status-checker.tsx
    └── index.ts
```

#### Widgets Structure

```
widgets/
├── header/
│   ├── ui/
│   │   ├── header.tsx
│   │   ├── user-menu.tsx
│   │   └── notifications-menu.tsx
│   └── index.ts
├── sidebar/
│   ├── ui/
│   │   ├── sidebar.tsx
│   │   └── navigation-links.tsx
│   └── index.ts
└── tenant-info/
    ├── ui/
    │   └── tenant-info.tsx
    └── index.ts
```

#### Processes Layer (Cross-Feature Concerns)

```
processes/
├── auth/
│   ├── model/
│   │   ├── auth-store.ts          # Zustand store with Immer
│   │   ├── auth-selectors.ts      # Granular selectors
│   │   └── types.ts
│   ├── lib/
│   │   ├── token-manager.ts       # Token lifecycle management
│   │   └── auth-guards.ts         # Route protection
│   └── index.ts
└── tenant/
    ├── model/
    │   ├── tenant-store.ts
    │   └── types.ts
    ├── lib/
    │   └── tenant-utils.ts
    └── index.ts
```

#### Environment Configuration

```bash
# API Configuration
VITE_API_URL_SERVER=http://localhost:8080

# Auth Domain Configuration
VITE_AUTH_DOMAIN_AUTH=https://auth.gripday.com
VITE_AUTH_DOMAIN_APP=https://app.gripday.com

# Redirect Configuration
VITE_AUTH_REDIRECT_AFTER_LOGIN=/dashboard
VITE_AUTH_REDIRECT_AFTER_LOGOUT=/
VITE_AUTH_REDIRECT_AFTER_SIGNUP=/verify-email

# MSW Configuration
VITE_ENABLE_MSW=true
VITE_LOG_LEVEL=info
```

---

## 🔄 Integration & Communication Patterns

### Authentication Flow

```
┌─────────────┐                ┌─────────────┐                ┌─────────────┐
│   Browser   │                │ Auth Portal │                │ User Service│
└──────┬──────┘                └──────┬──────┘                └──────┬──────┘
       │                              │                              │
       │  1. Navigate to app          │                              │
       ├─────────────────────────────>│                              │
       │                              │                              │
       │  2. Show login form          │                              │
       │<─────────────────────────────┤                              │
       │                              │                              │
       │  3. Submit credentials       │                              │
       ├─────────────────────────────>│                              │
       │                              │  4. POST /api/v1/auth/login  │
       │                              ├─────────────────────────────>│
       │                              │                              │
       │                              │  5. Validate & generate JWT  │
       │                              │<─────────────────────────────┤
       │                              │                              │
       │  6. Set cookie & redirect    │                              │
       │<─────────────────────────────┤                              │
       │                              │                              │
       │  7. Navigate to main app     │                              │
       ├──────────────────────────────┼─────────────────────────────>│
       │                              │                              │
```

### API Request Flow with Gateway

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Main App   │     │   Gateway   │     │ User Service│     │  PostgreSQL │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │  1. GET /api/v1/users/me             │                   │
       │   + Cookie: jwt_token                │                   │
       ├──────────────────>│                   │                   │
       │                   │                   │                   │
       │                   │  2. Validate JWT  │                   │
       │                   │  3. Extract user context             │
       │                   │  4. Check rate limit (Redis)         │
       │                   │  5. Add headers:  │                   │
       │                   │     X-User-ID     │                   │
       │                   │     X-Username    │                   │
       │                   │     X-Tenant-ID   │                   │
       │                   │     X-Correlation-ID                 │
       │                   │                   │                   │
       │                   │  6. Forward request                  │
       │                   ├──────────────────>│                   │
       │                   │                   │  7. Query user    │
       │                   │                   ├──────────────────>│
       │                   │                   │  8. User data     │
       │                   │                   │<──────────────────┤
       │                   │  9. Response      │                   │
       │                   │<──────────────────┤                   │
       │  10. User data    │                   │                   │
       │<──────────────────┤                   │                   │
       │                   │                   │                   │
```

### Multi-Tenant Request Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Main App   │     │   Gateway   │     │ User Service│     │  PostgreSQL │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │  1. Request with tenant context      │                   │
       │   X-Tenant-ID: tenant-123            │                   │
       ├──────────────────>│                   │                   │
       │                   │  2. Extract tenant from:             │
       │                   │     - Header (priority 1)            │
       │                   │     - JWT claim (priority 2)         │
       │                   │     - Subdomain (priority 3)         │
       │                   │                   │                   │
       │                   │  3. Set TenantContext                │
       │                   │  4. Forward with X-Tenant-ID         │
       │                   ├──────────────────>│                   │
       │                   │                   │  5. Set schema    │
       │                   │                   │     SET search_path = tenant_123 │
       │                   │                   ├──────────────────>│
       │                   │                   │  6. Query data    │
       │                   │                   │<──────────────────┤
       │                   │  7. Response      │                   │
       │                   │<──────────────────┤                   │
       │  8. Data          │                   │                   │
       │<──────────────────┤                   │                   │
       │                   │                   │                   │
```

### Error Handling (RFC 9457 Problem Details)

**Backend Error Response Format**

```json
{
  "type": "validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "fields": [
    {
      "field": "email",
      "message": "Email is already registered",
      "rejectedValue": "user@example.com"
    }
  ],
  "correlationId": "abc-123",
  "requestId": "req-456",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Frontend Error Handling**

```typescript
// Automatic field error mapping
const mutation = useFormMutation<void, FormValues>(form, async (values) => api.post("/endpoint", values), {
  notifyError: {
    title: t`Error`,
    fallback: t`Failed to submit`,
    showTechnicalDetails: false,
    enableRetry: true,
  },
});

// Field errors automatically mapped to form
// form.errors = { email: 'Email is already registered' }
```

### Rate Limiting Response

**When Rate Limit Exceeded**

```json
{
  "type": "/problems/rate_limit_exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Tenant rate limit exceeded",
  "instance": "/api/v1/auth/login",
  "code": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2024-01-15T10:30:00Z",
  "retryAfter": 60,
  "correlationId": "1634567890-abc12345",
  "tenantId": "tenant-123"
}
```

**Response Headers**

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1634568790
Retry-After: 60
```

---

## 🧪 Testing Strategy

### Backend Testing

**Unit Tests (JUnit 5)**

- AAA pattern (Arrange-Act-Assert)
- Service layer business logic
- Domain model validation
- Utility functions
- Coverage: 60-90% depending on service

**Integration Tests (Testcontainers)**

- PostgreSQL integration with Testcontainers
- Redis integration tests
- REST API endpoint tests
- Security configuration tests
- Multi-tenant isolation tests

**Architecture Tests (ArchUnit)**

- Layer dependency rules
- Package structure validation
- Naming conventions
- No cyclic dependencies
- Service layer accessed only by controllers

**Commands**

```bash
# Unit tests only
mvn test -Dcheckstyle.skip=true

# Integration tests only
mvn verify -DskipUnitTests -Dcheckstyle.skip=true

# All tests with coverage
mvn clean verify -Dcheckstyle.skip=true

# Architecture tests
mvn test -Dtest=ArchitectureTest -Dcheckstyle.skip=true
```

### Frontend Testing

**Unit Tests (Vitest)**

- Co-located with source files (component.test.tsx)
- React Testing Library for component tests
- Mock Service Worker (MSW) for API mocking
- Coverage: v8 provider with HTML/text reporters
- Test environment: happy-dom

**E2E Tests (Playwright)**

- Auto-start dev server via playwright.config.ts
- Chromium (default), Firefox, WebKit (CI or ALL_BROWSERS=true)
- Parallel execution locally, sequential in CI
- Retries: 1 locally, 2 in CI
- Artifacts: screenshots, videos, traces on failure

**Architecture Tests**

- Automated FSD compliance verification
- Layer structure validation
- Public API (index.ts) enforcement
- Required segments verification
- Naming conventions validation

**Commands**

```bash
# Unit tests
pnpm test                    # Run all unit tests
pnpm test:arch               # Run FSD architecture tests
pnpm test:ui                 # Run with Vitest UI
pnpm test:coverage           # Generate coverage report

# E2E tests
pnpm playwright:install      # First time: install browsers
pnpm e2e                     # Run tests (Chromium only)
pnpm e2e:ui                  # Interactive UI mode
pnpm e2e:headed              # Watch tests run
pnpm e2e:debug               # Debug with Playwright Inspector
pnpm e2e:report              # View HTML report
pnpm e2e:all-browsers        # Test on all browsers
```

---

## 🚀 Deployment & Operations

### Local Development Setup

**Prerequisites**

- Java 21+
- Node.js 22+ (LTS)
- PNPM 10.20+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)
- Redis (or use Docker)

**Backend Setup**

```bash
# Clone repository
git clone https://github.com/your-org/gripday.git
cd gripday/backend

# Copy environment variables
cp .env.example .env

# Start infrastructure with Docker Compose
docker compose up -d

# Build all services
mvn clean install -Dcheckstyle.skip=true

# Run User Service
cd gripday-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run Gateway Service (separate terminal)
cd gripday-gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run Bookstore Service (separate terminal)
cd gripday-bookstore-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend Setup**

```bash
# Auth Portal
cd auth.gripday.com
pnpm install
cp .env.example .env
pnpm dev  # Runs on http://localhost:5173

# Main Application (separate terminal)
cd app.gripday.com
pnpm install
cp .env.example .env
pnpm dev  # Runs on http://localhost:5174 (or configure port)
```

### Docker Deployment

**Service-Specific Dockerfiles**

Each service has a multi-stage Dockerfile:

```dockerfile
# Example: gripday-user-service/Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Docker Compose for Full Platform**

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Rebuild and restart
docker compose up -d --build
```

### Kubernetes Deployment

**Helm Charts Available**

```bash
# Install User Service
helm install user-service ./helm/user-service \
  --namespace gripday \
  --create-namespace

# Install Gateway Service
helm install gateway-service ./helm/gateway-service \
  --namespace gripday

# Install Bookstore Service
helm install bookstore-service ./helm/bookstore-service \
  --namespace gripday

# Install full platform
helm install gripday ./helm/gripday-platform \
  --namespace gripday \
  --create-namespace
```

**Kubernetes Manifests**

```bash
# Apply configurations
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n gripday

# View logs
kubectl logs -f deployment/user-service -n gripday

# Scale services
kubectl scale deployment/user-service --replicas=3 -n gripday
```

### Environment Profiles

**Backend Profiles**

- `local` - Local development with H2/PostgreSQL
- `staging` - Staging environment with external PostgreSQL
- `production` - Production with optimized settings

**Activate Profile**

```bash
# Via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=staging

# Via environment variable
export SPRING_PROFILES_ACTIVE=production
java -jar app.jar

# Via Docker
docker run -e SPRING_PROFILES_ACTIVE=production gripday-user-service
```

---

## 📊 Observability & Monitoring

### Metrics Collection (Prometheus)

**Exposed Metrics**

- JVM metrics (memory, GC, threads)
- HTTP request metrics (count, duration, status)
- Database connection pool metrics
- Redis connection metrics
- Custom business metrics (user registrations, login attempts)
- Circuit breaker metrics
- Rate limiting metrics

**Prometheus Configuration**

```yaml
# docker/prometheus/prometheus.yml
scrape_configs:
  - job_name: "user-service"
    static_configs:
      - targets: ["user-service:8080"]
    metrics_path: "/actuator/prometheus"

  - job_name: "gateway-service"
    static_configs:
      - targets: ["gateway-service:8081"]
    metrics_path: "/actuator/prometheus"

  - job_name: "bookstore-service"
    static_configs:
      - targets: ["bookstore-service:8082"]
    metrics_path: "/actuator/prometheus"
```

### Distributed Tracing (OpenTelemetry)

**Trace Propagation**

- Correlation ID generated at gateway
- Propagated via headers (X-Correlation-ID)
- Included in all logs
- Spans created for:
  - HTTP requests
  - Database queries
  - Redis operations
  - External API calls

**Configuration**

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0 # 100% sampling in dev, 10% in prod
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

### Logging (Loki + Promtail)

**Structured JSON Logging**

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "org.gripday.user.service.UserService",
  "message": "User created successfully",
  "correlationId": "abc-123",
  "userId": "1",
  "username": "john.doe",
  "tenantId": "tenant-123",
  "requestId": "req-456",
  "duration": 45,
  "thread": "http-nio-8080-exec-1"
}
```

**Log Aggregation**

- Promtail collects logs from all services
- Loki stores and indexes logs
- Grafana queries and visualizes logs
- Correlation ID enables cross-service tracing

### Grafana Dashboards

**Pre-configured Dashboards**

1. **Platform Overview**
   - Service health status
   - Request rate and latency
   - Error rate
   - Active users

2. **User Service Dashboard**
   - Authentication metrics
   - User registration rate
   - Failed login attempts
   - Token generation rate
   - Email verification rate

3. **Gateway Dashboard**
   - Request routing metrics
   - Rate limiting statistics
   - Circuit breaker status
   - Tenant-specific metrics

4. **Database Dashboard**
   - Connection pool usage
   - Query performance
   - Slow queries
   - Transaction rate

5. **JVM Dashboard**
   - Heap memory usage
   - GC activity
   - Thread count
   - CPU usage

**Access Grafana**

```
URL: http://localhost:3000
Username: admin
Password: (from .env file)
```

### Health Checks

**Actuator Endpoints**

```
GET /actuator/health          - Overall health status
GET /actuator/health/liveness - Kubernetes liveness probe
GET /actuator/health/readiness - Kubernetes readiness probe
GET /actuator/info            - Application information
GET /actuator/metrics         - Available metrics
GET /actuator/prometheus      - Prometheus metrics
```

**Health Indicators**

- Database connectivity
- Redis connectivity
- Disk space
- Custom business health checks

---

## 🔒 Security Considerations

### Authentication Security

**JWT Token Security**

- RSA256 asymmetric signing (2048-bit keys)
- Short-lived access tokens (15 minutes)
- Longer-lived refresh tokens (7 days)
- JTI-based token blacklisting with Redis TTL
- Secure token storage (HTTP-only cookies for web)
- Token rotation on refresh

**Password Security**

- BCrypt hashing with configurable strength
- Minimum 8 characters
- Must include: uppercase, lowercase, number, special character
- Password history (prevent reuse of last 5 passwords)
- Secure password reset with time-limited tokens

**Account Protection**

- Account lockout after 5 failed attempts
- 15-minute lockout duration
- Failed attempts counter with 30-minute sliding window
- IP-based rate limiting (5 attempts/minute)
- Email verification required for sensitive operations

### API Security

**Gateway Security**

- JWT validation at gateway level
- Rate limiting per IP and tenant
- Circuit breaker prevents cascading failures
- CORS configuration per environment
- Security headers (HSTS, CSP, X-Frame-Options)
- Request size limits

**Service Security**

- Method-level security with @PreAuthorize
- Role-based access control (RBAC)
- Permission-based access control
- Input validation with Bean Validation
- SQL injection prevention (parameterized queries)
- XSS prevention (output encoding)

### Multi-Tenant Security

**Data Isolation**

- Schema-per-tenant strategy
- No cross-tenant queries possible
- Tenant context validation on every request
- Tenant ID in JWT claims
- Tenant-scoped Redis keys

**Tenant Validation**

- Tenant existence check
- Tenant enabled status check
- User belongs to tenant validation
- Tenant quota enforcement

### Secrets Management

**Development**

- `.env` files (not committed to git)
- Environment variables
- Local configuration files

**Production**

- Kubernetes Secrets
- External secret management (AWS Secrets Manager, HashiCorp Vault)
- Encrypted configuration
- Key rotation policies

### Security Headers

**Recommended Headers**

```yaml
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

### Audit Logging

**Security Events Logged**

- User login (success/failure)
- User logout
- Password changes
- Failed authentication attempts
- Account lockouts
- Role/permission changes
- Admin operations
- Sensitive data access

**Audit Log Fields**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "eventType": "USER_LOGIN",
  "userId": "1",
  "username": "john.doe",
  "tenantId": "tenant-123",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "success": true,
  "correlationId": "abc-123",
  "details": {}
}
```

---

## 🤖 AI-Assisted Development Guidelines

### AGENTS.md Files

Each component (backend, auth portal, main app) includes comprehensive `AGENTS.md` files with:

**AI Communication Standards**

- Concise output requirements (2-3 sentence summaries)
- No auto-generated documentation files
- Action-oriented communication
- Minimal verbosity

**User Confirmation Policy**

- Always ask before applying changes
- Present proposed changes clearly
- Wait for explicit approval
- Verify changes after application

**Code Generation Principles**

- Type-first development
- Follow existing patterns
- Minimal implementation
- Architecture compliance (FSD for frontend, DDD for backend)

**Technology-Specific Guidelines**

- Backend: Spring Boot patterns, Java 21 features, DDD patterns
- Frontend: FSD architecture, React 19 patterns, Mantine UI usage
- Testing: Co-located tests, architecture tests, integration tests

### Commit Message Generation

**AI Should Generate Commit Messages After:**

- Completing multi-file changes (3+ files)
- Implementing features or bug fixes
- Performing refactoring
- Making configuration changes

**Conventional Commits Format**

```
type(scope): subject

[optional body]

[optional footer]
```

**Allowed Types**

- `feat`: New feature
- `fix`: Bug fix
- `rfc`: Request for comments
- `docs`: Documentation
- `style`: Code style
- `improvement`: Enhancements
- `refactor`: Code refactoring
- `perf`: Performance
- `test`: Tests
- `chore`: Maintenance
- `build`: Build system
- `ci`: CI/CD

**Examples**

```bash
feat(user-service): add email verification endpoint

Implements email verification flow with token-based validation.
Includes rate limiting (3 emails/hour) and 24-hour token expiration.

---

fix(gateway): correct rate limit counter reset logic

The sliding window algorithm was not properly removing expired
entries from Redis sorted set. Updated cleanup logic.

Closes #156

---

refactor(services): extract common JWT validation to shared utility

- Move JWT validation from user-service and gateway-service
- Consolidate RSA key loading and token parsing
- Update both services to use shared JwtValidator
```

### Maven Command Best Practices

**ALWAYS use `-Dcheckstyle.skip=true` during development:**

```bash
# ✅ RECOMMENDED
mvn clean verify -Dcheckstyle.skip=true
mvn test -Dcheckstyle.skip=true
mvn clean install -Dcheckstyle.skip=true

# ✅ Explicit style check when ready
mvn checkstyle:check

# ❌ NOT RECOMMENDED during active development
mvn clean verify  # May fail due to style issues
```

**Rationale:**

- Focus on functionality first, style second
- Faster iteration cycle
- CI/CD enforces style checks before merge
- Separation of concerns

---

## 📚 Development Workflows

### Adding a New Feature (Frontend)

**1. Plan Feature Structure**

```bash
# Determine FSD layer
# - Feature: User-facing functionality
# - Widget: Composite UI block
# - Entity: Business entity/model
# - Shared: Reusable utility

# Example: Adding user profile feature
features/user-profile/
├── ui/
│   ├── user-profile-feature.tsx
│   └── profile-card.tsx
├── model/
│   ├── types.ts
│   ├── validation.ts
│   └── queries.ts
└── index.ts
```

**2. Define Types**

```typescript
// features/user-profile/model/types.ts
export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar?: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  avatar?: string;
}
```

**3. Create Validation Schema**

```typescript
// features/user-profile/model/validation.ts
import { z } from "zod";
import { t } from "@lingui/core/macro";

export const updateProfileSchema = z.object({
  firstName: z.string().min(1, t`First name is required`),
  lastName: z.string().min(1, t`Last name is required`),
  avatar: z.string().url().optional(),
});
```

**4. Implement Component**

```typescript
// features/user-profile/ui/user-profile-feature.tsx
import { useForm } from '@mantine/form';
import { zodResolver } from 'mantine-form-zod-resolver';
import { useFormMutation } from '@/shared/lib';
import { updateProfileSchema } from '../model/validation';

export function UserProfileFeature() {
  const form = useForm({
    initialValues: { firstName: '', lastName: '' },
    validate: zodResolver(updateProfileSchema),
  });

  const mutation = useFormMutation(
    form,
    async (values) => api.patch('/api/v1/users/me', values),
    {
      notifySuccess: { title: t`Success`, message: t`Profile updated` },
    }
  );

  return (
    <form onSubmit={form.onSubmit((values) => mutation.mutate(values))}>
      {/* Form fields */}
    </form>
  );
}
```

**5. Export Public API**

```typescript
// features/user-profile/index.ts
export { UserProfileFeature } from "./ui/user-profile-feature";
export type { UserProfile, UpdateProfileRequest } from "./model/types";
```

**6. Add Tests**

```typescript
// features/user-profile/ui/user-profile-feature.test.tsx
import { render, screen } from '@testing-library/react';
import { UserProfileFeature } from './user-profile-feature';

describe('UserProfileFeature', () => {
  it('renders form fields', () => {
    render(<UserProfileFeature />);
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
  });
});
```

**7. Verify Architecture Compliance**

```bash
pnpm test:arch
```

### Adding a New Endpoint (Backend)

**1. Define DTO (Java Record)**

```java
// presentation/dto/CreateUserRequest.java
public record CreateUserRequest(
  @NotBlank(message = "Username is required") @Size(min = 3, max = 20) String username,

  @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

  @NotBlank(message = "Password is required") @ValidPassword String password,

  String firstName,
  String lastName,

  @NotEmpty(message = "At least one role is required") Set<String> roles
) {}
```

**2. Add Service Method**

```java
// service/UserService.java
@Service
@Transactional
public class UserService {

  public UserResponse createUser(CreateUserRequest request) {
    // Validate username uniqueness
    if (userRepository.existsByUsername(request.username())) {
      throw new DuplicateUsernameException(request.username());
    }

    // Create user entity
    var user = User.builder()
      .username(request.username())
      .email(request.email())
      .password(passwordEncoder.encode(request.password()))
      .firstName(request.firstName())
      .lastName(request.lastName())
      .enabled(true)
      .build();

    // Assign roles
    var roles = roleRepository.findByNameIn(request.roles());
    user.setRoles(new HashSet<>(roles));

    // Save and return
    var saved = userRepository.save(user);
    return UserMapper.toResponse(saved);
  }
}
```

**3. Create Controller Endpoint**

```java
// presentation/web/UserController.java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

  private final UserService userService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Create new user", description = "Creates a new user with specified roles")
  @ApiResponses(
    {
      @ApiResponse(responseCode = "201", description = "User created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "409", description = "Username already exists"),
    }
  )
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    var response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
```

**4. Add Integration Test**

```java
// UserControllerIntegrationTest.java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void createUser_WithValidRequest_ReturnsCreated() {
    var request = new CreateUserRequest("testuser", "test@example.com", "SecurePass123!", "Test", "User", Set.of("USER"));

    var response = restTemplate.withBasicAuth("admin", "admin").postForEntity("/api/v1/users", request, UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().username()).isEqualTo("testuser");
  }
}
```

**5. Update OpenAPI Documentation**

```java
// Annotations already added in controller
// Swagger UI automatically updated at /swagger-ui.html
```

**6. Run Tests**

```bash
mvn test -Dcheckstyle.skip=true
mvn verify -Dcheckstyle.skip=true
```

### Adding a New Microservice

**1. Create Service Module**

```bash
cd backend
mkdir gripday-newservice-service
cd gripday-newservice-service
```

**2. Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.gripday</groupId>
        <artifactId>gripday</artifactId>
        <version>0.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>gripday-newservice-service</artifactId>
    <name>Gripday New Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <!-- Add other dependencies -->
    </dependencies>
</project>
```

**3. Create Application Class**

```java
package org.gripday.newservice;

@SpringBootApplication
public class NewServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(NewServiceApplication.class, args);
  }
}
```

**4. Configure application.yml**

```yaml
server:
  port: 8083

spring:
  application:
    name: newservice-service

  datasource:
    url: jdbc:postgresql://localhost:5432/gripday_newservice
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**5. Add to Parent POM**

```xml
<!-- backend/pom.xml -->
<modules>
    <module>gripday-user-service</module>
    <module>gripday-gateway-service</module>
    <module>gripday-bookstore-service</module>
    <module>gripday-newservice-service</module>
</modules>
```

**6. Create Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**7. Add to Gateway Routes**

```yaml
# gripday-gateway-service/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: newservice-service
          uri: http://newservice-service:8083
          predicates:
            - Path=/api/v1/newservice/**
          filters:
            - name: CircuitBreaker
              args:
                name: newservice-service
```

**8. Create Docker Compose**

```yaml
# gripday-newservice-service/docker-compose.yml
version: "3.8"

services:
  newservice-db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: gripday_newservice
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5435:5432"

  newservice-service:
    build: .
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://newservice-db:5432/gripday_newservice
    depends_on:
      - newservice-db
```

**9. Build and Run**

```bash
mvn clean install -Dcheckstyle.skip=true
docker compose up -d
```

---

## 🎯 Use Cases & Business Scenarios

### User Management Scenarios

**1. User Registration Flow**

```
1. User visits auth portal
2. Fills registration form (username, email, password, names)
3. Submits form → POST /api/v1/auth/signup
4. Backend validates and creates user
5. Sends verification email with token
6. User clicks link → GET /api/v1/auth/email/verify?token=xxx
7. Email verified, user can login
```

**2. User Login Flow**

```
1. User enters credentials on auth portal
2. Submits form → POST /api/v1/auth/login
3. Backend validates credentials
4. Generates JWT tokens (access + refresh)
5. Sets HTTP-only cookie
6. Redirects to main application
7. Main app validates token via gateway
8. User sees dashboard
```

**3. Admin User Management**

```
1. Admin logs in to main application
2. Navigates to /users page
3. Views paginated user list
4. Searches for specific user
5. Clicks "Edit" → Opens user form
6. Updates user details
7. Submits → PUT /api/v1/admin/users/{id}
8. Backend validates and updates
9. Table refreshes with updated data
```

**4. Password Reset Flow**

```
1. User clicks "Forgot Password" on auth portal
2. Enters email → POST /api/v1/auth/password/forgot
3. Backend generates reset token
4. Sends email with reset link
5. User clicks link → Redirects to /reset-password?token=xxx
6. User enters new password
7. Submits → POST /api/v1/auth/password/reset
8. Backend validates token and updates password
9. User redirected to login
```

### Multi-Tenant Scenarios

**1. Tenant Isolation**

```
1. User logs in with tenant context
2. JWT includes tenantId claim
3. All requests include X-Tenant-ID header
4. Gateway extracts and validates tenant
5. Backend sets schema: SET search_path = tenant_123
6. All queries isolated to tenant schema
7. No cross-tenant data access possible
```

**2. Tenant Creation**

```
1. Super admin creates new tenant
2. POST /api/v1/admin/tenants
3. Backend creates tenant record
4. Runs Liquibase migrations for new schema
5. Creates default organization
6. Assigns tenant owner
7. Tenant ready for users
```

**3. Cross-Tenant Statistics**

```
1. Super admin views tenant statistics
2. GET /api/v1/admin/tenants/statistics
3. Backend iterates all tenants
4. For each tenant:
   - Switch to tenant schema
   - Count users, organizations
   - Calculate utilization
5. Aggregate results
6. Return statistics array
```

### Bookstore Scenarios

**1. Book Search**

```
1. Customer visits bookstore
2. Enters search criteria (title, author, category)
3. GET /api/v1/bookstore/books/search?title=Java&category=Programming
4. Backend applies filters
5. Queries database with pagination
6. Returns matching books
7. Frontend displays results
```

**2. Inventory Management**

```
1. Admin adds new book
2. POST /api/v1/bookstore/admin/books
3. Backend creates book record
4. Creates inventory record (quantity: 100, threshold: 10)
5. Book available for purchase
6. Customer reserves book
7. POST /api/v1/bookstore/admin/inventory/{bookId}/reserve?quantity=1
8. Available quantity decreases
9. Order fulfilled → Release or adjust inventory
```

**3. Low Stock Alert**

```
1. Inventory drops below threshold
2. GET /api/v1/bookstore/inventory/low-stock
3. Backend queries inventory where quantity < threshold
4. Returns low stock items
5. Admin receives notification
6. Admin replenishes stock
7. PUT /api/v1/bookstore/admin/inventory/{bookId}
```

---

## 🚦 Performance & Scalability

### Performance Optimization Strategies

**Backend**

- Connection pooling (HikariCP)
- Database query optimization with indexes
- Redis caching for frequently accessed data
- Reactive programming in gateway (non-blocking I/O)
- Lazy loading with JPA
- Pagination for large datasets
- Async processing for non-critical operations

**Frontend**

- Code splitting with TanStack Router
- Lazy loading for route components
- React.memo for expensive components
- useMemo/useCallback for computed values
- Virtual scrolling for large lists
- Image optimization in production builds
- Bundle size optimization with Vite

### Scalability Patterns

**Horizontal Scaling**

- Stateless services (JWT-based auth)
- Independent service scaling
- Load balancing across instances
- Shared Redis for distributed state
- Database per service pattern

**Vertical Scaling**

- JVM heap size configuration
- Connection pool sizing
- Thread pool tuning
- Database connection limits

**Caching Strategy**

- L1: Application cache (Caffeine)
- L2: Distributed cache (Redis)
- Cache-aside pattern
- TTL-based expiration
- Cache invalidation on updates

### Load Testing Recommendations

**Tools**

- JMeter for backend load testing
- Gatling for realistic scenarios
- k6 for modern load testing
- Locust for Python-based tests

**Scenarios to Test**

- User registration (100 concurrent users)
- User login (500 concurrent users)
- Dashboard data loading (1000 concurrent users)
- User search and pagination (200 concurrent users)
- Book search (500 concurrent users)
- Rate limiting thresholds
- Circuit breaker activation

**Metrics to Monitor**

- Response time (p50, p95, p99)
- Throughput (requests/second)
- Error rate
- CPU and memory usage
- Database connection pool usage
- Redis connection usage
- JVM garbage collection

---

## 🔧 Troubleshooting Guide

### Common Issues

**Backend Issues**

**1. Service Won't Start**

```bash
# Check logs
docker compose logs -f user-service

# Common causes:
# - Database not ready (add depends_on with health check)
# - Port already in use (change port in application.yml)
# - Missing environment variables (check .env file)
# - Database migration failure (check Liquibase logs)
```

**2. JWT Validation Fails**

```bash
# Verify JWK Set endpoint is accessible
curl http://localhost:8080/.well-known/jwks.json

# Check gateway configuration
# Ensure SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI is correct

# Verify token is valid
# Use jwt.io to decode and inspect token
```

**3. Database Connection Issues**

```bash
# Check PostgreSQL is running
docker compose ps

# Test connection
psql -h localhost -U gripday_user -d gripday_user

# Check connection pool settings in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

**4. Redis Connection Issues**

```bash
# Check Redis is running
docker compose ps

# Test connection
redis-cli -h localhost -p 6379 ping

# Check Redis configuration
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**Frontend Issues**

**1. API Calls Fail with CORS**

```typescript
// Check API client configuration
// Ensure withCredentials: true for cookie-based auth
export const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// Check backend CORS configuration
// Ensure allowed origins include frontend URL
```

**2. Authentication Redirect Loop**

```typescript
// Check auth store initialization
// Ensure token validation on app load
// Verify redirect URLs in .env

// Check for infinite redirect loops
// Add guards to prevent re-redirecting
```

**3. Build Fails**

```bash
# Clear cache and reinstall
rm -rf node_modules pnpm-lock.yaml
pnpm install

# Check TypeScript errors
pnpm type-check

# Check for circular dependencies
pnpm knip
```

**4. Tests Fail**

```bash
# Clear test cache
pnpm test --clearCache

# Run specific test
pnpm test user-form

# Check for MSW handler issues
# Ensure handlers match API endpoints
```

---

## 📖 API Documentation

### Swagger UI Access

**User Service**

```
URL: http://localhost:8080/swagger-ui.html
Description: Authentication, user management, tenant management
```

**Gateway Service**

```
URL: http://localhost:8081/swagger-ui.html
Description: Gateway routing, rate limiting, circuit breaker
```

**Bookstore Service**

```
URL: http://localhost:8082/swagger-ui.html
Description: Book catalog, inventory management
```

### API Versioning Strategy

**URL-Based Versioning (Current)**

```
/api/v1/auth/login
/api/v1/users/me
/api/v1/bookstore/books
```

**Header-Based Versioning (Supported)**

```
GET /api/auth/login
Headers:
  API-Version: 1
```

**Deprecation Policy**

- New version released: v2
- Old version supported: 6 months
- Deprecation notice: 3 months before removal
- Breaking changes: Major version bump

### Rate Limiting

**Default Limits**

```yaml
Global IP-based: 1000 requests/minute
Tenant-specific: Configurable per tenant

Endpoint-specific:
  /api/v1/auth/login: 10 requests/minute
  /api/v1/auth/signup: 5 requests/minute
  /api/v1/bookstore/**: 100 requests/minute
```

**Rate Limit Headers**

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1634568790
Retry-After: 15
```

---

## 🎓 Learning Resources

### Architecture Patterns

**Microservices**

- [Microservices Patterns by Chris Richardson](https://microservices.io/patterns/)
- [Building Microservices by Sam Newman](https://www.oreilly.com/library/view/building-microservices-2nd/9781492034018/)

**Domain-Driven Design**

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://www.oreilly.com/library/view/implementing-domain-driven-design/9780133039900/)

**Feature-Sliced Design**

- [FSD Official Documentation](https://feature-sliced.design/)
- [FSD Examples and Best Practices](https://github.com/feature-sliced/examples)

### Technology-Specific

**Spring Boot**

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

**React & Frontend**

- [React Documentation](https://react.dev/)
- [TanStack Query](https://tanstack.com/query/latest)
- [TanStack Router](https://tanstack.com/router/latest)
- [Mantine UI](https://mantine.dev/)

**Testing**

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers](https://www.testcontainers.org/)
- [Vitest Documentation](https://vitest.dev/)
- [Playwright Documentation](https://playwright.dev/)

### Observability

**OpenTelemetry**

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/observability-primer/)

**Prometheus & Grafana**

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

---

## 🗺️ Roadmap & Future Enhancements

### Planned Features

**Backend**

- [ ] GraphQL API support
- [ ] Event-driven architecture with Kafka
- [ ] Service mesh integration (Istio)
- [ ] Advanced caching strategies (multi-level)
- [ ] Real-time notifications with WebSocket
- [ ] Audit log search and analytics
- [ ] Advanced tenant management (quotas, billing)
- [ ] API rate limiting per user
- [ ] OAuth2 social login (Google, GitHub)
- [ ] Two-factor authentication (2FA)

**Frontend**

- [ ] Progressive Web App (PWA) support
- [ ] Offline mode with service workers
- [ ] Real-time updates with WebSocket
- [ ] Advanced data visualization
- [ ] Drag-and-drop interfaces
- [ ] File upload with progress
- [ ] Advanced search with filters
- [ ] Bulk operations UI
- [ ] Export data (CSV, PDF)
- [ ] Dark mode improvements

**DevOps**

- [ ] Kubernetes autoscaling (HPA, VPA)
- [ ] Blue-green deployment
- [ ] Canary releases
- [ ] A/B testing infrastructure
- [ ] Automated rollback on failures
- [ ] Multi-region deployment
- [ ] Disaster recovery automation
- [ ] Cost optimization monitoring

**Observability**

- [ ] Advanced alerting rules
- [ ] Anomaly detection
- [ ] Performance profiling
- [ ] User behavior analytics
- [ ] Business metrics dashboards
- [ ] SLA monitoring
- [ ] Incident management integration

### Extension Points

**Adding New Services**

1. Follow microservice template
2. Implement JWT validation
3. Add to gateway routes
4. Configure observability
5. Add to Docker Compose
6. Create Kubernetes manifests

**Adding New Features**

1. Follow FSD architecture (frontend)
2. Follow DDD patterns (backend)
3. Add comprehensive tests
4. Update API documentation
5. Add to CI/CD pipeline

**Customization Options**

- Custom authentication providers
- Custom authorization rules
- Custom business domains
- Custom UI themes
- Custom metrics and dashboards
- Custom notification channels

---

## 📝 Conclusion

The Gripday platform provides a solid foundation for building production-ready microservices applications with modern architecture patterns. It demonstrates:

✅ **Microservices Architecture** - Service decomposition, API gateway, database per service  
✅ **Security Best Practices** - JWT authentication, RBAC, multi-tenancy, audit logging  
✅ **Modern Frontend** - React 19, Feature-Sliced Design, type-safe development  
✅ **Observability** - Distributed tracing, metrics, structured logging  
✅ **Testing** - Comprehensive test coverage with unit, integration, and E2E tests  
✅ **DevOps** - Docker, Kubernetes, CI/CD pipelines  
✅ **AI-Assisted Development** - Comprehensive AGENTS.md guidelines

### Getting Started Checklist

**For New Developers:**

- [ ] Clone repository
- [ ] Install prerequisites (Java 21, Node 22, Docker)
- [ ] Copy .env.example to .env
- [ ] Start infrastructure with Docker Compose
- [ ] Run backend services
- [ ] Run frontend applications
- [ ] Access Swagger UI and test APIs
- [ ] Read AGENTS.md for development guidelines

**For AI Agents:**

- [ ] Read AGENTS.md in each component
- [ ] Understand FSD architecture (frontend)
- [ ] Understand DDD patterns (backend)
- [ ] Follow concise communication standards
- [ ] Always ask before applying changes
- [ ] Generate commit messages for complex tasks
- [ ] Use `-Dcheckstyle.skip=true` during development

### Support & Community

**Documentation:**

- Platform README: `backend/README.md`
- Service READMEs: `backend/gripday-*/README.md`
- Frontend READMEs: `auth.gripday.com/README.md`, `app.gripday.com/README.md`
- AI Guidelines: `*/AGENTS.md`

**Contributing:**

- Follow conventional commits
- Add tests for new features
- Update documentation
- Run architecture tests
- Follow code review checklist

---

**Built with ❤️ for developers building scalable microservices applications**
