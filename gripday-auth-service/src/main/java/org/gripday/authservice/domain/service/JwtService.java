package org.gripday.authservice.domain.service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.gripday.authservice.config.JwtConfiguration;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.presentation.dto.UserContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation, validation, and management. Handles access tokens, refresh tokens, and token blacklisting.
 */
@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtConfiguration jwtConfiguration;
  private final RedisTemplate<String, String> redisTemplate;

  public JwtService(final JwtEncoder jwtEncoder, final JwtDecoder jwtDecoder,
      final JwtConfiguration jwtConfiguration, final RedisTemplate<String, String> redisTemplate) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.jwtConfiguration = jwtConfiguration;
    this.redisTemplate = redisTemplate;
  }

  /**
   * Generate access token with user context claims.
   */
  public String generateAccessToken(User user) {
    var now = Instant.now();
    var expiry = now.plus(jwtConfiguration.getAccessTokenExpiry());

    var userContext = createUserContext(user);
    var claims = createTokenClaims(userContext, now, expiry, "access");

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
    return jwt.getTokenValue();
  }

  /**
   * Generate refresh token for token renewal.
   */
  public String generateRefreshToken(User user) {
    var now = Instant.now();
    var expiry = now.plus(jwtConfiguration.getRefreshTokenExpiry());

    var claims = JwtClaimsSet.builder()
        .issuer(jwtConfiguration.getIssuer())
        .subject(user.getId().toString())
        .issuedAt(now)
        .expiresAt(expiry)
        .claim("type", "refresh")
        .claim("username", user.getUsername())
        .claim("tenantId", user.getTenantId())
        .build();

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
    return jwt.getTokenValue();
  }

  /**
   * Validate and decode JWT token.
   */
  public Jwt validateToken(String token) {
    try {
      var jwt = jwtDecoder.decode(token);

      // Check if token is blacklisted
      if (isTokenBlacklisted(jwt.getId())) {
        throw new JwtException("Token has been invalidated");
      }

      return jwt;
    } catch (JwtException e) {
      throw new JwtException("Invalid or expired token", e);
    }
  }

  /**
   * Extract user context from JWT token using pattern matching.
   */
  public UserContext extractUserContext(Jwt jwt) {
    var claims = jwt.getClaims();

    var userId = extractLong(claims.get("sub"));
    var username = extractString(claims.get("username"));
    var email = extractString(claims.get("email"));
    var roles = extractStringSet(claims.get("roles"));
    var permissions = extractStringSet(claims.get("permissions"));
    var firstName = extractString(claims.get("firstName"));
    var lastName = extractString(claims.get("lastName"));
    var tenantId = extractString(claims.get("tenantId"));
    var customClaims = extractCustomClaims(claims);

    return new UserContext(
        userId, username, email, roles, permissions,
        firstName, lastName, tenantId, customClaims
    );
  }

  /**
   * Invalidate token by adding to blacklist.
   */
  public void invalidateToken(String token) {
    try {
      var jwt = jwtDecoder.decode(token);
      var jti = jwt.getId();
      var expiry = jwt.getExpiresAt();

      if (jti != null && expiry != null) {
        var ttl = expiry.getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
          var blacklistKey = "blacklist:token:" + jti;
          redisTemplate.opsForValue().set(blacklistKey, "true", ttl, TimeUnit.SECONDS);
        }
      }
    } catch (JwtException e) {
      // Token is already invalid, no need to blacklist
    }
  }

  /**
   * Check if token is blacklisted.
   */
  private boolean isTokenBlacklisted(String jti) {
    if (jti == null) {
      return false;
    }

    var blacklistKey = "blacklist:token:" + jti;
    return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
  }

  /**
   * Revoke all refresh tokens for a specific user.
   */
  public void revokeAllRefreshTokensForUser(String userId) {
    var pattern = "refresh:token:" + userId + ":*";
    var keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }

    // Also add user to revoked refresh tokens set
    var revokedKey = "revoked:refresh:" + userId;
    redisTemplate.opsForValue().set(revokedKey, String.valueOf(Instant.now().getEpochSecond()));
  }

  /**
   * Check if user's refresh tokens have been revoked after a specific time.
   */
  public boolean isUserRefreshRevoked(String userId, Instant tokenIssuedAt) {
    var revokedKey = "revoked:refresh:" + userId;
    var revokedAtStr = redisTemplate.opsForValue().get(revokedKey);

    if (revokedAtStr == null) {
      return false;
    }

    try {
      var revokedAt = Instant.ofEpochSecond(Long.parseLong(revokedAtStr));
      return tokenIssuedAt.isBefore(revokedAt);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Create user context from User entity.
   */
  private UserContext createUserContext(User user) {
    var roles = user.getAuthorities().stream()
        .map(authority -> authority.getName())
        .collect(Collectors.toSet());

    return new UserContext(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles,
        Set.of(), // Permissions can be derived from roles
        user.getFirstName(),
        user.getLastName(),
        user.getTenantId(),
        Map.of()
    );
  }

  /**
   * Create JWT claims set with user context.
   */
  private JwtClaimsSet createTokenClaims(UserContext userContext, Instant issuedAt, Instant expiresAt, String type) {
    return JwtClaimsSet.builder()
        .issuer(jwtConfiguration.getIssuer())
        .subject(userContext.userId().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .id(generateJti())
        .claim("type", type)
        .claim("username", userContext.username())
        .claim("email", userContext.email())
        .claim("roles", userContext.roles())
        .claim("permissions", userContext.permissions())
        .claim("firstName", userContext.firstName())
        .claim("lastName", userContext.lastName())
        .claim("tenantId", userContext.tenantId())
        .build();
  }

  /**
   * Generate unique JWT ID.
   */
  private String generateJti() {
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Extract Long value from claims using pattern matching.
   */
  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  /**
   * Extract String value from claims.
   */
  private String extractString(Object value) {
    return value instanceof String s ? s : null;
  }

  /**
   * Extract Set of Strings from claims.
   */
  @SuppressWarnings("unchecked")
  private Set<String> extractStringSet(Object value) {
    return switch (value) {
      case Set<?> set -> set.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .collect(Collectors.toSet());
      case java.util.List<?> list -> list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .collect(Collectors.toSet());
      case null, default -> Set.of();
    };
  }

  /**
   * Extract custom claims excluding standard JWT claims.
   */
  private Map<String, Object> extractCustomClaims(Map<String, Object> allClaims) {
    var standardClaims = Set.of("sub", "iss", "iat", "exp", "jti", "type",
        "username", "email", "roles", "permissions",
        "firstName", "lastName", "tenantId");

    return allClaims.entrySet().stream()
        .filter(entry -> !standardClaims.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}