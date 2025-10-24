# Gripday Auth Service

Centralized authentication and user management microservice providing JWT-based authentication, user lifecycle management, and role-based access control for the Gripday platform.

## Quick Start

### Prerequisites
- Java 21
- Docker and Docker Compose
- PostgreSQL 15+
- Redis 7+

### Local Development Setup

1. **Start dependencies:**
```bash
cd gripday-auth-service
docker-compose up -d postgres redis
```

2. **Run database migrations:**
```bash
mvn liquibase:update
```

3. **Start the service:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The service will be available at `http://localhost:8081`

### API Documentation
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8081/v3/api-docs`

## Authentication Endpoints

### User Registration
```bash
curl -X POST http://localhost:8081/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "default"
  }'
```

**Response (201 Created):**
```json
{
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": false,
  "tenantId": "default",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### User Login
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!",
    "rememberMe": false
  }'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800,
  "userContext": {
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"],
    "tenantId": "default"
  }
}
```

### Token Refresh
```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### User Logout
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Configuration

### Environment Variables
```bash
# Database
GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/gripday_auth
GRIPDAY_DATABASE_USERNAME=gripday
GRIPDAY_DATABASE_PASSWORD=password

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# JWT
GRIPDAY_AUTH_JWT_SECRET=your-secret-key
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
```

### Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down
```

## Health Checks
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`

## Troubleshooting

### Common Issues

**Database Connection Failed**
- Verify PostgreSQL is running: `docker-compose ps postgres`
- Check connection settings in `application-local.yml`

**JWT Token Invalid**
- Ensure JWT secret is configured
- Check token expiration times
- Verify token format in Authorization header

**Redis Connection Failed**
- Verify Redis is running: `docker-compose ps redis`
- Check Redis connection settings

### Logs
```bash
# View application logs
docker-compose logs -f auth-service

# View database logs
docker-compose logs -f postgres
```

## Development

### Build
```bash
mvn clean package
```

### Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify
```

### Code Quality
```bash
mvn clean compile -Pcode-quality
```