# Quick Start: Alerting Setup for IQ Scaffold Billing Service

This guide provides step-by-step instructions to set up alerting for the Billing Service.

## Prerequisites

- Docker and Docker Compose installed
- Billing Service running and exposing metrics on port 8082
- Access to email/Slack/PagerDuty for notifications

## Setup Steps

### 1. Configure Environment Variables

Copy the example environment file and configure with your credentials:

```bash
cd docker/observability
cp .env.alerting.example .env
```

Edit `.env` and configure:

```bash
# SMTP Configuration (Gmail example)
SMTP_HOST=smtp.gmail.com:587
SMTP_FROM=alerts@iqscaffold.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password  # Generate from Google Account settings

# Slack (optional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# PagerDuty (optional)
PAGERDUTY_SERVICE_KEY=your-service-key

# Alert Recipients
ALERT_EMAIL_CRITICAL=ops-critical@iqscaffold.com
ALERT_EMAIL_WARNING=ops-warning@iqscaffold.com
ALERT_EMAIL_PAYMENT=finance@iqscaffold.com,ops@iqscaffold.com
ALERT_EMAIL_BUSINESS=product@iqscaffold.com,ops@iqscaffold.com
```

### 2. Start Observability Stack

```bash
cd docker/observability
docker-compose -f docker-compose.observability.yml up -d
```

This starts:

- **Prometheus** (port 9090) - Metrics collection and alert evaluation
- **Alertmanager** (port 9093) - Alert routing and notifications
- **Grafana** (port 3000) - Dashboards and visualization
- **Loki** (port 3100) - Log aggregation
- **Promtail** - Log shipping
- **Jaeger** (port 16686) - Distributed tracing

### 3. Verify Services

```bash
# Check all services are running
docker-compose -f docker-compose.observability.yml ps

# Check Prometheus is scraping billing service
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.labels.job=="iqscaffold-billing-service")'

# Check alert rules are loaded
curl http://localhost:9090/api/v1/rules | jq '.data.groups[] | select(.name | contains("billing"))'

# Check Alertmanager is healthy
curl http://localhost:9093/-/healthy
```

### 4. Access UIs

- **Prometheus**: http://localhost:9090
  - View metrics and alerts
  - Test alert queries
- **Alertmanager**: http://localhost:9093
  - View active alerts
  - Manage silences
  - Check notification status

- **Grafana**: http://localhost:3000
  - Username: admin
  - Password: admin (or configured value)
  - View dashboards with alert annotations

### 5. Test Alerting

Send a test alert to verify the pipeline:

```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "TestAlert",
        "severity": "warning",
        "service": "billing-service",
        "category": "test"
      },
      "annotations": {
        "summary": "This is a test alert",
        "description": "Testing the alerting pipeline end-to-end"
      }
    }
  ]'
```

Check:

1. Alert appears in Alertmanager UI
2. Email notification received
3. Slack notification received (if configured)

### 6. Configure Slack (Optional)

1. Go to https://api.slack.com/messaging/webhooks
2. Create a new Slack app or use existing
3. Add Incoming Webhook integration
4. Create channels:
   - `#billing-alerts-critical`
   - `#billing-alerts-warning`
5. Copy webhook URL to `.env` file
6. Restart Alertmanager:
   ```bash
   docker-compose -f docker-compose.observability.yml restart alertmanager
   ```

### 7. Configure PagerDuty (Optional)

1. Create a PagerDuty service for billing alerts
2. Add Events API v2 integration
3. Copy integration key to `.env` file
4. Restart Alertmanager:
   ```bash
   docker-compose -f docker-compose.observability.yml restart alertmanager
   ```

## Alert Overview

### Critical Alerts (Immediate Response)

| Alert                        | Threshold     | Impact                |
| ---------------------------- | ------------- | --------------------- |
| High Payment Failure Rate    | > 5%          | Revenue loss          |
| High API Error Rate          | > 1%          | Service degradation   |
| High DB Connection Pool      | > 80%         | Connection exhaustion |
| Webhook Processing Failures  | > 0           | Event loss            |
| Service Health Check Failure | Down for 1min | Service unavailable   |

### Warning Alerts (Response within 1 hour)

| Alert                 | Threshold       | Impact            |
| --------------------- | --------------- | ----------------- |
| High Response Time    | P95 > 500ms     | Poor UX           |
| Low Cache Hit Rate    | < 70%           | Increased DB load |
| High Queue Depth      | > 1000 messages | Processing lag    |
| Trial Conversion Drop | > 20% drop      | Revenue impact    |

## Monitoring Alerts

### View Active Alerts

**Prometheus**: http://localhost:9090/alerts

**Alertmanager**: http://localhost:9093/#/alerts

### View Alert History

**Grafana**: http://localhost:3000

- Go to Explore
- Select Loki datasource
- Query: `{job="alertmanager"}`

### Check Notification Status

```bash
# View Alertmanager logs
docker logs iqscaffold-alertmanager

# Check for notification errors
docker logs iqscaffold-alertmanager 2>&1 | grep -i error

# View notification metrics
curl http://localhost:9093/metrics | grep alertmanager_notifications
```

## Silencing Alerts

### During Maintenance

```bash
# Silence all billing service alerts for 2 hours
docker exec -it iqscaffold-alertmanager \
  amtool silence add service=billing-service \
  --duration=2h \
  --comment="Maintenance window"
```

### Via UI

1. Go to http://localhost:9093
2. Click "Silences" → "New Silence"
3. Add matchers (e.g., `alertname=HighResponseTime`)
4. Set duration and comment
5. Click "Create"

## Troubleshooting

### Alerts Not Firing

1. Check Prometheus is scraping metrics:

   ```bash
   curl http://localhost:9090/api/v1/targets
   ```

2. Verify alert rules are loaded:

   ```bash
   curl http://localhost:9090/api/v1/rules
   ```

3. Check alert evaluation:
   ```bash
   curl http://localhost:9090/api/v1/alerts
   ```

### Notifications Not Sent

1. Check Alertmanager logs:

   ```bash
   docker logs iqscaffold-alertmanager
   ```

2. Verify SMTP credentials:

   ```bash
   docker exec -it iqscaffold-alertmanager cat /etc/alertmanager/alertmanager.yml
   ```

3. Test SMTP connection:
   ```bash
   telnet smtp.gmail.com 587
   ```

### Email Not Received

1. Check spam folder
2. Verify sender email is not blacklisted
3. Check SMTP logs in Alertmanager
4. Test with different email provider

## Next Steps

1. **Review Alert Thresholds**: Tune based on your baseline metrics
2. **Create Runbooks**: Document response procedures for each alert
3. **Set Up Dashboards**: Create Grafana dashboards with alert annotations
4. **Configure Escalation**: Set up PagerDuty escalation policies
5. **Test Regularly**: Run alert drills to verify the pipeline

## References

- [Prometheus Alerting](https://prometheus.io/docs/alerting/latest/overview/)
- [Alertmanager Configuration](https://prometheus.io/docs/alerting/latest/configuration/)
- [Billing Service Alerting Guide](../../iqscaffold-billing-service/grafana/ALERTING.md)
- [Alertmanager README](./alertmanager/README.md)
