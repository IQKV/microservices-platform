# Bookstore Service Deployment Guide

Complete guide for deploying the Bookstore Service across different environments.

## Prerequisites

### Required Services

- **User Service**: Must be deployed first (provides JWT authentication)
- **Gateway Service**: Optional but recommended for production
- **Observability Stack**: Prometheus, Grafana, Jaeger (optional)

### Infrastructure Requirements

**Local/Development**
- Kubernetes cluster (minikube, kind, k3s)
- 4 CPU cores, 8GB RAM minimum
- 20GB storage

**Staging**
- Kubernetes 1.24+
- 8 CPU cores, 16GB RAM minimum
- 50GB SSD storage
- Ingress controller with TLS support

**Production**
- Kubernetes 1.24+
- 16+ CPU cores, 32GB+ RAM
- 100GB+ SSD storage
- Multi-zone deployment
- External secret management
- Backup solution for PostgreSQL

## Deployment Steps

### 1. Prepare Namespace

```bash
# Create namespace
kubectl create namespace gripday-bookstore

# Label namespace for network policies
kubectl label namespace gripday-bookstore \
  app.kubernetes.io/part-of=gripday \
  environment=local
```

### 2. Create Secrets

**Local Development**

```bash
# Secrets are embedded in values.yaml for local development
# No action needed
```

**Staging/Production**

```bash
# Option 1: Manual secret creation
kubectl create secret generic bookstore-service-secrets \
  --from-literal=GRIPDAY_DATABASE_USERNAME=bookstore_user \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=$(openssl rand -base64 64) \
  --from-literal=GRIPDAY_CACHE_REDIS_PASSWORD=$(openssl rand -base64 32) \
  -n gripday-bookstore

# Option 2: From file
kubectl create secret generic bookstore-service-secrets \
  --from-env-file=.env.production \
  -n gripday-bookstore

# Option 3: External Secrets Operator (recommended for production)
# See: https://external-secrets.io/
```

### 3. Install Helm Chart

**Local Development**

```bash
helm install bookstore-service ./helm/bookstore-service \
  --namespace gripday-bookstore \
  --create-namespace \
  --wait \
  --timeout 5m
```

**Staging**

```bash
helm install bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-staging.yaml \
  --namespace gripday-bookstore-staging \
  --create-namespace \
  --wait \
  --timeout 10m
```

**Production**

```bash
# Dry run first
helm install bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-production.yaml \
  --namespace gripday-bookstore-production \
  --dry-run \
  --debug

# Install
helm install bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-production.yaml \
  --namespace gripday-bookstore-production \
  --create-namespace \
  --wait \
  --timeout 15m
```

### 4. Verify Deployment

```bash
# Check all resources
kubectl get all -n gripday-bookstore

# Check pod status
kubectl get pods -n gripday-bookstore -w

# Check deployment rollout
kubectl rollout status deployment/bookstore-service -n gripday-bookstore

# Check HPA
kubectl get hpa -n gripday-bookstore

# Check PVCs
kubectl get pvc -n gripday-bookstore
```

### 5. Verify Health

```bash
# Port forward to service
kubectl port-forward -n gripday-bookstore svc/bookstore-service 8080:8080

# Check health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Check API documentation
open http://localhost:8080/swagger-ui.html
```

### 6. Verify Database

```bash
# Get PostgreSQL pod
POSTGRES_POD=$(kubectl get pod -n gripday-bookstore -l app.kubernetes.io/name=bookstore-postgres -o jsonpath='{.items[0].metadata.name}')

# Connect to database
kubectl exec -it $POSTGRES_POD -n gripday-bookstore -- \
  psql -U gripday_user -d gripday_bookstore_local

# Check tables (should be created by Liquibase)
\dt

# Check Liquibase changelog
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;

# Exit
\q
```

### 7. Verify Redis

```bash
# Get Redis pod
REDIS_POD=$(kubectl get pod -n gripday-bookstore -l app.kubernetes.io/name=bookstore-redis -o jsonpath='{.items[0].metadata.name}')

# Test Redis connection
kubectl exec -it $REDIS_POD -n gripday-bookstore -- redis-cli ping

# Check Redis info
kubectl exec -it $REDIS_POD -n gripday-bookstore -- redis-cli info
```

## Upgrading

### Rolling Update

```bash
# Update image tag
helm upgrade bookstore-service ./helm/bookstore-service \
  --set image.tag=1.1.0 \
  --namespace gripday-bookstore \
  --wait

# Watch rollout
kubectl rollout status deployment/bookstore-service -n gripday-bookstore
```

### Configuration Changes

```bash
# Update with new values
helm upgrade bookstore-service ./helm/bookstore-service \
  -f ./helm/bookstore-service/values-production.yaml \
  --set replicaCount=10 \
  --namespace gripday-bookstore-production \
  --wait
```

### Rollback

```bash
# List releases
helm history bookstore-service -n gripday-bookstore

# Rollback to previous version
helm rollback bookstore-service -n gripday-bookstore

# Rollback to specific revision
helm rollback bookstore-service 3 -n gripday-bookstore
```

## Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment bookstore-service \
  --replicas=5 \
  -n gripday-bookstore

# Disable HPA temporarily
kubectl patch hpa bookstore-service-hpa \
  -n gripday-bookstore \
  -p '{"spec":{"minReplicas":5,"maxReplicas":5}}'
```

### Autoscaling Configuration

```bash
# Update HPA
helm upgrade bookstore-service ./helm/bookstore-service \
  --set autoscaling.minReplicas=10 \
  --set autoscaling.maxReplicas=30 \
  --set autoscaling.targetCPUUtilizationPercentage=50 \
  -n gripday-bookstore
```

## Backup and Restore

### PostgreSQL Backup

```bash
# Create backup
kubectl exec -n gripday-bookstore bookstore-postgres-0 -- \
  pg_dump -U gripday_user gripday_bookstore_production > backup.sql

# Backup to PVC
kubectl exec -n gripday-bookstore bookstore-postgres-0 -- \
  pg_dump -U gripday_user gripday_bookstore_production | \
  gzip > /backups/bookstore-$(date +%Y%m%d-%H%M%S).sql.gz
```

### PostgreSQL Restore

```bash
# Restore from backup
kubectl exec -i -n gripday-bookstore bookstore-postgres-0 -- \
  psql -U gripday_user gripday_bookstore_production < backup.sql
```

### Redis Backup

```bash
# Trigger RDB snapshot
kubectl exec -n gripday-bookstore bookstore-redis-0 -- redis-cli BGSAVE

# Copy RDB file
kubectl cp gripday-bookstore/bookstore-redis-0:/data/dump.rdb ./redis-backup.rdb
```

## Monitoring

### Prometheus Metrics

```bash
# Port forward to Prometheus
kubectl port-forward -n monitoring svc/prometheus 9090:9090

# Query bookstore metrics
# http://localhost:9090/graph?g0.expr=http_server_requests_seconds_count{application="bookstore-service"}
```

### Grafana Dashboards

```bash
# Port forward to Grafana
kubectl port-forward -n monitoring svc/grafana 3000:3000

# Import dashboard
# Dashboard ID: gripday-bookstore-service
```

### Logs

```bash
# Stream logs
kubectl logs -f -n gripday-bookstore \
  -l app.kubernetes.io/name=gripday-bookstore-service \
  --tail=100

# Search logs with grep
kubectl logs -n gripday-bookstore \
  -l app.kubernetes.io/name=gripday-bookstore-service \
  --tail=1000 | grep ERROR

# Export logs
kubectl logs -n gripday-bookstore \
  -l app.kubernetes.io/name=gripday-bookstore-service \
  --since=1h > bookstore-logs.txt
```

## Troubleshooting

### Pod Crashes

```bash
# Check pod events
kubectl describe pod <pod-name> -n gripday-bookstore

# Check previous logs
kubectl logs <pod-name> -n gripday-bookstore --previous

# Check resource usage
kubectl top pod -n gripday-bookstore
```

### Database Issues

```bash
# Check PostgreSQL logs
kubectl logs -n gripday-bookstore -l app.kubernetes.io/name=bookstore-postgres

# Check connections
kubectl exec -n gripday-bookstore bookstore-postgres-0 -- \
  psql -U gripday_user -d gripday_bookstore_production \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Check slow queries
kubectl exec -n gripday-bookstore bookstore-postgres-0 -- \
  psql -U gripday_user -d gripday_bookstore_production \
  -c "SELECT pid, now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC;"
```

### Network Issues

```bash
# Test connectivity from pod
kubectl exec -it <bookstore-pod> -n gripday-bookstore -- sh

# Inside pod:
curl http://bookstore-postgres:5432
curl http://bookstore-redis:6379
curl http://user-service:8080/.well-known/jwks.json

# Check network policies
kubectl get networkpolicies -n gripday-bookstore
kubectl describe networkpolicy bookstore-service-netpol -n gripday-bookstore
```

### Performance Issues

```bash
# Check HPA metrics
kubectl get hpa -n gripday-bookstore -w

# Check resource usage
kubectl top pods -n gripday-bookstore
kubectl top nodes

# Check database performance
kubectl exec -n gripday-bookstore bookstore-postgres-0 -- \
  psql -U gripday_user -d gripday_bookstore_production \
  -c "SELECT * FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;"
```

## Cleanup

### Uninstall Chart

```bash
# Uninstall release
helm uninstall bookstore-service -n gripday-bookstore

# Delete namespace (removes all resources)
kubectl delete namespace gripday-bookstore
```

### Preserve Data

```bash
# Backup PVCs before deletion
kubectl get pvc -n gripday-bookstore

# Create snapshots or copy data before uninstalling
```

## Best Practices

### Production Deployment

1. **Always use specific image tags** (not `latest`)
2. **Test in staging first** before production deployment
3. **Use external secret management** (AWS Secrets Manager, Vault)
4. **Enable backups** for PostgreSQL and Redis
5. **Monitor metrics and logs** continuously
6. **Set up alerts** for critical issues
7. **Use pod disruption budgets** to prevent service disruption
8. **Configure resource limits** appropriately
9. **Use network policies** to restrict traffic
10. **Regular security updates** for base images

### High Availability

1. **Multi-zone deployment** with topology spread constraints
2. **Minimum 3 replicas** in production
3. **Pod anti-affinity** to spread across nodes
4. **Health checks** properly configured
5. **Graceful shutdown** with preStop hooks
6. **Database replication** for PostgreSQL (consider external managed service)
7. **Redis Sentinel or Cluster** for high availability

### Security

1. **Rotate secrets regularly** (90-day rotation recommended)
2. **Use least privilege** for service accounts
3. **Enable pod security policies**
4. **Scan images** for vulnerabilities
5. **Use TLS** for all external communication
6. **Audit logs** for security events
7. **Network policies** to restrict traffic

## Support

- Documentation: https://docs.gripday.site
- Issues: https://github.com/gripday/bookstore-service/issues
- Platform Team: platform@gripday.site
