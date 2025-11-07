# Gripday Platform - Domain Mapping

This document provides a clear mapping of all domains and their purposes across environments.

## Production Domains (`*.pynity.com`)

| Domain | Purpose | Service | Ingress | Status |
|--------|---------|---------|---------|--------|
| **pynity.com** | Marketing/Landing Page | Static Site | ✅ Yes | 🟡 Optional |
| **app.pynity.com** | Main Web Application | React/Vue App | ✅ Yes | 🟡 When deployed |
| **auth.pynity.com** | Auth UI (Login/Signup) | Auth Frontend | ✅ Yes | 🟡 RESERVED |
| **api.pynity.com** | API Gateway | Gateway Service | ✅ Yes | ✅ Active |

### Application Details
- **app.pynity.com**: Operational dashboard, admin panel, logged-in user features
- **auth.pynity.com**: Public authentication pages (login, signup, password reset)
- **api.pynity.com**: All backend API endpoints

### Backend Services (Internal Only - NO Ingress)
| Service | Port | Access Method |
|---------|------|---------------|
| Auth Service | 8081 | `https://api.pynity.com/api/v1/auth/*` |
| Bookstore Service | 8082 | `https://api.pynity.com/api/v1/bookstore/*` |
| Other Services | Various | `https://api.pynity.com/api/v1/*` |

## Staging Domains (`*.pynity.website`)

| Domain | Purpose | Service | Ingress | Status |
|--------|---------|---------|---------|--------|
| **pynity.website** | Marketing/Landing Page | Static Site | ✅ Yes | 🟡 Optional |
| **app.pynity.website** | Main Web Application | React/Vue App | ✅ Yes | 🟡 When deployed |
| **auth.pynity.website** | Auth UI (Login/Signup) | Auth Frontend | ✅ Yes | 🟡 RESERVED |
| **api.pynity.website** | API Gateway | Gateway Service | ✅ Yes | ✅ Active |

### Application Details
- **app.pynity.website**: Operational dashboard, admin panel, logged-in user features
- **auth.pynity.website**: Public authentication pages (login, signup, password reset)
- **api.pynity.website**: All backend API endpoints

### Backend Services (Internal Only - NO Ingress)
| Service | Port | Access Method |
|---------|------|---------------|
| Auth Service | 8081 | `https://api.pynity.website/api/v1/auth/*` |
| Bookstore Service | 8082 | `https://api.pynity.website/api/v1/bookstore/*` |
| Other Services | Various | `https://api.pynity.website/api/v1/*` |

## Local Development Domains

| Domain | Purpose | Service | Port |
|--------|---------|---------|------|
| **localhost** | Main Application | React/Vue App | 3000/5173 |
| **auth.pynity.site** | Auth Service (Direct) | Auth Service | 8081 |
| **api.pynity.site** | API Gateway | Gateway Service | 8080 |
| **bookstore.pynity.site** | Bookstore Service (Direct) | Bookstore Service | 8082 |

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
                  │  8081    │          │  8082    │
                  └──────────┘          └──────────┘
                  (NO Ingress)          (NO Ingress)
```

---

## Domain Purpose Details

### 1. Main Application UI (`app.pynity.com` / `app.pynity.website`)

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

**Makes API calls to**: `api.pynity.com/api/v1/*`

**Example**:
```
https://app.pynity.com/dashboard
https://app.pynity.com/admin
https://app.pynity.com/profile
https://app.pynity.com/reports
https://app.pynity.com/settings
```

---

### 2. Auth UI (`auth.pynity.com` / `auth.pynity.website`) - RESERVED

**Purpose**: Dedicated authentication frontend application

**Serves**:
- Login page
- Signup/registration
- Password reset
- Email verification
- OAuth2 callback handling
- Two-factor authentication

**Makes API calls to**: `api.pynity.com/api/v1/auth/*`

**Example**:
```
https://auth.pynity.com/login
https://auth.pynity.com/signup
https://auth.pynity.com/forgot-password
https://auth.pynity.com/reset-password
https://auth.pynity.com/verify-email
https://auth.pynity.com/oauth/google/callback
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

### 3. API Gateway (`api.pynity.com` / `api.pynity.website`)

**Purpose**: Single entry point for all backend APIs

**Routes**:
```yaml
/api/v1/auth/**       → Auth Service (8081)
/api/v1/users/**      → Auth Service (8081)
/api/v1/tenants/**    → Auth Service (8081)
/api/v1/bookstore/**  → Bookstore Service (8082)
/actuator/**          → Gateway health endpoints
```

**Example API calls**:
```bash
# Login API (called by Auth UI)
POST https://api.pynity.com/api/v1/auth/login

# Get user profile (called by Main UI)
GET https://api.pynity.com/api/v1/users/me

# List books (called by Main UI)
GET https://api.pynity.com/api/v1/bookstore/books
```

---

## User Flow Example

### Login Flow

```
1. User visits main app: https://app.pynity.com
   └─> App detects no auth token
   
2. Redirect to Auth UI: https://auth.pynity.com/login
   └─> Auth UI shows login form
   
3. User submits credentials
   └─> POST https://api.pynity.com/api/v1/auth/login
       └─> Gateway routes to Auth Service (internal)
           └─> Returns JWT token
           
4. Auth UI stores token
   └─> Redirect back: https://app.pynity.com/dashboard
   
5. Main app makes authenticated requests
   └─> GET https://api.pynity.com/api/v1/users/me
       └─> Include Authorization: Bearer <token>
```

---

## Common Mistakes to Avoid

### ❌ WRONG - Direct backend access
```bash
# These will NOT work in staging/production:
https://auth.pynity.com/api/v1/auth/login          # Wrong! This is for UI
https://bookstore.pynity.com/api/v1/books          # Wrong! No ingress

# Backend services have NO direct ingress
```

### ✅ CORRECT - Through API Gateway
```bash
# All backend APIs go through gateway:
https://api.pynity.com/api/v1/auth/login           # Correct!
https://api.pynity.com/api/v1/bookstore/books      # Correct!
```

### ✅ CORRECT - Frontend applications
```bash
# Frontend UIs have their own ingresses:
https://app.pynity.com/dashboard                   # Main App (logged-in)
https://auth.pynity.com/login                      # Auth UI (public)
```

---

## DNS Configuration Required

### Production (pynity.com)
```
pynity.com           A/CNAME  → Landing/Marketing Site (optional)
app.pynity.com       A/CNAME  → Ingress Controller IP/hostname
auth.pynity.com      A/CNAME  → Ingress Controller IP/hostname
api.pynity.com       A/CNAME  → Ingress Controller IP/hostname
```

### Staging (pynity.website)
```
pynity.website         A/CNAME  → Landing/Marketing Site (optional)
app.pynity.website     A/CNAME  → Ingress Controller IP/hostname
auth.pynity.website    A/CNAME  → Ingress Controller IP/hostname
api.pynity.website     A/CNAME  → Ingress Controller IP/hostname
```

---

## Security Considerations

### Auth UI Domain (`auth.pynity.com`)
- ✅ Stricter Content Security Policy (CSP)
- ✅ HSTS with preload
- ✅ Frame-Options: DENY
- ✅ Rate limiting on auth endpoints
- ✅ Separate TLS certificate
- ✅ Can apply Cloudflare Bot Protection
- ✅ Isolated from main app vulnerabilities

### API Gateway Domain (`api.pynity.com`)
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
admin.pynity.com     → Admin panel UI
docs.pynity.com      → API documentation
cdn.pynity.com       → Static assets CDN
ws.pynity.com        → WebSocket gateway
```

---

## Quick Reference

| Need | Use |
|------|-----|
| User login page | `https://auth.pynity.com/login` |
| Login API call | `POST https://api.pynity.com/api/v1/auth/login` |
| Main app dashboard | `https://app.pynity.com/dashboard` |
| Admin panel | `https://app.pynity.com/admin` |
| Any backend API | `https://api.pynity.com/api/v1/*` |
| Backend service directly | ❌ Not possible (internal only) |

---

## Verification Commands

```bash
# Check active ingresses
kubectl get ingress -A

# Expected in staging/production:
# NAMESPACE        NAME                          HOSTS
# staging-env      gateway-service-ingress       api.pynity.website
# staging-env      auth-ui-ingress (when ready)  auth.pynity.website
# staging-env      main-app-ingress (when ready) app.pynity.website

# Test API gateway
curl https://api.pynity.com/actuator/health

# Auth UI (when deployed)
curl https://auth.pynity.com

# Should NOT exist:
# auth-service-ingress (backend - no direct access)
# bookstore-service-ingress (backend - no direct access)
```

---

**Last Updated**: Implementation of API Gateway architecture with proper domain separation
