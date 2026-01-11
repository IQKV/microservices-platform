package com.iqscaffold.userservice.authentication;

import com.iqscaffold.userservice.usermanagement.User;

/**
 * Interface for authentication services providing secure user authentication,
 * token management, and session handling with comprehensive security measures.
 *
 * <p>This interface defines the contract for authentication operations including:
 * <ul>
 *   <li>User authentication with brute force protection</li>
 *   <li>JWT token generation and refresh</li>
 *   <li>Password change operations</li>
 *   <li>Session management and logout</li>
 *   <li>Security audit logging</li>
 * </ul>
 *
 * <h4>Security Features:</h4>
 * <ul>
 *   <li><strong>Account Lockout</strong> - Protection against brute force attacks</li>
 *   <li><strong>Input Sanitization</strong> - Prevention of injection attacks</li>
 *   <li><strong>Audit Logging</strong> - Security event tracking</li>
 *   <li><strong>Token Management</strong> - Secure JWT lifecycle management</li>
 * </ul>
 */
public interface AuthenticationService {

  /**
   * Authenticate a user with security measures and audit logging.
   *
   * @param request   The login request containing username/email and password
   * @param ipAddress The client's IP address for security logging
   * @param userAgent The client's User-Agent header for device identification
   * @return TokenResponse containing access token, refresh token, and user context
   * @throws AuthenticationException            If credentials are invalid or authentication fails
   * @throws AccountLockedException             If the account is locked due to failed attempts
   * @throws EmailVerificationRequiredException If email verification is required
   */
  TokenResponse authenticateUser(LoginRequest request, String ipAddress, String userAgent);

  /**
   * Change the password for an authenticated user with security validation.
   *
   * @param userId          The ID of the user changing password
   * @param currentPassword The current password for verification
   * @param newPassword     The new password to set
   * @param clientIp        The client IP address for audit logging
   * @throws AuthenticationException If current password is invalid or change fails
   */
  void changePassword(Long userId, String currentPassword, String newPassword, String clientIp);

  /**
   * Refresh access token using a valid refresh token.
   *
   * @param request The refresh token request
   * @return New access token and user context
   * @throws AuthenticationException If the refresh token is invalid or expired
   */
  TokenResponse refreshToken(RefreshTokenRequest request);

  /**
   * Logout user by invalidating current session tokens.
   *
   * @param accessToken The access token to invalidate
   * @param sessionId   The session ID to invalidate
   */
  void logoutUser(String accessToken, String sessionId);

  /**
   * Log out user from all sessions by invalidating all tokens.
   *
   * @param userId The user ID to log out from all sessions
   */
  void logoutAllUserSessions(Long userId);

  /**
   * Log out user from all devices by revoking all refresh tokens and sessions.
   *
   * @param userId The user ID to log out from all devices
   */
  void logoutFromAllDevices(Long userId);

  /**
   * Validate session and extend if needed.
   *
   * @param sessionId The session ID to validate
   * @return true if the session is valid and extended, false otherwise
   */
  boolean validateAndExtendSession(String sessionId);

  /**
   * Get active sessions for a user.
   *
   * @param userId The user ID to get sessions for
   * @return Set of active session objects
   */
  java.util.Set<Object> getUserActiveSessions(Long userId);

  /**
   * Handle authentication success using pattern matching.
   *
   * @param user         The authenticated user
   * @param accessToken  The generated access token
   * @param refreshToken The generated refresh token
   * @return Authentication success result
   */
  AuthenticationResult.Success createAuthenticationSuccess(User user, String accessToken, String refreshToken);

  /**
   * Handle authentication failure using pattern matching.
   *
   * @param reason    The failure reason
   * @param errorCode The error code
   * @return Authentication failure result
   */
  AuthenticationResult.Failure createAuthenticationFailure(String reason, String errorCode);

  /**
   * Update a user's preferred locale setting.
   *
   * @param userId The user ID whose locale to update
   * @param locale The new preferred locale (e.g., "en", "es", "fr")
   * @throws AuthenticationException If user is not found or locale is invalid
   */
  void updateUserLocale(Long userId, String locale);
}
