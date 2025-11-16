# Gripday Platform - Helm Chart

Complete Helm chart for deploying the Gripday microservices platform.

## Quick Start

### Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- kubectl configured
- Ingress controller (nginx recommended)

### Install

```bash
# Add priority classes
kubectl apply -f ../../k8s/priority-classes.yaml

# Build dependencies
helm dependency build

# Install for local development
helm install gripday . --create-namespace

# Install for staging
helm install gripday . -f values-staging.yaml --create-namespace

# Install for production
helm install gripday . -f values-production.yaml --create-namespace
```

## Architecture

```
┌─────────────────────────────────────────────┐
│           Ingress (nginx)                   │
└──────────────────┬──────────────────────────┘
                   │
           ┌───────▼────────┐
           │ Gateway Service │
           └───────┬─────────┘
                   │
        ┏━━━━━━━━━━┻━━━━━━━━━━┓
        ▼                      ▼
┌───────────────┐      ┌──────────────────┐
│ User Service  │      │Bookstore Service │
├───────────────┤      ├──────────────────┤
│ PostgreSQL    │      │  PostgreSQL      │
│ Redis         │      │  Redis           │
└───────────────┘      └──────────────────┘
```

## Configuration

### Environment-Specific Deployments

**Local/Development:**
```bash
helm install gripday . 
# Uses: gripday-dev-env namespace
```

**Staging:**
```bash
helm install gripday . -f values-staging.yaml
# Uses: gripday-staging-env namespace
```

**Test:**
```bash
helm install gripday . -f values-test.yaml
# Uses: gripday-test-env namespace
```

**Production:**
```bash
helm install gripday . -f values-production.yaml
# Uses: gripday-production-env namespace
```

### Custom Configuration

Create `custom-values.yaml`:

```yaml
global:
  environment: production
  namespace: gripday-production-env

user-service:
  replicaCount: 5
  autoscaling:
    maxReplicas: 20

gateway-service:
  replicaCount: 10
  autoscaling:
    maxReplicas: 50
```

Install:
```bash
helm install gripday . -f custom-values.yaml
```

### Selective Service Deployment

```bash
# Install only user service
helm install gripday . \
  --set gateway-service.enabled=false \
  --set bookstore-service.enabled=false

# Install without observability
helm install gripday . \
  --set observability.enabled=false
```

## Upgrading

```bash
# Upgrade with new values
helm upgrade gripday . -f values-production.yaml

# Upgrade specific service version
helm upgrade gripday . \
  --set user-service.image.tag=1.1.0 \
  --set gateway-service.image.tag=1.1.0
```

## Uninstalling

```bash
# Uninstall release
helm uninstall gripday

# Clean up namespace
kubectl delete namespace gripday-dev-env
```

## Monitoring

### Health Checks

```bash
# Gateway
kubectl port-forward -n gripday-dev-env svc/gateway-service 8080:8080
curl http://localhost:8080/actuator/health

# User Service
kubectl port-forward -n gripday-dev-env svc/user-service 8081:8080
curl http://localhost:8081/actuator/health
```

### View Logs

```bash
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=gateway-service
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=user-service
kubectl logs -f -n gripday-dev-env -l app.kubernetes.io/name=bookstore-service
```

### View Resources

```bash
kubectl get all -n gripday-dev-env
kubectl get hpa -n gripday-dev-env
kubectl get pvc -n gripday-dev-env
```

## Troubleshooting

### Pods Not Starting

```bash
kubectl describe pod <pod-name> -n gripday-dev-env
kubectl logs <pod-name> -n gripday-dev-env
```

### Service Communication Issues

```bash
# Test DNS
kubectl run test --image=busybox --rm -it -- \
  nslookup user-service.gripday-dev-env.svc.cluster.local
```

### Database Connection Issues

```bash
# Check PostgreSQL
kubectl exec -it <postgres-pod> -n gripday-dev-env -- \
  psql -U gripday_user -d gripday_user_local
```

## Security

- All pods run as non-root
- Read-only root filesystems
- Network policies enabled
- Pod Security Standards enforced
- Secrets management via Kubernetes Secrets

**Production**: Use external secret management (AWS Secrets Manager, HashiCorp Vault, etc.)

## Values Reference

See individual service charts for detailed configuration:
- [User Service](../user-service/README.md)
- [Gateway Service](../gateway-service/README.md)
- [Bookstore Service](../bookstore-service/README.md)

## Support

For issues or questions, see [GAPS-AND-FIXES.md](./GAPS-AND-FIXES.md)
