# Billing Service Runbook

## Overview

This runbook provides step-by-step procedures for common operational tasks, troubleshooting, and incident response for the Billing Service.

## Table of Contents

1. [Service Health Checks](#service-health-checks)
2. [Common Operations](#common-operations)
3. [Troubleshooting](#troubleshooting)
4. [Incident Response](#incident-response)
5. [Maintenance Procedures](#maintenance-procedures)
6. [Monitoring and Alerts](#monitoring-and-alerts)

---

## Service Health Checks

### Check Service Status

**Command**:
```bash
curl http://localhost:8082/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "rabbitmq": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Check Database Connectivity

**Command**:
```bash
curl http://localhost:8082/actuator/health/db
```

### Check Redis Connectivity

**Command**:
```bash
curl http://localhost:8082/actuator/health/redis
```

### Check RabbitMQ Connectivity

**Command**:
```bash
curl http://localhost:8082/actuator/health/rabbitmq
```

### View Metrics

**Command**:
```bash
curl http://localhost:8082/actuator/metrics
```

### View Prometheus Metrics

**Command**:
```bash
curl http://localhost:8082/actuator/prometheus
```

---

## Common Operations

### 1. Manual Subscription Creation

**Scenario**: Create a subscription for a tenant manually (e.g., enterprise contract).

**Steps**:

1. Get the tenant ID from the User Service
2. Identify the appropriate plan ID
3. Create the subscription via API:

```bash
curl -X POST http://localhost:8082/api/v1/admin/subscriptions \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant-uuid",
    "planId": 3,
    "startDate": "2024-12-09T00:00:00Z",
    "skipTrial": true,
    "paymentProvider": "MANUAL"
  }'
```

4. Verify subscription creation:

```bash
curl http://localhost:8082/api/v1/admin/subscriptions/{subscriptionId} \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2. Extend Trial Period

**Scenario**: Customer requests trial extension.

**Steps**:

1. Locate the subscription ID
2. Extend the trial:

```bash
curl -X POST http://localhost:8082/api/v1/admin/subscriptions/{id}/extend-trial \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "additionalDays": 7,
    "reason": "Customer request - evaluating advanced features"
  }'
```

3. Verify the new trial end date
4. Notify the customer

### 3. Force Cancel Subscription

**Scenario**: Cancel subscription immediately (e.g., policy violation, fraud).

**Steps**:

1. Locate the subscription ID
2. Force cancel:

```bash
curl -X POST http://localhost:8082/api/v1/admin/subscriptions/{id}/cancel \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cancelImmediately": true,
    "reason": "Policy violation",
    "refundAmount": 0.00
  }'
```

3. Verify subscription status is CANCELED
4. Document the reason in the audit log

### 4. Manual Payment Recording

**Scenario**: Record a manual payment (e.g., wire transfer, check).

**Steps**:

1. Locate the invoice ID
2. Record the payment:

```bash
curl -X POST http://localhost:8082/api/v1/admin/payments/manual \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": 789,
    "amount": 49.00,
    "currency": "USD",
    "paymentMethod": "WIRE_TRANSFER",
    "transactionReference": "WT-2024-12345",
    "notes": "Wire transfer received on 2024-12-09"
  }'
```

3. Verify invoice status is PAID
4. Send receipt to customer

### 5. Void Invoice

**Scenario**: Cancel an invoice that was issued in error.

**Steps**:

1. Verify invoice is not yet paid
2. Void the invoice:

```bash
curl -X POST http://localhost:8082/api/v1/admin/invoices/{id}/void \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Issued in error - duplicate invoice"
  }'
```

3. Verify invoice status is VOID
4. Generate a new invoice if needed

### 6. Process Refund

**Scenario**: Issue a refund to a customer.

**Steps**:

1. Locate the payment ID
2. Process the refund:

```bash
curl -X POST http://localhost:8082/api/v1/payments/{id}/refund \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 49.00,
    "reason": "Customer dissatisfaction"
  }'
```

3. Verify refund status
4. Notify customer
5. Update subscription if needed

### 7. Reset Usage Counters

**Scenario**: Reset usage counters for a tenant (e.g., billing cycle change).

**Steps**:

1. Backup current usage data
2. Reset counters:

```bash
curl -X POST http://localhost:8082/api/v1/admin/usage/{tenantId}/reset \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "metricTypes": ["API_CALLS", "STORAGE_GB", "EMAIL_SENDS"],
    "reason": "Billing cycle adjustment"
  }'
```

3. Verify counters are reset
4. Document the action

### 8. Update Plan Pricing

**Scenario**: Update pricing for a subscription plan.

**Steps**:

1. Locate the plan ID
2. Update pricing:

```bash
curl -X PUT http://localhost:8082/api/v1/admin/plans/{id} \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "basePrice": 59.00,
    "effectiveDate": "2025-01-01T00:00:00Z"
  }'
```

3. Verify pricing update
4. Notify existing customers of price change
5. Update marketing materials

---

## Troubleshooting

### Issue: Payment Processing Failures

**Symptoms**:
- Payments failing with "payment_failed" error
- High rate of payment failures in logs

**Diagnosis**:

1. Check payment provider status:
```bash
curl https://status.stripe.com
```

2. Review recent payment failures:
```bash
curl http://localhost:8082/api/v1/admin/payments?status=FAILED&limit=50 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

3. Check payment provider credentials:
```bash
# Verify environment variables
echo $STRIPE_API_KEY
echo $STRIPE_WEBHOOK_SECRET
```

**Resolution**:

1. If provider is down, enable circuit breaker:
```bash
# Update configuration
curl -X POST http://localhost:8082/actuator/circuitbreaker/stripe/open \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

2. If credentials are invalid, update secrets:
```bash
# Update Kubernetes secret
kubectl create secret generic billing-secrets \
  --from-literal=stripe-api-key=$NEW_STRIPE_KEY \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart service
kubectl rollout restart deployment/billing-service
```

3. Retry failed payments:
```bash
curl -X POST http://localhost:8082/api/v1/admin/payments/retry-failed \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Issue: Quota Check Failures

**Symptoms**:
- Services reporting quota check errors
- High latency on quota check endpoints

**Diagnosis**:

1. Check Redis connectivity:
```bash
redis-cli -h localhost -p 6379 ping
```

2. Check Redis memory usage:
```bash
redis-cli -h localhost -p 6379 info memory
```

3. Review quota check logs:
```bash
kubectl logs -l app=billing-service --tail=100 | grep "quota.check"
```

**Resolution**:

1. If Redis is down, restart Redis:
```bash
kubectl rollout restart statefulset/redis
```

2. If Redis is out of memory, increase memory limit:
```bash
kubectl edit statefulset/redis
# Update memory limits
```

3. Clear quota cache if stale:
```bash
redis-cli -h localhost -p 6379 FLUSHDB
```

### Issue: Invoice Generation Failures

**Symptoms**:
- Invoices not being generated
- Scheduled job failures in logs

**Diagnosis**:

1. Check scheduled job status:
```bash
curl http://localhost:8082/actuator/scheduledtasks \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

2. Review invoice generation logs:
```bash
kubectl logs -l app=billing-service --tail=100 | grep "invoice.generation"
```

3. Check database connectivity:
```bash
psql -h localhost -U billing_user -d billing_db -c "SELECT 1"
```

**Resolution**:

1. Manually trigger invoice generation:
```bash
curl -X POST http://localhost:8082/api/v1/admin/invoices/generate-batch \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2024-12-01T00:00:00Z",
    "endDate": "2024-12-31T23:59:59Z"
  }'
```

2. If database connection issues, check connection pool:
```bash
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active
```

3. Restart service if needed:
```bash
kubectl rollout restart deployment/billing-service
```

### Issue: Webhook Processing Delays

**Symptoms**:
- Webhooks timing out
- Payment status not updating

**Diagnosis**:

1. Check RabbitMQ queue depth:
```bash
rabbitmqctl list_queues name messages
```

2. Review webhook processing logs:
```bash
kubectl logs -l app=billing-service --tail=100 | grep "webhook"
```

3. Check webhook consumer status:
```bash
curl http://localhost:8082/actuator/health/rabbitmq
```

**Resolution**:

1. Scale up webhook consumers:
```bash
kubectl scale deployment/billing-service --replicas=3
```

2. Purge dead letter queue if needed:
```bash
rabbitmqctl purge_queue billing.webhooks.dlq
```

3. Replay failed webhooks:
```bash
curl -X POST http://localhost:8082/api/v1/admin/webhooks/replay \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2024-12-09T00:00:00Z",
    "endDate": "2024-12-09T23:59:59Z"
  }'
```

---

## Incident Response

### Critical: Payment Provider Outage

**Severity**: P1 (Critical)

**Impact**: All payment processing stopped

**Response Steps**:

1. **Acknowledge** (within 5 minutes):
   - Post incident in #incidents Slack channel
   - Update status page: https://status.iqscaffold.com

2. **Assess** (within 15 minutes):
   - Check payment provider status page
   - Estimate duration of outage
   - Identify affected customers

3. **Mitigate** (within 30 minutes):
   - Enable circuit breaker for payment provider
   - Switch to backup payment provider if available
   - Queue failed payments for retry

4. **Communicate**:
   - Notify customers via email
   - Post updates every 30 minutes
   - Update status page

5. **Resolve**:
   - Verify payment provider is operational
   - Disable circuit breaker
   - Process queued payments
   - Verify all payments succeeded

6. **Post-Mortem**:
   - Document incident timeline
   - Identify improvements
   - Update runbook

### High: Database Connection Pool Exhausted

**Severity**: P2 (High)

**Impact**: Service degradation, slow responses

**Response Steps**:

1. **Immediate Action**:
```bash
# Check connection pool metrics
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active

# Increase connection pool size temporarily
kubectl set env deployment/billing-service \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50
```

2. **Identify Root Cause**:
```bash
# Check for long-running queries
psql -h localhost -U billing_user -d billing_db -c \
  "SELECT pid, now() - query_start as duration, query 
   FROM pg_stat_activity 
   WHERE state = 'active' 
   ORDER BY duration DESC;"
```

3. **Resolve**:
   - Kill long-running queries if needed
   - Optimize slow queries
   - Update connection pool configuration permanently

### Medium: High Memory Usage

**Severity**: P3 (Medium)

**Impact**: Potential service instability

**Response Steps**:

1. **Monitor**:
```bash
# Check memory usage
curl http://localhost:8082/actuator/metrics/jvm.memory.used

# Check heap dump
kubectl exec -it billing-service-pod -- jmap -heap 1
```

2. **Analyze**:
```bash
# Generate heap dump
kubectl exec -it billing-service-pod -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1

# Download and analyze with VisualVM or Eclipse MAT
```

3. **Mitigate**:
   - Increase memory limits if needed
   - Restart service to clear memory
   - Optimize memory-intensive operations

---

## Maintenance Procedures

### Database Backup

**Frequency**: Daily at 2:00 AM

**Procedure**:

1. Create backup:
```bash
pg_dump -h localhost -U billing_user -d billing_db \
  -F c -f billing_backup_$(date +%Y%m%d).dump
```

2. Verify backup:
```bash
pg_restore --list billing_backup_$(date +%Y%m%d).dump
```

3. Upload to S3:
```bash
aws s3 cp billing_backup_$(date +%Y%m%d).dump \
  s3://iqscaffold-backups/billing/
```

4. Verify upload:
```bash
aws s3 ls s3://iqscaffold-backups/billing/
```

### Database Migration

**Procedure**:

1. Backup database (see above)

2. Test migration in staging:
```bash
# Run Liquibase update
./mvnw liquibase:update -Dspring.profiles.active=staging
```

3. Verify migration:
```bash
# Check changelog
psql -h localhost -U billing_user -d billing_db -c \
  "SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;"
```

4. Deploy to production:
```bash
kubectl apply -f k8s/billing-service-deployment.yaml
```

5. Monitor for errors:
```bash
kubectl logs -f -l app=billing-service
```

### Cache Invalidation

**Procedure**:

1. Identify cache keys to invalidate:
```bash
redis-cli -h localhost -p 6379 KEYS "billing:*"
```

2. Invalidate specific cache:
```bash
redis-cli -h localhost -p 6379 DEL "billing:subscription:123"
```

3. Invalidate all billing caches:
```bash
redis-cli -h localhost -p 6379 EVAL \
  "return redis.call('del', unpack(redis.call('keys', 'billing:*')))" 0
```

### Log Rotation

**Procedure**:

1. Check log size:
```bash
du -sh /var/log/billing-service/
```

2. Rotate logs:
```bash
logrotate -f /etc/logrotate.d/billing-service
```

3. Verify rotation:
```bash
ls -lh /var/log/billing-service/
```

---

## Monitoring and Alerts

### Key Metrics to Monitor

1. **Payment Success Rate**
   - Metric: `billing.payments.success.rate`
   - Threshold: < 95%
   - Alert: P2

2. **Invoice Generation Success Rate**
   - Metric: `billing.invoices.generation.success.rate`
   - Threshold: < 99%
   - Alert: P2

3. **Quota Check Latency**
   - Metric: `billing.quota.check.latency.p99`
   - Threshold: > 100ms
   - Alert: P3

4. **Database Connection Pool Usage**
   - Metric: `hikaricp.connections.active`
   - Threshold: > 80%
   - Alert: P2

5. **Memory Usage**
   - Metric: `jvm.memory.used`
   - Threshold: > 85%
   - Alert: P3

6. **Error Rate**
   - Metric: `http.server.requests.errors`
   - Threshold: > 1%
   - Alert: P2

### Alert Configuration

**Prometheus Alert Rules**:

```yaml
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
          description: "Connection pool usage is {{ $value }}%"
```

### Dashboard Links

- **Grafana Dashboard**: http://grafana.iqscaffold.com/d/billing-service
- **Prometheus**: http://prometheus.iqscaffold.com
- **Kibana Logs**: http://kibana.iqscaffold.com
- **Status Page**: https://status.iqscaffold.com

---

## Emergency Contacts

- **On-Call Engineer**: +1-555-0100 (PagerDuty)
- **Engineering Manager**: engineering-manager@iqscaffold.com
- **DevOps Team**: devops@iqscaffold.com
- **Stripe Support**: https://support.stripe.com
- **PayPal Support**: https://www.paypal.com/merchantsupport

---

## Useful Commands

### Service Management

```bash
# Restart service
kubectl rollout restart deployment/billing-service

# Scale service
kubectl scale deployment/billing-service --replicas=3

# View logs
kubectl logs -f -l app=billing-service

# Execute command in pod
kubectl exec -it billing-service-pod -- /bin/bash
```

### Database Queries

```bash
# Count active subscriptions
psql -h localhost -U billing_user -d billing_db -c \
  "SELECT COUNT(*) FROM subscriptions WHERE status = 'ACTIVE';"

# List recent payments
psql -h localhost -U billing_user -d billing_db -c \
  "SELECT * FROM payments ORDER BY created_at DESC LIMIT 10;"

# Check invoice status distribution
psql -h localhost -U billing_user -d billing_db -c \
  "SELECT status, COUNT(*) FROM invoices GROUP BY status;"
```

### Redis Commands

```bash
# Check quota cache
redis-cli -h localhost -p 6379 GET "billing:quota:tenant-uuid:API_CALLS"

# List all billing keys
redis-cli -h localhost -p 6379 KEYS "billing:*"

# Check cache TTL
redis-cli -h localhost -p 6379 TTL "billing:subscription:123"
```

---

## Version History

- **v1.0** (2024-12-09): Initial runbook creation
- **v1.1** (TBD): Add disaster recovery procedures
- **v1.2** (TBD): Add performance tuning guide
