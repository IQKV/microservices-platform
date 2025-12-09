# Billing Service Helm Chart

This Helm chart deploys the IQ Scaffold Billing & Subscription Management Service to Kubernetes.

## Overview

The Billing Service provides comprehensive subscription lifecycle management, payment processing, invoicing, and usage-based billing for the IQ Scaffold platform. It includes:

- Subscription lifecycle management (create, upgrade, downgrade, cancel, reactivate)
- Multi-provider payment integration (Stripe, PayPal, manual)
- Automated invoice generation and PDF delivery
- Usage-based billing and quota enforcement
- Customer billing portal
- Administrative analytics and reporting

## Prerequisites

- Kubernetes 1.24+
- Helm 3.8+
- PostgreSQL 15+ (included in chart or external)
- Redis 7.2+ (included in chart or external)
- RabbitMQ 3.12+ (included in chart or external)
- User Service deployed and accessible

## Installation

### Install with default values (development)

```bash
helm install billing-service ./billing-service \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

### Install for staging environment

```bash
helm install billing-service ./billing-service \
  --namespace iqscaffold-staging-env \
  --create-namespace \
  --values ./billing-service/values-staging.yaml
```

### Install for production environment

```bash
helm install billing-service ./billing-service \
  --namespace iqscaffold-production-env \
  --create-namespace \
  --values ./billing-service/values-production.yaml \
  --set secrets.data.STRIPE_API_KEY=<base64-encoded-stripe-key> \
  --set secrets.data.STRIPE_WEBHOOK_SECRET=<base64-encoded-webhook-secret>
```

### Install for test environment

```bash
helm install billing-service ./billing-service \
  --namespace iqscaffold-test-env \
  --create-namespace \
  --values ./billing-service/values-test.yaml
```

## Configuration

### Key Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of billing service replicas | `1` |
| `image.repository` | Billing service image repository | `iqscaffold/iqscaffold-billing-service` |
| `image.tag` | Billing service image tag | `1.0.0` |
| `resources.requests.memory` | Memory request per pod | `768Mi` |
| `resources.requests.cpu` | CPU request per pod | `500m` |
| `resources.limits.memory` | Memory limit per pod | `1536Mi` |
| `resources.limits.cpu` | CPU limit per pod | `1000m` |
| `autoscaling.enabled` | Enable horizontal pod autoscaling | `false` |
| `autoscaling.minReplicas` | Minimum number of replicas | `1` |
| `autoscaling.maxReplicas` | Maximum number of replicas | `3` |
| `postgresql.enabled` | Deploy PostgreSQL with chart | `true` |
| `postgresql.persistence.size` | PostgreSQL storage size | `10Gi` |
| `redis.enabled` | Deploy Redis with chart | `true` |
| `redis.persistence.size` | Redis storage size | `2Gi` |
| `rabbitmq.enabled` | Deploy RabbitMQ with chart | `true` |
| `rabbitmq.persistence.size` | RabbitMQ storage size | `5Gi` |

### Payment Provider Configuration

The billing service supports multiple payment providers:

#### Stripe (Default)

```yaml
env:
  - name: IQSCAFFOLD_BILLING_PAYMENT_PROVIDER
    value: "stripe"

secretEnv:
  - name: STRIPE_API_KEY
    secretKeyRef:
      name: billing-service-secrets
      key: STRIPE_API_KEY
  - name: STRIPE_WEBHOOK_SECRET
    secretKeyRef:
      name: billing-service-secrets
      key: STRIPE_WEBHOOK_SECRET
```

#### PayPal

```yaml
env:
  - name: IQSCAFFOLD_BILLING_PAYMENT_PROVIDER
    value: "paypal"

secretEnv:
  - name: PAYPAL_CLIENT_ID
    secretKeyRef:
      name: billing-service-secrets
      key: PAYPAL_CLIENT_ID
  - name: PAYPAL_CLIENT_SECRET
    secretKeyRef:
      name: billing-service-secrets
      key: PAYPAL_CLIENT_SECRET
```

#### Manual (for testing or enterprise contracts)

```yaml
env:
  - name: IQSCAFFOLD_BILLING_PAYMENT_PROVIDER
    value: "manual"
```

### Subscription Configuration

```yaml
env:
  - name: IQSCAFFOLD_BILLING_SUBSCRIPTION_DEFAULT_CURRENCY
    value: "USD"
  - name: IQSCAFFOLD_BILLING_SUBSCRIPTION_TRIAL_DAYS
    value: "14"
  - name: IQSCAFFOLD_BILLING_SUBSCRIPTION_GRACE_PERIOD_DAYS
    value: "3"
  - name: IQSCAFFOLD_BILLING_SUBSCRIPTION_ALLOW_MULTIPLE_SUBSCRIPTIONS
    value: "false"
```

### Usage Metering Configuration

```yaml
env:
  - name: IQSCAFFOLD_BILLING_USAGE_METERING_ENABLED
    value: "true"
  - name: IQSCAFFOLD_BILLING_USAGE_BATCH_SIZE
    value: "100"
  - name: IQSCAFFOLD_BILLING_USAGE_FLUSH_INTERVAL
    value: "PT30S"
  - name: IQSCAFFOLD_BILLING_USAGE_RETENTION_DAYS
    value: "365"
```

### Invoice Configuration

```yaml
env:
  - name: IQSCAFFOLD_BILLING_INVOICE_NUMBER_FORMAT
    value: "INV-{YEAR}{MONTH}-{SEQUENCE}"
  - name: IQSCAFFOLD_BILLING_INVOICE_DUE_DAYS
    value: "7"
  - name: IQSCAFFOLD_BILLING_INVOICE_AUTO_FINALIZE
    value: "true"
  - name: IQSCAFFOLD_BILLING_INVOICE_PDF_GENERATION_ENABLED
    value: "true"
```

## Secrets Management

### Creating Secrets Manually

For production deployments, create secrets manually instead of using the chart's default secrets:

```bash
# Create billing service secrets
kubectl create secret generic billing-service-secrets \
  --namespace iqscaffold-production-env \
  --from-literal=IQSCAFFOLD_DATABASE_USERNAME=billing_user \
  --from-literal=IQSCAFFOLD_DATABASE_PASSWORD=<secure-password> \
  --from-literal=IQSCAFFOLD_RABBITMQ_USERNAME=billing_user \
  --from-literal=IQSCAFFOLD_RABBITMQ_PASSWORD=<secure-password> \
  --from-literal=STRIPE_API_KEY=<stripe-api-key> \
  --from-literal=STRIPE_WEBHOOK_SECRET=<stripe-webhook-secret>

# Disable chart-managed secrets
helm install billing-service ./billing-service \
  --namespace iqscaffold-production-env \
  --values ./billing-service/values-production.yaml \
  --set secrets.enabled=false
```

### Using External Secrets Operator

For production environments, consider using External Secrets Operator to sync secrets from AWS Secrets Manager, HashiCorp Vault, or other secret stores.

## Monitoring and Observability

The billing service exposes Prometheus metrics at `/actuator/prometheus` and includes:

- Request rate and latency metrics
- Payment success/failure rates
- Subscription churn rate
- Database connection pool usage
- Cache hit rate
- Message queue depth
- Business metrics (MRR, ARR, active subscriptions)

### Accessing Metrics

```bash
# Port-forward to access metrics
kubectl port-forward -n iqscaffold-dev-env svc/billing-service 8082:80

# Access metrics endpoint
curl http://localhost:8082/actuator/prometheus
```

## Health Checks

The service provides three health check endpoints:

- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe (used by Kubernetes)
- `/actuator/health/readiness` - Readiness probe (used by Kubernetes)

## Upgrading

### Upgrade to new version

```bash
helm upgrade billing-service ./billing-service \
  --namespace iqscaffold-dev-env \
  --values ./billing-service/values.yaml
```

### Rollback to previous version

```bash
helm rollback billing-service -n iqscaffold-dev-env
```

## Uninstallation

```bash
helm uninstall billing-service -n iqscaffold-dev-env
```

**Note:** This will not delete PersistentVolumeClaims. To delete them:

```bash
kubectl delete pvc -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-service
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-service
```

### View logs

```bash
kubectl logs -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-service --tail=100 -f
```

### Check service connectivity

```bash
kubectl exec -n iqscaffold-dev-env -it <billing-service-pod> -- curl http://localhost:8082/actuator/health
```

### Database connectivity issues

```bash
# Check PostgreSQL pod
kubectl get pods -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-postgres

# Check PostgreSQL logs
kubectl logs -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-postgres

# Test database connection from billing service pod
kubectl exec -n iqscaffold-dev-env -it <billing-service-pod> -- \
  curl -v telnet://billing-postgres:5432
```

### Redis connectivity issues

```bash
# Check Redis pod
kubectl get pods -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-redis

# Test Redis connection
kubectl exec -n iqscaffold-dev-env -it <billing-service-pod> -- \
  curl -v telnet://billing-redis:6379
```

### RabbitMQ connectivity issues

```bash
# Check RabbitMQ pod
kubectl get pods -n iqscaffold-dev-env -l app.kubernetes.io/name=billing-rabbitmq

# Access RabbitMQ management UI
kubectl port-forward -n iqscaffold-dev-env svc/billing-rabbitmq 15672:15672
# Open http://localhost:15672 in browser
```

## Architecture

The billing service follows a multi-tenant architecture with:

- **Public Schema**: Stores subscription plans (shared across all tenants)
- **Tenant Schemas**: Each tenant has a dedicated schema for subscriptions, invoices, payments, usage records

### Database Schema

```
public/
├── subscription_plans
└── ...

tenant_<tenant_id>/
├── subscriptions
├── invoices
├── payments
├── payment_methods
├── usage_records
└── billing_events
```

## Security

- All pods run as non-root user (UID 1001)
- Read-only root filesystem
- Network policies restrict traffic between services
- Secrets are base64-encoded (use external secret management for production)
- JWT-based authentication with User Service
- Rate limiting enabled by default

## Support

For issues and questions:
- GitHub Issues: https://github.com/iqscaffold/backend/issues
- Documentation: https://docs.iqscaffold.com
- Email: support@iqscaffold.com
