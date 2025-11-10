# API Prefix Quick Reference

## Configuration Properties

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true # Enable/disable prefix handling
        prefix: /api # Prefix string (use "" for no prefix)
        strip-count: 0 # Number of segments to strip (usually 0)
```

## Java API

### Inject the Service

```java
@Service
public class MyService {

  private final ApiPrefixService apiPrefixService;

  public MyService(ApiPrefixService apiPrefixService) {
    this.apiPrefixService = apiPrefixService;
  }
}
```

### Common Operations

```java
// Get prefix
String prefix = apiPrefixService.getPrefix();

// Check if enabled
boolean enabled = apiPrefixService.isEnabled();

// Build full path
String fullPath = apiPrefixService.buildFullPath("/v1/auth/login");

// Local: "/api/v1/auth/login"
// Production: "/v1/auth/login"

// Check if path has prefix
boolean hasPrefix = apiPrefixService.hasPrefix("/api/v1/auth/login");

// Strip prefix (if configured)
String stripped = apiPrefixService.stripPrefix("/api/v1/auth/login");

// Normalize path
String normalized = apiPrefixService.normalizePath("v1/auth/login");
// Result: "/v1/auth/login"
```

## Environment Configuration

### Local (.env.local)

```bash
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=/api
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
```

### Production (.env.production)

```bash
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
```

## URL Examples

| Environment | Gateway URL                   | Full Request URL                                |
| ----------- | ----------------------------- | ----------------------------------------------- |
| Local       | `http://localhost:8080`       | `http://localhost:8080/api/v1/auth/login`       |
| Staging     | `https://staging.gripday.com` | `https://staging.gripday.com/api/v1/auth/login` |
| Production  | `https://api.gripday.com`     | `https://api.gripday.com/v1/auth/login`         |

## Testing

```bash
# Run tests
mvn test -Dtest=ApiPrefixServiceTest

# Local testing with prefix
curl http://localhost:8080/api/v1/auth/login

# Production simulation (no prefix)
export GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=""
curl http://localhost:8080/v1/auth/login
```

## Troubleshooting

| Issue                     | Solution                                                                   |
| ------------------------- | -------------------------------------------------------------------------- |
| Routes not matching       | Verify route predicates use `${gripday.gateway.routing.api-prefix.prefix}` |
| Wrong path to backend     | Check `strip-count` configuration                                          |
| Public paths not working  | Include both prefixed and non-prefixed variants                            |
| Configuration not applied | Check Spring profile is active                                             |

## Files Modified/Created

### Configuration

- `application.yml` - Base configuration with prefix properties
- `application-local.yml` - Local environment (prefix: `/api`)
- `application-staging.yml` - Staging environment (prefix: `/api`)
- `application-production.yml` - Production environment (prefix: `""`)

### Java Classes

- `GatewayProperties.java` - Added `ApiPrefix` record
- `ApiPrefixService.java` - Service for prefix operations
- `ApiPrefixConfigurationLogger.java` - Startup configuration logger
- `ApiPrefixServiceTest.java` - Unit tests

### Documentation

- `API-PREFIX-CONFIGURATION.md` - Comprehensive guide
- `API-PREFIX-JAVA-IMPLEMENTATION.md` - Java implementation details
- `API-PREFIX-QUICK-REFERENCE.md` - This quick reference
- `CHANGELOG-API-PREFIX.md` - Implementation summary
