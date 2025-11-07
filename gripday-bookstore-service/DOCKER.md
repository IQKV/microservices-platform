# Docker Configuration for Bookstore Service

This document describes the Docker setup for the Gripday Bookstore Service, including containerization, multi-environment support, and deployment procedures.

## Overview

The Bookstore Service uses a multi-stage Docker build with Java 21 and includes:

- PostgreSQL database for data persistence
- Redis for caching and session management
- Environment-specific configurations
- Health checks and monitoring
- Resource management and security best practices

## Quick Start

### Local Development

```bash
# Start all services (bookstore, PostgreSQL, Redis)
docker compose up -d

# View logs
docker-compose logs -f bookstore-service

# Stop services
docker-compose down
```

### Build Custom Image

```bash
# Build for local development
./scripts/docker-build.sh local

# Build for staging
./scripts/docker-build.sh staging v1.0.0

# Build for production
./scripts/docker-build.sh production v1.0.0
```

### Deploy to Environment

```bash
# Deploy to local
./scripts/docker-deploy.sh local

# Deploy to staging
./scripts/docker-deploy.sh staging

# Deploy to production
./scripts/docker-deploy.sh production
```

## Docker Files Structure

```
gripday-bookstore-service/
├── Dockerfile                      # Multi-stage build configuration
├── .dockerignore                   # Files excluded from build context
├── docker-compose.yml              # Local development environment
├── docker-compose.staging.yml      # Staging environment
├── docker-compose.production.yml   # Production environment
├── .env.example                    # Environment variables template
├── .env.local                      # Local development variables
├── .env.staging                    # Staging environment variables
├── .env.production                 # Production environment variables
└── scripts/
    ├── docker-build.sh             # Build script
    ├── docker-deploy.sh            # Deployment script
    └── init-db.sql                 # Database initialization
```

## Environment Configurations

### Local Development

- **Profile**: `local`
- **Database**: PostgreSQL container (localhost:5432)
- **Redis**: Redis container (localhost:6379)
- **Memory**: 768MB limit, 256MB reserved
- **CPU**: 0.5 cores limit, 0.25 cores reserved
- **Logging**: Console with DEBUG level

### Staging

- **Profile**: `staging`
- **Database**: External PostgreSQL (configured via env vars)
- **Redis**: External Redis (configured via env vars)
- **Memory**: 1.5GB limit, 512MB reserved
- **CPU**: 1.0 cores limit, 0.5 cores reserved
- **Logging**: JSON format with INFO level

### Production

- **Profile**: `production`
- **Database**: External PostgreSQL (configured via env vars)
- **Redis**: External Redis (configured via env vars)
- **Memory**: 3GB limit, 1GB reserved
- **CPU**: 2.0 cores limit, 1.0 cores reserved
- **Replicas**: 2 instances with rolling updates
- **Logging**: JSON format with WARN level

## Environment Variables

### Required Variables

| Variable         | Description       | Local           | Staging  | Production |
|------------------|-------------------|-----------------|----------|------------|
| `DB_HOST`        | Database hostname | bookstore-db    | External | External   |
| `DB_USERNAME`    | Database username | bookstore_user  | Required | Required   |
| `DB_PASSWORD`    | Database password | bookstore_pass  | Required | Required   |
| `REDIS_HOST`     | Redis hostname    | bookstore-redis | External | External   |
| `JWT_ISSUER_URI` | JWT issuer URI    | Local auth      | External | External   |

### Optional Variables

| Variable         | Description      | Default      |
|------------------|------------------|--------------|
| `DB_PORT`        | Database port    | 5432         |
| `DB_NAME`        | Database name    | bookstore_db |
| `REDIS_PORT`     | Redis port       | 6379         |
| `REDIS_PASSWORD` | Redis password   | (empty)      |
| `SERVER_PORT`    | Application port | 8082         |

## Health Checks

All environments include health checks:

- **Endpoint**: `http://localhost:8082/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 3 attempts
- **Start Period**: 90-120 seconds (varies by environment)

## Resource Management

### JVM Optimization

- Container-aware JVM settings
- G1 garbage collector for production
- Memory percentage-based allocation
- String deduplication enabled

### Container Resources

- Memory limits prevent OOM conditions
- CPU limits ensure fair resource sharing
- Reserved resources guarantee minimum allocation
- Environment-specific tuning

## Security Features

### Container Security

- Non-root user execution (UID 1001)
- Minimal base image (Alpine Linux)
- Security scanning support (Trivy)
- Read-only filesystem where possible

### Network Security

- Internal network isolation
- No direct external database access
- Environment-specific network configurations
- Secure credential management

## Monitoring and Observability

### Metrics

- Prometheus metrics endpoint: `/actuator/prometheus`
- Custom business metrics included
- JVM and system metrics
- Application performance monitoring

### Logging

- Structured JSON logging (staging/production)
- Correlation ID propagation
- Audit logging for admin operations
- Configurable log levels per environment

### Tracing

- OpenTelemetry integration
- Distributed tracing support
- Request correlation across services

## Troubleshooting

### Common Issues

1. **Service won't start**
   ```bash
   # Check logs
   docker-compose logs bookstore-service
   
   # Check health
   curl http://localhost:8082/actuator/health
   ```

2. **Database connection issues**
   ```bash
   # Check database container
   docker-compose logs bookstore-db
   
   # Test connection
   docker-compose exec bookstore-db psql -U bookstore_user -d bookstore_db
   ```

3. **Redis connection issues**
   ```bash
   # Check Redis container
   docker-compose logs bookstore-redis
   
   # Test connection
   docker-compose exec bookstore-redis redis-cli ping
   ```

### Performance Tuning

1. **Memory Issues**
    - Adjust `JAVA_OPTS` memory settings
    - Monitor container memory usage
    - Check for memory leaks in application logs

2. **CPU Issues**
    - Review CPU limits in docker-compose files
    - Monitor application performance metrics
    - Consider scaling replicas in production

3. **Database Performance**
    - Monitor connection pool metrics
    - Review slow query logs
    - Optimize database indexes

## Development Workflow

### Local Development

```bash
# Start development environment
docker compose up -d

# Rebuild after code changes
docker-compose build bookstore-service
docker compose up -d bookstore-service

# View real-time logs
docker-compose logs -f bookstore-service
```

### Testing

```bash
# Run tests in container
docker-compose exec bookstore-service ./mvnw test

# Integration testing
docker compose -f docker-compose.yml -f docker-compose.test.yml up --abort-on-container-exit
```

### Deployment

```bash
# Build and tag for deployment
./scripts/docker-build.sh production v1.2.0

# Deploy to production
./scripts/docker-deploy.sh production
```

## Best Practices

1. **Image Building**
    - Use multi-stage builds for smaller images
    - Leverage Docker layer caching
    - Minimize image attack surface

2. **Configuration Management**
    - Use environment variables for configuration
    - Keep secrets out of images
    - Use environment-specific compose files

3. **Resource Management**
    - Set appropriate memory and CPU limits
    - Monitor resource usage
    - Scale based on actual demand

4. **Security**
    - Run as non-root user
    - Use official base images
    - Regularly update dependencies
    - Scan images for vulnerabilities

5. **Monitoring**
    - Implement health checks
    - Use structured logging
    - Monitor application metrics
    - Set up alerting for critical issues