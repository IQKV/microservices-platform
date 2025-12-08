# Billing Service Alerting Guide

This document describes the alerting configuration for the IQ Scaffold Billing Service, including alert definitions, thresholds, notification channels, and response procedures.

## Table of Contents

- [Overview](#overview)
- [Alert Categories](#alert-categories)
- [Critical Alerts](#critical-alerts)
- [Warning Alerts](#warning-alerts)
- [Alert Configuration](#alert-configuration)
- [Notification Channels](#notification-channels)
- [Response Procedures](#response-procedures)
- [Alert Tuning](#alert-tuning)

## Overview

The Billing Service implements comprehensive alerting to ensure:
- **Payment processing reliability** - Detect and respond to payment failures quickly
- **Service availability** - Monitor service health and dependencies
- **Performance monitoring** - Track API response times and throughput
- **Business metrics** - Monitor trial conversions, churn, and revenue indicators

### Alert Severity Levels

| Severity | Response Time | Notification Channels | Examples |
|----------|--------------|----------------------|----------|
| **Critical** | Immediate (< 5 min) | Email, Slack, PagerDuty | Service down, payment failures > 5% |
| **Warning** | Within 1 hour | Email, Slack | High response time, low cache hit rate |

## Alert Categories

### 1. Payment Alerts
Monitor payment processing health and payment provider integration.

### 2. API Health Alerts
Monitor API availability, error rates, and response times.

### 3. Database Alerts
Monitor database connectivity, connection pool, and query performance.

### 4. Infrastructure Alerts
Monitor Redis, RabbitMQ, and other infrastructure dependencies.

### 5. Business Metrics Alerts
Monitor trial conversions, churn rates, and revenue indicators.

## Critical Alerts

### 1. High Payment Failure Rate

**Alert Name**: `HighPaymentFailureRate`

**Condition**: Payment failure rate > 5% over 5 minutes

**Impact**: Revenue loss, customer dissatisfaction, potential payment provider issues

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check Grafana dashboard for payment failure details
2. Review payment provider status pages (Stripe, PayPal)
3. Check application logs for payment errors
4. Verify payment provider API credentials
5. Check for network connectivity issues
6. If provider issue: Enable fallback to manual processing
7. If application issue: Roll back recent changes
8. Notify finance team of potential revenue impact

**Runbook**: https://docs.iqscaffold.com/runbooks/high-payment-failure-rate

---

### 2. High API Error Rate

**Alert Name**: `HighAPIErrorRate`

**Condition**: API error rate (5xx responses) > 1% over 5 minutes

**Impact**: Service degradation, customer impact, potential data loss

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check service health dashboard
2. Review application logs for error patterns
3. Check database connectivity and performance
4. Verify Redis and RabbitMQ connectivity
5. Check for recent deployments or configuration changes
6. Scale service if resource exhaustion
7. Roll back if recent deployment caused issue
8. Implement circuit breaker if external dependency failing

**Runbook**: https://docs.iqscaffold.com/runbooks/high-api-error-rate

---

### 3. High Database Connection Pool Usage

**Alert Name**: `HighDatabaseConnectionPoolUsage`

**Condition**: Connection pool usage > 80% for 5 minutes

**Impact**: Connection exhaustion, request timeouts, service degradation

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check active database connections
2. Identify long-running queries
3. Check for connection leaks in application
4. Review recent code changes for connection handling
5. Increase connection pool size if needed
6. Optimize slow queries
7. Restart service if connection leak suspected
8. Scale database if resource exhaustion

**Runbook**: https://docs.iqscaffold.com/runbooks/high-db-connection-pool

---

### 4. Webhook Processing Failures

**Alert Name**: `WebhookProcessingFailures`

**Condition**: Webhook processing failures > 0 for 2 minutes

**Impact**: Payment events lost, subscription state inconsistency, revenue tracking issues

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check webhook processing logs
2. Verify webhook signature validation
3. Check payment provider webhook configuration
4. Review dead letter queue for failed webhooks
5. Manually replay failed webhooks if needed
6. Verify database connectivity
7. Check for application errors in webhook handlers
8. Contact payment provider if webhook format changed

**Runbook**: https://docs.iqscaffold.com/runbooks/webhook-processing-failures

---

### 5. Service Health Check Failure

**Alert Name**: `ServiceHealthCheckFailure`

**Condition**: Service health check fails for 1 minute

**Impact**: Service unavailable, all billing operations blocked

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check service status in Kubernetes/Docker
2. Review service logs for startup errors
3. Verify database connectivity
4. Check Redis connectivity
5. Check RabbitMQ connectivity
6. Verify configuration is correct
7. Check for resource exhaustion (CPU, memory)
8. Restart service if hung
9. Scale service if resource exhaustion
10. Notify all teams of service outage

**Runbook**: https://docs.iqscaffold.com/runbooks/service-down

---

### 6. Service Health Indicator Unhealthy

**Alert Name**: `ServiceHealthIndicatorUnhealthy`

**Condition**: Health indicator reports unhealthy for 2 minutes

**Impact**: Partial service degradation, specific features unavailable

**Notification**: Email, Slack, PagerDuty

**Response Procedure**:
1. Check which health indicator is failing (database, Redis, RabbitMQ)
2. Verify connectivity to failing dependency
3. Check dependency health and logs
4. Restart dependency if needed
5. Verify network connectivity
6. Check for configuration issues
7. Implement circuit breaker if dependency consistently failing

**Runbook**: https://docs.iqscaffold.com/runbooks/health-indicator-unhealthy

## Warning Alerts

### 1. High Response Time

**Alert Name**: `HighResponseTime`

**Condition**: P95 response time > 500ms for 10 minutes

**Impact**: Poor user experience, potential timeout issues

**Notification**: Email, Slack

**Response Procedure**:
1. Check Grafana dashboard for slow endpoints
2. Review application performance metrics
3. Check database query performance
4. Verify cache hit rates
5. Check for N+1 query problems
6. Review recent code changes
7. Optimize slow queries
8. Increase cache TTL if appropriate
9. Scale service if resource exhaustion

**Runbook**: https://docs.iqscaffold.com/runbooks/high-response-time

---

### 2. Low Cache Hit Rate

**Alert Name**: `LowCacheHitRate`

**Condition**: Cache hit rate < 70% for 15 minutes

**Impact**: Increased database load, slower response times

**Notification**: Email, Slack

**Response Procedure**:
1. Check cache metrics in Grafana
2. Verify Redis connectivity and health
3. Check cache key patterns
4. Review cache TTL settings
5. Check for cache eviction issues
6. Verify cache warming is working
7. Increase cache size if needed
8. Adjust TTL settings if appropriate

**Runbook**: https://docs.iqscaffold.com/runbooks/low-cache-hit-rate

---

### 3. High Message Queue Depth

**Alert Name**: `HighMessageQueueDepth`

**Condition**: Queue depth > 1000 messages for 10 minutes

**Impact**: Processing lag, delayed notifications, potential message loss

**Notification**: Email, Slack

**Response Procedure**:
1. Check RabbitMQ management UI
2. Identify which queue is backing up
3. Check consumer health and processing rate
4. Verify consumer is running
5. Check for consumer errors in logs
6. Scale consumers if needed
7. Increase consumer concurrency
8. Check for slow message processing
9. Verify database and external service performance

**Runbook**: https://docs.iqscaffold.com/runbooks/high-queue-depth

---

### 4. Trial Conversion Rate Drop

**Alert Name**: `TrialConversionRateDrop`

**Condition**: Trial conversion rate drops > 20% compared to 24h ago

**Impact**: Revenue impact, potential product or pricing issues

**Notification**: Email, Slack

**Response Procedure**:
1. Check business metrics dashboard
2. Review recent product changes
3. Check for payment processing issues
4. Verify trial expiration emails are sending
5. Review pricing changes
6. Check for user experience issues
7. Analyze trial user behavior
8. Notify product team for investigation
9. Consider A/B test rollback if recent change

**Runbook**: https://docs.iqscaffold.com/runbooks/trial-conversion-drop

---

### 5. High Payment Retry Rate

**Alert Name**: `HighPaymentRetryRate`

**Condition**: Payment retry rate > 0.1 retries/sec for 15 minutes

**Impact**: Potential payment provider issues, customer card issues

**Notification**: Email, Slack

**Response Procedure**:
1. Check payment retry metrics
2. Review payment failure reasons
3. Check payment provider status
4. Verify retry schedule is appropriate
5. Check for card expiration issues
6. Review dunning email effectiveness
7. Consider adjusting retry schedule
8. Notify finance team of retry patterns

**Runbook**: https://docs.iqscaffold.com/runbooks/high-payment-retry-rate

---

### 6. Slow Invoice Generation

**Alert Name**: `SlowInvoiceGeneration`

**Condition**: P95 invoice generation time > 5 seconds for 10 minutes

**Impact**: Delayed invoicing, poor user experience

**Notification**: Email, Slack

**Response Procedure**:
1. Check invoice generation metrics
2. Review PDF generation performance
3. Check for large invoices with many line items
4. Verify database query performance
5. Check for external service delays
6. Optimize invoice generation logic
7. Consider async PDF generation
8. Scale PDF generation workers

**Runbook**: https://docs.iqscaffold.com/runbooks/slow-invoice-generation

---

### 7. Redis Connection Issues

**Alert Name**: `RedisConnectionIssues`

**Condition**: No active Redis connections for 2 minutes

**Impact**: Caching unavailable, increased database load

**Notification**: Email, Slack

**Response Procedure**:
1. Check Redis server health
2. Verify network connectivity
3. Check Redis logs for errors
4. Verify Redis configuration
5. Restart Redis if needed
6. Check for connection pool exhaustion
7. Verify application Redis client configuration

**Runbook**: https://docs.iqscaffold.com/runbooks/redis-connection-issues

---

### 8. RabbitMQ Connection Issues

**Alert Name**: `RabbitMQConnectionIssues`

**Condition**: No active RabbitMQ connections for 2 minutes

**Impact**: Async processing unavailable, event publishing blocked

**Notification**: Email, Slack

**Response Procedure**:
1. Check RabbitMQ server health
2. Verify network connectivity
3. Check RabbitMQ logs for errors
4. Verify RabbitMQ configuration
5. Restart RabbitMQ if needed
6. Check for connection pool exhaustion
7. Verify application RabbitMQ client configuration

**Runbook**: https://docs.iqscaffold.com/runbooks/rabbitmq-connection-issues

## Alert Configuration

### Prometheus Alert Rules

Alert rules are defined in `docker/observability/prometheus/billing-alerts.yml`.

### Alertmanager Configuration

Alertmanager routing and notification configuration is in `docker/observability/alertmanager/alertmanager.yml`.

### Environment Variables

Configure notification channels via environment variables:

```bash
# SMTP for email alerts
SMTP_HOST=smtp.gmail.com:587
SMTP_FROM=alerts@iqscaffold.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Slack webhook
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# PagerDuty integration
PAGERDUTY_SERVICE_KEY=your-service-key

# Alert recipients
ALERT_EMAIL_CRITICAL=ops-critical@iqscaffold.com
ALERT_EMAIL_WARNING=ops-warning@iqscaffold.com
ALERT_EMAIL_PAYMENT=finance@iqscaffold.com
ALERT_EMAIL_BUSINESS=product@iqscaffold.com
```

## Notification Channels

### Email
- **Critical alerts**: Immediate email to ops team
- **Warning alerts**: Email to ops team (less frequent)
- **Payment alerts**: Email to finance and ops teams
- **Business alerts**: Email to product and ops teams

### Slack
- **Channel**: `#billing-alerts-critical` for critical alerts
- **Channel**: `#billing-alerts-warning` for warning alerts
- **Format**: Formatted messages with severity, summary, and runbook links

### PagerDuty
- **Critical alerts only**: Pages on-call engineer
- **Escalation**: Follows PagerDuty escalation policy
- **Acknowledgment**: Required within 5 minutes

## Response Procedures

### General Response Workflow

1. **Acknowledge**: Acknowledge alert in PagerDuty/Slack
2. **Assess**: Check Grafana dashboards for context
3. **Investigate**: Review logs and metrics
4. **Mitigate**: Take immediate action to restore service
5. **Resolve**: Fix root cause
6. **Document**: Update runbook with findings
7. **Post-mortem**: Conduct post-mortem for critical incidents

### Escalation Path

1. **Level 1**: On-call engineer (immediate response)
2. **Level 2**: Senior engineer (if not resolved in 30 minutes)
3. **Level 3**: Engineering manager (if not resolved in 1 hour)
4. **Level 4**: CTO (for major outages)

### Communication

- **Internal**: Update Slack channel with status
- **External**: Update status page if customer-facing
- **Stakeholders**: Notify finance/product teams for business impact

## Alert Tuning

### Adjusting Thresholds

Alert thresholds should be tuned based on:
- Historical baseline metrics
- Business requirements
- False positive rate
- Mean time to detect (MTTD)

### Adding New Alerts

1. Define alert in `billing-alerts.yml`
2. Test alert with sample data
3. Document in this guide
4. Create runbook
5. Deploy and monitor

### Removing Alerts

1. Analyze alert history and value
2. Document reason for removal
3. Update this guide
4. Remove from `billing-alerts.yml`

### Alert Fatigue Prevention

- Review alert frequency monthly
- Tune thresholds to reduce false positives
- Consolidate related alerts
- Use inhibition rules to suppress redundant alerts
- Implement auto-remediation where possible

## Metrics Reference

### Payment Metrics
- `billing_payment_attempts_total` - Total payment attempts
- `billing_payment_failures_total` - Total payment failures
- `billing_payment_retries_total` - Total payment retries

### API Metrics
- `http_server_requests_seconds_count` - HTTP request count
- `http_server_requests_seconds_bucket` - HTTP request duration histogram

### Database Metrics
- `hikaricp_connections_active` - Active database connections
- `hikaricp_connections_max` - Maximum database connections

### Cache Metrics
- `cache_gets_total{result="hit"}` - Cache hits
- `cache_gets_total` - Total cache gets

### Queue Metrics
- `rabbitmq_queue_messages` - Messages in queue
- `rabbitmq_connections` - Active RabbitMQ connections

### Business Metrics
- `billing_trial_conversions_total` - Trial to paid conversions
- `billing_invoice_generation_duration_seconds` - Invoice generation time

## References

- [Prometheus Alerting Documentation](https://prometheus.io/docs/alerting/latest/overview/)
- [Alertmanager Configuration](https://prometheus.io/docs/alerting/latest/configuration/)
- [Grafana Dashboards](./dashboards/)
- [Runbook Repository](https://docs.iqscaffold.com/runbooks/)
