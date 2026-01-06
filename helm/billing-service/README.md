# IQ Scaffold Billing Service Helm Chart

This Helm chart deploys the IQ Scaffold Billing Service to a Kubernetes cluster.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PostgreSQL (can be deployed with this chart or use external instance)

## Installation

### 1. Add Required Helm Repositories

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2. Install Dependencies

```bash
helm dependency update
```

### 3. Deploy to Development

```bash
helm install billing-service . \
  --namespace iqscaffold-dev \
  --create-namespace
```

### 4. Deploy to Production

```bash
helm install billing-service . \
  --namespace iqscaffold-production \
  --create-namespace \
  --values values-production.yaml
```

## Configuration

### Required Secrets

Before deploying to production, create the required secrets:

```bash
kubectl create secret generic billing-service-secrets \
  --namespace iqscaffold-production \
  --from-literal=DATABASE_URL="jdbc:postgresql://postgresql:5432/iqscaffold_billing_production" \
  --from-literal=DATABASE_USERNAME="your_db_user" \
  --from-literal=DATABASE_PASSWORD="your_db_password" \
  --from-literal=STRIPE_API_KEY="sk_live_your_stripe_key" \
  --from-literal=STRIPE_PUBLIC_KEY="pk_live_your_stripe_key" \
  --from-literal=STRIPE_SECRET_KEY="sk_live_your_stripe_key" \
  --from-literal=STRIPE_WEBHOOK_SECRET="whsec_your_webhook_secret" \
  --from-literal=STRIPE_CLIENT_ID="ca_your_client_id" \
  --from-literal=STRIPE_CONNECT_CLIENT_ID="ca_your_connect_client_id" \
  --from-literal=SMTP_HOST="your_smtp_host" \
  --from-literal=SMTP_PORT="587" \
  --from-literal=SMTP_USERNAME="your_smtp_user" \
  --from-literal=SMTP_PASSWORD="your_smtp_password"
```

### Environment Variables

The following environment variables are configured through the Helm values:

#### Service Integration
- `IQSCAFFOLD_USER_SERVICE_URL`: URL of the user service
- `EMAIL_SERVICE_URL`: URL of the email service
- `JWT_ISSUER`: JWT token issuer

#### Payment Configuration
- `PAYMENT_PROVIDER`: Payment provider (stripe)
- `BILLING_SAAS_MODE`: Enable SaaS mode (true/false)

#### Email Configuration
- `EMAIL_FROM_EMAIL`: Default sender email address
- `EMAIL_FROM_NAME`: Default sender name
- `APP_BASE_URL`: Base URL for email links

#### Notification Configuration
- `BILLING_EMAIL_NOTIFICATIONS`: Enable email notifications
- `BILLING_WEBHOOK_NOTIFICATIONS`: Enable webhook notifications

### Customization

You can customize the deployment by creating your own values file:

```yaml
# my-values.yaml
replicaCount: 5

resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"

env:
  - name: EMAIL_FROM_EMAIL
    value: "billing@mycompany.com"
```

Then deploy with:

```bash
helm install billing-service . -f my-values.yaml
```

## Monitoring

The service exposes the following endpoints for monitoring:

- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/prometheus` - Prometheus metrics

## Upgrading

To upgrade the deployment:

```bash
helm upgrade billing-service . \
  --namespace iqscaffold-production \
  --values values-production.yaml
```

## Uninstalling

To uninstall the deployment:

```bash
helm uninstall billing-service --namespace iqscaffold-production
```

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -n iqscaffold-production -l app.kubernetes.io/name=billing-service
```

### View Logs

```bash
kubectl logs -n iqscaffold-production -l app.kubernetes.io/name=billing-service -f
```

### Check Configuration

```bash
kubectl describe configmap billing-service-config -n iqscaffold-production
kubectl describe secret billing-service-secrets -n iqscaffold-production
```

### Database Connection Issues

1. Check if PostgreSQL is running:
   ```bash
   kubectl get pods -n iqscaffold-production -l app.kubernetes.io/name=postgresql
   ```

2. Test database connectivity:
   ```bash
   kubectl exec -it deployment/billing-service -n iqscaffold-production -- \
     curl -f http://localhost:8082/actuator/health
   ```

### Stripe Configuration Issues

1. Verify Stripe secrets are properly set:
   ```bash
   kubectl get secret billing-service-secrets -n iqscaffold-production -o yaml
   ```

2. Check application logs for Stripe-related errors:
   ```bash
   kubectl logs -n iqscaffold-production -l app.kubernetes.io/name=billing-service | grep -i stripe
   ```