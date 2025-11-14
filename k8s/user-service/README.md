# Gripday User Service - Kubernetes Deployment

This directory contains Kubernetes manifests and deployment scripts for the Gripday User Service.

## Overview

The auth service provides centralized authentication, authorization, and user management for the entire Gripday platform. It's built with Spring Boot 3.5.6, Java 21, and follows the three-tier architecture pattern with JWT-based authentication.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Ingress       │    │   User Service  │    │   PostgreSQL    │
│   Controller    │───▶│   (Port 8080)   │───▶│   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │     Redis       │
                       │     Cache       │
                       └─────────────────┘
```

## Components

### Core Service

- **user-service**: Main Spring Boot application (port 8080)
- **user-postgres**: PostgreSQL 15.8 database for user data and authentication
- **user-redis**: Redis 7.4 for session management and caching

### Kubernetes Resources

- **Deployment**: Application pods with rolling update strategy
- **Service**: ClusterIP service for internal communication
- **Ingress**: HTTP/HTTPS routing with environment-specific CORS
- **ConfigMap**: Application configuration for each environment
- **Secret**: Sensitive configuration (JWT secrets, database credentials)
- **ServiceAccount**: Pod security account with minimal permissions
- **PodDisruptionBudget**: High availability configuration

## Deployment

### Prerequisites

1. **Kubernetes Cluster**: Running cluster with kubectl access
2. **Ingress Controller**: NGINX ingress controller installed
3. **Storage Class**: Available storage class for persistent volumes
4. **Docker Images**: Built auth service image
5. **Cert Manager**: For TLS certificates in staging/production

### Quick Deployment

```bash
# Deploy to local environment (minikube)
kubectl apply -f namespace.yaml
kubectl apply -f .

# Deploy specific environment
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f user-postgres-deployment.yaml
kubectl apply -f user-postgres-service.yaml
kubectl apply -f user-redis-deployment.yaml
kubectl apply -f user-redis-service.yaml
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml
kubectl apply -f user-service-ingress.yaml
```

### Manual Deployment Steps

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Apply configuration and secrets
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# 3. Deploy databases
kubectl apply -f user-postgres-deployment.yaml
kubectl apply -f user-postgres-service.yaml
kubectl apply -f user-redis-deployment.yaml
kubectl apply -f user-redis-service.yaml

# 4. Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=user-postgres -n gripday-auth --timeout=300s
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=user-redis -n gripday-auth --timeout=300s

# 5. Deploy application
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml
kubectl apply -f user-service-ingress.yaml

# 6. Verify deployment
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-user-service -n gripday-auth --timeout=300s
```

## Configuration

### Environment Variables

The service uses the following key environment variables:

```bash
# Database Configuration
GRIPDAY_DATABASE_URL=jdbc:postgresql://user-postgres:5432/gripday_user_local
GRIPDAY_DATABASE_USERNAME=auth_user
GRIPDAY_DATABASE_PASSWORD=auth_pass

# Redis Configuration
GRIPDAY_CACHE_REDIS_HOST=user-redis
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_DATABASE=0
GRIPDAY_CACHE_REDIS_PASSWORD=redis_pass

# JWT Configuration
GRIPDAY_AUTH_JWT_SECRET=<jwt-secret-key>

# OAuth2 Configuration (Optional)
GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID=<google-client-id>
GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_SECRET=<google-client-secret>
GRIPDAY_AUTH_OAUTH2_GOOGLE_ENABLED=false

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=gripday-user-service
```

### Resource Requirements

| Component    | CPU Request | CPU Limit | Memory Request | Memory Limit |
| ------------ | ----------- | --------- | -------------- | ------------ |
| User Service | 250m        | 500m      | 384Mi          | 768Mi        |
| PostgreSQL   | 250m        | 500m      | 256Mi          | 512Mi        |
| Redis        | 100m        | 200m      | 128Mi          | 256Mi        |

### Environment-Specific Configuration

#### Local Environment

- **Replicas**: 2
- **CORS**: Permissive (localhost, minikube)
- **TLS**: Disabled
- **Tracing**: 100% sampling
- **Log Level**: DEBUG

#### Staging Environment

- **Replicas**: 2
- **CORS**: Restricted to staging domain
- **TLS**: Let's Encrypt staging certificates
- **Tracing**: 10% sampling
- **Log Level**: INFO
- **Rate Limiting**: 100 requests/minute

#### Production Environment

- **Replicas**: 3
- **CORS**: Restricted to production domain
- **TLS**: Let's Encrypt production certificates
- **Tracing**: 1% sampling
- **Log Level**: WARN
- **Rate Limiting**: 1000 requests/minute, 50 RPS

## Networking

### Service Ports

- **User Service**: 8080 (HTTP)
- **PostgreSQL**: 5432
- **Redis**: 6379

### Ingress Routes

#### Local Environment

```
http://auth.gripday.site/api/v1/auth/*     → Authentication API
http://auth.gripday.site/api/v1/users/*    → User Management API
http://auth.gripday.site/swagger-ui/*      → Swagger UI
http://auth.gripday.site/actuator/*        → Health/metrics
```

#### Staging Environment

```
https://auth.gripday.website/api/v1/auth/*  → Authentication API
https://auth.gripday.website/api/v1/users/* → User Management API
```

#### Production Environment

```
https://auth.gripday.com/api/v1/auth/*  → Authentication API
https://auth.gripday.com/api/v1/users/* → User Management API
```

## Security

### Pod Security

- **Non-root user**: Runs as UID 1001
- **Read-only root filesystem**: Prevents runtime modifications
- **No privilege escalation**: Security hardening
- **Dropped capabilities**: Minimal required capabilities
- **Service account**: Dedicated service account with no token mounting

### Network Security

- **TLS termination**: At ingress level for staging/production
- **CORS policies**: Environment-specific CORS configuration
- **Rate limiting**: Request rate limiting at ingress
- **Pod anti-affinity**: Spread pods across nodes for availability

### Secrets Management

- **JWT secrets**: Secure random keys for token signing
- **Database credentials**: Environment-specific credentials
- **OAuth2 secrets**: External provider credentials
- **Redis passwords**: Optional for local, required for staging/production

## Monitoring and Observability

### Health Checks

- **Liveness Probe**: `/actuator/health/liveness` (60s delay, 30s interval)
- **Readiness Probe**: `/actuator/health/readiness` (30s delay, 10s interval)
- **Startup Probe**: `/actuator/health` (30s delay, 10s interval, 12 failures)

### Metrics

- **Prometheus metrics**: Available at `/actuator/prometheus`
- **Custom metrics**: Authentication operations, user registrations, JWT operations
- **JVM metrics**: Memory, GC, thread pools
- **Database metrics**: Connection pool, query performance
- **Redis metrics**: Cache hit rates, connection status

### Tracing

- **OpenTelemetry**: Distributed tracing integration
- **Jaeger**: Trace collection and visualization
- **Correlation IDs**: Request correlation across services
- **User context**: User and tenant information in traces

### Logging

- **Structured logging**: JSON format for staging/production
- **Log levels**: DEBUG (local), INFO (staging), WARN (production)
- **Audit logging**: Authentication events, user operations
- **Security logging**: Failed login attempts, suspicious activities

## API Endpoints

### Authentication Endpoints

- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User authentication
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/password/forgot` - Password reset request
- `POST /api/v1/password/reset` - Password reset confirmation

### User Management Endpoints (Admin Only)

- `GET /api/v1/users` - List users with pagination
- `GET /api/v1/users/{id}` - Get user details
- `POST /api/v1/users` - Create user (admin only)
- `PUT /api/v1/users/{id}` - Update user (admin only)
- `DELETE /api/v1/users/{id}` - Delete user (admin only)
- `PUT /api/v1/users/{id}/roles` - Update user roles (admin only)

### Tenant Management Endpoints (Admin Only)

- `GET /api/v1/tenants` - List tenants
- `POST /api/v1/tenants` - Create tenant
- `PUT /api/v1/tenants/{id}` - Update tenant
- `DELETE /api/v1/tenants/{id}` - Delete tenant

### Management Endpoints

- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /swagger-ui.html` - API documentation

## Troubleshooting

### Common Issues

1. **Pod not starting**

   ```bash
   kubectl describe pod -l app.kubernetes.io/name=gripday-user-service -n gripday-auth
   kubectl logs -l app.kubernetes.io/name=gripday-user-service -n gripday-auth
   ```

2. **Database connection issues**

   ```bash
   kubectl exec -it deployment/user-postgres -n gripday-auth -- psql -U auth_user -d gripday_user_local
   ```

3. **Redis connection issues**

   ```bash
   kubectl exec -it deployment/user-redis -n gripday-auth -- redis-cli ping
   ```

4. **JWT token validation issues**

   ```bash
   kubectl logs -f deployment/user-service -n gripday-auth | grep JWT
   ```

5. **Service not accessible**
   ```bash
   kubectl get ingress -n gripday-auth
   kubectl describe ingress user-service-ingress -n gripday-auth
   ```

### Useful Commands

```bash
# Check all resources
kubectl get all -n gripday-auth

# View logs
kubectl logs -f deployment/user-service -n gripday-auth

# Port forward for local access
kubectl port-forward service/user-service 8080:8080 -n gripday-auth

# Scale manually
kubectl scale deployment user-service --replicas=3 -n gripday-auth

# Check secrets
kubectl get secrets -n gripday-auth

# View configuration
kubectl describe configmap user-service-config -n gripday-auth
```

### Debug Authentication Flow

```bash
# Test user registration
curl -X POST http://auth.gripday.site/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Test user login
curl -X POST http://auth.gripday.site/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'

# Test token validation
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -H "Authorization: Bearer $TOKEN" \
     http://auth.gripday.site/api/v1/users/me
```

## Integration

### Gateway Service Integration

The auth service integrates with the gateway service for:

- **JWT token validation**: Shared JWT secret for token verification
- **User context propagation**: Standard JWT claims format
- **Multi-tenant support**: Tenant information in JWT tokens
- **Rate limiting coordination**: Shared Redis instance for distributed rate limiting

### Platform Integration

- **Microservice authentication**: All platform services validate JWT tokens
- **User context sharing**: Standardized user context format across services
- **Tenant isolation**: Multi-tenant data separation and routing
- **Observability**: Centralized tracing and metrics collection

### External Integrations

- **OAuth2 providers**: Google, GitHub, Microsoft (configurable)
- **Email services**: Password reset and notification emails
- **Audit systems**: Security event logging and monitoring
- **Identity providers**: LDAP, Active Directory (future)

## Development

### Local Development Setup

1. **Start minikube**

   ```bash
   minikube start
   minikube addons enable ingress
   ```

2. **Deploy auth service**

   ```bash
   kubectl apply -f namespace.yaml
   kubectl apply -f .
   ```

3. **Configure local DNS**

   ```bash
   # Add to /etc/hosts (Linux/Mac) or C:\Windows\System32\drivers\etc\hosts (Windows)
   echo "$(minikube ip) auth.gripday.site" >> /etc/hosts
   ```

4. **Access services**

   ```bash
   # Test health endpoint
   curl http://auth.gripday.site/actuator/health

   # Access Swagger UI
   open http://auth.gripday.site/swagger-ui.html
   ```

### Testing

```bash
# Run health checks
kubectl exec -it deployment/user-service -n gripday-auth -- curl localhost:8080/actuator/health

# Test database connectivity
kubectl exec -it deployment/user-postgres -n gripday-auth -- pg_isready -U auth_user

# Test Redis connectivity
kubectl exec -it deployment/user-redis -n gripday-auth -- redis-cli ping

# Load testing
kubectl run load-test --image=busybox --rm -it --restart=Never -- /bin/sh
# Inside the pod:
# while true; do wget -q -O- http://user-service:8080/actuator/health; sleep 1; done
```

## Maintenance

### Backup

- **Database backup**: Use pg_dump for PostgreSQL backups
- **Redis backup**: Use BGSAVE for Redis snapshots
- **Configuration backup**: Store manifests in version control
- **Secrets backup**: Secure backup of JWT keys and credentials

### Updates

- **Rolling updates**: Use deployment rolling update strategy
- **Database migrations**: Liquibase handles schema migrations automatically
- **Configuration updates**: Update ConfigMaps and restart pods
- **Security updates**: Regular updates of base images and dependencies

### Monitoring

- **Resource usage**: Monitor CPU, memory, and storage usage
- **Performance metrics**: Track response times and error rates
- **Security metrics**: Monitor authentication failures and suspicious activities
- **Business metrics**: Track user registrations, login patterns, and tenant usage

### Scaling

- **Horizontal scaling**: Add more auth service replicas
- **Database scaling**: Consider read replicas for high load
- **Redis scaling**: Use Redis Cluster for high availability
- **Load balancing**: Ensure proper load distribution across pods
