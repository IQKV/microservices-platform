package org.gripday.userservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.userservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.userservice.security.AccountLockoutService;
import org.gripday.userservice.security.InputSanitizer;
import org.gripday.userservice.security.SecurityAuditService;
import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.usermanagement.User;
import org.gripday.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Unit tests for AuthenticationService.
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

  @Mock
  private Counter counter;

  private AuthenticationService service;
  private User testUser;
  private LoginRequest loginRequest;

  @BeforeEach
  void setUp() {
    service = new AuthenticationService(
        userRepository,
        passwordEncoder,
        jwtService,
        accountLockoutService,
        securityAuditService,
        inputSanitizer,
        sessionService,
        meterRegistry
    );

    // Setup test user
    testUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    setUserId(testUser, 1L);
    testUser.setEnabled(true);
    testUser.setEmailVerified(true);
    
    var authority = new Authority("ROLE_USER", "User role");
    testUser.setAuthorities(Set.of(authority));

    // Setup login request
    loginRequest = new LoginRequest("testuser", "password123", false);

    // Setup default mocks
    when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
    when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
    when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
    when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
    when(meterRegistry.counter(anyString())).thenReturn(counter);
  }

  @Test
  @DisplayName("Should authenticate user successfully")
  void shouldAuthenticateUserSuccessfully() {
    // Arrange
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
    when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

    // Act
    var response = service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.expiresIn()).isEqualTo(900L);
    assertThat(response.user()).isNotNull();
    assertThat(response.user().username()).isEqualTo("testuser");
    
    verify(accountLockoutService).clearFailedAttempts("testuser");
    verify(securityAuditService).logSuccessfulAuthentication("testuser", "127.0.0.1", "Mozilla/5.0");
    verify(sessionService).storeSession(anyString(), any(), any(Duration.class));
  }

  @Test
  @DisplayName("Should authenticate with remember me option")
  void shouldAuthenticateWithRememberMe() {
    // Arrange
    var rememberMeRequest = new LoginRequest("testuser", "password123", true);
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
    when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

    // Act
    var response = service.authenticateUser(rememberMeRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    assertThat(response.expiresIn()).isEqualTo(604800L); // 7 days
  }

  @Test
  @DisplayName("Should throw exception for suspicious input")
  void shouldThrowExceptionForSuspiciousInput() {
    // Arrange
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Invalid input detected");

    verify(securityAuditService).logSuspiciousActivity(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Should throw exception for SQL injection attempt")
  void shouldThrowExceptionForSqlInjection() {
    // Arrange
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Invalid input detected");
  }

  @Test
  @DisplayName("Should throw exception for locked account")
  void shouldThrowExceptionForLockedAccount() {
    // Arrange
    when(accountLockoutService.isAccountLocked("testuser")).thenReturn(true);
    when(accountLockoutService.getTimeUntilUnlock("testuser")).thenReturn(Duration.ofMinutes(15));

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AccountLockedException.class)
        .hasMessageContaining("Account is locked");

    verify(securityAuditService).logFailedAuthentication("testuser", "Account locked", "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should throw exception for non-existent user")
  void shouldThrowExceptionForNonExistentUser() {
    // Arrange
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Invalid username or password");

    verify(accountLockoutService).recordFailedAttempt("testuser");
    verify(securityAuditService).logFailedAuthentication("testuser", "User not found", "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should throw exception for invalid password")
  void shouldThrowExceptionForInvalidPassword() {
    // Arrange
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);
    when(accountLockoutService.recordFailedAttempt("testuser")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Invalid username or password");

    verify(accountLockoutService).recordFailedAttempt("testuser");
    verify(securityAuditService).logFailedAuthentication("testuser", "Invalid password", "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should lock account after too many failed attempts")
  void shouldLockAccountAfterTooManyFailedAttempts() {
    // Arrange
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);
    when(accountLockoutService.recordFailedAttempt("testuser")).thenReturn(true);
    when(accountLockoutService.getFailedAttempts("testuser")).thenReturn(5);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class);

    verify(securityAuditService).logAccountLockout("testuser", 5, "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should throw exception for disabled account")
  void shouldThrowExceptionForDisabledAccount() {
    // Arrange
    testUser.setEnabled(false);
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Account is disabled");

    verify(securityAuditService).logFailedAuthentication("testuser", "Account disabled", "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should throw exception for unverified email")
  void shouldThrowExceptionForUnverifiedEmail() {
    // Arrange
    testUser.setEmailVerified(false);
    when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(AuthenticationService.EmailVerificationRequiredException.class)
        .hasMessageContaining("Email verification required");

    verify(securityAuditService).logFailedAuthentication("testuser", "Email not verified", "127.0.0.1", "Mozilla/5.0");
  }

  @Test
  @DisplayName("Should change password successfully")
  void shouldChangePasswordSuccessfully() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);
    when(passwordEncoder.matches("newPassword", "hashed-password")).thenReturn(false);
    when(passwordEncoder.encode("newPassword")).thenReturn("new-hashed-password");

    // Act
    service.changePassword(1L, "oldPassword", "newPassword", "127.0.0.1");

    // Assert
    verify(userRepository).save(testUser);
    verify(jwtService).revokeAllRefreshTokensForUser("1");
    verify(sessionService).invalidateAllUserSessions("1");
    verify(securityAuditService).logTokenEvent("testuser", "password_changed", "127.0.0.1", "web");
  }

  @Test
  @DisplayName("Should throw exception when changing password with invalid current password")
  void shouldThrowExceptionForInvalidCurrentPassword() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("wrongPassword", "hashed-password")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.changePassword(1L, "wrongPassword", "newPassword", "127.0.0.1"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Current password is incorrect");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when new password is too short")
  void shouldThrowExceptionForShortPassword() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.changePassword(1L, "oldPassword", "short", "127.0.0.1"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("does not meet minimum requirements");
  }

  @Test
  @DisplayName("Should throw exception when new password same as current")
  void shouldThrowExceptionForSamePassword() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.changePassword(1L, "oldPassword", "oldPassword", "127.0.0.1"))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("must be different from current password");
  }

  @Test
  @DisplayName("Should refresh token successfully")
  void shouldRefreshTokenSuccessfully() {
    // Arrange
    var refreshRequest = new RefreshTokenRequest("refresh-token");
    var mockJwt = createMockJwt("1", "refresh", Instant.now());
    
    when(jwtService.validateToken("refresh-token")).thenReturn(mockJwt);
    when(jwtService.isUserRefreshRevoked("1", mockJwt.getIssuedAt())).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");

    // Act
    var response = service.refreshToken(refreshRequest);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.expiresIn()).isEqualTo(900L);
  }

  @Test
  @DisplayName("Should throw exception for invalid token type")
  void shouldThrowExceptionForInvalidTokenType() {
    // Arrange
    var refreshRequest = new RefreshTokenRequest("access-token");
    var mockJwt = createMockJwt("1", "access", Instant.now());
    
    when(jwtService.validateToken("access-token")).thenReturn(mockJwt);

    // Act & Assert
    assertThatThrownBy(() -> service.refreshToken(refreshRequest))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Invalid token type");
  }

  @Test
  @DisplayName("Should throw exception for revoked refresh token")
  void shouldThrowExceptionForRevokedRefreshToken() {
    // Arrange
    var refreshRequest = new RefreshTokenRequest("refresh-token");
    var mockJwt = createMockJwt("1", "refresh", Instant.now());
    
    when(jwtService.validateToken("refresh-token")).thenReturn(mockJwt);
    when(jwtService.isUserRefreshRevoked("1", mockJwt.getIssuedAt())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.refreshToken(refreshRequest))
        .isInstanceOf(AuthenticationService.AuthenticationException.class)
        .hasMessageContaining("Refresh tokens have been revoked");
  }

  @Test
  @DisplayName("Should logout user successfully")
  void shouldLogoutUserSuccessfully() {
    // Arrange
    var accessToken = "access-token";
    var sessionId = "session-123";
    var mockJwt = createMockJwt("1", "access", Instant.now());
    
    when(jwtService.validateToken(accessToken)).thenReturn(mockJwt);

    // Act
    service.logoutUser(accessToken, sessionId);

    // Assert
    verify(jwtService).invalidateToken(accessToken);
    verify(sessionService).deleteSession(sessionId);
    verify(sessionService).removeUserSession("1", sessionId);
  }

  @Test
  @DisplayName("Should logout from all devices")
  void shouldLogoutFromAllDevices() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    service.logoutFromAllDevices(1L);

    // Assert
    verify(jwtService).revokeAllRefreshTokensForUser("1");
    verify(sessionService).invalidateAllUserSessions("1");
    verify(securityAuditService).logTokenEvent("testuser", "logout_from_all_devices", "user", "web");
  }

  @Test
  @DisplayName("Should validate and extend session")
  void shouldValidateAndExtendSession() {
    // Arrange
    var sessionId = "session-123";
    when(sessionService.sessionExists(sessionId)).thenReturn(true);

    // Act
    var isValid = service.validateAndExtendSession(sessionId);

    // Assert
    assertThat(isValid).isTrue();
    verify(sessionService).extendSession(eq(sessionId), any(Duration.class));
  }

  @Test
  @DisplayName("Should return false for invalid session")
  void shouldReturnFalseForInvalidSession() {
    // Arrange
    var sessionId = "invalid-session";
    when(sessionService.sessionExists(sessionId)).thenReturn(false);

    // Act
    var isValid = service.validateAndExtendSession(sessionId);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should get user active sessions")
  void shouldGetUserActiveSessions() {
    // Arrange
    var sessions = Set.of("session-1", "session-2");
    when(sessionService.getUserSessions("1")).thenReturn(Set.copyOf(sessions));

    // Act
    var result = service.getUserActiveSessions(1L);

    // Assert
    assertThat(result).hasSize(2);
  }

  private Jwt createMockJwt(String userId, String type, Instant issuedAt) {
    return new Jwt(
        "token-value",
        issuedAt,
        issuedAt.plusSeconds(3600),
        java.util.Map.of("alg", "HS256"),
        java.util.Map.of("sub", userId, "type", type)
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
