# Gripday Platform - Domain Mapping

This document provides a clear mapping of all domains and their purposes across environments.

## Production Domains (`*.gripday.com`)

| Domain               | Purpose                | Service         | Ingress | Status           |
| -------------------- | ---------------------- | --------------- | ------- | ---------------- |
| **gripday.com**      | Marketing/Landing Page | Static Site     | ✅ Yes  | 🟡 Optional      |
| **app.gripday.com**  | Main Web Application   | React/Vue App   | ✅ Yes  | 🟡 When deployed |
| **auth.gripday.com** | Auth UI (Login/Signup) | Auth Frontend   | ✅ Yes  | 🟡 RESERVED      |
| **api.gripday.com**  | API Gateway            | Gateway Service | ✅ Yes  | ✅ Active        |

### Application Details

- **app.gripday.com**: Operational dashboard, admin panel, logged-in user features
- **auth.gripday.com**: Public authentication pages (login, signup, password reset)
- **api.gripday.com**: All backend API endpoints

### Backend Services (Internal Only - NO Ingress)

| Service           | Port    | Access Method                                |
| ----------------- | ------- | -------------------------------------------- |
| Auth Service      | 8080    | `https://api.gripday.com/api/v1/auth/*`      |
| Bookstore Service | 8080    | `https://api.gripday.com/api/v1/bookstore/*` |
| Other Services    | Various | `https://api.gripday.com/api/v1/*`           |

## Staging Domains (`*.gripday.website`)

| Domain                   | Purpose                | Service         | Ingress | Status           |
| ------------------------ | ---------------------- | --------------- | ------- | ---------------- |
| **gripday.website**      | Marketing/Landing Page | Static Site     | ✅ Yes  | 🟡 Optional      |
| **app.gripday.website**  | Main Web Application   | React/Vue App   | ✅ Yes  | 🟡 When deployed |
| **auth.gripday.website** | Auth UI (Login/Signup) | Auth Frontend   | ✅ Yes  | 🟡 RESERVED      |
| **api.gripday.website**  | API Gateway            | Gateway Service | ✅ Yes  | ✅ Active        |

### Application Details

- **app.gripday.website**: Operational dashboard, admin panel, logged-in user features
- **auth.gripday.website**: Public authentication pages (login, signup, password reset)
- **api.gripday.website**: All backend API endpoints

### Backend Services (Internal Only - NO Ingress)

| Service           | Port    | Access Method                                    |
| ----------------- | ------- | ------------------------------------------------ |
| Auth Service      | 8080    | `https://api.gripday.website/api/v1/auth/*`      |
| Bookstore Service | 8080    | `https://api.gripday.website/api/v1/bookstore/*` |
| Other Services    | Various | `https://api.gripday.website/api/v1/*`           |

## Local Development Domains

| Domain                     | Purpose                    | Service           | Port      |
| -------------------------- | -------------------------- | ----------------- | --------- |
| **localhost**              | Main Application           | React/Vue App     | 3000/5173 |
| **auth.gripday.site**      | Auth Service (Direct)      | Auth Service      | 8080      |
| **api.gripday.site**       | API Gateway                | Gateway Service   | 8080      |
| **bookstore.gripday.site** | Bookstore Service (Direct) | Bookstore Service | 8080      |

> **Note**: In local development, backend services have direct ingress for debugging purposes only.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          INTERNET                                │
└───────────────────┬─────────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┬───────────────────┐
        │           │           │                   │
        │ HTTPS     │ HTTPS     │ HTTPS             │
        ▼           ▼           ▼                   │
┌──────────────┐ ┌──────────┐ ┌──────────────┐    │
│app.gripday   │ │auth.grip │ │api.gripday   │    │
│.com          │ │day.com   │ │.com          │    │
│              │ │          │ │              │    │
│ Main App     │ │ Auth UI  │ │ Gateway      │    │
│ (Dashboard)  │ │ (Login)  │ │ Service      │    │
└──────────────┘ └──────────┘ └──────┬───────┘    │
                                      │             │
                                      │ Internal    │
                                      │ Network     │
                                      │             │
                         ┌────────────┴──────────┐  │
                         │                       │  │
                         ▼                       ▼  │
                  ┌──────────┐          ┌──────────┐
                  │  Auth    │          │Bookstore │
                  │ Service  │          │ Service  │
                  │  8080    │          │  8080    │
                  └──────────┘          └──────────┘
                  (NO Ingress)          (NO Ingress)
```

---

## Domain Purpose Details

### 1. Main Application UI (`app.gripday.com` / `app.gripday.website`)

**Purpose**: Main web application for logged-in users

**Serves**:

- Operational Dashboard
- Admin Panel
- User profile pages
- Business logic pages
- Reports and analytics
- Settings and configuration
- Team management

**Requires**: Authentication (user must be logged in)

**Makes API calls to**: `api.gripday.com/api/v1/*`

**Example**:

```
https://app.gripday.com/dashboard
https://app.gripday.com/admin
https://app.gripday.com/profile
https://app.gripday.com/reports
https://app.gripday.com/settings
```

---

### 2. Auth UI (`auth.gripday.com` / `auth.gripday.website`) - RESERVED

**Purpose**: Dedicated authentication frontend application

**Serves**:

- Login page
- Signup/registration
- Password reset
- Email verification
- OAuth2 callback handling
- Two-factor authentication

**Makes API calls to**: `api.gripday.com/api/v1/auth/*`

**Example**:

```
https://auth.gripday.com/login
https://auth.gripday.com/signup
https://auth.gripday.com/forgot-password
https://auth.gripday.com/reset-password
https://auth.gripday.com/verify-email
https://auth.gripday.com/oauth/google/callback
```

**Why separate**:

- ✅ Security isolation from main app
- ✅ Can apply stricter CSP policies
- ✅ Independent deployment and updates
- ✅ Can be served from CDN
- ✅ Better performance (lighter bundle)
- ✅ Easier to customize branding

**File**: `auth-ui-ingress.yaml.example` (template provided)

---

### 3. API Gateway (`api.gripday.com` / `api.gripday.website`)

**Purpose**: Single entry point for all backend APIs

**Routes**:

```yaml
/api/v1/auth/**       → Auth Service (8080)
/api/v1/users/**      → Auth Service (8080)
/api/v1/tenants/**    → Auth Service (8080)
/api/v1/bookstore/**  → Bookstore Service (8080)
/actuator/**          → Gateway health endpoints
```

**Example API calls**:

```bash
# Login API (called by Auth UI)
POST https://api.gripday.com/api/v1/auth/login

# Get user profile (called by Main UI)
GET https://api.gripday.com/api/v1/users/me

# List books (called by Main UI)
GET https://api.gripday.com/api/v1/bookstore/books
```

---

## User Flow Example

### Login Flow

```
1. User visits main app: https://app.gripday.com
   └─> App detects no auth token

2. Redirect to Auth UI: https://auth.gripday.com/login
   └─> Auth UI shows login form

3. User submits credentials
   └─> POST https://api.gripday.com/api/v1/auth/login
       └─> Gateway routes to Auth Service (internal)
           └─> Returns JWT token

4. Auth UI stores token
   └─> Redirect back: https://app.gripday.com/dashboard

5. Main app makes authenticated requests
   └─> GET https://api.gripday.com/api/v1/users/me
       └─> Include Authorization: Bearer <token>
```

---

## Common Mistakes to Avoid

### ❌ WRONG - Direct backend access

```bash
# These will NOT work in staging/production:
https://auth.gripday.com/api/v1/auth/login          # Wrong! This is for UI
https://bookstore.gripday.com/api/v1/books          # Wrong! No ingress

# Backend services have NO direct ingress
```

### ✅ CORRECT - Through API Gateway

```bash
# All backend APIs go through gateway:
https://api.gripday.com/api/v1/auth/login           # Correct!
https://api.gripday.com/api/v1/bookstore/books      # Correct!
```

### ✅ CORRECT - Frontend applications

```bash
# Frontend UIs have their own ingresses:
https://app.gripday.com/dashboard                   # Main App (logged-in)
https://auth.gripday.com/login                      # Auth UI (public)
```

---

## DNS Configuration Required

### Production (gripday.com)

```
gripday.com           A/CNAME  → Landing/Marketing Site (optional)
app.gripday.com       A/CNAME  → Ingress Controller IP/hostname
auth.gripday.com      A/CNAME  → Ingress Controller IP/hostname
api.gripday.com       A/CNAME  → Ingress Controller IP/hostname
```

### Staging (gripday.website)

```
gripday.website         A/CNAME  → Landing/Marketing Site (optional)
app.gripday.website     A/CNAME  → Ingress Controller IP/hostname
auth.gripday.website    A/CNAME  → Ingress Controller IP/hostname
api.gripday.website     A/CNAME  → Ingress Controller IP/hostname
```

---

## Security Considerations

### Auth UI Domain (`auth.gripday.com`)

- ✅ Stricter Content Security Policy (CSP)
- ✅ HSTS with preload
- ✅ Frame-Options: DENY
- ✅ Rate limiting on auth endpoints
- ✅ Separate TLS certificate
- ✅ Can apply Cloudflare Bot Protection
- ✅ Isolated from main app vulnerabilities

### API Gateway Domain (`api.gripday.com`)

- ✅ JWT validation
- ✅ Rate limiting per endpoint
- ✅ Request logging with correlation IDs
- ✅ Circuit breaker patterns
- ✅ CORS policies enforced
- ✅ No cookies (stateless)

---

## Future Considerations

### Additional Domains (when needed)

```
admin.gripday.com     → Admin panel UI
docs.gripday.com      → API documentation
cdn.gripday.com       → Static assets CDN
ws.gripday.com        → WebSocket gateway
```

---

## Quick Reference

| Need                     | Use                                              |
| ------------------------ | ------------------------------------------------ |
| User login page          | `https://auth.gripday.com/login`                 |
| Login API call           | `POST https://api.gripday.com/api/v1/auth/login` |
| Main app dashboard       | `https://app.gripday.com/dashboard`              |
| Admin panel              | `https://app.gripday.com/admin`                  |
| Any backend API          | `https://api.gripday.com/api/v1/*`               |
| Backend service directly | ❌ Not possible (internal only)                  |

---

## Verification Commands

```bash
# Check active ingresses
kubectl get ingress -A

# Expected in staging/production:
# NAMESPACE        NAME                          HOSTS
# staging-env      gateway-service-ingress       api.gripday.website
# staging-env      auth-ui-ingress (when ready)  auth.gripday.website
# staging-env      main-app-ingress (when ready) app.gripday.website

# Test API gateway
curl https://api.gripday.com/actuator/health

# Auth UI (when deployed)
curl https://auth.gripday.com

# Should NOT exist:
# auth-service-ingress (backend - no direct access)
# bookstore-service-ingress (backend - no direct access)
```

---

**Last Updated**: Implementation of API Gateway architecture with proper domain separation
