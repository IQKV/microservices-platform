# Docker Configuration for Gripday Platform

This directory contains Docker configurations for the Gripday microservices platform.

## Quick Start

1. **Copy environment file:**

   ```bash
   cp .env.example .env
   ```

2. **Update environment variables in `.env` file**

3. **Start the platform:**

   ```bash
   # Make scripts executable (Unix/Linux/macOS)
   chmod +x docker/scripts/*.sh

   # Start all services
   ./docker/scripts/start-platform.sh
   ```

4. **Or use Docker Compose directly:**

   ```bash
   # Build and start all services
   docker compose up -d --build

   # View logs
   docker-compose logs -f

   # Stop services
   docker-compose down
   ```

## Service URLs

- **Gateway Service:** http://localhost:8080
- **User Service:** http://localhost:8080
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

## Environment-Specific Deployments

### Staging

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

### Production

```bash
docker compose -f docker-compose.yml -f docker-compose.production.yml up -d
```

## Individual Service Development

### User Service Only

```bash
cd gripday-user-service
docker compose up -d
```

### Gateway Service Only

```bash
cd gripday-gateway-service
docker compose up -d
```

## Health Checks

All services include health checks:

- **User Service:** `curl http://localhost:8080/actuator/health`
- **Gateway Service:** `curl http://localhost:8080/actuator/health`
- **PostgreSQL:** `pg_isready -U gripday_user -d gripday_auth`
- **Redis:** `redis-cli ping`

## Volumes

The platform uses named volumes for data persistence:

- `gripday_postgres_auth_data` - Auth service database
- `gripday_redis_data` - Redis cache data
- `gripday_prometheus_data` - Prometheus metrics
- `gripday_grafana_data` - Grafana dashboards
- `gripday_loki_data` - Loki logs

## Troubleshooting

### View Service Logs

```bash
docker-compose logs -f [service-name]
```

### Restart a Service

```bash
docker-compose restart [service-name]
```

### Clean Up Everything

```bash
docker-compose down -v --remove-orphans
docker system prune -f
```

### Check Service Health

```bash
docker-compose ps
```

## Configuration Files

- `prometheus.yml` - Prometheus scraping configuration
- `loki-config.yml` - Loki log aggregation configuration
- `promtail-config.yml` - Promtail log collection configuration
- `init-auth.sql` - PostgreSQL initialization script
