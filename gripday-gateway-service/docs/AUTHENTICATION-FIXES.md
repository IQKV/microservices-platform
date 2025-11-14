# Gateway Service Authentication Fixes

## Overview
Fixed three critical issues in the gateway service authentication and configuration implementation to ensure consistent use of GripdayProperties and eliminate duplicate JWT validation logic.

## Changes Made

### 1. SecurityConfiguration - Removed Duplicate JWT Validation

**Problem**: The SecurityConfiguration was using Spring Security's OAuth2 Resource Server with JWK validation, which conflicted with the custom JwtAuthenticationFilter that uses HMAC-based JWT validation.

**Solution**: 
- Removed the OAuth2 Resource Server configuration entirely
- Changed `.anyExchange().authenticated()` to `.anyExchange().permitAll()`
- Authentication is now exclusively handled by `JwtAuthenticationFilter` (order -100)
- SecurityConfiguration now only handles CORS and path-based authorization rules

**Rationale**: 
- The system uses HMAC (HS256) with shared secret keys, not RSA with JWK
- JwtAuthenticationFilter provides comprehensive authentication with user/tenant context extraction
- Single authentication mechanism eliminates confusion and potential conflicts

**Before**:
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwkSetUri("http://localhost:8080/.well-known/jwks.json"))
)
```

**After**:
```java
// JWT authentication handled by JwtAuthenticationFilter
.anyExchange().permitAll()
```

### 2. RequestTransformationFilter - Now Uses GripdayProperties

**Problem**: RequestTransformationFilter had hardcoded configuration defaults instead of reading from GripdayProperties.

**Solution**:
- Injected `GripdayProperties` into the filter constructor
- All configuration now read from `gripdayProperties.gateway().transformation().request()`
- Respects enabled/disabled flags for each transformation feature
- Uses configured headers to remove and additional headers from properties

**Configuration Respected**:
- `enabled` - Master switch for request transformation
- `enableHeaderEnrichment` - Controls service identification headers
- `enableUserContextPropagation` - Controls X-User-ID header
- `enableTenantContextPropagation` - Controls X-Tenant-ID header
- `headersToRemove` - List of headers to strip from requests
- `additionalHeaders` - Map of custom headers to add

**Before**:
```java
private boolean enableHeaderEnrichment = true;  // Hardcoded
private boolean enableUserContextPropagation = true;
private boolean enableTenantContextPropagation = true;
```

**After**:
```java
var transformationConfig = gripdayProperties.gateway().transformation().request();
if (!transformationConfig.enabled()) {
    return (exchange, chain) -> chain.filter(exchange);
}
// All settings read from transformationConfig
```

### 3. Documentation Updates

**Added**:
- Clarified that JWT authentication uses HMAC (HS256) with shared secrets
- Documented that JwtAuthenticationFilter is the single source of authentication
- Added comments explaining SecurityConfiguration's reduced scope

## Authentication Flow

The complete authentication flow is now:

1. **CorrelationIdFilter** (order -300): Generates/propagates correlation IDs
2. **TenantExtractionFilter** (order -200): Extracts tenant context from headers/subdomain
3. **JwtAuthenticationFilter** (order -100): Validates JWT, extracts user context, propagates headers
4. **TenantRateLimitingFilter** (order -50): Applies rate limits
5. **RequestTransformationFilter** (via route): Enriches headers based on GripdayProperties
6. **Route to downstream service**

## Configuration Properties Used

All authentication and transformation behavior is now controlled by:

```yaml
gripday:
  gateway:
    security:
      jwt:
        secret-key: ${JWT_SECRET_KEY}
        issuer: gripday
        audience: gripday-services
        algorithm: HS256
      authentication:
        enabled: true
        user-service-url: http://localhost:8080
        enable-user-context-propagation: true
      public-paths:
        - /api/v1/auth/login
        - /api/v1/auth/signup
        # ... etc
    
    transformation:
      request:
        enabled: true
        enable-header-enrichment: true
        enable-user-context-propagation: true
        enable-tenant-context-propagation: true
        headers-to-remove:
          - Authorization-Internal
          - X-Internal-Token
        additional-headers:
          X-Gateway-Version: "1.0.0"
```

## Headers Propagated to Downstream Services

When authentication succeeds, the following headers are added:

**From JwtAuthenticationFilter**:
- `X-Correlation-ID` - Request correlation ID
- `X-User-ID` - Authenticated user ID
- `X-Username` - Authenticated username
- `X-User-Roles` - Comma-separated list of user roles
- `X-Tenant-ID` - Tenant identifier (if present)

**From RequestTransformationFilter** (if enabled):
- `X-Gateway-Service` - "gripday-gateway"
- `X-Request-Source` - "gateway"
- `X-Request-Timestamp` - Request timestamp in milliseconds
- Any additional headers from `additionalHeaders` configuration

## Testing Recommendations

1. **Verify JWT validation works**: Test with valid/invalid tokens
2. **Verify public paths**: Ensure public endpoints don't require authentication
3. **Verify user context propagation**: Check downstream services receive X-User-ID, X-Username, X-User-Roles
4. **Verify tenant context**: Test tenant extraction from header, JWT, and subdomain
5. **Verify transformation flags**: Test with transformation features disabled
6. **Verify header removal**: Ensure sensitive headers are stripped

## Additional Fixes (Phase 2)

### 4. API Prefix Property Name Alignment

**Problem**: Inconsistent property naming between YAML configuration and Java record.
- Base `application.yml` used `strip-in-production: true` (boolean)
- Profile-specific configs used `strip-count: 0` (integer)
- Java record defined `stripCount` (integer)

**Solution**:
- Standardized on `strip-count` (kebab-case in YAML, maps to `stripCount` in Java)
- Removed the confusing `strip-in-production` boolean property
- Added validation: `@Min(0) @Max(5)` to prevent invalid strip counts
- Enhanced documentation with usage examples

**Configuration**:
```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true
        prefix: /api
        strip-count: 0  # 0 = no stripping, 1 = strip first segment, etc.
```

**Usage Examples**:
- **Development/Staging**: `prefix: /api`, `strip-count: 0` → URLs like `/api/v1/users` forwarded as-is
- **Production**: `prefix: ""`, `strip-count: 0` → No prefix (deployed on api.gripday.com)
- **Strip Mode**: `prefix: /api`, `strip-count: 1` → `/api/v1/users` forwarded as `/v1/users`

### 5. User Context Propagation Validation

**Problem**: The `enableUserContextPropagation` setting in GripdayProperties was not being respected by JwtAuthenticationFilter.

**Solution**:
- Added conditional check in `propagateContextHeaders()` method
- User context headers (X-User-ID, X-Username, X-User-Roles) only added when enabled
- Added debug logging to track when propagation is enabled/disabled
- Correlation ID and Tenant ID still propagated regardless (different concerns)

**Before**:
```java
// Always propagated user context headers
if (userContext.userId() != null) {
  builder.header(X_USER_ID_HEADER, userContext.userId().toString());
}
```

**After**:
```java
// Respects configuration setting
if (gripdayProperties.gateway().security().authentication().enableUserContextPropagation()) {
  if (userContext.userId() != null) {
    builder.header(X_USER_ID_HEADER, userContext.userId().toString());
  }
  // ... other user context headers
  logger.debug("User context propagated for user: {}", userContext.username());
} else {
  logger.debug("User context propagation is disabled");
}
```

**Use Cases**:
- **Enabled (default)**: Downstream services receive full user context for authorization
- **Disabled**: Privacy-focused scenarios where user identity shouldn't be propagated
- **Note**: JWT token is still validated; this only controls header propagation

## Migration Notes

### Configuration Changes Required

**Action Required**: Update `application.yml` base configuration:
- Replace `strip-in-production: true` with `strip-count: 0`

**Before**:
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-in-production: true
```

**After**:
```yaml
api-prefix:
  enabled: true
  prefix: /api
  strip-count: 0
```

### Behavior Changes

The system behavior remains largely the same, but now:
- Uses a single, consistent authentication mechanism (JwtAuthenticationFilter only)
- All configuration comes from GripdayProperties (no hardcoded values)
- User context propagation can be controlled via configuration
- API prefix configuration is consistent across all environments
