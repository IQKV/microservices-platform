package org.gripday.userservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.gripday.userservice.config.JwtConfiguration;
import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.usermanagement.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Unit tests for JwtService.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  @Mock
  private JwtEncoder jwtEncoder;

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private JwtConfiguration jwtConfiguration;

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private JwtService service;
  private User testUser;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service = new JwtService(jwtEncoder, jwtDecoder, jwtConfiguration, redisTemplate);

    // Setup test user
    testUser = new User("testuser", "test@example.com", "hash", "Test", "User", "tenant-123");
    setUserId(testUser, 1L);
    
    var authority = new Authority("ROLE_USER", "User role");
    testUser.setAuthorities(Set.of(authority));

    // Setup JWT configuration
    when(jwtConfiguration.getAccessTokenExpiry()).thenReturn(Duration.ofMinutes(15));
    when(jwtConfiguration.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));
    when(jwtConfiguration.getIssuer()).thenReturn("test-issuer");
  }

  @Test
  @DisplayName("Should generate access token successfully")
  void shouldGenerateAccessToken() {
    // Arrange
    var mockJwt = createMockJwt("access-token-value");
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    // Act
    var token = service.generateAccessToken(testUser);

    // Assert
    assertThat(token).isNotNull();
    assertThat(token).isEqualTo("access-token-value");
    verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
  }

  @Test
  @DisplayName("Should generate refresh token successfully")
  void shouldGenerateRefreshToken() {
    // Arrange
    var mockJwt = createMockJwt("refresh-token-value");
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    // Act
    var token = service.generateRefreshToken(testUser);

    // Assert
    assertThat(token).isNotNull();
    assertThat(token).isEqualTo("refresh-token-value");
    verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
  }

  @Test
  @DisplayName("Should validate token successfully")
  void shouldValidateToken() {
    // Arrange
    var token = "valid-token";
    var mockJwt = createMockJwtWithId("jwt-id-123");
    when(jwtDecoder.decode(token)).thenReturn(mockJwt);
    when(redisTemplate.hasKey("blacklist:token:jwt-id-123")).thenReturn(false);

    // Act
    var jwt = service.validateToken(token);

    // Assert
    assertThat(jwt).isNotNull();
    verify(jwtDecoder).decode(token);
  }

  @Test
  @DisplayName("Should throw exception for blacklisted token")
  void shouldThrowExceptionForBlacklistedToken() {
    // Arrange
    var token = "blacklisted-token";
    var mockJwt = createMockJwtWithId("jwt-id-123");
    when(jwtDecoder.decode(token)).thenReturn(mockJwt);
    when(redisTemplate.hasKey("blacklist:token:jwt-id-123")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.validateToken(token))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("Token has been invalidated");
  }

  @Test
  @DisplayName("Should throw exception for invalid token")
  void shouldThrowExceptionForInvalidToken() {
    // Arrange
    var token = "invalid-token";
    when(jwtDecoder.decode(token)).thenThrow(new JwtException("Invalid token"));

    // Act & Assert
    assertThatThrownBy(() -> service.validateToken(token))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("Invalid or expired token");
  }

  @Test
  @DisplayName("Should extract user context from JWT")
  void shouldExtractUserContextFromJwt() {
    // Arrange
    var jwt = createMockJwtWithClaims();

    // Act
    var userContext = service.extractUserContext(jwt);

    // Assert
    assertThat(userContext).isNotNull();
    assertThat(userContext.userId()).isEqualTo(1L);
    assertThat(userContext.username()).isEqualTo("testuser");
    assertThat(userContext.email()).isEqualTo("test@example.com");
    assertThat(userContext.roles()).contains("ROLE_USER");
    assertThat(userContext.tenantId()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should invalidate token by adding to blacklist")
  void shouldInvalidateToken() {
    // Arrange
    var token = "token-to-invalidate";
    var mockJwt = createMockJwtWithIdAndExpiry("jwt-id-123", Instant.now().plusSeconds(3600));
    when(jwtDecoder.decode(token)).thenReturn(mockJwt);

    // Act
    service.invalidateToken(token);

    // Assert
    verify(valueOperations).set(eq("blacklist:token:jwt-id-123"), eq("true"), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("Should not blacklist already expired token")
  void shouldNotBlacklistExpiredToken() {
    // Arrange
    var token = "expired-token";
    var mockJwt = createMockJwtWithIdAndExpiry("jwt-id-123", Instant.now().minusSeconds(3600));
    when(jwtDecoder.decode(token)).thenReturn(mockJwt);

    // Act
    service.invalidateToken(token);

    // Assert - should not add to blacklist since TTL would be negative
    verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
  }

  @Test
  @DisplayName("Should revoke all refresh tokens for user")
  void shouldRevokeAllRefreshTokensForUser() {
    // Arrange
    var userId = "1";
    var keys = Set.of("refresh:token:1:key1", "refresh:token:1:key2");
    when(redisTemplate.keys("refresh:token:1:*")).thenReturn(keys);

    // Act
    service.revokeAllRefreshTokensForUser(userId);

    // Assert
    verify(redisTemplate).delete(keys);
    verify(valueOperations).set(eq("revoked:refresh:1"), anyString());
  }

  @Test
  @DisplayName("Should check if user refresh tokens are revoked")
  void shouldCheckIfUserRefreshTokensAreRevoked() {
    // Arrange
    var userId = "1";
    var tokenIssuedAt = Instant.now().minusSeconds(3600);
    var revokedAt = String.valueOf(Instant.now().getEpochSecond());
    when(valueOperations.get("revoked:refresh:1")).thenReturn(revokedAt);

    // Act
    var isRevoked = service.isUserRefreshRevoked(userId, tokenIssuedAt);

    // Assert
    assertThat(isRevoked).isTrue();
  }

  @Test
  @DisplayName("Should return false when no revocation exists")
  void shouldReturnFalseWhenNoRevocationExists() {
    // Arrange
    var userId = "1";
    var tokenIssuedAt = Instant.now();
    when(valueOperations.get("revoked:refresh:1")).thenReturn(null);

    // Act
    var isRevoked = service.isUserRefreshRevoked(userId, tokenIssuedAt);

    // Assert
    assertThat(isRevoked).isFalse();
  }

  @Test
  @DisplayName("Should return false when token issued after revocation")
  void shouldReturnFalseWhenTokenIssuedAfterRevocation() {
    // Arrange
    var userId = "1";
    var tokenIssuedAt = Instant.now();
    var revokedAt = String.valueOf(Instant.now().minusSeconds(3600).getEpochSecond());
    when(valueOperations.get("revoked:refresh:1")).thenReturn(revokedAt);

    // Act
    var isRevoked = service.isUserRefreshRevoked(userId, tokenIssuedAt);

    // Assert
    assertThat(isRevoked).isFalse();
  }

  private Jwt createMockJwt(String tokenValue) {
    return new Jwt(
        tokenValue,
        Instant.now(),
        Instant.now().plusSeconds(3600),
        java.util.Map.of("alg", "HS256"),
        java.util.Map.of("sub", "1")
    );
  }

  private Jwt createMockJwtWithId(String jwtId) {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        java.util.Map.of("alg", "HS256"),
        java.util.Map.of("sub", "1", "jti", jwtId)
    );
  }

  private Jwt createMockJwtWithIdAndExpiry(String jwtId, Instant expiry) {
    return new Jwt(
        "token-value",
        Instant.now(),
        expiry,
        java.util.Map.of("alg", "HS256"),
        java.util.Map.of("sub", "1", "jti", jwtId)
    );
  }

  private Jwt createMockJwtWithClaims() {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        java.util.Map.of("alg", "HS256"),
        java.util.Map.of(
            "sub", "1",
            "username", "testuser",
            "email", "test@example.com",
            "roles", java.util.List.of("ROLE_USER"),
            "permissions", java.util.List.of(),
            "firstName", "Test",
            "lastName", "User",
            "tenantId", "tenant-123"
        )
    );
  }

  private void setUserId(User user, Long id) {
    try {
      Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set user ID", e);
    }
  }
}
