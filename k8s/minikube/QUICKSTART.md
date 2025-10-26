# Gripday Platform - Minikube Quick Start

⚡ Deploy the complete Gripday microservices platform to minikube in minutes.

## Prerequisites

- **minikube** installed: https://minikube.sigs.k8s.io/docs/start/
- **kubectl** installed: https://kubernetes.io/docs/tasks/tools/
- **Docker** (for building images)

## 🚀 Quick Deploy (3 Steps)

### 1. Start Minikube

```bash
minikube start --cpus=4 --memory=8192
```

### 2. Deploy Platform

**Linux/Mac:**
```bash
cd k8s/minikube
./deploy-minikube.sh
```

**Windows PowerShell:**
```powershell
cd k8s\minikube
.\deploy-minikube.ps1
```

### 3. Access Services

Get the service URLs:
```bash
minikube service gateway-service -n gripday --url
```

Or use direct URLs:
```bash
# Replace <minikube-ip> with your minikube IP
http://<minikube-ip>:30080  # Gateway Service
http://<minikube-ip>:30081  # Auth Service
http://<minikube-ip>:30082  # Bookstore Service
```

## 🧪 Test the API

### Register a User

```bash
GATEWAY_URL=$(minikube service gateway-service -n gripday --url)

curl -X POST $GATEWAY_URL/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Check Health

```bash
curl $GATEWAY_URL/actuator/health
```

## 📊 Monitor Deployment

```bash
# Watch pods start
kubectl get pods -n gripday -w

# View logs
kubectl logs -f deployment/gateway-service -n gripday
kubectl logs -f deployment/auth-service -n gripday

# Check all resources
kubectl get all -n gripday
```

## 🔨 Build & Deploy

To build Docker images locally and deploy:

**Linux/Mac:**
```bash
./deploy-minikube.sh --build
```

**Windows:**
```powershell
.\deploy-minikube.ps1 -Build
```

## 🧹 Cleanup

**Linux/Mac:**
```bash
./cleanup-minikube.sh
```

**Windows:**
```powershell
.\cleanup-minikube.ps1
```

## 🔧 Useful Commands

### Port Forwarding

If NodePort doesn't work, use port forwarding:

```bash
# Gateway Service
kubectl port-forward -n gripday svc/gateway-service 8080:8080

# Auth Service
kubectl port-forward -n gripday svc/auth-service 8081:8081

# Bookstore Service
kubectl port-forward -n gripday svc/bookstore-service 8082:8082
```

### Access Database

```bash
# PostgreSQL Auth DB
kubectl exec -it deployment/postgres-auth -n gripday -- psql -U gripday_user -d gripday_auth

# PostgreSQL Bookstore DB
kubectl exec -it deployment/postgres-bookstore -n gripday -- psql -U gripday_user -d gripday_bookstore

# Redis
kubectl exec -it deployment/redis -n gripday -- redis-cli
```

### Scale Services

```bash
# Scale gateway to 2 replicas
kubectl scale deployment/gateway-service --replicas=2 -n gripday

# Scale auth service to 2 replicas
kubectl scale deployment/auth-service --replicas=2 -n gripday
```

### View Resource Usage

```bash
# Enable metrics server
minikube addons enable metrics-server

# View pod resource usage
kubectl top pods -n gripday

# View node resource usage
kubectl top nodes
```

## 🐛 Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n gripday

# Check events
kubectl get events -n gripday --sort-by='.lastTimestamp'
```

### Service Not Accessible

```bash
# Verify service endpoints
kubectl get endpoints -n gripday

# Check minikube IP
minikube ip

# Try using minikube service command
minikube service gateway-service -n gripday
```

### Image Pull Errors

If images aren't available:

1. Build them locally:
   ```bash
   eval $(minikube docker-env)
   ./deploy-minikube.sh --build
   ```

2. Or pull from a registry (if available):
   ```bash
   docker pull gripday/auth-service:latest
   docker pull gripday/gateway-service:latest
   docker pull gripday/bookstore-service:latest
   ```

### Database Connection Issues

```bash
# Check PostgreSQL
kubectl exec -it deployment/postgres-auth -n gripday -- pg_isready -U gripday_user

# Check Redis
kubectl exec -it deployment/redis -n gripday -- redis-cli ping
```

### Reset Everything

```bash
# Delete namespace and redeploy
./cleanup-minikube.sh
./deploy-minikube.sh
```

## 📚 Full Documentation

For complete documentation, see [README.md](README.md)

## 🎯 What's Deployed?

- **Gateway Service** - API Gateway with routing, rate limiting
- **Auth Service** - Authentication & user management  
- **Bookstore Service** - Example business service
- **PostgreSQL** - Two databases (auth & bookstore)
- **Redis** - Caching and session storage

All services are configured with:
- ✅ Health checks
- ✅ Resource limits
- ✅ Auto-restart on failure
- ✅ Optimized for local development

## 💡 Tips

1. **Increase minikube resources** for better performance:
   ```bash
   minikube start --cpus=6 --memory=12288
   ```

2. **Use dashboard** for visual monitoring:
   ```bash
   minikube dashboard
   ```

3. **Access logs easily**:
   ```bash
   kubectl logs -f deployment/gateway-service -n gripday --tail=50
   ```

4. **Quick restart** after code changes:
   ```bash
   kubectl rollout restart deployment/auth-service -n gripday
   ```

## 🆘 Need Help?

- Check [README.md](README.md) for detailed documentation
- Review [Troubleshooting Guide](../../docs/troubleshooting/common-issues.md)
- See [API Documentation](../../docs/api/complete-api-reference.md)
