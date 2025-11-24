package org.gripday.userservice.passwordmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.userservice.authentication.JwtService;
import org.gripday.userservice.config.RedisConfig.TenantAwareRedisService;
import org.gripday.userservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.userservice.security.InputSanitizer;
import org.gripday.userservice.security.SecurityAuditService;
import org.gripday.userservice.shared.EmailService;
import org.gripday.userservice.usermanagement.User;
import org.gripday.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for PasswordResetService.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @Mock
  private SecurityAuditService securityAuditService;

  @Mock
  private InputSanitizer inputSanitizer;

  @Mock
  private TenantAwareSessionService sessionService;

  @Mock
  private TenantAwareRedisService redisService;

  @Mock
  private EmailService emailService;

  @Mock
  private MeterRegistry meterRegistry;

  @Mock
  private Counter counter;

  private PasswordResetService service;
  private User testUser;

  @BeforeEach
  void setUp() {
    service = new PasswordResetService(
        userRepository,
        passwordEncoder,
        jwtService,
        securityAuditService,
        inputSanitizer,
        sessionService,
        redisService,
        emailService,
        meterRegistry
    );

    testUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");

    // Setup default mocks
    lenient().when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
    lenient().when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
    lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
  }

  @Test
  @DisplayName("Should initiate password reset successfully")
  void shouldInitiatePasswordResetSuccessfully() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act
    service.initiatePasswordReset("test@example.com", "127.0.0.1", "Mozilla/5.0");

    // Assert - service calls set twice: once for token->userId and once for user->token
    verify(redisService, org.mockito.Mockito.times(2)).set(anyString(), any(), eq(Duration.ofMinutes(30)));
    verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
    verify(securityAuditService).logTokenEvent(eq("testuser"), eq("password_reset_initiated"), eq("127.0.0.1"), eq("Mozilla/5.0"));
  }

  @Test
  @DisplayName("Should not reveal user existence when email not found")
  void shouldNotRevealUserExistenceWhenEmailNotFound() {
    // Arrange
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    // Act
    service.initiatePasswordReset("unknown@example.com", "127.0.0.1", "Mozilla/5.0");

    // Assert - should not throw exception or send email
    verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    verify(securityAuditService).logSuspiciousActivity(
        eq(null), 
        eq("Password reset requested for unknown email"), 
        eq("127.0.0.1"), 
        eq("Mozilla/5.0")
    );
  }

  @Test
  @DisplayName("Should validate reset token successfully")
  void shouldValidateResetTokenSuccessfully() {
    // Arrange
    var token = "valid-token";
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should return false for invalid token")
  void shouldReturnFalseForInvalidToken() {
    // Arrange
    var token = "invalid-token";
    when(redisService.get("password-reset:token:invalid-token")).thenReturn(null);

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should return false for unsafe token input")
  void shouldReturnFalseForUnsafeTokenInput() {
    // Arrange
    var token = "unsafe-token";
    when(inputSanitizer.isInputSafe("unsafe-token")).thenReturn(false);

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should return false when user no longer exists")
  void shouldReturnFalseWhenUserNoLongerExists() {
    // Arrange
    var token = "valid-token";
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should reset password successfully")
  void shouldResetPasswordSuccessfully() {
    // Arrange
    var token = "valid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode(newPassword)).thenReturn("new-hashed-password");

    // Act
    service.resetPassword(token, newPassword, "127.0.0.1");

    // Assert
    verify(userRepository).save(testUser);
    verify(jwtService).revokeAllRefreshTokensForUser("1");
    verify(sessionService).invalidateAllUserSessions("1");
    verify(redisService).delete("password-reset:token:valid-token");
    verify(emailService).sendPasswordResetConfirmedEmail(testUser);
    verify(securityAuditService).logTokenEvent("testuser", "password_reset_completed", "127.0.0.1", "system");
  }

  @Test
  @DisplayName("Should throw exception for invalid reset token")
  void shouldThrowExceptionForInvalidResetToken() {
    // Arrange
    var token = "invalid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:invalid-token")).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> service.resetPassword(token, newPassword, "127.0.0.1"))
        .isInstanceOf(PasswordResetService.PasswordResetException.class)
        .hasMessageContaining("Invalid or expired reset token");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for unsafe token input during reset")
  void shouldThrowExceptionForUnsafeTokenInputDuringReset() {
    // Arrange
    var token = "unsafe-token";
    var newPassword = "newPassword123";
    
    when(inputSanitizer.isInputSafe("unsafe-token")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.resetPassword(token, newPassword, "127.0.0.1"))
        .isInstanceOf(PasswordResetService.PasswordResetException.class)
        .hasMessageContaining("Invalid reset token");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when user not found during reset")
  void shouldThrowExceptionWhenUserNotFoundDuringReset() {
    // Arrange
    var token = "valid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.resetPassword(token, newPassword, "127.0.0.1"))
        .isInstanceOf(PasswordResetService.PasswordResetException.class)
        .hasMessageContaining("User not found");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for password too short")
  void shouldThrowExceptionForPasswordTooShort() {
    // Arrange
    var token = "valid-token";
    var newPassword = "short";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThatThrownBy(() -> service.resetPassword(token, newPassword, "127.0.0.1"))
        .isInstanceOf(PasswordResetService.PasswordResetException.class)
        .hasMessageContaining("Password does not meet minimum requirements");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should continue reset even if confirmation email fails")
  void shouldContinueResetEvenIfConfirmationEmailFails() {
    // Arrange
    var token = "valid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode(newPassword)).thenReturn("new-hashed-password");
    lenient().doThrow(new RuntimeException("Email service unavailable"))
        .when(emailService).sendPasswordResetConfirmedEmail(testUser);

    // Act - should not throw exception
    service.resetPassword(token, newPassword, "127.0.0.1");

    // Assert - password should still be reset
    verify(userRepository).save(testUser);
    verify(jwtService).revokeAllRefreshTokensForUser("1");
  }

  @Test
  @DisplayName("Should delete token after successful reset")
  void shouldDeleteTokenAfterSuccessfulReset() {
    // Arrange
    var token = "valid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode(newPassword)).thenReturn("new-hashed-password");

    // Act
    service.resetPassword(token, newPassword, "127.0.0.1");

    // Assert
    verify(redisService).delete("password-reset:token:valid-token");
    verify(redisService).delete("password-reset:user:null"); // user.getId() returns null in test
  }

  @Test
  @DisplayName("Should handle numeric user ID from Redis")
  void shouldHandleNumericUserIdFromRedis() {
    // Arrange
    var token = "valid-token";
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1); // Integer instead of Long
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should handle string user ID from Redis")
  void shouldHandleStringUserIdFromRedis() {
    // Arrange
    var token = "valid-token";
    when(redisService.get("password-reset:token:valid-token")).thenReturn("1"); // String instead of Long
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    var isValid = service.isResetTokenValid(token);

    // Assert
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should store token with 30 minute TTL")
  void shouldStoreTokenWith30MinuteTTL() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act
    service.initiatePasswordReset("test@example.com", "127.0.0.1", "Mozilla/5.0");

    // Assert - service calls set twice: once for token->userId and once for user->token
    verify(redisService, org.mockito.Mockito.times(2)).set(anyString(), any(), eq(Duration.ofMinutes(30)));
  }

  @Test
  @DisplayName("Should not throw exception when initiate fails")
  void shouldNotThrowExceptionWhenInitiateFails() {
    // Arrange
    when(inputSanitizer.isInputSafe(anyString())).thenReturn(false);

    // Act - should not throw exception
    service.initiatePasswordReset("test@example.com", "127.0.0.1", "Mozilla/5.0");

    // Assert - method completes without exception
    verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
  }

  @Test
  @DisplayName("Should sanitize email input during initiate")
  void shouldSanitizeEmailInputDuringInitiate() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act
    service.initiatePasswordReset("test@example.com", "127.0.0.1", "Mozilla/5.0");

    // Assert
    verify(inputSanitizer).sanitizeInput("test@example.com");
  }

  @Test
  @DisplayName("Should sanitize token and password during reset")
  void shouldSanitizeTokenAndPasswordDuringReset() {
    // Arrange
    var token = "valid-token";
    var newPassword = "newPassword123";
    
    when(redisService.get("password-reset:token:valid-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode(newPassword)).thenReturn("new-hashed-password");

    // Act
    service.resetPassword(token, newPassword, "127.0.0.1");

    // Assert
    verify(inputSanitizer).sanitizeInput(token);
    verify(inputSanitizer).sanitizeInput(newPassword);
  }
}
