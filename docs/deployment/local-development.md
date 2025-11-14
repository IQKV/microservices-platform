# Local Development Deployment Guide

This guide provides step-by-step instructions for setting up the Gripday microservices platform in a local development environment.

## Prerequisites

### Required Software

- **Java 21** - OpenJDK or Oracle JDK
- **Maven 3.9+** - Build automation tool
- **Docker 24+** - Container runtime
- **Docker Compose 2.0+** - Multi-container orchestration
- **Git** - Version control

### Verification Commands

```bash
# Verify Java version
java -version
# Should show Java 21

# Verify Maven version
mvn -version
# Should show Maven 3.9+

# Verify Docker
docker --version
docker-compose --version
```

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/gripday/gripday.git
cd gripday
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL and Redis
docker compose up -d postgres redis

# Verify services are running
docker-compose ps
```

### 3. Build Services

```bash
# Build all services
mvn clean package

# Or build individual services
mvn clean package -pl gripday-user-service
mvn clean package -pl gripday-gateway-service
```

### 4. Run Database Migrations

```bash
cd gripday-user-service
mvn liquibase:update -Dspring.profiles.active=local
cd ..
```

### 5. Start Services

```bash
# Terminal 1 - User Service
cd gripday-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 - Gateway Service
cd gripday-gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 6. Verify Deployment

```bash
# Check auth service health
curl http://localhost:8080/actuator/health

# Check gateway service health
curl http://localhost:8080/actuator/health

# Test authentication flow
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

## Docker Compose Setup

### Full Platform Deployment

```bash
# Start all services with Docker Compose
docker compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Clean up volumes (removes data)
docker-compose down -v
```

### Individual Service Deployment

```bash
# Auth service only
cd gripday-user-service
docker compose up -d

# Gateway service only
cd gripday-gateway-service
docker compose up -d
```

## Environment Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# Database Configuration
POSTGRES_DB=gripday_platform
POSTGRES_USER=gripday
POSTGRES_PASSWORD=dev_password_123
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Configuration
JWT_SECRET=dev_secret_key_change_in_production_min_256_bits
JWT_ACCESS_TOKEN_EXPIRY=PT15M
JWT_REFRESH_TOKEN_EXPIRY=P7D

# Gateway Configuration
GATEWAY_PORT=8080
AUTH_SERVICE_URL=http://localhost:8080

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
OTEL_SERVICE_NAME=gripday
OTEL_TRACES_SAMPLER=traceidratio
OTEL_TRACES_SAMPLER_ARG=0.1

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
```

### Service-Specific Environment Files

**User Service (.env.local):**

```bash
# Database
GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/gripday_user
GRIPDAY_DATABASE_USERNAME=gripday
GRIPDAY_DATABASE_PASSWORD=dev_password_123

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# JWT
GRIPDAY_AUTH_JWT_SECRET=dev_secret_key_change_in_production_min_256_bits
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
```

**Gateway Service (.env.local):**

```bash
# Gateway
GRIPDAY_GATEWAY_PORT=8080
GRIPDAY_GATEWAY_AUTH_SERVICE_URL=http://localhost:8080

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# Rate Limiting
GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE=100
GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY=20

# Circuit Breaker
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE=PT30S

# CORS
GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Logging
LOGGING_LEVEL_ORG_GRIPDAY=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG
```

## Database Setup

### PostgreSQL Configuration

**Docker Compose PostgreSQL:**

```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: gripday_platform
      POSTGRES_USER: gripday
      POSTGRES_PASSWORD: dev_password_123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gripday -d gripday_platform"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Database Initialization

**Create databases:**

```sql
-- Connect as postgres user
CREATE DATABASE gripday_user;
CREATE DATABASE gripday_gateway;

-- Create application user
CREATE USER gripday WITH PASSWORD 'dev_password_123';
GRANT ALL PRIVILEGES ON DATABASE gripday_user TO gripday;
GRANT ALL PRIVILEGES ON DATABASE gripday_gateway TO gripday;
```

### Liquibase Migrations

**Run migrations manually:**

```bash
cd gripday-user-service

# Update to latest version
mvn liquibase:update -Dspring.profiles.active=local

# Check migration status
mvn liquibase:status -Dspring.profiles.active=local

# Rollback last changeset (if needed)
mvn liquibase:rollback -Dliquibase.rollbackCount=1 -Dspring.profiles.active=local
```

**Migration files location:**

```
gripday-user-service/src/main/resources/db/changelog/
├── db.changelog-master.xml
├── V1__Create_users_table.xml
├── V2__Create_authorities_table.xml
├── V3__Create_user_authorities_table.xml
├── V4__Create_user_audit_log_table.xml
└── V5__Add_indexes.xml
```

## Redis Setup

### Redis Configuration

**Docker Compose Redis:**

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Redis Verification

```bash
# Connect to Redis
docker exec -it gripday_redis_1 redis-cli

# Test Redis connection
127.0.0.1:6379> ping
PONG

# Check Redis info
127.0.0.1:6379> info server
```

## Development Workflow

### Hot Reload Development

**Using Spring Boot DevTools:**

```bash
# Add to pom.xml (already included)
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

# Run with DevTools
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### IDE Configuration

**IntelliJ IDEA:**

1. Import as Maven project
2. Set Project SDK to Java 21
3. Enable annotation processing
4. Configure run configurations with `local` profile

**VS Code:**

1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure `launch.json` with local profile

### Testing

**Run all tests:**

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Specific test class
mvn test -Dtest=AuthenticationResourceTest

# Skip tests (for faster builds)
mvn package -DskipTests
```

**Test with Docker:**

```bash
# Run tests with Testcontainers
mvn verify -Dspring.profiles.active=test

# Clean test environment
docker system prune -f
```

## Monitoring and Observability

### Health Checks

```bash
# Auth service health
curl http://localhost:8080/actuator/health

# Gateway service health
curl http://localhost:8080/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/redis
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/gateway.requests
```

### Logs

```bash
# View application logs
docker-compose logs -f user-service
docker-compose logs -f gateway-service

# View infrastructure logs
docker-compose logs -f postgres
docker-compose logs -f redis

# Follow logs with grep
docker-compose logs -f user-service | grep ERROR
```

## Troubleshooting

### Common Issues

**Port Already in Use:**

```bash
# Find process using port
lsof -i :8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different ports
mvn spring-boot:run -Dserver.port=8080
```

**Database Connection Failed:**

```bash
# Check PostgreSQL status
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Test connection
psql -h localhost -U gripday -d gripday_user
```

**Redis Connection Failed:**

```bash
# Check Redis status
docker-compose ps redis

# Test Redis connection
redis-cli -h localhost -p 6379 ping
```

**Maven Build Issues:**

```bash
# Clean and rebuild
mvn clean install

# Update dependencies
mvn dependency:resolve

# Check for conflicts
mvn dependency:tree
```

### Performance Tuning

**JVM Options for Development:**

```bash
export MAVEN_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Or in IDE run configuration
-Xmx2g -Xms1g -XX:+UseG1GC -Dspring.profiles.active=local
```

**Docker Resource Limits:**

```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: "0.5"
        reservations:
          memory: 512M
          cpus: "0.25"
```

## Next Steps

After successful local setup:

1. **Explore APIs**: Visit Swagger UI at `http://localhost:8080/swagger-ui.html`
2. **Test Authentication**: Use provided cURL examples
3. **Add New Services**: Follow the extensible platform patterns
4. **Configure IDE**: Set up debugging and hot reload
5. **Review Logs**: Monitor application behavior and performance

For production deployment, see [Production Deployment Guide](production-deployment.md).
