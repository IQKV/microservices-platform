package com.iqscaffold.userservice.security;

/**
 * Interface for security audit logging service providing comprehensive security event tracking.
 *
 * <p>This interface defines the contract for security audit operations including:
 * <ul>
 *   <li>Authentication event logging (success/failure)</li>
 *   <li>Account security events (lockouts, password changes)</li>
 *   <li>User lifecycle events (registration, profile changes)</li>
 *   <li>Suspicious activity detection and logging</li>
 *   <li>Token lifecycle events</li>
 * </ul>
 *
 * <h4>Security Benefits:</h4>
 * <ul>
 *   <li><strong>Compliance</strong> - Required for security frameworks (SOX, PCI-DSS)</li>
 *   <li><strong>Forensic Analysis</strong> - Timeline reconstruction for security incidents</li>
 *   <li><strong>Threat Detection</strong> - Pattern analysis for anomaly detection</li>
 *   <li><strong>Monitoring</strong> - Real-time security event tracking</li>
 * </ul>
 *
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public interface SecurityAuditService {

  /**
   * Log successful authentication event with comprehensive context capture.
   *
   * @param username  The username of the successfully authenticated user
   * @param ipAddress The client's IP address for network context
   * @param userAgent The client's User-Agent header for device identification
   */
  void logSuccessfulAuthentication(String username, String ipAddress, String userAgent);

  /**
   * Log failed authentication attempt with detailed context and reason.
   *
   * @param username  The attempted username (may not exist)
   * @param reason    The specific reason for authentication failure
   * @param ipAddress The client's IP address for tracking
   * @param userAgent The client's User-Agent header
   */
  void logFailedAuthentication(String username, String reason, String ipAddress, String userAgent);

  /**
   * Log account lockout event due to excessive failed attempts.
   *
   * @param username       The username of the locked account
   * @param failedAttempts The number of failed attempts that triggered lockout
   * @param ipAddress      The client's IP address
   * @param userAgent      The client's User-Agent header
   */
  void logAccountLockout(String username, int failedAttempts, String ipAddress, String userAgent);

  /**
   * Log password change event for security tracking.
   *
   * @param username  The username of the user changing password
   * @param ipAddress The client's IP address
   * @param userAgent The client's User-Agent header
   */
  void logPasswordChange(String username, String ipAddress, String userAgent);

  /**
   * Log user registration event for audit trail.
   *
   * @param username  The new user's username
   * @param email     The new user's email address
   * @param ipAddress The client's IP address
   * @param userAgent The client's User-Agent header
   */
  void logUserRegistration(String username, String email, String ipAddress, String userAgent);

  /**
   * Log rate limiting event when limits are exceeded.
   *
   * @param ipAddress The client's IP address that exceeded limits
   * @param userAgent The client's User-Agent header
   * @param endpoint  The endpoint that was rate limited
   */
  void logRateLimitExceeded(String ipAddress, String userAgent, String endpoint);

  /**
   * Log suspicious activity for security monitoring.
   *
   * @param username  The username associated with suspicious activity
   * @param activity  Description of the suspicious activity
   * @param ipAddress The client's IP address
   * @param userAgent The client's User-Agent header
   */
  void logSuspiciousActivity(String username, String activity, String ipAddress, String userAgent);

  /**
   * Log JWT token lifecycle events (generation, refresh, revocation).
   *
   * @param username  The username associated with the token event
   * @param action    The token action (generated, refreshed, revoked, etc.)
   * @param ipAddress The client's IP address
   * @param userAgent The client's User-Agent header
   */
  void logTokenEvent(String username, String action, String ipAddress, String userAgent);

  /**
   * Log user locale preference change for audit trail.
   *
   * @param userId    The user ID whose locale was changed
   * @param username  The username of the user
   * @param newLocale The new locale preference
   */
  void logUserLocaleChange(Long userId, String username, String newLocale);
}
