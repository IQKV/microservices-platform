# Authentication Migration Guide

## Overview

This guide helps you migrate from the old authentication implementation (HMAC in Gateway) to the new implementation (RSA256 everywhere with JWK endpoint).

---

## Migration Strategy

### Option 1: Zero-Downtime Migration (Recommended)

This approach allows both HMAC and RSA validation to work simultaneously during migration.

#### Phase 1: Deploy User Service with JWK Endpoint

1. **Deploy User Service** with new changes
   - JWK endpoint will be available
   - Existing RSA token generation continues to work
   - No breaking changes

2. **Verify JWK Endpoint**
   ```bash
   curl http://user-service:8080/.well-known/jwks.json
   ```

#### Phase 2: Update Gateway to Support Both Methods

1. **Temporarily keep HMAC validation** in Gateway
2. **Add RSA validation** alongside HMAC
3. **Test both methods** work correctly

#### Phase 3: Switch Gateway to RSA Only

1. **Deploy Gateway** with RSA-only validation
2. **Monitor for errors**
3. **Verify all services** can authenticate

#### Phase 4: Update Downstream Services

1. **Update each downstream service** to use JWK endpoint
2. **Deploy one service at a time**
3. **Verify authentication** works

#### Phase 5: Cleanup

1. **Remove HMAC configuration** from Gateway
2. **Remove shared secret** from environment variables
3. **Update documentation**

### Option 2: Quick Migration (Requires Downtime)

If you can afford brief downtime:

1. **Deploy all services simultaneously**
2. **Restart all services**
3. **Verify authentication works**

---

## Step-by-Step Instructions

### Step 1: Backup Current Configuration

```bash
# Backup User Service configuration
cp gripday-user-service/src/main/resources/application.yml \
   gripday-user-service/src/main/resources/application.yml.backup

# Backup Gateway configuration
cp gripday-gateway-service/src/main/resources/application.yml \
   gripday-gateway-service/src/main/resources/application.yml.backup
```

### Step 2: Update User Service

1. **Add new files:**
   - `JwtKeyManagementService.java`
   - `TokenCleanupService.java`
   - `JwkSetResource.java`
   - `AdminKeyManagementResource.java`

2. **Update existing files:**
   - `JwtConfiguration.java`
   - `SecurityConfig.java`

3. **Build and test:**

   ```bash
   cd gripday-user-service
   mvn clean package -DskipTests
   ```

4. **Deploy User Service:**

   ```bash
   # Stop current service
   docker-compose stop user-service

   # Deploy new version
   docker-compose up -d user-service

   # Check logs
   docker-compose logs -f user-service
   ```

5. **Verify JWK endpoint:**
   ```bash
   curl http://localhost:8080/.well-known/jwks.json
   ```

### Step 3: Update Gateway Service

1. **Update files:**
   - `SecurityConfiguration.java`
   - `JwtAuthenticationFilter.java`
   - `GripdayProperties.java`
   - `application.yml`

2. **Update environment variables:**

   ```bash
   # Add to .env or docker-compose.yml
   USER_SERVICE_JWK_URI=http://user-service:8080/.well-known/jwks.json
   ```

3. **Build and test:**

   ```bash
   cd gripday-gateway-service
   mvn clean package -DskipTests
   ```

4. **Deploy Gateway Service:**

   ```bash
   # Stop current service
   docker-compose stop gateway-service

   # Deploy new version
   docker-compose up -d gateway-service

   # Check logs
   docker-compose logs -f gateway-service
   ```

### Step 4: Update Downstream Services

For each downstream service (e.g., Bookstore Service):

1. **Update application.yml:**

   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://user-service:8080
             jwk-set-uri: http://user-service:8080/.well-known/jwks.json
   ```

2. **Remove manual public key configuration** (if any)

3. **Deploy service:**
   ```bash
   docker-compose restart bookstore-service
   ```

### Step 5: Verify End-to-End

1. **Test login:**

   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"Test123!"}' | jq -r '.accessToken')

   echo "Token: $TOKEN"
   ```

2. **Test protected endpoint:**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        http://localhost:8080/api/v1/bookstore/books
   ```

3. **Test token refresh:**

   ```bash
   REFRESH_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"Test123!"}' | jq -r '.refreshToken')

   curl -X POST http://localhost:8080/api/v1/auth/refresh \
     -H "Content-Type: application/json" \
     -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
   ```

4. **Check metrics:**
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep jwt
   curl http://localhost:8080/actuator/prometheus | grep token_cleanup
   ```

### Step 6: Cleanup

1. **Remove HMAC secret from environment:**

   ```bash
   # Remove from .env or docker-compose.yml
   # JWT_SECRET_KEY=...
   ```

2. **Update documentation**

3. **Remove backup files** (after confirming everything works)

---

## Configuration Changes Summary

### User Service

**Before:**

```yaml
gripday:
  auth:
    jwt:
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
      issuer: gripday-user-service
```

**After:** (No changes needed - same configuration)

### Gateway Service

**Before:**

```yaml
gripday:
  gateway:
    security:
      jwt:
        secret-key: ${JWT_SECRET_KEY}
        issuer: gripday
        audience: gripday-services
        algorithm: HS256
```

**After:**

```yaml
gripday:
  gateway:
    security:
      jwt:
        issuer: gripday-user-service
        algorithm: RS256
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
      public-paths:
        - /.well-known/jwks.json
        # ... other paths
```

### Downstream Services

**Before:**

```java
@Bean
public JwtDecoder jwtDecoder(KeyPair keyPair) {
  var publicKey = (RSAPublicKey) keyPair.getPublic();
  return NimbusJwtDecoder.withPublicKey(publicKey).build();
}
```

**After:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

---

## Rollback Procedure

If you need to rollback:

### Quick Rollback

1. **Restore backup configurations:**

   ```bash
   cp application.yml.backup application.yml
   ```

2. **Redeploy services:**
   ```bash
   docker-compose restart user-service gateway-service
   ```

### Detailed Rollback

#### User Service

1. Revert code changes
2. Rebuild: `mvn clean package`
3. Redeploy: `docker-compose up -d user-service`

#### Gateway Service

1. Revert code changes
2. Restore HMAC configuration
3. Rebuild: `mvn clean package`
4. Redeploy: `docker-compose up -d gateway-service`

#### Downstream Services

1. Revert to manual public key configuration
2. Redeploy each service

---

## Troubleshooting

### Issue: JWK Endpoint Returns 404

**Cause:** SecurityConfig not updated to allow public access

**Solution:**

```java
.requestMatchers("/.well-known/jwks.json").permitAll()
```

### Issue: Gateway Cannot Validate Tokens

**Cause:** JWK URI not configured or incorrect

**Solution:**

```yaml
gripday:
  gateway:
    security:
      jwt:
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

### Issue: Downstream Service Cannot Connect to JWK Endpoint

**Cause:** Network connectivity or DNS resolution issue

**Solution:**

1. Check service discovery
2. Verify network connectivity
3. Use IP address instead of hostname temporarily

### Issue: Old Tokens Not Working After Key Rotation

**Cause:** Grace period expired or not configured

**Solution:**

- Grace period is 7 days by default
- Old tokens should work during grace period
- Check key management service logs

### Issue: Redis Memory Growing

**Cause:** Token cleanup job not running

**Solution:**

1. Check if `@EnableScheduling` is present
2. Verify cleanup job logs
3. Manually trigger cleanup:
   ```bash
   curl -X POST http://localhost:8080/api/v1/admin/keys/cleanup \
     -H "Authorization: Bearer $ADMIN_TOKEN"
   ```

---

## Verification Checklist

After migration, verify:

- [ ] JWK endpoint accessible: `curl http://localhost:8080/.well-known/jwks.json`
- [ ] Login works: Test with valid credentials
- [ ] Token validation works: Access protected endpoints
- [ ] Token refresh works: Refresh access token
- [ ] Logout works: Verify token blacklisting
- [ ] Gateway propagates headers: Check downstream logs
- [ ] Metrics available: Check Prometheus endpoint
- [ ] Cleanup job scheduled: Check logs at 2 AM
- [ ] Key rotation scheduled: Verify cron expression
- [ ] Admin endpoints work: Test manual rotation/cleanup

---

## Performance Considerations

### JWK Endpoint Caching

Downstream services cache JWK Set to reduce load:

```yaml
# Spring Security default caching
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
          # Cached for 5 minutes by default
```

### Redis Connection Pool

Ensure Redis connection pool is properly sized:

```yaml
gripday:
  cache:
    redis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
```

### Token Cleanup Performance

Monitor cleanup job duration:

```bash
curl http://localhost:8080/actuator/prometheus | grep token_cleanup_duration
```

---

## Security Considerations

### Key Rotation Schedule

Default: Every 90 days

To change:

```java
@Scheduled(cron = "0 0 0 1 */3 *") // Every 3 months
```

### Grace Period

Default: 7 days

To change:

```java
private static final long KEY_ROTATION_GRACE_PERIOD_DAYS = 7;
```

### Token Expiry

- Access Token: 15 minutes
- Refresh Token: 7 days

To change:

```yaml
gripday:
  auth:
    jwt:
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
```

---

## Monitoring After Migration

### Key Metrics

```bash
# JWK endpoint requests
http_server_requests_seconds_count{uri="/.well-known/jwks.json"}

# Token cleanup
token_cleanup_refresh_tokens_total
token_cleanup_duration_seconds

# Key rotation
jwt_key_rotation_total
jwt_active_keys_count

# Redis memory
redis_memory_used_bytes
```

### Logs to Monitor

```bash
# User Service
docker-compose logs -f user-service | grep -E "JWK|rotation|cleanup"

# Gateway Service
docker-compose logs -f gateway-service | grep -E "JWT|authentication"
```

---

## Support

If you encounter issues during migration:

1. Check logs for error messages
2. Verify configuration matches this guide
3. Test each component individually
4. Use rollback procedure if needed
5. Consult AUTHENTICATION-FIXES-IMPLEMENTATION.md for details

---

## Post-Migration Tasks

1. **Update documentation** with new architecture
2. **Train team** on new authentication flow
3. **Set up monitoring** and alerts
4. **Schedule key rotation** review
5. **Plan for future enhancements** (MFA, OAuth2, etc.)

---

## Success Criteria

Migration is successful when:

- ✅ All services authenticate successfully
- ✅ JWK endpoint returns valid keys
- ✅ Token validation works across all services
- ✅ Token cleanup runs without errors
- ✅ Key rotation works (test manually)
- ✅ Metrics are being collected
- ✅ No authentication errors in logs
- ✅ Performance is acceptable
- ✅ Security is improved
- ✅ Team is trained on new system
