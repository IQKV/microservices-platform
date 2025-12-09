# Billing Service Deployment Guide

## Overview

This document provides comprehensive deployment procedures for the Billing Service across different environments (local, staging, production).

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Local Deployment](#local-deployment)
4. [Staging Deployment](#staging-deployment)
5. [Production Deployment](#production-deployment)
6. [Database Migrations](#database-migrations)
7. [Rollback Procedures](#rollback-procedures)
8. [Health Checks](#health-checks)
9. [Monitoring Setup](#monitoring-setup)

---

## Prerequisites

### Required Tools

- **Java 21**: OpenJDK 21 or later
- **Maven 3.9+**: Build tool
- **Docker 24+**: Container runtime
- **Kubernetes 1.28+**: Container orchestration
- **kubectl**: Kubernetes CLI
- **Helm 3.12+**: Kubernetes package manager
- **PostgreSQL 15+**: Database
- **Redis 7+**: Caching layer
- **RabbitMQ 3.12+**: Message broker

### Access Requirements

- **Kubernetes Cluster**: Access to target cluster
- **Container Registry**: Push access to Docker registry
- **Database**: Admin access for migrations
- **Secrets Management**: Access to secrets (Vault, AWS Secrets Manager)
- **Payment Providers**: API keys for Stripe/PayPal

---

## Environment Configuration

### Environment Variables

#### Required for All Environments

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/billing_db
SPRING_DATASOURCE_USERNAME=billing_user
SPRING_DATASOURCE_PASSWORD=<secret>

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=<secret>

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=billing_user
SPRING_RABBITMQ_PASSWORD=<secret>

# JWT Configuration
IQSCAFFOLD_BILLING_SECURITY_JWT_JWK_SET_URI=http://user-service:8081/.well-known/jwks.json

# Payment Providers
STRIPE_API_KEY=<secret>
STRIPE_WEBHOOK_SECRET=<secret>
PAYPAL_CLIENT_ID=<secret>
PAYPAL_CLIENT_SECRET=<secret>
```

#### Environment-Specific

**Local**:
```bash
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8082
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_IQSCAFFOLD=DEBUG
```

**Staging**:
```bash
SPRING_PROFILES_ACTIVE=staging
SERVER_PORT=8082
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_IQSCAFFOLD=INFO
```

**Production**:
```bash
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8082
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_IQSCAFFOLD=INFO
```

### Configuration Files

#### application.yml (Base Configuration)

```yaml
server:
  port: 8082
  shutdown: graceful

spring:
  application:
    name: billing-service
  
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}
    password: ${SPRING_REDIS_PASSWORD}
  
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST}
    port: ${SPRING_RABBITMQ_PORT}
    username: ${SPRING_RABBITMQ_USERNAME}
    password: ${SPRING_RABBITMQ_PASSWORD}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

iqscaffold:
  billing:
    security:
      jwt:
        jwk-set-uri: ${IQSCAFFOLD_BILLING_SECURITY_JWT_JWK_SET_URI}
    payment:
      provider: stripe
      stripe:
        api-key: ${STRIPE_API_KEY}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET}
```

---

## Local Deployment

### Using Docker Compose

1. **Start Infrastructure**:

```bash
cd backend/iqscaffold-billing-service
docker-compose up -d postgres redis rabbitmq
```

2. **Run Database Migrations**:

```bash
./mvnw liquibase:update -Dspring.profiles.active=local
```

3. **Build Application**:

```bash
./mvnw clean package -DskipTests
```

4. **Run Application**:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

5. **Verify Deployment**:

```bash
curl http://localhost:8082/actuator/health
```

### Using Docker

1. **Build Docker Image**:

```bash
docker build -t iqscaffold/billing-service:latest .
```

2. **Run Container**:

```bash
docker run -d \
  --name billing-service \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/billing_db \
  -e SPRING_REDIS_HOST=host.docker.internal \
  -e SPRING_RABBITMQ_HOST=host.docker.internal \
  iqscaffold/billing-service:latest
```

3. **View Logs**:

```bash
docker logs -f billing-service
```

---

## Staging Deployment

### Pre-Deployment Checklist

- [ ] Code reviewed and approved
- [ ] All tests passing
- [ ] Database migrations tested
- [ ] Configuration reviewed
- [ ] Secrets updated in Vault
- [ ] Deployment window scheduled
- [ ] Rollback plan prepared

### Deployment Steps

1. **Build and Tag Image**:

```bash
# Build image
docker build -t iqscaffold/billing-service:${VERSION} .

# Tag for staging
docker tag iqscaffold/billing-service:${VERSION} \
  iqscaffold/billing-service:staging-${VERSION}

# Push to registry
docker push iqscaffold/billing-service:staging-${VERSION}
```

2. **Update Kubernetes Secrets**:

```bash
# Create/update secrets
kubectl create secret generic billing-secrets \
  --from-literal=database-password=${DB_PASSWORD} \
  --from-literal=redis-password=${REDIS_PASSWORD} \
  --from-literal=rabbitmq-password=${RABBITMQ_PASSWORD} \
  --from-literal=stripe-api-key=${STRIPE_API_KEY} \
  --from-literal=stripe-webhook-secret=${STRIPE_WEBHOOK_SECRET} \
  --namespace=staging \
  --dry-run=client -o yaml | kubectl apply -f -
```

3. **Run Database Migrations**:

```bash
# Connect to staging database
kubectl run -it --rm migration-job \
  --image=iqscaffold/billing-service:staging-${VERSION} \
  --restart=Never \
  --namespace=staging \
  --env="SPRING_PROFILES_ACTIVE=staging" \
  --command -- ./mvnw liquibase:update
```

4. **Deploy Application**:

```bash
# Update deployment
kubectl set image deployment/billing-service \
  billing-service=iqscaffold/billing-service:staging-${VERSION} \
  --namespace=staging

# Or use Helm
helm upgrade billing-service ./helm/billing-service \
  --namespace=staging \
  --values=helm/billing-service/values-staging.yaml \
  --set image.tag=staging-${VERSION}
```

5. **Monitor Deployment**:

```bash
# Watch rollout status
kubectl rollout status deployment/billing-service --namespace=staging

# Check pod status
kubectl get pods -l app=billing-service --namespace=staging

# View logs
kubectl logs -f -l app=billing-service --namespace=staging
```

6. **Verify Deployment**:

```bash
# Health check
curl https://billing-staging.iqscaffold.com/actuator/health

# Smoke tests
./scripts/smoke-tests.sh staging
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Staging deployment successful
- [ ] Smoke tests passed in staging
- [ ] Performance tests completed
- [ ] Security scan passed
- [ ] Change request approved
- [ ] Deployment window scheduled (low-traffic period)
- [ ] On-call engineer notified
- [ ] Rollback plan tested
- [ ] Database backup completed
- [ ] Monitoring alerts configured

### Deployment Steps

1. **Create Database Backup**:

```bash
# Backup production database
pg_dump -h prod-db.iqscaffold.com -U billing_user -d billing_db \
  -F c -f billing_backup_$(date +%Y%m%d_%H%M%S).dump

# Upload to S3
aws s3 cp billing_backup_$(date +%Y%m%d_%H%M%S).dump \
  s3://iqscaffold-backups/billing/production/
```

2. **Build and Tag Production Image**:

```bash
# Tag for production
docker tag iqscaffold/billing-service:${VERSION} \
  iqscaffold/billing-service:production-${VERSION}

docker tag iqscaffold/billing-service:${VERSION} \
  iqscaffold/billing-service:latest

# Push to registry
docker push iqscaffold/billing-service:production-${VERSION}
docker push iqscaffold/billing-service:latest
```

3. **Update Production Secrets**:

```bash
# Update secrets from Vault
kubectl create secret generic billing-secrets \
  --from-literal=database-password=$(vault read -field=password secret/prod/billing/database) \
  --from-literal=redis-password=$(vault read -field=password secret/prod/billing/redis) \
  --from-literal=rabbitmq-password=$(vault read -field=password secret/prod/billing/rabbitmq) \
  --from-literal=stripe-api-key=$(vault read -field=api-key secret/prod/billing/stripe) \
  --from-literal=stripe-webhook-secret=$(vault read -field=webhook-secret secret/prod/billing/stripe) \
  --namespace=production \
  --dry-run=client -o yaml | kubectl apply -f -
```

4. **Run Database Migrations**:

```bash
# Run migrations in production
kubectl run -it --rm migration-job \
  --image=iqscaffold/billing-service:production-${VERSION} \
  --restart=Never \
  --namespace=production \
  --env="SPRING_PROFILES_ACTIVE=production" \
  --command -- ./mvnw liquibase:update

# Verify migrations
kubectl logs migration-job --namespace=production
```

5. **Deploy with Blue-Green Strategy**:

```bash
# Deploy new version (green)
helm upgrade billing-service ./helm/billing-service \
  --namespace=production \
  --values=helm/billing-service/values-production.yaml \
  --set image.tag=production-${VERSION} \
  --set replicaCount=3 \
  --wait \
  --timeout=10m

# Monitor new pods
kubectl get pods -l app=billing-service,version=${VERSION} --namespace=production

# Check health
for i in {1..10}; do
  curl https://billing.iqscaffold.com/actuator/health
  sleep 5
done
```

6. **Smoke Tests**:

```bash
# Run production smoke tests
./scripts/smoke-tests.sh production

# Verify critical flows
./scripts/verify-payment-flow.sh
./scripts/verify-subscription-flow.sh
./scripts/verify-quota-check.sh
```

7. **Monitor Metrics**:

```bash
# Check error rate
curl https://prometheus.iqscaffold.com/api/v1/query?query=rate(http_server_requests_errors_total[5m])

# Check latency
curl https://prometheus.iqscaffold.com/api/v1/query?query=http_server_requests_latency_p99

# Check payment success rate
curl https://prometheus.iqscaffold.com/api/v1/query?query=billing_payments_success_rate
```

8. **Gradual Traffic Shift** (if using canary deployment):

```bash
# Shift 10% traffic to new version
kubectl patch service billing-service \
  --namespace=production \
  --patch '{"spec":{"selector":{"version":"${VERSION}","weight":"10"}}}'

# Monitor for 15 minutes
# If stable, shift 50%
# If stable, shift 100%
```

9. **Complete Deployment**:

```bash
# Scale down old version
kubectl scale deployment/billing-service-old --replicas=0 --namespace=production

# Update latest tag
kubectl set image deployment/billing-service \
  billing-service=iqscaffold/billing-service:latest \
  --namespace=production
```

---

## Database Migrations

### Migration Strategy

We use Liquibase for database migrations with separate changesets for system (public schema) and tenant schemas.

### Running Migrations

#### System Migrations (Public Schema)

```bash
# Run system migrations
./mvnw liquibase:update \
  -Dliquibase.changeLogFile=db/changelog/system/master.xml \
  -Dspring.profiles.active=production
```

#### Tenant Migrations (All Tenant Schemas)

```bash
# Run tenant migrations for all tenants
./scripts/run-tenant-migrations.sh production
```

### Migration Script Example

```bash
#!/bin/bash
# scripts/run-tenant-migrations.sh

ENVIRONMENT=$1

# Get list of tenant schemas
TENANTS=$(psql -h $DB_HOST -U $DB_USER -d $DB_NAME -t -c \
  "SELECT schema_name FROM information_schema.schemata 
   WHERE schema_name LIKE 'tenant_%';")

# Run migrations for each tenant
for TENANT in $TENANTS; do
  echo "Running migrations for $TENANT..."
  
  ./mvnw liquibase:update \
    -Dliquibase.changeLogFile=db/changelog/tenant/master.xml \
    -Dliquibase.defaultSchemaName=$TENANT \
    -Dspring.profiles.active=$ENVIRONMENT
  
  if [ $? -eq 0 ]; then
    echo "✓ Migrations completed for $TENANT"
  else
    echo "✗ Migrations failed for $TENANT"
    exit 1
  fi
done

echo "All tenant migrations completed successfully"
```

### Rollback Migrations

```bash
# Rollback last changeset
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.2.0

# Rollback to specific date
./mvnw liquibase:rollback -Dliquibase.rollbackDate=2024-12-01
```

---

## Rollback Procedures

### Quick Rollback (Kubernetes)

```bash
# Rollback to previous version
kubectl rollout undo deployment/billing-service --namespace=production

# Rollback to specific revision
kubectl rollout undo deployment/billing-service --to-revision=2 --namespace=production

# Check rollback status
kubectl rollout status deployment/billing-service --namespace=production
```

### Full Rollback with Database

1. **Stop New Version**:

```bash
kubectl scale deployment/billing-service --replicas=0 --namespace=production
```

2. **Rollback Database**:

```bash
# Restore from backup
pg_restore -h prod-db.iqscaffold.com -U billing_user -d billing_db \
  -c billing_backup_20241209_100000.dump

# Or rollback migrations
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.2.0
```

3. **Deploy Previous Version**:

```bash
kubectl set image deployment/billing-service \
  billing-service=iqscaffold/billing-service:production-1.2.0 \
  --namespace=production

kubectl scale deployment/billing-service --replicas=3 --namespace=production
```

4. **Verify Rollback**:

```bash
curl https://billing.iqscaffold.com/actuator/health
./scripts/smoke-tests.sh production
```

---

## Health Checks

### Kubernetes Liveness Probe

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Kubernetes Readiness Probe

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8082
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### Manual Health Checks

```bash
# Overall health
curl https://billing.iqscaffold.com/actuator/health

# Database health
curl https://billing.iqscaffold.com/actuator/health/db

# Redis health
curl https://billing.iqscaffold.com/actuator/health/redis

# RabbitMQ health
curl https://billing.iqscaffold.com/actuator/health/rabbitmq
```

---

## Monitoring Setup

### Prometheus Metrics

```yaml
# prometheus-config.yaml
scrape_configs:
  - job_name: 'billing-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - production
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: billing-service
      - source_labels: [__meta_kubernetes_pod_ip]
        target_label: __address__
        replacement: ${1}:8082
    metrics_path: /actuator/prometheus
```

### Grafana Dashboards

Import pre-built dashboards from `grafana/dashboards/`:

- `billing-overview.json`: Service overview
- `payment-processing.json`: Payment metrics
- `subscription-lifecycle.json`: Subscription metrics
- `quota-enforcement.json`: Quota and usage metrics

### Alerts

```yaml
# prometheus-alerts.yaml
groups:
  - name: billing-service
    rules:
      - alert: HighPaymentFailureRate
        expr: rate(billing_payments_failed_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High payment failure rate"
          description: "Payment failure rate is {{ $value }}%"
      
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
```

---

## Post-Deployment Verification

### Verification Checklist

- [ ] Health checks passing
- [ ] All pods running
- [ ] No error logs
- [ ] Metrics being collected
- [ ] Smoke tests passed
- [ ] Payment processing working
- [ ] Subscription creation working
- [ ] Quota checks working
- [ ] Invoice generation working
- [ ] Webhooks processing
- [ ] Database connections healthy
- [ ] Redis connections healthy
- [ ] RabbitMQ connections healthy

### Smoke Test Script

```bash
#!/bin/bash
# scripts/smoke-tests.sh

ENVIRONMENT=$1
BASE_URL="https://billing-${ENVIRONMENT}.iqscaffold.com"

echo "Running smoke tests for $ENVIRONMENT..."

# Test 1: Health check
echo "Test 1: Health check"
curl -f $BASE_URL/actuator/health || exit 1

# Test 2: List plans
echo "Test 2: List subscription plans"
curl -f $BASE_URL/api/v1/plans || exit 1

# Test 3: Check quota (requires auth)
echo "Test 3: Check quota"
curl -f -H "Authorization: Bearer $TEST_TOKEN" \
  $BASE_URL/api/v1/internal/quota/check \
  -d '{"tenantId":"test-tenant","metricType":"API_CALLS","quantity":1}' || exit 1

echo "✓ All smoke tests passed"
```

---

## Troubleshooting Deployment Issues

### Issue: Pods Not Starting

**Check**:
```bash
kubectl describe pod billing-service-xxx --namespace=production
kubectl logs billing-service-xxx --namespace=production
```

**Common Causes**:
- Image pull errors
- Configuration errors
- Resource limits
- Health check failures

### Issue: Database Migration Failures

**Check**:
```bash
kubectl logs migration-job --namespace=production
```

**Resolution**:
- Verify database connectivity
- Check migration changesets
- Review Liquibase logs
- Rollback if needed

### Issue: High Memory Usage

**Check**:
```bash
kubectl top pods -l app=billing-service --namespace=production
```

**Resolution**:
- Increase memory limits
- Check for memory leaks
- Analyze heap dump

---

## Support

For deployment support:

- **DevOps Team**: devops@iqscaffold.com
- **On-Call Engineer**: +1-555-0100
- **Runbook**: See RUNBOOK.md
- **Incident Response**: See RUNBOOK.md#incident-response
