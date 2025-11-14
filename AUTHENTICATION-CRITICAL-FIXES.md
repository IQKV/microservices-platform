# Critical Authentication Fixes - Implementation Guide

This document provides implementation guidance for the **4 critical gaps** that must be addressed before production deployment.

---

## 1. JWK Endpoint Implementation

### Problem
Downstream services cannot dynamically fetch the public key for JWT validation.

### Solution

#### Step 1: Create JWK Controller in User Service

```java
package org.gripday.userservice.presentation.web;

import java.util.Map;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWK Set endpoint for public key distribution.
 * Allows downstream services to validate JWT tokens.
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "JWK Set", description = "JSON Web Key Set for JWT validation")
public class JwkSetController {

  private final JWKSource<SecurityContext> jwkSource;

  public JwkSetController(final JWKSource<SecurityContext> jwkSource) {
    this.jwkSource = jwkSource;
  }

  @Operation(
      summary = "Get JWK Set",
      description = "Returns the JSON Web Key Set containing public keys for JWT validation"
  )
  @GetMapping("/jwks.json")
  public Map<String, Object> jwkSet() {
    try {
      return jwkSource.getJWKSet(null, null).toJSONObject();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to retrieve JWK Set", e);
    }
  }
}
```

#### Step 2: Update Security Config to Allow Public Access

```java
// In SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    // JWK endpoint must be public
    .requestMatchers("/.well-known/jwks.json").permitAll()
    // ... rest of config
)
```

#### Step 3: Update Downstream Services Configuration

```yaml
# bookstore-service/src/main/resources/application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://user-service:8080
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

#### Step 4: Remove Manual Public Key Configuration

```java
// Remove this from downstream services:
@Bean
public JwtDecoder jwtDecoder(KeyPair keyPair) {
  var publicKey = (RSAPublicKey) keyPair.getPublic();
  return NimbusJwtDecoder.withPublicKey(publicKey).build();
}

// Spring Boot will auto-configure using jwk-set-uri
```

### Testing

```bash
# Test JWK endpoint
curl http://localhost:8080/.well-known/jwks.json

# Expected response:
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "gripday-user-key",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

---

## 2. Key Rotation Implementation

### Problem
RSA keys are generated once at startup with no rotation mechanism.

### Solution

#### Step 1: Create Key Storage Service

```java
package org.gripday.userservice.domain.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages RSA key pairs for JWT signing with rotation support.
 */
@Service
public class JwtKeyManagementService {

  private static final Logger logger = LoggerFactory.getLogger(JwtKeyManagementService.class);
  private static final int KEY_SIZE = 2048;
  private static final long KEY_ROTATION_GRACE_PERIOD_DAYS = 7;

  private final ConcurrentHashMap<String, KeyEntry> keys = new ConcurrentHashMap<>();
  private volatile String currentKeyId;

  public JwtKeyManagementService() {
    // Generate initial key pair
    rotateKeys();
  }

  /**
   * Rotate keys by generating a new key pair.
   * Old keys are kept for validation during grace period.
   */
  public synchronized void rotateKeys() {
    try {
      var keyId = UUID.randomUUID().toString();
      var keyPair = generateKeyPair();
      var keyEntry = new KeyEntry(keyId, keyPair, Instant.now());

      keys.put(keyId, keyEntry);
      currentKeyId = keyId;

      logger.info("Generated new RSA key pair with ID: {}", keyId);

      // Clean up old keys (older than grace period)
      cleanupOldKeys();

    } catch (final Exception e) {
      logger.error("Failed to rotate keys", e);
      throw new RuntimeException("Key rotation failed", e);
    }
  }

  /**
   * Get current key pair for signing.
   */
  public KeyPair getCurrentKeyPair() {
    var keyEntry = keys.get(currentKeyId);
    if (keyEntry == null) {
      throw new IllegalStateException("No current key pair available");
    }
    return keyEntry.keyPair();
  }

  /**
   * Get current key ID.
   */
  public String getCurrentKeyId() {
    return currentKeyId;
  }

  /**
   * Get JWK Set containing all active public keys.
   */
  public JWKSet getJwkSet() {
    var jwkList = new ArrayList<RSAKey>();

    for (var entry : keys.values()) {
      var publicKey = (RSAPublicKey) entry.keyPair().getPublic();
      var privateKey = (RSAPrivateKey) entry.keyPair().getPrivate();

      var jwk = new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID(entry.keyId())
          .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
          .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
          .build();

      jwkList.add(jwk);
    }

    return new JWKSet(jwkList);
  }

  /**
   * Get key pair by ID for validation.
   */
  public KeyPair getKeyPairById(String keyId) {
    var keyEntry = keys.get(keyId);
    return keyEntry != null ? keyEntry.keyPair() : null;
  }

  /**
   * Remove keys older than grace period.
   */
  private void cleanupOldKeys() {
    var cutoffTime = Instant.now().minusSeconds(KEY_ROTATION_GRACE_PERIOD_DAYS * 24 * 60 * 60);

    var removedKeys = keys.entrySet().stream()
        .filter(entry -> !entry.getKey().equals(currentKeyId))
        .filter(entry -> entry.getValue().createdAt().isBefore(cutoffTime))
        .map(entry -> {
          keys.remove(entry.getKey());
          return entry.getKey();
        })
        .toList();

    if (!removedKeys.isEmpty()) {
      logger.info("Removed {} old key(s): {}", removedKeys.size(), removedKeys);
    }
  }

  private KeyPair generateKeyPair() {
    try {
      var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(KEY_SIZE);
      return keyPairGenerator.generateKeyPair();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to generate RSA key pair", e);
    }
  }

  /**
   * Key entry with metadata.
   */
  private record KeyEntry(
      String keyId,
      KeyPair keyPair,
      Instant createdAt
  ) {
  }
}
```

#### Step 2: Update JWT Configuration

```java
package org.gripday.userservice.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.gripday.userservice.domain.service.JwtKeyManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfiguration {

  private final JwtKeyManagementService keyManagementService;

  public JwtConfiguration(final JwtKeyManagementService keyManagementService) {
    this.keyManagementService = keyManagementService;
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    return (jwkSelector, context) -> jwkSelector.select(keyManagementService.getJwkSet());
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    // Use JWK Set for validation (supports multiple keys)
    return NimbusJwtDecoder.withJwkSetUri("http://localhost:8080/.well-known/jwks.json").build();
  }
}
```

#### Step 3: Update JWT Service to Include Key ID

```java
// In JwtService.java
private JwtClaimsSet createTokenClaims(...) {
  return JwtClaimsSet.builder()
      .issuer(jwtConfiguration.getIssuer())
      .subject(userContext.userId().toString())
      .issuedAt(issuedAt)
      .expiresAt(expiresAt)
      .id(generateJti())
      .claim("type", type)
      // Add other claims...
      .build();
}

// JWT encoder will automatically add "kid" header
```

#### Step 4: Add Scheduled Key Rotation

```java
package org.gripday.userservice.config;

import org.gripday.userservice.domain.service.JwtKeyManagementService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class KeyRotationScheduler {

  private final JwtKeyManagementService keyManagementService;

  public KeyRotationScheduler(final JwtKeyManagementService keyManagementService) {
    this.keyManagementService = keyManagementService;
  }

  /**
   * Rotate keys every 90 days (security best practice).
   */
  @Scheduled(cron = "0 0 0 1 */3 *") // Every 3 months at midnight
  public void rotateKeys() {
    keyManagementService.rotateKeys();
  }
}
```

### Configuration

```yaml
# application.yml
gripday:
  auth:
    jwt:
      key-rotation:
        enabled: true
        schedule: "0 0 0 1 */3 *"  # Every 3 months
        grace-period-days: 7        # Keep old keys for 7 days
```

---

## 3. Consistent JWT Validation (Remove HMAC from Gateway)

### Problem
Gateway uses HMAC-SHA256 while downstream services use RSA256, creating complexity and security concerns.

### Solution

#### Step 1: Update Gateway Security Configuration

```java
package org.gripday.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private final GripdayProperties gripdayProperties;

  public SecurityConfiguration(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(gripdayProperties.gateway().security().publicPaths().toArray(new String[0]))
            .permitAll()
            .anyExchange()
            .authenticated()
        )
        // Use OAuth2 Resource Server with JWK Set
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
        )
        .build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    var jwkSetUri = gripdayProperties.gateway().security().jwt().jwkSetUri();
    return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }
}
```

#### Step 2: Update Gateway Properties

```yaml
# gateway-service/src/main/resources/application.yml
gripday:
  gateway:
    security:
      jwt:
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
        issuer: gripday-user-service
      # Remove secret-key configuration
```

#### Step 3: Update JWT Authentication Filter

```java
package org.gripday.gatewayservice.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extract user context from validated JWT and propagate via headers.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication())
        .filter(auth -> auth instanceof JwtAuthenticationToken)
        .map(auth -> (JwtAuthenticationToken) auth)
        .map(jwtAuth -> jwtAuth.getToken())
        .flatMap(jwt -> {
          // Extract user context from JWT
          var userContext = extractUserContext(jwt);
          
          // Add headers
          var modifiedRequest = exchange.getRequest().mutate()
              .header("X-User-ID", userContext.userId().toString())
              .header("X-Username", userContext.username())
              .header("X-User-Roles", String.join(",", userContext.roles()))
              .header("X-Tenant-ID", userContext.tenantId())
              .build();
          
          return chain.filter(exchange.mutate().request(modifiedRequest).build());
        })
        .switchIfEmpty(chain.filter(exchange));
  }

  private UserContext extractUserContext(Jwt jwt) {
    // Extract from JWT claims
    return new UserContext(
        jwt.getClaim("userId"),
        jwt.getClaim("username"),
        jwt.getClaim("email"),
        jwt.getClaim("roles"),
        jwt.getClaim("tenantId")
    );
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
```

#### Step 4: Remove HMAC Dependencies

```xml
<!-- Remove from pom.xml if not needed elsewhere -->
<!-- <dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
</dependency> -->
```

### Benefits
- ✅ Single algorithm (RSA256) everywhere
- ✅ No shared secrets to manage
- ✅ Easier key rotation
- ✅ Better security (asymmetric)
- ✅ Simpler configuration

---

## 4. Token Cleanup Jobs

### Problem
Redis accumulates stale refresh tokens and revocation records without cleanup.

### Solution

#### Step 1: Create Token Cleanup Service

```java
package org.gripday.userservice.domain.service;

import java.time.Instant;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled cleanup service for expired tokens and sessions.
 */
@Service
public class TokenCleanupService {

  private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public TokenCleanupService(
      final RedisTemplate<String, String> redisTemplate,
      final MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Clean up expired tokens and sessions daily at 2 AM.
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupExpiredTokens() {
    logger.info("Starting token cleanup job");
    var startTime = Instant.now();

    try {
      var refreshTokensRemoved = cleanupExpiredRefreshTokens();
      var revocationRecordsRemoved = cleanupOldRevocationRecords();
      var sessionsRemoved = cleanupExpiredSessions();
      var blacklistEntriesRemoved = cleanupExpiredBlacklistEntries();

      var duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();

      logger.info("Token cleanup completed in {}ms. Removed: {} refresh tokens, {} revocation records, {} sessions, {} blacklist entries",
          duration, refreshTokensRemoved, revocationRecordsRemoved, sessionsRemoved, blacklistEntriesRemoved);

      // Record metrics
      meterRegistry.counter("token.cleanup.refresh_tokens", "status", "removed").increment(refreshTokensRemoved);
      meterRegistry.counter("token.cleanup.revocations", "status", "removed").increment(revocationRecordsRemoved);
      meterRegistry.counter("token.cleanup.sessions", "status", "removed").increment(sessionsRemoved);
      meterRegistry.counter("token.cleanup.blacklist", "status", "removed").increment(blacklistEntriesRemoved);
      meterRegistry.timer("token.cleanup.duration").record(java.time.Duration.ofMillis(duration));

    } catch (final Exception e) {
      logger.error("Token cleanup job failed", e);
      meterRegistry.counter("token.cleanup.errors").increment();
    }
  }

  /**
   * Remove expired refresh tokens (older than 7 days).
   */
  private long cleanupExpiredRefreshTokens() {
    var pattern = "refresh:token:*";
    var keys = redisTemplate.keys(pattern);
    
    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;
    var cutoffTime = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 days

    for (var key : keys) {
      try {
        // Check if token is expired
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup refresh token: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove old revocation records (older than 30 days).
   */
  private long cleanupOldRevocationRecords() {
    var pattern = "revoked:refresh:*";
    var keys = redisTemplate.keys(pattern);
    
    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;
    var cutoffTime = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 days

    for (var key : keys) {
      try {
        var revokedAtStr = redisTemplate.opsForValue().get(key);
        if (revokedAtStr != null) {
          var revokedAt = Instant.ofEpochSecond(Long.parseLong(revokedAtStr));
          if (revokedAt.isBefore(cutoffTime)) {
            redisTemplate.delete(key);
            removed++;
          }
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup revocation record: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove expired sessions.
   */
  private long cleanupExpiredSessions() {
    var pattern = "session:*";
    var keys = redisTemplate.keys(pattern);
    
    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;

    for (var key : keys) {
      try {
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup session: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove expired blacklist entries (already handled by TTL, but double-check).
   */
  private long cleanupExpiredBlacklistEntries() {
    var pattern = "blacklist:token:*";
    var keys = redisTemplate.keys(pattern);
    
    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;

    for (var key : keys) {
      try {
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup blacklist entry: {}", key, e);
      }
    }

    return removed;
  }
}
```

#### Step 2: Enable Scheduling

```java
// In UserServiceApplication.java or separate config
@EnableScheduling
@SpringBootApplication
public class UserServiceApplication {
  // ...
}
```

#### Step 3: Add Configuration

```yaml
# application.yml
gripday:
  auth:
    token-cleanup:
      enabled: true
      schedule: "0 0 2 * * *"  # Daily at 2 AM
      refresh-token-retention-days: 7
      revocation-record-retention-days: 30
```

#### Step 4: Add Metrics Dashboard

```yaml
# Prometheus metrics for monitoring
token_cleanup_refresh_tokens_total{status="removed"}
token_cleanup_revocations_total{status="removed"}
token_cleanup_sessions_total{status="removed"}
token_cleanup_blacklist_total{status="removed"}
token_cleanup_errors_total
token_cleanup_duration_seconds
```

---

## Testing All Fixes

### 1. Test JWK Endpoint

```bash
# Get JWK Set
curl http://localhost:8080/.well-known/jwks.json | jq

# Verify key structure
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "...",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

### 2. Test Key Rotation

```bash
# Trigger manual rotation (add admin endpoint)
curl -X POST http://localhost:8080/api/v1/admin/keys/rotate \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Verify old tokens still work (grace period)
curl -H "Authorization: Bearer $OLD_TOKEN" \
     http://localhost:8080/api/v1/auth/profile

# Verify new tokens use new key
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}' | jq -r '.accessToken' | \
  jwt decode -

# Check kid in header
```

### 3. Test Gateway RSA Validation

```bash
# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}' | jq -r '.accessToken')

# Access via Gateway
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/bookstore/books

# Verify headers propagated
# Check Gateway logs for X-User-ID, X-Username, etc.
```

### 4. Test Token Cleanup

```bash
# Check Redis before cleanup
redis-cli KEYS "refresh:token:*" | wc -l
redis-cli KEYS "revoked:refresh:*" | wc -l

# Trigger cleanup (or wait for scheduled run)
# Add admin endpoint to trigger manually
curl -X POST http://localhost:8080/api/v1/admin/tokens/cleanup \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Check Redis after cleanup
redis-cli KEYS "refresh:token:*" | wc -l
redis-cli KEYS "revoked:refresh:*" | wc -l

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep token_cleanup
```

---

## Deployment Checklist

### Before Deployment

- [ ] JWK endpoint implemented and tested
- [ ] Key rotation service implemented
- [ ] Gateway updated to use RSA validation
- [ ] Token cleanup jobs scheduled
- [ ] All tests passing
- [ ] Metrics and monitoring configured
- [ ] Documentation updated

### During Deployment

1. Deploy User Service first (with JWK endpoint)
2. Verify JWK endpoint accessible
3. Deploy Gateway Service (with RSA validation)
4. Deploy downstream services
5. Verify end-to-end authentication flow
6. Monitor metrics and logs

### After Deployment

- [ ] Verify JWK endpoint returns valid keys
- [ ] Test authentication flow
- [ ] Verify token validation works
- [ ] Check cleanup job runs successfully
- [ ] Monitor Redis memory usage
- [ ] Set up alerts for failures

---

## Rollback Plan

If issues occur:

1. **JWK Endpoint Issues:**
   - Revert to manual public key configuration
   - Update downstream services

2. **Key Rotation Issues:**
   - Disable scheduled rotation
   - Use single key temporarily

3. **Gateway RSA Validation Issues:**
   - Revert to HMAC validation
   - Update configuration

4. **Cleanup Job Issues:**
   - Disable scheduled cleanup
   - Manual cleanup if needed

---

## Monitoring and Alerts

### Key Metrics to Monitor

```yaml
# JWK Endpoint
- http_server_requests_seconds{uri="/.well-known/jwks.json"}
- jwk_endpoint_errors_total

# Key Rotation
- jwt_key_rotation_total
- jwt_key_rotation_errors_total
- jwt_active_keys_count

# Token Cleanup
- token_cleanup_refresh_tokens_total
- token_cleanup_duration_seconds
- token_cleanup_errors_total

# Redis Memory
- redis_memory_used_bytes
- redis_keys_count{pattern="refresh:token:*"}
```

### Recommended Alerts

```yaml
# Alert if JWK endpoint fails
- alert: JWKEndpointDown
  expr: rate(jwk_endpoint_errors_total[5m]) > 0
  for: 5m

# Alert if key rotation fails
- alert: KeyRotationFailed
  expr: rate(jwt_key_rotation_errors_total[1h]) > 0

# Alert if cleanup job fails
- alert: TokenCleanupFailed
  expr: rate(token_cleanup_errors_total[1d]) > 0

# Alert if Redis memory grows too large
- alert: RedisMemoryHigh
  expr: redis_memory_used_bytes > 1GB
```

---

## Conclusion

These 4 critical fixes address the most important gaps in the authentication implementation:

1. ✅ **JWK Endpoint** - Enables dynamic key distribution
2. ✅ **Key Rotation** - Improves security posture
3. ✅ **Consistent Validation** - Simplifies architecture
4. ✅ **Token Cleanup** - Prevents memory leaks

Implement these fixes before production deployment to ensure a secure, scalable, and maintainable authentication system.
