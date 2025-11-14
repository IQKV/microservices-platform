# Gripday Platform - Minikube Deployment

Local Kubernetes deployment that mirrors production architecture while maintaining debugging flexibility.

Easy-to-use Kubernetes manifests for deploying the Gripday microservices platform on minikube.

## Overview

This directory contains everything needed to deploy the Gripday microservices platform to minikube for local development and testing.

### 🎯 Architecture

Minikube deployment supports **3 access patterns**:

1. **API Gateway Pattern** (Production-like)
   - Traffic flows: Ingress → Gateway → Backend Services
   - Domain-based routing: `api.gripday.site`
   - Mirrors staging/production architecture

2. **Direct NodePort** (Quick Testing)
   - Direct IP:Port access to any service
   - No DNS setup required
   - Great for rapid development

3. **Direct Service Ingress** (Debugging Only)
   - Domain-based direct access: `auth.gripday.site`
   - Bypasses gateway for debugging
   - **Does NOT exist in production!**

> **Note**: Production/staging only use method #1 (API Gateway). Methods #2 and #3 are minikube-only for development convenience.

### Prerequisites

1. **Install minikube**: https://minikube.sigs.k8s.io/docs/start/
2. **Install kubectl**: https://kubernetes.io/docs/tasks/tools/
3. **Docker**: For building images (or use pre-built images)

### 1. Start Minikube

```bash
# Start minikube with sufficient resources
minikube start --cpus=4 --memory=8192 --driver=docker

# Verify minikube is running
minikube status
```

### 2. Build Docker Images (Optional)

If you want to build images locally:

```bash
# Point your shell to minikube's docker daemon
eval $(minikube docker-env)

# Build all service images (from project root)
cd ../..
docker build -t gripday/user-service:latest -f gripday-user-service/Dockerfile .
docker build -t gripday/gateway-service:latest -f gripday-gateway-service/Dockerfile .
docker build -t gripday/bookstore-service:latest -f gripday-bookstore-service/Dockerfile .
```

### 3. Deploy to Minikube

```bash
# Navigate to minikube directory
cd k8s/minikube

# Deploy everything with one command
./deploy-minikube.sh

# Or deploy manually
kubectl apply -f all-in-one.yaml
```

### 4. Access Services

```bash
# Get service URLs
minikube service gateway-service -n gripday --url
minikube service user-service -n gripday --url
minikube service bookstore-service -n gripday --url

# Or use port forwarding
kubectl port-forward -n gripday svc/gateway-service 8080:8080
kubectl port-forward -n gripday svc/user-service 8080:8080
kubectl port-forward -n gripday svc/bookstore-service 8080:8080
```

### 5. Test the Platform

```bash
# Get gateway service URL
GATEWAY_URL=$(minikube service gateway-service -n gripday --url)

# Register a user
curl -X POST $GATEWAY_URL/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login (after email verification)
curl -X POST $GATEWAY_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Minikube Cluster                   │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │         Namespace: gripday                   │   │
│  │                                               │   │
│  │  ┌────────────────┐  ┌──────────────────┐  │   │
│  │  │ Gateway Service│  │  User Service    │  │   │
│  │  │  NodePort      │  │  NodePort        │  │   │
│  │  │  30080         │  │  30081           │  │   │
│  │  └────────┬───────┘  └─────────┬────────┘  │   │
│  │           │                     │            │   │
│  │  ┌────────┴───────────┐  ┌─────┴────────┐  │   │
│  │  │ Bookstore Service  │  │    Redis     │  │   │
│  │  │  NodePort 30082    │  │              │  │   │
│  │  └────────────────────┘  └──────────────┘  │   │
│  │                                               │   │
│  │  ┌──────────────┐  ┌──────────────┐        │   │
│  │  │  Postgres    │  │  Postgres    │        │   │
│  │  │  Auth DB     │  │  Bookstore   │        │   │
│  │  └──────────────┘  └──────────────┘        │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Components

### Services

- **Gateway Service** - Port 30080 (NodePort)
- **User Service** - Port 30081 (NodePort)
- **Bookstore Service** - Port 30082 (NodePort)

### Databases

- **PostgreSQL User** - Port 5432 (ClusterIP)
- **PostgreSQL Bookstore** - Port 5432 (ClusterIP)
- **Redis** - Port 6379 (ClusterIP)

## Resource Configuration

Optimized for local development with minimal resource usage:

| Service    | Replicas | CPU Request | Memory Request |
| ---------- | -------- | ----------- | -------------- |
| Gateway    | 1        | 100m        | 256Mi          |
| Auth       | 1        | 100m        | 256Mi          |
| Bookstore  | 1        | 100m        | 256Mi          |
| PostgreSQL | 1        | 100m        | 128Mi          |
| Redis      | 1        | 50m         | 64Mi           |

## Useful Commands

### View All Resources

```bash
kubectl get all -n gripday
```

### View Logs

```bash
# Gateway service logs
kubectl logs -f deployment/gateway-service -n gripday

# User service logs
kubectl logs -f deployment/user-service -n gripday

# Bookstore service logs
kubectl logs -f deployment/bookstore-service -n gripday
```

### Check Pod Status

```bash
kubectl get pods -n gripday -w
```

### Access Pod Shell

```bash
# Access auth service pod
kubectl exec -it deployment/user-service -n gripday -- sh

# Access PostgreSQL
kubectl exec -it deployment/postgres-user -n gripday -- psql -U gripday_user -d gripday_user
```

### Scale Services

```bash
# Scale gateway service
kubectl scale deployment/gateway-service --replicas=2 -n gripday

# Scale auth service
kubectl scale deployment/user-service --replicas=2 -n gripday
```

### Delete Everything

```bash
# Delete all resources
kubectl delete namespace gripday

# Or use the script
./cleanup-minikube.sh
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n gripday

# Check events
kubectl get events -n gripday --sort-by='.lastTimestamp'
```

### Database Connection Issues

```bash
# Check if PostgreSQL is ready
kubectl exec -it deployment/postgres-user -n gripday -- pg_isready -U gripday_user

# Check Redis
kubectl exec -it deployment/redis -n gripday -- redis-cli ping
```

### Image Pull Issues

```bash
# If using local images, ensure you're using minikube's docker daemon
eval $(minikube docker-env)

# Set image pull policy to Never or IfNotPresent
# Already configured in manifests
```

### Service Not Accessible

```bash
# Check service endpoints
kubectl get endpoints -n gripday

# Check service details
kubectl describe service gateway-service -n gripday

# Try port forwarding instead of NodePort
kubectl port-forward -n gripday svc/gateway-service 8080:8080
```

### Check Application Health

```bash
# Port forward and test health endpoints
kubectl port-forward -n gripday svc/user-service 8080:8080 &
curl http://localhost:8080/actuator/health

kubectl port-forward -n gripday svc/gateway-service 8080:8080 &
curl http://localhost:8080/actuator/health
```

## Configuration

### Environment Variables

All sensitive configuration is stored in Kubernetes secrets. Default values are suitable for local development.

To modify:

```bash
# Edit secrets
kubectl edit secret gripday-secrets -n gripday

# Or delete and recreate
kubectl delete secret gripday-secrets -n gripday
kubectl create secret generic gripday-secrets -n gripday \
  --from-literal=JWT_SECRET="your-secret-key" \
  --from-literal=DATABASE_PASSWORD="your-password"
```

### ConfigMaps

Application configuration is stored in ConfigMaps:

```bash
# Edit configuration
kubectl edit configmap gripday-config -n gripday

# Restart pods to apply changes
kubectl rollout restart deployment/user-service -n gripday
kubectl rollout restart deployment/gateway-service -n gripday
kubectl rollout restart deployment/bookstore-service -n gripday
```

## Development Workflow

### 1. Make Code Changes

Edit your application code in the respective service directories.

### 2. Rebuild Docker Images

```bash
eval $(minikube docker-env)
docker build -t gripday/user-service:latest -f gripday-user-service/Dockerfile .
```

### 3. Restart Deployments

```bash
kubectl rollout restart deployment/user-service -n gripday
```

### 4. Watch Logs

```bash
kubectl logs -f deployment/user-service -n gripday
```

## Performance Tips

### Enable Addons

```bash
# Enable metrics server
minikube addons enable metrics-server

# View resource usage
kubectl top nodes
kubectl top pods -n gripday
```

### Increase Resources

```bash
# Stop minikube
minikube stop

# Start with more resources
minikube start --cpus=6 --memory=12288
```

## Next Steps

- Explore [Production Deployment](../README.md) for staging/production setup
- Check [API Documentation](../../docs/api/complete-api-reference.md)
- Review [Service Documentation](../../README.md)

## Support

For issues and questions:

- Check [Troubleshooting Guide](../../docs/troubleshooting/common-issues.md)
- Review service-specific READMEs in each service directory
