# Bookstore Service Helm Chart

Production-ready Helm chart for deploying the Gripday Bookstore Service with PostgreSQL and Redis on Kubernetes.

## Overview

The Bookstore Service is a Spring Boot microservice that provides book catalog management, inventory tracking, and search capabilities. This chart includes:

- **Bookstore Service**: Spring Boot application with JWT authentication
- **PostgreSQL 15**: Dedicated database with Liquibase migrations
- **Redis 7**: Caching layer for improved performance
- **Observability**: Prometheus metrics, distributed tracing, structured logging
- **Security**: Network policies, pod security contexts, RBAC

## Prerequisites

- Kubernetes 1.24+
- Helm 3.8+
- PV provisioner for persistent storage
- Nginx Ingress Controller (if ingress enabled)
- User Service deployed (for JWT validation)

## Quick Start

### Development Environment

```bash
# Install with default values (optimized for k3s single instance)
helm install bookstore-service ./helm/bookstore-service \
  --namespace gripday-dev-env \
  --create-namespace

# Verify deployment
kubectl get pods -n gripday-dev-env
kubectl get svc -n gripday-dev-env
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-bookstore-service
```

**Important Notes**:
- Bookstore service is **internal-only** (no public ingress)
- Access via Gateway Service at `http://api.gripday.site/api/v1/bookstore`
- Service exposed on port 80, container runs on port 8080
- Default configuration optimized for k3s single node (1 replica, HPA disabled)

### Staging Environment

```bash
# Create secrets first (replace with actual values)
kubectl create secret generic bookstore-service-secrets \
  --from-literal=GRIPDAY_DATABASE_USERNAME=bookstore_user \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=secure_password \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=jwt_secret_key \
  --from-literal=GRIPDAY_CACHE_REDIS_PASSWORD=redis_password \
  -n gripday-staging-env

# Install with staging configuration
helm install bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-staging.yaml \
  --namespace gripday-staging-env \
  --create-namespace
```

### Production Environment

```bash
# Secrets should be managed via external secret management (AWS Secrets Manager, Vault, etc.)

# Install with production configuration
helm install bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-production.yaml \
  --namespace gripday-production-env \
  --create-namespace
```

## Upgrading

```bash
# Upgrade with new values
helm upgrade bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-production.yaml \
  --namespace gripday-production-env

# Rollback if needed
helm rollback bookstore-service --namespace gripday-production-env
```

## Uninstalling

```bash
helm uninstall bookstore-service --namespace gripday-dev-env
```

## Configuration

The following table lists the configurable parameters of the Bookstore Service chart and their default values.

### Global Parameters

| Parameter            | Description      | Default   |
| -------------------- | ---------------- | --------- |
| `global.environment` | Environment name | `dev`     |
| `global.platform`    | Platform name    | `gripday` |

### Application Parameters

| Parameter          | Description        | Default                     |
| ------------------ | ------------------ | --------------------------- |
| `replicaCount`     | Number of replicas | `1` (k3s optimized)         |
| `image.repository` | Image repository   | `gripday/bookstore-service` |
| `image.tag`        | Image tag          | `1.0.0`                     |
| `image.pullPolicy` | Image pull policy  | `IfNotPresent`              |

### Service Parameters

| Parameter                  | Description                  | Default     |
| -------------------------- | ---------------------------- | ----------- |
| `service.type`             | Service type                 | `ClusterIP` |
| `service.port`             | Service port (external)      | `80`        |
| `service.targetPort`       | Container port (internal)    | `8080`      |
| `service.headless.enabled` | Create headless service      | `true`      |

### Access Configuration

**Ingress**: Disabled (internal-only service)
- Bookstore service has no public ingress
- All traffic routes through Gateway Service
- Access URL: `http://api.gripday.site/api/v1/bookstore`

**Service Discovery**:
- Internal URL: `http://bookstore-service` (port 80)
- Gateway uses simple service name for routing
- No namespace qualification needed (same namespace deployment)

### Resources

| Parameter                   | Description    | Default |
| --------------------------- | -------------- | ------- |
| `resources.requests.memory` | Memory request | `384Mi` |
| `resources.requests.cpu`    | CPU request    | `250m`  |
| `resources.limits.memory`   | Memory limit   | `768Mi` |
| `resources.limits.cpu`      | CPU limit      | `500m`  |

### Autoscaling

| Parameter                                       | Description                | Default                    |
| ----------------------------------------------- | -------------------------- | -------------------------- |
| `autoscaling.enabled`                           | Enable HPA                 | `false` (disabled for k3s) |
| `autoscaling.minReplicas`                       | Minimum replicas           | `1`                        |
| `autoscaling.maxReplicas`                       | Maximum replicas           | `3`                        |
| `autoscaling.targetCPUUtilizationPercentage`    | Target CPU                 | `70`                       |
| `autoscaling.targetMemoryUtilizationPercentage` | Target memory              | `80`                       |

### PostgreSQL Parameters

| Parameter                              | Description          | Default                  |
| -------------------------------------- | -------------------- | ------------------------ |
| `postgresql.enabled`                   | Enable PostgreSQL    | `true`                   |
| `postgresql.image.tag`                 | PostgreSQL image tag | `15.8-alpine`            |
| `postgresql.persistence.size`          | PVC size             | `10Gi`                   |
| `postgresql.persistence.storageClass`  | Storage class        | `local-path` (k3s)       |
| `postgresql.resources.requests.memory` | Memory request       | `512Mi`                  |
| `postgresql.resources.requests.cpu`    | CPU request          | `500m`                   |

### Redis Parameters

| Parameter                        | Description       | Default            |
| -------------------------------- | ----------------- | ------------------ |
| `redis.enabled`                  | Enable Redis      | `true`             |
| `redis.image.tag`                | Redis image tag   | `7.2-alpine`       |
| `redis.persistence.size`         | PVC size          | `2Gi`              |
| `redis.persistence.storageClass` | Storage class     | `local-path` (k3s) |
| `redis.config.maxmemory`         | Max memory        | `512mb`            |

### Security Parameters

| Parameter                      | Description           | Default                |
| ------------------------------ | --------------------- | ---------------------- |
| `networkPolicy.enabled`        | Enable network policy | `true`                 |
| `podSecurityContext.runAsUser` | Run as user ID        | `1001`                 |
| `priorityClassName`            | Priority class name   | `""` (empty for k3s)   |
| `ingress.enabled`              | Enable ingress        | `false` (internal-only)|

## Examples

### Install with custom values

```bash
helm install bookstore-service ./bookstore-service -f custom-values.yaml
```

### Upgrade with new values

```bash
helm upgrade bookstore-service ./bookstore-service --set replicaCount=3
```

### Install without PostgreSQL (use external database)

```bash
helm install bookstore-service ./bookstore-service --set postgresql.enabled=false
```

### Disable autoscaling

```bash
helm install bookstore-service ./bookstore-service --set autoscaling.enabled=false
```

## Environment-Specific Configuration

### Local Development (`values.yaml`)

- 2 replicas, autoscaling enabled (2-10 pods)
- Embedded secrets (base64 encoded defaults)
- Permissive CORS for local development
- Human-readable console logging
- Full tracing (100% sampling)
- 10Gi PostgreSQL, 2Gi Redis storage

### Staging (`values-staging.yaml`)

- 3 replicas, autoscaling enabled (3-15 pods)
- External secret management required
- Staging domain with TLS
- JSON structured logging
- Full tracing (100% sampling)
- 20Gi PostgreSQL, 5Gi Redis storage
- Medium priority class

### Production (`values-production.yaml`)

- 5 replicas, autoscaling enabled (5-20 pods)
- External secret management required (AWS Secrets Manager, Vault)
- Production domain with TLS
- JSON structured logging
- Reduced tracing (10% sampling)
- 50Gi PostgreSQL, 10Gi Redis storage
- High priority class
- Strict pod anti-affinity
- Node affinity for compute-optimized instances

## Architecture

### Service Dependencies

```
┌─────────────────┐
│  User Service   │ ← JWT issuer (JWK endpoint)
└────────┬────────┘
         │
         ↓ JWT validation
┌─────────────────┐     ┌──────────────┐
│ Gateway Service │────→│   Bookstore  │
└─────────────────┘     │   Service    │
                        └──────┬───────┘
                               │
                    ┌──────────┴──────────┐
                    ↓                     ↓
              ┌──────────┐         ┌──────────┐
              │PostgreSQL│         │  Redis   │
              └──────────┘         └──────────┘
```

### Key Features

- **JWT Authentication**: Validates tokens from User Service via JWK endpoint
- **Database Per Service**: Dedicated PostgreSQL instance with Liquibase migrations
- **Redis Caching**: Improves read performance for catalog queries
- **Horizontal Scaling**: HPA based on CPU/memory with intelligent scaling policies
- **Network Isolation**: Network policies restrict traffic to authorized services
- **Observability**: Prometheus metrics, OpenTelemetry tracing, structured logs

## Configuration

### Required Secrets

Create a Kubernetes secret with these keys:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: bookstore-service-secrets
  namespace: gripday-dev-env
type: Opaque
stringData:
  GRIPDAY_DATABASE_USERNAME: bookstore_user
  GRIPDAY_DATABASE_PASSWORD: secure_db_password
  GRIPDAY_AUTH_JWT_SECRET: jwt_secret_key
  GRIPDAY_CACHE_REDIS_PASSWORD: redis_password # optional
```

### JWT Configuration

The service validates JWT tokens from User Service:

```yaml
env:
  - name: JWT_ISSUER_URI
    value: "http://user-service"
  - name: JWT_JWK_SET_URI
    value: "http://user-service/.well-known/jwks.json"
```

### CORS Configuration

Configure allowed origins for frontend applications:

```yaml
env:
  - name: CORS_ALLOWED_ORIGINS
    value: "https://gripday.site,https://*.gripday.site"
```

## Monitoring

### Health Checks

```bash
# Liveness probe
curl http://bookstore-service/actuator/health/liveness

# Readiness probe
curl http://bookstore-service/actuator/health/readiness

# Full health details
curl http://bookstore-service/actuator/health
```

### Metrics

```bash
# Prometheus metrics endpoint
curl http://bookstore-service/actuator/prometheus

# View metrics in Grafana
# Dashboard: Gripday Bookstore Service Overview
```

### API Documentation

```bash
# Swagger UI (via Gateway)
http://api.gripday.site/swagger-ui.html

# OpenAPI JSON (via Gateway)
http://api.gripday.site/api-docs

# Internal access (from within cluster)
curl http://bookstore-service/swagger-ui.html
curl http://bookstore-service/api-docs
```

## Troubleshooting

### Check Deployment Status

```bash
# Pod status
kubectl get pods -n gripday-dev-env -l app.kubernetes.io/name=gripday-bookstore-service

# Deployment status
kubectl rollout status deployment/bookstore-service -n gripday-dev-env

# HPA status (if enabled)
kubectl get hpa -n gripday-dev-env
```

### View Logs

```bash
# Application logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-bookstore-service

# PostgreSQL logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=bookstore-postgres

# Redis logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=bookstore-redis

# Previous container logs (if crashed)
kubectl logs -n gripday-dev-env <pod-name> --previous
```

### Debug Issues

```bash
# Describe pod for events
kubectl describe pod <pod-name> -n gripday-dev-env

# Check service endpoints
kubectl get endpoints -n gripday-dev-env

# Test database connectivity
kubectl exec -it <bookstore-pod> -n gripday-dev-env -- \
  psql -h bookstore-postgres -U gripday_user -d gripday_bookstore_local

# Test Redis connectivity
kubectl exec -it <bookstore-pod> -n gripday-dev-env -- \
  redis-cli -h bookstore-redis ping

# Check network policies
kubectl get networkpolicies -n gripday-dev-env
kubectl describe networkpolicy bookstore-service-netpol -n gripday-dev-env
```

### Common Issues

**Pods not starting**

- Check resource quotas: `kubectl describe resourcequota -n gripday-bookstore`
- Verify secrets exist: `kubectl get secrets -n gripday-bookstore`
- Check image pull: `kubectl describe pod <pod-name> -n gripday-bookstore`

**Database connection failures**

- Verify PostgreSQL is running: `kubectl get pods -n gripday-bookstore -l app.kubernetes.io/name=bookstore-postgres`
- Check database credentials in secrets
- Verify network policy allows traffic

**JWT validation failures**

- Ensure User Service is accessible: `curl http://user-service:8080/.well-known/jwks.json`
- Check JWT_ISSUER_URI and JWT_JWK_SET_URI environment variables
- Verify network policy allows egress to User Service

## Performance Tuning

### Resource Optimization

Adjust resources based on load:

```yaml
resources:
  requests:
    memory: "768Mi" # Increase for high traffic
    cpu: "500m"
  limits:
    memory: "1536Mi"
    cpu: "1000m"
```

### Autoscaling Tuning

```yaml
autoscaling:
  minReplicas: 5
  maxReplicas: 20
  targetCPUUtilizationPercentage: 60 # Lower = more aggressive scaling
  targetMemoryUtilizationPercentage: 70
```

### Database Optimization

```yaml
postgresql:
  resources:
    requests:
      memory: "1Gi"
      cpu: "1000m"
  persistence:
    size: 50Gi
    storageClass: fast-ssd # Use SSD for better performance
```

### Redis Optimization

```yaml
redis:
  config:
    maxmemory: "1024mb" # Increase for more caching
    maxmemoryPolicy: "allkeys-lru"
  persistence:
    size: 10Gi
```

## Security

### Network Policies

The chart includes network policies that:

- Allow ingress from Gateway Service only
- Allow egress to PostgreSQL and Redis
- Allow egress to observability services
- Block all other traffic

### Pod Security

- Runs as non-root user (UID 1001)
- Read-only root filesystem
- Drops all capabilities
- Seccomp profile enabled

### Secrets Management

**Local/Staging**: Use Kubernetes secrets
**Production**: Use external secret management:

- AWS Secrets Manager with External Secrets Operator
- HashiCorp Vault
- Azure Key Vault

## Support

For issues and questions:

- GitHub Issues: https://github.com/gripday/bookstore-service
- Platform Team: platform@gripday.site
- Documentation: https://docs.gripday.site
