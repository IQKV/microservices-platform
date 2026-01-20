# IQ Scaffold Contact Service

A comprehensive CRM contact management service built with Spring Boot, providing multi-tenant contact management capabilities with lead scoring and conversion tracking.

## Features

- **Contact Management**: Complete REST API for contact CRUD operations with lead scoring
- **Bulk Operations**: Efficient bulk create, update status, delete, and update lead scores (max 100 per request)
- **Lead Conversion**: Track contacts converted from leads with conversion timestamps
- **Event Publishing**: RabbitMQ events for contact lifecycle (created, updated, deleted)
- **Multi-tenancy**: Schema-per-tenant isolation with automatic tenant context resolution
- **Internationalization**: Support for English, Spanish, and French
- **Security**: JWT-based authentication and authorization with tenant extraction
- **Caching**: Hibernate second-level caching with Ehcache
- **Observability**: Metrics, tracing, and health checks with Micrometer and OpenTelemetry
- **API Documentation**: OpenAPI/Swagger integration

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java**: 21
- **Database**: PostgreSQL 15 with Liquibase migrations
- **Cache**: Hibernate second-level cache with Ehcache 3
- **Messaging**: RabbitMQ for event-driven communication
- **Security**: Spring Security with OAuth2 JWT Resource Server
- **Documentation**: SpringDoc OpenAPI 2.x
- **Observability**: Micrometer, OpenTelemetry, Prometheus
- **Testing**: JUnit 5, Mockito, H2 (test), Testcontainers, ArchUnit

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
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Docs: http://localhost:8080/api-docs
   - Health Check: http://localhost:8080/actuator/health
   - Prometheus Metrics: http://localhost:8080/actuator/prometheus

### Docker Development

```bash
# Build and run all services
docker-compose up --build

# Run in detached mode
docker-compose up -d
```

## API Endpoints

### Contacts

**Base Path:** `/api/v1/contacts`

- `GET /api/v1/contacts` - List contacts (paginated, with search and filtering)
- `POST /api/v1/contacts` - Create contact
- `GET /api/v1/contacts/{id}` - Get contact by ID
- `PUT /api/v1/contacts/{id}` - Update contact
- `DELETE /api/v1/contacts/{id}` - Delete contact
- `GET /api/v1/contacts/company/{companyId}` - Get contacts by company
- `PATCH /api/v1/contacts/{id}/score` - Update lead score

### Bulk Operations

**Base Path:** `/api/v1/contacts`

- `POST /api/v1/contacts/bulk` - Bulk create contacts (max 100)
- `PATCH /api/v1/contacts/bulk/status` - Bulk update contact status (max 100)
- `DELETE /api/v1/contacts/bulk` - Bulk delete contacts (max 100)
- `PATCH /api/v1/contacts/bulk/scores` - Bulk update lead scores (max 100)

### Query Parameters

**List Contacts (`GET /api/v1/contacts`):**

- `search` - Search term (matches first name, last name, email)
- `status` - Filter by contact status (ACTIVE, INACTIVE, LEAD, PROSPECT, CUSTOMER, ARCHIVED)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field and direction (e.g., `lastName,asc`)

### Bulk Operations

All bulk operations support up to 100 items per request and return a detailed response with success/failure status for each item.

**Bulk Create (`POST /api/v1/contacts/bulk`):**

```json
{
  "contacts": [
    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "phone": "+1234567890",
      "status": "ACTIVE"
    }
  ]
}
```

**Bulk Update Status (`PATCH /api/v1/contacts/bulk/status`):**

```json
{
  "contactIds": [1, 2, 3],
  "status": "CUSTOMER"
}
```

**Bulk Delete (`DELETE /api/v1/contacts/bulk`):**

```json
{
  "contactIds": [1, 2, 3]
}
```

**Bulk Update Lead Scores (`PATCH /api/v1/contacts/bulk/scores`):**

```json
{
  "updates": [
    { "contactId": 1, "score": 85 },
    { "contactId": 2, "score": 90 }
  ]
}
```

**Bulk Operation Response:**

```json
{
  "successCount": 2,
  "failureCount": 1,
  "results": [
    {
      "contactId": 1,
      "email": "john.doe@example.com",
      "success": true,
      "message": "Contact created successfully",
      "contact": { ... }
    },
    {
      "contactId": null,
      "email": "duplicate@example.com",
      "success": false,
      "message": "Contact with email already exists",
      "contact": null
    }
  ]
}
```

### Event Publishing

The service publishes RabbitMQ events for contact lifecycle operations:

**Exchange:** `crm.events` (topic)

**Events Published:**

1. **contact.created** - When a new contact is created

   ```json
   {
     "eventId": "uuid",
     "eventType": "contact.created",
     "timestamp": "2026-01-15T10:30:00Z",
     "tenantId": "tenant-123",
     "userId": "user-456",
     "contactId": "contact-789",
     "metadata": {
       "firstName": "John",
       "lastName": "Doe",
       "email": "john.doe@example.com",
       "status": "ACTIVE",
       "convertedFromLeadId": "lead-123"
     }
   }
   ```

2. **contact.updated** - When a contact is updated

   ```json
   {
     "eventId": "uuid",
     "eventType": "contact.updated",
     "timestamp": "2026-01-15T10:35:00Z",
     "tenantId": "tenant-123",
     "userId": "user-456",
     "contactId": "contact-789",
     "metadata": {
       "firstName": "John",
       "lastName": "Doe",
       "email": "john.doe@example.com",
       "status": "CUSTOMER"
     }
   }
   ```

3. **contact.deleted** - When a contact is deleted
   ```json
   {
     "eventId": "uuid",
     "eventType": "contact.deleted",
     "timestamp": "2026-01-15T10:40:00Z",
     "tenantId": "tenant-123",
     "userId": "user-456",
     "contactId": "contact-789",
     "metadata": {}
   }
   ```

**Event Consumers:**

- Lead Service listens to `contact.created` events to update lead conversion tracking
- Pipeline Service listens to `contact.created` events to move pipeline items to "Won" stage

### Integration with Other Services

**Lead Service Integration:**

- Contacts can be created from lead conversions via `POST /api/v1/leads/{id}/convert`
- Lead Service calls Contact Service REST API to create contact
- Contact Service publishes `contact.created` event with `convertedFromLeadId`
- Lead Service consumes event to update lead status and log activity

**Pipeline Service Integration:**

- Pipeline Service listens to `contact.created` events
- When contact is created from lead, pipeline item moves to "Won" stage
- Conversion timestamp is recorded on pipeline item

### Current Implementation

The service provides complete REST API functionality:

- **REST Endpoints**: Full CRUD operations with search and filtering
- **Domain Model**: Contact entity with lead scoring and conversion tracking
- **Repository Layer**: JPA repository with custom queries
- **Service Layer**: Business logic for contact management operations
- **Event Publishing**: RabbitMQ events for contact lifecycle (created, updated, deleted)
- **Event Consumers**: Lead Service and Pipeline Service consume contact events
- **Multi-tenancy**: Schema-per-tenant with automatic context resolution
- **Security**: JWT authentication and tenant extraction filters

## Configuration

### Environment Variables

Key environment variables for configuration:

```bash
# Database
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5434/iqscaffold_contact_local
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_contact
IQSCAFFOLD_DATABASE_PASSWORD=iqscaffold_password

# RabbitMQ
IQSCAFFOLD_MESSAGING_RABBITMQ_HOST=localhost
IQSCAFFOLD_MESSAGING_RABBITMQ_PORT=5673
IQSCAFFOLD_MESSAGING_RABBITMQ_USERNAME=iqscaffold
IQSCAFFOLD_MESSAGING_RABBITMQ_PASSWORD=iqscaffold_password

# Security
USER_SERVICE_URL=http://user-service:8080
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

- `tenant_info` - Tenant metadata and schema mapping (managed by user service)

### Tenant Schemas

Each tenant has its own schema with:

- `contacts` - Contact information with lead scoring and conversion tracking
  - Basic info: first name, last name, email, phone, job title
  - Lead scoring: lead_score field for qualification
  - Conversion tracking: converted_from_lead_id, converted_at
  - Company association: company_id reference
  - Status tracking: ACTIVE, INACTIVE, LEAD, PROSPECT, CUSTOMER, ARCHIVED
  - Audit fields: created_at, updated_at, created_by, updated_by

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify

# With coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Configuration

- **Unit Tests**: Use H2 in-memory database
- **Integration Tests**: Disabled by default (require tenant schema setup)
- **Cache**: Disabled in tests to avoid ehcache.xml dependency
- **Coverage**: 70% minimum instruction coverage, 50% branch coverage

### Code Quality

The project enforces:

- **Checkstyle**: Google Java Style Guide compliance
- **JaCoCo**: 70% instruction coverage, 50% branch coverage minimum
- **ArchUnit**: Architecture rules and layer dependencies
- **Maven Enforcer**: Java 21+ and Maven 3.9+ requirements

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

1. **Database Connection**: Verify PostgreSQL is running on port 5434 and credentials are correct
2. **RabbitMQ Connection**: Check RabbitMQ service on port 5673 and credentials
3. **JWT Validation**: Ensure user service is accessible at configured URL
4. **Tenant Context**: Verify `X-Tenant-ID` header is present in requests
5. **Cache Issues**: In tests, cache is disabled; in production, ensure Ehcache configuration is valid

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
