# IQ Scaffold Contact Service

A comprehensive CRM contact management service built with Spring Boot, providing multi-tenant contact, company, and activity management capabilities.

## Features

- **Contact Management**: Create, read, update, and delete contacts with lead scoring
- **Company Management**: Manage companies with hierarchical relationships
- **Activity Tracking**: Track interactions, tasks, and communications
- **Multi-tenancy**: Schema-per-tenant isolation
- **Internationalization**: Support for English, Spanish, and French
- **Security**: JWT-based authentication and authorization
- **Caching**: Redis and Hibernate second-level caching
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
   docker-compose up -d postgres-contact redis-contact rabbitmq-contact
   ```

2. **Run the application**:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the application**:
   - API: http://localhost:8083
   - Swagger UI: http://localhost:8083/swagger-ui.html
   - Health Check: http://localhost:8083/actuator/health

### Docker Development

```bash
# Build and run all services
docker-compose up --build

# Run in detached mode
docker-compose up -d
```

## API Endpoints

### Contacts

- `GET /api/v1/crm/contacts` - List contacts
- `POST /api/v1/crm/contacts` - Create contact
- `GET /api/v1/crm/contacts/{id}` - Get contact by ID
- `PUT /api/v1/crm/contacts/{id}` - Update contact
- `DELETE /api/v1/crm/contacts/{id}` - Delete contact

### Companies

- `GET /api/v1/crm/companies` - List companies
- `POST /api/v1/crm/companies` - Create company
- `GET /api/v1/crm/companies/{id}` - Get company by ID
- `PUT /api/v1/crm/companies/{id}` - Update company
- `DELETE /api/v1/crm/companies/{id}` - Delete company

### Activities

- `GET /api/v1/crm/activities` - List activities
- `POST /api/v1/crm/activities` - Create activity
- `GET /api/v1/crm/activities/{id}` - Get activity by ID
- `PUT /api/v1/crm/activities/{id}` - Update activity
- `DELETE /api/v1/crm/activities/{id}` - Delete activity

## Configuration

### Environment Variables

Key environment variables for configuration:

```bash
# Database
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5432/iqscaffold_contact_local
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_contact
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

# CRM Features
CRM_ENABLE_LEAD_SCORING=true
CRM_ENABLE_ACTIVITY_TRACKING=true
CRM_ENABLE_EMAIL_INTEGRATION=true
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

- `contacts` - Contact information and lead scoring
- `companies` - Company details and hierarchies
- `activities` - Interaction tracking and task management

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

### Adding New Features

1. Create feature branch from `main`
2. Implement feature with tests
3. Update documentation
4. Submit pull request

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

## Troubleshooting

### Common Issues

1. **Database Connection**: Verify PostgreSQL is running and credentials are correct
2. **Redis Connection**: Check Redis service and port configuration
3. **JWT Validation**: Ensure user service is accessible and JWT configuration is correct

### Logs

```bash
# View application logs
docker-compose logs contact-service

# Follow logs
docker-compose logs -f contact-service
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
