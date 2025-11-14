# Helm Charts Migration Guide

## Overview

This document provides guidance on migrating from raw Kubernetes manifests to Helm charts for the Gripday Platform.

## What Was Created

### Individual Service Charts

1. **user-service/** - Complete Helm chart for Authentication Service
   - Deployment, Service, Ingress
   - PostgreSQL database
   - Redis cache
   - Network policies, resource quotas
   - Configurable via values.yaml

2. **bookstore-service/** - Complete Helm chart for Bookstore Service
   - Deployment with HPA enabled
   - PostgreSQL database
   - Redis cache
   - Network policies, resource quotas
   - Configurable via values.yaml

3. **gateway-service/** - Complete Helm chart for API Gateway
   - Deployment with HPA enabled
   - Redis cache
   - Ingress configuration
   - Network policies, resource quotas
   - Configurable via values.yaml

4. **gripday/** - Umbrella chart for complete platform
   - Manages all three services
   - Centralized configuration
   - Dependency management

## Migration Path

### From K8s Manifests to Helm

#### Current State (k8s/)

```
k8s/
├── user-service/
│   ├── namespace.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   └── ...
├── bookstore-service/
│   └── ...
└── gateway-service/
    └── ...
```

#### New State (helm/)

```
helm/
├── user-service/
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── templates/
│   └── README.md
├── bookstore-service/
│   └── ...
├── gateway-service/
│   └── ...
└── gripday/
    └── ...
```

## Key Differences

### 1. Parameterization

**Before (K8s manifest)**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: gripday-dev-env
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: user-service
          image: gripday/user-service:1.0.0
```

**After (Helm template)**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: { { include "user-service.fullname" . } }
  namespace: { { .Values.namespace.name } }
spec:
  replicas: { { .Values.replicaCount } }
  template:
    spec:
      containers:
        - name: user-service
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
```

### 2. Environment-Specific Values

**Helm allows easy environment switching**:

```bash
# Local
helm install user-service ./user-service

# Staging
helm install user-service ./user-service -f values-staging.yaml

# Production
helm install user-service ./user-service -f values-production.yaml
```

### 3. Dependency Management

**Umbrella chart manages dependencies**:

```yaml
# gripday/Chart.yaml
dependencies:
  - name: user-service
    version: "1.0.0"
    repository: "file://../user-service"
  - name: bookstore-service
    version: "1.0.0"
    repository: "file://../bookstore-service"
  - name: gateway-service
    version: "1.0.0"
    repository: "file://../gateway-service"
```

## Migration Steps

### Step 1: Install Priority Classes

```bash
kubectl apply -f k8s/priority-classes.yaml
```

### Step 2: Choose Migration Approach

#### Option A: Parallel Deployment (Recommended for Testing)

Keep existing k8s deployments, deploy Helm charts to different namespaces:

```bash
# Deploy Helm charts with different namespace
helm install gripday-helm ./helm/gripday \
  --set user-service.namespace.name=gripday-user-helm \
  --set bookstore-service.namespace.name=gripday-bookstore-helm \
  --set gateway-service.namespace.name=gripday-gateway-helm
```

Test and validate, then migrate traffic.

#### Option B: In-Place Upgrade (For Production)

1. **Backup current state**:

```bash
kubectl get all -n gripday-user -o yaml > backup-auth.yaml
kubectl get all -n gripday-bookstore -o yaml > backup-bookstore.yaml
kubectl get all -n gripday-gateway -o yaml > backup-gateway.yaml
```

2. **Delete existing resources**:

```bash
kubectl delete -f k8s/user-service/
kubectl delete -f k8s/bookstore-service/
kubectl delete -f k8s/gateway-service/
```

3. **Install Helm charts**:

```bash
helm install gripday ./helm/gripday
```

### Step 3: Verify Migration

```bash
# Check pod status
kubectl get pods -A | grep gripday

# Check services
kubectl get svc -A | grep gripday

# Check ingress
kubectl get ingress -A

# Test health endpoints
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080
curl http://localhost:8080/actuator/health
```

### Step 4: Update CI/CD Pipelines

**Before**:

```bash
kubectl apply -f k8s/user-service/
```

**After**:

```bash
helm upgrade --install user-service ./helm/user-service \
  --namespace gripday-user \
  --create-namespace
```

## Benefits of Helm Charts

### 1. **Reusability**

- Single chart for multiple environments
- Parameterized configurations
- Template functions for common patterns

### 2. **Version Management**

- Chart versioning
- Rollback capability
- Release history

### 3. **Dependency Management**

- Umbrella charts
- Sub-chart dependencies
- Coordinated deployments

### 4. **Simplified Operations**

```bash
# Single command deployment
helm install gripday ./helm/gripday

# Easy upgrades
helm upgrade gripday ./helm/gripday

# Simple rollbacks
helm rollback gripday
```

### 5. **Configuration Management**

- Values files for environments
- Override mechanisms
- Secret management integration

## Comparison: K8s vs Helm

| Feature      | K8s Manifests          | Helm Charts                 |
| ------------ | ---------------------- | --------------------------- |
| Deployment   | `kubectl apply`        | `helm install`              |
| Updates      | Manual file edits      | Values override             |
| Rollback     | Manual                 | `helm rollback`             |
| Templating   | Kustomize/manual       | Built-in Go templates       |
| Packaging    | Multiple files         | Single archive              |
| Versioning   | Git only               | Chart version + app version |
| Dependencies | Manual ordering        | Declared dependencies       |
| Environments | Multiple manifest sets | Single chart + values files |

## Best Practices

### 1. Values Organization

```yaml
# values.yaml (defaults)
replicaCount: 2
image:
  repository: gripday/user-service
  tag: "1.0.0"

# values-production.yaml (overrides)
replicaCount: 5
resources:
  limits:
    memory: "2Gi"
```

### 2. Secret Management

**Do not commit secrets to values.yaml**:

```bash
# Use --set for secrets
helm install user-service ./user-service \
  --set secrets.data.GRIPDAY_DATABASE_PASSWORD=$(base64 <<< "real-password")

# Or use external secret management
# - Sealed Secrets
# - External Secrets Operator
# - Vault
```

### 3. Chart Versioning

Follow semantic versioning:

- **Major**: Breaking changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes

### 4. Testing

```bash
# Lint chart
helm lint ./helm/user-service

# Dry run
helm install test ./helm/user-service --dry-run --debug

# Template output
helm template test ./helm/user-service > output.yaml
```

## Troubleshooting

### Issue: Helm release already exists

```bash
# Check existing releases
helm list -A

# Delete old release
helm uninstall <release-name> -n <namespace>
```

### Issue: Values not being applied

```bash
# Debug with --dry-run
helm install test ./helm/user-service --dry-run --debug

# Check rendered templates
helm template test ./helm/user-service
```

### Issue: Chart dependencies not found

```bash
cd helm/gripday
helm dependency build
helm dependency update
```

## Maintenance

### Updating Charts

1. **Modify templates or values**
2. **Increment chart version** in Chart.yaml
3. **Update README** if needed
4. **Test changes**:
   ```bash
   helm lint ./helm/user-service
   helm install test ./helm/user-service --dry-run
   ```
5. **Upgrade deployment**:
   ```bash
   helm upgrade user-service ./helm/user-service
   ```

### Adding New Services

1. Create new chart directory
2. Copy structure from existing chart
3. Customize templates and values
4. Add to umbrella chart dependencies
5. Update documentation

## Support

For questions or issues with Helm migration:

- Review individual chart READMEs
- Check Helm documentation: https://helm.sh/docs/
- Contact: Gripday Platform Team

---

**Document Version**: 1.0  
**Last Updated**: October 2025
