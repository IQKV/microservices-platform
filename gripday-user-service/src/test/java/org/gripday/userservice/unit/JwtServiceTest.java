package org.gripday.userservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.gripday.userservice.domain.service.JwtService;
import org.gripday.userservice.infrastructure.entity.Authority;
import org.gripday.userservice.infrastructure.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/**
 * Unit tests for JwtService focusing on happy path scenarios. Tests JWT token generation and validation with valid user context and tenant claims.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  @Mock
  private JwtEncoder jwtEncoder;

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private org.gripday.userservice.config.JwtConfiguration jwtConfiguration;

  @Mock
  private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

  private JwtService jwtService;
  private User testUser;

  @BeforeEach
  void setUp() {
    // Mock JWT configuration with lenient to avoid unnecessary stubbing errors
    org.mockito.Mockito.lenient().when(jwtConfiguration.getAccessTokenExpiry()).thenReturn(java.time.Duration.ofMinutes(15));
    org.mockito.Mockito.lenient().when(jwtConfiguration.getRefreshTokenExpiry()).thenReturn(java.time.Duration.ofDays(7));
    org.mockito.Mockito.lenient().when(jwtConfiguration.getIssuer()).thenReturn("gripday-user-service");

    jwtService = new JwtService(jwtEncoder, jwtDecoder, jwtConfiguration, redisTemplate);
    testUser = createTestUser(1L, "testuser", "test@example.com", "tenant-1");

    // Add authorities to test user
    var userRole = new Authority("USER", "Standard user role");
    var adminRole = new Authority("ADMIN", "Administrator role");
    testUser.addAuthority(userRole);
    testUser.addAuthority(adminRole);
  }

  @Test
  void generateAccessToken_WithValidUser_ShouldContainUserContext() {
    // Given
    var expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access.token";

    // Mock JWT encoder
    when(jwtEncoder.encode(any())).thenReturn(createMockJwt(expectedToken, "access"));

    // When
    var token = jwtService.generateAccessToken(testUser);

    // Then
    assertNotNull(token);
    assertEquals(expectedToken, token);

    // Verify encoder was called with correct claims
    verify(jwtEncoder).encode(argThat(encoderParameters -> {
      var claims = encoderParameters.getClaims();
      return claims.getSubject().equals("1")
          && claims.getClaim("username").equals("testuser")
          && claims.getClaim("email").equals("test@example.com")
          && claims.getClaim("tenantId").equals("tenant-1")
          && claims.getClaim("type").equals("access");
    }));
  }

  @Test
  void generateRefreshToken_WithValidUser_ShouldContainBasicClaims() {
    // Given
    var expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.token";

    // Mock JWT encoder
    when(jwtEncoder.encode(any())).thenReturn(createMockJwt(expectedToken, "refresh"));

    // When
    var token = jwtService.generateRefreshToken(testUser);

    // Then
    assertNotNull(token);
    assertEquals(expectedToken, token);

    // Verify encoder was called with correct claims for refresh token
    verify(jwtEncoder).encode(argThat(encoderParameters -> {
      var claims = encoderParameters.getClaims();
      return claims.getSubject().equals("1")
          && claims.getClaim("username").equals("testuser")
          && claims.getClaim("tenantId").equals("tenant-1")
          && claims.getClaim("type").equals("refresh");
    }));
  }

  @Test
  void validateToken_WithValidJwtToken_ShouldReturnJwt() {
    // Given
    var tokenString = "valid.jwt.token";
    var mockJwt = createMockJwtWithClaims();

    // Mock JWT decoder and Redis template with lenient
    org.mockito.Mockito.lenient().when(jwtDecoder.decode(tokenString)).thenReturn(mockJwt);
    org.mockito.Mockito.lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);

    // When
    var jwt = jwtService.validateToken(tokenString);

    // Then
    assertNotNull(jwt);
    assertEquals("1", jwt.getSubject());
    assertEquals("testuser", jwt.getClaimAsString("username"));
    assertEquals("test@example.com", jwt.getClaimAsString("email"));
    assertEquals("tenant-1", jwt.getClaimAsString("tenantId"));
    assertEquals("access", jwt.getClaimAsString("type"));

    verify(jwtDecoder).decode(tokenString);
  }

  @Test
  void extractUserContext_FromValidJwt_ShouldReturnCompleteContext() {
    // Given
    var mockJwt = createMockJwtWithClaims();

    // When
    var userContext = jwtService.extractUserContext(mockJwt);

    // Then
    assertNotNull(userContext);
    assertEquals(1L, userContext.userId());
    assertEquals("testuser", userContext.username());
    assertEquals("test@example.com", userContext.email());
    assertEquals("tenant-1", userContext.tenantId());
    assertEquals("John", userContext.firstName());
    assertEquals("Doe", userContext.lastName());
    assertTrue(userContext.roles().contains("USER"));
    assertTrue(userContext.roles().contains("ADMIN"));
  }

  @Test
  void generateTokenWithTenantClaims_ShouldIncludeTenantContext() {
    // Given
    var expectedToken = "tenant.aware.token";

    // Mock JWT encoder
    when(jwtEncoder.encode(any())).thenReturn(createMockJwt(expectedToken, "access"));

    // When
    var token = jwtService.generateAccessToken(testUser);

    // Then
    assertNotNull(token);

    // Verify tenant-specific claims are included
    verify(jwtEncoder).encode(argThat(encoderParameters -> {
      var claims = encoderParameters.getClaims();
      return claims.getClaim("tenantId").equals("tenant-1")
          && claims.getClaim("username").equals("testuser")
          && claims.getClaim("email").equals("test@example.com");
    }));
  }

  @Test
  void generateTokenWithRoles_ShouldIncludeUserRoles() {
    // Given
    var expectedToken = "role.aware.token";

    // Mock JWT encoder
    when(jwtEncoder.encode(any())).thenReturn(createMockJwt(expectedToken, "access"));

    // When
    var token = jwtService.generateAccessToken(testUser);

    // Then
    assertNotNull(token);

    // Verify roles are included in claims
    verify(jwtEncoder).encode(argThat(encoderParameters -> {
      var claims = encoderParameters.getClaims();
      var roles = (Set<?>) claims.getClaim("roles");
      return roles != null
          && roles.contains("USER")
          && roles.contains("ADMIN");
    }));
  }

  @Test
  void validateTokenExpiration_WithValidToken_ShouldNotThrow() {
    // Given
    var tokenString = "valid.unexpired.token";
    var futureExpiry = Instant.now().plusSeconds(3600); // 1 hour from now
    var mockJwt = createMockJwtWithExpiry(futureExpiry);

    // Mock JWT decoder
    when(jwtDecoder.decode(tokenString)).thenReturn(mockJwt);

    // When & Then
    assertDoesNotThrow(() -> {
      var jwt = jwtService.validateToken(tokenString);
      assertNotNull(jwt);
      assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
    });
  }

  @Test
  void extractTenantFromToken_WithValidJwt_ShouldReturnTenantId() {
    // Given
    var mockJwt = createMockJwtWithClaims();

    // When
    var userContext = jwtService.extractUserContext(mockJwt);

    // Then
    assertEquals("tenant-1", userContext.tenantId());
  }

  @Test
  void extractUserIdFromToken_WithValidJwt_ShouldReturnUserId() {
    // Given
    var mockJwt = createMockJwtWithClaims();

    // When
    var userContext = jwtService.extractUserContext(mockJwt);

    // Then
    assertEquals(1L, userContext.userId());
  }

  @Test
  void validateToken_WithValidToken_ShouldReturnJwt() {
    // Given
    var tokenString = "valid.token";
    var mockJwt = createMockJwtWithClaims();

    // Mock JWT decoder and Redis template with lenient
    org.mockito.Mockito.lenient().when(jwtDecoder.decode(tokenString)).thenReturn(mockJwt);
    org.mockito.Mockito.lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);

    // When
    var jwt = jwtService.validateToken(tokenString);

    // Then
    assertNotNull(jwt);
    verify(jwtDecoder).decode(tokenString);
  }

  private Jwt createMockJwt(String tokenValue, String tokenType) {
    var headers = Map.<String, Object>of("alg", "HS256", "typ", "JWT");
    var claims = Map.<String, Object>of(
        "sub", "1",
        "type", tokenType,
        "iat", Instant.now().getEpochSecond(),
        "exp", Instant.now().plusSeconds(3600).getEpochSecond()
    );

    return new Jwt(tokenValue, Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
  }

  private Jwt createMockJwtWithClaims() {
    var headers = Map.<String, Object>of("alg", "HS256", "typ", "JWT");

    // Use HashMap for claims with more than 10 entries
    var claims = new java.util.HashMap<String, Object>();
    claims.put("sub", "1");
    claims.put("username", "testuser");
    claims.put("email", "test@example.com");
    claims.put("tenantId", "tenant-1");
    claims.put("firstName", "John");
    claims.put("lastName", "Doe");
    claims.put("roles", Set.of("USER", "ADMIN"));
    claims.put("permissions", Set.of("READ_PROFILE", "WRITE_PROFILE"));
    claims.put("type", "access");
    claims.put("iat", Instant.now().getEpochSecond());
    claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());

    return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
  }

  private Jwt createMockJwtWithExpiry(Instant expiry) {
    var headers = Map.<String, Object>of("alg", "HS256", "typ", "JWT");
    var claims = Map.<String, Object>of(
        "sub", "1",
        "username", "testuser",
        "type", "access",
        "iat", Instant.now().getEpochSecond(),
        "exp", expiry.getEpochSecond()
    );

    return new Jwt("token-value", Instant.now(), expiry, headers, claims);
  }

  private User createTestUser(Long id, String username, String email, String tenantId) {
    var user = new User(username, email, "hashedPassword", "John", "Doe", tenantId);

    // Use reflection to set the ID for testing
    try {
      var idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (final Exception e) {
      // Log exception for debugging test failures
      System.err.println("Failed to set user ID via reflection: " + e.getMessage());
    }

    return user;
  }
}