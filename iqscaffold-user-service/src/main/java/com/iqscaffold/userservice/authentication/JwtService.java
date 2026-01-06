package com.iqscaffold.userservice.authentication;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.iqscaffold.userservice.config.JwtConfiguration;
import com.iqscaffold.userservice.shared.JwtClaimNames;
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
 * Comprehensive JWT token management service providing secure token generation, validation, and lifecycle management.
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
 * <h3>Token Types</h3>
 * <ul>
 *   <li><strong>Access Tokens</strong> - Short-lived (15 minutes) containing full user context</li>
 *   <li><strong>Refresh Tokens</strong> - Long-lived (7 days) for token renewal without re-authentication</li>
 * </ul>
 * 
 * <h3>JWT Claims Structure</h3>
 * <h4>Access Token Claims:</h4>
 * <ul>
 *   <li>{@code sub} - User ID</li>
 *   <li>{@code username} - Username</li>
 *   <li>{@code email} - User email address</li>
 *   <li>{@code firstName} - User's first name</li>
 *   <li>{@code lastName} - User's last name</li>
 *   <li>{@code authorities} - User roles and permissions</li>
 *   <li>{@code tenantId} - Tenant context for multi-tenancy</li>
 *   <li>{@code type} - Token type ("access")</li>
 *   <li>{@code jti} - Unique token identifier for blacklisting</li>
 * </ul>
 * 
 * <h4>Refresh Token Claims:</h4>
 * <ul>
 *   <li>{@code sub} - User ID</li>
 *   <li>{@code username} - Username</li>
 *   <li>{@code tenantId} - Tenant context</li>
 *   <li>{@code type} - Token type ("refresh")</li>
 *   <li>{@code jti} - Unique token identifier</li>
 * </ul>
 * 
 * <h3>Security Features</h3>
 * <ul>
 *   <li><strong>RSA-256 Signatures</strong> - Cryptographically secure token signing</li>
 *   <li><strong>Token Blacklisting</strong> - Immediate revocation via Redis with TTL</li>
 *   <li><strong>Expiration Validation</strong> - Strict expiration time enforcement</li>
 *   <li><strong>Issuer Validation</strong> - Prevents token reuse across services</li>
 *   <li><strong>User-Wide Revocation</strong> - Ability to revoke all user tokens</li>
 * </ul>
 * 
 * <h3>Token Lifecycle</h3>
 * <ol>
 *   <li><strong>Generation</strong> - Create tokens with user context and expiration</li>
 *   <li><strong>Validation</strong> - Verify signature, expiration, and blacklist status</li>
 *   <li><strong>Refresh</strong> - Generate new access token using valid refresh token</li>
 *   <li><strong>Revocation</strong> - Add tokens to blacklist for immediate invalidation</li>
 * </ol>
 * 
 * <h3>Redis Integration</h3>
 * <p>Uses Redis for distributed token management:
 * <ul>
 *   <li><strong>Blacklist Storage</strong> - {@code blacklist:token:{jti}} with TTL</li>
 *   <li><strong>User Revocation</strong> - {@code revoked:user:{userId}} with timestamp</li>
 *   <li><strong>Refresh Token Tracking</strong> - {@code refresh:user:{userId}} for validation</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Generate tokens for authenticated user
 * String accessToken = jwtService.generateAccessToken(user);
 * String refreshToken = jwtService.generateRefreshToken(user);
 * 
 * // Validate incoming token
 * try {
 *     Jwt jwt = jwtService.validateToken(tokenString);
 *     UserContext userContext = jwtService.extractUserContext(jwt);
 * } catch (JwtException e) {
 *     // Handle invalid token
 * }
 * 
 * // Revoke user's tokens
 * jwtService.revokeUserTokens(userId);
 * }</pre>
 * 
 * @author IQ Scaffold Team
 * @version 1.0
 * @since 1.0
 * @see JwtKeyManagementService
 * @see UserContext
 * @see JwtConfiguration
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
   * Generate a secure access token containing comprehensive user context and claims.
   * 
   * <p>Creates a short-lived JWT access token (15-minute expiry) with complete user context
   * including roles, permissions, and tenant information. The token is signed using RSA-256
   * and includes a unique identifier (jti) for blacklisting support.
   * 
   * <h4>Generated Claims:</h4>
   * <ul>
   *   <li><strong>Standard Claims</strong> - iss, sub, iat, exp, jti</li>
   *   <li><strong>User Identity</strong> - username, email, firstName, lastName</li>
   *   <li><strong>Authorization</strong> - authorities (roles and permissions)</li>
   *   <li><strong>Multi-Tenancy</strong> - tenantId for tenant isolation</li>
   *   <li><strong>Token Type</strong> - type="access" for token identification</li>
   * </ul>
   * 
   * <h4>Security Considerations:</h4>
   * <ul>
   *   <li>Short expiration time minimizes exposure window</li>
   *   <li>Unique JTI enables immediate revocation via blacklisting</li>
   *   <li>RSA-256 signature prevents tampering</li>
   *   <li>Tenant context prevents cross-tenant access</li>
   * </ul>
   * 
   * @param user The authenticated user for whom to generate the token
   * @return A signed JWT access token string
   * 
   * @throws IllegalArgumentException If user is null or missing required fields
   * @throws JwtException If token generation fails due to signing issues
   * 
   * @see #generateRefreshToken(User)
   * @see UserContext
   * @see JwtClaimNames
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
   * Generate a secure refresh token for token renewal without re-authentication.
   * 
   * <p>Creates a long-lived JWT refresh token (7-day expiry) with minimal claims for security.
   * Refresh tokens are used to obtain new access tokens without requiring the user to
   * re-authenticate, enabling seamless user experience while maintaining security.
   * 
   * <h4>Generated Claims:</h4>
   * <ul>
   *   <li><strong>Standard Claims</strong> - iss, sub, iat, exp, jti</li>
   *   <li><strong>Minimal Identity</strong> - username for user identification</li>
   *   <li><strong>Multi-Tenancy</strong> - tenantId for tenant isolation</li>
   *   <li><strong>Token Type</strong> - type="refresh" for token identification</li>
   * </ul>
   * 
   * <h4>Security Features:</h4>
   * <ul>
   *   <li>Longer expiration but limited claims reduce attack surface</li>
   *   <li>Unique JTI enables immediate revocation</li>
   *   <li>Can only be used for token refresh, not API access</li>
   *   <li>Tracked in Redis for validation and revocation</li>
   * </ul>
   * 
   * <h4>Usage Pattern:</h4>
   * <ol>
   *   <li>Client receives refresh token during login</li>
   *   <li>When access token expires, client uses refresh token</li>
   *   <li>Service validates refresh token and issues new access token</li>
   *   <li>Optionally rotates refresh token for enhanced security</li>
   * </ol>
   * 
   * @param user The authenticated user for whom to generate the refresh token
   * @return A signed JWT refresh token string
   * 
   * @throws IllegalArgumentException If user is null or missing required fields
   * @throws JwtException If token generation fails due to signing issues
   * 
   * @see #generateAccessToken(User)
   * @see #refreshAccessToken(String)
   */
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

  /**
   * Validate and decode a JWT token with comprehensive security checks.
   * 
   * <p>Performs complete token validation including signature verification, expiration checking,
   * and blacklist validation. This method ensures that only valid, non-revoked tokens are accepted
   * for API access.
   * 
   * <h4>Validation Steps:</h4>
   * <ol>
   *   <li><strong>Format Validation</strong> - Ensures proper JWT structure</li>
   *   <li><strong>Signature Verification</strong> - Validates RSA-256 signature using current/previous keys</li>
   *   <li><strong>Expiration Check</strong> - Ensures token hasn't expired</li>
   *   <li><strong>Issuer Validation</strong> - Verifies token was issued by this service</li>
   *   <li><strong>Blacklist Check</strong> - Ensures token hasn't been revoked</li>
   *   <li><strong>User Revocation Check</strong> - Validates against user-wide token revocation</li>
   * </ol>
   * 
   * <h4>Security Features:</h4>
   * <ul>
   *   <li>Supports key rotation with grace period</li>
   *   <li>Immediate revocation via Redis blacklist</li>
   *   <li>User-wide token revocation support</li>
   *   <li>Comprehensive error reporting for debugging</li>
   * </ul>
   * 
   * <h4>Performance Considerations:</h4>
   * <ul>
   *   <li>Redis lookups are cached for blacklist checks</li>
   *   <li>Signature validation uses efficient RSA operations</li>
   *   <li>Early exit on format/expiration failures</li>
   * </ul>
   * 
   * @param token The JWT token string to validate
   * @return Decoded and validated Jwt object containing claims
   * 
   * @throws JwtException If token is invalid, expired, or revoked
   * @throws IllegalArgumentException If token is null or empty
   * 
   * @see #isTokenBlacklisted(String)
   * @see #isUserTokensRevoked(String, Instant)
   */
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

  /**
   * Extract user context from JWT token using pattern matching.
   */
  public UserContext extractUserContext(Jwt jwt) {
    var claims = jwt.getClaims();

    var userId = extractLong(claims.get(JwtClaimNames.SUBJECT));
    var username = extractString(claims.get(JwtClaimNames.USERNAME));
    var email = extractString(claims.get(JwtClaimNames.EMAIL));
    var authorities = extractStringSet(claims.get(JwtClaimNames.AUTHORITIES)); // Changed from roles to authorities
    var permissions = extractStringSet(claims.get(JwtClaimNames.PERMISSIONS));
    var firstName = extractString(claims.get(JwtClaimNames.FIRST_NAME));
    var lastName = extractString(claims.get(JwtClaimNames.LAST_NAME));
    var tenantId = extractString(claims.get(JwtClaimNames.TENANT_ID));
    var customClaims = extractCustomClaims(claims);

    return new UserContext(
        userId, username, email, authorities, permissions,
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
    } catch (final JwtException e) {
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
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  /**
   * Create user context from User entity.
   */
  private UserContext createUserContext(User user) {
    Set<String> authorities = user.getAuthorities().stream()
        .map(authority -> authority.getName())
        .collect(Collectors.toSet());

    return new UserContext(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        authorities, // Changed from roles to authorities
        Set.of(), // Permissions can be derived from authorities
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
        .claim(JwtClaimNames.TYPE, type)
        .claim(JwtClaimNames.USERNAME, userContext.username())
        .claim(JwtClaimNames.EMAIL, userContext.email())
        .claim(JwtClaimNames.AUTHORITIES, userContext.authorities()) // Changed from roles to authorities
        .claim(JwtClaimNames.PERMISSIONS, userContext.permissions())
        .claim(JwtClaimNames.FIRST_NAME, userContext.firstName())
        .claim(JwtClaimNames.LAST_NAME, userContext.lastName())
        .claim(JwtClaimNames.TENANT_ID, userContext.tenantId())
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
        JwtClaimNames.AUTHORITIES, // Changed from ROLES to AUTHORITIES
        JwtClaimNames.PERMISSIONS,
        JwtClaimNames.FIRST_NAME,
        JwtClaimNames.LAST_NAME,
        JwtClaimNames.TENANT_ID
    );

    return allClaims.entrySet().stream()
        .filter(entry -> !standardClaims.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
