# Structured Logging Configuration

This document describes the structured logging setup for the IQ Scaffold Billing Service.

## Overview

The billing service uses structured JSON logging with the Logstash Logback Encoder to provide:
- Consistent log format across all environments
- Correlation ID tracking for distributed tracing
- Automatic context enrichment (tenant ID, user ID, request details)
- Performance monitoring and business event tracking
- Integration with observability tools (Loki, Grafana, etc.)

## Configuration Files

### logback-spring.xml
Main logging configuration with:
- JSON format using Logstash Logback Encoder
- Profile-specific configurations (local vs production)
- Async appenders for performance
- Rolling file appenders for production
- Structured field mapping

### application.yml
Logging level configuration and structured logging enablement:
```yaml
logging:
  level:
    root: INFO
    com.iqscaffold.billingservice: DEBUG
  config: classpath:logback-spring.xml

iqscaffold:
  observability:
    logging:
      format: json
      include-correlation-id: true
      include-trace-id: true
      enable-payment-events: true
      enable-billing-events: true
```

## Structured Logging Features

### 1. Correlation ID Tracking
Every request gets a correlation ID that's propagated through:
- HTTP headers (X-Correlation-ID, X-Request-ID)
- MDC context for all log entries
- Response headers for client tracking

### 2. Context Enrichment
Automatic addition of context fields:
- `correlationId`: Request correlation ID
- `requestId`: Unique request identifier
- `tenantId`: Multi-tenant context
- `userId`: Authenticated user context
- `method`: HTTP method
- `uri`: Request URI
- `remoteAddr`: Client IP address

### 3. Business Event Logging
Specialized loggers for different event types:
- **SECURITY**: Authentication, authorization events
- **PAYMENT**: Payment processing events
- **BILLING**: Billing operations (invoices, subscriptions)
- **PERFORMANCE**: Operation timing and metrics

### 4. Automatic Logging Aspects
AOP-based automatic logging for:
- Service layer methods (performance monitoring)
- Repository layer methods (database operation tracking)
- Custom annotations for business events

## Usage Examples

### Manual Structured Logging

```java
import com.iqscaffold.billingservice.config.LoggingConfiguration.StructuredLogger;

// Log payment events
StructuredLogger.logPaymentEvent(
    "payment_successful", 
    paymentId, 
    tenantId,
    Map.of("amount", amount, "currency", "USD")
);

// Log security events
StructuredLogger.logSecurityEvent(
    "login_attempt", 
    userId, 
    tenantId, 
    Map.of("success", true, "method", "jwt")
);

// Log billing events
StructuredLogger.logBillingEvent(
    "invoice_generated", 
    invoiceId, 
    tenantId,
    Map.of("amount", total, "items", itemCount)
);
```

### Annotation-Based Logging

```java
@LogPerformance(operation = "process-payment", logArgs = true)
@LogBusinessEvent(eventType = "payment_processing", description = "Processing customer payment")
public PaymentResult processPayment(PaymentRequest request) {
    // Method implementation
    return result;
}
```

### Automatic Service Logging
All service and repository methods are automatically logged with:
- Method entry/exit
- Execution time
- Error handling
- Slow operation warnings

## Log Structure

### Standard Log Entry
```json
{
  "@timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "message": "Payment successful notifications sent for payment: pay_123",
  "logger": "com.iqscaffold.billingservice.payment.PaymentNotificationService",
  "thread": "http-nio-8082-exec-1",
  "service": "iqscaffold-billing-service",
  "version": "1.0.0",
  "mdc": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "requestId": "req_123456",
    "tenantId": "tenant_abc",
    "userId": "user_789",
    "method": "POST",
    "uri": "/api/v1/billing/payments/process"
  }
}
```

### Payment Event Log Entry
```json
{
  "@timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "message": "Payment event: payment_successful - Payment ID: pay_123",
  "logger": "PAYMENT",
  "service": "iqscaffold-billing-service",
  "mdc": {
    "eventType": "payment",
    "paymentEvent": "payment_successful",
    "paymentId": "pay_123",
    "tenantId": "tenant_abc",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
  },
  "args": [
    {
      "amount": 99.99,
      "currency": "USD",
      "customerEmail": "customer@example.com",
      "paymentMethod": "card"
    }
  ]
}
```

### Performance Log Entry
```json
{
  "@timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "message": "Performance metric - Operation: payment-processing - Duration: 1250ms",
  "logger": "PERFORMANCE",
  "service": "iqscaffold-billing-service",
  "mdc": {
    "eventType": "performance",
    "operation": "payment-processing",
    "durationMs": "1250",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

## Environment-Specific Behavior

### Local Development
- Pretty-printed JSON for readability
- Console output only
- DEBUG level for application packages

### Staging/Production
- Compact JSON format
- Both console and file output
- Async processing for performance
- Log rotation and retention policies
- INFO level for most packages

## Integration with Observability Stack

### Loki Integration
Logs are automatically ingested by Promtail and sent to Loki with labels:
- `service`: iqscaffold-billing-service
- `environment`: local/staging/production
- `level`: log level
- `logger`: logger name

### Grafana Dashboards
Structured logs enable rich dashboards showing:
- Payment processing metrics
- Error rates by operation
- Performance trends
- Security events
- Business KPIs

### Alerting
Structured logs support alerting on:
- High error rates
- Slow operations (>1s)
- Security events
- Payment failures
- System health issues

## Best Practices

1. **Use Structured Loggers**: Prefer `StructuredLogger` methods over plain logging
2. **Include Context**: Always include tenant ID and relevant business identifiers
3. **Log Business Events**: Use specific event types for business operations
4. **Avoid Sensitive Data**: Never log passwords, tokens, or PII
5. **Use Correlation IDs**: Ensure correlation IDs flow through all operations
6. **Monitor Performance**: Log slow operations and set up alerts
7. **Consistent Naming**: Use consistent event names and field names

## Troubleshooting

### Common Issues

1. **Missing Correlation ID**: Ensure requests include X-Correlation-ID header
2. **Performance Impact**: Use async appenders in production
3. **Log Volume**: Adjust log levels for high-traffic environments
4. **Missing Context**: Verify MDC is properly set in async operations

### Debug Configuration

To enable debug logging for specific components:
```yaml
logging:
  level:
    com.iqscaffold.billingservice.payment: DEBUG
    PAYMENT: DEBUG
    PERFORMANCE: DEBUG
```

## Dependencies

Required dependencies for structured logging:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```