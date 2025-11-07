# Gripday Platform - Kubernetes Architecture

## Overview

The Gripday platform follows a **microservices architecture** with an **API Gateway pattern** (Backend for Frontend - BFF). This document describes the network architecture, ingress configuration, and security boundaries.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        INTERNET                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTPS
                       ▼
         ┌─────────────────────────────┐
         │   Kubernetes Ingress        │
         │   (NGINX Ingress Controller)│
         └─────────────┬───────────────┘
                       │
           ┌───────────┴────────────┐
           │                        │
           ▼                        ▼
   ┌──────────────┐         ┌──────────────┐
   │   Gateway    │         │   React UI   │
   │   Service    │         │  (Frontend)  │
   │  (Port 8080) │         │              │
   └──────┬───────┘         └──────────────┘
          │
          │ Internal Cluster Network
          │ (No Direct Internet Access)
          │
     ┌────┴────┬─────────────┐
     │         │             │
     ▼         ▼             ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│  Auth   │ │Bookstore│ │  Other  │
│ Service │ │ Service │ │Services │
│   8081  │ │   8082  │ │   ...   │
└────┬────┘ └────┬────┘ └─────────┘
     │           │
     ▼           ▼
  ┌──────┐    ┌──────┐
  │ DB   │    │ DB   │
  └──────┘    └──────┘
```

## Ingress Configuration

### Production & Staging Environments

#### ✅ **Services WITH Ingress**
1. **Gateway Service** - `api.pynity.com` / `api.pynity.website`
   - Single entry point for all backend APIs
   - Routes: `/api/v1/*`
   - TLS: Yes (Let's Encrypt)
   - Rate Limiting: Yes
   - CORS: Configured per environment

2. **Main Application** (when deployed) - `app.pynity.com` / `app.pynity.website`
   - Operational dashboard for logged-in users
   - Admin panel and management features
   - Requires authentication
   - TLS: Yes

3. **Auth UI** (when deployed) - `auth.pynity.com` / `auth.pynity.website`
   - Dedicated authentication frontend (login, signup, password reset)
   - Public-facing authentication pages
   - Isolated from main app for security
   - TLS: Yes

4. **Landing Page** (optional) - `pynity.com` / `pynity.website`
   - Marketing/promotional landing page
   - Public website
   - Can be static site or separate service

#### ❌ **Services WITHOUT Ingress**
1. **Auth Service** - Internal only
   - Accessible only via: `https://api.pynity.com/api/v1/auth/*` → Gateway → Auth
   - No direct internet access
   - Network policy: Only accepts traffic from gateway

2. **Bookstore Service** - Internal only
   - Accessible only via: `https://api.pynity.com/api/v1/bookstore/*` → Gateway → Bookstore
   - No direct internet access
   - Network policy: Only accepts traffic from gateway

3. **All Other Backend Services** - Internal only
   - No direct ingress
   - Must be routed through gateway

### Local Development Environment

In local development, **all services have direct ingress** for easier debugging:
- Auth: `http://auth.pynity.site`
- Bookstore: `http://localhost/api/v1/bookstore/*`
- Gateway: `http://api.pynity.site`

This allows developers to:
- Test services independently
- Access Swagger UI directly
- Debug without gateway overhead
- Use Postman/curl for direct testing

## API Routing Through Gateway

### Gateway Routes Configuration

The gateway service routes all external traffic to internal services:

```yaml
# Auth Service Routes
/api/v1/auth/**      → http://auth-service:8081/api/v1/auth/**
/api/v1/users/**     → http://auth-service:8081/api/v1/users/**
/api/v1/tenants/**   → http://auth-service:8081/api/v1/tenants/**

# Bookstore Service Routes
/api/v1/bookstore/** → http://bookstore-service:8082/api/v1/bookstore/**

# Gateway Health
/actuator/**         → http://localhost:8080/actuator/**
```

### Request Flow Example

```
1. Client Request:
   GET https://api.pynity.com/api/v1/bookstore/books/123

2. Ingress Controller:
   - Terminates TLS
   - Routes to gateway-service

3. Gateway Service:
   - Validates JWT (if required)
   - Applies rate limiting
   - Logs request with correlation ID
   - Routes to: http://bookstore-service.production-env:8082/api/v1/bookstore/books/123

4. Bookstore Service:
   - Receives request with user context headers
   - Validates JWT independently (defense in depth)
   - Processes request
   - Returns response

5. Response Path:
   Gateway → Ingress → Client
```

## Network Policies

### Gateway Service Network Policy

**Ingress:**
- ✅ From: Ingress Controller (nginx-ingress namespace)
- ✅ From: Monitoring (Prometheus)
- ✅ From: Same namespace (service mesh)

**Egress:**
- ✅ To: Auth Service (port 8081)
- ✅ To: Bookstore Service (port 8082)
- ✅ To: Redis (rate limiting)
- ✅ To: DNS
- ✅ To: Observability services (OTLP, Jaeger)

### Backend Services Network Policy (Auth, Bookstore, etc.)

**Ingress:**
- ✅ From: Gateway Service ONLY
- ✅ From: Same namespace (pod-to-pod)
- ✅ From: Monitoring (Prometheus)
- ❌ From: Ingress Controller (BLOCKED)
- ❌ From: Internet (BLOCKED)

**Egress:**
- ✅ To: Own database (PostgreSQL)
- ✅ To: Own cache (Redis)
- ✅ To: DNS
- ✅ To: Observability services
- ✅ To: Auth Service (for JWT validation - defense in depth)

## Security Benefits

### 1. **Reduced Attack Surface**
- Only 1-2 services exposed to internet (Gateway + UI)
- Backend services completely isolated
- Defense in depth with network policies

### 2. **Centralized Security**
- Single point for authentication
- Unified rate limiting
- Consistent CORS policies
- Centralized TLS certificate management

### 3. **Network Isolation**
- Backend services can't be accessed directly
- Even if gateway is compromised, backend services have additional security layers
- Database and cache services only accessible from their respective service pods

### 4. **Simplified Secrets Management**
- TLS certificates only on gateway/UI
- OAuth2 credentials only in auth service
- Database credentials isolated to service level

## Monitoring and Observability

### Access Points

All monitoring access goes through proper channels:

1. **Gateway Metrics**: `https://api.pynity.com/actuator/prometheus`
2. **Backend Service Metrics**: Scraped by Prometheus from internal cluster network
3. **Logs**: Collected by logging sidecar or DaemonSet
4. **Traces**: Sent to OTLP collector (internal service)

### Prometheus Scraping

Network policies allow Prometheus to scrape all services:
```yaml
- from:
  - namespaceSelector:
      matchLabels:
        app.kubernetes.io/name: monitoring
```

## Environment Comparison

| Feature | Local | Staging | Production |
|---------|-------|---------|------------|
| Backend Service Ingress | ✅ Yes (debugging) | ❌ No | ❌ No |
| Gateway Ingress | ✅ Yes | ✅ Yes | ✅ Yes |
| UI Ingress | ✅ Yes | ✅ Yes | ✅ Yes |
| TLS | ❌ No | ✅ Yes | ✅ Yes |
| Rate Limiting | 60/min | 120/min | 1000/min |
| Network Policies | ⚠️  Permissive | ✅ Enforced | ✅ Enforced |
| Direct Service Access | ✅ Allowed | ❌ Blocked | ❌ Blocked |

## Unified Namespace Strategy

### Staging Environment
- **Namespace**: `staging-env`
- **All Services**: Auth, Gateway, Bookstore in same namespace
- **Benefits**: Simplified RBAC, easier resource quotas, simpler network policies

### Production Environment
- **Namespace**: `production-env`
- **All Services**: Auth, Gateway, Bookstore in same namespace
- **Benefits**: Complete isolation from staging, unified monitoring

### Local Environment
- **Namespaces**: Separate per service (`gripday-auth`, `gripday-gateway`, `gripday-bookstore`)
- **Reason**: Mirrors production-like namespace complexity for testing

## API Gateway Pattern (BFF) Details

### Why Backend for Frontend (BFF)?

1. **API Composition**: Gateway can aggregate multiple backend calls
2. **Protocol Translation**: HTTP/REST → gRPC, WebSocket, etc.
3. **Security**: Centralized authentication and authorization
4. **Caching**: Response caching at gateway level
5. **Rate Limiting**: Protect backend services from overload
6. **Circuit Breaking**: Prevent cascading failures
7. **Request/Response Transformation**: Adapt backend APIs for frontend needs

### Gateway Features

- ✅ JWT Validation
- ✅ Rate Limiting (Redis-backed)
- ✅ Circuit Breaker (Resilience4j)
- ✅ Request Logging with Correlation IDs
- ✅ Distributed Tracing (OpenTelemetry)
- ✅ CORS Configuration
- ✅ API Versioning Support
- ✅ Header Injection (User Context, Tenant ID)

## Reserved Domains

### Production Domains
- **`pynity.com`** → Landing/Marketing page (optional)
- **`app.pynity.com`** → Main application (dashboard, admin, logged-in features)
- **`auth.pynity.com`** → Auth UI (login/signup frontend) - RESERVED
- **`api.pynity.com`** → Gateway Service (all backend APIs)

### Staging Domains
- **`pynity.website`** → Landing/Marketing page (optional)
- **`app.pynity.website`** → Main application (dashboard, admin, logged-in features)
- **`auth.pynity.website`** → Auth UI (login/signup frontend) - RESERVED
- **`api.pynity.website`** → Gateway Service (all backend APIs)

### API Access Examples

**Authentication APIs** (through gateway):
```bash
# Main Application (logged-in users)
https://app.pynity.com/dashboard           # Dashboard page
https://app.pynity.com/admin               # Admin panel
https://app.pynity.com/reports             # Reports

# Auth UI Frontend (public pages)
https://auth.pynity.com/login              # Login page
https://auth.pynity.com/signup             # Signup page
https://auth.pynity.com/forgot-password    # Password reset page

# Backend APIs (through gateway)
POST https://api.pynity.com/api/v1/auth/login       # Login API
POST https://api.pynity.com/api/v1/auth/signup      # Signup API
GET  https://api.pynity.com/api/v1/users/me         # Get user profile
GET  https://api.pynity.com/api/v1/bookstore/books  # Bookstore API
```

## Deployment

### Production Deployment

```bash
# Deploy using unified API Gateway pattern
./deploy-production.sh

# Only gateway ingress will be deployed
# Backend services remain internal
# Auth UI ingress can be deployed separately when ready
```

### Verification

```bash
# Check that only gateway has ingress
kubectl get ingress -A

# Should show:
# NAMESPACE        NAME                      HOSTS
# gripday-production-env   gateway-service-ingress   api.pynity.com

# Backend services should have NO ingress in production/staging
```

## Troubleshooting

### Backend Service Not Accessible

**Problem**: Can't reach auth or bookstore service
**Solution**: These are internal-only. Access via gateway:
```bash
# ❌ Wrong (direct access blocked)
curl https://auth.pynity.com/api/v1/auth/login

# ✅ Correct (through gateway)
curl https://api.pynity.com/api/v1/auth/login
```

### Network Policy Issues

Check if traffic is being blocked:
```bash
# Check network policies
kubectl get networkpolicies -n gripday-production-env

# Describe specific policy
kubectl describe networkpolicy auth-service-network-policy -n gripday-production-env
```

### Gateway Routing Issues

Check gateway routes:
```bash
# Access gateway actuator
kubectl port-forward svc/gateway-service 8080:8080 -n gripday-production-env

# Check routes
curl http://localhost:8080/actuator/gateway/routes
```

## Future Enhancements

1. **Service Mesh**: Consider Istio/Linkerd for advanced traffic management
2. **API Management**: Kong or Apigee for enterprise API management
3. **GraphQL Gateway**: Unified GraphQL API over REST services
4. **WebSocket Support**: Real-time communication through gateway
5. **Multi-Region**: Geographic routing through gateway

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Kubernetes Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
- [BFF Pattern](https://samnewman.io/patterns/architectural/bff/)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
