package org.gripday.userservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Comprehensive unit tests for AuthenticationService.
 * Tests cover authentication, password management, token operations, and session handling.
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

    // Setup test user with all required fields
    testUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    setUserId(testUser, 1L);
    testUser.setEnabled(true);
    testUser.setEmailVerified(true);
    
    var authority = new Authority("ROLE_USER", "User role");
    testUser.setAuthorities(Set.of(authority));

    // Setup login request
    loginRequest = new LoginRequest("testuser", "password123", false);
  }

  @Nested
  @DisplayName("Authentication Tests")
  class AuthenticationTests {

    @Test
    @DisplayName("Should authenticate user successfully with all security checks")
    void shouldAuthenticateUserSuccessfully() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
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
      assertThat(response.sessionId()).isNotNull();
      assertThat(response.user()).isNotNull();
      assertThat(response.user().username()).isEqualTo("testuser");
      assertThat(response.user().email()).isEqualTo("test@example.com");
      assertThat(response.user().tenantId()).isEqualTo("tenant-123");
      
      // Verify security operations
      verify(inputSanitizer).sanitizeInput("testuser");
      verify(inputSanitizer).isInputSafe("testuser");
      verify(inputSanitizer).containsSqlInjection("testuser");
      verify(accountLockoutService).isAccountLocked("testuser");
      verify(accountLockoutService).clearFailedAttempts("testuser");
      verify(securityAuditService).logSuccessfulAuthentication("testuser", "127.0.0.1", "Mozilla/5.0");
      verify(securityAuditService).logTokenEvent("testuser", "generated", "127.0.0.1", "Mozilla/5.0");
      verify(sessionService).storeSession(anyString(), any(), any(Duration.class));
      verify(sessionService).addUserSession(eq("1"), anyString());
    }

    @Test
    @DisplayName("Should authenticate with remember me option and extended session")
    void shouldAuthenticateWithRememberMe() {
      // Arrange
      var rememberMeRequest = new LoginRequest("testuser", "password123", true);
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
      when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
      when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

      // Act
      var response = service.authenticateUser(rememberMeRequest, "127.0.0.1", "Mozilla/5.0");

      // Assert
      assertThat(response.expiresIn()).isEqualTo(604800L); // 7 days
      
      // Verify session timeout is extended for remember me
      var durationCaptor = ArgumentCaptor.forClass(Duration.class);
      verify(sessionService).storeSession(anyString(), any(), durationCaptor.capture());
      assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("Should throw exception for suspicious input and log security event")
    void shouldThrowExceptionForSuspiciousInput() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe("testuser")).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Invalid input detected");

      verify(securityAuditService).logSuspiciousActivity(
          eq("testuser"), 
          eq("Potential injection attempt in username"), 
          eq("127.0.0.1"), 
          eq("Mozilla/5.0")
      );
      verifyNoInteractions(userRepository);
      verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Should throw exception for SQL injection attempt and log security event")
    void shouldThrowExceptionForSqlInjection() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Invalid input detected");

      verify(securityAuditService).logSuspiciousActivity(anyString(), anyString(), anyString(), anyString());
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception for locked account with time remaining")
    void shouldThrowExceptionForLockedAccount() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked("testuser")).thenReturn(true);
      when(accountLockoutService.getTimeUntilUnlock("testuser")).thenReturn(Duration.ofMinutes(15));

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AccountLockedException.class)
          .hasMessageContaining("Account is locked")
          .hasMessageContaining("15 minutes");

      verify(securityAuditService).logFailedAuthentication("testuser", "Account locked", "127.0.0.1", "Mozilla/5.0");
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception for non-existent user and prevent user enumeration")
    void shouldThrowExceptionForNonExistentUser() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Invalid username or password");

      // Verify failed attempt is recorded even for non-existent users (prevents enumeration)
      verify(accountLockoutService).recordFailedAttempt("testuser");
      verify(securityAuditService).logFailedAuthentication("testuser", "User not found", "127.0.0.1", "Mozilla/5.0");
      verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception for invalid password and record failed attempt")
    void shouldThrowExceptionForInvalidPassword() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);
      when(accountLockoutService.recordFailedAttempt("testuser")).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Invalid username or password");

      verify(accountLockoutService).recordFailedAttempt("testuser");
      verify(accountLockoutService, never()).clearFailedAttempts(anyString());
      verify(securityAuditService).logFailedAuthentication("testuser", "Invalid password", "127.0.0.1", "Mozilla/5.0");
      verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should lock account after too many failed attempts and log security event")
    void shouldLockAccountAfterTooManyFailedAttempts() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);
      when(accountLockoutService.recordFailedAttempt("testuser")).thenReturn(true);
      when(accountLockoutService.getFailedAttempts("testuser")).thenReturn(5);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Invalid username or password");

      verify(accountLockoutService).recordFailedAttempt("testuser");
      verify(accountLockoutService).getFailedAttempts("testuser");
      verify(securityAuditService).logAccountLockout("testuser", 5, "127.0.0.1", "Mozilla/5.0");
      verify(securityAuditService, never()).logSuccessfulAuthentication(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception for disabled account after password validation")
    void shouldThrowExceptionForDisabledAccount() {
      // Arrange
      testUser.setEnabled(false);
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Account is disabled");

      verify(securityAuditService).logFailedAuthentication("testuser", "Account disabled", "127.0.0.1", "Mozilla/5.0");
      verify(jwtService, never()).generateAccessToken(any());
      verify(accountLockoutService, never()).clearFailedAttempts(anyString());
    }

    @Test
    @DisplayName("Should throw exception for unverified email with specific exception type")
    void shouldThrowExceptionForUnverifiedEmail() {
      // Arrange
      testUser.setEmailVerified(false);
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
      when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
      when(accountLockoutService.isAccountLocked(anyString())).thenReturn(false);
      when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.authenticateUser(loginRequest, "127.0.0.1", "Mozilla/5.0"))
          .isInstanceOf(AuthenticationService.EmailVerificationRequiredException.class)
          .hasMessageContaining("Email verification required");

      verify(securityAuditService).logFailedAuthentication("testuser", "Email not verified", "127.0.0.1", "Mozilla/5.0");
      verify(jwtService, never()).generateAccessToken(any());
    }
  }

  @Nested
  @DisplayName("Password Change Tests")
  class PasswordChangeTests {

    @Test
    @DisplayName("Should change password successfully and revoke all sessions")
    void shouldChangePasswordSuccessfully() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);
      when(passwordEncoder.matches("newPassword", "hashed-password")).thenReturn(false);
      when(passwordEncoder.encode("newPassword")).thenReturn("new-hashed-password");
      when(meterRegistry.counter(anyString())).thenReturn(counter);

      // Act
      service.changePassword(1L, "oldPassword", "newPassword", "127.0.0.1");

      // Assert
      assertThat(testUser.getPasswordHash()).isEqualTo("new-hashed-password");
      verify(inputSanitizer, times(2)).sanitizeInput(anyString());
      verify(userRepository).save(testUser);
      verify(jwtService).revokeAllRefreshTokensForUser("1");
      verify(sessionService).invalidateAllUserSessions("1");
      verify(securityAuditService).logTokenEvent("testuser", "password_changed", "127.0.0.1", "web");
      verify(counter).increment();
    }

    @Test
    @DisplayName("Should throw exception when changing password with invalid current password")
    void shouldThrowExceptionForInvalidCurrentPassword() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("wrongPassword", "hashed-password")).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> service.changePassword(1L, "wrongPassword", "newPassword", "127.0.0.1"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("Current password is incorrect");

      verify(securityAuditService).logFailedAuthentication(
          eq("testuser"), 
          eq("Invalid current password during password change"), 
          eq("127.0.0.1"), 
          eq("web")
      );
      verify(userRepository, never()).save(any());
      verify(jwtService, never()).revokeAllRefreshTokensForUser(anyString());
    }

    @Test
    @DisplayName("Should throw exception when new password is too short (< 8 characters)")
    void shouldThrowExceptionForShortPassword() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.changePassword(1L, "oldPassword", "short", "127.0.0.1"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("does not meet minimum requirements");

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when new password same as current password")
    void shouldThrowExceptionForSamePassword() {
      // Arrange
      when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches("oldPassword", "hashed-password")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.changePassword(1L, "oldPassword", "oldPassword", "127.0.0.1"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("must be different from current password");

      verify(userRepository, never()).save(any());
      verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found during password change")
    void shouldThrowExceptionWhenUserNotFound() {
      // Arrange
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.changePassword(999L, "oldPassword", "newPassword", "127.0.0.1"))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessageContaining("User not found");

      verify(passwordEncoder, never()).matches(anyString(), anyString());
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Token Refresh Tests")
  class TokenRefreshTests {

    @Test
    @DisplayName("Should refresh token successfully and return new access token")
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
      assertThat(response.user()).isNotNull();
      assertThat(response.user().username()).isEqualTo("testuser");
      
      verify(jwtService).validateToken("refresh-token");
      verify(jwtService).isUserRefreshRevoked("1", mockJwt.getIssuedAt());
      verify(jwtService).generateAccessToken(testUser);
    }

    @Test
    @DisplayName("Should throw exception for invalid token type (access instead of refresh)")
    void shouldThrowExceptionForInvalidTokenType() {
      // Arrange
      var refreshRequest = new RefreshTokenRequest("access-token");
      var mockJwt = createMockJwt("1", "access", Instant.now());
      
      when(jwtService.validateToken("access-token")).thenReturn(mockJwt);

      // Act & Assert
      assertThatThrownBy(() -> service.refreshToken(refreshRequest))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessage("Token refresh failed")
          .hasCauseInstanceOf(AuthenticationService.AuthenticationException.class);

      verify(jwtService, never()).generateAccessToken(any());
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception for revoked refresh token (user-wide revocation)")
    void shouldThrowExceptionForRevokedRefreshToken() {
      // Arrange
      var refreshRequest = new RefreshTokenRequest("refresh-token");
      var mockJwt = createMockJwt("1", "refresh", Instant.now());
      
      when(jwtService.validateToken("refresh-token")).thenReturn(mockJwt);
      when(jwtService.isUserRefreshRevoked("1", mockJwt.getIssuedAt())).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.refreshToken(refreshRequest))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessage("Token refresh failed")
          .hasCauseInstanceOf(AuthenticationService.AuthenticationException.class);

      verify(jwtService, never()).generateAccessToken(any());
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception when user is disabled during token refresh")
    void shouldThrowExceptionWhenUserDisabled() {
      // Arrange
      testUser.setEnabled(false);
      var refreshRequest = new RefreshTokenRequest("refresh-token");
      var mockJwt = createMockJwt("1", "refresh", Instant.now());
      
      when(jwtService.validateToken("refresh-token")).thenReturn(mockJwt);
      when(jwtService.isUserRefreshRevoked("1", mockJwt.getIssuedAt())).thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      // Act & Assert
      assertThatThrownBy(() -> service.refreshToken(refreshRequest))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessage("Token refresh failed")
          .hasCauseInstanceOf(AuthenticationService.AuthenticationException.class);

      verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found during token refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
      // Arrange
      var refreshRequest = new RefreshTokenRequest("refresh-token");
      var mockJwt = createMockJwt("999", "refresh", Instant.now());
      
      when(jwtService.validateToken("refresh-token")).thenReturn(mockJwt);
      when(jwtService.isUserRefreshRevoked("999", mockJwt.getIssuedAt())).thenReturn(false);
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.refreshToken(refreshRequest))
          .isInstanceOf(AuthenticationService.AuthenticationException.class)
          .hasMessage("Token refresh failed")
          .hasCauseInstanceOf(AuthenticationService.AuthenticationException.class);

      verify(jwtService, never()).generateAccessToken(any());
    }
  }

  @Nested
  @DisplayName("Logout Tests")
  class LogoutTests {

    @Test
    @DisplayName("Should logout user successfully and invalidate token and session")
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
      verify(jwtService).validateToken(accessToken);
      verify(sessionService).deleteSession(sessionId);
      verify(sessionService).removeUserSession("1", sessionId);
    }

    @Test
    @DisplayName("Should logout from all devices and revoke all tokens")
    void shouldLogoutFromAllDevices() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(meterRegistry.counter(anyString())).thenReturn(counter);

      // Act
      service.logoutFromAllDevices(1L);

      // Assert
      verify(jwtService).revokeAllRefreshTokensForUser("1");
      verify(sessionService).invalidateAllUserSessions("1");
      verify(securityAuditService, times(2)).logTokenEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle logout with null session ID gracefully")
    void shouldHandleLogoutWithNullSessionId() {
      // Arrange
      var accessToken = "access-token";

      // Act
      service.logoutUser(accessToken, null);

      // Assert
      verify(jwtService).invalidateToken(accessToken);
      verify(sessionService, never()).deleteSession(anyString());
    }

    @Test
    @DisplayName("Should handle logout with empty session ID gracefully")
    void shouldHandleLogoutWithEmptySessionId() {
      // Arrange
      var accessToken = "access-token";

      // Act
      service.logoutUser(accessToken, "   ");

      // Assert
      verify(jwtService).invalidateToken(accessToken);
      verify(sessionService, never()).deleteSession(anyString());
    }
  }

  @Nested
  @DisplayName("Session Management Tests")
  class SessionManagementTests {

    @Test
    @DisplayName("Should validate and extend session by 30 minutes")
    void shouldValidateAndExtendSession() {
      // Arrange
      var sessionId = "session-123";
      when(sessionService.sessionExists(sessionId)).thenReturn(true);

      // Act
      var isValid = service.validateAndExtendSession(sessionId);

      // Assert
      assertThat(isValid).isTrue();
      
      var durationCaptor = ArgumentCaptor.forClass(Duration.class);
      verify(sessionService).extendSession(eq(sessionId), durationCaptor.capture());
      assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Should return false for invalid session without extending")
    void shouldReturnFalseForInvalidSession() {
      // Arrange
      var sessionId = "invalid-session";
      when(sessionService.sessionExists(sessionId)).thenReturn(false);

      // Act
      var isValid = service.validateAndExtendSession(sessionId);

      // Assert
      assertThat(isValid).isFalse();
      verify(sessionService, never()).extendSession(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should return false for null session ID")
    void shouldReturnFalseForNullSessionId() {
      // Act
      var isValid = service.validateAndExtendSession(null);

      // Assert
      assertThat(isValid).isFalse();
      verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Should return false for empty session ID")
    void shouldReturnFalseForEmptySessionId() {
      // Act
      var isValid = service.validateAndExtendSession("   ");

      // Assert
      assertThat(isValid).isFalse();
      verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Should get user active sessions")
    void shouldGetUserActiveSessions() {
      // Arrange
      var sessions = Set.of("session-1", "session-2", "session-3");
      when(sessionService.getUserSessions("1")).thenReturn(Set.copyOf(sessions));

      // Act
      var result = service.getUserActiveSessions(1L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).hasSize(3);
      assertThat(result).containsExactlyInAnyOrder("session-1", "session-2", "session-3");
      verify(sessionService).getUserSessions("1");
    }

    @Test
    @DisplayName("Should return empty set when user has no active sessions")
    void shouldReturnEmptySetWhenNoActiveSessions() {
      // Arrange
      when(sessionService.getUserSessions("1")).thenReturn(Set.of());

      // Act
      var result = service.getUserActiveSessions(1L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should logout all user sessions and log security event")
    void shouldLogoutAllUserSessions() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(meterRegistry.counter(anyString())).thenReturn(counter);

      // Act
      service.logoutAllUserSessions(1L);

      // Assert
      verify(sessionService).invalidateAllUserSessions("1");
      verify(securityAuditService).logTokenEvent("testuser", "logout_from_all_devices", "system", "system");
      verify(counter).increment();
    }

    @Test
    @DisplayName("Should handle logout all sessions when user not found gracefully")
    void shouldHandleLogoutAllSessionsWhenUserNotFound() {
      // Arrange
      when(userRepository.findById(999L)).thenReturn(Optional.empty());
      when(meterRegistry.counter(anyString())).thenReturn(counter);

      // Act
      service.logoutAllUserSessions(999L);

      // Assert
      verify(sessionService).invalidateAllUserSessions("999");
      verify(securityAuditService, never()).logTokenEvent(anyString(), anyString(), anyString(), anyString());
      verify(counter).increment();
    }
  }

  @Nested
  @DisplayName("Authentication Result Tests")
  class AuthenticationResultTests {

    @Test
    @DisplayName("Should create authentication success result with all user context")
    void shouldCreateAuthenticationSuccess() {
      // Arrange
      var accessToken = "access-token";
      var refreshToken = "refresh-token";
      MDC.put("correlationId", "test-correlation-id");

      // Act
      var result = service.createAuthenticationSuccess(testUser, accessToken, refreshToken);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.user()).isNotNull();
      assertThat(result.user().userId()).isEqualTo(1L);
      assertThat(result.user().username()).isEqualTo("testuser");
      assertThat(result.user().email()).isEqualTo("test@example.com");
      assertThat(result.user().tenantId()).isEqualTo("tenant-123");
      assertThat(result.user().roles()).contains("ROLE_USER");
      assertThat(result.accessToken()).isEqualTo("access-token");
      assertThat(result.refreshToken()).isEqualTo("refresh-token");
      assertThat(result.correlationId()).isEqualTo("test-correlation-id");
      assertThat(result.timestamp()).isNotNull();
      assertThat(result.timestamp()).isBeforeOrEqualTo(Instant.now());

      // Cleanup
      MDC.clear();
    }

    @Test
    @DisplayName("Should create authentication failure result with error details")
    void shouldCreateAuthenticationFailure() {
      // Arrange
      MDC.put("correlationId", "test-correlation-id");

      // Act
      var result = service.createAuthenticationFailure("Invalid credentials", "AUTH_001");

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.reason()).isEqualTo("Invalid credentials");
      assertThat(result.errorCode()).isEqualTo("AUTH_001");
      assertThat(result.correlationId()).isEqualTo("test-correlation-id");
      assertThat(result.timestamp()).isNotNull();
      assertThat(result.timestamp()).isBeforeOrEqualTo(Instant.now());

      // Cleanup
      MDC.clear();
    }

    @Test
    @DisplayName("Should handle null correlation ID in authentication success")
    void shouldHandleNullCorrelationIdInSuccess() {
      // Arrange
      MDC.clear();
      var accessToken = "access-token";
      var refreshToken = "refresh-token";

      // Act
      var result = service.createAuthenticationSuccess(testUser, accessToken, refreshToken);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.correlationId()).isNull();
    }

    @Test
    @DisplayName("Should handle null correlation ID in authentication failure")
    void shouldHandleNullCorrelationIdInFailure() {
      // Arrange
      MDC.clear();

      // Act
      var result = service.createAuthenticationFailure("Invalid credentials", "AUTH_001");

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.correlationId()).isNull();
    }
  }

  // Helper methods

  /**
   * Creates a mock JWT token for testing.
   */
  private Jwt createMockJwt(String userId, String type, Instant issuedAt) {
    return new Jwt(
        "token-value",
        issuedAt,
        issuedAt.plusSeconds(3600),
        Map.of("alg", "HS256", "typ", "JWT"),
        Map.of("sub", userId, "type", type, "iat", issuedAt.getEpochSecond())
    );
  }

  /**
   * Sets the user ID using reflection (since ID is typically managed by JPA).
   */
  private void setUserId(User user, Long id) {
    try {
      Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to set user ID via reflection", e);
    }
  }
}
