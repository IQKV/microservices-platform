package org.gripday.authservice.domain.service;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.gripday.authservice.config.RedisConfig.TenantAwareRedisService;
import org.gripday.authservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.AuthenticationResult;
import org.gripday.authservice.presentation.dto.LoginRequest;
import org.gripday.authservice.presentation.dto.RefreshTokenRequest;
import org.gripday.authservice.presentation.dto.TokenResponse;
import org.gripday.authservice.presentation.dto.UserContext;
import org.gripday.authservice.presentation.validation.InputSanitizer;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enhanced service for user authentication with security measures. Includes account lockout, audit logging, and input sanitization.
 */
@Service
@Transactional
public class AuthenticationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AccountLockoutService accountLockoutService;
  private final SecurityAuditService securityAuditService;
  private final InputSanitizer inputSanitizer;
  private final TenantAwareSessionService sessionService;
  private final TenantAwareRedisService redisService;
  private final EmailService emailService;
  private final MeterRegistry meterRegistry;

  public AuthenticationService(final UserRepository userRepository,
      final PasswordEncoder passwordEncoder,
      final JwtService jwtService,
      final AccountLockoutService accountLockoutService,
      final SecurityAuditService securityAuditService,
      final InputSanitizer inputSanitizer,
      final TenantAwareSessionService sessionService,
      final TenantAwareRedisService redisService,
      final EmailService emailService,
      final MeterRegistry meterRegistry) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.accountLockoutService = accountLockoutService;
    this.securityAuditService = securityAuditService;
    this.inputSanitizer = inputSanitizer;
    this.sessionService = sessionService;
    this.redisService = redisService;
    this.emailService = emailService;
    this.meterRegistry = meterRegistry;
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
        throw new AuthenticationException("Invalid reset token");
      }

      // Lookup token -> userId
      var tokenKey = "password-reset:token:" + sanitizedToken;
      Object userIdObj = redisService.get(tokenKey);
      if (userIdObj == null) {
        throw new AuthenticationException("Invalid or expired reset token");
      }

      Long userId = (userIdObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(userIdObj));

      var userOpt = userRepository.findById(userId);
      if (userOpt.isEmpty()) {
        throw new AuthenticationException("User not found");
      }

      var user = userOpt.get();

      // Validate basic password requirements (length >= 8); stronger checks can be added
      if (sanitizedPassword == null || sanitizedPassword.length() < 8) {
        throw new AuthenticationException("Password does not meet minimum requirements");
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

      // Audit
      securityAuditService.logTokenEvent(user.getUsername(), "password_reset_completed", clientIp, "system");
      meterRegistry.counter("auth.password_reset.completed").increment();
    } catch (final AuthenticationException e) {
      throw e;
    } catch (final Exception e) {
      throw new AuthenticationException("Password reset failed", e);
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Authenticate user with enhanced security measures. Includes account lockout, audit logging, and input sanitization.
   */
  public TokenResponse authenticateUser(LoginRequest request, String ipAddress, String userAgent) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);

    try {
      // Sanitize input to prevent injection attacks
      var sanitizedUsername = inputSanitizer.sanitizeInput(request.username());

      // Check for suspicious input patterns
      if (!inputSanitizer.isInputSafe(request.username())
          || inputSanitizer.containsSqlInjection(request.username())) {
        securityAuditService.logSuspiciousActivity(
            sanitizedUsername, "Potential injection attempt in username", ipAddress, userAgent);
        throw new AuthenticationException("Invalid input detected");
      }

      // Check if account is locked
      if (accountLockoutService.isAccountLocked(sanitizedUsername)) {
        var timeUntilUnlock = accountLockoutService.getTimeUntilUnlock(sanitizedUsername);
        securityAuditService.logFailedAuthentication(
            sanitizedUsername, "Account locked", ipAddress, userAgent);
        throw new AccountLockedException("Account is locked. Try again in " +
            timeUntilUnlock.toMinutes() + " minutes");
      }

      // Find user by username or email using var
      var userOptional = userRepository.findByUsernameOrEmail(
          sanitizedUsername,
          sanitizedUsername
      );

      if (userOptional.isEmpty()) {
        // Record failed attempt even for non-existent users to prevent enumeration
        accountLockoutService.recordFailedAttempt(sanitizedUsername);
        securityAuditService.logFailedAuthentication(
            sanitizedUsername, "User not found", ipAddress, userAgent);
        throw new AuthenticationException("Invalid username or password");
      }

      var user = userOptional.get();

      // Verify password
      if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        // Record failed attempt
        var shouldLock = accountLockoutService.recordFailedAttempt(user.getUsername());

        if (shouldLock) {
          var failedAttempts = accountLockoutService.getFailedAttempts(user.getUsername());
          securityAuditService.logAccountLockout(
              user.getUsername(), failedAttempts, ipAddress, userAgent);
        } else {
          securityAuditService.logFailedAuthentication(
              user.getUsername(), "Invalid password", ipAddress, userAgent);
        }

        throw new AuthenticationException("Invalid username or password");
      }

      // Check if user is enabled
      if (!user.getEnabled()) {
        securityAuditService.logFailedAuthentication(
            user.getUsername(), "Account disabled", ipAddress, userAgent);
        throw new AuthenticationException("Account is disabled");
      }

      // Check if email is verified
      if (user.getEmailVerified() == null || !user.getEmailVerified()) {
        securityAuditService.logFailedAuthentication(
            user.getUsername(), "Email not verified", ipAddress, userAgent);
        throw new EmailVerificationRequiredException("Email verification required");
      }

      // Clear failed attempts on successful authentication
      accountLockoutService.clearFailedAttempts(user.getUsername());

      // Generate tokens
      var accessToken = jwtService.generateAccessToken(user);
      var refreshToken = jwtService.generateRefreshToken(user);

      // Create session with tenant isolation
      var sessionId = UUID.randomUUID().toString();
      var sessionData = createSessionData(user, ipAddress, userAgent);
      var sessionTimeout = request.rememberMe()
          ? java.time.Duration.ofDays(7) : java.time.Duration.ofMinutes(30);

      sessionService.storeSession(sessionId, sessionData, sessionTimeout);
      sessionService.addUserSession(user.getId().toString(), sessionId);

      // Log successful authentication
      securityAuditService.logSuccessfulAuthentication(user.getUsername(), ipAddress, userAgent);
      securityAuditService.logTokenEvent(user.getUsername(), "generated", ipAddress, userAgent);

      // Create user context
      var userContext = createUserContext(user);

      // Determine token expiry based on rememberMe flag
      var expiresIn = request.rememberMe() ? 604800L : 900L; // 7 days or 15 minutes

      return new TokenResponse(accessToken, refreshToken, expiresIn, userContext, sessionId);

    } catch (final AuthenticationException | AccountLockedException | EmailVerificationRequiredException e) {
      throw e;
    } catch (final Exception e) {
      securityAuditService.logFailedAuthentication(
          request.username(), "System error: " + e.getMessage(), ipAddress, userAgent);
      throw new AuthenticationException("Authentication failed", e);
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
        throw new AuthenticationException("Invalid input detected");
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
      System.err.println("Error initiating password reset: " + e.getMessage());
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Refresh JWT access token using refresh token.
   */
  public TokenResponse refreshToken(RefreshTokenRequest request) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);

    try {
      // Validate refresh token
      var jwt = jwtService.validateToken(request.refreshToken());

      // Check token type
      var tokenType = jwt.getClaimAsString("type");
      if (!"refresh".equals(tokenType)) {
        throw new AuthenticationException("Invalid token type");
      }

      // Get user ID from token
      var userId = Long.parseLong(jwt.getSubject());

      // Enforce user-wide refresh token revocation
      var issuedAt = jwt.getIssuedAt();
      if (jwtService.isUserRefreshRevoked(String.valueOf(userId), issuedAt)) {
        throw new AuthenticationException("Refresh tokens have been revoked for this user");
      }

      var userOptional = userRepository.findById(userId);

      if (userOptional.isEmpty()) {
        throw new AuthenticationException("User not found");
      }

      var user = userOptional.get();

      // Check if user is still enabled
      if (!user.getEnabled()) {
        throw new AuthenticationException("Account is disabled");
      }

      // Generate new access token
      var newAccessToken = jwtService.generateAccessToken(user);
      var userContext = createUserContext(user);

      return new TokenResponse(
          newAccessToken,
          request.refreshToken(), // Keep the same refresh token
          900L, // 15 minutes
          userContext
      );

    } catch (final JwtException e) {
      throw new AuthenticationException("Invalid or expired refresh token", e);
    } catch (final Exception e) {
      throw new AuthenticationException("Token refresh failed", e);
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Logout user and invalidate tokens and session.
   */
  @CacheEvict(value = "jwt-blacklist", key = "#accessToken")
  public void logoutUser(String accessToken, String sessionId) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);

    try {
      // Invalidate the access token
      jwtService.invalidateToken(accessToken);

      // Invalidate session if provided
      if (sessionId != null && !sessionId.trim().isEmpty()) {
        sessionService.deleteSession(sessionId);

        // Extract user ID from token to remove user session mapping
        try {
          var jwt = jwtService.validateToken(accessToken);
          var userId = jwt.getSubject();
          sessionService.removeUserSession(userId, sessionId);
        } catch (final Exception e) {
          // Log but don't fail logout if we can't extract user ID
          System.err.println("Could not extract user ID for session cleanup: " + e.getMessage());
        }
      }

    } catch (final Exception e) {
      // Log error but don't throw exception for logout
      System.err.println("Error during logout: " + e.getMessage());
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Logout all sessions for a user (admin function or security measure).
   */
  @CacheEvict(value = {"jwt-blacklist", "users"}, allEntries = true)
  public void logoutAllUserSessions(Long userId) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);

    try {
      // Invalidate all sessions for the user
      sessionService.invalidateAllUserSessions(userId.toString());

      // Log security event
      var userOpt = userRepository.findById(userId);
      userOpt.ifPresent(user -> securityAuditService.logTokenEvent(
          user.getUsername(), "logout_from_all_devices", "system", "system"
      ));
      meterRegistry.counter("auth.logout.all").increment();
    } catch (final Exception e) {
      System.err.println("Error during logout from all devices: " + e.getMessage());
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Logout user from all devices by revoking all refresh tokens and sessions.
   */
  public void logoutFromAllDevices(Long userId) {
    var correlationId = generateCorrelationId();
    MDC.put("correlationId", correlationId);

    try {
      // Revoke all refresh tokens for the user
      jwtService.revokeAllRefreshTokensForUser(userId.toString());

      // Logout from all sessions
      logoutAllUserSessions(userId);

      // Log security event
      var userOpt = userRepository.findById(userId);
      userOpt.ifPresent(user -> securityAuditService.logTokenEvent(
          user.getUsername(), "logout_from_all_devices", "user", "web"
      ));

      meterRegistry.counter("auth.logout.all_devices").increment();
    } catch (final Exception e) {
      System.err.println("Error during logout from all devices: " + e.getMessage());
    } finally {
      MDC.remove("correlationId");
    }
  }

  /**
   * Validate session and extend if needed.
   */
  @Cacheable(value = "sessions", key = "#sessionId")
  public boolean validateAndExtendSession(String sessionId) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      return false;
    }

    try {
      if (sessionService.sessionExists(sessionId)) {
        // Extend session by 30 minutes
        sessionService.extendSession(sessionId, java.time.Duration.ofMinutes(30));
        return true;
      }
      return false;
    } catch (final Exception e) {
      System.err.println("Error validating session: " + e.getMessage());
      return false;
    }
  }

  /**
   * Get active sessions for a user.
   */
  @Cacheable(value = "sessions", key = "'user_sessions_' + #userId")
  public java.util.Set<Object> getUserActiveSessions(Long userId) {
    try {
      return sessionService.getUserSessions(userId.toString());
    } catch (final Exception e) {
      System.err.println("Error getting user sessions: " + e.getMessage());
      return java.util.Set.of();
    }
  }

  /**
   * Handle authentication success using pattern matching.
   */
  public AuthenticationResult.Success createAuthenticationSuccess(User user, String accessToken, String refreshToken) {
    var userContext = createUserContext(user);
    var correlationId = MDC.get("correlationId");

    return new AuthenticationResult.Success(
        userContext,
        accessToken,
        refreshToken,
        correlationId,
        Instant.now()
    );
  }

  /**
   * Handle authentication failure using pattern matching.
   */
  public AuthenticationResult.Failure createAuthenticationFailure(String reason, String errorCode) {
    var correlationId = MDC.get("correlationId");

    return new AuthenticationResult.Failure(
        reason,
        errorCode,
        correlationId,
        Instant.now()
    );
  }

  /**
   * Create user context from User entity using var and modern syntax.
   */
  private UserContext createUserContext(User user) {
    var roles = user.getAuthorities().stream()
        .map(authority -> authority.getName())
        .collect(java.util.stream.Collectors.toSet());

    return new UserContext(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles,
        java.util.Set.of(), // Permissions derived from roles
        user.getFirstName(),
        user.getLastName(),
        user.getTenantId(),
        java.util.Map.of()
    );
  }

  /**
   * Create session data for Redis storage with tenant isolation.
   */
  private SessionData createSessionData(User user, String ipAddress, String userAgent) {
    return new SessionData(
        user.getId(),
        user.getUsername(),
        user.getTenantId(),
        ipAddress,
        userAgent,
        Instant.now(),
        Instant.now()
    );
  }

  /**
   * Generate correlation ID for request tracking.
   */
  private String generateCorrelationId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Session data record for Redis storage.
   */
  public record SessionData(
      Long userId,
      String username,
      String tenantId,
      String ipAddress,
      String userAgent,
      Instant createdAt,
      Instant lastAccessedAt
  ) {

  }

  /**
   * Custom exception for authentication errors.
   */
  public static class AuthenticationException extends RuntimeException {

    public AuthenticationException(final String message) {
      super(message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Custom exception for account lockout scenarios.
   */
  public static class AccountLockedException extends RuntimeException {

    public AccountLockedException(final String message) {
      super(message);
    }
  }

  /**
   * Custom exception for email verification required scenarios.
   */
  public static class EmailVerificationRequiredException extends RuntimeException {

    public EmailVerificationRequiredException(final String message) {
      super(message);
    }
  }
}