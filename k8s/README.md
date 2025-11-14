# Gripday Platform - Kubernetes Deployments

Complete Kubernetes deployment configurations for all environments.

## 📁 Directory Structure

```
k8s/
├── minikube/              # Local development on minikube ⭐ NEW
│   ├── README.md         # Complete minikube documentation
│   ├── QUICKSTART.md     # 3-step quick start guide
│   ├── all-in-one.yaml   # Deploy entire platform
│   └── deploy-*.sh/ps1   # Automated deployment scripts
├── user-service/          # Auth service production manifests
├── gateway-service/       # Gateway service production manifests
├── bookstore-service/     # Bookstore service production manifests
└── scripts/              # Deployment automation scripts
```

## 🎯 Choose Your Deployment

### Minikube (Local Development) ⭐ RECOMMENDED FOR GETTING STARTED

**Purpose:** Local development and testing on minikube  
**Audience:** Developers, local testing, learning  
**Setup Time:** < 5 minutes  
**Resources:** Minimal (1 CPU, 1GB RAM)

```bash
cd minikube
./deploy-minikube.sh
```

**Features:**

- ✅ Single-command deployment
- ✅ Optimized for laptop/desktop
- ✅ All-in-one manifest
- ✅ NodePort access (no ingress needed)
- ✅ Automated scripts (Bash & PowerShell)
- ✅ Quick teardown and rebuild

**Read More:** [minikube/README.md](minikube/README.md)

### Production Deployment

**Purpose:** Production-ready deployment with high availability  
**Audience:** DevOps, Production environments  
**Setup Time:** 15-30 minutes  
**Resources:** High (multi-node cluster)

```bash
# Deploy specific service
cd user-service
kubectl apply -f .

# Or use deployment scripts
cd ..
./deploy-production.sh
```

**Features:**

- ✅ High availability (3 replicas)
- ✅ Ingress with TLS
- ✅ Persistent storage
- ✅ Network policies
- ✅ Auto-scaling (HPA)
- ✅ Pod disruption budgets
- ✅ Security hardening

**Read More:**

- [user-service/README.md](user-service/README.md)
- [gateway-service/README.md](gateway-service/README.md)
- [bookstore-service/README.md](bookstore-service/README.md)

### Staging Deployment

**Purpose:** Pre-production testing environment  
**Audience:** QA, Integration testing  
**Setup Time:** 15-20 minutes

```bash
./deploy-staging.sh
```

**Features:**

- ✅ Production-like configuration
- ✅ Let's Encrypt staging certificates
- ✅ 2 replicas for redundancy
- ✅ Separate namespace

## 🚀 Quick Start

### For Local Development (Minikube)

1. **Start minikube:**

   ```bash
   minikube start --cpus=4 --memory=8192
   ```

2. **Deploy platform:**

   ```bash
   cd minikube
   ./deploy-minikube.sh
   ```

3. **Access services:**
   ```bash
   minikube service gateway-service -n gripday --url
   ```

**Complete guide:** [minikube/QUICKSTART.md](minikube/QUICKSTART.md)

### For Production

1. **Review prerequisites:**
   - Kubernetes cluster (1.25+)
   - kubectl configured
   - Ingress controller installed
   - Storage class available
   - Cert-manager (for TLS)

2. **Deploy services:**

   ```bash
   ./deploy-production.sh
   ```

3. **Verify deployment:**
   ```bash
   ./health-check.sh
   ```

**Complete guide:** [user-service/README.md](user-service/README.md)

## 📊 Comparison

| Feature         | Minikube     | Staging        | Production      |
| --------------- | ------------ | -------------- | --------------- |
| **Environment** | Local laptop | Cloud/Server   | Cloud cluster   |
| **Replicas**    | 1            | 2              | 3+              |
| **Storage**     | emptyDir     | PVC            | PVC + Backups   |
| **Access**      | NodePort     | Ingress (HTTP) | Ingress (HTTPS) |
| **Resources**   | Low          | Medium         | High            |
| **Security**    | Basic        | Medium         | Hardened        |
| **Monitoring**  | Basic        | Full           | Full + Alerts   |
| **Setup Time**  | < 5 min      | 15 min         | 30 min          |
| **Use Case**    | Development  | Testing        | Production      |

**Detailed comparison:** [minikube/COMPARISON.md](minikube/COMPARISON.md)

## 📖 Documentation

### Getting Started

- [Minikube Quick Start](minikube/QUICKSTART.md) - Start here!
- [Minikube Complete Guide](minikube/README.md)
- [Minikube vs Production](minikube/COMPARISON.md)

### Service Documentation

- [User Service Deployment](user-service/README.md)
- [Gateway Service Deployment](gateway-service/README.md)
- [Bookstore Service Deployment](bookstore-service/README.md)

### Scripts and Automation

- [Deployment Scripts](scripts/README.md)
- [Health Check Script](health-check.sh)
- [Scaling Script](scale-services.sh)

## 🛠️ Available Scripts

### Deployment Scripts

```bash
# Deploy to different environments
./deploy-local.sh          # Deploy to local cluster
./deploy-staging.sh        # Deploy to staging
./deploy-production.sh     # Deploy to production

# Or use minikube scripts
cd minikube
./deploy-minikube.sh       # Minikube deployment (Bash)
.\deploy-minikube.ps1      # Minikube deployment (PowerShell)
```

### Management Scripts

```bash
./health-check.sh          # Check all services health
./scale-services.sh        # Scale services
./rollback.sh              # Rollback deployment
./setup-namespace.sh       # Create namespaces

# Cleanup
cd minikube
./cleanup-minikube.sh      # Clean minikube deployment
```

### Configuration Scripts

```bash
./apply-configs.sh         # Apply ConfigMaps and Secrets
```

## 🔧 Common Tasks

### Deploy Everything (Minikube)

```bash
cd k8s/minikube
./deploy-minikube.sh
```

### Deploy Everything (Production)

```bash
cd k8s
./deploy-production.sh
```

### Check Health

```bash
./health-check.sh
```

### View Logs

```bash
# Minikube
kubectl logs -f deployment/gateway-service -n gripday

# Production
kubectl logs -f deployment/gateway-service -n gripday-gateway-production
```

### Scale Services

```bash
./scale-services.sh gateway-service 5
```

### Clean Up (Minikube)

```bash
cd minikube
./cleanup-minikube.sh
```

## 🏗️ Architecture

### Minikube Architecture

```
┌─────────────────────────────────┐
│      Single Namespace: gripday  │
│                                  │
│  ┌──────────┐  ┌──────────┐    │
│  │ Gateway  │  │   Auth   │    │
│  │ :30080   │  │  :30081  │    │
│  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐    │
│  │Bookstore │  │  Redis   │    │
│  │ :30082   │  │          │    │
│  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐    │
│  │Postgres  │  │Postgres  │    │
│  │  Auth    │  │Bookstore │    │
│  └──────────┘  └──────────┘    │
└─────────────────────────────────┘
```

### Production Architecture

```
┌─────────────────────────────────────────┐
│    Ingress Controller (TLS)             │
└────────────┬────────────────────────────┘
             │
   ┌─────────┼────────────┐
   │         │            │
┌──▼──────┐ ┌▼─────────┐ ┌▼──────────┐
│Gateway  │ │Auth      │ │Bookstore  │
│NS       │ │NS        │ │NS         │
│         │ │          │ │           │
│3 pods   │ │3 pods    │ │2 pods     │
└─────────┘ └──────────┘ └───────────┘
   │            │              │
   ▼            ▼              ▼
[Redis]    [PostgreSQL]  [PostgreSQL]
 Cluster    + Replicas    + Replicas
```

## 💡 Best Practices

### For Local Development (Minikube)

1. Use the provided automation scripts
2. Keep resources minimal (1 replica)
3. Use NodePort for easy access
4. Don't worry about persistence
5. Quick iteration: deploy → test → teardown → repeat

### For Production

1. Use separate namespaces per service
2. Enable all security features
3. Configure persistent storage
4. Set up monitoring and alerting
5. Use Ingress with TLS
6. Configure auto-scaling
7. Implement network policies
8. Regular backups

## 🔐 Security

### Minikube (Development)

- Basic security context
- Default credentials (CHANGE for any external access)
- No network policies (all pods can communicate)
- HTTP only (no TLS)

### Production

- Pod security policies enforced
- Secrets from vault/sealed-secrets
- Network policies (least privilege)
- TLS everywhere
- Read-only root filesystem
- Non-root users
- Capability dropping

## 📦 Resource Requirements

### Minikube Cluster

- **CPU:** 4 cores recommended
- **Memory:** 8GB recommended
- **Disk:** 20GB
- **Platform Usage:** ~0.5 CPU, ~1GB RAM

### Production Cluster (per environment)

- **Nodes:** 3+ worker nodes
- **CPU:** 8+ cores per node
- **Memory:** 16+ GB per node
- **Disk:** 100+ GB per node
- **Platform Usage:** ~2-4 CPUs, ~4-8GB RAM

## 🆘 Troubleshooting

### Minikube Issues

See [minikube/README.md#troubleshooting](minikube/README.md#troubleshooting)

### Production Issues

See service-specific READMEs:

- [User Service Troubleshooting](user-service/README.md#troubleshooting)
- [Gateway Service Troubleshooting](gateway-service/README.md#troubleshooting)

### Common Issues

- **Pods not starting:** Check resources, events, and logs
- **Can't access services:** Verify service type and ports
- **Database connection errors:** Check credentials and network
- **Image pull errors:** Verify image availability

## 📚 Additional Resources

- [Main Project README](../README.md)
- [API Documentation](../docs/api/complete-api-reference.md)
- [Configuration Guide](../docs/configuration/environment-variables.md)
- [Local Development Guide](../docs/deployment/local-development.md)
- [Troubleshooting Guide](../docs/troubleshooting/common-issues.md)

## 🚀 Next Steps

### New Users

1. Start with [minikube/QUICKSTART.md](minikube/QUICKSTART.md)
2. Deploy to minikube
3. Test the API
4. Review [COMPARISON.md](minikube/COMPARISON.md)

### Production Deployment

1. Review [user-service/README.md](user-service/README.md)
2. Set up infrastructure (cluster, storage, ingress)
3. Configure secrets
4. Run `./deploy-production.sh`
5. Verify with `./health-check.sh`

## 🤝 Contributing

When adding new services or modifying deployments:

1. Update minikube manifests for local development
2. Update production manifests with proper security
3. Add documentation
4. Test both environments
5. Update this README

## 📝 Notes

- **Minikube manifests** are optimized for ease of use and quick iteration
- **Production manifests** are optimized for reliability and security
- All scripts support both Linux/Mac (Bash) and Windows (PowerShell)
- Default credentials are for development only - ALWAYS change for production

---

**Need Help?** Start with [minikube/QUICKSTART.md](minikube/QUICKSTART.md) or check service-specific READMEs.
