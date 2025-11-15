package org.gripday.userservice.domain.service;

import jakarta.validation.constraints.NotBlank;

import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.userservice.config.RedisConfig.TenantAwareRedisService;
import org.gripday.userservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.userservice.infrastructure.repository.UserRepository;
import org.gripday.userservice.presentation.validation.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for password reset functionality. Handles password reset initiation and completion with security measures.
 */
@Service
@Transactional
public class PasswordResetService {

  private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecurityAuditService securityAuditService;
  private final InputSanitizer inputSanitizer;
  private final TenantAwareSessionService sessionService;
  private final TenantAwareRedisService redisService;
  private final EmailService emailService;
  private final MeterRegistry meterRegistry;

  public PasswordResetService(
      final UserRepository userRepository,
      final PasswordEncoder passwordEncoder,
      final JwtService jwtService,
      final SecurityAuditService securityAuditService,
      final InputSanitizer inputSanitizer,
      final TenantAwareSessionService sessionService,
      final TenantAwareRedisService redisService,
      final EmailService emailService,
      final MeterRegistry meterRegistry) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.securityAuditService = securityAuditService;
    this.inputSanitizer = inputSanitizer;
    this.sessionService = sessionService;
    this.redisService = redisService;
    this.emailService = emailService;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Validate if a reset token is valid and not expired.
   * 
   * @param resetToken the token to validate
   * @return true if token is valid, false otherwise
   */
  public boolean isResetTokenValid(@NotBlank String resetToken) {
    try {
      var sanitizedToken = inputSanitizer.sanitizeInput(resetToken);
      
      if (!inputSanitizer.isInputSafe(sanitizedToken)) {
        return false;
      }

      var tokenKey = "password-reset:token:" + sanitizedToken;
      Object userIdObj = redisService.get(tokenKey);
      
      if (userIdObj == null) {
        return false;
      }

      Long userId = (userIdObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(userIdObj));
      
      // Verify user still exists
      return userRepository.findById(userId).isPresent();
    } catch (final Exception e) {
      logger.debug("Token validation failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Reset password using a valid reset token. Revokes all refresh tokens and sessions.
   */
  public void resetPassword(@NotBlank String resetToken, @NotBlank String newPassword, String clientIp) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);
    try {
      var sanitizedToken = inputSanitizer.sanitizeInput(resetToken);
      var sanitizedPassword = inputSanitizer.sanitizeInput(newPassword);

      if (!inputSanitizer.isInputSafe(sanitizedToken)) {
        throw new PasswordResetException("Invalid reset token");
      }

      // Lookup token -> userId
      var tokenKey = "password-reset:token:" + sanitizedToken;
      Object userIdObj = redisService.get(tokenKey);
      if (userIdObj == null) {
        throw new PasswordResetException("Invalid or expired reset token");
      }

      Long userId = (userIdObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(userIdObj));

      var userOpt = userRepository.findById(userId);
      if (userOpt.isEmpty()) {
        throw new PasswordResetException("User not found");
      }

      var user = userOpt.get();

      // Validate basic password requirements (length >= 8); stronger checks can be added
      if (sanitizedPassword == null || sanitizedPassword.length() < 8) {
        throw new PasswordResetException("Password does not meet minimum requirements");
      }

      // Update password
      user.setPasswordHash(passwordEncoder.encode(sanitizedPassword));
      userRepository.save(user);

      // Revoke all refresh tokens for the user
      jwtService.revokeAllRefreshTokensForUser(String.valueOf(userId));

      // Invalidate all sessions for the user
      sessionService.invalidateAllUserSessions(String.valueOf(userId));

      // Delete the token so it cannot be reused
      redisService.delete(tokenKey);

      // Also delete the last-token mapping if present
      redisService.delete("password-reset:user:" + user.getId());

      // Send password reset confirmation email
      try {
        emailService.sendPasswordResetConfirmedEmail(user);
        logger.info("Password reset confirmation email sent successfully to user: {} ({})",
            user.getUsername(), user.getEmail());
      } catch (final Exception e) {
        // Log error but don't fail the password reset process
        logger.error("Failed to send password reset confirmation email to user: {} ({}), but password reset was successful",
            user.getUsername(), user.getEmail(), e);
      }

      // Audit
      securityAuditService.logTokenEvent(user.getUsername(), "password_reset_completed", clientIp, "system");
      meterRegistry.counter("auth.password_reset.completed").increment();
    } catch (final PasswordResetException e) {
      throw e;
    } catch (final Exception e) {
      throw new PasswordResetException("Password reset failed", e);
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Initiate password reset flow by generating a one-time token and sending an email. This method is idempotent and always returns successfully to avoid user enumeration.
   */
  public void initiatePasswordReset(String email, String ipAddress, String userAgent) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);
    try {
      var sanitizedEmail = inputSanitizer.sanitizeInput(email);
      // Basic sanity check
      if (!inputSanitizer.isInputSafe(sanitizedEmail)) {
        throw new PasswordResetException("Invalid input detected");
      }

      var userOpt = userRepository.findByEmail(sanitizedEmail);
      if (userOpt.isEmpty()) {
        // Do not reveal existence; log minimal info
        securityAuditService.logSuspiciousActivity(null,
            "Password reset requested for unknown email", ipAddress, userAgent);
        meterRegistry.counter("auth.password_reset.initiated").increment();
        return;
      }

      var user = userOpt.get();

      // Generate secure random token
      var token = java.util.UUID.randomUUID().toString();

      // Store mapping token -> userId with TTL (e.g., 30 minutes)
      var tokenKey = "password-reset:token:" + token;
      redisService.set(tokenKey, user.getId(), java.time.Duration.ofMinutes(30));

      // Optionally keep last token per user (to invalidate old ones)
      var userKey = "password-reset:user:" + user.getId();
      redisService.set(userKey, token, java.time.Duration.ofMinutes(30));

      // Send email with reset link
      emailService.sendPasswordResetEmail(user, token);

      // Audit
      securityAuditService.logTokenEvent(user.getUsername(), "password_reset_initiated", ipAddress, userAgent);
    } catch (final Exception e) {
      // Intentionally do not leak errors to caller; log and return
      logger.error("Error initiating password reset: {}", e.getMessage());
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Generate correlation ID for request tracking.
   */
  private String generateCorrelationId() {
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Custom exception for password reset errors.
   */
  public static class PasswordResetException extends RuntimeException {

    public PasswordResetException(final String message) {
      super(message);
    }

    public PasswordResetException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
