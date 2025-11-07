# Gripday Gateway Service

API Gateway providing intelligent routing, JWT authentication, rate limiting, and circuit breaker functionality for the Gripday microservices platform.

## Quick Start

### Prerequisites

- Java 21
- Docker and Docker Compose
- Redis 7+
- Auth Service running

### Local Development Setup

1. **Start dependencies:**

```bash
cd gripday-gateway-service
docker compose up -d redis
```

2. **Ensure Auth Service is running:**

```bash
# Auth service should be available at http://localhost:8081
curl http://localhost:8081/actuator/health
```

3. **Start the gateway:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The gateway will be available at `http://localhost:8080`

### API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## Routing and Security

### Service Routes

The gateway automatically routes requests to backend services:

```bash
# Auth service routes (no authentication required)
curl http://localhost:8080/api/v1/auth/login
curl http://localhost:8080/api/v1/auth/signup

# Protected routes (JWT token required)
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users
```

### Authentication Flow

1. **Obtain JWT token from Auth Service:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

2. **Use token for protected endpoints:**

```bash
curl -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
     -H "X-Tenant-ID: default" \
     http://localhost:8080/api/v1/users
```

### Multi-Tenant Support

The gateway supports tenant isolation through multiple methods:

**Header-based tenant identification:**

```bash
curl -H "Authorization: Bearer <token>" \
     -H "X-Tenant-ID: tenant-123" \
     http://localhost:8080/api/v1/users
```

**Subdomain-based routing:**

```bash
# Routes to tenant-123 context
curl -H "Authorization: Bearer <token>" \
     http://tenant-123.localhost:8080/api/v1/users
```

### Rate Limiting

The gateway implements Redis-backed rate limiting:

```bash
# Rate limit headers in response
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1642248000

# Rate limit exceeded
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1642248060
```

### Circuit Breaker

Circuit breaker status and fallback responses:

```bash
# Circuit breaker open - fallback response
HTTP/1.1 503 Service Unavailable
{
  "error": {
    "code": "SERVICE_UNAVAILABLE",
    "message": "Service temporarily unavailable",
    "details": "Circuit breaker is open for auth-service"
  }
}
```

## Configuration

### Environment Variables

```bash
# Gateway
GRIPDAY_GATEWAY_PORT=8080
GRIPDAY_GATEWAY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Redis (for rate limiting and caching)
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# Auth Service
GRIPDAY_GATEWAY_AUTH_SERVICE_URL=http://localhost:8081

# Rate Limiting
GRIPDAY_GATEWAY_RATE_LIMITING_DEFAULT_REQUESTS_PER_MINUTE=100
GRIPDAY_GATEWAY_RATE_LIMITING_BURST_CAPACITY=20

# Circuit Breaker
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50
GRIPDAY_GATEWAY_CIRCUIT_BREAKER_WAIT_DURATION_IN_OPEN_STATE=PT30S

# Multi-tenant
GRIPDAY_TENANT_HEADER_NAME=X-Tenant-ID
GRIPDAY_TENANT_SUBDOMAIN_ENABLED=true
```

### Service Discovery

Configure backend service routes in `application-local.yml`:

```yaml
gripday:
  gateway:
    routes:
      - id: auth-service
        uri: http://localhost:8081
        predicates:
          - Path=/api/v1/auth/**
        filters:
          - name: RequestRateLimiter
            args:
              redis-rate-limiter.replenishRate: 10
              redis-rate-limiter.burstCapacity: 20
```

### Docker Compose

```bash
# Start gateway with dependencies
docker compose up -d

# View logs
docker-compose logs -f gateway-service

# Stop services
docker-compose down
```

## Health Checks

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Gateway Routes: `GET /actuator/gateway/routes`
- Circuit Breakers: `GET /actuator/circuitbreakers`

## Troubleshooting

### Common Issues

**Service Route Not Found (404)**

- Check service registration in `application.yml`
- Verify backend service is running and healthy
- Check gateway route configuration

**Authentication Failed (401)**

- Verify JWT token is valid and not expired
- Check Authorization header format: `Bearer <token>`
- Ensure Auth Service is accessible

**Rate Limit Exceeded (429)**

- Check rate limiting configuration
- Verify Redis connection for rate limiting storage
- Review rate limit headers in response

**Circuit Breaker Open (503)**

- Check backend service health
- Review circuit breaker metrics: `/actuator/circuitbreakers`
- Wait for circuit breaker to transition to half-open state

### Logs

```bash
# View gateway logs
docker-compose logs -f gateway-service

# View Redis logs
docker-compose logs -f redis

# Enable debug logging
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG
```

### Monitoring

```bash
# Check gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# View metrics
curl http://localhost:8080/actuator/metrics/gateway.requests
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

### Adding New Service Routes

1. **Configure route in application.yml:**

```yaml
gripday:
  gateway:
    routes:
      - id: new-service
        uri: http://localhost:8082
        predicates:
          - Path=/api/v1/newservice/**
        filters:
          - name: JwtAuthenticationFilter
          - name: RequestRateLimiter
```

2. **Update Docker Compose:**

```yaml
services:
  new-service:
    image: gripday/new-service:latest
    ports:
      - "8082:8082"
    networks:
      - gripday-network
```
