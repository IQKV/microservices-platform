# Gripday Platform - Minikube Deployment Files Index

Complete reference for all files in the minikube deployment directory.

## 📁 File Structure

```
k8s/minikube/
├── README.md                    # Complete documentation
├── QUICKSTART.md               # Get started in 3 steps
├── COMPARISON.md               # Minikube vs Production comparison
├── INDEX.md                    # This file
├── all-in-one.yaml            # Complete platform deployment
├── infrastructure-only.yaml    # Just databases and Redis
├── deploy-minikube.sh         # Bash deployment script
├── deploy-minikube.ps1        # PowerShell deployment script
├── cleanup-minikube.sh        # Bash cleanup script
├── cleanup-minikube.ps1       # PowerShell cleanup script
└── .gitignore                 # Git ignore rules
```

## 📄 File Descriptions

### Documentation Files

#### `README.md`

**Purpose:** Comprehensive documentation for minikube deployment  
**Contains:**

- Detailed setup instructions
- Architecture diagrams
- Configuration reference
- Troubleshooting guide
- Development workflows

**Use when:** You need complete information about the minikube setup

#### `QUICKSTART.md`

**Purpose:** Fast-track guide to get running in minutes  
**Contains:**

- 3-step deployment process
- Common commands
- Quick troubleshooting
- Essential operations

**Use when:** You want to deploy quickly without reading everything

#### `COMPARISON.md`

**Purpose:** Explains differences between minikube and production manifests  
**Contains:**

- Feature-by-feature comparison
- Resource usage differences
- When to use each approach
- Migration guidance

**Use when:** You need to understand why minikube setup differs from production

#### `INDEX.md`

**Purpose:** This file - quick reference to all files  
**Contains:**

- File structure overview
- File descriptions
- Quick usage guide

**Use when:** You need to know what each file does

### Kubernetes Manifests

#### `all-in-one.yaml`

**Purpose:** Single file to deploy the entire platform  
**Contains:**

- Namespace
- ConfigMaps
- Secrets
- All deployments (Auth, Gateway, Bookstore, PostgreSQL, Redis)
- All services
- Init containers with dependencies

**Use when:** You want to deploy everything at once

**Deploy with:**

```bash
kubectl apply -f all-in-one.yaml
```

**What gets deployed:**

- ✅ 2 PostgreSQL databases (auth + bookstore)
- ✅ 1 Redis instance
- ✅ 3 microservices (auth, gateway, bookstore)
- ✅ NodePort services for external access
- ✅ Health checks and resource limits

#### `infrastructure-only.yaml`

**Purpose:** Deploy just the databases and Redis  
**Contains:**

- PostgreSQL for Auth (NodePort 30432)
- PostgreSQL for Bookstore (NodePort 30433)
- Redis (NodePort 30379)

**Use when:**

- Running services locally but need databases in K8s
- Testing database migrations
- Developing without full platform deployment

**Deploy with:**

```bash
kubectl apply -f infrastructure-only.yaml
```

**Access infrastructure:**

```bash
# PostgreSQL Auth
psql -h $(minikube ip) -p 30432 -U gripday_user -d gripday_auth

# PostgreSQL Bookstore
psql -h $(minikube ip) -p 30433 -U gripday_user -d gripday_bookstore

# Redis
redis-cli -h $(minikube ip) -p 30379
```

### Deployment Scripts

#### `deploy-minikube.sh` (Linux/Mac)

**Purpose:** Automated deployment script with options  
**Features:**

- Prerequisite checking
- Optional image building
- Wait for pod readiness
- Health check verification
- Service URL display

**Usage:**

```bash
# Basic deployment
./deploy-minikube.sh

# Build images and deploy
./deploy-minikube.sh --build

# Deploy without waiting
./deploy-minikube.sh --no-wait

# Build, deploy, and open in browser
./deploy-minikube.sh --build --open
```

**Flags:**

- `-b, --build` - Build Docker images first
- `-n, --no-wait` - Don't wait for pods to be ready
- `-o, --open` - Open services in browser
- `-h, --help` - Show help

#### `deploy-minikube.ps1` (Windows)

**Purpose:** PowerShell version of deployment script  
**Features:** Same as bash version

**Usage:**

```powershell
# Basic deployment
.\deploy-minikube.ps1

# Build images and deploy
.\deploy-minikube.ps1 -Build

# Deploy without waiting
.\deploy-minikube.ps1 -NoWait

# Build, deploy, and open in browser
.\deploy-minikube.ps1 -Build -Open
```

### Cleanup Scripts

#### `cleanup-minikube.sh` (Linux/Mac)

**Purpose:** Remove all deployed resources  
**Features:**

- Confirmation prompt
- Namespace deletion
- Wait for complete cleanup

**Usage:**

```bash
# Cleanup with confirmation
./cleanup-minikube.sh

# Force cleanup without confirmation
./cleanup-minikube.sh --force
```

#### `cleanup-minikube.ps1` (Windows)

**Purpose:** PowerShell cleanup script  
**Features:** Same as bash version

**Usage:**

```powershell
# Cleanup with confirmation
.\cleanup-minikube.ps1

# Force cleanup
.\cleanup-minikube.ps1 -Force
```

### Configuration Files

#### `.gitignore`

**Purpose:** Prevent committing temporary and local files  
**Ignores:**

- Log files
- Temporary files
- Local configuration overrides
- Backup files

## 🚀 Quick Usage Guide

### First Time Setup

1. **Read this:** `QUICKSTART.md`
2. **Run this:** `./deploy-minikube.sh` or `.\deploy-minikube.ps1`
3. **Access services** using URLs displayed after deployment

### Common Scenarios

#### Scenario 1: Quick Deploy Everything

```bash
./deploy-minikube.sh
```

#### Scenario 2: Build Images and Deploy

```bash
./deploy-minikube.sh --build
```

#### Scenario 3: Deploy Infrastructure Only

```bash
kubectl apply -f infrastructure-only.yaml
```

#### Scenario 4: Clean Up and Redeploy

```bash
./cleanup-minikube.sh --force
./deploy-minikube.sh
```

#### Scenario 5: Deploy and Monitor

```bash
./deploy-minikube.sh
kubectl get pods -n gripday -w
```

## 📊 Resource Summary

### What Gets Deployed (all-in-one.yaml)

| Component            | Replicas | CPU Request | Memory Request | Port  |
| -------------------- | -------- | ----------- | -------------- | ----- |
| Gateway Service      | 1        | 100m        | 256Mi          | 30080 |
| Auth Service         | 1        | 100m        | 256Mi          | 30081 |
| Bookstore Service    | 1        | 100m        | 256Mi          | 30082 |
| PostgreSQL Auth      | 1        | 100m        | 128Mi          | 5432  |
| PostgreSQL Bookstore | 1        | 100m        | 128Mi          | 5432  |
| Redis                | 1        | 50m         | 64Mi           | 6379  |

**Total Resources:**

- CPU: 550m (0.55 cores)
- Memory: 960Mi (~1GB)

### What Gets Deployed (infrastructure-only.yaml)

| Component            | CPU Request | Memory Request | NodePort |
| -------------------- | ----------- | -------------- | -------- |
| PostgreSQL Auth      | 100m        | 128Mi          | 30432    |
| PostgreSQL Bookstore | 100m        | 128Mi          | 30433    |
| Redis                | 50m         | 64Mi           | 30379    |

**Total Resources:**

- CPU: 250m (0.25 cores)
- Memory: 320Mi

## 🎯 Recommended Reading Order

1. **New to project?** Start with `QUICKSTART.md`
2. **Want details?** Read `README.md`
3. **Comparing options?** Check `COMPARISON.md`
4. **Looking for a file?** Use this `INDEX.md`

## 🔗 Related Documentation

- **Main Project:** [../../README.md](../../README.md)
- **Auth Service:** [../../gripday-auth-service/README.md](../../gripday-auth-service/README.md)
- **Gateway Service:** [../../gripday-gateway-service/README.md](../../gripday-gateway-service/README.md)
- **Bookstore Service:** [../../gripday-bookstore-service/README.md](../../gripday-bookstore-service/README.md)
- **Production K8s:** [../auth-service/README.md](../auth-service/README.md)

## 💡 Tips

### For Bash Users (Linux/Mac)

- Make scripts executable: `chmod +x *.sh`
- Use tab completion for kubectl commands
- Add alias: `alias k=kubectl`

### For PowerShell Users (Windows)

- Enable script execution: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
- Use PowerShell 7+ for best experience
- Add alias: `Set-Alias -Name k -Value kubectl`

### General Tips

- Always check minikube status first: `minikube status`
- Use `kubectl get all -n gripday` to see everything
- Monitor logs with: `kubectl logs -f deployment/<name> -n gripday`
- Port forward if NodePort doesn't work: `kubectl port-forward -n gripday svc/<service> <port>:<port>`

## 🆘 Getting Help

### Quick Help

- Run scripts with `--help` or `-Help` flag
- Check `QUICKSTART.md` for common issues
- Review `README.md` troubleshooting section

### Detailed Help

- Read `COMPARISON.md` to understand design choices
- Check project docs in `../../docs/`
- Review service-specific READMEs

## 📝 Notes

- All scripts are designed to be idempotent (safe to run multiple times)
- Default credentials are provided for local development only
- Storage is ephemeral (data lost when pods restart)
- Single replicas used to minimize resource usage
- NodePort services for easy external access (30080, 30081, 30082)

## ✅ Checklist for First Deployment

- [ ] Minikube installed and running
- [ ] kubectl installed and configured
- [ ] Read QUICKSTART.md
- [ ] Run deployment script
- [ ] Verify pods are running: `kubectl get pods -n gripday`
- [ ] Access gateway service: `http://$(minikube ip):30080`
- [ ] Test API endpoints
- [ ] Review logs if any issues

---

**Last Updated:** Created for Gripday Platform minikube deployment  
**Maintained by:** Platform Team  
**Questions?** Check README.md or project documentation
