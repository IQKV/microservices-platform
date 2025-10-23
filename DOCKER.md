# Docker Configuration Guide

This document provides comprehensive information about the Docker setup for the Gripday Microservices Platform.

## Overview

The platform includes:
- **Multi-stage Dockerfiles** for optimized container images
- **Environment-specific** Docker Compose configurations
- **Service isolation** with dedicated networks
- **Health checks** for all services
- **Observability stack** integration
- **Development-friendly** configurations

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Network                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Gateway   │  │    Auth     │  │    Infrastructure   │ │
│  │   Service   │  │   Service   │  │                     │ │
│  │   :8080     │  │    :8081    │  │  PostgreSQL :5432   │ │
│  └─────────────┘  └─────────────┘  │  Redis      :6379   │ │
│                                    │  Prometheus :9090   │ │
│                                    │  Grafana    :3000   │ │
│                                    │  Loki       :3100   │ │
│                                    └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Edit environment variables
nano .env  # or your preferred editor
```

### 2. Start Platform

```bash
# Start all services
docker-compose up -d --build

# Check service status
docker-compose ps

# View logs
docker-compose logs -f
```

### 3. Access Services

- **Gateway Service:** http://localhost:8080
- **Auth Service:** http://localhost:8081/actuator/health
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

## Configuration Files

### Docker Compose Files

| File | Purpose | Usage |
|------|---------|-------|
| `docker-compose.yml` | Base configuration | Local development |
| `docker-compose.staging.yml` | Staging overrides | Staging deployment |
| `docker-compose.production.yml` | Production overrides | Production deployment |
| `gripday-auth-service/docker-compose.yml` | Auth service only | Individual development |
| `gripday-gateway-service/docker-compose.yml` | Gateway service only | Individual development |

### Environment Files

| File | Purpose |
|------|---------|
| `.env.example` | Environment template |
| `.env` | Local environment variables (create from example) |

### Configuration Directories

```
docker/
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── datasources.yml
├── loki/
│   └── loki-config.yml
├── postgres/
│   └── init-auth.sql
├── prometheus/
│   └── prometheus.yml
├── promtail/
│   └── promtail-config.yml
└── scripts/
    ├── build-services.sh
    ├── start-platform.sh
    ├── stop-platform.sh
    └── validate-config.sh
```

## Dockerfile Features

### Multi-Stage Builds

Both services use multi-stage builds for optimization:

1. **Builder Stage**: Compiles Java code with Maven
2. **Runtime Stage**: Minimal JRE with application JAR

### Security Features

- Non-root user execution
- Minimal base images (Alpine Linux)
- Security-focused JVM options

### Performance Optimizations

- Container-aware JVM settings
- G1 garbage collector
- Memory percentage limits
- String deduplication

## Environment-Specific Deployments

### Local Development

```bash
docker-compose up -d
```

**Features:**
- Debug-friendly settings
- Exposed ports for direct access
- Volume mounts for development
- Relaxed security settings

### Staging

```bash
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

**Features:**
- Resource limits
- Environment variable externalization
- Production-like configuration
- Monitoring enabled

### Production

```bash
docker-compose -f docker-compose.yml -f docker-compose.production.yml up -d
```

**Features:**
- High availability (replicas)
- Strict resource limits
- Security hardening
- Performance optimization
- Rolling updates

## Service Dependencies

### Dependency Graph

```
Gateway Service
    ├── Auth Service (health check)
    └── Redis (health check)

Auth Service
    ├── PostgreSQL (health check)
    └── Redis (health check)

Observability
    ├── Prometheus (scrapes services)
    ├── Grafana (uses Prometheus & Loki)
    ├── Loki (log aggregation)
    └── Promtail (log collection)
```

### Health Checks

All services include comprehensive health checks:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

## Individual Service Development

### Auth Service Only

```bash
cd gripday-auth-service
docker-compose up -d
```

**Includes:**
- PostgreSQL database
- Redis cache
- Auth service

### Gateway Service Only

```bash
cd gripday-gateway-service
docker-compose up -d
```

**Includes:**
- Redis cache
- Mock auth service (WireMock)
- Gateway service

## Networking

### Network Configuration

- **Network Name:** `gripday-network`
- **Driver:** Bridge
- **DNS Resolution:** Automatic service discovery

### Service Communication

Services communicate using container names:
- `auth-service:8081`
- `gateway-service:8080`
- `postgres-auth:5432`
- `redis:6379`

## Volume Management

### Named Volumes

| Volume | Purpose | Persistence |
|--------|---------|-------------|
| `gripday_postgres_auth_data` | Auth database | Persistent |
| `gripday_redis_data` | Cache data | Persistent |
| `gripday_prometheus_data` | Metrics | Persistent |
| `gripday_grafana_data` | Dashboards | Persistent |
| `gripday_loki_data` | Logs | Persistent |

### Volume Commands

```bash
# List volumes
docker volume ls | grep gripday

# Remove all volumes (data loss!)
docker-compose down -v

# Backup volume
docker run --rm -v gripday_postgres_auth_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .

# Restore volume
docker run --rm -v gripday_postgres_auth_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /data
```

## Monitoring and Observability

### Prometheus Configuration

- **Scrape Interval:** 15s
- **Targets:** All microservices
- **Metrics Path:** `/actuator/prometheus`

### Grafana Setup

- **Default Login:** admin/admin
- **Datasources:** Prometheus, Loki
- **Dashboards:** Auto-provisioned

### Loki Configuration

- **Log Retention:** Configurable
- **Storage:** Filesystem (development)
- **Ingestion:** Via Promtail

## Troubleshooting

### Common Issues

#### Services Won't Start

```bash
# Check Docker daemon
docker info

# Validate configuration
docker-compose config

# Check resource usage
docker system df
```

#### Database Connection Issues

```bash
# Check PostgreSQL health
docker-compose exec postgres-auth pg_isready -U gripday_user -d gripday_auth

# View database logs
docker-compose logs postgres-auth

# Connect to database
docker-compose exec postgres-auth psql -U gripday_user -d gripday_auth
```

#### Memory Issues

```bash
# Check container resource usage
docker stats

# Increase Docker memory limit
# Docker Desktop: Settings > Resources > Memory
```

### Debugging Commands

```bash
# View service logs
docker-compose logs -f [service-name]

# Execute commands in container
docker-compose exec [service-name] /bin/sh

# Check service health
docker-compose ps

# Restart specific service
docker-compose restart [service-name]

# Rebuild and restart
docker-compose up -d --build [service-name]
```

### Performance Tuning

#### JVM Optimization

```bash
# Current JVM settings
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# For high-throughput applications
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:G1HeapRegionSize=16m"
```

#### Database Optimization

```bash
# PostgreSQL performance settings (production)
shared_buffers=256MB
effective_cache_size=1GB
maintenance_work_mem=64MB
checkpoint_completion_target=0.9
```

## Security Considerations

### Container Security

- Non-root user execution
- Minimal base images
- No unnecessary packages
- Security scanning recommended

### Network Security

- Internal service communication
- No exposed database ports (production)
- TLS termination at gateway

### Secrets Management

- Environment variables for configuration
- External secret management recommended (production)
- No hardcoded secrets in images

## Maintenance

### Updates

```bash
# Update base images
docker-compose pull

# Rebuild with latest base images
docker-compose build --no-cache

# Rolling update (production)
docker-compose up -d --no-deps [service-name]
```

### Cleanup

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Complete cleanup
docker system prune -a
```

### Backup Strategy

1. **Database Backups:** Regular PostgreSQL dumps
2. **Volume Backups:** Tar archives of named volumes
3. **Configuration Backups:** Git repository
4. **Image Backups:** Registry storage

## Best Practices

### Development

- Use `.env` files for local configuration
- Keep containers stateless
- Use health checks for dependencies
- Monitor resource usage

### Production

- Use specific image tags (not `latest`)
- Implement proper logging
- Set resource limits
- Use external databases
- Implement backup strategies
- Monitor security vulnerabilities

### CI/CD Integration

```bash
# Build images
docker build -t gripday/auth-service:${VERSION} -f gripday-auth-service/Dockerfile .

# Push to registry
docker push gripday/auth-service:${VERSION}

# Deploy with specific version
docker-compose -f docker-compose.production.yml up -d
```