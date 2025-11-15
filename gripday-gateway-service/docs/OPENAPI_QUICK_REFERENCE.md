# OpenAPI Quick Reference

## Add a New Service (3 Steps)

### 1. Add Service Configuration

```yaml
gripday:
  gateway:
    routing:
      services:
        my-service:
          uri: http://localhost:8082
          path: /v1/myservice/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "My Service APIs"
            description: "My service description"
            context-path: "my-service"
```

### 2. Add Public Paths (Optional)

```yaml
gripday:
  gateway:
    security:
      public-paths:
        - /my-service/swagger-ui/**
        - /my-service/api-docs/**
```

### 3. Restart Gateway

That's it! No code changes needed.

## Access Documentation

| What               | URL                                                    |
| ------------------ | ------------------------------------------------------ |
| Gateway Swagger UI | `http://localhost:8080/swagger-ui.html`                |
| Service Swagger UI | `http://localhost:8080/{context-path}/swagger-ui.html` |
| Service API Docs   | `http://localhost:8080/{context-path}/api-docs`        |
| Discovery Endpoint | `http://localhost:8080/api/v1/docs`                    |

## Configuration Properties

```yaml
openapi:
  enabled: true # Enable/disable (default: false)
  display-name: "Service Name" # Swagger UI dropdown name
  description: "Description" # Service description
  context-path: "service-path" # URL path (default: service key)
```

## Common Patterns

### Enable for All Environments

```yaml
openapi:
  enabled: true
```

### Enable Only in Development

```yaml
# application-local.yml
openapi:
  enabled: true

# application-production.yml
openapi:
  enabled: false
```

### Custom Context Path

```yaml
openapi:
  context-path: "api/users" # Results in /api/users/swagger-ui.html
```

### Disable Documentation

```yaml
openapi:
  enabled: false
# Or omit the openapi section entirely
```

## Testing

```bash
# List all documentation endpoints
curl http://localhost:8080/api/v1/docs

# Access service OpenAPI JSON
curl http://localhost:8080/user-service/api-docs

# Open Swagger UI in browser
open http://localhost:8080/swagger-ui.html
```

## Troubleshooting

| Issue                     | Solution                                           |
| ------------------------- | -------------------------------------------------- |
| Service not in dropdown   | Check `enabled: true` and `display-name` is set    |
| 404 on documentation URL  | Verify `context-path` and service `uri`            |
| Documentation not loading | Check service is running and accessible            |
| Routes not created        | Check logs for "Created Swagger UI route" messages |

## Environment Variables

Override service URIs:

```bash
GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI=http://user-service:8080
GRIPDAY_GATEWAY_ROUTING_BOOKSTORE_SERVICE_URI=http://bookstore-service:8081
```

## Production Checklist

- [ ] Disable OpenAPI in production config
- [ ] Remove documentation paths from public paths
- [ ] Disable SpringDoc in production
- [ ] Use separate documentation portal if needed

```yaml
# application-production.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            enabled: false
```

## Complete Example

```yaml
gripday:
  gateway:
    routing:
      services:
        user-service:
          uri: http://localhost:8080
          path: /v1/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "User Service APIs"
            description: "Authentication and user management"
            context-path: "user-service"
    security:
      public-paths:
        - /api/v1/docs
        - /swagger-ui/**
        - /api-docs/**
        - /user-service/swagger-ui/**
        - /user-service/api-docs/**
```

## Generated Routes

For each service, the gateway automatically creates:

```
/{context-path}/swagger-ui.html     → {service-uri}/swagger-ui.html
/{context-path}/swagger-ui/**       → {service-uri}/swagger-ui/**
/{context-path}/api-docs            → {service-uri}/api-docs
/{context-path}/api-docs/**         → {service-uri}/api-docs/**
```

## Architecture Components

- **GripdayProperties** - Configuration binding
- **DynamicOpenApiRoutesConfiguration** - Route creation
- **OpenApiConfig** - OpenAPI groups
- **SpringDocConfiguration** - Swagger UI registration
- **ApiDocumentationResource** - Discovery endpoint

## Further Reading

- [OPENAPI_CONFIGURATION_GUIDE.md](OPENAPI_CONFIGURATION_GUIDE.md) - Complete guide
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - Usage documentation
- [ROUTING_SUMMARY.md](ROUTING_SUMMARY.md) - All gateway routes
