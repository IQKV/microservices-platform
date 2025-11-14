# API Prefix Configuration Guide

## Overview

The API prefix feature allows environment-specific URL routing strategies. This is useful when:
- Development/staging environments use `/api` prefix for clarity
- Production deploys on `api.gripday.com` subdomain (no prefix needed)
- You need to strip path segments before forwarding to downstream services

## Configuration Properties

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true          # Enable/disable API prefix handling
        prefix: /api           # The prefix to use (can be empty string)
        strip-count: 0         # Number of path segments to strip (0-5)
```

## Common Scenarios

### Scenario 1: Development/Staging with /api Prefix

**Use Case**: Keep `/api` prefix visible in URLs for clarity during development.

**Configuration**:
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-count: 0
```

**Behavior**:
- Client requests: `http://localhost:8080/api/v1/users`
- Forwarded to service: `http://user-service:8080/api/v1/users`
- No path modification

### Scenario 2: Production without Prefix

**Use Case**: Deploy on `api.gripday.com` subdomain, no `/api` prefix needed.

**Configuration**:
```yaml
api-prefix:
  enabled: true
  prefix: ""
  strip-count: 0
```

**Behavior**:
- Client requests: `https://api.gripday.com/v1/users`
- Forwarded to service: `http://user-service:8080/v1/users`
- No path modification

### Scenario 3: Strip Prefix Before Forwarding

**Use Case**: Accept `/api/v1/users` but forward as `/v1/users` to downstream services.

**Configuration**:
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-count: 1
```

**Behavior**:
- Client requests: `http://localhost:8080/api/v1/users`
- Forwarded to service: `http://user-service:8080/v1/users`
- First path segment (`/api`) stripped

### Scenario 4: Disabled API Prefix

**Use Case**: Bypass all API prefix handling.

**Configuration**:
```yaml
api-prefix:
  enabled: false
  prefix: /api
  strip-count: 0
```

**Behavior**:
- All requests forwarded as-is
- No prefix handling or stripping

## Strip Count Examples

The `strip-count` property removes path segments from the beginning of the path:

| Original Path | strip-count | Forwarded Path |
|--------------|-------------|----------------|
| `/api/v1/users` | 0 | `/api/v1/users` |
| `/api/v1/users` | 1 | `/v1/users` |
| `/api/v1/users` | 2 | `/users` |
| `/api/gateway/v1/users` | 2 | `/v1/users` |

## Environment-Specific Configuration

### Local Development (application-local.yml)
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-count: 0  # Keep full path for debugging
```

### Staging (application-staging.yml)
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-count: 0  # Keep full path for testing
```

### Production (application-production.yml)
```yaml
api-prefix:
  enabled: true
  prefix: ""      # No prefix on api.gripday.com
  strip-count: 0  # No stripping needed
```

## Integration with Spring Cloud Gateway

The API prefix configuration integrates with Spring Cloud Gateway's route definitions:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8080
          predicates:
            - Path=${gripday.gateway.routing.api-prefix.prefix}/v1/auth/**
          filters:
            - StripPrefix=${gripday.gateway.routing.api-prefix.strip-count}
```

## Validation Rules

- `enabled`: Boolean (true/false)
- `prefix`: String, must not be blank (can be empty string "")
- `strip-count`: Integer, range 0-5 (validated by `@Min(0) @Max(5)`)

## Programmatic Access

Access API prefix configuration in Java code:

```java
@Component
public class MyService {
  private final GripdayProperties gripdayProperties;
  
  public MyService(GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }
  
  public void example() {
    var apiPrefix = gripdayProperties.gateway().routing().apiPrefix();
    
    if (apiPrefix.enabled()) {
      String prefix = apiPrefix.prefix();
      int stripCount = apiPrefix.stripCount();
      // Use configuration...
    }
  }
}
```

## Troubleshooting

### Issue: Routes not matching

**Symptom**: 404 errors for valid endpoints

**Solution**: Ensure the `prefix` in route predicates matches your configuration:
```yaml
predicates:
  - Path=${gripday.gateway.routing.api-prefix.prefix}/v1/auth/**
```

### Issue: Path segments not stripped

**Symptom**: Downstream service receives unexpected path

**Solution**: Verify `strip-count` matches the number of segments to remove:
```yaml
filters:
  - StripPrefix=${gripday.gateway.routing.api-prefix.strip-count}
```

### Issue: Configuration not loading

**Symptom**: Default values used instead of configured values

**Solution**: 
1. Verify `@ConfigurationPropertiesScan` is present in main application class
2. Check YAML indentation (must be exact)
3. Ensure property names use kebab-case: `strip-count` not `stripCount`

## Best Practices

1. **Use consistent prefixes**: Stick with `/api` or no prefix across environments
2. **Minimize stripping**: Prefer `strip-count: 0` for simpler debugging
3. **Document changes**: Update API documentation when changing prefix strategy
4. **Test thoroughly**: Verify all routes work after configuration changes
5. **Environment variables**: Override in production using environment variables:
   ```bash
   GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=""
   GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
   ```
