# Gripday Bookstore Service

A Spring Boot 3.5.6 microservice for book catalog and inventory management, built with Java 21 and following the three-tier architecture pattern.

## Architecture

- **Presentation Layer**: REST controllers in `presentation.web` package
- **Domain Layer**: Business logic services in `domain.service` package  
- **Infrastructure Layer**: Data repositories in `infrastructure.repository` package

## Technology Stack

- Java 21
- Spring Boot 3.5.6
- PostgreSQL 15+
- Redis for caching
- Liquibase for database migrations
- Docker & Docker Compose
- Kubernetes for service discovery

## Quick Start

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+ (or use included wrapper)

### Local Development

1. **Start dependencies:**
   ```bash
   docker-compose up -d bookstore-db bookstore-redis
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the application:**
   - API: http://localhost:8082
   - Health Check: http://localhost:8082/actuator/health
   - Swagger UI: http://localhost:8082/swagger-ui.html

### Docker Development

```bash
# Build and run all services
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f bookstore-service
```

## Configuration

### Environment Files

- `.env.local` - Local development
- `.env.example` - Template for environment variables

### Profiles

- `local` - Local development with detailed logging
- `staging` - Staging environment with JSON logging
- `production` - Production environment with minimal logging

### Kubernetes Deployment

The service is designed to run in Kubernetes and uses Kubernetes service discovery instead of Eureka. Service-to-service communication happens through Kubernetes DNS.

## Database

### Liquibase Migrations

Migrations are located in `src/main/resources/db/changelog/changes/`

```bash
# Generate diff
./mvnw liquibase:diff

# Update database
./mvnw liquibase:update

# Rollback
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

## API Documentation

OpenAPI documentation is available at `/swagger-ui.html` when the application is running.

## Monitoring

- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Development

### Build

```bash
./mvnw clean compile
```

### Test

```bash
./mvnw test
```

### Package

```bash
./mvnw clean package
```

## Deployment

### Staging

```bash
docker-compose -f docker-compose.staging.yml up -d
```

### Production

```bash
docker-compose -f docker-compose.production.yml up -d
```