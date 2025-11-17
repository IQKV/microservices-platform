# User Service Helm Chart

Production-ready Helm chart for deploying the Gripday User Service with PostgreSQL and Redis on Kubernetes.

## Overview

The User Service is the centralized authentication and user management microservice for the Gripday platform. It provides JWT-based authentication, user lifecycle management, email verification, password reset flows, and role-based access control.

**Architecture**: User service is internal-only with ClusterIP service. Access via Gateway Service at `api.gripday.site/api/v1/auth`

## Features

- **JWT Authentication**: Access tokens (15min) and refresh tokens (7 days) with RSA256 signing
- **User Management**: Registration, login, profile management, and admin operations
- **Email Verification**: Token-based email verification with rate limiting
- **Password Management**: Secure password reset flow with time-limited tokens
- **Account Security**: Lockout protection, rate limiting, audit logging
- **Multi-Tenancy**: Tenant-scoped data isolation
- **JWK Endpoint**: Public key distribution for downstream JWT validation (/.well-known/jwks.json)
- **Observability**: Prometheus metrics, OpenTelemetry tracing, structured logging

## Prerequisites

- Kubernetes 1.24+
- Helm 3.8+
- PV provisioner for persistent storage (local-path for k3s)
- Gateway Service deployed (for external access)
- SMTP server for email functionality (production)

## Environments and Domains

| Environment | Access Domain       | Namespace              | Values File            |
| ----------- | ------------------- | ---------------------- | ---------------------- |
| Development | api.gripday.site    | gripday-dev-env        | values.yaml            |
| Test        | api.gripday.website | gripday-test-env       | values-test.yaml       |
| Staging     | api.gripday.space   | gripday-staging-env    | values-staging.yaml    |
| Production  | api.gripday.com     | gripday-production-env | values-production.yaml |

**Note**: User service is accessed via Gateway Service at `/api/v1/auth/**` and `/api/v1/users/**`

## Quick Start

### Local Development (k3s)

```bash
# Install with default values (dev environment)
helm install user-service ./helm/user-service \
  --namespace gripday-dev-env \
  --create-namespace

# Verify deployment
kubectl get pods -n gripday-dev-env
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-user-service

# Check database initialization
kubectl logs -n gripday-dev-env -l app.kubernetes.io/name=user-postgres
```

**Note**: Default values are optimized for k3s single-node development with 1 replica, HPA disabled, and local-path storage.

### Test Environment

```bash
# Create secrets first
kubectl create secret generic user-service-secrets \
  --from-literal=GRIPDAY_DATABASE_USERNAME=user_db_user \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=secure_password \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=jwt_secret_key \
  --from-literal=SMTP_USERNAME=smtp_user \
  --from-literal=SMTP_PASSWORD=smtp_password \
  -n gripday-test-env

# Install with test configuration
helm install user-service ./helm/user-service \
  -f ./helm/user-service/values-test.yaml \
  --namespace gripday-test-env \
  --create-namespace
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
  -n gripday-staging-env

# Install with staging configuration
helm install user-service ./helm/user-service \
  -f ./helm/user-service/values-staging.yaml \
  --namespace gripday-staging-env \
  --create-namespace
```

### Production Environment

```bash
# Create secrets externally (AWS Secrets Manager, Vault, etc.)
# Then install with production configuration
helm install user-service ./helm/user-service \
  -f ./helm/user-service/values-production.yaml \
  --namespace gripday-production-env \
  --create-namespace
```

## Configuration

### Key Parameters

| Parameter                     | Description        | Default                |
| ----------------------------- | ------------------ | ---------------------- |
| `replicaCount`                | Number of replicas | `1`                    |
| `image.repository`            | Image repository   | `gripday/user-service` |
| `image.tag`                   | Image tag          | `1.0.0`                |
| `service.port`                | Service port       | `80`                   |
| `service.targetPort`          | Container port     | `8080`                 |
| `autoscaling.enabled`         | Enable HPA         | `false`                |
| `autoscaling.minReplicas`     | Minimum replicas   | `1`                    |
| `autoscaling.maxReplicas`     | Maximum replicas   | `3`                    |
| `postgresql.enabled`          | Enable PostgreSQL  | `true`                 |
| `postgresql.persistence.size` | PostgreSQL storage | `5Gi`                  |
| `redis.enabled`               | Enable Redis       | `true`                 |
| `redis.persistence.size`      | Redis storage      | `1Gi`                  |
| `priorityClassName`           | Pod priority class | `""`                   |

### Environment-Specific Configuration

**Local Development** (`values.yaml`)

- 1 replica (k3s optimized)
- HPA disabled
- PDB disabled
- No priority class
- Embedded secrets for development
- 5Gi PostgreSQL, 1Gi Redis storage with local-path storage class
- Human-readable console logging
- Full tracing (100% sampling)
- Database: gripday_user_local
- SMTP disabled (localhost)
- No affinity/topology constraints

**Test** (`values-test.yaml`)

- 2 replicas, autoscaling 2-10 pods (CPU 70%, Memory 80%)
- PDB enabled (minAvailable: 1)
- High priority class
- External secret management required
- 10Gi PostgreSQL, 2Gi Redis storage with standard storage class
- JSON structured logging
- Full tracing (100% sampling)
- Database: gripday_user_test
- SMTP enabled (SendGrid)
- App base URL: https://gripday.website

**Staging** (`values-staging.yaml`)

- 3 replicas, autoscaling 3-15 pods (CPU 65%, Memory 75%)
- PDB enabled (minAvailable: 2)
- High priority class
- External secret management required
- 10Gi PostgreSQL, 2Gi Redis storage with fast-ssd storage class
- JSON structured logging
- Full tracing (100% sampling)
- Database: gripday_user_staging
- SMTP enabled (SendGrid)
- App base URL: https://gripday.space

**Production** (`values-production.yaml`)

- 5 replicas, autoscaling 5-20 pods (CPU 60%, Memory 70%)
- PDB enabled (minAvailable: 3)
- Critical priority class
- External secret management required (AWS Secrets Manager, Vault)
- 20Gi PostgreSQL, 5Gi Redis storage with fast-ssd storage class
- JSON structured logging
- Reduced tracing (10% sampling)
- Database: gripday_user_production
- SMTP enabled (SendGrid)
- App base URL: https://gripday.com
- OAuth2 Google enabled
- Strict pod anti-affinity (requiredDuringScheduling)
- Multi-zone distribution with topology spread constraints
- Preferred node types: c5.xlarge, c5.2xlarge

## Java Configuration

### JVM Options by Environment

**Development**:

```
-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

**Test/Staging**:

```
-Xms384m -Xmx768m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof
```

**Production**:

```
-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof
-XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled
```

## Resource Requirements

### User Service

| Environment | Replicas | CPU Request | Memory Request | CPU Limit | Memory Limit |
| ----------- | -------- | ----------- | -------------- | --------- | ------------ |
| Development | 1        | 250m        | 384Mi          | 500m      | 768Mi        |
| Test        | 2        | 300m        | 512Mi          | 750m      | 1Gi          |
| Staging     | 3        | 300m        | 512Mi          | 750m      | 1Gi          |
| Production  | 5        | 500m        | 768Mi          | 1000m     | 1536Mi       |

### PostgreSQL

| Environment | CPU Request | Memory Request | CPU Limit | Memory Limit | Storage | Storage Class |
| ----------- | ----------- | -------------- | --------- | ------------ | ------- | ------------- |
| Development | 250m        | 256Mi          | 500m      | 512Mi        | 5Gi     | local-path    |
| Test        | 500m        | 512Mi          | 1000m     | 1Gi          | 10Gi    | standard      |
| Staging     | 500m        | 512Mi          | 1000m     | 1Gi          | 10Gi    | fast-ssd      |
| Production  | 1000m       | 1Gi            | 2000m     | 2Gi          | 20Gi    | fast-ssd      |

PostgreSQL configuration:

- Image: postgres:15.8-alpine
- Extensions: uuid-ossp, pgcrypto
- Liquibase migrations enabled

### Redis

| Environment | CPU Request | Memory Request | CPU Limit | Memory Limit | Storage | Max Memory | Storage Class |
| ----------- | ----------- | -------------- | --------- | ------------ | ------- | ---------- | ------------- |
| Development | 100m        | 128Mi          | 200m      | 256Mi        | 1Gi     | 256mb      | local-path    |
| Test        | 150m        | 192Mi          | 300m      | 384Mi        | 2Gi     | 384mb      | standard      |
| Staging     | 150m        | 192Mi          | 300m      | 384Mi        | 2Gi     | 384mb      | fast-ssd      |
| Production  | 200m        | 256Mi          | 400m      | 512Mi        | 5Gi     | 512mb      | fast-ssd      |

Redis configuration:

- Image: redis:7.2-alpine
- Max memory policy: allkeys-lru
- Append-only file: enabled
- Production: RDB snapshots enabled (900 1 300 10 60 10000)

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
- Custom metrics prefix: `gripday_user`
- Distribution percentiles: 0.5, 0.95, 0.99

### Logging

- Development: Human-readable console format
- Test/Staging/Production: JSON structured logging
- Correlation ID propagation via X-Correlation-ID header
- Log levels:
  - Development: INFO (SQL: not shown)
  - Test: INFO (SQL: WARN)
  - Staging: INFO (SQL: WARN, AUDIT: INFO)
  - Production: WARN (SQL: WARN, AUDIT: INFO)

## Architecture

### Service Dependencies

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│      Gateway Service (Public)       │
└──────┬──────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│         User Service (Internal)     │
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

### API Endpoints

**Authentication** (`/api/v1/auth/**`):

- POST /login - User login
- POST /signup - User registration
- POST /refresh - Refresh access token
- POST /validate - Validate JWT token
- GET /health - Service health check
- POST /email/verify - Verify email with token
- POST /email/resend - Resend verification email
- GET /email/status - Check email verification status
- POST /password/forgot - Request password reset
- POST /password/reset - Reset password with token

**User Management** (`/api/v1/users/**`):

- GET /me - Get current user profile
- PUT /me - Update current user profile
- DELETE /me - Delete current user account

**Admin Operations** (`/api/v1/admin/**`):

- GET /users - List all users (admin only)
- POST /users - Create user (admin only)
- GET /users/{id} - Get user by ID (admin only)
- PUT /users/{id} - Update user (admin only)
- DELETE /users/{id} - Delete user (admin only)

**JWK Endpoint**:

- GET /.well-known/jwks.json - Public keys for JWT validation

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
# Pod status (adjust namespace as needed)
kubectl get pods -n gripday-dev-env -l app.kubernetes.io/name=gripday-user-service

# Deployment status
kubectl rollout status deployment/user-service -n gripday-dev-env

# HPA status (if enabled)
kubectl get hpa -n gripday-dev-env
```

### View Logs

```bash
# Application logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gripday-user-service

# PostgreSQL logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=user-postgres

# Redis logs
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=user-redis

# Previous container logs (if crashed)
kubectl logs -n gripday-dev-env <pod-name> --previous
```

### Test Authentication

```bash
# Port forward
kubectl port-forward -n gripday-dev-env svc/user-service 8080:80

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
kubectl exec -it -n gripday-dev-env \
  $(kubectl get pod -n gripday-dev-env -l app.kubernetes.io/name=user-postgres -o jsonpath='{.items[0].metadata.name}') \
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
kubectl exec -it -n gripday-dev-env \
  $(kubectl get pod -n gripday-dev-env -l app.kubernetes.io/name=user-redis -o jsonpath='{.items[0].metadata.name}') \
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

- Check resource quotas: `kubectl describe resourcequota -n gripday-dev-env`
- Verify secrets exist: `kubectl get secrets -n gripday-dev-env`
- Check image pull: `kubectl describe pod <pod-name> -n gripday-dev-env`

**Database connection failures**

- Verify PostgreSQL is running: `kubectl get pods -n gripday-dev-env -l app.kubernetes.io/name=user-postgres`
- Check database credentials in secrets
- Verify network policy allows traffic

**Liquibase migration failures**

- Check application logs for migration errors
- Verify database user has necessary permissions
- Check Liquibase changelog table: `SELECT * FROM databasechangelog;`

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
# Upgrade with new values (adjust namespace and values file as needed)
helm upgrade user-service ./helm/user-service \
  -f ./helm/user-service/values-production.yaml \
  --namespace gripday-production-env

# Rollback if needed
helm rollback user-service --namespace gripday-production-env
```

## Uninstalling

```bash
# Adjust namespace as needed
helm uninstall user-service --namespace gripday-dev-env
```

## Security

### Network Policies

The chart includes network policies that:

- Allow ingress from Gateway Service
- Allow ingress from same namespace
- Allow ingress from monitoring namespace
- Allow egress to PostgreSQL and Redis
- Allow egress to SMTP server (port 587)
- Allow egress to observability services
- Allow egress to OAuth2 providers (port 443)
- Block all other traffic

### Pod Security

- Runs as non-root user (UID 1001)
- Read-only root filesystem
- Drops all capabilities
- Seccomp profile enabled
- Pod security standards: restricted

### Secrets Management

**Local Development**: Embedded base64-encoded secrets in values.yaml (not for production)
**Test/Staging**: Use Kubernetes secrets created manually
**Production**: Use external secret management (AWS Secrets Manager, Vault, Azure Key Vault)

Required secrets:

- `GRIPDAY_DATABASE_USERNAME`: PostgreSQL username
- `GRIPDAY_DATABASE_PASSWORD`: PostgreSQL password
- `GRIPDAY_AUTH_JWT_SECRET`: RSA private key for JWT signing
- `SMTP_USERNAME`: SMTP username (optional in dev)
- `SMTP_PASSWORD`: SMTP password (optional in dev)
- `GRIPDAY_CACHE_REDIS_PASSWORD`: Redis password (optional)
- `GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_ID`: Google OAuth2 client ID (optional)
- `GRIPDAY_AUTH_OAUTH2_GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret (optional)

## Included Resources

The Helm chart creates the following Kubernetes resources:

- Namespace (with pod security standards)
- ServiceAccount (with automountServiceAccountToken: false)
- ConfigMap (application.yml configuration)
- Secret (database, JWT, SMTP, and OAuth2 credentials)
- Deployment (user service pods)
- Service (ClusterIP with headless option)
- Ingress (disabled by default, internal-only service)
- HorizontalPodAutoscaler (optional, disabled by default)
- PodDisruptionBudget (optional, disabled by default)
- NetworkPolicy (ingress/egress rules)
- ResourceQuota (namespace resource limits)
- LimitRange (container resource constraints)
- PostgreSQL Deployment (user database)
- PostgreSQL Service (ClusterIP)
- PostgreSQL NetworkPolicy (access control)
- Redis Deployment (sessions and token blacklist)
- Redis Service (ClusterIP)
- Redis NetworkPolicy (access control)

## Support

- GitHub Issues: https://github.com/gripday/user-service
- Platform Team: platform@gripday.site
- Documentation: https://docs.gripday.site
