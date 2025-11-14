# Dynamic Route Configuration

## Overview

The gateway service supports dynamic route enabling/disabling through the `enabled` property for each service. This allows you to control which microservices are accessible through the gateway without code changes.

## Configuration

### Basic Structure

```yaml
gripday:
  gateway:
    routing:
      services:
        service-name:
          uri: http://service-host:port
          path: /v1/service/**
          enabled: true # Set to false to disable this service
          connect-timeout: 5000
          response-timeout: 30000
```

### Properties

- **uri**: The base URI of the downstream service
- **path**: The path pattern for routing (used in route predicates)
- **enabled**: Boolean flag to enable/disable the service route
- **connect-timeout**: Connection timeout in milliseconds
- **response-timeout**: Response timeout in milliseconds

## Use Cases

### 1. Disable Service During Maintenance

Temporarily disable a service without removing its configuration:

```yaml
services:
  user-service:
    uri: http://user-service:8080
    path: /v1/auth/**
    enabled: false # Service under maintenance
    connect-timeout: 5000
    response-timeout: 30000
```

**Result**: All requests to `/api/v1/auth/**` will return 404 Not Found.

### 2. Environment-Specific Services

Enable different services in different environments:

**Development (application-local.yml)**:

```yaml
services:
  user-service:
    enabled: true
  bookstore-service:
    enabled: true
  analytics-service:
    enabled: false # Not needed in development
```

**Production (application-production.yml)**:

```yaml
services:
  user-service:
    enabled: true
  bookstore-service:
    enabled: true
  analytics-service:
    enabled: true # Enabled in production
```

### 3. Feature Flags

Use service enabling as a feature flag mechanism:

```yaml
services:
  experimental-service:
    uri: http://experimental-service:8080
    path: /v1/experimental/**
    enabled: ${ENABLE_EXPERIMENTAL_FEATURES:false}
    connect-timeout: 5000
    response-timeout: 30000
```

### 4. Gradual Rollout

Enable services gradually across environments:

```yaml
# Staging - test new service
services:
  new-service:
    enabled: true

# Production - keep disabled until validated
services:
  new-service:
    enabled: false
```

## Current Service Routes

### User Service

**Enabled by default**: Yes

**Routes**:

- `/api/v1/auth/**` - Authentication endpoints
- `/api/v1/password/**` - Password management
- `/api/v1/users/**` - User management
- `/api/v1/organizations/**` - Organization management
- `/api/v1/admin/**` - Admin endpoints

**Configuration**:

```yaml
services:
  user-service:
    uri: http://localhost:8080
    path: /v1/auth/**
    enabled: true
    connect-timeout: 5000
    response-timeout: 30000
```

**Note**: Health check and actuator routes depend on user-service being enabled.

### Bookstore Service

**Enabled by default**: Yes

**Routes**:

- `/api/v1/bookstore/**` - Book catalog and inventory

**Configuration**:

```yaml
services:
  bookstore-service:
    uri: http://localhost:8080
    path: /v1/bookstore/**
    enabled: true
    connect-timeout: 5000
    response-timeout: 30000
```

## Runtime Behavior

### When Service is Enabled

1. Route is registered in Spring Cloud Gateway
2. Requests matching the path pattern are forwarded to the service
3. All configured filters are applied (transformation, rate limiting, etc.)
4. Service appears in gateway logs: `Enabling {service-name} routes: {uri}`

### When Service is Disabled

1. Route is NOT registered in Spring Cloud Gateway
2. Requests matching the path pattern return 404 Not Found
3. No filters are applied (request never reaches filter chain)
4. Service status logged: `{Service-name} is disabled or not configured`

### When Service is Not Configured

Same behavior as disabled service - route is not created.

## Logging

The gateway logs service status during startup:

**Enabled Service**:

```
INFO  o.g.g.config.GatewayConfig - Enabling user-service routes: http://localhost:8080
INFO  o.g.g.config.GatewayConfig - Enabling bookstore-service routes: http://localhost:8080
```

**Disabled Service**:

```
WARN  o.g.g.config.GatewayConfig - User service is disabled or not configured
INFO  o.g.g.config.GatewayConfig - Bookstore service is disabled or not configured
```

## Environment Variable Override

Override service enabled status using environment variables:

```bash
# Disable user service
GRIPDAY_GATEWAY_ROUTING_SERVICES_USER_SERVICE_ENABLED=false

# Disable bookstore service
GRIPDAY_GATEWAY_ROUTING_SERVICES_BOOKSTORE_SERVICE_ENABLED=false
```

## Adding New Services

To add a new service route:

1. **Add configuration** in `application.yml`:

```yaml
services:
  new-service:
    uri: http://new-service:8080
    path: /v1/newservice/**
    enabled: true
    connect-timeout: 5000
    response-timeout: 30000
```

2. **Update GatewayConfig.java** to register the route:

```java
var newServiceConfig = services.get("new-service");
if (newServiceConfig != null && newServiceConfig.enabled()) {
  logger.info("Enabling new-service routes: {}", newServiceConfig.uri());
  routesBuilder.route("new-service", r -> r
      .path("/api/v1/newservice/**")
      .filters(f -> f
          .filter(requestTransformationFilter.apply(new RequestTransformationFilter.Config()))
          .filter(responseTransformationFilter.apply(new ResponseTransformationFilter.Config()))
          .filter(loadBalancingFilter.apply(createLoadBalancingConfig("new-service")))
      )
      .uri(newServiceConfig.uri())
  );
} else {
  logger.info("New service is disabled or not configured");
}
```

3. **Add public paths** if needed (in `security.public-paths`):

```yaml
security:
  public-paths:
    - /api/v1/newservice/public/**
```

## Best Practices

1. **Default to Enabled**: Set `enabled: true` for production-ready services
2. **Document Reasons**: Comment why a service is disabled
3. **Environment Consistency**: Keep service names consistent across environments
4. **Graceful Degradation**: Design clients to handle 404 responses gracefully
5. **Monitor Logs**: Check startup logs to verify expected services are enabled
6. **Test Thoroughly**: Verify both enabled and disabled states work correctly

## Troubleshooting

### Issue: Service enabled but returns 404

**Possible Causes**:

1. Path pattern doesn't match request path
2. Service URI is incorrect
3. Route not registered (check startup logs)

**Solution**:

```bash
# Check gateway logs for route registration
grep "Enabling.*routes" logs/gripday-gateway-service.log

# Verify configuration
curl http://localhost:8080/actuator/gateway/routes
```

### Issue: Service disabled but still accessible

**Possible Causes**:

1. Configuration not reloaded
2. Multiple gateway instances with different configs
3. Cached routes

**Solution**:

1. Restart gateway service
2. Verify configuration file changes
3. Check environment variable overrides

### Issue: Cannot disable service

**Possible Causes**:

1. Environment variable override
2. Profile-specific configuration override
3. Configuration syntax error

**Solution**:

```bash
# Check effective configuration
curl http://localhost:8080/actuator/configprops | grep "user-service"

# Verify no environment variable override
echo $GRIPDAY_GATEWAY_ROUTING_SERVICES_USER_SERVICE_ENABLED
```

## Testing

### Test Enabled Service

```bash
# Should return 200 OK
curl -X GET http://localhost:8080/api/v1/auth/health
```

### Test Disabled Service

```bash
# Should return 404 Not Found
curl -X GET http://localhost:8080/api/v1/auth/health
```

### Verify Route Registration

```bash
# List all registered routes
curl http://localhost:8080/actuator/gateway/routes | jq '.[] | {id, uri, predicates}'
```

## Security Considerations

1. **Public Paths**: Disabled services still respect public path configuration
2. **Authentication**: Disabling a service doesn't bypass authentication
3. **Rate Limiting**: Disabled services don't consume rate limit quotas
4. **Audit Logging**: Service enable/disable events should be logged

## Performance Impact

- **Enabled Service**: Normal routing overhead
- **Disabled Service**: Zero overhead (route not registered)
- **Configuration Loading**: One-time cost at startup

## Related Configuration

- **API Prefix**: Works with enabled/disabled services
- **Rate Limiting**: Only applies to enabled services
- **Circuit Breaker**: Only applies to enabled services
- **Load Balancing**: Only applies to enabled services
