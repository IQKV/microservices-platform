# Billing Service - Grafana Dashboards

This directory contains Grafana dashboard definitions for monitoring the Billing Service.

## Dashboards

### 1. Billing Service - Health & Performance

**File:** `dashboards/billing-service-health.json`

Monitors operational health and performance metrics:

- **API Response Time (p95)** - Response time percentiles by endpoint
- **Request Rate** - Requests per second by endpoint
- **Error Rate** - Percentage of 5xx errors
- **Cache Hit Rate** - Redis cache effectiveness
- **Database Connection Pool Usage** - HikariCP connection pool utilization
- **Message Queue Depth** - RabbitMQ queue depth

**Refresh Rate:** 10 seconds  
**Time Range:** Last 1 hour

### 2. Billing Service - Business Metrics

**File:** `dashboards/billing-business-metrics.json`

Tracks key business KPIs:

- **Monthly Recurring Revenue (MRR)** - Current MRR in USD
- **Annual Recurring Revenue (ARR)** - Current ARR in USD
- **Active Subscriptions** - Number of active subscriptions
- **Trial Subscriptions** - Number of trial subscriptions
- **Subscription Events** - Created, canceled, upgraded, downgraded rates
- **Trial Events** - Started, converted, expired rates
- **Churn Rate** - Percentage of canceled subscriptions
- **Trial Conversion Rate** - Percentage of trials converted to paid
- **Average Revenue Per User (ARPU)** - Average monthly revenue per subscription

**Refresh Rate:** 30 seconds  
**Time Range:** Last 24 hours

### 3. Billing Service - Payments & Invoices

**File:** `dashboards/billing-payments-invoices.json`

Monitors payment and invoice processing:

- **Payment Events** - Success, failed, refunded rates
- **Payment Failure Rate** - Percentage of failed payments
- **Payment Processing Time** - p95 and p99 latency
- **Invoice Events** - Generated, paid, voided rates
- **Invoice Generation Time** - p95 and p99 latency
- **Webhook Events** - Received, processed, failed rates
- **Webhook Processing Time** - p95 and p99 latency

**Refresh Rate:** 10 seconds  
**Time Range:** Last 1 hour

### 4. Billing Service - Usage & Analytics

**File:** `dashboards/billing-usage-analytics.json`

Tracks usage metering and analytics:

- **Usage Recording Rate** - Usage records created per second
- **Usage Recording Latency** - p95 and p99 latency
- **Quota Exceeded Events** - Rate of quota violations
- **Total Quota Exceeded** - Aggregate quota violations
- **Error Rates** - API, database, payment, and webhook errors

**Refresh Rate:** 10 seconds  
**Time Range:** Last 1 hour

## Metrics Exposed

The Billing Service exposes Prometheus metrics at `/actuator/prometheus`.

### Business Metrics

| Metric                                   | Type    | Description                        |
| ---------------------------------------- | ------- | ---------------------------------- |
| `billing_mrr`                            | Gauge   | Monthly Recurring Revenue in cents |
| `billing_arr`                            | Gauge   | Annual Recurring Revenue in cents  |
| `billing_subscriptions_active`           | Gauge   | Number of active subscriptions     |
| `billing_subscriptions_trial`            | Gauge   | Number of trial subscriptions      |
| `billing_subscriptions_canceled`         | Gauge   | Number of canceled subscriptions   |
| `billing_subscriptions_created_total`    | Counter | Total subscriptions created        |
| `billing_subscriptions_upgraded_total`   | Counter | Total subscriptions upgraded       |
| `billing_subscriptions_downgraded_total` | Counter | Total subscriptions downgraded     |
| `billing_subscriptions_canceled_total`   | Counter | Total subscriptions canceled       |
| `billing_trials_started_total`           | Counter | Total trials started               |
| `billing_trials_converted_total`         | Counter | Total trials converted to paid     |
| `billing_trials_expired_total`           | Counter | Total trials expired               |

### Payment Metrics

| Metric                                     | Type    | Description                 |
| ------------------------------------------ | ------- | --------------------------- |
| `billing_payments_success_total`           | Counter | Total successful payments   |
| `billing_payments_failed_total`            | Counter | Total failed payments       |
| `billing_payments_refunded_total`          | Counter | Total refunded payments     |
| `billing_payments_processing_time_seconds` | Timer   | Payment processing duration |

### Invoice Metrics

| Metric                                     | Type    | Description                 |
| ------------------------------------------ | ------- | --------------------------- |
| `billing_invoices_generated_total`         | Counter | Total invoices generated    |
| `billing_invoices_paid_total`              | Counter | Total invoices paid         |
| `billing_invoices_voided_total`            | Counter | Total invoices voided       |
| `billing_invoices_generation_time_seconds` | Timer   | Invoice generation duration |

### Usage Metrics

| Metric                                 | Type    | Description                 |
| -------------------------------------- | ------- | --------------------------- |
| `billing_usage_recorded_total`         | Counter | Total usage records created |
| `billing_quota_exceeded_total`         | Counter | Total quota exceeded events |
| `billing_usage_recording_time_seconds` | Timer   | Usage recording duration    |

### Webhook Metrics

| Metric                                     | Type    | Description                           |
| ------------------------------------------ | ------- | ------------------------------------- |
| `billing_webhooks_received_total`          | Counter | Total webhooks received               |
| `billing_webhooks_processed_total`         | Counter | Total webhooks processed successfully |
| `billing_webhooks_failed_total`            | Counter | Total webhook processing failures     |
| `billing_webhooks_processing_time_seconds` | Timer   | Webhook processing duration           |

### Error Metrics

| Metric                                 | Type    | Description                     |
| -------------------------------------- | ------- | ------------------------------- |
| `billing_errors_api_total`             | Counter | Total API errors                |
| `billing_errors_api_by_endpoint_total` | Counter | API errors by endpoint (tagged) |
| `billing_errors_database_total`        | Counter | Total database errors           |

### Operational Metrics

Standard Spring Boot Actuator metrics are also available:

- `http_server_requests_seconds` - HTTP request metrics
- `hikaricp_connections_*` - Database connection pool metrics
- `redis_cache_gets_total` - Redis cache metrics
- `rabbitmq_queue_messages` - RabbitMQ queue metrics
- `jvm_*` - JVM metrics (memory, threads, GC)

## Installation

### Import Dashboards to Grafana

1. **Via Grafana UI:**
   - Navigate to Dashboards → Import
   - Upload the JSON file or paste the JSON content
   - Select your Prometheus datasource
   - Click Import

2. **Via Provisioning:**
   - Copy dashboard JSON files to Grafana provisioning directory:
     ```bash
     cp dashboards/*.json /etc/grafana/provisioning/dashboards/
     ```
   - Restart Grafana to load the dashboards

3. **Via Docker Compose:**
   - Mount the dashboards directory in your Grafana container:
     ```yaml
     grafana:
       volumes:
         - ./iqscaffold-billing-service/grafana/dashboards:/etc/grafana/provisioning/dashboards
     ```

### Configure Prometheus Datasource

Ensure Grafana has a Prometheus datasource configured:

1. Navigate to Configuration → Data Sources
2. Add Prometheus datasource
3. Set URL to your Prometheus server (e.g., `http://prometheus:9090`)
4. Set name to `prometheus` (or update dashboard JSON files with your datasource name)
5. Click Save & Test

## Alerting

Consider setting up alerts for:

### Critical Alerts

- Payment failure rate > 5%
- API error rate > 1%
- Database connection pool > 80%
- Webhook processing failures
- Service health check failures

### Warning Alerts

- Response time p95 > 500ms
- Cache hit rate < 70%
- Message queue depth > 1000
- Trial conversion rate drops > 20%
- Churn rate increases > 10%

## Customization

### Modifying Dashboards

1. Edit the JSON files directly, or
2. Import to Grafana, make changes in UI, then export updated JSON

### Adding Custom Metrics

To add custom metrics to the Billing Service:

1. Inject `BillingMetrics` into your service
2. Call appropriate metric methods:

   ```java
   @Service
   @RequiredArgsConstructor
   public class MyService {

     private final BillingMetrics billingMetrics;

     public void myMethod() {
       billingMetrics.recordCounter("my_custom_metric", "tag1", "value1");
     }
   }
   ```

3. Update dashboard JSON to visualize the new metric

### Metric Tags

Use tags to add dimensions to metrics:

```java
billingMetrics.recordCounter("billing.custom.metric",
    "tenant", tenantId,
    "plan", planCode,
    "status", status
);
```

## Troubleshooting

### Metrics Not Appearing

1. Verify Prometheus is scraping the billing service:
   - Check Prometheus targets: `http://prometheus:9090/targets`
   - Ensure billing service is listed and UP

2. Verify metrics endpoint is accessible:

   ```bash
   curl http://billing-service:8082/actuator/prometheus
   ```

3. Check Grafana datasource connection:
   - Configuration → Data Sources → Prometheus → Test

### Dashboard Not Loading

1. Verify datasource UID matches in JSON:
   - Open JSON file
   - Check `datasource.uid` matches your Prometheus datasource

2. Check Grafana logs for errors:
   ```bash
   docker logs grafana
   ```

### Missing Data Points

1. Check metric retention in Prometheus configuration
2. Verify time range in dashboard matches data availability
3. Ensure billing service has been running long enough to generate metrics

## Best Practices

1. **Monitor Regularly** - Review dashboards daily for anomalies
2. **Set Up Alerts** - Configure alerts for critical metrics
3. **Baseline Metrics** - Establish normal ranges for key metrics
4. **Correlate Events** - Use annotations to mark deployments and incidents
5. **Optimize Queries** - Use recording rules for expensive queries
6. **Archive Old Data** - Configure Prometheus retention policies

## References

- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
