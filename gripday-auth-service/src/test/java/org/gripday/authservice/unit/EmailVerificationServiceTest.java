package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.gripday.authservice.domain.service.EmailOperations;
import org.gripday.authservice.domain.service.EmailVerificationMetricsService;
import org.gripday.authservice.domain.service.EmailVerificationService;
import org.gripday.authservice.domain.service.TenantContext;
import org.gripday.authservice.infrastructure.entity.EmailVerificationToken;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.EmailVerificationTokenRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EmailVerificationService focusing on core functionality. Tests token generation, email verification flow, rate limiting, and cleanup logic.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock
  private EmailVerificationTokenRepository tokenRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private EmailOperations emailService;

  private EmailVerificationService emailVerificationService;
  private User testUser;
  private EmailVerificationToken testToken;

  @BeforeEach
  void setUp() {
    var metricsService = mock(EmailVerificationMetricsService.class);
    emailVerificationService = new EmailVerificationService(
        tokenRepository, userRepository, emailService, metricsService
    );

    testUser = createTestUser(1L, "testuser", "test@example.com", "tenant-1");
    testUser.setEmailVerified(false);

    testToken = new EmailVerificationToken(
        "test-token-123",
        testUser.getId(),
        LocalDateTime.now().plusHours(24),
        testUser.getTenantId()
    );

    // Set up tenant context
    TenantContext.setCurrentTenantId("tenant-1");
  }

  @Test
  void generateVerificationToken_WithValidUser_ShouldSucceed() {
    // Given
    when(tokenRepository.countTokensCreatedSince(eq(testUser.getId()), eq(testUser.getTenantId()), any(LocalDateTime.class)))
        .thenReturn(0L);
    when(tokenRepository.markAllUnusedTokensAsUsedByUserIdAndTenantId(testUser.getId(), testUser.getTenantId()))
        .thenReturn(0);
    when(tokenRepository.save(any(EmailVerificationToken.class)))
        .thenReturn(testToken);
    doNothing().when(emailService).sendVerificationEmail(eq(testUser), anyString());

    // When
    var result = emailVerificationService.generateVerificationToken(testUser);

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify token was saved
    verify(tokenRepository).save(any(EmailVerificationToken.class));

    // Verify email was sent
    verify(emailService).sendVerificationEmail(eq(testUser), anyString());

    // Verify existing tokens were invalidated
    verify(tokenRepository).markAllUnusedTokensAsUsedByUserIdAndTenantId(testUser.getId(), testUser.getTenantId());
  }

  @Test
  void generateVerificationToken_WithAlreadyVerifiedUser_ShouldThrowException() {
    // Given
    testUser.setEmailVerified(true);

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.generateVerificationToken(testUser));

    assertEquals("User email is already verified", exception.getMessage());

    // Verify no token was created or email sent
    verify(tokenRepository, never()).save(any());
    verify(emailService, never()).sendVerificationEmail(any(), any());
  }

  @Test
  void generateVerificationToken_WithRateLimitExceeded_ShouldThrowException() {
    // Given
    when(tokenRepository.countTokensCreatedSince(eq(testUser.getId()), eq(testUser.getTenantId()), any(LocalDateTime.class)))
        .thenReturn(3L); // Exceeds limit of 3

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.generateVerificationToken(testUser));

    assertTrue(exception.getMessage().contains("Rate limit exceeded"));

    // Verify no token was created or email sent
    verify(tokenRepository, never()).save(any());
    verify(emailService, never()).sendVerificationEmail(any(), any());
  }

  @Test
  void generateVerificationToken_WithEmailSendFailure_ShouldMarkTokenAsUsed() {
    // Given
    when(tokenRepository.countTokensCreatedSince(eq(testUser.getId()), eq(testUser.getTenantId()), any(LocalDateTime.class)))
        .thenReturn(0L);
    when(tokenRepository.markAllUnusedTokensAsUsedByUserIdAndTenantId(testUser.getId(), testUser.getTenantId()))
        .thenReturn(0);
    when(tokenRepository.save(any(EmailVerificationToken.class)))
        .thenReturn(testToken);
    doThrow(new RuntimeException("Email send failed"))
        .when(emailService).sendVerificationEmail(eq(testUser), anyString());

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.generateVerificationToken(testUser));

    assertEquals("Failed to send verification email", exception.getMessage());

    // Verify token was saved twice (once for creation, once for marking as used)
    verify(tokenRepository, times(2)).save(any(EmailVerificationToken.class));
  }

  @Test
  void verifyEmail_WithValidToken_ShouldSucceed() {
    // Given
    var token = "valid-token-123";
    var validToken = new EmailVerificationToken(
        token,
        testUser.getId(),
        LocalDateTime.now().plusHours(1), // Not expired
        testUser.getTenantId()
    );

    when(tokenRepository.findByTokenAndUsedFalse(token))
        .thenReturn(Optional.of(validToken));
    when(userRepository.findById(testUser.getId()))
        .thenReturn(Optional.of(testUser));
    when(tokenRepository.save(validToken))
        .thenReturn(validToken);
    when(userRepository.save(testUser))
        .thenReturn(testUser);

    // When
    var result = emailVerificationService.verifyEmail(token);

    // Then
    assertNotNull(result);
    assertTrue(result.success());
    assertEquals("Email verified successfully", result.message());
    assertEquals("testuser", result.username());

    // Verify token was marked as used
    verify(tokenRepository).save(validToken);
    assertTrue(validToken.getUsed());

    // Verify user was updated
    verify(userRepository).save(testUser);
  }

  @Test
  void verifyEmail_WithInvalidToken_ShouldThrowException() {
    // Given
    var token = "invalid-token";
    when(tokenRepository.findByTokenAndUsedFalse(token))
        .thenReturn(Optional.empty());

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.verifyEmail(token));

    assertEquals("Invalid or already used verification token", exception.getMessage());

    // Verify no user update occurred
    verify(userRepository, never()).save(any());
  }

  @Test
  void verifyEmail_WithExpiredToken_ShouldThrowException() {
    // Given
    var token = "expired-token";
    var expiredToken = new EmailVerificationToken(
        token,
        testUser.getId(),
        LocalDateTime.now().minusHours(1), // Expired
        testUser.getTenantId()
    );

    when(tokenRepository.findByTokenAndUsedFalse(token))
        .thenReturn(Optional.of(expiredToken));

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.verifyEmail(token));

    assertEquals("Verification token has expired", exception.getMessage());

    // Verify no user update occurred
    verify(userRepository, never()).save(any());
  }

  @Test
  void verifyEmail_WithAlreadyVerifiedUser_ShouldThrowException() {
    // Given
    var token = "valid-token";
    var validToken = new EmailVerificationToken(
        token,
        testUser.getId(),
        LocalDateTime.now().plusHours(1),
        testUser.getTenantId()
    );

    testUser.setEmailVerified(true); // Already verified

    when(tokenRepository.findByTokenAndUsedFalse(token))
        .thenReturn(Optional.of(validToken));
    when(userRepository.findById(testUser.getId()))
        .thenReturn(Optional.of(testUser));
    when(tokenRepository.save(validToken))
        .thenReturn(validToken);

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.verifyEmail(token));

    assertEquals("User email is already verified", exception.getMessage());

    // Verify token was still marked as used
    verify(tokenRepository).save(validToken);
    assertTrue(validToken.getUsed());
  }

  @Test
  void resendVerificationEmail_WithValidEmail_ShouldSucceed() {
    // Given
    var email = "test@example.com";
    when(userRepository.findByEmail(email))
        .thenReturn(Optional.of(testUser));
    when(tokenRepository.countTokensCreatedSince(eq(testUser.getId()), eq(testUser.getTenantId()), any(LocalDateTime.class)))
        .thenReturn(0L);
    when(tokenRepository.markAllUnusedTokensAsUsedByUserIdAndTenantId(testUser.getId(), testUser.getTenantId()))
        .thenReturn(0);
    when(tokenRepository.save(any(EmailVerificationToken.class)))
        .thenReturn(testToken);
    doNothing().when(emailService).sendVerificationEmail(eq(testUser), anyString());

    // When
    var result = emailVerificationService.resendVerificationEmail(email, "127.0.0.1");

    // Then
    assertNotNull(result);
    assertTrue(result.success());
    assertEquals("Verification email sent successfully", result.message());
    assertEquals("testuser", result.username());

    // Verify email was sent
    verify(emailService).sendVerificationEmail(eq(testUser), anyString());
  }

  @Test
  void resendVerificationEmail_WithNonExistentEmail_ShouldThrowException() {
    // Given
    var email = "nonexistent@example.com";
    when(userRepository.findByEmail(email))
        .thenReturn(Optional.empty());

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.resendVerificationEmail(email, "127.0.0.1"));

    assertTrue(exception.getMessage().contains("User not found with email"));

    // Verify no email was sent
    verify(emailService, never()).sendVerificationEmail(any(), any());
  }

  @Test
  void resendVerificationEmail_WithAlreadyVerifiedUser_ShouldThrowException() {
    // Given
    var email = "test@example.com";
    testUser.setEmailVerified(true);
    when(userRepository.findByEmail(email))
        .thenReturn(Optional.of(testUser));

    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.resendVerificationEmail(email, "127.0.0.1"));

    assertEquals("User email is already verified", exception.getMessage());

    // Verify no email was sent
    verify(emailService, never()).sendVerificationEmail(any(), any());
  }

  @Test
  void cleanupExpiredTokens_ShouldDeleteExpiredTokens() {
    // Given
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
        .thenReturn(5); // 5 tokens deleted

    // When
    emailVerificationService.cleanupExpiredTokens();

    // Then
    verify(tokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }

  @Test
  void getRemainingEmailCount_ShouldReturnCorrectCount() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.countTokensCreatedSince(eq(userId), eq(tenantId), any(LocalDateTime.class)))
        .thenReturn(1L); // 1 token sent in last hour

    // When
    var result = emailVerificationService.getRemainingEmailCount(userId, tenantId);

    // Then
    assertEquals(2, result); // 3 max - 1 sent = 2 remaining
  }

  @Test
  void getRemainingEmailCount_WithMaxTokensSent_ShouldReturnZero() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.countTokensCreatedSince(eq(userId), eq(tenantId), any(LocalDateTime.class)))
        .thenReturn(3L); // 3 tokens sent (max limit)

    // When
    var result = emailVerificationService.getRemainingEmailCount(userId, tenantId);

    // Then
    assertEquals(0, result);
  }

  @Test
  void hasUnusedTokens_WithUnusedTokens_ShouldReturnTrue() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.countUnusedTokensByUserIdAndTenantId(userId, tenantId))
        .thenReturn(1L);

    // When
    var result = emailVerificationService.hasUnusedTokens(userId, tenantId);

    // Then
    assertTrue(result);
  }

  @Test
  void hasUnusedTokens_WithNoUnusedTokens_ShouldReturnFalse() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.countUnusedTokensByUserIdAndTenantId(userId, tenantId))
        .thenReturn(0L);

    // When
    var result = emailVerificationService.hasUnusedTokens(userId, tenantId);

    // Then
    assertFalse(result);
  }

  @Test
  void getMostRecentUnusedToken_WithExistingToken_ShouldReturnToken() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.findMostRecentUnusedTokenByUserIdAndTenantId(userId, tenantId))
        .thenReturn(Optional.of(testToken));

    // When
    var result = emailVerificationService.getMostRecentUnusedToken(userId, tenantId);

    // Then
    assertNotNull(result);
    assertEquals(testToken.getToken(), result.getToken());
  }

  @Test
  void getMostRecentUnusedToken_WithNoToken_ShouldReturnNull() {
    // Given
    var userId = 1L;
    var tenantId = "tenant-1";
    when(tokenRepository.findMostRecentUnusedTokenByUserIdAndTenantId(userId, tenantId))
        .thenReturn(Optional.empty());

    // When
    var result = emailVerificationService.getMostRecentUnusedToken(userId, tenantId);

    // Then
    assertNull(result);
  }

  @Test
  void verifyEmail_WithNullToken_ShouldThrowException() {
    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.verifyEmail(null));

    assertEquals("Verification token cannot be null or empty", exception.getMessage());
  }

  @Test
  void verifyEmail_WithEmptyToken_ShouldThrowException() {
    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.verifyEmail(""));

    assertEquals("Verification token cannot be null or empty", exception.getMessage());
  }

  @Test
  void resendVerificationEmail_WithNullEmail_ShouldThrowException() {
    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.resendVerificationEmail(null, "127.0.0.1"));

    assertEquals("Email address cannot be null or empty", exception.getMessage());
  }

  @Test
  void resendVerificationEmail_WithEmptyEmail_ShouldThrowException() {
    // When & Then
    var exception = assertThrows(EmailVerificationService.EmailVerificationException.class,
        () -> emailVerificationService.resendVerificationEmail("", "127.0.0.1"));

    assertEquals("Email address cannot be null or empty", exception.getMessage());
  }

  private User createTestUser(Long id, String username, String email, String tenantId) {
    var user = new User(username, email, "hashedPassword", "Test", "User", tenantId);

    // Use reflection to set the ID for testing
    try {
      var idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (Exception e) {
      // Ignore for test purposes
    }

    return user;
  }
}