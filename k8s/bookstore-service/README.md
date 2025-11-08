# Gripday Bookstore Service - Kubernetes Deployment

This directory contains Kubernetes manifests and deployment scripts for the Gripday Bookstore Service.

## Overview

The bookstore service provides book catalog management, inventory tracking, and search functionality. It's built with Spring Boot 3.5.6, Java 21, and follows the three-tier architecture pattern.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Ingress       │    │  Bookstore      │    │   PostgreSQL    │
│   Controller    │───▶│   Service       │───▶│   Database      │
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

- **bookstore-service**: Main Spring Boot application (port 8082)
- **bookstore-postgres**: PostgreSQL 15.8 database for persistent storage
- **bookstore-redis**: Redis 7.4 for caching and session management

### Kubernetes Resources

- **Deployment**: Application pods with rolling update strategy
- **Service**: ClusterIP service for internal communication
- **Ingress**: HTTP/HTTPS routing with CORS and rate limiting
- **HPA**: Horizontal Pod Autoscaler for automatic scaling
- **NetworkPolicy**: Security policies for network isolation
- **ConfigMap**: Application configuration
- **Secret**: Sensitive configuration (passwords, JWT secrets)
- **PVC**: Persistent storage for databases

## Deployment

### Prerequisites

1. **Kubernetes Cluster**: Running cluster with kubectl access
2. **Ingress Controller**: NGINX ingress controller installed
3. **Storage Class**: Available storage class for persistent volumes
4. **Docker Images**: Built bookstore service image

### Quick Deployment

```bash
# Deploy to local environment (minikube)
./deploy-bookstore.sh

# Deploy to staging
./deploy-bookstore.sh -e staging

# Deploy to production
./deploy-bookstore.sh -e production

# Dry run to see what would be deployed
./deploy-bookstore.sh --dry-run
```

### Manual Deployment

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Apply configuration and secrets
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# 3. Deploy databases
kubectl apply -f bookstore-postgres-deployment.yaml
kubectl apply -f bookstore-postgres-service.yaml
kubectl apply -f bookstore-redis-deployment.yaml
kubectl apply -f bookstore-redis-service.yaml

# 4. Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-postgres -n gripday-bookstore --timeout=300s
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=bookstore-redis -n gripday-bookstore --timeout=300s

# 5. Deploy application
kubectl apply -f bookstore-service-deployment.yaml
kubectl apply -f bookstore-service-service.yaml
kubectl apply -f bookstore-service-hpa.yaml
kubectl apply -f network-policy.yaml
kubectl apply -f bookstore-service-ingress.yaml

# 6. Verify deployment
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=gripday-bookstore-service -n gripday-bookstore --timeout=300s
```

## Configuration

### Environment Variables

The service uses the following key environment variables:

```bash
# Database Configuration
GRIPDAY_DATABASE_URL=jdbc:postgresql://bookstore-postgres:5432/gripday_bookstore_local
GRIPDAY_DATABASE_USERNAME=bookstore_user
GRIPDAY_DATABASE_PASSWORD=bookstore_pass

# Redis Configuration
GRIPDAY_CACHE_REDIS_HOST=bookstore-redis
GRIPDAY_CACHE_REDIS_PORT=6379
GRIPDAY_CACHE_REDIS_DATABASE=2

# JWT Configuration
GRIPDAY_AUTH_JWT_SECRET=<shared-jwt-secret>

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=gripday-bookstore-service
```

### Resource Requirements

| Component         | CPU Request | CPU Limit | Memory Request | Memory Limit |
| ----------------- | ----------- | --------- | -------------- | ------------ |
| Bookstore Service | 250m        | 500m      | 384Mi          | 768Mi        |
| PostgreSQL        | 250m        | 500m      | 256Mi          | 512Mi        |
| Redis             | 100m        | 200m      | 128Mi          | 256Mi        |

### Scaling Configuration

- **Min Replicas**: 2 (local), 2 (staging), 3 (production)
- **Max Replicas**: 10 (local), 6 (staging), 20 (production)
- **CPU Threshold**: 70% (local), 75% (staging), 60% (production)
- **Memory Threshold**: 80% (local), 85% (staging), 70% (production)

## Networking

### Service Ports

- **Bookstore Service**: 8082 (HTTP)
- **PostgreSQL**: 5432
- **Redis**: 6379

### Ingress Routes

#### Local Environment

```
http://localhost/api/v1/bookstore/*          → Bookstore API (protected)
http://localhost/api/v1/bookstore/public/*   → Public API (no auth)
http://localhost/bookstore/swagger-ui/*      → Swagger UI
http://localhost/bookstore/actuator/*        → Health/metrics
```

#### Staging Environment

```
https://api.gripday.website/api/v1/bookstore/*  → Bookstore API
```

#### Production Environment

```
https://api.gripday.com/api/v1/bookstore/*  → Bookstore API
```

### Network Policies

The deployment includes network policies that:

- Allow ingress from gateway service and ingress controller
- Allow egress to PostgreSQL and Redis
- Allow egress to auth service for JWT validation
- Allow egress to observability services
- Deny all other traffic

## Security

### Pod Security

- **Non-root user**: Runs as UID 1001
- **Read-only root filesystem**: Prevents runtime modifications
- **No privilege escalation**: Security hardening
- **Dropped capabilities**: Minimal required capabilities

### Network Security

- **Network policies**: Restrict traffic between pods
- **TLS termination**: At ingress level for staging/production
- **CORS policies**: Environment-specific CORS configuration
- **Rate limiting**: Request rate limiting at ingress

### Secrets Management

- **JWT secrets**: Shared with auth service
- **Database credentials**: Environment-specific
- **Redis passwords**: Optional for local, required for staging/production

## Monitoring and Observability

### Health Checks

- **Liveness Probe**: `/actuator/health/liveness` (60s delay, 30s interval)
- **Readiness Probe**: `/actuator/health/readiness` (30s delay, 10s interval)
- **Startup Probe**: `/actuator/health` (30s delay, 10s interval, 12 failures)

### Metrics

- **Prometheus metrics**: Available at `/actuator/prometheus`
- **Custom metrics**: Book operations, inventory levels, search performance
- **JVM metrics**: Memory, GC, thread pools

### Tracing

- **OpenTelemetry**: Distributed tracing integration
- **Jaeger**: Trace collection and visualization
- **Correlation IDs**: Request correlation across services

### Logging

- **Structured logging**: JSON format for staging/production
- **Log levels**: DEBUG (local), INFO (staging), WARN (production)
- **Audit logging**: Administrative operations tracking

## Troubleshooting

### Common Issues

1. **Pod not starting**

   ```bash
   kubectl describe pod -l app.kubernetes.io/name=gripday-bookstore-service -n gripday-bookstore
   kubectl logs -l app.kubernetes.io/name=gripday-bookstore-service -n gripday-bookstore
   ```

2. **Database connection issues**

   ```bash
   kubectl exec -it deployment/bookstore-postgres -n gripday-bookstore -- psql -U bookstore_user -d gripday_bookstore_local
   ```

3. **Redis connection issues**

   ```bash
   kubectl exec -it deployment/bookstore-redis -n gripday-bookstore -- redis-cli ping
   ```

4. **Service not accessible**
   ```bash
   kubectl get ingress -n gripday-bookstore
   kubectl describe ingress bookstore-service-ingress -n gripday-bookstore
   ```

### Useful Commands

```bash
# Check all resources
kubectl get all -n gripday-bookstore

# View logs
kubectl logs -f deployment/bookstore-service -n gripday-bookstore

# Port forward for local access
kubectl port-forward service/bookstore-service 8082:8082 -n gripday-bookstore

# Scale manually
kubectl scale deployment bookstore-service --replicas=3 -n gripday-bookstore

# Check HPA status
kubectl get hpa -n gripday-bookstore

# View network policies
kubectl get networkpolicy -n gripday-bookstore
```

## API Endpoints

### Public Endpoints (No Authentication)

- `GET /api/v1/bookstore/public/books` - List books with pagination
- `GET /api/v1/bookstore/public/books/{id}` - Get book details
- `GET /api/v1/bookstore/public/search` - Search books

### Protected Endpoints (Authentication Required)

- `POST /api/v1/bookstore/books` - Create book (admin only)
- `PUT /api/v1/bookstore/books/{id}` - Update book (admin only)
- `DELETE /api/v1/bookstore/books/{id}` - Delete book (admin only)
- `PUT /api/v1/bookstore/inventory/{bookId}` - Update inventory (admin only)

### Management Endpoints

- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /swagger-ui.html` - API documentation

## Integration

### Gateway Service Integration

The bookstore service integrates with the gateway service for:

- **Routing**: All requests routed through gateway
- **Authentication**: JWT validation via gateway
- **Rate limiting**: Distributed rate limiting
- **CORS**: Centralized CORS handling

### Auth Service Integration

- **JWT validation**: Shared JWT secret for token validation
- **User context**: Extract user information from JWT tokens
- **Role-based access**: Admin operations require admin role

### Observability Stack Integration

- **Prometheus**: Metrics collection
- **Grafana**: Dashboards and visualization
- **Jaeger**: Distributed tracing
- **Loki**: Log aggregation

## Development

### Local Development Setup

1. **Start minikube**

   ```bash
   minikube start
   ```

2. **Deploy bookstore service**

   ```bash
   ./deploy-bookstore.sh
   ```

3. **Access services**

   ```bash
   # Add to /etc/hosts
   echo "$(minikube ip) localhost" >> /etc/hosts

   # Access API
   curl http://localhost/api/v1/bookstore/public/books

   # Access Swagger UI
   open http://localhost/bookstore/swagger-ui.html
   ```

### Testing

```bash
# Run integration tests
kubectl apply -f ../test-pod.yaml
kubectl exec -it test-pod -- curl http://bookstore-service:8082/actuator/health

# Load testing
kubectl run load-test --image=busybox --rm -it --restart=Never -- /bin/sh
# Inside the pod:
# while true; do wget -q -O- http://bookstore-service:8082/api/v1/bookstore/public/books; sleep 1; done
```

## Maintenance

### Backup

- **Database backup**: Use pg_dump for PostgreSQL backups
- **Redis backup**: Use BGSAVE for Redis snapshots
- **Configuration backup**: Store manifests in version control

### Updates

- **Rolling updates**: Use deployment rolling update strategy
- **Database migrations**: Liquibase handles schema migrations
- **Configuration updates**: Update ConfigMaps and restart pods

### Monitoring

- **Resource usage**: Monitor CPU, memory, and storage usage
- **Performance metrics**: Track response times and error rates
- **Business metrics**: Monitor book operations and search performance
