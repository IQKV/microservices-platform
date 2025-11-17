# Gateway Service Helm Chart

Production-ready Helm chart for deploying the Gripday Gateway Service with Redis on Kubernetes.

## Overview

The Gateway Service is the **single public entry point** for the Gripday microservices platform. Built on Spring Cloud Gateway, it provides intelligent routing, JWT authentication, Redis-backed rate limiting, circuit breaker patterns, and request transformation. All client traffic flows through the gateway at `api.gripday.site`.

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
- PV provisioner for Redis persistent storage
- Nginx Ingress Controller (if ingress enabled)
- User Service deployed (for JWT validation)
- Bookstore Service deployed (for routing)

## Quick Start

### Local Development

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

**Note**: Gateway is the only service with public ingress at `api.gripday.site`

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
# Install with production configuration
helm install gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-production.yaml \
  --namespace gripday-production-env \
  --create-namespace
```

## Configuration

### Key Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of gateway replicas | `3` |
| `image.repository` | Image repository | `gripday/gateway-service` |
| `image.tag` | Image tag | `1.0.0` |
| `service.port` | Service port | `8080` |
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `3` |
| `autoscaling.maxReplicas` | Maximum replicas | `15` |
| `redis.enabled` | Enable Redis | `true` |
| `redis.persistence.size` | Redis storage size | `2Gi` |
| `priorityClassName` | Pod priority class | `high-priority` |

### Environment-Specific Configuration

**Local Development** (`values.yaml`)
- 3 replicas, autoscaling 3-15 pods
- Embedded secrets for development
- Permissive CORS for local development
- Human-readable console logging
- Full tracing (100% sampling)
- 2Gi Redis storage

**Staging** (`values-staging.yaml`)
- 5 replicas, autoscaling 5-20 pods
- External secret management required
- Staging domain with TLS
- JSON structured logging
- Full tracing (100% sampling)
- 5Gi Redis storage
- High priority class

**Production** (`values-production.yaml`)
- 10 replicas, autoscaling 10-30 pods
- External secret management required
- Production domain with TLS
- JSON structured logging
- Reduced tracing (10% sampling)
- 10Gi Redis storage
- Critical priority class
- Strict pod anti-affinity
- Multi-zone distribution

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

- `/api/v1/auth/**` → User Service (authentication endpoints)
- `/api/v1/users/me` → User Service (user profile)
- `/api/v1/admin/**` → User Service (admin endpoints)
- `/api/v1/bookstore/**` → Bookstore Service (catalog and inventory)
- `/.well-known/jwks.json` → User Service (JWK Set for JWT validation)
- `/actuator/**` → Gateway actuator endpoints

### Rate Limiting Policies

- Login: 10 requests/minute, burst 20
- Signup: 5 requests/minute, burst 10
- Books: 100 requests/minute, burst 200
- Default: 60 requests/minute, burst 100

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
# Pod status
kubectl get pods -n gripday-gateway -l app.kubernetes.io/name=gripday-gateway-service

# Deployment status
kubectl rollout status deployment/gateway-service -n gripday-gateway

# HPA status
kubectl get hpa -n gripday-gateway
```

### View Logs

```bash
# Application logs
kubectl logs -f -n gripday-gateway -l app.kubernetes.io/name=gripday-gateway-service

# Redis logs
kubectl logs -f -n gripday-gateway -l app.kubernetes.io/name=gateway-redis

# Previous container logs (if crashed)
kubectl logs -n gripday-gateway <pod-name> --previous
```

### Test Routing

```bash
# Port forward
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080

# Test User Service route
curl http://localhost:8080/api/v1/auth/health

# Test Bookstore Service route
curl http://localhost:8080/api/v1/bookstore/books

# Test with JWT
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users/me
```

### Test Rate Limiting

```bash
# Trigger rate limit
for i in {1..100}; do 
  curl -X POST http://localhost:8080/api/v1/auth/login
done

# Check rate limit headers
curl -I http://localhost:8080/api/v1/auth/login
# X-RateLimit-Limit: 10
# X-RateLimit-Remaining: 9
# X-RateLimit-Reset: 1634568790
```

### Debug Redis

```bash
# Connect to Redis
kubectl exec -it -n gripday-gateway \
  $(kubectl get pod -n gripday-gateway -l app.kubernetes.io/name=gateway-redis -o jsonpath='{.items[0].metadata.name}') \
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
# Upgrade with new values
helm upgrade gateway-service ./helm/gateway-service \
  -f ./helm/gateway-service/values-production.yaml \
  --namespace gripday-gateway-production

# Rollback if needed
helm rollback gateway-service --namespace gripday-gateway-production
```

## Uninstalling

```bash
helm uninstall gateway-service --namespace gripday-gateway
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

**Local/Staging**: Use Kubernetes secrets
**Production**: Use external secret management (AWS Secrets Manager, Vault, Azure Key Vault)

## Support

- GitHub Issues: https://github.com/gripday/gateway-service
- Platform Team: platform@gripday.site
- Documentation: https://docs.gripday.site
