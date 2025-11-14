package org.gripday.userservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.userservice.config.RedisConfig.TenantAwareRedisService;
import org.gripday.userservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.userservice.domain.service.EmailService;
import org.gripday.userservice.domain.service.JwtService;
import org.gripday.userservice.domain.service.PasswordResetService;
import org.gripday.userservice.domain.service.SecurityAuditService;
import org.gripday.userservice.infrastructure.entity.User;
import org.gripday.userservice.infrastructure.repository.UserRepository;
import org.gripday.userservice.presentation.validation.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for PasswordResetService focusing on happy path scenarios.
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

  private PasswordResetService passwordResetService;
  private User testUser;

  @BeforeEach
  void setUp() {
    passwordResetService = new PasswordResetService(
        userRepository, passwordEncoder, jwtService, securityAuditService,
        inputSanitizer, sessionService, redisService, emailService, meterRegistry
    );

    testUser = createTestUser(1L, "testuser", "test@example.com", "tenant-1");
  }

  @Test
  void resetPassword_WithValidToken_ShouldSucceed() {
    // Given
    var resetToken = "valid-reset-token";
    var newPassword = "newPassword123";
    var clientIp = "192.168.1.1";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(resetToken)).thenReturn(resetToken);
    when(inputSanitizer.sanitizeInput(newPassword)).thenReturn(newPassword);
    when(inputSanitizer.isInputSafe(resetToken)).thenReturn(true);

    // Mock Redis token lookup
    when(redisService.get("password-reset:token:" + resetToken)).thenReturn(1L);

    // Mock user lookup
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Mock password encoding
    when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

    // Mock user save
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // Mock JWT service
    doNothing().when(jwtService).revokeAllRefreshTokensForUser("1");

    // Mock session service
    doNothing().when(sessionService).invalidateAllUserSessions("1");

    // Mock Redis delete
    when(redisService.delete(anyString())).thenReturn(true);

    // Mock email service
    doNothing().when(emailService).sendPasswordResetConfirmedEmail(testUser);

    // Mock metrics
    when(meterRegistry.counter("auth.password_reset.completed")).thenReturn(counter);

    // When
    assertDoesNotThrow(() -> passwordResetService.resetPassword(resetToken, newPassword, clientIp));

    // Then
    verify(userRepository).save(testUser);
    verify(jwtService).revokeAllRefreshTokensForUser("1");
    verify(sessionService).invalidateAllUserSessions("1");
    verify(redisService).delete("password-reset:token:" + resetToken);
    verify(redisService).delete("password-reset:user:" + testUser.getId());
    verify(emailService).sendPasswordResetConfirmedEmail(testUser);
    verify(securityAuditService).logTokenEvent("testuser", "password_reset_completed", clientIp, "system");
  }

  @Test
  void resetPassword_WithInvalidToken_ShouldThrowException() {
    // Given
    var resetToken = "invalid-token";
    var newPassword = "newPassword123";
    var clientIp = "192.168.1.1";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(resetToken)).thenReturn(resetToken);
    when(inputSanitizer.sanitizeInput(newPassword)).thenReturn(newPassword);
    when(inputSanitizer.isInputSafe(resetToken)).thenReturn(true);

    // Mock Redis token lookup - token not found
    when(redisService.get("password-reset:token:" + resetToken)).thenReturn(null);

    // When & Then
    var exception = assertThrows(PasswordResetService.PasswordResetException.class, () -> {
      passwordResetService.resetPassword(resetToken, newPassword, clientIp);
    });

    assertEquals("Invalid or expired reset token", exception.getMessage());
  }

  @Test
  void initiatePasswordReset_WithValidEmail_ShouldSucceed() {
    // Given
    var email = "test@example.com";
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(email)).thenReturn(email);
    when(inputSanitizer.isInputSafe(email)).thenReturn(true);

    // Mock user lookup
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

    // Mock Redis set
    doNothing().when(redisService).set(anyString(), any(), any(Duration.class));

    // Mock email service
    doNothing().when(emailService).sendPasswordResetEmail(eq(testUser), anyString());

    // When
    assertDoesNotThrow(() -> passwordResetService.initiatePasswordReset(email, ipAddress, userAgent));

    // Then
    verify(userRepository).findByEmail(email);
    verify(redisService).set(anyString(), eq(testUser.getId()), eq(Duration.ofMinutes(30)));
    verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
    verify(securityAuditService).logTokenEvent(eq("testuser"), eq("password_reset_initiated"), eq(ipAddress), eq(userAgent));
  }

  @Test
  void initiatePasswordReset_WithUnknownEmail_ShouldNotRevealExistence() {
    // Given
    var email = "unknown@example.com";
    var ipAddress = "192.168.1.1";
    var userAgent = "Mozilla/5.0";

    // Mock input sanitization
    when(inputSanitizer.sanitizeInput(email)).thenReturn(email);
    when(inputSanitizer.isInputSafe(email)).thenReturn(true);

    // Mock user lookup - user not found
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // Mock metrics
    when(meterRegistry.counter("auth.password_reset.initiated")).thenReturn(counter);

    // When
    assertDoesNotThrow(() -> passwordResetService.initiatePasswordReset(email, ipAddress, userAgent));

    // Then
    verify(securityAuditService).logSuspiciousActivity(null,
        "Password reset requested for unknown email", ipAddress, userAgent);
  }

  private User createTestUser(Long id, String username, String email, String tenantId) {
    var user = new User(username, email, "hashedPassword", "John", "Doe", tenantId);

    // Use reflection to set the ID for testing
    try {
      var idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (final Exception e) {
      System.err.println("Failed to set user ID via reflection: " + e.getMessage());
    }

    return user;
  }
}
