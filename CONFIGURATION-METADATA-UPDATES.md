# Spring Configuration Metadata Updates

## Overview

This document summarizes the updates made to Spring configuration metadata files to support the new RSA256-based authentication architecture with JWK endpoint.

---

## Gateway Service Metadata Updates

### File Location

`gripday-gateway-service/src/main/resources/META-INF/spring-configuration-metadata.json`

### Changes Made

#### 1. New Property: `gripday.gateway.security.jwt.jwk-set-uri`

**Added:**

```json
{
  "name": "gripday.gateway.security.jwt.jwk-set-uri",
  "type": "java.lang.String",
  "description": "JWK Set URI for fetching public keys (required for RS256)",
  "defaultValue": "http://localhost:8080/.well-known/jwks.json"
}
```

**Purpose:** Specifies the URL where the Gateway Service fetches public keys from the User Service for JWT validation.

**Hints Added:**

```json
{
  "name": "gripday.gateway.security.jwt.jwk-set-uri",
  "values": [
    {
      "value": "http://localhost:8080/.well-known/jwks.json",
      "description": "Local User Service JWK endpoint"
    },
    {
      "value": "http://user-service:8080/.well-known/jwks.json",
      "description": "Docker/Kubernetes User Service JWK endpoint"
    },
    {
      "value": "https://api.gripday.com/.well-known/jwks.json",
      "description": "Production User Service JWK endpoint"
    }
  ]
}
```

#### 2. Updated Property: `gripday.gateway.security.jwt.algorithm`

**Before:**

```json
{
  "name": "gripday.gateway.security.jwt.algorithm",
  "type": "java.lang.String",
  "description": "JWT algorithm (HS256 or RS256)",
  "defaultValue": "HS256"
}
```

**After:**

```json
{
  "name": "gripday.gateway.security.jwt.algorithm",
  "type": "java.lang.String",
  "description": "JWT algorithm (HS256 or RS256)",
  "defaultValue": "RS256"
}
```

**Change:** Default algorithm changed from `HS256` to `RS256` to reflect the new architecture.

**Hints Updated:**

```json
{
  "name": "gripday.gateway.security.jwt.algorithm",
  "values": [
    {
      "value": "RS256",
      "description": "RSA SHA-256 (asymmetric) - Recommended"
    },
    {
      "value": "HS256",
      "description": "HMAC SHA-256 (symmetric) - Deprecated"
    }
  ]
}
```

#### 3. Removed Property: `gripday.gateway.security.jwt.secret-key`

**Status:** REMOVED

**Reason:** No longer needed with RSA256. The Gateway now fetches public keys from the JWK endpoint instead of using a shared secret.

**Migration:** Remove `secret-key` from all configuration files and use `jwk-set-uri` instead.

#### 4. Updated Property: `gripday.gateway.security.jwt.issuer`

**Before:**

```json
{
  "name": "gripday.gateway.security.jwt.issuer",
  "type": "java.lang.String",
  "description": "JWT issuer",
  "defaultValue": "gripday"
}
```

**After:**

```json
{
  "name": "gripday.gateway.security.jwt.issuer",
  "type": "java.lang.String",
  "description": "JWT issuer (must match User Service issuer)",
  "defaultValue": "gripday-user-service"
}
```

**Changes:**

- Default value changed from `gripday` to `gripday-user-service`
- Description clarified that it must match User Service issuer

#### 5. Updated Property: `gripday.gateway.security.jwt.audience`

**Before:**

```json
{
  "name": "gripday.gateway.security.jwt.audience",
  "type": "java.lang.String",
  "description": "JWT audience",
  "defaultValue": "gripday-services"
}
```

**After:**

```json
{
  "name": "gripday.gateway.security.jwt.audience",
  "type": "java.lang.String",
  "description": "JWT audience (optional for RS256)"
}
```

**Change:** Removed default value and clarified it's optional for RS256.

#### 6. New Properties: Token Expiry Configuration

**Added:**

```json
{
  "name": "gripday.gateway.security.jwt.access-token-expiry",
  "type": "java.time.Duration",
  "description": "Access token expiration duration",
  "defaultValue": "PT15M"
},
{
  "name": "gripday.gateway.security.jwt.refresh-token-expiry",
  "type": "java.lang.String",
  "description": "Refresh token expiration duration",
  "defaultValue": "P7D"
}
```

**Purpose:** Document the expected token expiry durations for reference.

---

## User Service Metadata

### File Location

`gripday-user-service/src/main/resources/META-INF/spring-configuration-metadata.json`

### Status

**No changes required.**

**Reason:** The User Service JWT configuration is managed programmatically by `JwtKeyManagementService` and `JwtConfiguration` classes, which don't expose configuration properties. The key rotation and token cleanup are automatic and don't require user configuration.

---

## IDE Integration Benefits

### IntelliJ IDEA / Eclipse / VS Code

With these metadata updates, IDEs will provide:

1. **Autocomplete** for new properties:
   - `gripday.gateway.security.jwt.jwk-set-uri`
   - Token expiry properties

2. **Validation** warnings:
   - Deprecated `secret-key` property usage
   - Missing required `jwk-set-uri` when using RS256

3. **Documentation** tooltips:
   - Hover over properties to see descriptions
   - View available values and their purposes

4. **Quick Fixes**:
   - Suggestions to migrate from HS256 to RS256
   - Hints for common JWK endpoint URLs

### Example IDE Experience

**Before (HS256):**

```yaml
gripday:
  gateway:
    security:
      jwt:
        secret-key: ${JWT_SECRET_KEY}
        algorithm: HS256
```

**After (RS256):**

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        jwk-set-uri: http://localhost:8080/.well-known/jwks.json # ✓ Autocomplete available
        # secret-key removed - no longer needed
```

---

## Configuration Examples

### Development Environment

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: http://localhost:8080/.well-known/jwks.json
        access-token-expiry: PT15M
        refresh-token-expiry: P7D
```

### Docker Compose Environment

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
        access-token-expiry: PT15M
        refresh-token-expiry: P7D
```

### Kubernetes Environment

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: http://user-service.default.svc.cluster.local:8080/.well-known/jwks.json
        access-token-expiry: PT15M
        refresh-token-expiry: P7D
```

### Production Environment

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: https://api.gripday.com/.well-known/jwks.json
        access-token-expiry: PT15M
        refresh-token-expiry: P7D
```

---

## Migration Guide for Configuration

### Step 1: Update Gateway Configuration

**Old Configuration (HMAC):**

```yaml
gripday:
  gateway:
    security:
      jwt:
        secret-key: ${JWT_SECRET_KEY}
        algorithm: HS256
        issuer: gripday
        audience: gripday-services
```

**New Configuration (RSA):**

```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: ${USER_SERVICE_JWK_URI:http://localhost:8080/.well-known/jwks.json}
        # secret-key property has been removed
        # audience is optional
```

### Step 2: Update Environment Variables

**Remove:**

```bash
JWT_SECRET_KEY=your-secret-key  # No longer used
```

**Add:**

```bash
USER_SERVICE_JWK_URI=http://user-service:8080/.well-known/jwks.json
```

### Step 4: Verify Configuration

```bash
# Check if JWK endpoint is accessible
curl http://user-service:8080/.well-known/jwks.json

# Test authentication flow
curl -X POST http://gateway:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}'
```

---

## Validation

### Maven Validation

The metadata files are validated during Maven build:

```bash
# Validate Gateway Service metadata
cd gripday-gateway-service
mvn validate

# Validate User Service metadata
cd gripday-user-service
mvn validate
```

### JSON Schema Validation

The metadata files follow Spring Boot's configuration metadata JSON schema:

- Schema: `https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html`
- Format: JSON with groups, properties, and hints

---

## Troubleshooting

### Issue: IDE Not Showing Autocomplete

**Solution:**

1. Rebuild project: `mvn clean install`
2. Refresh IDE: File → Invalidate Caches / Restart
3. Verify metadata file location: `src/main/resources/META-INF/spring-configuration-metadata.json`

### Issue: Deprecated Warning Not Showing

**Solution:**

1. Ensure IDE has Spring Boot plugin installed
2. Check that metadata file is in classpath
3. Verify JSON syntax is valid

### Issue: Wrong Default Values

**Solution:**

1. Check `defaultValue` in metadata matches actual code
2. Verify property type matches (String, Duration, Boolean, etc.)
3. Update metadata file and rebuild

---

## References

- [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [Spring Boot Configuration Processor](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html#appendix.configuration-metadata.annotation-processor)
- [AUTHENTICATION-ARCHITECTURE.md](AUTHENTICATION-ARCHITECTURE.md) - Architecture overview
- [AUTHENTICATION-MIGRATION-GUIDE.md](AUTHENTICATION-MIGRATION-GUIDE.md) - Migration instructions

---

## Summary

The configuration metadata has been updated to:

1. ✅ Add `jwk-set-uri` property for JWK endpoint configuration
2. ✅ Change default algorithm from HS256 to RS256
3. ✅ Remove `secret-key` property (no longer supported)
4. ✅ Update issuer default to match User Service
5. ✅ Add helpful hints for common configurations
6. ✅ Document token expiry properties

These changes improve the developer experience by providing better IDE support, validation, and documentation for the new RSA256-based authentication architecture.
