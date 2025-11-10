# API Prefix Configuration Metadata

## Overview

This document describes the Spring configuration metadata additions for the API prefix feature. The metadata provides IDE autocomplete, validation, and documentation for configuration properties.

## Metadata Location

File: `src/main/resources/META-INF/spring-configuration-metadata.json`

## Added Groups

### `gripday.gateway.routing.api-prefix`

**Type:** `org.gripday.gatewayservice.config.GatewayProperties$Routing$ApiPrefix`

**Description:** API prefix configuration for environment-specific routing. Allows configuring /api prefix in development/staging and clean URLs in production (e.g., api.gripday.com)

## Added Properties

### 1. `gripday.gateway.routing.api-prefix.enabled`

| Attribute       | Value                                                                   |
| --------------- | ----------------------------------------------------------------------- |
| **Type**        | `java.lang.Boolean`                                                     |
| **Default**     | `true`                                                                  |
| **Description** | Enable API prefix handling for environment-specific routing             |
| **Source Type** | `org.gripday.gatewayservice.config.GatewayProperties$Routing$ApiPrefix` |

**Usage:**

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
```

**Environment Variable:**

```bash
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_ENABLED=true
```

---

### 2. `gripday.gateway.routing.api-prefix.prefix`

| Attribute       | Value                                                                                                                                           |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **Type**        | `java.lang.String`                                                                                                                              |
| **Default**     | `/api`                                                                                                                                          |
| **Description** | API prefix to prepend to all routes. Use '/api' for development/staging, empty string for production when deployed on api.gripday.com subdomain |
| **Source Type** | `org.gripday.gatewayservice.config.GatewayProperties$Routing$ApiPrefix`                                                                         |

**Suggested Values:**

- `/api` - Standard API prefix for development and staging environments
- `` (empty) - No prefix for production when deployed on api.gripday.com subdomain
- `/v1` - Version-specific prefix (alternative approach)

**Usage:**

```yaml
# Development/Staging
gripday:
  gateway:
    routing:
      api-prefix:
        prefix: /api

# Production
gripday:
  gateway:
    routing:
      api-prefix:
        prefix: ""
```

**Environment Variable:**

```bash
# Development/Staging
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=/api

# Production
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=
```

---

### 3. `gripday.gateway.routing.api-prefix.strip-count`

| Attribute       | Value                                                                                                                                     |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| **Type**        | `java.lang.Integer`                                                                                                                       |
| **Default**     | `0`                                                                                                                                       |
| **Description** | Number of path segments to strip from incoming requests before forwarding to backend services. Usually 0 (no stripping) or 1 (strip /api) |
| **Source Type** | `org.gripday.gatewayservice.config.GatewayProperties$Routing$ApiPrefix`                                                                   |

**Suggested Values:**

- `0` - No stripping - forward full path to backend (recommended)
- `1` - Strip one segment (e.g., /api) before forwarding to backend
- `2` - Strip two segments (e.g., /api/v1) before forwarding to backend

**Usage:**

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        strip-count: 0
```

**Environment Variable:**

```bash
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
```

## IDE Support

### IntelliJ IDEA

With this metadata, IntelliJ IDEA provides:

1. **Autocomplete** - Suggests property names as you type
2. **Documentation** - Shows descriptions in tooltips
3. **Validation** - Warns about invalid values
4. **Navigation** - Jump to source type with Ctrl+Click

### VS Code

With Spring Boot extension, VS Code provides:

1. **IntelliSense** - Property name suggestions
2. **Hover Documentation** - Property descriptions
3. **Value Suggestions** - Suggested values from hints
4. **Type Validation** - Type checking for property values

## Example Configurations

### Local Development (application-local.yml)

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true # IDE shows: "Enable API prefix handling..."
        prefix: /api # IDE suggests: "/api", "", "/v1"
        strip-count: 0 # IDE suggests: 0, 1, 2
```

### Production (application-production.yml)

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
        prefix: "" # Empty for api.gripday.com
        strip-count: 0
```

## Validation

The metadata works with Spring Boot's validation annotations:

```java
public record ApiPrefix(
  @NotNull Boolean enabled, // Must not be null
  @NotBlank String prefix, // Must not be blank
  int stripCount // Integer (can be 0 or positive)
) {}
```

## Testing Metadata

### Verify Metadata Loading

```bash
# Build the project
mvn clean compile

# Check generated metadata
cat target/classes/META-INF/spring-configuration-metadata.json
```

### Test IDE Autocomplete

1. Open `application.yml` in your IDE
2. Type `gripday.gateway.routing.api-`
3. IDE should suggest `api-prefix` with description
4. Continue typing to see property suggestions

## Metadata Best Practices

1. **Clear Descriptions** - Explain what the property does and when to use it
2. **Provide Hints** - Suggest common values with descriptions
3. **Include Defaults** - Document default values
4. **Reference Documentation** - Link to detailed docs when needed
5. **Keep Updated** - Update metadata when properties change

## Related Files

- Configuration: `src/main/resources/application.yml`
- Java Source: `src/main/java/org/gripday/gatewayservice/config/GatewayProperties.java`
- Documentation: `docs/API-PREFIX-CONFIGURATION.md`
- Quick Reference: `docs/API-PREFIX-QUICK-REFERENCE.md`

## Troubleshooting

### Metadata Not Working in IDE

1. **Rebuild Project** - `mvn clean compile`
2. **Invalidate Caches** - In IntelliJ: File → Invalidate Caches / Restart
3. **Check Extension** - Ensure Spring Boot extension is installed (VS Code)
4. **Verify JSON** - Validate JSON syntax in metadata file

### Property Not Showing in Autocomplete

1. **Check Group Definition** - Ensure group is defined in `groups` array
2. **Verify Source Type** - Ensure source type matches Java class
3. **Check Property Name** - Ensure property name matches exactly
4. **Rebuild** - Clean and rebuild project

## Additional Resources

- [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
- [IDE Support](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html#appendix.configuration-metadata.annotation-processor)
