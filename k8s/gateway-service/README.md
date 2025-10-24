# Gripday Gateway Service - Kubernetes Deployment

This directory contains Kubernetes manifests and deployment scripts for the Gripday Gateway Service.

## Overview

The gateway service is the central entry point for the Gripday platform, providing intelligent API routing, JWT authentication, rate limiting, and circuit breaker functionality. Built with Spring Cloud Gateway, it uses reactive programming patterns for high-performance request handling and integrates with Redis for distributed rate limiting.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Ingress       │    │   Gateway       │    │   Auth Service  │
│   Controller    │───▶│   Service       │───▶│   (Port 8081)   │
└─────────────────┘    │   (Port 8080)   │    └─────────────────┘
                       └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │     Redis       │
                       │  Rate Limiting  │
                       └─────────────────┘
```

## Components

### Core Service
- **gateway-service**: Main Spring Cloud Gateway application (port 8080)
- **gateway-redis**: Redis 7.4 for rate limiting and session management

### Kubernetes Resources
- **Deployment**: Application pods with rolling update strategy
- **Service**: ClusterIP service for internal communication
- **Ingress**: HTTP/HTTPS routing with environment-specific configuration
- **HPA**: Horizontal Pod Autoscaler with advanced scaling policies
- **NetworkPolicy**: Security policies for network isolation
- **ConfigMap**: Application configuration for routing and policies
- **Secret**: Sensitive configuration (JWT secrets, Redis passwords)
- **ServiceAccount**: Pod security account with minimal permissions
- **PodDisruptionBudget**: High availability configuration

## Deployment

### Prerequisites

1. **Kubernetes Cluster**: Running cluster with kubectl access
2. **Ingress Controller**: NGINX ingress controller installed
3. **Storage Class**: Available storage class for Redis persistent volumes
4. **Docker Images**: Built gateway service image
5. **Cert Manager**: For TLS certificates in staging/production
6. **Auth Service**: Running auth service for JWT validation

### Quick Deployment

```bash
# Deploy to local environment (minikube)
kubectl apply -f namespace.yaml
kubectl apply -f .

# Deploy specific environment
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f gateway-redis-deployment.yaml
kubectl apply -f gateway-redis-service.yaml
kubectl apply -f gateway-service-deployment.yaml
kubectl apply -f gateway-service-service.yaml
kubectl apply -f gateway-service-hpa.yaml
kubectl apply -f network-policy.yaml
kubectl apply -f gateway-service-ingress.yaml
```

### Manual Deployment Steps

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Apply configuration and secrets
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# 3. Deploy Redis for rate limiting
kubectl apply -f gateway-redis-deployment.yaml
kubectl apply -f gateway-redis-service.yaml

# 4. Wait for Redis to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gateway-redis -n gripday-gateway --timeout=300s

# 5. Deploy gateway service
kubectl apply -f gateway-service-deployment.yaml
kubectl apply -f gateway-service-service.yaml
kubectl apply -f gateway-service-hpa.yaml
kubectl apply -f network-policy.yaml
kubectl apply -f gateway-service-ingress.yaml

# 6. Verify deployment
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-gateway-service -n gripday-gateway --timeout=300s
```

## Configuration

### Environment Variables

The service uses the following key environment variables:

```bash
# Redis Configuration
GRIPDAY_CACHE_REDIS_HOST=gateway-redis
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_DATABASE=1
GRIPDAY_CACHE_REDIS_PASSWORD=redis_pass

# JWT Configuration (must match auth service)
GRIPDAY_AUTH_JWT_SECRET=<shared-jwt-secret>

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=gripday-gateway-service

# JVM Configuration (optimized for reactive workloads)
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseZGC
```

### Resource Requirements

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Gateway Service | 500m | 1000m | 768Mi | 1536Mi |
| Redis | 100m | 200m | 256Mi | 512Mi |

### Environment-Specific Configuration

#### Local Environment
- **Replicas**: 3
- **CORS**: Permissive (localhost, minikube)
- **TLS**: Disabled
- **Rate Limiting**: 60 requests/minute
- **Tracing**: 100% sampling
- **HPA**: 3-10 replicas, 70% CPU threshold

#### Staging Environment
- **Replicas**: 2
- **CORS**: Restricted to staging domain
- **TLS**: Let's Encrypt staging certificates
- **Rate Limiting**: 120 requests/minute
- **Tracing**: 10% sampling
- **HPA**: 2-6 replicas, 70% CPU threshold

#### Production Environment
- **Replicas**: 5
- **CORS**: Restricted to production domain
- **TLS**: Let's Encrypt production certificates
- **Rate Limiting**: 1000 requests/minute
- **Tracing**: 1% sampling
- **HPA**: 5-20 replicas, 60% CPU threshold

## Networking

### Service Ports
- **Gateway Service**: 8080 (HTTP)
- **Redis**: 6379

### Ingress Routes

#### Local Environment
```
http://api.local.gripday.com/api/v1/auth/*     → Auth Service
http://api.local.gripday.com/actuator/*        → Gateway Health/Metrics
```

#### Staging Environment
```
https://api.staging.gripday.com/api/v1/auth/*  → Auth Service
https://api.staging.gripday.com/actuator/*     → Gateway Health/Metrics
```

#### Production Environment
```
https://api.gripday.com/api/v1/auth/*  → Auth Service
https://api.gripday.com/actuator/*     → Gateway Health/Metrics
```

### Routing Configuration

The gateway service routes requests to backend services:

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: auth-service
        uri: http://auth-service.gripday-auth.svc.cluster.local:8081
        predicates:
        - Path=/api/*/auth/**
        filters:
        - JwtAuthenticationFilter
        - RateLimitingFilter
```

## Security

### Pod Security
- **Non-root user**: Runs as UID 1001
- **Read-only root filesystem**: Prevents runtime modifications
- **No privilege escalation**: Security hardening
- **Dropped capabilities**: Minimal required capabilities
- **Service account**: Dedicated service account with no token mounting

### Network Security
- **Network policies**: Restrict traffic between pods
- **TLS termination**: At ingress level for staging/production
- **CORS policies**: Environment-specific CORS configuration
- **Rate limiting**: Distributed rate limiting with Redis
- **Pod anti-affinity**: Spread pods across nodes for availability

### Authentication & Authorization
- **JWT validation**: Validates tokens from auth service
- **User context propagation**: Extracts user information from JWT
- **Tenant context**: Multi-tenant request routing
- **Protected routes**: Automatic authentication for protected endpoints

## Rate Limiting

### Configuration
- **Backend**: Redis-based distributed rate limiting
- **Algorithm**: Token bucket with burst capacity
- **Granularity**: Per-user, per-IP, per-tenant
- **Fallback**: Circuit breaker on rate limit failures

### Rate Limits by Environment

| Environment | Requests/Minute | Burst Capacity | Replenish Rate |
|-------------|-----------------|----------------|----------------|
| Local | 60 | 10 | 1/second |
| Staging | 120 | 20 | 2/second |
| Production | 1000 | 100 | 16/second |

### Rate Limiting Headers
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1640995200
```

## Circuit Breaker

### Configuration
- **Failure Rate Threshold**: 50%
- **Wait Duration**: 30 seconds (open state)
- **Sliding Window**: 10 requests
- **Minimum Calls**: 5 requests

### Circuit Breaker States
- **Closed**: Normal operation, requests pass through
- **Open**: Failures exceed threshold, requests fail fast
- **Half-Open**: Test requests to check if service recovered

## Monitoring and Observability

### Health Checks
- **Liveness Probe**: `/actuator/health/liveness` (60s delay, 30s interval)
- **Readiness Probe**: `/actuator/health/readiness` (30s delay, 10s interval)
- **Startup Probe**: `/actuator/health` (30s delay, 10s interval, 12 failures)

### Metrics
- **Prometheus metrics**: Available at `/actuator/prometheus`
- **Gateway metrics**: Request routing, response times, error rates
- **Rate limiting metrics**: Rate limit hits, remaining capacity
- **Circuit breaker metrics**: Circuit state, failure rates
- **JVM metrics**: Memory, GC, thread pools optimized for reactive workloads

### Tracing
- **OpenTelemetry**: Distributed tracing integration
- **Jaeger**: Trace collection and visualization
- **Correlation IDs**: Request correlation across all services
- **User context**: User and tenant information in traces

### Logging
- **Structured logging**: JSON format for staging/production
- **Log levels**: DEBUG (local), INFO (staging), WARN (production)
- **Request logging**: HTTP requests, routing decisions, authentication events
- **Security logging**: Rate limiting events, authentication failures

## Auto-Scaling

### Horizontal Pod Autoscaler (HPA)

#### Local Environment
```yaml
minReplicas: 3
maxReplicas: 10
metrics:
  - CPU: 70%
  - Memory: 80%
  - HTTP RPS: 100/pod
```

#### Staging Environment
```yaml
minReplicas: 2
maxReplicas: 6
metrics:
  - CPU: 70%
  - Memory: 80%
```

#### Production Environment
```yaml
minReplicas: 5
maxReplicas: 20
metrics:
  - CPU: 60%
  - Memory: 70%
  - HTTP RPS: 500/pod
```

### Scaling Policies
- **Scale Up**: Fast response (50% increase, max 4 pods/minute)
- **Scale Down**: Conservative (10% decrease, max 2 pods/5 minutes)
- **Stabilization**: Prevent flapping with stabilization windows

## API Endpoints

### Gateway Management
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /actuator/gateway/routes` - Current route configuration
- `POST /actuator/gateway/refresh` - Refresh route configuration

### Proxied Endpoints
All requests are proxied to backend services:
- `/api/v1/auth/**` → Auth Service
- `/api/v1/users/**` → Auth Service (User Management)
- `/api/v1/tenants/**` → Auth Service (Tenant Management)

### Headers Added by Gateway
- `X-Correlation-ID` - Request correlation ID
- `X-Request-ID` - Unique request identifier
- `X-User-ID` - Authenticated user ID (from JWT)
- `X-Tenant-ID` - Tenant context (from JWT or header)

## Troubleshooting

### Common Issues

1. **Gateway not starting**
   ```bash
   kubectl describe pod -l app.kubernetes.io/name=gripday-gateway-service -n gripday-gateway
   kubectl logs -l app.kubernetes.io/name=gripday-gateway-service -n gripday-gateway
   ```

2. **Redis connection issues**
   ```bash
   kubectl exec -it deployment/gateway-redis -n gripday-gateway -- redis-cli ping
   kubectl logs -f deployment/gateway-redis -n gripday-gateway
   ```

3. **Route not working**
   ```bash
   kubectl exec -it deployment/gateway-service -n gripday-gateway -- curl localhost:8080/actuator/gateway/routes
   ```

4. **Rate limiting issues**
   ```bash
   kubectl exec -it deployment/gateway-redis -n gripday-gateway -- redis-cli monitor
   kubectl logs -f deployment/gateway-service -n gripday-gateway | grep -i rate
   ```

5. **JWT validation failures**
   ```bash
   kubectl logs -f deployment/gateway-service -n gripday-gateway | grep -i jwt
   ```

### Useful Commands

```bash
# Check all resources
kubectl get all -n gripday-gateway

# View logs
kubectl logs -f deployment/gateway-service -n gripday-gateway

# Port forward for local access
kubectl port-forward service/gateway-service 8080:8080 -n gripday-gateway

# Check HPA status
kubectl get hpa -n gripday-gateway
kubectl describe hpa gateway-service-hpa -n gripday-gateway

# View current routes
kubectl exec -it deployment/gateway-service -n gripday-gateway -- curl localhost:8080/actuator/gateway/routes

# Check Redis status
kubectl exec -it deployment/gateway-redis -n gripday-gateway -- redis-cli info

# View network policies
kubectl get networkpolicy -n gripday-gateway
```

### Debug Gateway Routing

```bash
# Test gateway health
curl http://api.local.gripday.com/actuator/health

# Test auth service routing
curl -X POST http://api.local.gripday.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'

# Test with authentication
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: default" \
     http://api.local.gripday.com/api/v1/users/me

# Test rate limiting
for i in {1..70}; do
  curl -w "%{http_code}\n" -o /dev/null -s http://api.local.gripday.com/actuator/health
done
```

## Integration

### Auth Service Integration
- **JWT validation**: Validates tokens using shared secret
- **User context**: Extracts user information from JWT tokens
- **Route protection**: Automatically protects routes requiring authentication
- **Token refresh**: Handles token refresh flows

### Backend Service Integration
- **Service discovery**: Routes to services via Kubernetes DNS
- **Load balancing**: Distributes requests across service replicas
- **Health checking**: Monitors backend service health
- **Circuit breaking**: Protects against cascading failures

### Observability Stack Integration
- **Prometheus**: Metrics collection and alerting
- **Grafana**: Dashboards and visualization
- **Jaeger**: Distributed tracing
- **Loki**: Log aggregation and analysis

## Development

### Local Development Setup

1. **Start minikube**
   ```bash
   minikube start
   minikube addons enable ingress
   ```

2. **Deploy auth service first**
   ```bash
   kubectl apply -f ../auth-service/
   ```

3. **Deploy gateway service**
   ```bash
   kubectl apply -f namespace.yaml
   kubectl apply -f .
   ```

4. **Configure local DNS**
   ```bash
   # Add to /etc/hosts (Linux/Mac) or C:\Windows\System32\drivers\etc\hosts (Windows)
   echo "$(minikube ip) api.local.gripday.com" >> /etc/hosts
   ```

5. **Test gateway**
   ```bash
   curl http://api.local.gripday.com/actuator/health
   ```

### Testing

```bash
# Test gateway health
kubectl exec -it deployment/gateway-service -n gripday-gateway -- curl localhost:8080/actuator/health

# Test Redis connectivity
kubectl exec -it deployment/gateway-redis -n gripday-gateway -- redis-cli ping

# Test route configuration
kubectl exec -it deployment/gateway-service -n gripday-gateway -- curl localhost:8080/actuator/gateway/routes

# Load testing
kubectl run load-test --image=busybox --rm -it --restart=Never -- /bin/sh
# Inside the pod:
# while true; do wget -q -O- http://gateway-service:8080/actuator/health; sleep 0.1; done
```

## Maintenance

### Backup
- **Redis backup**: Use BGSAVE for Redis snapshots
- **Configuration backup**: Store manifests in version control
- **Route configuration**: Export current routes for backup

### Updates
- **Rolling updates**: Use deployment rolling update strategy
- **Route updates**: Update ConfigMaps and refresh routes
- **Configuration updates**: Update ConfigMaps and restart pods
- **Zero-downtime**: Ensure minimum replicas during updates

### Monitoring
- **Resource usage**: Monitor CPU, memory, and network usage
- **Performance metrics**: Track response times, throughput, and error rates
- **Rate limiting**: Monitor rate limit effectiveness and adjust as needed
- **Circuit breaker**: Monitor circuit breaker state and failure patterns

### Scaling
- **Horizontal scaling**: HPA automatically scales based on metrics
- **Redis scaling**: Consider Redis Cluster for high availability
- **Load balancing**: Ensure proper load distribution
- **Performance tuning**: Optimize JVM settings for reactive workloads