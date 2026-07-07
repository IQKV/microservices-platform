# IQ Key Value Platform — Observability Stack

Standalone observability stack for local development. Run this when developing
services with `mvn spring-boot:run` (services on the host, observability tools
in Docker). For the all-in-one demo use `compose.demo.yaml` at the repo root instead.

## Components

| Tool           | Purpose                      | Port                                           |
| -------------- | ---------------------------- | ---------------------------------------------- |
| **Jaeger**     | Distributed tracing          | 16686 (UI), 4317 (OTLP gRPC), 4318 (OTLP HTTP) |
| **Prometheus** | Metrics collection           | 9090                                           |
| **Grafana**    | Dashboards and visualization | 3000                                           |
| **Loki**       | Log aggregation              | 3100                                           |
| **Promtail**   | Log shipping agent           | —                                              |

## Quick Start

### 1. Start the observability stack

```bash
cd docker/observability
docker compose -f docker-compose.observability.yml up -d
```

### 2. Start the microservices on the host

Each service exposes its API on an even port and its actuator on the next odd port.

| Service | API port | Actuator port |
| ------- | -------- | ------------- |
| IAM     | 8080     | 8081          |
| Billing | 8082     | 8083          |
| Audit   | 8084     | 8085          |
| CMS     | 8086     | 8087          |
| Gateway | 8088     | 8089          |

```bash
# Terminal per service — adjust profile and port as needed
cd foundation-iam-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd foundation-billing-service
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8082 -Dmanagement.server.port=8083

cd foundation-audit-service
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8084 -Dmanagement.server.port=8085

cd foundation-cms-service
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8086 -Dmanagement.server.port=8087

cd foundation-gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8088 -Dmanagement.server.port=8089
```

### 3. Access the dashboards

| Tool       | URL                    | Credentials   |
| ---------- | ---------------------- | ------------- |
| Jaeger UI  | http://localhost:16686 | —             |
| Prometheus | http://localhost:9090  | —             |
| Grafana    | http://localhost:3000  | admin / admin |
| Loki       | http://localhost:3100  | —             |

## Monitoring Endpoints

All services expose actuator endpoints on their management port:

| Service | Health                                | Metrics                                   | Info                                |
| ------- | ------------------------------------- | ----------------------------------------- | ----------------------------------- |
| IAM     | http://localhost:8081/actuator/health | http://localhost:8081/actuator/prometheus | http://localhost:8081/actuator/info |
| Billing | http://localhost:8083/actuator/health | http://localhost:8083/actuator/prometheus | http://localhost:8083/actuator/info |
| Audit   | http://localhost:8085/actuator/health | http://localhost:8085/actuator/prometheus | http://localhost:8085/actuator/info |
| CMS     | http://localhost:8087/actuator/health | http://localhost:8087/actuator/prometheus | http://localhost:8087/actuator/info |
| Gateway | http://localhost:8089/actuator/health | http://localhost:8089/actuator/prometheus | http://localhost:8089/actuator/info |

## Configuration

### Environment Variables

| Variable                      | Default | Description                                    |
| ----------------------------- | ------- | ---------------------------------------------- |
| `GRAFANA_USER`                | `admin` | Grafana admin username                         |
| `GRAFANA_ADMIN_PASSWORD`      | `admin` | Grafana admin password                         |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | —       | Set in each service to `http://localhost:4317` |

### OpenTelemetry — wiring services to Jaeger

Add to each service's run command or `application-local.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0 # 100% in local dev

otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
```

### Tracing Sampling Rates

| Environment | Rate       |
| ----------- | ---------- |
| Local       | 1.0 (100%) |
| SIT / UAT   | 0.1 (10%)  |
| Production  | 0.01 (1%)  |

### Logging Levels by Environment

| Environment | Level | Format         |
| ----------- | ----- | -------------- |
| Local       | DEBUG | Human-readable |
| SIT / UAT   | INFO  | JSON           |
| Production  | WARN  | JSON           |

## Structured Log Fields

All services emit JSON logs with these fields:

| Field           | Description                                             |
| --------------- | ------------------------------------------------------- |
| `timestamp`     | ISO 8601 UTC                                            |
| `level`         | DEBUG / INFO / WARN / ERROR                             |
| `message`       | Log message                                             |
| `service`       | Service name (e.g. `foundation-iam-service`)            |
| `correlationId` | Request correlation ID (from `X-Correlation-ID` header) |
| `traceId`       | OpenTelemetry trace ID                                  |
| `spanId`        | OpenTelemetry span ID                                   |
| `userId`        | Authenticated user ID (when available)                  |
| `tenantId`      | Tenant key (when available)                             |
| `logger`        | Logger class name                                       |

## Troubleshooting

**Services not appearing in Prometheus**

- Confirm the service is running and actuator is reachable: `curl http://localhost:8081/actuator/prometheus`
- Check `prometheus.yml` scrape targets match actual actuator ports

**No traces in Jaeger**

- Verify `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317` is set for the service
- Confirm Jaeger is healthy: `docker logs foundation-jaeger`

**Missing logs in Loki**

- Check Promtail is running: `docker logs foundation-promtail`
- Verify log files exist at `/var/log/{slug}-service/*.log`
- Confirm `logback-spring.xml` writes to the expected path in local profile

**Grafana datasource errors**

- Ensure Prometheus and Loki containers are healthy before Grafana starts
- Re-run `docker compose -f docker-compose.observability.yml restart grafana`

## Production Considerations

1. **Security** — enable auth on Prometheus (`--web.enable-admin-api` + basic auth reverse proxy)
2. **Retention** — set `--storage.tsdb.retention.time` to match your SLA
3. **Alerting** — add Prometheus alerting rules and wire Alertmanager
4. **Backup** — snapshot Grafana dashboards and Loki chunks to object storage
5. **Sampling** — reduce OTEL sampling to 1–10% in production to control trace volume
