# Helm Deployment Commands - Quick Reference

## Prerequisites Setup

```bash
# 1. Install priority classes
kubectl apply -f ../k8s/priority-classes.yaml

# 2. Verify priority classes
kubectl get priorityclasses
```

## Complete Platform Deployment

### Deploy Everything (Umbrella Chart)

```bash
cd helm/gripday

# Build dependencies
helm dependency build

# Install complete platform
helm install gripday . --namespace gripday --create-namespace

# With custom values
helm install gripday . -f values-production.yaml --namespace production --create-namespace
```

## Individual Service Deployments

### Auth Service

```bash
# Install
helm install auth-service ./helm/auth-service \
  --namespace gripday-auth \
  --create-namespace

# Upgrade
helm upgrade auth-service ./helm/auth-service -n gripday-auth

# Uninstall
helm uninstall auth-service -n gripday-auth
```

### Bookstore Service

```bash
# Install
helm install bookstore-service ./helm/bookstore-service \
  --namespace gripday-bookstore \
  --create-namespace

# Upgrade
helm upgrade bookstore-service ./helm/bookstore-service -n gripday-bookstore

# Uninstall
helm uninstall bookstore-service -n gripday-bookstore
```

### Gateway Service

```bash
# Install
helm install gateway-service ./helm/gateway-service \
  --namespace gripday-gateway \
  --create-namespace

# Upgrade
helm upgrade gateway-service ./helm/gateway-service -n gripday-gateway

# Uninstall
helm uninstall gateway-service -n gripday-gateway
```

## Environment-Specific Deployments

### Local/Development

```bash
# Using default values
helm install gripday ./helm/gripday \
  --namespace dev \
  --create-namespace

# With debug enabled
helm install gripday ./helm/gripday \
  --namespace dev \
  --create-namespace \
  --debug
```

### Staging

```bash
# Create staging values file first (values-staging.yaml)
helm install gripday ./helm/gripday \
  -f values-staging.yaml \
  --namespace staging \
  --create-namespace
```

### Production

```bash
# Production deployment
helm install gripday ./helm/gripday \
  -f values-production.yaml \
  --namespace production \
  --create-namespace

# With timeout for large deployments
helm install gripday ./helm/gripday \
  -f values-production.yaml \
  --namespace production \
  --create-namespace \
  --timeout 10m
```

## Verification Commands

### Check Installations

```bash
# List all Helm releases
helm list -A

# Get release status
helm status gripday -n gripday

# Get release history
helm history gripday -n gripday
```

### Check Kubernetes Resources

```bash
# Check all pods
kubectl get pods -A | grep gripday

# Check specific namespace
kubectl get pods -n gripday-auth
kubectl get pods -n gripday-bookstore
kubectl get pods -n gripday-gateway

# Check services
kubectl get svc -A | grep gripday

# Check ingress
kubectl get ingress -A
```

### Health Checks

```bash
# Port forward gateway
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080

# Check health
curl http://localhost:8080/actuator/health

# Port forward auth service
kubectl port-forward -n gripday-auth svc/auth-service 8080:8080
curl http://localhost:8080/actuator/health

# Port forward bookstore service
kubectl port-forward -n gripday-bookstore svc/bookstore-service 8080:8080
curl http://localhost:8080/actuator/health
```

## Update Operations

### Upgrade with New Values

```bash
# Upgrade single service
helm upgrade auth-service ./helm/auth-service \
  --set replicaCount=3 \
  -n gripday-auth

# Upgrade platform
helm upgrade gripday ./helm/gripday \
  -f values-production.yaml \
  -n production
```

### Update Image Tag

```bash
# Update auth service image
helm upgrade auth-service ./helm/auth-service \
  --set image.tag=1.1.0 \
  -n gripday-auth

# Update via umbrella chart
helm upgrade gripday ./helm/gripday \
  --set auth-service.image.tag=1.1.0 \
  -n gripday
```

### Rolling Restart

```bash
# Restart deployment
kubectl rollout restart deployment/auth-service -n gripday-auth

# Check rollout status
kubectl rollout status deployment/auth-service -n gripday-auth
```

## Rollback Operations

```bash
# Rollback to previous version
helm rollback gripday -n gripday

# Rollback to specific revision
helm rollback gripday 2 -n gripday

# View rollback history
helm history gripday -n gripday
```

## Debugging

### Dry Run and Template

```bash
# Dry run installation
helm install test ./helm/auth-service \
  --dry-run \
  --debug \
  -n test

# Generate templates
helm template test ./helm/auth-service > output.yaml

# Lint chart
helm lint ./helm/auth-service
```

### View Logs

```bash
# Gateway logs
kubectl logs -f -n gripday-gateway \
  -l app.kubernetes.io/name=gripday-gateway-service

# Auth service logs
kubectl logs -f -n gripday-auth \
  -l app.kubernetes.io/name=gripday-auth-service

# Bookstore service logs
kubectl logs -f -n gripday-bookstore \
  -l app.kubernetes.io/name=gripday-bookstore-service

# View previous container logs (if crashed)
kubectl logs <pod-name> -n <namespace> --previous
```

### Describe Resources

```bash
# Describe pod
kubectl describe pod <pod-name> -n <namespace>

# Describe deployment
kubectl describe deployment auth-service -n gripday-auth

# Describe service
kubectl describe svc auth-service -n gripday-auth
```

## Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment auth-service --replicas=5 -n gripday-auth

# Scale via Helm upgrade
helm upgrade auth-service ./helm/auth-service \
  --set replicaCount=5 \
  -n gripday-auth
```

### Check HPA

```bash
# View HPA status
kubectl get hpa -A

# Describe HPA
kubectl describe hpa bookstore-service-hpa -n gripday-bookstore
```

## Cleanup

### Uninstall Services

```bash
# Uninstall complete platform
helm uninstall gripday -n gripday

# Uninstall individual services
helm uninstall auth-service -n gripday-auth
helm uninstall bookstore-service -n gripday-bookstore
helm uninstall gateway-service -n gripday-gateway
```

### Delete Namespaces

```bash
# Delete namespaces (includes all resources)
kubectl delete namespace gripday-auth
kubectl delete namespace gripday-bookstore
kubectl delete namespace gripday-gateway
kubectl delete namespace gripday
```

### Clean Up PVCs

```bash
# List PVCs
kubectl get pvc -A

# Delete specific PVC
kubectl delete pvc <pvc-name> -n <namespace>

# Delete all PVCs in namespace
kubectl delete pvc --all -n gripday-auth
```

## Backup and Restore

### Backup

```bash
# Get all Helm values
helm get values gripday -n gripday > backup-values.yaml

# Backup all resources
kubectl get all -n gripday-auth -o yaml > backup-auth.yaml
kubectl get all -n gripday-bookstore -o yaml > backup-bookstore.yaml
kubectl get all -n gripday-gateway -o yaml > backup-gateway.yaml

# Backup databases
kubectl exec -n gripday-auth auth-postgres-<pod> -- \
  pg_dump -U gripday_user gripday_auth_local > backup-auth-db.sql
```

### Restore

```bash
# Install from backup values
helm install gripday ./helm/gripday \
  -f backup-values.yaml \
  -n gripday \
  --create-namespace
```

## Advanced Operations

### Selective Service Deployment

```bash
# Deploy only auth and gateway (no bookstore)
helm install gripday ./helm/gripday \
  --set bookstore-service.enabled=false \
  -n gripday \
  --create-namespace
```

### Override Multiple Values

```bash
helm install gripday ./helm/gripday \
  --set auth-service.replicaCount=3 \
  --set gateway-service.replicaCount=5 \
  --set bookstore-service.autoscaling.minReplicas=4 \
  -n gripday \
  --create-namespace
```

### Install with Wait

```bash
# Wait for all pods to be ready
helm install gripday ./helm/gripday \
  --namespace gripday \
  --create-namespace \
  --wait \
  --timeout 10m
```

## Testing

### Connectivity Tests

```bash
# Test DNS resolution
kubectl run test-dns --image=busybox --rm -it -- \
  nslookup auth-service.gripday-auth.svc.cluster.local

# Test service connectivity
kubectl run test-curl --image=curlimages/curl --rm -it -- \
  curl http://auth-service.gripday-auth.svc.cluster.local:8080/actuator/health
```

### Load Testing

```bash
# Using kubectl port-forward and external tools
kubectl port-forward -n gripday-gateway svc/gateway-service 8080:8080 &

# Then use your preferred load testing tool
# ab, wrk, k6, etc.
```

---

**Quick Reference Card**  
**Version**: 1.0  
**Platform**: Gripday Microservices
