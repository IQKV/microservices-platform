# API Prefix IDE Integration Guide

## Overview

This guide demonstrates how the Spring configuration metadata enhances the development experience in modern IDEs.

## IDE Features Enabled

### 1. Property Autocomplete

When typing in `application.yml`:

```yaml
gripday:
  gateway:
    routing: api- # ← IDE suggests: api-prefix
```

**What You See:**

```
api-prefix
  API prefix configuration for environment-specific routing.
  Allows configuring /api prefix in development/staging and
  clean URLs in production (e.g., api.gripday.com)
```

### 2. Property Value Suggestions

When setting the prefix value:

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        prefix: # ← IDE suggests values
```

**Suggestions:**

```
/api
  Standard API prefix for development and staging environments

(empty string)
  No prefix for production when deployed on api.gripday.com subdomain

/v1
  Version-specific prefix (alternative approach)
```

### 3. Inline Documentation

Hover over any property to see documentation:

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        strip-count: 0 # ← Hover shows description
```

**Tooltip:**

```
gripday.gateway.routing.api-prefix.strip-count

Type: java.lang.Integer
Default: 0

Number of path segments to strip from incoming requests
before forwarding to backend services. Usually 0 (no
stripping) or 1 (strip /api)

Source: org.gripday.gatewayservice.config.GatewayProperties$Routing$ApiPrefix
```

### 4. Type Validation

IDE validates property types:

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: "yes" # ⚠️ Warning: Expected Boolean, got String
        strip-count: "one" # ⚠️ Warning: Expected Integer, got String
```

### 5. Navigation to Source

**Ctrl+Click** (or Cmd+Click on Mac) on property name jumps to:

```java
// GatewayProperties.java
public record ApiPrefix(@NotNull Boolean enabled, @NotBlank String prefix, int stripCount) {}
```

## IntelliJ IDEA Setup

### Prerequisites

1. IntelliJ IDEA 2023.1+ (Community or Ultimate)
2. Spring Boot plugin (usually pre-installed)
3. Maven or Gradle integration

### Enable Spring Support

1. **File → Project Structure → Facets**
2. Add **Spring** facet if not present
3. Ensure **Spring Boot** is detected

### Verify Metadata Loading

1. Build project: **Build → Build Project**
2. Check: **target/classes/META-INF/spring-configuration-metadata.json**
3. Open any `application.yml` file
4. Start typing `gripday.` - should see suggestions

### Troubleshooting

**No Autocomplete?**

- File → Invalidate Caches / Restart
- Rebuild project: `mvn clean compile`
- Check Spring Boot plugin is enabled

**Wrong Suggestions?**

- Ensure metadata file is in `src/main/resources/META-INF/`
- Verify JSON syntax is valid
- Check source type matches Java class

## VS Code Setup

### Prerequisites

1. VS Code 1.80+
2. Extension: **Spring Boot Extension Pack**
3. Extension: **YAML** by Red Hat

### Install Extensions

```bash
# Install Spring Boot Extension Pack
code --install-extension vmware.vscode-boot-dev-pack

# Install YAML support
code --install-extension redhat.vscode-yaml
```

### Enable Spring Boot Support

1. Open Command Palette: **Ctrl+Shift+P** (Cmd+Shift+P on Mac)
2. Type: **Spring Boot: Enable/Disable**
3. Ensure Spring Boot support is enabled

### Verify Metadata Loading

1. Build project: `mvn clean compile`
2. Open `application.yml`
3. Type `gripday.gateway.routing.api-` - should see suggestions
4. Hover over property - should see documentation

### Troubleshooting

**No IntelliSense?**

- Reload window: **Ctrl+Shift+P** → **Developer: Reload Window**
- Check extension is active: **Extensions** view
- Verify `spring-configuration-metadata.json` exists in target

**No Hover Documentation?**

- Ensure YAML extension is installed
- Check file is recognized as Spring Boot config
- Rebuild project

## Eclipse Setup

### Prerequisites

1. Eclipse 2023-06+
2. Spring Tools 4 (STS4) plugin

### Install Spring Tools

1. **Help → Eclipse Marketplace**
2. Search: **Spring Tools 4**
3. Install and restart Eclipse

### Enable Content Assist

1. **Window → Preferences → Spring → Boot**
2. Enable **Content Assist for application properties**
3. Set **Metadata scan depth** to appropriate level

### Verify Metadata Loading

1. Build project: Right-click project → **Maven → Update Project**
2. Open `application.yml`
3. Press **Ctrl+Space** - should see suggestions

## Configuration Examples with IDE Support

### Example 1: Local Development

```yaml
# Type 'gripday.' and IDE suggests 'gateway'
gripday:
  # Type 'gateway.' and IDE suggests 'routing'
  gateway:
    # Type 'routing.' and IDE suggests 'api-prefix'
    routing:
      # IDE shows: "API prefix configuration for..."
      api-prefix:
        # IDE suggests: true, false
        enabled: true
        # IDE suggests: "/api", "", "/v1"
        prefix: /api
        # IDE suggests: 0, 1, 2
        strip-count: 0
```

### Example 2: Production Configuration

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
        # Select empty string from suggestions
        prefix: "" # IDE shows: "No prefix for production..."
        strip-count: 0
```

### Example 3: Environment Variables

IDE can also help with environment variable names:

```properties
# .env file
# IDE suggests: GRIPDAY_GATEWAY_ROUTING_API_PREFIX_ENABLED
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_ENABLED=true
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_PREFIX=/api
GRIPDAY_GATEWAY_ROUTING_API_PREFIX_STRIP_COUNT=0
```

## Advanced IDE Features

### 1. Refactoring Support

When renaming properties in Java:

```java
// Before
public record ApiPrefix(String prefix) {}

// After (rename to 'pathPrefix')
public record ApiPrefix(String pathPrefix) {}
```

IDE can update:

- Configuration files
- Metadata JSON
- Documentation references

### 2. Find Usages

Right-click on property in Java → **Find Usages**

Shows all places where property is used:

- Configuration files (YAML, properties)
- Java code accessing the property
- Documentation references

### 3. Quick Documentation

**Ctrl+Q** (IntelliJ) or **Hover** (VS Code) on property shows:

- Property description
- Type information
- Default value
- Source class
- Suggested values

### 4. Code Completion in Java

When accessing properties in code:

```java
@Service
public class MyService {
  private final GatewayProperties props;

  public void example() {
    var prefix = props.routing().apiPrefix().  // ← IDE suggests methods
    //                                          prefix()
    //                                          enabled()
    //                                          stripCount()
  }
}
```

## Best Practices

### 1. Keep Metadata Updated

When adding/modifying properties:

1. Update Java record/class
2. Update `spring-configuration-metadata.json`
3. Rebuild project
4. Test in IDE

### 2. Provide Clear Descriptions

```json
{
  "name": "gripday.gateway.routing.api-prefix.prefix",
  "description": "API prefix to prepend to all routes. Use '/api' for development/staging, empty string for production when deployed on api.gripday.com subdomain"
}
```

### 3. Add Value Hints

```json
{
  "name": "gripday.gateway.routing.api-prefix.prefix",
  "values": [
    {
      "value": "/api",
      "description": "Standard API prefix for development and staging"
    }
  ]
}
```

### 4. Document Defaults

```json
{
  "name": "gripday.gateway.routing.api-prefix.enabled",
  "defaultValue": true
}
```

### 5. Test IDE Integration

After metadata changes:

1. Rebuild project
2. Test autocomplete in YAML
3. Verify hover documentation
4. Check value suggestions
5. Test navigation to source

## Keyboard Shortcuts

### IntelliJ IDEA

| Action              | Shortcut             |
| ------------------- | -------------------- |
| Autocomplete        | Ctrl+Space           |
| Quick Documentation | Ctrl+Q               |
| Navigate to Source  | Ctrl+B or Ctrl+Click |
| Find Usages         | Alt+F7               |
| Refactor/Rename     | Shift+F6             |

### VS Code

| Action              | Shortcut          |
| ------------------- | ----------------- |
| IntelliSense        | Ctrl+Space        |
| Hover Documentation | Hover mouse       |
| Go to Definition    | F12 or Ctrl+Click |
| Find All References | Shift+F12         |
| Rename Symbol       | F2                |

### Eclipse

| Action           | Shortcut     |
| ---------------- | ------------ |
| Content Assist   | Ctrl+Space   |
| Quick Info       | F2           |
| Open Declaration | F3           |
| Find References  | Ctrl+Shift+G |
| Rename           | Alt+Shift+R  |

## Resources

- [IntelliJ IDEA Spring Boot Support](https://www.jetbrains.com/help/idea/spring-boot.html)
- [VS Code Spring Boot Extension](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)
- [Eclipse Spring Tools](https://spring.io/tools)
- [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
