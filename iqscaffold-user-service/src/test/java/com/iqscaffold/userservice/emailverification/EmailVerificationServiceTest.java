package com.iqscaffold.userservice.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.iqscaffold.userservice.shared.EmailOperations;
import com.iqscaffold.userservice.shared.exception.EmailVerificationException;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Unit tests for EmailVerificationService.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock
  private VerificationTokenRepository tokenRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private EmailOperations emailService;

  @Mock
  private VerificationMetrics metricsService;

  private EmailVerificationService service;
  private User testUser;

  @BeforeEach
  void setUp() {
    service = new EmailVerificationService(
        tokenRepository,
        userRepository,
        emailService,
        metricsService
    );

    testUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    testUser.setEmailVerified(false);
  }

  @Test
  @DisplayName("Should generate verification token successfully")
  void shouldGenerateVerificationTokenSuccessfully() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(any(), any())).thenReturn(0L);

    // Act
    var token = service.generateVerificationToken(testUser);

    // Assert
    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
    verify(tokenRepository).markAllUnusedTokensAsUsedByUserId(null);
    verify(tokenRepository).save(any(VerificationToken.class));
    verify(emailService).sendVerificationEmail(eq(testUser), anyString());
  }

  @Test
  @DisplayName("Should throw exception when user already verified")
  void shouldThrowExceptionWhenUserAlreadyVerified() {
    // Arrange
    testUser.setEmailVerified(true);

    // Act & Assert
    assertThatThrownBy(() -> service.generateVerificationToken(testUser))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("already verified");

    verify(tokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when rate limit exceeded")
  void shouldThrowExceptionWhenRateLimitExceeded() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(any(), any())).thenReturn(3L);

    // Act & Assert
    assertThatThrownBy(() -> service.generateVerificationToken(testUser))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("Rate limit exceeded");

    verify(tokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should invalidate existing tokens before generating new one")
  void shouldInvalidateExistingTokensBeforeGeneratingNewOne() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(any(), any())).thenReturn(0L);

    // Act
    service.generateVerificationToken(testUser);

    // Assert
    verify(tokenRepository).markAllUnusedTokensAsUsedByUserId(null);
  }

  @Test
  @DisplayName("Should mark token as used when email sending fails")
  void shouldMarkTokenAsUsedWhenEmailSendingFails() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(any(), any())).thenReturn(0L);
    when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));
    doThrow(new RuntimeException("Email service unavailable"))
        .when(emailService).sendVerificationEmail(any(), anyString());

    // Act & Assert
    assertThatThrownBy(() -> service.generateVerificationToken(testUser))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("Failed to send verification email");

    // Verify save was called twice: once to create token, once to mark as used
    verify(tokenRepository, org.mockito.Mockito.times(2)).save(any(VerificationToken.class));
  }

  @Test
  @DisplayName("Should verify email successfully")
  void shouldVerifyEmailSuccessfully() {
    // Arrange
    var token = "valid-token";
    var verificationToken = new VerificationToken(token, 1L, LocalDateTime.now().plusHours(24), "tenant-123");

    when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    var response = service.verifyEmail(token);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.success()).isTrue();
    assertThat(response.message()).contains("verified successfully");
    assertThat(response.username()).isEqualTo("testuser");

    verify(tokenRepository).save(verificationToken);
    verify(userRepository).save(testUser);
    verify(metricsService).recordVerificationSuccess();
    verify(emailService).sendRegistrationConfirmedEmail(testUser);
  }

  @Test
  @DisplayName("Should throw exception for null token")
  void shouldThrowExceptionForNullToken() {
    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail(null))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("cannot be null or empty");

    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should throw exception for empty token")
  void shouldThrowExceptionForEmptyToken() {
    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail("  "))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("cannot be null or empty");

    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should throw exception for invalid token")
  void shouldThrowExceptionForInvalidToken() {
    // Arrange
    when(tokenRepository.findByTokenAndUsedFalse("invalid-token")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail("invalid-token"))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("Invalid or already used");

    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should throw exception for expired token")
  void shouldThrowExceptionForExpiredToken() {
    // Arrange
    var token = "expired-token";
    var verificationToken = new VerificationToken(token, 1L, LocalDateTime.now().minusHours(1), "tenant-123");

    when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));

    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail(token))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("expired");

    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    var token = "valid-token";
    var verificationToken = new VerificationToken(token, 1L, LocalDateTime.now().plusHours(24), "tenant-123");

    when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail(token))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("User not found");

    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should throw exception when user already verified")
  void shouldThrowExceptionWhenUserAlreadyVerifiedDuringVerification() {
    // Arrange
    var token = "valid-token";
    var verificationToken = new VerificationToken(token, 1L, LocalDateTime.now().plusHours(24), "tenant-123");
    testUser.setEmailVerified(true);

    when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThatThrownBy(() -> service.verifyEmail(token))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("already verified");

    verify(tokenRepository).save(verificationToken);
    verify(metricsService).recordVerificationFailed();
  }

  @Test
  @DisplayName("Should continue verification even if welcome email fails")
  void shouldContinueVerificationEvenIfWelcomeEmailFails() {
    // Arrange
    var token = "valid-token";
    var verificationToken = new VerificationToken(token, 1L, LocalDateTime.now().plusHours(24), "tenant-123");

    when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    doThrow(new RuntimeException("Email service unavailable"))
        .when(emailService).sendRegistrationConfirmedEmail(testUser);

    // Act
    var response = service.verifyEmail(token);

    // Assert - verification should still succeed
    assertThat(response.success()).isTrue();
    verify(userRepository).save(testUser);
    verify(metricsService).recordVerificationSuccess();
  }

  @Test
  @DisplayName("Should resend verification email successfully")
  void shouldResendVerificationEmailSuccessfully() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(tokenRepository.countTokensCreatedSince(any(), any())).thenReturn(0L);

    // Act
    var response = service.resendVerificationEmail("test@example.com", "127.0.0.1");

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.success()).isTrue();
    assertThat(response.message()).contains("sent successfully");
    verify(emailService).sendVerificationEmail(eq(testUser), anyString());
  }

  @Test
  @DisplayName("Should throw exception when resending to null email")
  void shouldThrowExceptionWhenResendingToNullEmail() {
    // Act & Assert
    assertThatThrownBy(() -> service.resendVerificationEmail(null, "127.0.0.1"))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when resending to non-existent user")
  void shouldThrowExceptionWhenResendingToNonExistentUser() {
    // Arrange
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.resendVerificationEmail("unknown@example.com", "127.0.0.1"))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  @DisplayName("Should throw exception when resending to already verified user")
  void shouldThrowExceptionWhenResendingToAlreadyVerifiedUser() {
    // Arrange
    testUser.setEmailVerified(true);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThatThrownBy(() -> service.resendVerificationEmail("test@example.com", "127.0.0.1"))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("already verified");
  }

  @Test
  @DisplayName("Should cleanup expired tokens")
  void shouldCleanupExpiredTokens() {
    // Arrange
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(tokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should handle cleanup errors gracefully")
  void shouldHandleCleanupErrorsGracefully() {
    // Arrange
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
        .thenThrow(new RuntimeException("Database error"));

    // Act - should not throw exception
    service.cleanupExpiredTokens();

    // Assert - method completes without exception
    verify(tokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should get remaining email count")
  void shouldGetRemainingEmailCount() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(anyLong(), any())).thenReturn(1L);

    // Act
    var remaining = service.getRemainingEmailCount(1L, "tenant-123");

    // Assert
    assertThat(remaining).isEqualTo(2);
  }

  @Test
  @DisplayName("Should return zero when rate limit reached")
  void shouldReturnZeroWhenRateLimitReached() {
    // Arrange
    when(tokenRepository.countTokensCreatedSince(anyLong(), any())).thenReturn(3L);

    // Act
    var remaining = service.getRemainingEmailCount(1L, "tenant-123");

    // Assert
    assertThat(remaining).isEqualTo(0);
  }

  @Test
  @DisplayName("Should check if user has unused tokens")
  void shouldCheckIfUserHasUnusedTokens() {
    // Arrange
    when(tokenRepository.countUnusedTokensByUserId(1L)).thenReturn(1L);

    // Act
    var hasUnused = service.hasUnusedTokens(1L, "tenant-123");

    // Assert
    assertThat(hasUnused).isTrue();
  }

  @Test
  @DisplayName("Should get verification status for verified user")
  void shouldGetVerificationStatusForVerifiedUser() {
    // Arrange
    testUser.setEmailVerified(true);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act
    var status = service.getVerificationStatus("test@example.com");

    // Assert
    assertThat(status).isNotNull();
    assertThat(status.emailVerified()).isTrue();
    assertThat(status.message()).contains("verified and active");
  }

  @Test
  @DisplayName("Should get verification status for unverified user")
  void shouldGetVerificationStatusForUnverifiedUser() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    // Act
    var status = service.getVerificationStatus("test@example.com");

    // Assert
    assertThat(status).isNotNull();
    assertThat(status.emailVerified()).isFalse();
    assertThat(status.message()).contains("pending");
  }

  @Test
  @DisplayName("Should throw exception when getting status for null email")
  void shouldThrowExceptionWhenGettingStatusForNullEmail() {
    // Act & Assert
    assertThatThrownBy(() -> service.getVerificationStatus(null))
        .isInstanceOf(EmailVerificationException.class)
        .hasMessageContaining("cannot be null or empty");
  }
}

