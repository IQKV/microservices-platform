# Configuration-Driven OpenAPI Solution

## Overview

The Gateway Service now provides a **zero-code, configuration-driven** approach to OpenAPI documentation aggregation. This solution eliminates the need for manual route configuration and code changes when adding new services.

## Key Features

### 1. Automatic Route Generation

Routes for Swagger UI and OpenAPI JSON are automatically created based on configuration.

### 2. Dynamic Service Discovery

Services are automatically discovered and registered in the Swagger UI dropdown.

### 3. Zero Code Changes

Add new services by simply updating YAML configuration - no Java code required.

### 4. Centralized Configuration

All service documentation settings in one place: `application.yml`

### 5. Environment-Specific Control

Enable/disable documentation per environment using Spring profiles.

## Architecture

### Components Created

1. **Enhanced GripdayProperties**
   - Added `OpenApiProperties` nested record
   - Supports per-service OpenAPI configuration
   - Location: `config/GripdayProperties.java`

2. **DynamicOpenApiRoutesConfiguration**
   - Automatically creates gateway routes for documentation
   - Generates Swagger UI and API docs routes
   - Location: `config/DynamicOpenApiRoutesConfiguration.java`

3. **OpenApiConfig**
   - Dynamic OpenAPI group configuration
   - Reads from properties to create groups
   - Location: `config/OpenApiConfig.java`

4. **SpringDocConfiguration**
   - Registers service URLs in Swagger UI dropdown
   - Runs at application startup
   - Location: `config/SpringDocConfiguration.java`

5. **ApiDocumentationResource**
   - REST endpoint for documentation discovery
   - Dynamically generates service list
   - Location: `presentation/web/ApiDocumentationResource.java`

### Data Flow

```
application.yml
    ↓
GripdayProperties (Spring Boot binding)
    ↓
    ├─→ DynamicOpenApiRoutesConfiguration
    │   └─→ Creates: /{service}/swagger-ui/**, /{service}/api-docs/**
    │
    ├─→ OpenApiConfig
    │   └─→ Creates: GroupedOpenApi beans
    │
    ├─→ SpringDocConfiguration
    │   └─→ Registers: Swagger UI dropdown entries
    │
    └─→ ApiDocumentationResource
        └─→ Exposes: GET /api/v1/docs
```

## Configuration Schema

### Service Configuration

```yaml
gripday:
  gateway:
    routing:
      services:
        <service-name>: # Unique service identifier
          uri: <service-url> # Service base URL
          path: <path-pattern> # Route path pattern
          enabled: <boolean> # Enable/disable service
          connect-timeout: <milliseconds> # Connection timeout
          response-timeout: <milliseconds> # Response timeout
          openapi: # OpenAPI configuration
            enabled: <boolean> # Enable/disable docs
            display-name: <string> # Swagger UI display name
            description: <string> # Service description
            context-path: <string> # Documentation URL path
```

### Example Configuration

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
            description: "Authentication and user management APIs"
            context-path: "user-service"
```

## Generated Artifacts

For each service with `openapi.enabled: true`, the system automatically generates:

### 1. Gateway Routes

```yaml
# Swagger UI Route
/{context-path}/swagger-ui.html
/{context-path}/swagger-ui/**
  → Rewrites to: /swagger-ui/**
  → Forwards to: {service-uri}/swagger-ui/**

# API Docs Route
/{context-path}/api-docs
/{context-path}/api-docs/**
  → Rewrites to: /api-docs/**
  → Forwards to: {service-uri}/api-docs/**
```

### 2. OpenAPI Groups

```java
GroupedOpenApi.builder()
    .group(serviceName)
    .displayName(openApiConfig.displayName())
    .pathsToMatch(contextPath + "/**")
    .build()
```

### 3. Swagger UI Entries

Dropdown entries in Swagger UI with service display names.

### 4. Discovery Endpoint Response

```json
{
  "services": [
    {
      "name": "user-service",
      "displayName": "User Service APIs",
      "description": "Authentication and user management APIs",
      "swaggerUi": "http://localhost:8080/user-service/swagger-ui.html",
      "apiDocs": "http://localhost:8080/user-service/api-docs",
      "directUri": "http://localhost:8080",
      "enabled": true
    }
  ]
}
```

## Usage Examples

### Adding a Service

**Before (Manual Approach):**

1. Add route in `application.yml`
2. Create `@Bean` method in `OpenApiConfig.java`
3. Update `ApiDocumentationResource.java`
4. Add public paths to security configuration
5. Rebuild and redeploy

**After (Configuration-Driven):**

1. Add service configuration in `application.yml`
2. Restart application

### Configuration Example

```yaml
# Add this to application.yml
gripday:
  gateway:
    routing:
      services:
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
```

Result:

- Routes created: `/order-service/swagger-ui.html`, `/order-service/api-docs`
- Swagger UI dropdown: "Order Service APIs"
- Discovery endpoint: Includes order-service

### Environment-Specific Configuration

```yaml
# application-local.yml (Development)
gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            enabled: true

# application-production.yml (Production)
gripday:
  gateway:
    routing:
      services:
        user-service:
          openapi:
            enabled: false  # Disabled in production
```

## Benefits

### 1. Maintainability

- Single source of truth for service configuration
- No scattered code changes across multiple files
- Easy to understand and modify

### 2. Scalability

- Add unlimited services without code changes
- Consistent pattern for all services
- Automatic route generation

### 3. Flexibility

- Per-service configuration
- Environment-specific settings
- Easy enable/disable

### 4. Developer Experience

- Simple YAML configuration
- No Java knowledge required for adding services
- Clear, self-documenting configuration

### 5. Consistency

- All services follow the same pattern
- Standardized URL structure
- Uniform documentation access

## Migration Path

### From Hardcoded Routes

**Old Approach:**

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
        - id: user-service-api-docs
          uri: http://localhost:8080
          predicates:
            - Path=/user-service/api-docs/**
          filters:
            - RewritePath=/user-service/api-docs(?<segment>/?.*), /api-docs$\{segment}
```

**New Approach:**

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

### From Manual Bean Configuration

**Old Approach:**

```java
@Bean
public GroupedOpenApi userServiceApi() {
  return GroupedOpenApi.builder().group("user-service").displayName("User Service APIs").pathsToMatch("/api/v1/auth/**").build();
}

@Bean
public GroupedOpenApi bookstoreServiceApi() {
  return GroupedOpenApi.builder().group("bookstore-service").displayName("Bookstore Service APIs").pathsToMatch("/api/v1/bookstore/**").build();
}
```

**New Approach:**
Configuration only - beans are created automatically!

## Best Practices

### 1. Naming Conventions

```yaml
# Use kebab-case for service names
user-service:        # Good
userService:         # Avoid
user_service:        # Avoid

# Use descriptive display names
display-name: "User Service APIs"     # Good
display-name: "User"                  # Less clear
```

### 2. Context Paths

```yaml
# Match service name for consistency
user-service:
  openapi:
    context-path: "user-service"  # Consistent

# Or use custom paths for organization
user-service:
  openapi:
    context-path: "api/users"     # Custom organization
```

### 3. Descriptions

```yaml
# Provide meaningful descriptions
description: "Authentication and user management APIs"  # Good
description: "User service"                             # Less helpful
```

### 4. Environment Configuration

```yaml
# Base configuration (application.yml)
user-service:
  uri: http://localhost:8080
  openapi:
    enabled: true

# Production override (application-production.yml)
user-service:
  openapi:
    enabled: false  # Disable in production
```

### 5. Security

```yaml
# Add documentation paths to public paths
gripday:
  gateway:
    security:
      public-paths:
        - /*/swagger-ui/** # Wildcard for all services
        - /*/api-docs/** # Wildcard for all services
```

## Troubleshooting

### Routes Not Created

**Check logs for:**

```
INFO  DynamicOpenApiRoutesConfiguration - Created Swagger UI route for 'user-service'
INFO  DynamicOpenApiRoutesConfiguration - Created API Docs route for 'user-service'
```

**Verify configuration:**

```yaml
user-service:
  enabled: true # Service must be enabled
  openapi:
    enabled: true # OpenAPI must be enabled
```

### Service Not in Dropdown

**Check logs for:**

```
INFO  SpringDocConfiguration - Registered OpenAPI documentation: User Service APIs -> /user-service/api-docs
```

**Verify:**

- `display-name` is set
- `enabled: true` for both service and openapi
- Application restarted after configuration change

### 404 on Documentation URL

**Verify:**

- Service is running and accessible
- `uri` is correct in configuration
- `context-path` matches URL being accessed
- Public paths include documentation paths

## Performance Considerations

### Startup Time

- Route generation happens at startup
- Minimal impact: O(n) where n = number of services
- Typical overhead: < 100ms for 10 services

### Runtime Performance

- No runtime overhead
- Routes are static after startup
- Same performance as manually configured routes

### Memory Usage

- Minimal additional memory
- One RouteDefinition per service
- One GroupedOpenApi bean per service

## Future Enhancements

### Potential Improvements

1. **Service Health Integration**
   - Automatically disable documentation for unhealthy services
   - Show service status in discovery endpoint

2. **Dynamic Reloading**
   - Reload configuration without restart
   - Hot-reload service documentation

3. **API Versioning Support**
   - Multiple API versions per service
   - Version-specific documentation

4. **Custom Route Filters**
   - Per-service filter configuration
   - Custom transformation rules

5. **Documentation Caching**
   - Cache OpenAPI specs
   - Reduce load on downstream services

## Conclusion

This configuration-driven approach provides a scalable, maintainable solution for OpenAPI documentation aggregation in the gateway. It eliminates manual code changes, reduces errors, and provides a consistent pattern for all services.

### Key Takeaways

- ✅ Zero code changes to add new services
- ✅ Configuration-driven architecture
- ✅ Automatic route generation
- ✅ Dynamic service discovery
- ✅ Environment-specific control
- ✅ Centralized configuration
- ✅ Consistent patterns

### Getting Started

1. Read [OPENAPI_QUICK_REFERENCE.md](OPENAPI_QUICK_REFERENCE.md)
2. Add your service configuration
3. Restart the gateway
4. Access documentation at `http://localhost:8080/swagger-ui.html`

For detailed information, see [OPENAPI_CONFIGURATION_GUIDE.md](OPENAPI_CONFIGURATION_GUIDE.md).
