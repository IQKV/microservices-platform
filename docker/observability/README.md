# Gripday Platform Observability Stack

This directory contains the complete observability stack for the Gripday microservices platform, including distributed tracing, metrics collection, log aggregation, and visualization.

## Components

### OpenTelemetry & Jaeger
- **Jaeger**: Distributed tracing system for monitoring and troubleshooting microservices
- **Port**: 16686 (UI), 4317 (OTLP gRPC), 4318 (OTLP HTTP)
- **URL**: http://localhost:16686

### Prometheus
- **Purpose**: Metrics collection and monitoring
- **Port**: 9090
- **URL**: http://localhost:9090
- **Scrapes**: Auth service (8081), Gateway service (8080)

### Grafana
- **Purpose**: Dashboards and visualization
- **Port**: 3000
- **URL**: http://localhost:3000
- **Credentials**: admin/admin (default)

### Loki & Promtail
- **Loki**: Log aggregation system
- **Promtail**: Log shipping agent
- **Port**: 3100 (Loki)
- **URL**: http://localhost:3100

## Quick Start

1. **Start the observability stack**:
   ```bash
   cd docker/observability
   docker compose -f docker-compose.observability.yml up -d
   ```

2. **Start the microservices**:
   ```bash
   # Terminal 1 - Auth Service
   cd gripday-auth-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local

   # Terminal 2 - Gateway Service
   cd gripday-gateway-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the dashboards**:
   - Jaeger UI: http://localhost:16686
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000

## Monitoring Endpoints

### Auth Service (Port 8081)
- Health: http://localhost:8081/actuator/health
- Metrics: http://localhost:8081/actuator/prometheus
- Info: http://localhost:8081/actuator/info

### Gateway Service (Port 8080)
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
- Info: http://localhost:8080/actuator/info

## Configuration

### Environment Variables
- `OTEL_EXPORTER_OTLP_ENDPOINT`: OpenTelemetry collector endpoint
- `GRAFANA_USER`: Grafana admin username
- `GRAFANA_PASSWORD`: Grafana admin password

### Logging Levels by Environment
- **Local**: DEBUG level with human-readable format
- **Staging**: INFO level with JSON format
- **Production**: WARN level with JSON format

### Tracing Sampling Rates
- **Local**: 100% sampling (1.0)
- **Staging**: 10% sampling (0.1)
- **Production**: 1% sampling (0.01)

## Custom Metrics

### Auth Service Metrics
- `gripday_auth_authentication_duration`: Authentication request duration
- `gripday_auth_authentication_total`: Total authentication attempts (success/failure)
- `gripday_auth_registration_duration`: User registration duration
- `gripday_auth_registration_total`: Total registration attempts
- `gripday_auth_token_refresh_duration`: Token refresh duration
- `gripday_auth_token_refresh_total`: Total token refresh attempts

### Gateway Service Metrics
- `gripday_gateway_request_duration`: Gateway request processing time
- `gripday_gateway_request_total`: Total gateway requests
- `gripday_gateway_authentication_duration`: Authentication validation time
- `gripday_gateway_ratelimit_hit`: Rate limit violations
- `gripday_gateway_circuitbreaker_open`: Circuit breaker state changes

## Structured Logging

### Log Fields
- `timestamp`: ISO 8601 timestamp in UTC
- `level`: Log level (DEBUG, INFO, WARN, ERROR)
- `message`: Log message
- `service`: Service name (gripday-auth-service, gripday-gateway-service)
- `correlationId`: Request correlation ID
- `traceId`: OpenTelemetry trace ID
- `spanId`: OpenTelemetry span ID
- `userId`: Authenticated user ID (when available)
- `tenantId`: Tenant ID (when available)
- `logger`: Logger name

### Correlation ID Flow
1. Gateway generates correlation ID for incoming requests
2. Correlation ID propagated to downstream services via headers
3. All log entries include correlation ID for request tracing
4. Correlation ID returned in response headers

## Troubleshooting

### Common Issues
1. **Services not appearing in Prometheus**: Check if actuator endpoints are accessible
2. **No traces in Jaeger**: Verify OTEL_EXPORTER_OTLP_ENDPOINT configuration
3. **Missing logs in Loki**: Check Promtail configuration and log file paths
4. **Grafana datasource errors**: Ensure all services are running and accessible

### Health Checks
```bash
# Check observability stack health
docker compose -f docker-compose.observability.yml ps

# Check service health
curl http://localhost:8081/actuator/health  # Auth service
curl http://localhost:8080/actuator/health  # Gateway service

# Check metrics endpoints
curl http://localhost:8081/actuator/prometheus  # Auth metrics
curl http://localhost:8080/actuator/prometheus  # Gateway metrics
```

## Production Considerations

1. **Security**: Configure authentication for Grafana and Prometheus
2. **Storage**: Use persistent volumes for production data
3. **Retention**: Configure appropriate data retention policies
4. **Alerting**: Set up Prometheus alerting rules
5. **Backup**: Implement backup strategies for dashboards and configurations