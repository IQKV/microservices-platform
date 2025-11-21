package org.gripday.userservice.emailverification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.gripday.userservice.shared.EmailOperations;
import org.gripday.userservice.usermanagement.User;
import org.gripday.userservice.usermanagement.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing email verification tokens and user email activation. Handles token generation, validation, rate limiting, and cleanup operations.
 */
@Service
@Transactional
public class EmailVerificationService {

  private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

  // Rate limiting configuration - maximum 3 emails per hour per user
  private static final int MAX_EMAILS_PER_HOUR = 3;
  private static final int TOKEN_EXPIRY_HOURS = 24;
  private static final int CLEANUP_EXPIRY_HOURS = 48;

  private final VerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailOperations emailService;
  private final VerificationMetrics metricsService;

  public EmailVerificationService(
      final VerificationTokenRepository tokenRepository,
      final UserRepository userRepository,
      final EmailOperations emailService,
      final VerificationMetrics metricsService) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
    this.metricsService = metricsService;
  }

  /**
   * Generate a new verification token for the user and send verification email. Implements rate limiting to prevent email spam.
   *
   * @param user The user to generate verification token for
   * @return The generated verification token
   * @throws EmailVerificationException if rate limit exceeded or user already verified
   */
  public String generateVerificationToken(User user) {
    if (user.getEmailVerified() != null && user.getEmailVerified()) {
      throw new EmailVerificationException("User email is already verified");
    }

    var userId = user.getId();

    // Check rate limiting - max 3 emails per hour
    var oneHourAgo = LocalDateTime.now().minusHours(1);
    var recentTokenCount = tokenRepository.countTokensCreatedSince(userId, oneHourAgo);

    if (recentTokenCount >= MAX_EMAILS_PER_HOUR) {
      throw new EmailVerificationException("Rate limit exceeded. Maximum " + MAX_EMAILS_PER_HOUR + " verification emails per hour");
    }

    // Invalidate any existing unused tokens for this user
    tokenRepository.markAllUnusedTokensAsUsedByUserId(userId);

    // Generate secure token using UUID
    var token = UUID.randomUUID().toString();
    var expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

    // Create and save new verification token
    var verificationToken = new VerificationToken(token, userId, expiresAt, user.getTenantId());
    tokenRepository.save(verificationToken);

    // Send verification email
    try {
      emailService.sendVerificationEmail(user, token);
      logger.info("Verification email sent successfully to user: {} ({})",
          user.getUsername(), user.getEmail());
    } catch (final Exception e) {
      logger.error("Failed to send verification email to user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      // Mark token as used since email failed to send
      verificationToken.markAsUsed();
      tokenRepository.save(verificationToken);
      throw new EmailVerificationException("Failed to send verification email", e);
    }

    return token;
  }

  /**
   * Verify user email using the provided token. Validates token and activates user account if valid.
   *
   * @param token The verification token to validate
   * @return EmailVerificationResponse with verification result
   * @throws EmailVerificationException if token is invalid, expired, or already used
   */
  @Transactional
  public VerificationResponse verifyEmail(String token) {
    if (token == null || token.trim().isEmpty()) {
      metricsService.recordVerificationFailed();
      throw new EmailVerificationException("Verification token cannot be null or empty");
    }

    // Find the verification token
    var verificationToken = tokenRepository.findByTokenAndUsedFalse(token)
        .orElseThrow(() -> {
          metricsService.recordVerificationFailed();
          return new EmailVerificationException("Invalid or already used verification token");
        });

    // Check if token is expired
    if (verificationToken.isExpired()) {
      metricsService.recordVerificationFailed();
      throw new EmailVerificationException("Verification token has expired");
    }

    // Get the user
    var user = userRepository.findById(verificationToken.getUserId())
        .orElseThrow(() -> {
          metricsService.recordVerificationFailed();
          return new EmailVerificationException("User not found for verification token");
        });

    // Verify tenant context matches


    // Check if user is already verified
    if (user.getEmailVerified() != null && user.getEmailVerified()) {
      // Mark token as used even if user is already verified
      verificationToken.markAsUsed();
      tokenRepository.save(verificationToken);
      metricsService.recordVerificationFailed();
      throw new EmailVerificationException("User email is already verified");
    }

    // Mark token as used
    verificationToken.markAsUsed();
    tokenRepository.save(verificationToken);

    // Activate user account
    user.setEmailVerified(true);
    userRepository.save(user);

    // Record successful verification
    metricsService.recordVerificationSuccess();

    logger.info("Email verification successful for user: {} ({})",
        user.getUsername(), user.getEmail());

    // Send welcome email after successful verification
    try {
      emailService.sendRegistrationConfirmedEmail(user);
      logger.info("Welcome email sent successfully to user: {} ({})",
          user.getUsername(), user.getEmail());
    } catch (final Exception e) {
      // Log error but don't fail the verification process
      logger.error("Failed to send welcome email to user: {} ({}), but verification was successful",
          user.getUsername(), user.getEmail(), e);
    }

    return new VerificationResponse(
        true,
        "Email verified successfully",
        user.getUsername(),
        LocalDateTime.now()
    );
  }

  /**
   * Resend verification email for a user. Implements rate limiting and generates new token.
   *
   * @param email     The email address to resend verification to
   * @param ipAddress The IP address of the request for rate limiting
   * @return EmailVerificationResponse with resend result
   * @throws EmailVerificationException if user not found, already verified, or rate limited
   */
  public VerificationResponse resendVerificationEmail(String email, String ipAddress) {
    if (email == null || email.trim().isEmpty()) {
      throw new EmailVerificationException("Email address cannot be null or empty");
    }

    // Find user by email
    var user = userRepository.findByEmail(email.trim())
        .orElseThrow(() -> new EmailVerificationException("User not found with email: " + email));

    // Check tenant context


    // Check if user is already verified
    if (user.getEmailVerified() != null && user.getEmailVerified()) {
      throw new EmailVerificationException("User email is already verified");
    }

    // Generate new verification token (this includes rate limiting check)
    generateVerificationToken(user);

    return new VerificationResponse(
        true,
        "Verification email sent successfully",
        user.getUsername(),
        null
    );
  }

  /**
   * Scheduled cleanup of expired verification tokens. Runs daily at 2 AM to remove tokens older than 48 hours.
   */
  @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
  public void cleanupExpiredTokens() {
    var cutoffTime = LocalDateTime.now().minusHours(CLEANUP_EXPIRY_HOURS);

    try {
      var deletedCount = tokenRepository.deleteByExpiresAtBefore(cutoffTime);

      if (deletedCount > 0) {
        logger.info("Cleaned up {} expired verification tokens older than {} hours",
            deletedCount, CLEANUP_EXPIRY_HOURS);
      }
    } catch (final Exception e) {
      logger.error("Failed to cleanup expired verification tokens", e);
    }
  }

  /**
   * Get the number of remaining verification emails a user can send within the rate limit window.
   *
   * @param userId   The user ID
   * @param tenantId The tenant ID
   * @return Number of remaining emails allowed
   */
  public int getRemainingEmailCount(Long userId, String tenantId) {
    var oneHourAgo = LocalDateTime.now().minusHours(1);
    var recentTokenCount = tokenRepository.countTokensCreatedSince(userId, oneHourAgo);
    return Math.max(0, MAX_EMAILS_PER_HOUR - (int) recentTokenCount);
  }

  /**
   * Check if a user has any unused verification tokens.
   *
   * @param userId   The user ID
   * @param tenantId The tenant ID
   * @return true if user has unused tokens
   */
  public boolean hasUnusedTokens(Long userId, String tenantId) {
    var unusedTokenCount = tokenRepository.countUnusedTokensByUserId(userId);
    return unusedTokenCount > 0;
  }

  /**
   * Get the most recent unused token for a user (for testing purposes).
   *
   * @param userId   The user ID
   * @param tenantId The tenant ID
   * @return The most recent unused token, or null if none exists
   */
  public VerificationToken getMostRecentUnusedToken(Long userId, String tenantId) {
    return tokenRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(userId)
        .orElse(null);
  }

  /**
   * Get email verification status for a user.
   *
   * @param email The email address to check
   * @return VerificationStatusResponse with status information
   * @throws EmailVerificationException if user not found
   */
  public VerificationStatusResponse getVerificationStatus(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new EmailVerificationException("Email address cannot be null or empty");
    }

    // Find user by email
    var user = userRepository.findByEmail(email.trim())
        .orElseThrow(() -> new EmailVerificationException("User not found with email: " + email));

    // Check tenant context


    var isVerified = user.getEmailVerified() != null && user.getEmailVerified();
    var message = isVerified
        ? "Email address is verified and active."
        : "Email verification pending. Please check your inbox.";

    return new VerificationStatusResponse(
        user.getEmail(),
        isVerified,
        user.getCreatedAt(),
        message
    );
  }

  /**
   * Custom exception for email verification operations.
   */
  public static class EmailVerificationException extends RuntimeException {

    public EmailVerificationException(final String message) {
      super(message);
    }

    public EmailVerificationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
