# IQ Scaffold Pipeline Service

A CRM pipeline management service built with Spring Boot, providing lead tracking, pipeline stage management, and follow-up scheduling capabilities.

## Features

- **Pipeline Stage Management**: Track leads through stages (New → Contacted → Qualified → Proposal → Won/Lost)
- **Lead Tracking**: Associate leads with pipeline stages
- **Follow-up Scheduling**: Schedule and track follow-ups with due dates
- **Activity Logging**: Track all lead interactions and stage transitions
- **Dashboard Stats**: Conversion metrics and pipeline analytics
- **Multi-tenancy**: Schema-per-tenant isolation
- **Security**: JWT-based authentication and authorization
- **Observability**: Metrics, tracing, and health checks
- **API Documentation**: OpenAPI/Swagger integration

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis + Hibernate/Ehcache
- **Messaging**: RabbitMQ
- **Security**: Spring Security with OAuth2 JWT
- **Documentation**: SpringDoc OpenAPI
- **Observability**: Micrometer, OpenTelemetry
- **Testing**: JUnit 5, Testcontainers

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Maven 3.9+

### Local Development

1. **Start infrastructure services**:

   ```bash
   docker-compose up -d postgres-pipeline redis-pipeline rabbitmq-pipeline
   ```

2. **Run the application**:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the application**:
   - API: http://localhost:8085
   - Swagger UI: http://localhost:8085/swagger-ui.html
   - Health Check: http://localhost:8085/actuator/health

### Docker Development

```bash
# Build and run all services
docker-compose up --build

# Run in detached mode
docker-compose up -d
```

## API Endpoints

### Pipeline Stages

- `GET /api/v1/pipeline/stages` - List all pipeline stages
- `POST /api/v1/pipeline/stages` - Create pipeline stage
- `GET /api/v1/pipeline/stages/{id}` - Get stage by ID
- `PUT /api/v1/pipeline/stages/{id}` - Update stage
- `DELETE /api/v1/pipeline/stages/{id}` - Delete stage
- `PUT /api/v1/pipeline/stages/{id}/order` - Reorder stages

### Pipeline Items (Leads in Pipeline)

- `GET /api/v1/pipeline/items` - List pipeline items
- `POST /api/v1/pipeline/items` - Add lead to pipeline
- `GET /api/v1/pipeline/items/{id}` - Get pipeline item
- `PUT /api/v1/pipeline/items/{id}` - Update pipeline item
- `PUT /api/v1/pipeline/items/{id}/stage` - Move to different stage
- `DELETE /api/v1/pipeline/items/{id}` - Remove from pipeline

### Follow-ups

- `GET /api/v1/pipeline/follow-ups` - List follow-ups
- `POST /api/v1/pipeline/follow-ups` - Schedule follow-up
- `GET /api/v1/pipeline/follow-ups/{id}` - Get follow-up
- `PUT /api/v1/pipeline/follow-ups/{id}` - Update follow-up
- `PUT /api/v1/pipeline/follow-ups/{id}/complete` - Mark as completed
- `GET /api/v1/pipeline/follow-ups/today` - Get today's follow-ups
- `GET /api/v1/pipeline/follow-ups/overdue` - Get overdue follow-ups

### Activity Log

- `GET /api/v1/pipeline/activities` - List activities
- `POST /api/v1/pipeline/activities` - Log activity
- `GET /api/v1/pipeline/activities/{id}` - Get activity
- `GET /api/v1/pipeline/activities/lead/{leadId}` - Get lead activities

### Dashboard & Stats

- `GET /api/v1/pipeline/dashboard/stats` - Get pipeline statistics
- `GET /api/v1/pipeline/dashboard/conversion` - Get conversion rates
- `GET /api/v1/pipeline/dashboard/velocity` - Get pipeline velocity

## Configuration

### Environment Variables

Key environment variables for configuration:

```bash
# Database
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5432/iqscaffold_pipeline_local
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_pipeline
IQSCAFFOLD_DATABASE_PASSWORD=iqscaffold_password

# Redis
IQSCAFFOLD_CACHE_REDIS_HOST=localhost
IQSCAFFOLD_CACHE_REDIS_PORT=6379

# RabbitMQ
IQSCAFFOLD_MESSAGING_RABBITMQ_HOST=localhost
IQSCAFFOLD_MESSAGING_RABBITMQ_PORT=5672

# Security
USER_SERVICE_URL=http://localhost:8081
JWT_ISSUER=iqscaffold-user-service

# Lead Service Integration
LEAD_SERVICE_URL=http://localhost:8084
```

### Profiles

- `local` - Local development with debug logging
- `staging` - Staging environment configuration
- `production` - Production environment with JSON logging

## Multi-tenancy

The service uses schema-per-tenant isolation:

1. **Tenant Identification**: Via `X-Tenant-ID` header
2. **Schema Management**: Automatic schema creation and migration
3. **Data Isolation**: Complete separation between tenants

## Database Schema

### System Schema (public)

- `tenant_info` - Tenant metadata and schema mapping

### Tenant Schemas

- `pipeline_stages` - Pipeline stage definitions and ordering
- `pipeline_items` - Leads in pipeline with current stage
- `follow_ups` - Scheduled follow-ups with due dates
- `pipeline_activities` - Activity log for all pipeline actions

## Pipeline Stages

Default pipeline stages:

1. **New** - Newly created leads
2. **Contacted** - Initial contact made
3. **Qualified** - Lead qualified as potential customer
4. **Proposal** - Proposal sent
5. **Won** - Deal closed successfully
6. **Lost** - Deal lost

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean verify jacoco:report
```

### Code Quality

The project includes:

- Checkstyle for code style
- JaCoCo for test coverage (70% minimum)
- ArchUnit for architecture testing

## Monitoring

### Health Checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`

### Metrics

- Prometheus: `/actuator/prometheus`
- Application metrics: `/actuator/metrics`

### Tracing

- OpenTelemetry integration
- Distributed tracing support

## License

This project is licensed under the MIT License - see the LICENSE file for details.
