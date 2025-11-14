# Spring Configuration Metadata

## Overview

The `spring-configuration-metadata.json` file provides IDE autocomplete and documentation for all GripdayProperties configuration options. This enables IntelliJ IDEA, VS Code, and other IDEs to offer intelligent code completion when editing `application.yml` files.

## Features

### IDE Autocomplete

When editing `application.yml`, your IDE will provide:

- Property name suggestions
- Type information
- Default values
- Descriptions
- Value hints for enums and common values

### Documented Properties

The metadata file documents all configuration properties including:

#### API Prefix Configuration

- `gripday.gateway.routing.api-prefix.enabled` - Enable/disable API prefix handling
- `gripday.gateway.routing.api-prefix.prefix` - API prefix string (/api, empty, etc.)
- `gripday.gateway.routing.api-prefix.strip-count` - Number of segments to strip (0-5)

#### Service Configuration

- `gripday.gateway.routing.services.*.enabled` - Enable/disable individual service routes
- `gripday.gateway.routing.services.*.uri` - Service base URI
- `gripday.gateway.routing.services.*.path` - Service path pattern
- `gripday.gateway.routing.services.*.connect-timeout` - Connection timeout (ms)
- `gripday.gateway.routing.services.*.response-timeout` - Response timeout (ms)

#### Security Configuration

- `gripday.gateway.security.jwt.secret-key` - JWT secret key for HMAC validation
- `gripday.gateway.security.jwt.algorithm` - JWT algorithm (HS256 or RS256)
- `gripday.gateway.security.jwt.issuer` - JWT issuer
- `gripday.gateway.security.jwt.audience` - JWT audience
- `gripday.gateway.security.authentication.enabled` - Enable authentication
- `gripday.gateway.security.authentication.user-service-url` - User service URL
- `gripday.gateway.security.authentication.enable-user-context-propagation` - Propagate user context headers

#### Transformation Configuration

- `gripday.gateway.transformation.request.enabled` - Enable request transformation
- `gripday.gateway.transformation.request.enable-header-enrichment` - Enable header enrichment
- `gripday.gateway.transformation.request.enable-user-context-propagation` - Enable user context propagation
- `gripday.gateway.transformation.request.enable-tenant-context-propagation` - Enable tenant context propagation

#### Feature Flags

- `gripday.gateway.rate-limiting.enabled` - Enable rate limiting
- `gripday.gateway.circuit-breaker.enabled` - Enable circuit breaker
- `gripday.gateway.cors.enabled` - Enable CORS

## Value Hints

The metadata provides intelligent value suggestions:

### API Prefix Strip Count

- `0` - No stripping - forward full path
- `1` - Strip one segment (e.g., /api)
- `2` - Strip two segments

### API Prefix Values

- `/api` - Standard API prefix for development/staging
- `` (empty) - No prefix for production (api.gripday.com)
- `/v1` - Version-specific prefix

### JWT Algorithm

- `HS256` - HMAC SHA-256 (symmetric, uses shared secret)
- `RS256` - RSA SHA-256 (asymmetric, uses public/private keys)

### Service Enabled Flag

- `true` - Enable service route (requests forwarded to service)
- `false` - Disable service route (requests return 404)

### User Context Propagation

- `true` - Propagate X-User-ID, X-Username, X-User-Roles headers
- `false` - Do not propagate user context headers

## IDE Support

### IntelliJ IDEA

1. **Autocomplete**: Press `Ctrl+Space` while editing YAML
2. **Documentation**: Hover over property names to see descriptions
3. **Validation**: Invalid values are highlighted
4. **Navigation**: `Ctrl+Click` on property names to jump to source

### VS Code

1. **Install**: Spring Boot Extension Pack
2. **Autocomplete**: Automatic when editing `application.yml`
3. **Validation**: Real-time validation of property values
4. **Hover**: Hover over properties for documentation

### Eclipse

1. **Install**: Spring Tools 4
2. **Autocomplete**: `Ctrl+Space` in YAML files
3. **Validation**: Automatic validation
4. **Quick Fix**: Suggestions for invalid values

## Updating Metadata

When adding new properties to `GripdayProperties`:

1. **Add Property Definition** in `GripdayProperties.java`:

```java
public record NewProperties(@NotBlank String newProperty) {}
```

2. **Update Metadata** in `spring-configuration-metadata.json`:

```json
{
  "name": "gripday.gateway.new-property",
  "type": "java.lang.String",
  "description": "Description of new property",
  "defaultValue": "default-value"
}
```

3. **Add Hints** (optional) for enum-like values:

```json
{
  "name": "gripday.gateway.new-property",
  "values": [
    { "value": "option1", "description": "First option" },
    { "value": "option2", "description": "Second option" }
  ]
}
```

4. **Rebuild Project**: Maven will copy metadata to target/classes

## Validation

The metadata file is validated during build:

```bash
# Validate JSON syntax
mvn clean compile

# Check metadata is copied to target
ls target/classes/META-INF/spring-configuration-metadata.json
```

## Benefits

1. **Developer Experience**: Faster configuration editing with autocomplete
2. **Documentation**: Inline documentation in IDE
3. **Type Safety**: IDE validates property types
4. **Discoverability**: Developers can explore available properties
5. **Consistency**: Standardized property naming and values

## Best Practices

1. **Keep Updated**: Update metadata when adding/changing properties
2. **Add Descriptions**: Provide clear, concise descriptions
3. **Include Defaults**: Document default values
4. **Add Hints**: Provide value suggestions for common properties
5. **Validate JSON**: Ensure JSON is valid before committing

## Troubleshooting

### Autocomplete Not Working

**Cause**: Metadata not in classpath or IDE not recognizing it

**Solution**:

1. Rebuild project: `mvn clean compile`
2. Refresh IDE project
3. Verify file exists: `target/classes/META-INF/spring-configuration-metadata.json`
4. Restart IDE

### Invalid Property Warnings

**Cause**: Property not in metadata or typo in property name

**Solution**:

1. Check property name spelling
2. Verify property exists in `GripdayProperties.java`
3. Update metadata if property is new
4. Rebuild project

### Missing Descriptions

**Cause**: Metadata not updated after adding properties

**Solution**:

1. Add property to `spring-configuration-metadata.json`
2. Rebuild project
3. Refresh IDE

## Related Files

- `GripdayProperties.java` - Source of truth for all properties
- `application.yml` - Base configuration file
- `application-{profile}.yml` - Environment-specific configurations

## References

- [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
