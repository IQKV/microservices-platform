package org.gripday.userservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.userservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.userservice.domain.service.AccountLockoutService;
import org.gripday.userservice.domain.service.AuthenticationService;
import org.gripday.userservice.domain.service.JwtService;
import org.gripday.userservice.domain.service.SecurityAuditService;
import org.gripday.userservice.infrastructure.entity.Authority;
import org.gripday.userservice.infrastructure.entity.User;
import org.gripday.userservice.infrastructure.repository.UserRepository;
import org.gripday.userservice.presentation.dto.LoginRequest;
import org.gripday.userservice.presentation.dto.RefreshTokenRequest;
import org.gripday.userservice.presentation.validation.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for AuthenticationService focusing on happy path scenarios. Tests successful authentication with valid credentials and proper security measures.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @Mock
  private AccountLockoutService accountLockoutService;

  @Mock
  private SecurityAuditService securityAuditService;

  @Mock
  private InputSanitizer inputSanitizer;

  @Mock
  private TenantAwareSessionService sessionService;

  @Mock
  private MeterRegistry meterRegistry;

  private AuthenticationService authenticationService;
  private User testUser;

  @BeforeEach
  void setUp() {
    authenticationService = new AuthenticationService(
        userRepository, passwordEncoder, jwtService, accountLockoutService,
        securityAuditService, inputSanitizer, sessionService, meterRegistry
    );

    testUser = createTestUser(1L, "testuser", "test@example.com", "tenant-1");
    var userRole = new Authority("USER", "Standard user role");
    testUser.addAuthority(userRole);
  }

  @Test
  void authenticateUser_WithValidCredentials_ShouldSucceed() {
    // Given
    var request = new LoginRequest("testuser", "validPassword", false);
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Ensure user has verified email
    testUser.setEmailVerified(true);

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput("testuser")).thenReturn("testuser");
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(true);
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(false);

    // Mock account lockout check
    when(accountLockoutService.isAccountLocked("testuser")).thenReturn(false);

    // Mock user lookup
    when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
        .thenReturn(Optional.of(testUser));

    // Mock password verification
    when(passwordEncoder.matches("validPassword", testUser.getPasswordHash())).thenReturn(true);

    // Mock JWT generation
    when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

    // Mock session service
    doNothing().when(sessionService).storeSession(anyString(), any(), any(Duration.class));
    doNothing().when(sessionService).addUserSession(anyString(), anyString());

    // When
    var result = authenticationService.authenticateUser(request, ipAddress, userAgent);

    // Then
    assertNotNull(result);
    assertEquals("access-token", result.accessToken());
    assertEquals("refresh-token", result.refreshToken());
    assertEquals(900L, result.expiresIn()); // 15 minutes for non-remember-me
    assertNotNull(result.user());
    assertEquals("testuser", result.user().username());
    assertEquals("test@example.com", result.user().email());
    assertEquals("tenant-1", result.user().tenantId());
    assertTrue(result.user().hasRole("USER"));

    // Verify interactions
    verify(accountLockoutService).clearFailedAttempts("testuser");
    verify(securityAuditService).logSuccessfulAuthentication("testuser", ipAddress, userAgent);
    verify(securityAuditService).logTokenEvent("testuser", "generated", ipAddress, userAgent);
  }

  @Test
  void authenticateUser_WithUnverifiedEmail_ShouldThrowEmailVerificationRequiredException() {
    // Given
    var request = new LoginRequest("testuser", "validPassword", false);
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Ensure user has unverified email
    testUser.setEmailVerified(false);

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput("testuser")).thenReturn("testuser");
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(true);
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(false);

    // Mock account lockout check
    when(accountLockoutService.isAccountLocked("testuser")).thenReturn(false);

    // Mock user lookup
    when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
        .thenReturn(Optional.of(testUser));

    // Mock password verification
    when(passwordEncoder.matches("validPassword", testUser.getPasswordHash())).thenReturn(true);

    // When & Then
    var exception = assertThrows(AuthenticationService.EmailVerificationRequiredException.class, () -> {
      authenticationService.authenticateUser(request, ipAddress, userAgent);
    });

    assertEquals("Email verification required", exception.getMessage());

    // Verify security audit was logged
    verify(securityAuditService).logFailedAuthentication("testuser", "Email not verified", ipAddress, userAgent);

    // Verify no tokens were generated
    verify(jwtService, never()).generateAccessToken(any());
    verify(jwtService, never()).generateRefreshToken(any());
  }

  @Test
  void authenticateUser_WithNullEmailVerified_ShouldThrowEmailVerificationRequiredException() {
    // Given
    var request = new LoginRequest("testuser", "validPassword", false);
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Ensure user has null emailVerified (unverified)
    testUser.setEmailVerified(null);

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput("testuser")).thenReturn("testuser");
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(true);
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(false);

    // Mock account lockout check
    when(accountLockoutService.isAccountLocked("testuser")).thenReturn(false);

    // Mock user lookup
    when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
        .thenReturn(Optional.of(testUser));

    // Mock password verification
    when(passwordEncoder.matches("validPassword", testUser.getPasswordHash())).thenReturn(true);

    // When & Then
    var exception = assertThrows(AuthenticationService.EmailVerificationRequiredException.class, () -> {
      authenticationService.authenticateUser(request, ipAddress, userAgent);
    });

    assertEquals("Email verification required", exception.getMessage());

    // Verify security audit was logged
    verify(securityAuditService).logFailedAuthentication("testuser", "Email not verified", ipAddress, userAgent);
  }

  @Test
  void authenticateUser_WithRememberMe_ShouldReturnLongerExpiry() {
    // Given
    var request = new LoginRequest("testuser", "validPassword", true);
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Mock all dependencies for successful authentication
    setupSuccessfulAuthenticationMocks();

    // When
    var result = authenticationService.authenticateUser(request, ipAddress, userAgent);

    // Then
    assertNotNull(result);
    assertEquals(604800L, result.expiresIn()); // 7 days for remember-me

    // Verify session timeout is longer for remember-me
    verify(sessionService).storeSession(anyString(), any(), eq(Duration.ofDays(7)));
  }

  @Test
  void authenticateUser_WithEmailAsUsername_ShouldSucceed() {
    // Given
    var request = new LoginRequest("test@example.com", "validPassword", false);
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Ensure user has verified email
    testUser.setEmailVerified(true);

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput("test@example.com")).thenReturn("test@example.com");
    when(inputSanitizer.isInputSafe("test@example.com")).thenReturn(true);
    when(inputSanitizer.containsSqlInjection("test@example.com")).thenReturn(false);

    // Mock account lockout check
    when(accountLockoutService.isAccountLocked("test@example.com")).thenReturn(false);

    // Mock user lookup by email
    when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
        .thenReturn(Optional.of(testUser));

    // Mock password verification
    when(passwordEncoder.matches("validPassword", testUser.getPasswordHash())).thenReturn(true);

    // Mock JWT generation
    when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

    // Mock session service
    doNothing().when(sessionService).storeSession(anyString(), any(), any(Duration.class));
    doNothing().when(sessionService).addUserSession(anyString(), anyString());

    // When
    var result = authenticationService.authenticateUser(request, ipAddress, userAgent);

    // Then
    assertNotNull(result);
    assertEquals("testuser", result.user().username());
    assertEquals("test@example.com", result.user().email());

    // Verify user was found by email
    verify(userRepository).findByUsernameOrEmail("test@example.com", "test@example.com");
  }

  @Test
  void refreshToken_WithValidRefreshToken_ShouldSucceed() {
    // Given
    var request = new RefreshTokenRequest("valid-refresh-token");
    var mockJwt = createMockJwt("1", "refresh");

    // Mock JWT validation
    when(jwtService.validateToken("valid-refresh-token")).thenReturn(mockJwt);

    // Mock user lookup
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Mock new access token generation
    when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");

    // When
    var result = authenticationService.refreshToken(request);

    // Then
    assertNotNull(result);
    assertEquals("new-access-token", result.accessToken());
    assertEquals("valid-refresh-token", result.refreshToken()); // Same refresh token
    assertEquals(900L, result.expiresIn()); // 15 minutes
    assertNotNull(result.user());
    assertEquals("testuser", result.user().username());

    // Verify interactions
    verify(jwtService).validateToken("valid-refresh-token");
    verify(userRepository).findById(1L);
    verify(jwtService).generateAccessToken(testUser);
  }

  @Test
  void logoutUser_WithValidTokenAndSession_ShouldSucceed() {
    // Given
    var accessToken = "valid-access-token";
    var sessionId = "session-123";
    var mockJwt = createMockJwt("1", "access");

    // Mock JWT validation for session cleanup
    when(jwtService.validateToken(accessToken)).thenReturn(mockJwt);

    // When
    assertDoesNotThrow(() -> authenticationService.logoutUser(accessToken, sessionId));

    // Then
    verify(jwtService).invalidateToken(accessToken);
    verify(sessionService).deleteSession(sessionId);
    verify(sessionService).removeUserSession("1", sessionId);
  }

  @Test
  void validateAndExtendSession_WithValidSession_ShouldReturnTrue() {
    // Given
    var sessionId = "valid-session-123";

    // Mock session service
    when(sessionService.sessionExists(sessionId)).thenReturn(true);

    // When
    var result = authenticationService.validateAndExtendSession(sessionId);

    // Then
    assertTrue(result);
    verify(sessionService).sessionExists(sessionId);
    verify(sessionService).extendSession(sessionId, Duration.ofMinutes(30));
  }

  @Test
  void getUserActiveSessions_WithValidUserId_ShouldReturnSessions() {
    // Given
    var userId = 1L;
    var sessions = Set.of("session-1", "session-2");

    // Mock session service
    when(sessionService.getUserSessions("1")).thenReturn(Set.copyOf(sessions));

    // When
    var result = authenticationService.getUserActiveSessions(userId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains("session-1"));
    assertTrue(result.contains("session-2"));
    verify(sessionService).getUserSessions("1");
  }

  @Test
  void logoutAllUserSessions_WithValidUserId_ShouldSucceed() {
    // Given
    var userId = 1L;

    // Mock user lookup
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    // Mock session service
    doNothing().when(sessionService).invalidateAllUserSessions("1");

    // When
    assertDoesNotThrow(() -> authenticationService.logoutAllUserSessions(userId));

    // Then
    verify(sessionService).invalidateAllUserSessions("1");
    verify(securityAuditService).logTokenEvent("testuser", "logout_from_all_devices", "system", "system");
  }

  @Test
  void createAuthenticationSuccess_WithValidUser_ShouldReturnSuccessResult() {
    // Given
    var accessToken = "access-token";
    var refreshToken = "refresh-token";

    // When
    var result = authenticationService.createAuthenticationSuccess(testUser, accessToken, refreshToken);

    // Then
    assertNotNull(result);
    assertEquals(accessToken, result.accessToken());
    assertEquals(refreshToken, result.refreshToken());
    assertNotNull(result.user());
    assertEquals("testuser", result.user().username());
    assertNotNull(result.timestamp());
  }

  @Test
  void createAuthenticationFailure_WithValidReason_ShouldReturnFailureResult() {
    // Given
    var reason = "Invalid credentials";
    var errorCode = "AUTH_INVALID_CREDENTIALS";

    // When
    var result = authenticationService.createAuthenticationFailure(reason, errorCode);

    // Then
    assertNotNull(result);
    assertEquals(reason, result.reason());
    assertEquals(errorCode, result.errorCode());
    assertNotNull(result.timestamp());
  }

  @Test
  void changePassword_WithValidCurrentPassword_ShouldSucceed() {
    // Given
    var userId = 1L;
    var currentPassword = "oldPassword123";
    var newPassword = "newPassword456";
    var clientIp = "192.168.1.1";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(currentPassword)).thenReturn(currentPassword);
    when(inputSanitizer.sanitizeInput(newPassword)).thenReturn(newPassword);

    // Mock user lookup
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    // Mock password verification - current password matches
    when(passwordEncoder.matches(currentPassword, testUser.getPasswordHash())).thenReturn(true);

    // Mock password verification - new password is different
    when(passwordEncoder.matches(newPassword, testUser.getPasswordHash())).thenReturn(false);

    // Mock password encoding
    when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

    // Mock user save
    when(userRepository.save(testUser)).thenReturn(testUser);

    // Mock JWT service
    doNothing().when(jwtService).revokeAllRefreshTokensForUser("1");

    // Mock session service
    doNothing().when(sessionService).invalidateAllUserSessions("1");

    // Mock metrics
    var counter = org.mockito.Mockito.mock(io.micrometer.core.instrument.Counter.class);
    when(meterRegistry.counter("auth.password.changed")).thenReturn(counter);

    // When
    assertDoesNotThrow(() -> authenticationService.changePassword(userId, currentPassword, newPassword, clientIp));

    // Then
    verify(userRepository).save(testUser);
    verify(jwtService).revokeAllRefreshTokensForUser("1");
    verify(sessionService).invalidateAllUserSessions("1");
    verify(securityAuditService).logTokenEvent("testuser", "password_changed", clientIp, "web");
  }

  @Test
  void changePassword_WithInvalidCurrentPassword_ShouldThrowException() {
    // Given
    var userId = 1L;
    var currentPassword = "wrongPassword";
    var newPassword = "newPassword456";
    var clientIp = "192.168.1.1";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(currentPassword)).thenReturn(currentPassword);
    when(inputSanitizer.sanitizeInput(newPassword)).thenReturn(newPassword);

    // Mock user lookup
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    // Mock password verification - current password does not match
    when(passwordEncoder.matches(currentPassword, testUser.getPasswordHash())).thenReturn(false);

    // When & Then
    var exception = assertThrows(AuthenticationService.AuthenticationException.class, () -> {
      authenticationService.changePassword(userId, currentPassword, newPassword, clientIp);
    });

    assertEquals("Current password is incorrect", exception.getMessage());

    // Verify security audit was logged
    verify(securityAuditService).logFailedAuthentication(
        "testuser", "Invalid current password during password change", clientIp, "web");
  }

  private void setupSuccessfulAuthenticationMocks() {
    // Ensure user has verified email
    testUser.setEmailVerified(true);

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput("testuser")).thenReturn("testuser");
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(true);
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(false);

    // Mock account lockout check
    when(accountLockoutService.isAccountLocked("testuser")).thenReturn(false);

    // Mock user lookup
    when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
        .thenReturn(Optional.of(testUser));

    // Mock password verification
    when(passwordEncoder.matches("validPassword", testUser.getPasswordHash())).thenReturn(true);

    // Mock JWT generation
    when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

    // Mock session service
    doNothing().when(sessionService).storeSession(anyString(), any(), any(Duration.class));
    doNothing().when(sessionService).addUserSession(anyString(), anyString());
  }

  private Jwt createMockJwt(String subject, String tokenType) {
    var headers = Map.<String, Object>of("alg", "HS256", "typ", "JWT");
    var claims = Map.<String, Object>of(
        "sub", subject,
        "type", tokenType,
        "iat", Instant.now().getEpochSecond(),
        "exp", Instant.now().plusSeconds(3600).getEpochSecond()
    );

    return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
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