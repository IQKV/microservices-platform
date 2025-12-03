# IQ Scaffold Platform - Minikube Deployment

Quick local Kubernetes deployment using Minikube for development and testing.

## Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) installed
- [kubectl](https://kubernetes.io/docs/tasks/tools/) installed
- [Docker](https://docs.docker.com/get-docker/) installed
- 4GB+ RAM available for Minikube

## Quick Start

### 1. Start Minikube

```bash
minikube start --memory=4096 --cpus=2
```

### 2. Deploy Platform

**Linux/macOS:**

```bash
./deploy-minikube.sh --build    # Build images and deploy
./deploy-minikube.sh            # Deploy with existing images
```

**Windows (PowerShell):**

```powershell
.\deploy-minikube.ps1 -Build    # Build images and deploy
.\deploy-minikube.ps1           # Deploy with existing images
```

### 3. Access Services

**NodePort (Direct Access):**

- Gateway: `http://$(minikube ip):30080`
- User Service: `http://$(minikube ip):30081`

**Ingress (Production-like):**

```bash
# Enable ingress addon
minikube addons enable ingress

# Add to /etc/hosts (Linux/macOS) or C:\Windows\System32\drivers\etc\hosts (Windows)
$(minikube ip) api.iqscaffold.site user.iqscaffold.site

# Access via domain names
curl http://api.iqscaffold.site/actuator/health
```

## Deployment Options

### Full Platform Deployment

Deploys all services (databases, cache, microservices):

```bash
kubectl apply -f all-in-one.yaml
```

### Infrastructure Only

Deploys only databases and Redis (useful for local service development):

```bash
kubectl apply -f infrastructure-only.yaml
```

Then run services locally with:

```bash
# User Service
cd iqscaffold-user-service
mvn spring-boot:run -Dspring.profiles.active=local

# Gateway Service
cd iqscaffold-gateway-service
mvn spring-boot:run -Dspring.profiles.active=local
```

## Useful Commands

### View Resources

```bash
kubectl get all -n iqscaffold-dev-env
kubectl get pods -n iqscaffold-dev-env
kubectl get services -n iqscaffold-dev-env
```

### View Logs

```bash
kubectl logs -f deployment/gateway-service -n iqscaffold-dev-env
kubectl logs -f deployment/user-service -n iqscaffold-dev-env
```

### Port Forwarding (Alternative to NodePort)

```bash
kubectl port-forward -n iqscaffold-dev-env svc/gateway-service 8080:8080
kubectl port-forward -n iqscaffold-dev-env svc/user-service 8081:8080
```

### Database Access

```bash
# PostgreSQL (User Service)
kubectl port-forward -n iqscaffold-dev-env svc/postgres-user 5432:5432
psql -h localhost -U iqscaffold_user -d iqscaffold_user

# Redis
kubectl port-forward -n iqscaffold-dev-env svc/redis 6379:6379
redis-cli -h localhost
```

### Restart Services

```bash
kubectl rollout restart deployment/gateway-service -n iqscaffold-dev-env
kubectl rollout restart deployment/user-service -n iqscaffold-dev-env
```

### Scale Services

```bash
kubectl scale deployment/gateway-service --replicas=2 -n iqscaffold-dev-env
```

## Cleanup

**Linux/macOS:**

```bash
./cleanup-minikube.sh           # With confirmation
./cleanup-minikube.sh --force   # Without confirmation
```

**Windows (PowerShell):**

```powershell
.\cleanup-minikube.ps1          # With confirmation
.\cleanup-minikube.ps1 -Force   # Without confirmation
```

Or manually:

```bash
kubectl delete namespace iqscaffold-dev-env
```

## Testing the API

### Register a User

```bash
curl -X POST http://$(minikube ip):30080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Login

```bash
curl -X POST http://$(minikube ip):30080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

### Health Checks

```bash
curl http://$(minikube ip):30080/actuator/health
curl http://$(minikube ip):30081/actuator/health
curl http://$(minikube ip):30082/actuator/health
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n iqscaffold-dev-env

# View pod logs
kubectl logs <pod-name> -n iqscaffold-dev-env

# Describe pod for events
kubectl describe pod <pod-name> -n iqscaffold-dev-env
```

### Image Pull Errors

If using locally built images, ensure you're using Minikube's Docker daemon:

```bash
eval $(minikube docker-env)
# Then rebuild images
```

### Out of Resources

```bash
# Increase Minikube resources
minikube stop
minikube delete
minikube start --memory=8192 --cpus=4
```

### Ingress Not Working

```bash
# Verify ingress addon is enabled
minikube addons list | grep ingress

# Enable if needed
minikube addons enable ingress

# Check ingress controller
kubectl get pods -n ingress-nginx
```

## Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                 Minikube Cluster Architecture                 │
│               (iqscaffold-dev-env namespace)                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐       ┌──────────────┐                      │
│  │   Gateway    │       │     User     │                      │
│  │   Service    │       │   Service    │                      │
│  │ (NodePort    │       │ (NodePort    │                      │
│  │    30080)    │       │    30081)    │                      │
│  └──────┬───────┘       └──────┬───────┘                      │
│         │                      │                              │
│         └────────────┬─────────┘                              │
│                      │                                       │
│            ┌─────────▼─────────┐                             │
│            │      Redis        │                             │
│            │    (Port 6379)    │                             │
│            └─────────┬─────────┘                             │
│                      │                                       │
│            ┌─────────▼─────────┐                             │
│            │    PostgreSQL     │                             │
│            │ (User Service DB) │                             │
│            │    (Port 5432)    │                             │
│            └───────────────────┘                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Configuration

### Default Credentials

- **Database User**: `iqscaffold_user`
- **Database Password**: `iqscaffold_password`
- **JWT Secret**: `local-dev-secret-key-change-in-production`

### Resource Limits

- **Gateway Service**: 256Mi-512Mi RAM, 100m-500m CPU
- **User Service**: 256Mi-512Mi RAM, 100m-500m CPU
- **PostgreSQL**: 128Mi-256Mi RAM, 100m-200m CPU
- **Redis**: 64Mi-128Mi RAM, 50m-100m CPU

### Environment Variables

All services use `SPRING_PROFILES_ACTIVE=local` by default.

## Files

- `all-in-one.yaml` - Complete platform deployment
- `infrastructure-only.yaml` - Databases and Redis only
- `ingress.yaml` - Ingress configuration
- `deploy-minikube.sh` - Deployment script (Linux/macOS)
- `deploy-minikube.ps1` - Deployment script (Windows)
- `cleanup-minikube.sh` - Cleanup script (Linux/macOS)
- `cleanup-minikube.ps1` - Cleanup script (Windows)

## Next Steps

After successful deployment:

1. Test the API endpoints
2. View logs to verify services are running
3. Access Swagger UI at service URLs
4. Develop and test your features
5. Use `kubectl port-forward` for debugging
