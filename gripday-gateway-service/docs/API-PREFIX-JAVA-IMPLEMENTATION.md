# API Prefix Java Implementation

## Overview

This document describes the Java implementation for the API prefix configuration feature.

## Components Created

### 1. Configuration Properties

#### `GatewayProperties.Routing.ApiPrefix`

Location: `org.gripday.gatewayservice.config.GatewayProperties`

```java
public record ApiPrefix(@NotNull Boolean enabled, @NotBlank String prefix, int stripCount) {}
```

**Properties:**

- `enabled`: Enable/disable API prefix handling
- `prefix`: The prefix string (e.g., "/api" or empty)
- `stripCount`: Number of path segments to strip from incoming requests

**Validation:**

- `enabled` must not be null
- `prefix` must not be blank
- `stripCount` is an integer (can be 0 or positive)

### 2. Service Layer

#### `ApiPrefixService`

Location: `org.gripday.gatewayservice.service.ApiPrefixService`

A service providing utilities for API prefix operations and path transformations.

**Methods:**

```java
// Get the configured API prefix
public String getPrefix()

// Get the strip count
public int getStripCount()

// Check if prefix handling is enabled
public boolean isEnabled()

// Build full path by combining prefix with service path
public String buildFullPath(String servicePath)

// Strip prefix from a path
public String stripPrefix(String path)

// Check if path has the configured prefix
public boolean hasPrefix(String path)

// Normalize path with leading slash
public String normalizePath(String path)

// Get configuration details as formatted string
public String getConfigurationDetails()
```

**Usage Examples:**

```java
@Service
public class MyService {

  private final ApiPrefixService apiPrefixService;

  public MyService(ApiPrefixService apiPrefixService) {
    this.apiPrefixService = apiPrefixService;
  }

  public void example() {
    // Build full path
    var fullPath = apiPrefixService.buildFullPath("/v1/auth/login");
    // Result: "/api/v1/auth/login" (in local/staging)
    // Result: "/v1/auth/login" (in production)

    // Check if path has prefix
    var hasPrefix = apiPrefixService.hasPrefix("/api/v1/auth/login");
    // Result: true (in local/staging)
    // Result: false (in production)

    // Strip prefix
    var stripped = apiPrefixService.stripPrefix("/api/v1/auth/login");
    // Result depends on stripCount configuration
  }
}
```

### 3. Configuration Logger

#### `ApiPrefixConfigurationLogger`

Location: `org.gripday.gatewayservice.config.ApiPrefixConfigurationLogger`

Logs API prefix configuration on application startup for verification.

**Logged Information:**

- Enabled status
- Configured prefix
- Strip count
- Example route transformations
- Configured service routes with full paths

**Sample Output:**

```
================================================================================
API Prefix Configuration
================================================================================
Enabled: true
Prefix: '/api'
Strip Count: 0
--------------------------------------------------------------------------------
Example Route Configuration:
  Service Path: /v1/auth/login
  Full Path: /api/v1/auth/login
  Backend Receives: /api/v1/auth/login (no stripping)
--------------------------------------------------------------------------------
Configured Service Routes:
  Auth Service:
    URI: http://localhost:8080
    Path Pattern: /api/v1/auth/**
    Connect Timeout: 10000ms
    Response Timeout: 60000ms
================================================================================
```

## Testing

### Unit Tests

#### `ApiPrefixServiceTest`

Location: `org.gripday.gatewayservice.service.ApiPrefixServiceTest`

Comprehensive test coverage for `ApiPrefixService`:

**Test Cases:**

- ✓ Should return configured prefix
- ✓ Should return strip count
- ✓ Should return enabled status
- ✓ Should build full path with prefix
- ✓ Should return service path when prefix is empty
- ✓ Should return service path when disabled
- ✓ Should strip prefix from path
- ✓ Should not strip when strip count is zero
- ✓ Should check if path has prefix
- ✓ Should return false when path does not have prefix
- ✓ Should normalize path with leading slash
- ✓ Should keep normalized path unchanged
- ✓ Should return root for empty path
- ✓ Should return configuration details

**Running Tests:**

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApiPrefixServiceTest

# Run with coverage
mvn test jacoco:report
```

## Integration with Existing Code

### No Breaking Changes

The implementation is designed to be non-breaking:

1. **Existing services continue to work** - No changes required to existing filters or services
2. **Configuration is additive** - New `apiPrefix` property added to `Routing` record
3. **Backward compatible** - Default values ensure existing behavior is preserved

### Optional Integration Points

Services can optionally inject `ApiPrefixService` for prefix-aware operations:

```java
@Component
public class CustomFilter implements GatewayFilter {

  private final ApiPrefixService apiPrefixService;

  public CustomFilter(ApiPrefixService apiPrefixService) {
    this.apiPrefixService = apiPrefixService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var path = exchange.getRequest().getPath().value();

    // Check if path has API prefix
    if (apiPrefixService.hasPrefix(path)) {
      // Handle prefixed path
    }

    return chain.filter(exchange);
  }
}
```

## Configuration Examples

### Local Development

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
        prefix: /api
        strip-count: 0
```

### Production

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
        prefix: ""
        strip-count: 0
```

### Environment Variables

```bash
# Override via environment variables
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_ENABLED=true
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=/api
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
```

## Best Practices

1. **Use ApiPrefixService** for all prefix-related operations
2. **Log configuration on startup** to verify correct settings
3. **Test both prefixed and non-prefixed paths** in your integration tests
4. **Document environment-specific URLs** in your API documentation
5. **Keep stripCount at 0** unless you need to strip segments for backend compatibility

## Troubleshooting

### Issue: Routes not matching

**Solution:** Check that route predicates use the dynamic prefix:

```yaml
predicates:
  - Path=${gripday.gateway.routing.api-prefix.prefix}${service.path}
```

### Issue: Backend receives wrong path

**Solution:** Verify `stripCount` configuration matches your backend expectations

### Issue: Public paths not working

**Solution:** Ensure public paths include both prefixed and non-prefixed variants

## Future Enhancements

Potential improvements for future versions:

1. **Dynamic prefix switching** - Change prefix at runtime without restart
2. **Path rewriting rules** - More complex path transformation logic
3. **Prefix per service** - Different prefixes for different backend services
4. **Metrics** - Track requests by prefix for monitoring
5. **Validation** - Ensure prefix consistency across configuration
