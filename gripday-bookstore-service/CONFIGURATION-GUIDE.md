# Configuration Guide - Bookstore Service

## Overview

The Bookstore Service uses Spring Boot profiles for environment-specific configuration. This guide explains all configuration options and how to deploy the service in different environments.

## Configuration Files

```
src/main/resources/
├── application.yml              # Base configuration (shared across all profiles)
├── application-local.yml        # Local development
├── application-staging.yml      # Staging environment
└── application-production.yml   # Production environment
```

## Profile Selection

### Via Command Line

```bash
# Local development
java -jar bookstore-service.jar --spring.profiles.active=local

# Staging
java -jar bookstore-service.jar --spring.profiles.active=staging

# Production
java -jar bookstore-service.jar --spring.profiles.active=production
```

### Via Environment Variable

```bash
export SPRING_PROFILES_ACTIVE=production
java -jar bookstore-service.jar
```

### Via Maven

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Environment Variables

### Required for All Environments

| Variable                 | Description           | Example                          |
| ------------------------ | --------------------- | -------------------------------- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local`, `staging`, `production` |

### Database Configuration

| Variable      | Description       | Default                  | Required            |
| ------------- | ----------------- | ------------------------ | ------------------- |
| `DB_HOST`     | PostgreSQL host   | `localhost`              | Staging, Production |
| `DB_PORT`     | PostgreSQL port   | `5432`                   | No                  |
| `DB_NAME`     | Database name     | `bookstore_db`           | Staging, Production |
| `DB_USERNAME` | Database username | `bookstore_user` (local) | Yes                 |
| `DB_PASSWORD` | Database password | `bookstore_pass` (local) | Yes                 |

### Redis Configuration

| Variable         | Description    | Default           | Required            |
| ---------------- | -------------- | ----------------- | ------------------- |
| `REDIS_HOST`     | Redis host     | `localhost`       | Staging, Production |
| `REDIS_PORT`     | Redis port     | `6379`            | No                  |
| `REDIS_PASSWORD` | Redis password | (empty for local) | Staging, Production |

### JWT Authentication Configuration

| Variable          | Description                  | Default                                       | Required            |
| ----------------- | ---------------------------- | --------------------------------------------- | ------------------- |
| `JWT_ISSUER_URI`  | User Service issuer URI      | `http://localhost:8080`                       | Staging, Production |
| `JWT_JWK_SET_URI` | JWK endpoint for public keys | `http://localhost:8080/.well-known/jwks.json` | Staging, Production |

### Server Configuration

| Variable      | Description      | Default | Required |
| ------------- | ---------------- | ------- | -------- |
| `SERVER_PORT` | HTTP server port | `8081`  | No       |

### CORS Configuration

| Variable               | Description              | Default          | Required |
| ---------------------- | ------------------------ | ---------------- | -------- |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | Profile-specific | No       |

## Profile-Specific Configuration

### Local Development Profile

**Purpose:** Local development with Docker Compose

**Characteristics:**

- Verbose logging (DEBUG level)
- All actuator endpoints exposed
- Permissive CORS
- Small connection pools
- Short cache TTL (5 minutes)

**Services Required:**

```bash
# Start dependencies
docker-compose up -d postgres redis

# Start User Service (for JWT validation)
cd ../gripday-user-service
mvn spring-boot:run

# Start Bookstore Service
cd ../gripday-bookstore-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Default Configuration:**

```yaml
Database: localhost:5432/bookstore_db
Redis: localhost:6379
Server Port: 8081
User Service: http://localhost:8080
JWK Endpoint: http://localhost:8080/.well-known/jwks.json
```

**Logging:**

- Application: DEBUG
- Security: DEBUG (see JWT validation)
- SQL: DEBUG (see all queries)
- Cache: DEBUG (see cache operations)

### Staging Profile

**Purpose:** Pre-production testing environment

**Characteristics:**

- Balanced logging (INFO level)
- Limited actuator endpoints
- Restricted CORS
- Medium connection pools
- Medium cache TTL (10 minutes)

**Environment Variables Required:**

```bash
export DB_HOST=staging-postgres.gripday.com
export DB_NAME=bookstore_staging
export DB_USERNAME=bookstore_staging_user
export DB_PASSWORD=<secure-password>

export REDIS_HOST=staging-redis.gripday.com
export REDIS_PASSWORD=<secure-password>

export JWT_ISSUER_URI=https://staging-api.gripday.com
export JWT_JWK_SET_URI=https://staging-api.gripday.com/.well-known/jwks.json

export CORS_ALLOWED_ORIGINS=https://staging.bookstore.gripday.com
```

**Deployment:**

```bash
java -jar bookstore-service.jar --spring.profiles.active=staging
```

**Logging:**

- Application: INFO
- Security: INFO
- SQL: INFO
- Audit: INFO (always enabled)

### Production Profile

**Purpose:** Production deployment

**Characteristics:**

- Minimal logging (WARN level)
- Minimal actuator endpoints
- Strict CORS
- Large connection pools
- Long cache TTL (15 minutes)
- Compression enabled
- Connection leak detection

**Environment Variables Required:**

```bash
export DB_HOST=prod-postgres.gripday.com
export DB_NAME=bookstore_production
export DB_USERNAME=bookstore_prod_user
export DB_PASSWORD=<secure-password>

export REDIS_HOST=prod-redis.gripday.com
export REDIS_PASSWORD=<secure-password>

export JWT_ISSUER_URI=https://api.gripday.com
export JWT_JWK_SET_URI=https://api.gripday.com/.well-known/jwks.json

export CORS_ALLOWED_ORIGINS=https://bookstore.gripday.com
```

**Deployment:**

```bash
java -jar bookstore-service.jar --spring.profiles.active=production
```

**Logging:**

- Application: WARN (errors only)
- Security: WARN
- SQL: WARN
- Audit: INFO (always enabled for compliance)

## Configuration Details

### Database Connection Pool

**Local:**

- Max Pool Size: 5
- Min Idle: 2
- Connection Timeout: 20s

**Staging:**

- Max Pool Size: 15
- Min Idle: 3
- Connection Timeout: 30s
- Leak Detection: 60s

**Production:**

- Max Pool Size: 20
- Min Idle: 5
- Connection Timeout: 30s
- Leak Detection: 60s
- Connection Test Query: SELECT 1

### Redis Connection Pool

**Local:**

- Max Active: 8
- Max Idle: 8
- Min Idle: 0

**Staging:**

- Max Active: 16
- Max Idle: 8
- Min Idle: 2

**Production:**

- Max Active: 32
- Max Idle: 16
- Min Idle: 4

### Cache TTL

| Environment | TTL        | Reason                                  |
| ----------- | ---------- | --------------------------------------- |
| Local       | 5 minutes  | Fast feedback during development        |
| Staging     | 10 minutes | Balance between testing and performance |
| Production  | 15 minutes | Optimal performance                     |

### JWT Validation

**Algorithm:** RSA256 (asymmetric encryption)

**Key Features:**

- Public keys fetched from JWK endpoint
- Automatic key rotation support (90-day cycle, 7-day grace period)
- Spring Security caches keys (5 minutes default)
- No shared secrets required

**Configuration:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: <user-service-url>
          jwk-set-uri: <user-service-url>/.well-known/jwks.json
```

### CORS Configuration

**Local:**

```
http://localhost:5173  # Vite dev server
http://localhost:3000  # React dev server
http://localhost:4200  # Angular dev server
```

**Staging:**

```
https://staging.bookstore.gripday.com
```

**Production:**

```
https://bookstore.gripday.com
```

**Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS

**Allowed Headers:**

- Authorization (JWT token)
- Content-Type
- X-Correlation-ID (distributed tracing)
- X-Tenant-ID (multi-tenancy)

### Actuator Endpoints

**Local (All Exposed):**

```
/actuator/*
```

**Staging & Production (Limited):**

```
/actuator/health      # Health checks
/actuator/info        # Build info
/actuator/metrics     # Metrics
/actuator/prometheus  # Prometheus metrics
```

## Docker Deployment

### Docker Compose (Local)

```yaml
version: "3.8"
services:
  bookstore-service:
    image: gripday/bookstore-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - JWT_ISSUER_URI=http://user-service:8080
      - JWT_JWK_SET_URI=http://user-service:8080/.well-known/jwks.json
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - redis
      - user-service
```

### Kubernetes (Production)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bookstore-service
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: bookstore-service
          image: gripday/bookstore-service:latest
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: DB_HOST
              valueFrom:
                secretKeyRef:
                  name: bookstore-db-secret
                  key: host
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: bookstore-db-secret
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: bookstore-db-secret
                  key: password
            - name: REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: bookstore-config
                  key: redis-host
            - name: JWT_ISSUER_URI
              value: "https://api.gripday.com"
            - name: JWT_JWK_SET_URI
              value: "https://api.gripday.com/.well-known/jwks.json"
          ports:
            - containerPort: 8081
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 5
```

## Health Checks

### Liveness Probe

```bash
curl http://localhost:8081/actuator/health/liveness
```

**Response:**

```json
{
  "status": "UP"
}
```

### Readiness Probe

```bash
curl http://localhost:8081/actuator/health/readiness
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

## Troubleshooting

### JWT Validation Fails

**Symptom:** 401 Unauthorized errors

**Check:**

1. Verify User Service is running
2. Check JWK endpoint is accessible:
   ```bash
   curl http://localhost:8080/.well-known/jwks.json
   ```
3. Enable debug logging:
   ```yaml
   logging:
     level:
       org.springframework.security: DEBUG
   ```
4. Verify issuer URI matches token's `iss` claim

### Database Connection Issues

**Symptom:** Connection timeout or refused

**Check:**

1. Verify database is running
2. Check connection parameters
3. Test connection:
   ```bash
   psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
   ```
4. Check connection pool settings
5. Look for connection leaks in logs

### Redis Connection Issues

**Symptom:** Cache operations fail

**Check:**

1. Verify Redis is running
2. Test connection:
   ```bash
   redis-cli -h $REDIS_HOST -p $REDIS_PORT ping
   ```
3. Check password if required
4. Verify network connectivity

### CORS Issues

**Symptom:** Browser blocks requests

**Check:**

1. Verify origin is in allowed list
2. Check browser console for CORS errors
3. Verify credentials setting matches frontend
4. Test with curl (bypasses CORS):
   ```bash
   curl -H "Origin: http://localhost:5173" \
        -H "Authorization: Bearer $TOKEN" \
        http://localhost:8081/api/v1/bookstore/books
   ```

## Security Best Practices

### Production Checklist

- [ ] All credentials via environment variables (never in code)
- [ ] CORS restricted to production frontend only
- [ ] Actuator endpoints limited to essential only
- [ ] Health endpoint details hidden (`show-details: never`)
- [ ] Logging set to WARN (minimal output)
- [ ] AUDIT logging always enabled
- [ ] Connection leak detection enabled
- [ ] Graceful shutdown configured
- [ ] HTTPS enforced (via reverse proxy/load balancer)
- [ ] JWT validation via JWK endpoint (not shared secrets)

### Secrets Management

**Never commit:**

- Database passwords
- Redis passwords
- API keys
- JWT secrets (not used with JWK, but general practice)

**Use:**

- Environment variables
- Kubernetes secrets
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault

## Monitoring

### Prometheus Metrics

```bash
curl http://localhost:8081/actuator/prometheus
```

**Key Metrics:**

- `http_server_requests_seconds` - Request duration
- `bookstore_books_created_total` - Books created
- `bookstore_books_updated_total` - Books updated
- `bookstore_inventory_updated_total` - Inventory updates
- `hikari_connections_active` - Active DB connections
- `cache_gets_total` - Cache hits/misses

### Grafana Dashboard

Import dashboard from: `docs/monitoring/grafana-dashboard.json`

## References

- [Authentication Architecture](../../docs/auth/authentication-architecture.md)
- [Architecture Alignment](AUTHENTICATION-ARCHITECTURE-ALIGNMENT.md)
- [Docker Documentation](DOCKER.md)
- [Spring Boot Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)

## Support

For issues or questions:

- Check logs with correlation ID
- Review health check endpoints
- Consult troubleshooting section
- Contact DevOps team
