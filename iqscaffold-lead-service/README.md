# IQ Scaffold Lead Service

A comprehensive CRM lead management service built with Spring Boot, providing multi-tenant lead capture, qualification, scoring, and conversion capabilities.

## Features

- **Lead Management**: Create, read, update, and delete leads with comprehensive tracking
- **Lead Qualification**: Automatic and manual lead qualification with scoring
- **Lead Scoring**: Configurable lead scoring system (0-100)
- **Lead Assignment**: Assign leads to sales representatives
- **Lead Conversion**: Convert qualified leads to contacts
- **Lead Sources**: Track lead sources (Website, Referral, Cold Call, etc.)
- **Lead Notes**: Add and manage notes for each lead
- **Activity Tracking**: Log all lead interactions and activities
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
   docker-compose up -d postgres-lead redis-lead rabbitmq-lead
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the application**:
   - API: http://localhost:8084
   - Swagger UI: http://localhost:8084/swagger-ui.html
   - Health Check: http://localhost:8084/actuator/health

### Docker Development

```bash
# Build and run all services
docker-compose up --build

# Run in detached mode
docker-compose up -d
```

## API Endpoints

### Leads
- `GET /api/v1/leads` - List leads with pagination and filters
- `POST /api/v1/leads` - Create new lead
- `GET /api/v1/leads/{id}` - Get lead by ID
- `PUT /api/v1/leads/{id}` - Update lead
- `DELETE /api/v1/leads/{id}` - Delete lead
- `GET /api/v1/leads/search?q={term}` - Search leads
- `GET /api/v1/leads?status={status}` - Filter by status
- `GET /api/v1/leads?source={source}` - Filter by source
- `GET /api/v1/leads?assignedTo={user}` - Filter by assignment

### Lead Actions
- `PATCH /api/v1/leads/{id}/qualify` - Qualify lead
- `PATCH /api/v1/leads/{id}/disqualify` - Disqualify lead
- `PATCH /api/v1/leads/{id}/assign` - Assign lead to user
- `PATCH /api/v1/leads/{id}/score` - Update lead score
- `POST /api/v1/leads/{id}/convert` - Convert lead to contact

### Lead Notes
- `GET /api/v1/leads/{id}/notes` - Get lead notes
- `POST /api/v1/leads/{id}/notes` - Add note to lead
- `PUT /api/v1/leads/{id}/notes/{noteId}` - Update note
- `DELETE /api/v1/leads/{id}/notes/{noteId}` - Delete note
- `PATCH /api/v1/leads/{id}/notes/{noteId}/pin` - Pin/unpin note

### Lead Activities
- `GET /api/v1/leads/{id}/activities` - Get lead activity timeline
- `POST /api/v1/leads/{id}/activities` - Log activity

### Lead Sources
- `GET /api/v1/leads/sources` - List available lead sources

## Configuration

### Environment Variables

Key environment variables for configuration:

```bash
# Database
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5432/iqscaffold_lead_local
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_lead
IQSCAFFOLD_DATABASE_PASSWORD=iqscaffold_password

# Redis
IQSCAFFOLD_CACHE_REDIS_HOST=localhost
IQSCAFFOLD_CACHE_REDIS_PORT=6379
IQSCAFFOLD_CACHE_REDIS_DATABASE=3

# RabbitMQ
IQSCAFFOLD_MESSAGING_RABBITMQ_HOST=localhost
IQSCAFFOLD_MESSAGING_RABBITMQ_PORT=5672

# Security
USER_SERVICE_URL=http://localhost:8081
JWT_ISSUER=iqscaffold-user-service

# Lead Features
LEAD_ENABLE_AUTO_SCORING=true
LEAD_ENABLE_AUTO_QUALIFICATION=false
```

### Profiles

- `local` - Local development with debug logging
- `staging` - Staging environment configuration
- `production` - Production environment with JSON logging

## Lead Lifecycle

```
1. Lead Capture
   └─> Lead created with status: NEW
       Source tracked (Website, Referral, etc.)

2. Lead Qualification
   └─> Manual or automatic qualification
       Score updated (0-100)
       Status: NEW → CONTACTED → QUALIFIED

3. Lead Assignment
   └─> Assign to sales representative
       Notifications sent

4. Lead Conversion
   └─> Convert qualified lead to contact
       Status: CONVERTED
       Contact created in contact-service
       Lead archived with conversion data

5. Alternative: Lead Lost
   └─> Mark as LOST or UNQUALIFIED
       Reason tracked in notes
```

## Lead Status Flow

```
NEW → CONTACTED → QUALIFIED → CONVERTED
  ↓       ↓           ↓
LOST    LOST    UNQUALIFIED
```

## Lead Scoring

- **Default Score**: 0
- **Max Score**: 100
- **Qualification Threshold**: 60 (configurable)
- **Auto-scoring**: Configurable based on lead attributes

### Scoring Factors (Example)
- Email provided: +10
- Phone provided: +10
- Company provided: +15
- Job title provided: +10
- Source quality: +5 to +20
- Engagement activities: +5 per activity

## Multi-tenancy

The service uses schema-per-tenant isolation:

1. **Tenant Identification**: Via `X-Tenant-ID` header
2. **Schema Management**: Automatic schema creation and migration
3. **Data Isolation**: Complete separation between tenants

## Database Schema

### System Schema (public)
- `tenant_info` - Tenant metadata and schema mapping

### Tenant Schemas
- `leads` - Lead information with scoring and qualification
- `lead_sources` - Available lead sources
- `lead_notes` - Notes attached to leads
- `lead_activities` - Activity timeline for leads

## Events Published

The service publishes events to RabbitMQ:

```
lead.created
  - When new lead is created
  - Payload: { leadId, email, source, status }

lead.qualified
  - When lead is qualified
  - Payload: { leadId, score, qualifiedBy }

lead.assigned
  - When lead is assigned
  - Payload: { leadId, assignedTo, assignedBy }

lead.converted
  - When lead is converted to contact
  - Payload: { leadId, contactId, convertedAt }

lead.status.changed
  - When lead status changes
  - Payload: { leadId, oldStatus, newStatus }
```

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

## Integration with Other Services

### User Service
- JWT authentication
- User information for assignments

### Contact Service
- Lead conversion creates contacts
- Event-driven communication

### Pipeline Service (Future)
- Lead pipeline management
- Stage tracking

## Troubleshooting

### Common Issues

1. **Database Connection**: Verify PostgreSQL is running and credentials are correct
2. **Redis Connection**: Check Redis service and port configuration
3. **JWT Validation**: Ensure user service is accessible and JWT configuration is correct
4. **Lead Conversion**: Verify contact service is running and accessible

### Logs

```bash
# View application logs
docker-compose logs lead-service

# Follow logs
docker-compose logs -f lead-service
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
