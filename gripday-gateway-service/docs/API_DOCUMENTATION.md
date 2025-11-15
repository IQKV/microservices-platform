# API Documentation Access Guide

## Overview

The Gateway Service now provides centralized access to API documentation from all microservices through Swagger UI. This allows developers to explore and test APIs from a single entry point.

## Accessing API Documentation

### Gateway Swagger UI (Aggregated)

Access the main Swagger UI with all services aggregated:

```
http://localhost:8080/swagger-ui.html
```

This provides a dropdown to switch between:

- **Gateway APIs** - Gateway-specific endpoints
- **User Service APIs** - Authentication and user management
- **Bookstore Service APIs** - Bookstore operations (when available)

### Individual Service Documentation

Access service-specific documentation through the gateway:

#### User Service

- Swagger UI: `http://localhost:8080/user-service/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/user-service/api-docs`

#### Bookstore Service

- Swagger UI: `http://localhost:8080/bookstore-service/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/bookstore-service/api-docs`

### Documentation Discovery Endpoint

Get a list of all available API documentation endpoints:

```bash
curl http://localhost:8080/api/v1/docs
```

Response:

```json
{
  "gateway": {
    "swaggerUi": "http://localhost:8080/swagger-ui.html",
    "apiDocs": "http://localhost:8080/api-docs"
  },
  "services": [
    {
      "name": "User Service",
      "description": "Authentication and user management APIs",
      "swaggerUi": "http://localhost:8080/user-service/swagger-ui.html",
      "apiDocs": "http://localhost:8080/user-service/api-docs",
      "directUri": "http://localhost:8080"
    }
  ]
}
```

## Gateway Routes for API Documentation

The following routes have been configured:

### User Service API Docs Route

```yaml
- id: user-service-api-docs
  uri: ${gripday.gateway.routing.services.user-service.uri}
  predicates:
    - Path=/user-service/api-docs,/user-service/api-docs/**
  filters:
    - RewritePath=/user-service/api-docs(?<segment>/?.*), /api-docs${segment}
```

### User Service Swagger UI Route

```yaml
- id: user-service-swagger-ui
  uri: ${gripday.gateway.routing.services.user-service.uri}
  predicates:
    - Path=/user-service/swagger-ui.html,/user-service/swagger-ui/**
  filters:
    - RewritePath=/user-service/swagger-ui(?<segment>/?.*), /swagger-ui${segment}
```

## Security Configuration

All API documentation endpoints are publicly accessible in the local development environment. In production, these should be restricted or disabled.

### Public Paths Configuration

The following paths are configured as public (no authentication required):

```yaml
gripday:
  gateway:
    security:
      public-paths:
        - /api/v1/docs
        - /swagger-ui/**
        - /api-docs/**
        - /user-service/swagger-ui/**
        - /user-service/api-docs/**
        - /bookstore-service/swagger-ui/**
        - /bookstore-service/api-docs/**
```

## Testing API Endpoints

### Using Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Select the service from the dropdown (top-right)
3. Expand an endpoint to view details
4. Click "Try it out" to test the endpoint
5. Fill in parameters and click "Execute"

### Authentication in Swagger UI

For protected endpoints:

1. Click the "Authorize" button (lock icon)
2. Enter your JWT token in the format: `Bearer <your-token>`
3. Click "Authorize"
4. All subsequent requests will include the token

### Getting a JWT Token

First, authenticate to get a token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

Use the `accessToken` in the Authorization header:

```bash
curl http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Configuration Details

### SpringDoc Configuration

Gateway service `application.yml`:

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    urls:
      - name: Gateway APIs
        url: /api-docs
      - name: User Service APIs
        url: /user-service/api-docs
    urls-primary-name: Gateway APIs
```

### OpenAPI Configuration Class

The `OpenApiConfig` class provides:

- Gateway API documentation metadata
- Security scheme definitions (JWT Bearer)
- Grouped API definitions for each service
- Server URLs for different environments

## Direct Service Access

You can also access services directly (bypassing the gateway) during development:

### User Service Direct Access

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```

Note: Direct access uses the service's own port and configuration.

## Production Considerations

### Disabling Documentation in Production

In production environments, consider:

1. **Disable Swagger UI completely:**

```yaml
springdoc:
  swagger-ui:
    enabled: false
```

2. **Restrict access to documentation:**

```yaml
gripday:
  gateway:
    security:
      public-paths:
        # Remove documentation paths from public access
```

3. **Use separate documentation portal:**

- Host static OpenAPI specs in a dedicated documentation site
- Use tools like Redoc or Stoplight for public API documentation

### Environment-Specific Configuration

Use Spring profiles to control documentation availability:

```yaml
# application-production.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## Troubleshooting

### Swagger UI Not Loading

1. Check that the gateway is running: `http://localhost:8080/actuator/health`
2. Verify SpringDoc dependency is in `pom.xml`
3. Check logs for any startup errors
4. Ensure the service URI is correct in configuration

### API Docs Not Showing Service Endpoints

1. Verify the service is running and accessible
2. Check the route configuration in `application.yml`
3. Test direct access to service API docs
4. Check gateway logs for routing errors

### CORS Issues

If testing from a web application:

1. Verify CORS configuration in `application-local.yml`
2. Check that your origin is in `allowed-origins`
3. Ensure `allow-credentials: true` if using authentication

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
