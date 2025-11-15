# OpenAPI Configuration Guide

## Overview

The Gateway Service provides a **configuration-driven** approach to OpenAPI documentation aggregation. Simply add a service to your configuration, and the gateway automatically:

1. Creates routes for Swagger UI and OpenAPI JSON endpoints
2. Registers the service in the Swagger UI dropdown
3. Exposes documentation through the `/api/v1/docs` discovery endpoint

No code changes required!

## Quick Start

### Adding a New Service

To add OpenAPI documentation for a new service, simply update `application.yml`:

```yaml
gripday:
  gateway:
    routing:
      services:
        my-new-service:
          uri: http://localhost:8082
          path: /v1/myservice/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "My New Service APIs"
            description: "Description of my service"
            context-path: "my-new-service"
```

That's it! The gateway will automatically:

- Create routes: `/my-new-service/swagger-ui.html` and `/my-new-service/api-docs`
- Add "My New Service APIs" to the Swagger UI dropdown
- Include it in the `/api/v1/docs` discovery endpoint

### Disabling OpenAPI for a Service

To disable OpenAPI documentation for a service:

```yaml
gripday:
  gateway:
    routing:
      services:
        my-service:
          # ... other config ...
          openapi:
            enabled: false # Documentation disabled
```

Or simply omit the `openapi` section entirely.

## Configuration Properties

### Service OpenAPI Configuration

Each service can have an `openapi` configuration block:

```yaml
openapi:
  enabled: true # Enable/disable OpenAPI for this service
  display-name: "Service Display Name" # Name shown in Swagger UI dropdown
  description: "Service description" # Description for documentation
  context-path: "service-name" # URL path prefix (default: service key name)
```

#### Property Details

| Property       | Type    | Required | Default             | Description                                   |
| -------------- | ------- | -------- | ------------------- | --------------------------------------------- |
| `enabled`      | boolean | No       | false               | Enable OpenAPI documentation for this service |
| `display-name` | string  | No       | "Service API"       | Display name in Swagger UI dropdown           |
| `description`  | string  | No       | "API documentation" | Service description                           |
| `context-path` | string  | No       | service key         | URL path prefix for documentation routes      |

### Context Path Examples

The `context-path` determines the URL structure for documentation:

```yaml
# Example 1: Default (uses service key)
my-service:
  openapi:
    enabled: true
    # context-path not specified, defaults to "my-service"
# Results in: /my-service/swagger-ui.html

# Example 2: Custom path
user-service:
  openapi:
    enabled: true
    context-path: "users"
# Results in: /users/swagger-ui.html

# Example 3: Nested path
inventory-service:
  openapi:
    enabled: true
    context-path: "api/inventory"
# Results in: /api/inventory/swagger-ui.html
```

## Complete Configuration Example

### application.yml

```yaml
gripday:
  gateway:
    routing:
      services:
        # User Service - Authentication and user management
        user-service:
          uri: http://localhost:8080
          path: /v1/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "User Service APIs"
            description: "Authentication and user management APIs"
            context-path: "user-service"

        # Bookstore Service - Book catalog and inventory
        bookstore-service:
          uri: http://localhost:8081
          path: /v1/bookstore/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "Bookstore Service APIs"
            description: "Book catalog and inventory management"
            context-path: "bookstore-service"

        # Order Service - Order processing
        order-service:
          uri: http://localhost:8083
          path: /v1/orders/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: true
            display-name: "Order Service APIs"
            description: "Order processing and fulfillment"
            context-path: "order-service"

        # Payment Service - Payment processing (OpenAPI disabled)
        payment-service:
          uri: http://localhost:8084
          path: /v1/payments/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          openapi:
            enabled: false # Documentation not exposed through gateway

        # Internal Service - No OpenAPI configuration
        internal-service:
          uri: http://localhost:8085
          path: /v1/internal/**
          enabled: true
          connect-timeout: 5000
          response-timeout: 30000
          # No openapi section = documentation disabled
```

### Environment-Specific Configuration

Override service URIs per environment using environment variables:

```bash
# Local development
GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI=http://localhost:8080

# Staging
GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI=http://user-service.staging:8080

# Production
GRIPDAY_GATEWAY_ROUTING_USER_SERVICE_URI=http://user-service.prod:8080
```

Or use Spring profiles:

```yaml
# application-production.yml
gripday:
  gateway:
    routing:
      services:
        user-service:
          uri: http://user-service.prod:8080
          openapi:
            enabled: false # Disable in production
```

## Generated Routes

For each service with OpenAPI enabled, the gateway automatically creates:

### Swagger UI Route

```
Pattern: /{context-path}/swagger-ui.html
         /{context-path}/swagger-ui/**

Rewrite: /{context-path}/swagger-ui/** -> /swagger-ui/**

Example: /user-service/swagger-ui.html -> http://localhost:8080/swagger-ui.html
```

### API Docs Route

```
Pattern: /{context-path}/api-docs
         /{context-path}/api-docs/**

Rewrite: /{context-path}/api-docs/** -> /api-docs/**

Example: /user-service/api-docs -> http://localhost:8080/api-docs
```

## Accessing Documentation

### Gateway Swagger UI (Aggregated)

Access all services from one interface:

```
http://localhost:8080/swagger-ui.html
```

Use the dropdown in the top-right corner to switch between services.

### Individual Service Documentation

Access service-specific documentation:

```
http://localhost:8080/{context-path}/swagger-ui.html
http://localhost:8080/{context-path}/api-docs
```

Examples:

- User Service: `http://localhost:8080/user-service/swagger-ui.html`
- Bookstore Service: `http://localhost:8080/bookstore-service/swagger-ui.html`

### Documentation Discovery API

Get a list of all available documentation endpoints:

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
      "name": "user-service",
      "displayName": "User Service APIs",
      "description": "Authentication and user management APIs",
      "swaggerUi": "http://localhost:8080/user-service/swagger-ui.html",
      "apiDocs": "http://localhost:8080/user-service/api-docs",
      "directUri": "http://localhost:8080",
      "enabled": true
    },
    {
      "name": "bookstore-service",
      "displayName": "Bookstore Service APIs",
      "description": "Book catalog and inventory management",
      "swaggerUi": "http://localhost:8080/bookstore-service/swagger-ui.html",
      "apiDocs": "http://localhost:8080/bookstore-service/api-docs",
      "directUri": "http://localhost:8081",
      "enabled": true
    }
  ],
  "totalServices": 2,
  "message": "Access Swagger UI through the gateway for aggregated API documentation"
}
```

## Security Configuration

### Making Documentation Public

Add documentation paths to public paths in your configuration:

```yaml
gripday:
  gateway:
    security:
      public-paths:
        # Gateway documentation
        - /api/v1/docs
        - /swagger-ui/**
        - /api-docs/**

        # Service-specific documentation (add for each service)
        - /user-service/swagger-ui/**
        - /user-service/api-docs/**
        - /bookstore-service/swagger-ui/**
        - /bookstore-service/api-docs/**
```

### Dynamic Public Path Generation

For a more maintainable approach, you can use wildcards:

```yaml
gripday:
  gateway:
    security:
      public-paths:
        - /api/v1/docs
        - /swagger-ui/**
        - /api-docs/**
        - /*/swagger-ui/** # All service Swagger UIs
        - /*/api-docs/** # All service API docs
```

### Production Security

In production, disable or restrict documentation:

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
            enabled: false # Disable for all services
```

## Architecture

### Components

1. **GripdayProperties** - Configuration properties with OpenAPI settings per service
2. **DynamicOpenApiRoutesConfiguration** - Automatically creates gateway routes
3. **OpenApiConfig** - Configures OpenAPI groups for Swagger UI
4. **SpringDocConfiguration** - Registers service URLs in Swagger UI dropdown
5. **ApiDocumentationResource** - REST endpoint for documentation discovery

### Flow Diagram

```
Configuration (application.yml)
    ↓
GripdayProperties (parsed at startup)
    ↓
    ├─→ DynamicOpenApiRoutesConfiguration
    │   └─→ Creates routes: /{service}/swagger-ui/**, /{service}/api-docs/**
    │
    ├─→ OpenApiConfig
    │   └─→ Creates GroupedOpenApi beans for each service
    │
    ├─→ SpringDocConfiguration
    │   └─→ Registers URLs in Swagger UI dropdown
    │
    └─→ ApiDocumentationResource
        └─→ Exposes /api/v1/docs discovery endpoint
```

### Request Flow

```
User Request: /user-service/swagger-ui.html
    ↓
Gateway Route Matching
    ↓
DynamicOpenApiRoutesConfiguration route
    ↓
Rewrite: /user-service/swagger-ui.html → /swagger-ui.html
    ↓
Forward to: http://localhost:8080/swagger-ui.html
    ↓
User Service responds with Swagger UI
```

## Advanced Usage

### Custom Context Paths

Use custom context paths for better URL organization:

```yaml
gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            context-path: "api/users" # Custom nested path
# Results in: /api/users/swagger-ui.html
```

### Conditional Documentation

Enable documentation only in specific environments:

```yaml
# application-local.yml
gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            enabled: true

# application-production.yml
gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            enabled: false
```

### Multiple Service Instances

For load-balanced services, point to the load balancer:

```yaml
gripday:
  gateway:
    routing:
      services:
        user-service:
          uri: http://user-service-lb:8080 # Load balancer
          openapi:
            enabled: true
```

## Troubleshooting

### Documentation Not Appearing

1. **Check service is enabled:**

```yaml
user-service:
  enabled: true # Must be true
  openapi:
    enabled: true # Must be true
```

2. **Check logs for registration:**

```
INFO  DynamicOpenApiRoutesConfiguration - Created Swagger UI route for 'user-service'
INFO  SpringDocConfiguration - Registered OpenAPI documentation: User Service APIs
```

3. **Verify service is accessible:**

```bash
curl http://localhost:8080/api-docs
```

### Routes Not Working

1. **Check route creation in logs:**

```
INFO  DynamicOpenApiRoutesConfiguration - Created Swagger UI route for 'user-service': /user-service/swagger-ui -> http://localhost:8080/swagger-ui
```

2. **Test direct service access:**

```bash
curl http://localhost:8080/swagger-ui.html
```

3. **Verify public paths configuration:**

```yaml
public-paths:
  - /user-service/swagger-ui/**
  - /user-service/api-docs/**
```

### Service Not in Dropdown

1. **Check SpringDoc registration:**

```
INFO  SpringDocConfiguration - Registered OpenAPI documentation: User Service APIs -> /user-service/api-docs
```

2. **Verify display-name is set:**

```yaml
openapi:
  display-name: "User Service APIs" # Required for dropdown
```

3. **Check browser console for errors**

## Migration Guide

### From Hardcoded Routes

**Before:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-swagger-ui
          uri: http://localhost:8080
          predicates:
            - Path=/user-service/swagger-ui/**
          filters:
            - RewritePath=/user-service/swagger-ui(?<segment>/?.*), /swagger-ui$\{segment}
```

**After:**

```yaml
gripday:
  gateway:
    routing:
      services:
        user-service:
          uri: http://localhost:8080
          openapi:
            enabled: true
            display-name: "User Service APIs"
            context-path: "user-service"
```

### From Manual Configuration

**Before:**

```java
@Bean
public GroupedOpenApi userServiceApi() {
  return GroupedOpenApi.builder().group("user-service").displayName("User Service APIs").pathsToMatch("/api/v1/auth/**").build();
}
```

**After:**
Just add to configuration - no code changes needed!

## Best Practices

1. **Use descriptive display names:**

```yaml
display-name: "User Service APIs"  # Good
display-name: "User"               # Less clear
```

2. **Provide meaningful descriptions:**

```yaml
description: "Authentication and user management APIs"  # Good
description: "User service"                             # Less helpful
```

3. **Use consistent context paths:**

```yaml
user-service:
  openapi:
    context-path: "user-service" # Matches service name

bookstore-service:
  openapi:
    context-path: "bookstore-service" # Consistent pattern
```

4. **Disable in production:**

```yaml
# application-production.yml
openapi:
  enabled: false
```

5. **Document your services:**
   Ensure downstream services have proper OpenAPI annotations and configuration.

## Examples

See the complete working examples in:

- `application.yml` - Base configuration
- `application-local.yml` - Development configuration
- `application-production.yml` - Production configuration (documentation disabled)
