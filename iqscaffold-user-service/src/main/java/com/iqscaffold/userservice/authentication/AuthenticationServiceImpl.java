package com.iqscaffold.userservice.authentication;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.iqscaffold.userservice.config.IqScaffoldProperties;
import com.iqscaffold.userservice.config.RedisConfig.TenantAwareSessionService;
import com.iqscaffold.userservice.security.AccountLockoutService;
import com.iqscaffold.userservice.security.InputSanitizer;
import com.iqscaffold.userservice.security.SecurityAuditService;
import com.iqscaffold.userservice.shared.UserServiceConstants;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserContext;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthenticationService providing secure user authentication with comprehensive security measures.
 *
 * <p>This service implements enterprise-grade authentication patterns including:
 * <ul>
 *   <li><strong>Account Lockout Protection</strong> - Prevents brute force attacks by locking accounts after failed attempts</li>
 *   <li><strong>Input Sanitization</strong> - Protects against injection attacks and malicious input</li>
 *   <li><strong>Security Audit Logging</strong> - Comprehensive logging of all authentication events</li>
 *   <li><strong>Session Management</strong> - Distributed session tracking with Redis</li>
 *   <li><strong>Multi-Tenant Support</strong> - Tenant-aware authentication with context isolation</li>
 *   <li><strong>JWT Token Management</strong> - Secure token generation with refresh capabilities</li>
 * </ul>
 *
 * @see JwtService
 * @see AccountLockoutService
 * @see SecurityAuditService
 */
@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AccountLockoutService accountLockoutService;
  private final SecurityAuditService securityAuditService;
  private final InputSanitizer inputSanitizer;
  private final TenantAwareSessionService sessionService;
  private final MeterRegistry meterRegistry;
  private final IqScaffoldProperties iqScaffoldProperties;
  private final com.iqscaffold.userservice.organization.OrganizationRepository organizationRepository;

  public AuthenticationServiceImpl(final UserRepository userRepository,
                                   final PasswordEncoder passwordEncoder,
                                   final JwtService jwtService,
                                   final AccountLockoutService accountLockoutService,
                                   final SecurityAuditService securityAuditService,
                                   final InputSanitizer inputSanitizer,
                                   final TenantAwareSessionService sessionService,
                                   final MeterRegistry meterRegistry,
                                   final IqScaffoldProperties iqScaffoldProperties,
                                   final com.iqscaffold.userservice.organization.OrganizationRepository organizationRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.accountLockoutService = accountLockoutService;
    this.securityAuditService = securityAuditService;
    this.inputSanitizer = inputSanitizer;
    this.sessionService = sessionService;
    this.meterRegistry = meterRegistry;
    this.iqScaffoldProperties = iqScaffoldProperties;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public TokenResponse authenticateUser(LoginRequest request, String ipAddress, String userAgent) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);

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
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
  public void changePassword(Long userId, String currentPassword, String newPassword, String clientIp) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);
    try {
      // Sanitize inputs
      var sanitizedCurrentPassword = inputSanitizer.sanitizeInput(currentPassword);
      var sanitizedNewPassword = inputSanitizer.sanitizeInput(newPassword);

      // Find user
      var userOpt = userRepository.findById(userId);
      if (userOpt.isEmpty()) {
        throw new AuthenticationException("User not found");
      }

      var user = userOpt.get();

      // Verify current password
      if (!passwordEncoder.matches(sanitizedCurrentPassword, user.getPasswordHash())) {
        securityAuditService.logFailedAuthentication(
            user.getUsername(), "Invalid current password during password change", clientIp, "web");
        throw new AuthenticationException("Current password is incorrect");
      }

      // Validate new password requirements (length >= 8)
      if (sanitizedNewPassword == null || sanitizedNewPassword.length() < 8) {
        throw new AuthenticationException("New password does not meet minimum requirements");
      }

      // Ensure new password is different from current
      if (passwordEncoder.matches(sanitizedNewPassword, user.getPasswordHash())) {
        throw new AuthenticationException("New password must be different from current password");
      }

      // Update password
      user.setPasswordHash(passwordEncoder.encode(sanitizedNewPassword));
      userRepository.save(user);

      // Revoke all refresh tokens for the user
      jwtService.revokeAllRefreshTokensForUser(String.valueOf(userId));

      // Invalidate all sessions for the user
      sessionService.invalidateAllUserSessions(String.valueOf(userId));

      // Audit
      securityAuditService.logTokenEvent(user.getUsername(), "password_changed", clientIp, "web");
      meterRegistry.counter("auth.password.changed").increment();

      logger.info("Password changed successfully for user: {} ({})", user.getUsername(), user.getEmail());
    } catch (final AuthenticationException e) {
      throw e;
    } catch (final Exception e) {
      throw new AuthenticationException("Password change failed", e);
    } finally {
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
  public TokenResponse refreshToken(RefreshTokenRequest request) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);

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
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
  @CacheEvict(value = "jwt-blacklist", key = "#accessToken")
  public void logoutUser(String accessToken, String sessionId) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);

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
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
  @CacheEvict(value = {"jwt-blacklist", "users"}, allEntries = true)
  public void logoutAllUserSessions(Long userId) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);

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
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
  public void logoutFromAllDevices(Long userId) {
    var correlationId = generateCorrelationId();
    MDC.put(UserServiceConstants.MDC.CORRELATION_ID, correlationId);

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
      MDC.remove(UserServiceConstants.MDC.CORRELATION_ID);
    }
  }

  @Override
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

  @Override
  @Cacheable(value = "sessions", key = "'user_sessions_' + #userId")
  public java.util.Set<Object> getUserActiveSessions(Long userId) {
    try {
      return sessionService.getUserSessions(userId.toString());
    } catch (final Exception e) {
      System.err.println("Error getting user sessions: " + e.getMessage());
      return java.util.Set.of();
    }
  }

  @Override
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

  @Override
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
    Set<String> roles = user.getAuthorities().stream()
        .map(authority -> authority.getName())
        .collect(java.util.stream.Collectors.toSet());

    // Get organization ID from tenant (1:1 relationship)
    String tenantId = user.getTenantId();
    Long organizationId = null;
    if (tenantId != null) {
      organizationId = organizationRepository.findByTenantId(tenantId)
          .map(com.iqscaffold.userservice.organization.Organization::getId)
          .orElse(null);
    }

    return new UserContext(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles,
        java.util.Set.of(), // Permissions derived from roles
        user.getFirstName(),
        user.getLastName(),
        user.getTenantId(),
        organizationId,
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

  @Override
  @Transactional
  @CacheEvict(value = "users", key = "#userId")
  public void updateUserLocale(Long userId, String locale) {
    logger.debug("Updating locale for user ID: {} to: {}", userId, locale);

    var user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationException("User not found"));

    // Validate locale using configuration
    var i18nConfig = iqScaffoldProperties.i18n();
    if (!i18nConfig.isLocaleSupported(locale)) {
      throw new AuthenticationException("Unsupported locale: " + locale +
                                        ". Supported locales: " + i18nConfig.supportedLocales());
    }

    // Update user's preferred locale
    user.setPreferredLocale(locale);
    userRepository.save(user);

    // Log the change for audit purposes
    securityAuditService.logUserLocaleChange(userId, user.getUsername(), locale);

    logger.info("Successfully updated locale for user: {} to: {}", user.getUsername(), locale);
  }
}
