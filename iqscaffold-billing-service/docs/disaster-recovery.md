# Billing Service Disaster Recovery Plan

## Overview

This document outlines disaster recovery procedures for the Billing Service to ensure business continuity in the event of catastrophic failures.

## Table of Contents

1. [Recovery Objectives](#recovery-objectives)
2. [Backup Strategy](#backup-strategy)
3. [Disaster Scenarios](#disaster-scenarios)
4. [Recovery Procedures](#recovery-procedures)
5. [Data Restoration](#data-restoration)
6. [Testing and Validation](#testing-and-validation)

---

## Recovery Objectives

### Recovery Time Objective (RTO)

**Target**: 4 hours

The maximum acceptable time to restore the billing service to operational status.

### Recovery Point Objective (RPO)

**Target**: 1 hour

The maximum acceptable amount of data loss measured in time.

### Service Level Objectives

- **Critical Operations** (RTO: 1 hour, RPO: 15 minutes):
  - Payment processing
  - Quota enforcement
  - Subscription status checks

- **Important Operations** (RTO: 4 hours, RPO: 1 hour):
  - Invoice generation
  - Usage tracking
  - Subscription management

- **Standard Operations** (RTO: 24 hours, RPO: 24 hours):
  - Analytics and reporting
  - Historical data queries

---

## Backup Strategy

### Database Backups

#### Automated Backups

**Frequency**: Every 6 hours

**Retention**:

- Hourly backups: 7 days
- Daily backups: 30 days
- Weekly backups: 90 days
- Monthly backups: 1 year

**Location**:

- Primary: AWS S3 (us-east-1)
- Secondary: AWS S3 (us-west-2)
- Tertiary: Azure Blob Storage (westus2)

**Backup Script**:

```bash
#!/bin/bash
# scripts/backup-database.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="billing_backup_${TIMESTAMP}.dump"

# Create backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -F c -f /tmp/$BACKUP_FILE

# Compress
gzip /tmp/$BACKUP_FILE

# Upload to S3 (primary)
aws s3 cp /tmp/${BACKUP_FILE}.gz \
  s3://iqscaffold-backups-primary/billing/

# Upload to S3 (secondary)
aws s3 cp /tmp/${BACKUP_FILE}.gz \
  s3://iqscaffold-backups-secondary/billing/ \
  --region us-west-2

# Upload to Azure (tertiary)
az storage blob upload \
  --account-name iqscaffoldbackups \
  --container-name billing \
  --name ${BACKUP_FILE}.gz \
  --file /tmp/${BACKUP_FILE}.gz

# Cleanup
rm /tmp/${BACKUP_FILE}.gz

# Verify backups
aws s3 ls s3://iqscaffold-backups-primary/billing/${BACKUP_FILE}.gz
aws s3 ls s3://iqscaffold-backups-secondary/billing/${BACKUP_FILE}.gz
```

#### Point-in-Time Recovery

PostgreSQL Write-Ahead Logging (WAL) enables point-in-time recovery:

**Configuration**:

```ini
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://iqscaffold-wal-archive/%f'
archive_timeout = 300  # 5 minutes
```

### Application Configuration Backups

**Frequency**: On every change

**Location**: Git repository + encrypted S3 bucket

**Backup Items**:

- Kubernetes manifests
- Helm charts
- Configuration files
- Secrets (encrypted)
- Environment variables

### Redis Backups

**Frequency**: Every hour

**Configuration**:

```conf
# redis.conf
save 900 1      # Save after 900 seconds if at least 1 key changed
save 300 10     # Save after 300 seconds if at least 10 keys changed
save 60 10000   # Save after 60 seconds if at least 10000 keys changed

dir /data/redis-backups
dbfilename billing-cache.rdb
```

### RabbitMQ Backups

**Frequency**: Daily

**Backup Items**:

- Queue definitions
- Exchange configurations
- Bindings
- Policies

**Backup Script**:

```bash
#!/bin/bash
# scripts/backup-rabbitmq.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Export definitions
rabbitmqadmin export /tmp/rabbitmq_definitions_${TIMESTAMP}.json

# Upload to S3
aws s3 cp /tmp/rabbitmq_definitions_${TIMESTAMP}.json \
  s3://iqscaffold-backups-primary/rabbitmq/
```

---

## Disaster Scenarios

### Scenario 1: Database Failure

**Impact**: Complete loss of billing data access

**Symptoms**:

- Database connection errors
- 500 errors on all endpoints
- Health checks failing

**Recovery Procedure**: See [Database Recovery](#database-recovery)

### Scenario 2: Complete Region Failure

**Impact**: All services in primary region unavailable

**Symptoms**:

- All health checks failing
- No response from any endpoint
- AWS region status page shows outage

**Recovery Procedure**: See [Region Failover](#region-failover)

### Scenario 3: Data Corruption

**Impact**: Corrupted billing data

**Symptoms**:

- Inconsistent data
- Calculation errors
- Failed transactions

**Recovery Procedure**: See [Data Restoration](#data-restoration)

### Scenario 4: Ransomware Attack

**Impact**: Encrypted or deleted data

**Symptoms**:

- Inaccessible data
- Ransom demands
- Unusual file modifications

**Recovery Procedure**: See [Security Incident Recovery](#security-incident-recovery)

### Scenario 5: Payment Provider Outage

**Impact**: Cannot process payments

**Symptoms**:

- Payment failures
- Timeout errors
- Provider status page shows outage

**Recovery Procedure**: See [Payment Provider Failover](#payment-provider-failover)

---

## Recovery Procedures

### Database Recovery

#### Step 1: Assess Damage

```bash
# Check database status
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT 1"

# Check for corruption
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "SELECT pg_database.datname, pg_size_pretty(pg_database_size(pg_database.datname))
   FROM pg_database;"
```

#### Step 2: Stop Application

```bash
# Scale down to prevent further damage
kubectl scale deployment/billing-service --replicas=0 --namespace=production
```

#### Step 3: Restore from Backup

```bash
# Download latest backup
aws s3 cp s3://iqscaffold-backups-primary/billing/billing_backup_latest.dump.gz \
  /tmp/

# Decompress
gunzip /tmp/billing_backup_latest.dump.gz

# Drop existing database (if needed)
psql -h $DB_HOST -U postgres -c "DROP DATABASE billing_db;"
psql -h $DB_HOST -U postgres -c "CREATE DATABASE billing_db;"

# Restore
pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  -c /tmp/billing_backup_latest.dump

# Verify restoration
psql -h $DB_HOST -U $DB_USER -d billing_db -c \
  "SELECT COUNT(*) FROM subscriptions;"
```

#### Step 4: Apply WAL for Point-in-Time Recovery

```bash
# Restore to specific point in time
pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  --recovery-target-time='2024-12-09 10:30:00' \
  /tmp/billing_backup_latest.dump
```

#### Step 5: Restart Application

```bash
# Scale up
kubectl scale deployment/billing-service --replicas=3 --namespace=production

# Monitor
kubectl logs -f -l app=billing-service --namespace=production
```

#### Step 6: Verify Recovery

```bash
# Run smoke tests
./scripts/smoke-tests.sh production

# Verify critical data
./scripts/verify-billing-data.sh
```

**Estimated Recovery Time**: 2-3 hours

### Region Failover

#### Step 1: Activate DR Region

```bash
# Switch DNS to DR region
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://dns-failover.json
```

#### Step 2: Restore Database in DR Region

```bash
# Download backup from secondary location
aws s3 cp s3://iqscaffold-backups-secondary/billing/billing_backup_latest.dump.gz \
  /tmp/ --region us-west-2

# Restore to DR database
pg_restore -h $DR_DB_HOST -U $DB_USER -d billing_db \
  /tmp/billing_backup_latest.dump
```

#### Step 3: Deploy Application in DR Region

```bash
# Deploy to DR cluster
kubectl config use-context dr-cluster

helm upgrade billing-service ./helm/billing-service \
  --namespace=production \
  --values=helm/billing-service/values-dr.yaml \
  --set image.tag=latest
```

#### Step 4: Verify DR Deployment

```bash
# Health checks
curl https://billing-dr.iqscaffold.com/actuator/health

# Smoke tests
./scripts/smoke-tests.sh dr
```

**Estimated Recovery Time**: 3-4 hours

### Data Restoration

#### Restore Specific Tables

```bash
# Restore only subscriptions table
pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  -t subscriptions \
  /tmp/billing_backup_20241209.dump
```

#### Restore Specific Tenant Data

```bash
# Restore tenant schema
pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  -n tenant_abc123 \
  /tmp/billing_backup_20241209.dump
```

#### Restore from Point in Time

```bash
# Restore to 1 hour ago
pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  --recovery-target-time='2024-12-09 09:30:00' \
  /tmp/billing_backup_20241209.dump
```

### Security Incident Recovery

#### Step 1: Isolate Systems

```bash
# Immediately isolate affected systems
kubectl delete networkpolicy allow-all --namespace=production

# Block all external traffic
kubectl apply -f k8s/network-policy-lockdown.yaml
```

#### Step 2: Assess Damage

```bash
# Check for unauthorized access
kubectl logs -l app=billing-service --since=24h | grep "401\|403"

# Check for data exfiltration
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceType,AttributeValue=AWS::S3::Object \
  --start-time 2024-12-08T00:00:00Z
```

#### Step 3: Restore from Clean Backup

```bash
# Restore from backup before incident
aws s3 cp s3://iqscaffold-backups-primary/billing/billing_backup_20241208.dump.gz \
  /tmp/

pg_restore -h $DB_HOST -U $DB_USER -d billing_db \
  -c /tmp/billing_backup_20241208.dump
```

#### Step 4: Rotate All Credentials

```bash
# Rotate database passwords
./scripts/rotate-database-credentials.sh

# Rotate API keys
./scripts/rotate-api-keys.sh

# Rotate JWT signing keys
./scripts/rotate-jwt-keys.sh
```

#### Step 5: Redeploy from Trusted Source

```bash
# Deploy from verified image
kubectl set image deployment/billing-service \
  billing-service=iqscaffold/billing-service:verified-${VERSION} \
  --namespace=production
```

### Payment Provider Failover

#### Step 1: Detect Outage

```bash
# Check provider status
curl https://status.stripe.com/api/v2/status.json

# Check recent payment failures
kubectl logs -l app=billing-service | grep "payment.failed" | tail -50
```

#### Step 2: Switch to Backup Provider

```bash
# Update configuration to use PayPal
kubectl set env deployment/billing-service \
  IQSCAFFOLD_BILLING_PAYMENT_PROVIDER=paypal \
  --namespace=production
```

#### Step 3: Retry Failed Payments

```bash
# Retry payments that failed during outage
curl -X POST https://billing.iqscaffold.com/api/v1/admin/payments/retry-failed \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "startDate": "2024-12-09T10:00:00Z",
    "endDate": "2024-12-09T11:00:00Z",
    "provider": "paypal"
  }'
```

#### Step 4: Monitor Recovery

```bash
# Monitor payment success rate
watch -n 5 'curl -s https://prometheus.iqscaffold.com/api/v1/query?query=billing_payments_success_rate'
```

---

## Testing and Validation

### DR Testing Schedule

- **Monthly**: Database restore test
- **Quarterly**: Full DR failover test
- **Annually**: Complete disaster recovery simulation

### Database Restore Test

```bash
#!/bin/bash
# scripts/test-database-restore.sh

echo "Starting database restore test..."

# Create test database
psql -h $DB_HOST -U postgres -c "CREATE DATABASE billing_db_test;"

# Download latest backup
aws s3 cp s3://iqscaffold-backups-primary/billing/billing_backup_latest.dump.gz \
  /tmp/test_restore.dump.gz

# Decompress
gunzip /tmp/test_restore.dump.gz

# Restore
pg_restore -h $DB_HOST -U $DB_USER -d billing_db_test \
  /tmp/test_restore.dump

# Verify data
SUBSCRIPTION_COUNT=$(psql -h $DB_HOST -U $DB_USER -d billing_db_test -t -c \
  "SELECT COUNT(*) FROM subscriptions;")

echo "Restored $SUBSCRIPTION_COUNT subscriptions"

# Cleanup
psql -h $DB_HOST -U postgres -c "DROP DATABASE billing_db_test;"

echo "Database restore test completed successfully"
```

### DR Failover Test

```bash
#!/bin/bash
# scripts/test-dr-failover.sh

echo "Starting DR failover test..."

# 1. Deploy to DR region
kubectl config use-context dr-cluster
helm upgrade billing-service ./helm/billing-service \
  --namespace=test \
  --values=helm/billing-service/values-dr.yaml

# 2. Restore database
./scripts/restore-database-dr.sh test

# 3. Run smoke tests
./scripts/smoke-tests.sh dr-test

# 4. Verify functionality
./scripts/verify-billing-functionality.sh dr-test

# 5. Cleanup
helm uninstall billing-service --namespace=test

echo "DR failover test completed successfully"
```

### Recovery Validation Checklist

After any recovery procedure:

- [ ] All pods running and healthy
- [ ] Database connectivity verified
- [ ] Redis connectivity verified
- [ ] RabbitMQ connectivity verified
- [ ] Health checks passing
- [ ] Smoke tests passing
- [ ] Payment processing working
- [ ] Subscription operations working
- [ ] Quota checks working
- [ ] Invoice generation working
- [ ] No data loss detected
- [ ] Metrics being collected
- [ ] Logs being generated
- [ ] Alerts configured
- [ ] Monitoring dashboards updated

---

## Communication Plan

### Incident Communication

**Internal**:

1. Post in #incidents Slack channel
2. Page on-call engineer
3. Notify engineering manager
4. Update status page

**External**:

1. Update status page: https://status.iqscaffold.com
2. Send email to affected customers
3. Post on Twitter/social media
4. Update support ticket system

### Status Page Updates

**Template**:

```
[INVESTIGATING] Billing Service Outage
Posted: 2024-12-09 10:30 UTC

We are currently investigating an issue with the Billing Service.
Payment processing and subscription operations may be affected.

We will provide updates every 30 minutes.

Next update: 2024-12-09 11:00 UTC
```

### Post-Incident Report

**Template**: See `docs/templates/post-incident-report.md`

**Required Sections**:

- Incident summary
- Timeline of events
- Root cause analysis
- Impact assessment
- Recovery actions taken
- Lessons learned
- Action items

---

## Contact Information

### Emergency Contacts

- **On-Call Engineer**: +1-555-0100 (PagerDuty)
- **Engineering Manager**: +1-555-0101
- **CTO**: +1-555-0102
- **DevOps Lead**: +1-555-0103

### Vendor Support

- **AWS Support**: 1-866-987-7638 (Enterprise Support)
- **Stripe Support**: https://support.stripe.com (Priority Support)
- **PayPal Support**: 1-888-221-1161 (Business Support)
- **PostgreSQL Support**: support@enterprisedb.com

### Internal Resources

- **Runbook**: docs/RUNBOOK.md
- **Deployment Guide**: docs/DEPLOYMENT.md
- **Architecture Docs**: docs/adr/
- **Monitoring**: https://grafana.iqscaffold.com
- **Logs**: https://kibana.iqscaffold.com

---

## Appendix

### Backup Verification Script

```bash
#!/bin/bash
# scripts/verify-backup.sh

BACKUP_FILE=$1

echo "Verifying backup: $BACKUP_FILE"

# Check file exists
if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: Backup file not found"
  exit 1
fi

# Check file size
SIZE=$(stat -f%z "$BACKUP_FILE")
if [ $SIZE -lt 1000000 ]; then
  echo "WARNING: Backup file is suspiciously small"
fi

# Test restore to temporary database
psql -h $DB_HOST -U postgres -c "CREATE DATABASE billing_db_verify;"

pg_restore -h $DB_HOST -U $DB_USER -d billing_db_verify \
  $BACKUP_FILE

# Verify data
TABLES=$(psql -h $DB_HOST -U $DB_USER -d billing_db_verify -t -c \
  "SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = 'public';")

echo "Restored $TABLES tables"

# Cleanup
psql -h $DB_HOST -U postgres -c "DROP DATABASE billing_db_verify;"

echo "Backup verification completed successfully"
```

### Recovery Time Log

| Date       | Scenario             | RTO Target | Actual RTO | Notes                                |
| ---------- | -------------------- | ---------- | ---------- | ------------------------------------ |
| 2024-12-01 | Database failure     | 4 hours    | 2.5 hours  | Successful recovery                  |
| 2024-11-15 | Region failover test | 4 hours    | 3.8 hours  | Within target                        |
| 2024-10-20 | Data corruption      | 4 hours    | 5.2 hours  | Exceeded target, improved procedures |

---

## Version History

- **v1.0** (2024-12-09): Initial disaster recovery plan
- **v1.1** (TBD): Add automated failover procedures
- **v1.2** (TBD): Add multi-region active-active setup
