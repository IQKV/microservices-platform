# Gateway Service Helm Chart

Production-ready Helm chart for deploying the Gripday Gateway Service with Redis on Kubernetes.

## Overview

The Gateway Service is the **single public entry point** for the Gripday microservices platform. Built on Spring Cloud Gateway, it provides intelligent routing, JWT authentication, Redis-backed rate limiting, circuit breaker patterns, and request transformation.

**Architecture**: Gateway is the only service with public ingress. All other services (User, Bookstore) are internal-only with ClusterIP services.

## Features

- **Intelligent Routing**: Dynamic request routing to downstream services (User Service, Bookstore Service)
- **JWT Authentication**: Validates tokens from User Service via JWK endpoint
- **Rate Limiting**: Redis-backed distributed rate limiting with endpoint-specific policies
- **Circuit Breaker**: Resilience4j fault tolerance with automatic failure detection
- **CORS Handling**: Environment-specific CORS policies
- **Request Transformation**: Header enrichment, correlation ID generation, user context propagation
- **Observability**: Prometheus metrics, OpenTelemetry tracing, structured logging

## Prerequisites

- Kubernetes 1.24+
- Helm 3.8+
- PV provisioner for Redis persistent storage (local-path for k3s)
- Nginx Ingress Controller (if ingress enabled)
- User Service deployed (for JWT validation)
- Bookstore Service deployed (for routing)

## Quick Start

### Local Development (k3s)

```bash
# Install with default values (dev environment)
helm install gateway-service ./helm/gateway-service \
  --namespace gripday-dev-env \
  --create-namespace

# Verify deployment
kubectl get pods -n gripday-dev-env
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-gateway-service

# Check ingress
kubectl get ingress -n gripday-dev-env
```

**Note**: Default values are optimized for k3s single-node development with 1 replica, HPA disabled, and local-path storage.

### Test Environment

```bash
# Create secrets first
kubectl create secret generic gateway-service-secrets \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=jwt_secret_key \
  --from-literal=GRIPDAY_CACHE_REDIS_PASSWORD=redis_password \
  -n gripday-test-env

# Install with test configuration
helm install gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-test.yaml \
  --namespace gripday-test-env \
  --create-namespace
```

### Staging Environment

```bash
# Create secrets first
kubectl create secret generic gateway-service-secrets \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=jwt_secret_key \
  --from-literal=GRIPDAY_CACHE_REDIS_PASSWORD=redis_password \
  -n gripday-staging-env

# Install with staging configuration
helm install gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-staging.yaml \
  --namespace gripday-staging-env \
  --create-namespace
```

### Production Environment

```bash
# Create secrets externally (AWS Secrets Manager, Vault, etc.)
# Then install with production configuration
helm install gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-production.yaml \
  --namespace gripday-production-env \
  --create-namespace
```

## Environments and Domains

| Environment | Domain              | Namespace              | Values File              |
| ----------- | ------------------- | ---------------------- | ------------------------ |
| Development | api.gripday.site    | gripday-dev-env        | values.yaml              |
| Test        | api.gripday.website | gripday-test-env       | values-test.yaml         |
| Staging     | api.gripday.space   | gripday-staging-env    | values-staging.yaml      |
| Production  | api.gripday.com     | gripday-production-env | values-production.yaml   |

## Configuration

### Key Parameters

| Parameter                 | Description                | Default                   |
| ------------------------- | -------------------------- | ------------------------- |
| `replicaCount`            | Number of gateway replicas | `1`                       |
| `image.repository`        | Image repository           | `gripday/gateway-service` |
| `image.tag`               | Image tag                  | `1.0.0`                   |
| `service.port`            | Service port               | `80`                      |
| `service.targetPort`      | Container port             | `8080`                    |
| `autoscaling.enabled`     | Enable HPA                 | `false`                   |
| `autoscaling.minReplicas` | Minimum replicas           | `1`                       |
| `autoscaling.maxReplicas` | Maximum replicas           | `3`                       |
| `redis.enabled`           | Enable Redis               | `true`                    |
| `redis.persistence.size`  | Redis storage size         | `2Gi`                     |
| `priorityClassName`       | Pod priority class         | `""`                      |

### Environment-Specific Configuration

**Local Development** (`values.yaml`)

- 1 replica (k3s optimized)
- HPA disabled
- PDB disabled
- No priority class
- Embedded secrets for development
- Permissive CORS for local development (localhost:3000, localhost:5173, localhost:8080)
- Human-readable console logging
- Full tracing (100% sampling)
- 2Gi Redis storage with local-path storage class
- Domain: api.gripday.site
- TLS disabled by default
- No affinity/topology constraints

**Test** (`values-test.yaml`)

- 3 replicas, autoscaling 3-15 pods (CPU 70%, Memory 80%)
- PDB enabled (minAvailable: 2)
- High priority class
- External secret management required
- Test domain: api.gripday.website
- JSON structured logging
- Full tracing (100% sampling)
- 5Gi Redis storage with standard storage class
- TLS disabled by default

**Staging** (`values-staging.yaml`)

- 5 replicas, autoscaling 5-20 pods (CPU 65%, Memory 75%)
- PDB enabled (minAvailable: 3)
- High priority class
- External secret management required
- Staging domain: api.gripday.space
- JSON structured logging
- Full tracing (100% sampling)
- 5Gi Redis storage with fast-ssd storage class
- TLS disabled by default

**Production** (`values-production.yaml`)

- 10 replicas, autoscaling 10-30 pods (CPU 60%, Memory 70%)
- PDB enabled (minAvailable: 7)
- Critical priority class
- External secret management required (AWS Secrets Manager, Vault)
- Production domain: api.gripday.com
- JSON structured logging
- Reduced tracing (10% sampling)
- 10Gi Redis storage with fast-ssd storage class
- Strict pod anti-affinity (requiredDuringScheduling)
- Multi-zone distribution with topology spread constraints
- TLS disabled by default
- Enhanced security headers and OWASP ModSecurity rules
- Preferred node types: c5.2xlarge, c5.4xlarge

## Architecture

### Service Dependencies

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│      Gateway Service (Ingress)      │
│  - JWT Validation                   │
│  - Rate Limiting                    │
│  - Circuit Breaker                  │
│  - Request Transformation           │
└──────┬──────────────────────────────┘
       │
       ├──────────────┬─────────────────┐
       ↓              ↓                 ↓
┌──────────────┐ ┌──────────────┐ ┌──────────┐
│ User Service │ │   Bookstore  │ │  Redis   │
│              │ │   Service    │ │ (Cache & │
│ (JWT Issuer) │ │              │ │  Rate    │
│              │ │              │ │ Limiting)│
└──────────────┘ └──────────────┘ └──────────┘
```

### Gateway Routes

- `/api/v1/auth/**` → User Service (authentication endpoints, 60 req/min, burst 100)
- `/api/v1/users/me` → User Service (user profile, 100 req/min, burst 150)
- `/api/v1/admin/**` → User Service (admin endpoints, 100 req/min, burst 150)
- `/api/v1/bookstore/**` → Bookstore Service (catalog and inventory, 100 req/min, burst 200)
- `/.well-known/jwks.json` → User Service (JWK Set for JWT validation, no rate limit)
- `/actuator/**` → Gateway actuator endpoints (no rate limit)

### Public Endpoints (No Authentication Required)

- `/.well-known/jwks.json`
- `/api/v1/auth/login`, `/api/v1/auth/signup`, `/api/v1/auth/refresh`
- `/api/v1/auth/validate`, `/api/v1/auth/health`
- `/api/v1/auth/email/**`, `/api/v1/auth/password/**`
- `/api/v1/bookstore/version`, `/api/v1/bookstore/books/**`
- `/api/v1/bookstore/inventory/**`
- `/actuator/health`, `/actuator/info`
- `/swagger-ui/**`, `/api-docs/**`

## Observability Configuration

### Tracing

- OpenTelemetry integration
- OTLP endpoint: http://otel-collector:4317 (or http://otel-collector.observability:4317 in staging/production)
- Jaeger endpoint: http://jaeger-collector:14268/api/traces (or .observability namespace)
- Sampling rates:
  - Development/Test/Staging: 100%
  - Production: 10%

### Metrics

- Prometheus metrics exposed at `/actuator/prometheus`
- Annotations for automatic scraping
- Custom metrics prefix: `gripday_gateway`
- Distribution percentiles: 0.5, 0.95, 0.99

### Logging

- Development: Human-readable console format
- Test/Staging/Production: JSON structured logging
- Correlation ID propagation via X-Correlation-ID header
- Log levels:
  - Development: INFO
  - Test/Staging: INFO
  - Production: WARN (AUDIT: INFO)

## Monitoring

### Health Checks

```bash
# Liveness probe
curl http://gateway-service:8080/actuator/health/liveness

# Readiness probe
curl http://gateway-service:8080/actuator/health/readiness

# Full health details
curl http://gateway-service:8080/actuator/health
```

### Gateway Routes

```bash
# View configured routes
curl http://gateway-service:8080/actuator/gateway/routes | jq

# Refresh routes
curl -X POST http://gateway-service:8080/actuator/gateway/refresh
```

### Metrics

```bash
# Prometheus metrics
curl http://gateway-service:8080/actuator/prometheus

# Gateway-specific metrics
curl http://gateway-service:8080/actuator/prometheus | grep gateway
```

## Troubleshooting

### Check Deployment Status

```bash
# Pod status (adjust namespace as needed)
kubectl get pods -n gripday-dev-env -l app.kubernetes.io/name=gripday-gateway-service

# Deployment status
kubectl rollout status deployment/gateway-service -n gripday-dev-env

# HPA status (if enabled)
kubectl get hpa -n gripday-dev-env
```

### View Logs

```bash
# Application logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-gateway-service

# Redis logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gateway-redis

# Previous container logs (if crashed)
kubectl logs -n gripday-dev-env <pod-name> --previous
```

### Test Routing

```bash
# Port forward
kubectl port-forward -n gripday-dev-env svc/gateway-service 8080:80

# Test User Service route
curl http://localhost:8080/api/v1/auth/health

# Test Bookstore Service route
curl http://localhost:8080/api/v1/bookstore/books

# Test with JWT
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users/me
```

### Test Rate Limiting

```bash
# Trigger rate limit on auth endpoint
for i in {1..70}; do
  curl -X POST http://localhost:8080/api/v1/auth/login
done

# Check rate limit headers
curl -I http://localhost:8080/api/v1/auth/login
```

### Debug Redis

```bash
# Connect to Redis
kubectl exec -it -n gripday-dev-env \
  $(kubectl get pod -n gripday-dev-env -l app.kubernetes.io/name=gateway-redis -o jsonpath='{.items[0].metadata.name}') \
  -- redis-cli

# Check rate limiting keys
KEYS "gripday:gateway:rate-limit:*"

# Check key TTL
TTL "gripday:gateway:rate-limit:user:123"
```

### Common Issues

**Routing failures**

- Verify downstream services are running
- Check service DNS resolution
- Verify network policies allow traffic

**JWT validation failures**

- Ensure User Service is accessible
- Check JWK endpoint: `curl http://user-service:8080/.well-known/jwks.json`
- Verify JWT_JWK_URI environment variable

**Rate limiting not working**

- Check Redis is running and accessible
- Verify Redis connection in gateway logs
- Check rate limit keys in Redis

**Circuit breaker open**

- Check downstream service health
- Review failure rate threshold configuration
- Check circuit breaker metrics

## Upgrading

```bash
# Upgrade with new values (adjust namespace and values file as needed)
helm upgrade gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-production.yaml \
  --namespace gripday-production-env

# Rollback if needed
helm rollback gateway-service --namespace gripday-production-env
```

## Uninstalling

```bash
# Adjust namespace as needed
helm uninstall gateway-service --namespace gripday-dev-env
```

## Security

### Network Policies

The chart includes network policies that:

- Allow ingress from Nginx Ingress Controller
- Allow egress to User Service and Bookstore Service
- Allow egress to Redis
- Allow egress to observability services
- Block all other traffic

### Pod Security

- Runs as non-root user (UID 1001)
- Read-only root filesystem
- Drops all capabilities
- Seccomp profile enabled

### Secrets Management

**Local Development**: Embedded base64-encoded secrets in values.yaml (not for production)
**Test/Staging**: Use Kubernetes secrets created manually
**Production**: Use external secret management (AWS Secrets Manager, Vault, Azure Key Vault)

Required secrets:
- `GRIPDAY_AUTH_JWT_SECRET`: JWT signing secret
- `GRIPDAY_CACHE_REDIS_PASSWORD`: Redis password (optional in dev)

## Java Configuration

### JVM Options by Environment

**Development**:
```
-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

**Test/Staging**:
```
-Xms768m -Xmx1536m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof
```

**Production**:
```
-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof 
-XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled
```

## Resource Requirements

### Gateway Service

| Environment | Replicas | CPU Request | Memory Request | CPU Limit | Memory Limit |
| ----------- | -------- | ----------- | -------------- | --------- | ------------ |
| Development | 1        | 500m        | 768Mi          | 1000m     | 1536Mi       |
| Test        | 3        | 750m        | 1Gi            | 1500m     | 2Gi          |
| Staging     | 5        | 750m        | 1Gi            | 1500m     | 2Gi          |
| Production  | 10       | 1000m       | 1536Mi         | 2000m     | 3Gi          |

### Redis

| Environment | CPU Request | Memory Request | CPU Limit | Memory Limit | Storage | Max Memory | Storage Class |
| ----------- | ----------- | -------------- | --------- | ------------ | ------- | ---------- | ------------- |
| Development | 200m        | 256Mi          | 400m      | 512Mi        | 2Gi     | 512mb      | local-path    |
| Test        | 300m        | 384Mi          | 600m      | 768Mi        | 5Gi     | 768mb      | standard      |
| Staging     | 300m        | 384Mi          | 600m      | 768Mi        | 5Gi     | 768mb      | fast-ssd      |
| Production  | 500m        | 768Mi          | 1000m     | 1536Mi       | 10Gi    | 1536mb     | fast-ssd      |

Redis configuration:
- Image: redis:7.2-alpine
- Max memory policy: allkeys-lru
- Append-only file: enabled
- Production: RDB snapshots enabled (900 1 300 10 60 10000)

## Included Resources

The Helm chart creates the following Kubernetes resources:

- Namespace (with pod security standards)
- ServiceAccount (with automountServiceAccountToken: false)
- ConfigMap (application.yml configuration)
- Secret (JWT and Redis credentials)
- Deployment (gateway service pods)
- Service (ClusterIP with headless option)
- Ingress (Nginx with CORS and security headers)
- HorizontalPodAutoscaler (optional, disabled by default)
- PodDisruptionBudget (optional, disabled by default)
- NetworkPolicy (ingress/egress rules)
- ResourceQuota (namespace resource limits)
- LimitRange (container resource constraints)
- Redis Deployment (cache and rate limiting)
- Redis Service (ClusterIP)
- Redis NetworkPolicy (access control)

## Support

- GitHub Issues: https://github.com/gripday/gateway-service
- Platform Team: platform@gripday.site
- Documentation: https://docs.gripday.site
