package com.iqscaffold.userservice.authentication;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.iqscaffold.userservice.config.JwtConfiguration;
import com.iqscaffold.userservice.shared.JwtClaimNames;
import com.iqscaffold.userservice.tenancy.TenantContext;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * Implementation of JwtService providing comprehensive JWT token management.
 *
 * <p>This service implements enterprise-grade JWT token handling with the following capabilities:
 * <ul>
 *   <li><strong>Token Generation</strong> - Creates access and refresh tokens with user context claims</li>
 *   <li><strong>Token Validation</strong> - Verifies signatures, expiration, and blacklist status</li>
 *   <li><strong>Token Blacklisting</strong> - Immediate token revocation using Redis</li>
 *   <li><strong>Multi-Device Support</strong> - Per-device and global token management</li>
 *   <li><strong>Key Rotation Support</strong> - Works with rotating RSA key pairs</li>
 * </ul>
 *
 * @author IQ Scaffold Team
 * @version 1.0
 * @see JwtService
 * @see UserContext
 * @see JwtConfiguration
 * @since 1.0
 */
@Service
public class JwtServiceImpl implements JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtConfiguration jwtConfiguration;
  private final RedisTemplate<String, String> redisTemplate;
  private final com.iqscaffold.userservice.organization.OrganizationRepository organizationRepository;

  public JwtServiceImpl(final JwtEncoder jwtEncoder, final JwtDecoder jwtDecoder,
                        final JwtConfiguration jwtConfiguration, final RedisTemplate<String, String> redisTemplate,
                        final com.iqscaffold.userservice.organization.OrganizationRepository organizationRepository) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.jwtConfiguration = jwtConfiguration;
    this.redisTemplate = redisTemplate;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public String generateAccessToken(User user) {
    var now = Instant.now();
    var expiry = now.plus(jwtConfiguration.getAccessTokenExpiry());

    var userContext = createUserContext(user);
    var claims = createTokenClaims(userContext, now, expiry, "access");

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
    return jwt.getTokenValue();
  }

  @Override
  public String generateRefreshToken(User user) {
    var now = Instant.now();
    var expiry = now.plus(jwtConfiguration.getRefreshTokenExpiry());

    var claims = JwtClaimsSet.builder()
        .issuer(jwtConfiguration.getIssuer())
        .subject(user.getId().toString())
        .issuedAt(now)
        .expiresAt(expiry)
        .claim(JwtClaimNames.TYPE, JwtClaimNames.TOKEN_TYPE_REFRESH)
        .claim(JwtClaimNames.USERNAME, user.getUsername())
        .claim(JwtClaimNames.TENANT_ID, user.getTenantId())
        .build();

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
    return jwt.getTokenValue();
  }

  @Override
  public Jwt validateToken(String token) {
    try {
      var jwt = jwtDecoder.decode(token);

      // Check if token is blacklisted
      if (isTokenBlacklisted(jwt.getId())) {
        throw new JwtException("Token has been invalidated");
      }

      return jwt;
    } catch (final JwtException e) {
      throw new JwtException("Invalid or expired token", e);
    }
  }

  @Override
  public UserContext extractUserContext(Jwt jwt) {
    var claims = jwt.getClaims();

    var userId = extractLong(claims.get(JwtClaimNames.SUBJECT));
    var username = extractString(claims.get(JwtClaimNames.USERNAME));
    var email = extractString(claims.get(JwtClaimNames.EMAIL));
    var authorities = extractStringSet(claims.get(JwtClaimNames.AUTHORITIES));
    var permissions = extractStringSet(claims.get(JwtClaimNames.PERMISSIONS));
    var firstName = extractString(claims.get(JwtClaimNames.FIRST_NAME));
    var lastName = extractString(claims.get(JwtClaimNames.LAST_NAME));
    var tenantId = extractString(claims.get(JwtClaimNames.TENANT_ID));
    var organizationId = extractLong(claims.get(JwtClaimNames.ORGANIZATION_ID));
    var customClaims = extractCustomClaims(claims);

    return new UserContext(
        userId, username, email, authorities, permissions,
        firstName, lastName, tenantId, organizationId, customClaims
    );
  }

  @Override
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
    } catch (final JwtException e) {
      // Token is already invalid, no need to blacklist
    }
  }

  @Override
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

  @Override
  public boolean isUserRefreshRevoked(String userId, Instant tokenIssuedAt) {
    var revokedKey = "revoked:refresh:" + userId;
    var revokedAtStr = redisTemplate.opsForValue().get(revokedKey);

    if (revokedAtStr == null) {
      return false;
    }

    try {
      var revokedAt = Instant.ofEpochSecond(Long.parseLong(revokedAtStr));
      return tokenIssuedAt.isBefore(revokedAt);
    } catch (final NumberFormatException e) {
      return false;
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
   * Create user context from User entity.
   */
  private UserContext createUserContext(User user) {
    Set<String> authorities = user.getAuthorities().stream()
        .map(authority -> authority.getName())
        .collect(Collectors.toSet());

    // Get organization ID from tenant (1:1 relationship)
    String tenantId = TenantContext.getCurrentTenantId();
    Long organizationId = null;
    if (tenantId != null) {
      organizationId = organizationRepository.findByTenantId(tenantId)
          .map(com.iqscaffold.userservice.organization.Organization::getId)
          .orElse(null);
    }

    return new UserContext(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        authorities,
        Set.of(), // Permissions can be derived from authorities
        user.getFirstName(),
        user.getLastName(),
        user.getTenantId(),
        organizationId,
        Map.of("preferredLocale", user.getPreferredLocale())
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
        .claim(JwtClaimNames.TYPE, type)
        .claim(JwtClaimNames.USERNAME, userContext.username())
        .claim(JwtClaimNames.EMAIL, userContext.email())
        .claim(JwtClaimNames.AUTHORITIES, userContext.authorities())
        .claim(JwtClaimNames.PERMISSIONS, userContext.permissions())
        .claim(JwtClaimNames.FIRST_NAME, userContext.firstName())
        .claim(JwtClaimNames.LAST_NAME, userContext.lastName())
        .claim(JwtClaimNames.TENANT_ID, userContext.tenantId())
        .claim(JwtClaimNames.ORGANIZATION_ID, userContext.organizationId())
        .claim(JwtClaimNames.PREFERRED_LOCALE, userContext.customClaims().get("preferredLocale"))
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
        } catch (final NumberFormatException e) {
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
    var standardClaims = Set.of(
        JwtClaimNames.SUBJECT,
        JwtClaimNames.ISSUER,
        JwtClaimNames.ISSUED_AT,
        JwtClaimNames.EXPIRATION,
        JwtClaimNames.JWT_ID,
        JwtClaimNames.TYPE,
        JwtClaimNames.USERNAME,
        JwtClaimNames.EMAIL,
        JwtClaimNames.AUTHORITIES,
        JwtClaimNames.PERMISSIONS,
        JwtClaimNames.FIRST_NAME,
        JwtClaimNames.LAST_NAME,
        JwtClaimNames.TENANT_ID,
        JwtClaimNames.ORGANIZATION_ID,
        JwtClaimNames.PREFERRED_LOCALE
    );

    return allClaims.entrySet().stream()
        .filter(entry -> !standardClaims.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
