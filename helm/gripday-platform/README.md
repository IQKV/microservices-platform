# Gripday Platform - Umbrella Helm Chart

This umbrella chart deploys the complete Gripday Platform microservices architecture.

## Services Included

1. **User Service** - Authentication and authorization
2. **Bookstore Service** - Business domain service
3. **Gateway Service** - API Gateway (Spring Cloud Gateway)

## Architecture

```
┌─────────────────────────────────────────────┐
│           Ingress (nginx)                   │
└──────────────────┬──────────────────────────┘
                   │
           ┌───────▼────────┐
           │ Gateway Service │
           │   (Port 8080)   │
           └───────┬─────────┘
                   │
        ┏━━━━━━━━━━┻━━━━━━━━━━┓
        ▼                      ▼
┌───────────────┐      ┌──────────────────┐
│ User Service  │      │Bookstore Service │
│  (Port 8080)  │      │   (Port 8080)    │
├───────────────┤      ├──────────────────┤
│ PostgreSQL    │      │  PostgreSQL      │
│ Redis         │      │  Redis           │
└───────────────┘      └──────────────────┘
```

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- PV provisioner support
- Ingress controller (nginx recommended)

## Quick Start

### 1. Install Priority Classes

First, install the priority classes:

```bash
kubectl apply -f ../k8s/priority-classes.yaml
```

### 2. Install the Complete Platform

```bash
# Build dependencies
helm dependency build

# Install all services
helm install gripday . --namespace gripday --create-namespace
```

### 3. Verify Installation

```bash
# Check all pods
kubectl get pods -n gripday-auth
kubectl get pods -n gripday-bookstore
kubectl get pods -n gripday-gateway

# Check services
kubectl get svc -n gripday-auth
kubectl get svc -n gripday-bookstore
kubectl get svc -n gripday-gateway

# Check ingress
kubectl get ingress -A
```

## Installation Options

### Install Individual Services

```bash
# Install only auth service
helm install gripday . --set bookstore-service.enabled=false --set gateway-service.enabled=false

# Install without gateway
helm install gripday . --set gateway-service.enabled=false
```

### Custom Values

Create a `custom-values.yaml`:

```yaml
global:
  environment: production

user-service:
  replicaCount: 3
  resources:
    requests:
      memory: "512Mi"
      cpu: "500m"

gateway-service:
  replicaCount: 5
  autoscaling:
    minReplicas: 5
    maxReplicas: 20
```

Install with custom values:

```bash
helm install gripday . -f custom-values.yaml
```

## Configuration

### Global Configuration

| Parameter            | Description         | Default            |
| -------------------- | ------------------- | ------------------ |
| `global.environment` | Environment name    | `local`            |
| `global.platform`    | Platform identifier | `gripday` |

### Service-Specific Configuration

Each service can be configured independently. See individual service READMEs:

- [User Service](../user-service/README.md)
- [Bookstore Service](../bookstore-service/README.md)
- [Gateway Service](../gateway-service/README.md)

## Upgrading

```bash
# Upgrade with new values
helm upgrade gripday . -f custom-values.yaml

# Upgrade specific service version
helm upgrade gripday . --set user-service.image.tag=1.1.0
```

## Uninstalling

```bash
# Uninstall the platform
helm uninstall gripday --namespace gripday

# Clean up namespaces
kubectl delete namespace gripday-auth
kubectl delete namespace gripday-bookstore
kubectl delete namespace gripday-gateway
```

## Monitoring

### Health Checks

```bash
# Gateway health
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080
curl http://localhost:8080/actuator/health

# Auth service health
kubectl port-forward -n gripday-auth svc/user-service 8080:8080
curl http://localhost:8080/actuator/health

# Bookstore service health
kubectl port-forward -n gripday-bookstore svc/bookstore-service 8080:8080
curl http://localhost:8080/actuator/health
```

### View Logs

```bash
# Gateway logs
kubectl logs -f -n gripday-gateway -l app.kubernetes.io/name=gripday-gateway-service

# Auth service logs
kubectl logs -f -n gripday-auth -l app.kubernetes.io/name=gripday-user-service

# Bookstore service logs
kubectl logs -f -n gripday-bookstore -l app.kubernetes.io/name=gripday-bookstore-service
```

## Troubleshooting

### Common Issues

1. **Pods not starting**

   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   kubectl logs <pod-name> -n <namespace>
   ```

2. **Service communication issues**

   ```bash
   # Test DNS resolution
   kubectl run test-pod --image=busybox --rm -it -- nslookup user-service.gripday-auth.svc.cluster.local
   ```

3. **Database connection issues**

   ```bash
   # Check PostgreSQL
   kubectl exec -it <postgres-pod> -n <namespace> -- psql -U gripday_user -d <database>
   ```

4. **Persistent volume issues**
   ```bash
   kubectl get pv
   kubectl get pvc -A
   ```

## Development Workflow

### Local Development

```bash
# Install in local/dev mode
helm install gripday . --set global.environment=local

# Use port-forwarding for local access
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080
```

### Staging Deployment

```bash
helm install gripday . -f values-staging.yaml --namespace staging
```

### Production Deployment

```bash
helm install gripday . -f values-production.yaml --namespace production
```

## Network Policies

Network policies are enabled by default for security:

- Gateway accepts external traffic via ingress
- Services communicate within the cluster
- Database access restricted to service pods

## Security

- All pods run as non-root users
- Read-only root filesystems where possible
- Pod Security Standards enforced (restricted)
- Network policies limit pod-to-pod communication
- Secrets management via Kubernetes Secrets (consider external secret managers for production)

## Scaling

### Manual Scaling

```bash
# Scale gateway
kubectl scale deployment gateway-service -n gripday-gateway --replicas=5

# Scale auth service
kubectl scale deployment user-service -n gripday-auth --replicas=3
```

### Auto-scaling (HPA)

HPA is configured for:

- Gateway Service (3-10 replicas)
- Bookstore Service (2-10 replicas)

Monitor HPA:

```bash
kubectl get hpa -A
```

## Backup and Restore

### Database Backups

```bash
# Backup auth database
kubectl exec -n gripday-auth user-postgres-<pod> -- pg_dump -U gripday_user gripday_user_local > auth-backup.sql

# Backup bookstore database
kubectl exec -n gripday-bookstore bookstore-postgres-<pod> -- pg_dump -U gripday_user gripday_bookstore_local > bookstore-backup.sql
```

## Support and Contact

For issues, questions, or contributions:

- Email: platform-team@gripday.site
- GitHub: https://github.com/gripday/platform

## License

Proprietary - Gripday Platform Team
