# Gripday Platform - Helm Charts

This directory contains Helm charts for deploying the Gripday microservices platform on Kubernetes.

## 📁 Directory Structure

```
helm/
├── gripday/        # Umbrella chart (deploys all services)
├── user-service/            # Authentication & Authorization service
├── bookstore-service/       # Bookstore business domain service
├── gateway-service/         # API Gateway (Spring Cloud Gateway)
└── README.md               # This file
```

## 🚀 Quick Start

### Option 1: Deploy Complete Platform (Recommended)

Deploy all services using the umbrella chart:

```bash
cd gripday
helm dependency build
helm install gripday ./gripday --namespace gripday --create-namespace
```

### Option 2: Deploy Individual Services

Deploy services independently:

```bash
# 1. Install priority classes first
kubectl apply -f ../k8s/priority-classes.yaml

# 2. Install User Service
helm install user-service ./user-service --namespace gripday-auth --create-namespace

# 3. Install Bookstore Service
helm install bookstore-service ./bookstore-service --namespace gripday-bookstore --create-namespace

# 4. Install Gateway Service
helm install gateway-service ./gateway-service --namespace gripday-gateway --create-namespace
```

## 📊 Charts Overview

### 1. User Service

**Purpose**: Authentication, authorization, user management, and multi-tenancy

**Components**:

- Spring Boot application (port 8080)
- PostgreSQL database
- Redis cache
- Network policies
- Resource quotas

**Key Features**:

- JWT-based authentication
- OAuth2 integration (Google)
- Multi-tenant support
- Role-based access control

**Installation**:

```bash
helm install user-service ./user-service -n gripday-auth --create-namespace
```

**Configuration**: See [user-service/README.md](./user-service/README.md)

---

### 2. Bookstore Service

**Purpose**: Business domain service for book inventory and management

**Components**:

- Spring Boot application (port 8080)
- PostgreSQL database
- Redis cache
- Horizontal Pod Autoscaler
- Network policies

**Key Features**:

- Book catalog management
- Inventory tracking
- Search capabilities
- Auto-scaling enabled

**Installation**:

```bash
helm install bookstore-service ./bookstore-service -n gripday-bookstore --create-namespace
```

**Configuration**: See [bookstore-service/README.md](./bookstore-service/README.md)

---

### 3. Gateway Service

**Purpose**: API Gateway for routing, rate limiting, and circuit breaking

**Components**:

- Spring Cloud Gateway (port 8080)
- Redis cache
- Horizontal Pod Autoscaler
- Network policies

**Key Features**:

- Request routing
- Rate limiting
- Circuit breaker patterns
- CORS configuration
- JWT validation

**Installation**:

```bash
helm install gateway-service ./gateway-service -n gripday-gateway --create-namespace
```

**Configuration**: See [gateway-service/README.md](./gateway-service/README.md)

---

### 4. Gripday Platform (Umbrella Chart)

**Purpose**: Deploy and manage all services as a unified platform

**Benefits**:

- Single command deployment
- Coordinated upgrades
- Centralized configuration
- Dependency management

**Installation**:

```bash
cd gripday
helm dependency build
helm install gripday . --namespace gripday --create-namespace
```

**Configuration**: See [gripday/README.md](./gripday/README.md)

## 🔧 Prerequisites

- **Kubernetes**: 1.19 or higher
- **Helm**: 3.0 or higher
- **Storage**: PersistentVolume provisioner (for databases)
- **Ingress**: NGINX Ingress Controller (recommended)
- **Resources**: Minimum 8GB RAM, 4 vCPUs for full platform

## 📝 Common Operations

### Install Priority Classes

**Required before first installation:**

```bash
kubectl apply -f ../k8s/priority-classes.yaml
```

### View Installed Releases

```bash
helm list -A
```

### Check Pod Status

```bash
kubectl get pods -n gripday-auth
kubectl get pods -n gripday-bookstore
kubectl get pods -n gripday-gateway
```

### Port Forward for Local Access

```bash
# Gateway (main entry point)
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080

# Auth service
kubectl port-forward -n gripday-auth svc/user-service 8080:8080

# Bookstore service
kubectl port-forward -n gripday-bookstore svc/bookstore-service 8080:8080
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

### Upgrade Services

```bash
# Upgrade individual service
helm upgrade user-service ./user-service -n gripday-auth

# Upgrade entire platform
helm upgrade gripday ./gripday -n gripday
```

### Uninstall Services

```bash
# Uninstall individual service
helm uninstall user-service -n gripday-auth

# Uninstall entire platform
helm uninstall gripday -n gripday

# Clean up namespaces
kubectl delete namespace gripday-auth gripday-bookstore gripday-gateway
```

## 🎯 Environment-Specific Deployments

### Local/Development

```bash
helm install gripday ./gripday \
  --set global.environment=local \
  --namespace dev \
  --create-namespace
```

### Staging

```bash
helm install gripday ./gripday \
  -f gripday/values-staging.yaml \
  --namespace staging \
  --create-namespace
```

### Production

```bash
helm install gripday ./gripday \
  -f gripday/values-production.yaml \
  --namespace production \
  --create-namespace
```

## 🔐 Security Features

All charts include:

- ✅ Pod Security Standards (restricted)
- ✅ Network Policies
- ✅ Non-root containers
- ✅ Read-only root filesystems
- ✅ Resource quotas and limits
- ✅ Service account with minimal permissions
- ✅ Secret management

## 📈 Observability

All services include:

- Prometheus metrics (`/actuator/prometheus`)
- Health checks (`/actuator/health`)
- OpenTelemetry tracing support
- Structured logging

### Access Metrics

```bash
# Port-forward and access metrics
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080
curl http://localhost:8080/actuator/prometheus
```

## 🧪 Testing Charts

### Lint Charts

```bash
helm lint ./user-service
helm lint ./bookstore-service
helm lint ./gateway-service
helm lint ./gripday
```

### Dry Run

```bash
helm install gripday ./gripday --dry-run --debug
```

### Template Output

```bash
helm template gripday ./gripday > output.yaml
```

## 🆘 Troubleshooting

### Chart Installation Fails

```bash
# Check Helm release status
helm status <release-name> -n <namespace>

# View Helm release history
helm history <release-name> -n <namespace>

# Debug installation
helm install <release-name> <chart> --dry-run --debug
```

### Pod Crashes or CrashLoopBackOff

```bash
# Describe pod
kubectl describe pod <pod-name> -n <namespace>

# View logs
kubectl logs <pod-name> -n <namespace>

# View previous container logs
kubectl logs <pod-name> -n <namespace> --previous
```

### Database Connection Issues

```bash
# Test database connectivity
kubectl exec -it <postgres-pod> -n <namespace> -- psql -U gripday_user -d <database>

# Check service DNS
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup auth-postgres.gripday-auth.svc.cluster.local
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

## 🤝 Contributing

When adding or modifying charts:

1. Follow existing chart structure
2. Update values.yaml with sensible defaults
3. Document all parameters in README
4. Test with `helm lint` and `helm install --dry-run`
5. Update this main README if adding new charts

## 📞 Support

For questions or issues:

- **Documentation**: See individual chart READMEs
- **Issues**: Create issue in project repository
- **Team**: Contact Gripday Platform Team

## 📄 License

Proprietary - Gripday Platform Team

---

**Last Updated**: October 2025  
**Platform Version**: 1.0.0
