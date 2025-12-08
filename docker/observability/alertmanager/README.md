# Alertmanager Configuration for IQ Scaffold Billing Service

This directory contains the Alertmanager configuration for the IQ Scaffold platform, specifically configured for the Billing Service alerts.

## Overview

Alertmanager handles alerts sent by Prometheus and routes them to the appropriate notification channels based on severity, category, and other labels.

## Architecture

```
Prometheus → Alertmanager → Notification Channels
                ↓
        (Email, Slack, PagerDuty)
```

## Alert Severity Levels

### Critical Alerts
- **Response Time**: 10 seconds group wait, 2 minutes group interval, 1 hour repeat
- **Notification Channels**: Email, Slack, PagerDuty
- **Examples**:
  - Payment failure rate > 5%
  - API error rate > 1%
  - Database connection pool > 80%
  - Webhook processing failures
  - Service health check failures

### Warning Alerts
- **Response Time**: 1 minute group wait, 10 minutes group interval, 12 hours repeat
- **Notification Channels**: Email, Slack
- **Examples**:
  - Response time p95 > 500ms
  - Cache hit rate < 70%
  - Message queue depth > 1000
  - Trial conversion rate drops > 20%

## Alert Categories

### Payment Alerts
- Routed to finance team and ops team
- Includes payment failures, retry rates, and payment provider issues

### Business Alerts
- Routed to product team and ops team
- Includes trial conversion rates, churn metrics, and revenue indicators

### Health Alerts
- Routed to ops team
- Includes service health, database, Redis, and RabbitMQ connectivity

### Performance Alerts
- Routed to ops team
- Includes API response times, invoice generation times, and throughput metrics

## Configuration

### Environment Variables

Configure the following environment variables in your `.env` file or docker-compose:

```bash
# SMTP Configuration
SMTP_HOST=smtp.gmail.com:587
SMTP_FROM=alerts@iqscaffold.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Slack Integration
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# PagerDuty Integration
PAGERDUTY_SERVICE_KEY=your-pagerduty-service-key

# Alert Recipients
ALERT_EMAIL_CRITICAL=ops-critical@iqscaffold.com
ALERT_EMAIL_WARNING=ops-warning@iqscaffold.com
ALERT_EMAIL_PAYMENT=finance@iqscaffold.com,ops@iqscaffold.com
ALERT_EMAIL_BUSINESS=product@iqscaffold.com,ops@iqscaffold.com
```

### SMTP Setup (Gmail Example)

1. Enable 2-factor authentication on your Gmail account
2. Generate an App Password:
   - Go to Google Account → Security → 2-Step Verification → App passwords
   - Select "Mail" and "Other (Custom name)"
   - Copy the generated password
3. Use the app password as `SMTP_PASSWORD`

### Slack Setup

1. Create a Slack App or use Incoming Webhooks
2. Add Incoming Webhook integration to your workspace
3. Create channels: `#billing-alerts-critical` and `#billing-alerts-warning`
4. Copy the webhook URL and set as `SLACK_WEBHOOK_URL`

### PagerDuty Setup

1. Create a PagerDuty service for billing alerts
2. Generate an integration key (Events API v2)
3. Set the integration key as `PAGERDUTY_SERVICE_KEY`

## Alert Routing

### Route Tree

```
All Alerts (default-receiver)
├── Critical Alerts → critical-alerts receiver
│   ├── Email (immediate)
│   ├── Slack (immediate)
│   └── PagerDuty (immediate)
├── Warning Alerts → warning-alerts receiver
│   ├── Email (delayed)
│   └── Slack (delayed)
├── Payment Alerts → payment-alerts receiver
│   └── Email (finance + ops)
└── Business Alerts → business-alerts receiver
    └── Email (product + ops)
```

### Inhibition Rules

Alertmanager suppresses certain alerts to reduce noise:

1. **Warning alerts suppressed when critical alert fires** for the same service
2. **Specific alerts suppressed when service is down** (e.g., API errors when health check fails)

## Alert Templates

Custom HTML email templates are located in `templates/default.tmpl`:

- Formatted HTML emails with color-coded severity
- Includes alert details, labels, annotations, and runbook links
- Responsive design for mobile viewing

## Testing Alerts

### Test Alert Firing

```bash
# Send a test alert to Alertmanager
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "TestAlert",
        "severity": "warning",
        "service": "billing-service"
      },
      "annotations": {
        "summary": "This is a test alert",
        "description": "Testing the alerting pipeline"
      }
    }
  ]'
```

### Verify Alert Routing

1. Access Alertmanager UI: http://localhost:9093
2. Check "Alerts" tab to see active alerts
3. Check "Silences" tab to manage alert silences
4. Check "Status" tab to verify receiver configuration

### Test Notification Channels

```bash
# Test email notification
docker exec -it iqscaffold-alertmanager \
  amtool alert add alertname=TestEmail severity=critical service=billing-service

# Check Alertmanager logs
docker logs iqscaffold-alertmanager
```

## Silencing Alerts

### Via UI

1. Go to http://localhost:9093
2. Click "Silences" → "New Silence"
3. Add matchers (e.g., `alertname=HighResponseTime`)
4. Set duration and comment
5. Click "Create"

### Via CLI

```bash
# Silence an alert for 2 hours
docker exec -it iqscaffold-alertmanager \
  amtool silence add alertname=HighResponseTime \
  --duration=2h \
  --comment="Maintenance window"

# List active silences
docker exec -it iqscaffold-alertmanager amtool silence query

# Expire a silence
docker exec -it iqscaffold-alertmanager amtool silence expire <silence-id>
```

## Runbooks

Each alert includes a runbook URL in the format:
```
https://docs.iqscaffold.com/runbooks/<alert-name-kebab-case>
```

Runbooks should include:
- Alert description and impact
- Troubleshooting steps
- Resolution procedures
- Escalation path
- Related documentation

## Monitoring Alertmanager

### Health Check

```bash
curl http://localhost:9093/-/healthy
```

### Metrics

Alertmanager exposes metrics at http://localhost:9093/metrics:

- `alertmanager_alerts` - Number of active alerts
- `alertmanager_notifications_total` - Total notifications sent
- `alertmanager_notifications_failed_total` - Failed notifications
- `alertmanager_silences` - Number of active silences

### Logs

```bash
# View Alertmanager logs
docker logs -f iqscaffold-alertmanager

# Search for notification errors
docker logs iqscaffold-alertmanager 2>&1 | grep -i error
```

## Troubleshooting

### Alerts Not Firing

1. Check Prometheus is scraping metrics: http://localhost:9090/targets
2. Verify alert rules are loaded: http://localhost:9090/rules
3. Check alert evaluation: http://localhost:9090/alerts
4. Verify Alertmanager connectivity in Prometheus config

### Notifications Not Sent

1. Check Alertmanager logs for errors
2. Verify SMTP/Slack/PagerDuty credentials
3. Test notification channels manually
4. Check inhibition rules aren't suppressing alerts
5. Verify receiver configuration in alertmanager.yml

### Email Delivery Issues

1. Verify SMTP credentials and host
2. Check spam folder
3. Verify sender email is not blacklisted
4. Test SMTP connection:
   ```bash
   telnet smtp.gmail.com 587
   ```

### Slack Notifications Not Appearing

1. Verify webhook URL is correct
2. Check Slack channel exists
3. Verify bot has permission to post
4. Test webhook manually:
   ```bash
   curl -X POST $SLACK_WEBHOOK_URL \
     -H 'Content-Type: application/json' \
     -d '{"text":"Test message"}'
   ```

## Best Practices

1. **Alert Fatigue**: Don't alert on everything - focus on actionable alerts
2. **Runbooks**: Always provide runbook links for quick resolution
3. **Severity**: Use appropriate severity levels (critical vs warning)
4. **Grouping**: Group related alerts to reduce notification spam
5. **Silences**: Use silences during maintenance windows
6. **Testing**: Regularly test alert pipeline end-to-end
7. **Documentation**: Keep runbooks up-to-date
8. **Review**: Periodically review and tune alert thresholds

## References

- [Prometheus Alerting](https://prometheus.io/docs/alerting/latest/overview/)
- [Alertmanager Configuration](https://prometheus.io/docs/alerting/latest/configuration/)
- [Alert Routing](https://prometheus.io/docs/alerting/latest/configuration/#route)
- [Notification Templates](https://prometheus.io/docs/alerting/latest/notifications/)
