# User Service Helm Chart

Production-ready Helm chart for deploying the Gripday User Service with PostgreSQL and Redis on Kubernetes.

## Overview

The User Service is the centralized authentication and user management microservice for the Gripday platform. It provides JWT-based authentication, user lifecycle management, email verification, password reset flows, and role-based access control.

## Features

- **JWT Authentication**: Access tokens (15min) and refresh tokens (7 days) with RSA256 signing
- **User Management**: Registration, login, profile management, and admin operations
- **Email Verification**: Token-based email verification with rate limiting
- **Password Management**: Secure password reset flow with time-limited tokens
- **Account Security**: Lockout protection, rate limiting, audit logging
- **Multi-Tenancy**: Tenant-scoped data isolation
- **JWK Endpoint**: Public key distribution for downstream JWT validation
- **Observability**: Prometheus metrics, OpenTelemetry tracing, structured logging

## Prerequisites

- Kubernetes 1.24+
- Helm 3.8+
- PV provisioner for persistent storage
- Nginx Ingress Controller (if ingress enabled)
- SMTP server for email functionality (production)

## Quick Start

### Local Development

```bash
# Install with default values
helm install user-service ./helm/user-service \
  --namespace gripday-user \
  --create-namespace

# Verify deployment
kubectl get pods -n gripday-user
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=gripday-user-service
```

### Staging Environment

```bash
# Create secrets first
kubectl create secret generic user-service-secrets \
  --from-literal=GRIPDAY_DATABASE_USERNAME=user_db_user \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=secure_password \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=jwt_secret_key \
  --from-literal=SMTP_USERNAME=smtp_user \
  --from-literal=SMTP_PASSWORD=smtp_password \
  -n gripday-user-staging

# Install with staging configuration
helm install user-service ./helm/user-service \
  -f ./helm/user-service/values-staging.yaml \
  --namespace gripday-user-staging \
  --create-namespace
```

### Production Environment

```bash
# Install with production configuration
helm install user-service ./helm/user-service \
  -f ./helm/user-service/values-production.yaml \
  --namespace gripday-user-production \
  --create-namespace
```

## Configuration

### Key Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of replicas | `2` |
| `image.repository` | Image repository | `gripday/user-service` |
| `image.tag` | Image tag | `1.0.0` |
| `service.port` | Service port | `8080` |
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `2` |
| `autoscaling.maxReplicas` | Maximum replicas | `10` |
| `postgresql.enabled` | Enable PostgreSQL | `true` |
| `postgresql.persistence.size` | PostgreSQL storage | `5Gi` |
| `redis.enabled` | Enable Redis | `true` |
| `redis.persistence.size` | Redis storage | `1Gi` |
| `priorityClassName` | Pod priority class | `high-priority` |

### Environment-Specific Configuration

**Local Development** (`values.yaml`)
- 2 replicas, autoscaling 2-10 pods
- Embedded secrets for development
- 5Gi PostgreSQL, 1Gi Redis storage
- Human-readable console logging
- Full tracing (100% sampling)
- High priority class

**Staging** (`values-staging.yaml`)
- 3 replicas, autoscaling 3-15 pods
- External secret management required
- 10Gi PostgreSQL, 2Gi Redis storage
- JSON structured logging
- Full tracing (100% sampling)
- Staging domain with TLS
- High priority class

**Production** (`values-production.yaml`)
- 5 replicas, autoscaling 5-20 pods
- External secret management required
- 20Gi PostgreSQL, 5Gi Redis storage
- JSON structured logging
- Reduced tracing (10% sampling)
- Production domain with TLS
- Critical priority class
- Strict pod anti-affinity
- Multi-zone distribution

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- PV provisioner support in the underlying infrastructure
- Ingress controller (nginx) if ingress is enabled

## Installing the Chart

To install the chart with the release name `user-service`:

```bash
helm install user-service ./user-service
```

To install in a specific namespace:

```bash
helm install user-service ./user-service --namespace gripday-user --create-namespace
```

## Uninstalling the Chart

To uninstall/delete the `user-service` deployment:

```bash
helm uninstall user-service --namespace gripday-user
```

## Configuration

The following table lists the configurable parameters of the User Service chart and their default values.

### Global Parameters

| Parameter            | Description      | Default            |
| -------------------- | ---------------- | ------------------ |
| `global.environment` | Environment name | `local`            |
| `global.platform`    | Platform name    | `gripday` |

### Application Parameters

| Parameter          | Description        | Default                |
| ------------------ | ------------------ | ---------------------- |
| `replicaCount`     | Number of replicas | `2`                    |
| `image.repository` | Image repository   | `gripday/user-service` |
| `image.tag`        | Image tag          | `1.0.0`                |
| `image.pullPolicy` | Image pull policy  | `IfNotPresent`         |

### Service Parameters

| Parameter                  | Description             | Default     |
| -------------------------- | ----------------------- | ----------- |
| `service.type`             | Service type            | `ClusterIP` |
| `service.port`             | Service port            | `8080`      |
| `service.headless.enabled` | Create headless service | `true`      |

### Ingress Parameters

| Parameter               | Description        | Default             |
| ----------------------- | ------------------ | ------------------- |
| `ingress.enabled`       | Enable ingress     | `true`              |
| `ingress.className`     | Ingress class name | `nginx`             |
| `ingress.hosts[0].host` | Hostname           | `user.gripday.site` |

### Resources

| Parameter                   | Description    | Default |
| --------------------------- | -------------- | ------- |
| `resources.requests.memory` | Memory request | `384Mi` |
| `resources.requests.cpu`    | CPU request    | `250m`  |
| `resources.limits.memory`   | Memory limit   | `768Mi` |
| `resources.limits.cpu`      | CPU limit      | `500m`  |

### PostgreSQL Parameters

| Parameter                              | Description          | Default       |
| -------------------------------------- | -------------------- | ------------- |
| `postgresql.enabled`                   | Enable PostgreSQL    | `true`        |
| `postgresql.image.tag`                 | PostgreSQL image tag | `15.8-alpine` |
| `postgresql.persistence.size`          | PVC size             | `5Gi`         |
| `postgresql.resources.requests.memory` | Memory request       | `256Mi`       |
| `postgresql.resources.requests.cpu`    | CPU request          | `250m`        |

### Redis Parameters

| Parameter                | Description     | Default      |
| ------------------------ | --------------- | ------------ |
| `redis.enabled`          | Enable Redis    | `true`       |
| `redis.image.tag`        | Redis image tag | `7.2-alpine` |
| `redis.persistence.size` | PVC size        | `1Gi`        |
| `redis.config.maxmemory` | Max memory      | `256mb`      |

### Security Parameters

| Parameter                      | Description           | Default         |
| ------------------------------ | --------------------- | --------------- |
| `networkPolicy.enabled`        | Enable network policy | `true`          |
| `podSecurityContext.runAsUser` | Run as user ID        | `1001`          |
| `priorityClassName`            | Priority class name   | `high-priority` |

## Examples

### Install with custom values

```bash
helm install user-service ./user-service -f custom-values.yaml
```

### Upgrade with new values

```bash
helm upgrade user-service ./user-service --set replicaCount=3
```

### Install without PostgreSQL (use external database)

```bash
helm install user-service ./user-service --set postgresql.enabled=false
```

### Enable autoscaling

```bash
helm install user-service ./user-service \
  --set autoscaling.enabled=true \
  --set autoscaling.minReplicas=2 \
  --set autoscaling.maxReplicas=10
```

## Values Files for Different Environments

### Local Development (values.yaml)

Default values file

### Staging (values-staging.yaml)

```bash
helm install user-service ./user-service -f values-staging.yaml
```

### Production (values-production.yaml)

```bash
helm install user-service ./user-service -f values-production.yaml
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods -n gripday-user
```

### View logs

```bash
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=gripday-user-service
```

### Check service endpoints

```bash
kubectl get endpoints -n gripday-user
```

### Describe pod for events

```bash
kubectl describe pod <pod-name> -n gripday-user
```

## Architecture

### Service Dependencies

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│      Gateway Service (Optional)     │
└──────┬──────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│         User Service                │
│  - JWT Authentication               │
│  - User Management                  │
│  - Email Verification               │
│  - Password Reset                   │
└──────┬──────────────────────────────┘
       │
       ├──────────────┬─────────────────┐
       ↓              ↓                 ↓
┌──────────────┐ ┌──────────────┐ ┌──────────┐
│ PostgreSQL   │ │    Redis     │ │   SMTP   │
│ (User Data)  │ │ (Sessions &  │ │  Server  │
│              │ │  Blacklist)  │ │          │
└──────────────┘ └──────────────┘ └──────────┘
```

### Key Features

- **JWT Token Generation**: RSA256-signed tokens with public key distribution via JWK endpoint
- **Email Verification**: Token-based workflow with rate limiting (3 emails/hour)
- **Password Reset**: Secure flow with time-limited tokens (24h expiry)
- **Account Lockout**: Progressive lockout after 5 failed attempts (15min duration)
- **Token Blacklist**: Redis-backed blacklist for logout functionality
- **Multi-Tenancy**: Tenant context in JWT claims for downstream services
- **Audit Logging**: Security events with correlation IDs

## Monitoring

### Health Checks

```bash
# Liveness probe
curl http://user-service:8080/actuator/health/liveness

# Readiness probe
curl http://user-service:8080/actuator/health/readiness

# Full health details
curl http://user-service:8080/actuator/health
```

### JWK Endpoint

```bash
# Get public keys for JWT validation
curl http://user-service:8080/.well-known/jwks.json | jq

# Example response
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "key-id-1",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

### Metrics

```bash
# Prometheus metrics
curl http://user-service:8080/actuator/prometheus

# User-specific metrics
curl http://user-service:8080/actuator/prometheus | grep gripday_user
```

## Troubleshooting

### Check Deployment Status

```bash
# Pod status
kubectl get pods -n gripday-user -l app.kubernetes.io/name=gripday-user-service

# Deployment status
kubectl rollout status deployment/user-service -n gripday-user

# HPA status
kubectl get hpa -n gripday-user
```

### View Logs

```bash
# Application logs
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=gripday-user-service

# PostgreSQL logs
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=user-postgres

# Redis logs
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=user-redis

# Previous container logs (if crashed)
kubectl logs -n gripday-user <pod-name> --previous
```

### Test Authentication

```bash
# Port forward
kubectl port-forward -n gripday-user svc/user-service 8080:8080

# Register user
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'

# Get current user (requires JWT)
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8080/api/v1/users/me
```

### Debug Database

```bash
# Connect to PostgreSQL
kubectl exec -it -n gripday-user \
  $(kubectl get pod -n gripday-user -l app.kubernetes.io/name=user-postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U gripday_user -d gripday_user_local

# Check tables
\dt

# Check users
SELECT id, username, email, enabled, email_verified FROM users;

# Check Liquibase changelog
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;

# Exit
\q
```

### Debug Redis

```bash
# Connect to Redis
kubectl exec -it -n gripday-user \
  $(kubectl get pod -n gripday-user -l app.kubernetes.io/name=user-redis -o jsonpath='{.items[0].metadata.name}') \
  -- redis-cli

# Check token blacklist
KEYS "gripday:user:token:blacklist:*"

# Check session data
KEYS "spring:session:*"

# Check key TTL
TTL "gripday:user:token:blacklist:abc123"
```

### Common Issues

**Pods not starting**
- Check resource quotas: `kubectl describe resourcequota -n gripday-user`
- Verify secrets exist: `kubectl get secrets -n gripday-user`
- Check image pull: `kubectl describe pod <pod-name> -n gripday-user`

**Database connection failures**
- Verify PostgreSQL is running: `kubectl get pods -n gripday-user -l app.kubernetes.io/name=user-postgres`
- Check database credentials in secrets
- Verify network policy allows traffic

**Email not sending**
- Check SMTP configuration in environment variables
- Verify SMTP credentials in secrets
- Check application logs for email errors
- Test SMTP connectivity from pod

**JWT validation failures in downstream services**
- Ensure JWK endpoint is accessible: `curl http://user-service:8080/.well-known/jwks.json`
- Check network policy allows traffic from downstream services
- Verify JWT issuer matches configuration

## Upgrading

```bash
# Upgrade with new values
helm upgrade user-service ./helm/user-service \
  -f ./helm/user-service/values-production.yaml \
  --namespace gripday-user-production

# Rollback if needed
helm rollback user-service --namespace gripday-user-production
```

## Security

### Network Policies

The chart includes network policies that:
- Allow ingress from Gateway Service
- Allow egress to PostgreSQL and Redis
- Allow egress to SMTP server (port 587)
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

Required secrets:
- `GRIPDAY_DATABASE_USERNAME`
- `GRIPDAY_DATABASE_PASSWORD`
- `GRIPDAY_AUTH_JWT_SECRET` (RSA private key for JWT signing)
- `SMTP_USERNAME` (for email functionality)
- `SMTP_PASSWORD` (for email functionality)
- `GRIPDAY_CACHE_REDIS_PASSWORD` (optional)
- `GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID` (optional, for OAuth2)
- `GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_SECRET` (optional, for OAuth2)

## Support

- GitHub Issues: https://github.com/gripday/user-service
- Platform Team: platform@gripday.site
- Documentation: https://docs.gripday.site
