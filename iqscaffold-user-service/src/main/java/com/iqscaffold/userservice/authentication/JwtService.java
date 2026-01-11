package com.iqscaffold.userservice.authentication;

import java.time.Instant;

import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserContext;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Interface for JWT token management service providing secure token generation,
 * validation, and lifecycle management.
 *
 * <p>This interface defines the contract for JWT operations including:
 * <ul>
 *   <li>Token generation (access and refresh tokens)</li>
 *   <li>Token validation and verification</li>
 *   <li>Token blacklisting and revocation</li>
 *   <li>User context extraction from tokens</li>
 * </ul>
 *
 * <h4>Token Types:</h4>
 * <ul>
 *   <li><strong>Access Tokens</strong> - Short-lived tokens containing user context</li>
 *   <li><strong>Refresh Tokens</strong> - Long-lived tokens for token renewal</li>
 * </ul>
 */
public interface JwtService {

  /**
   * Generate a secure access token containing comprehensive user context and claims.
   *
   * @param user The authenticated user for whom to generate the token
   * @return A signed JWT access token string
   * @throws IllegalArgumentException If user is null or missing required fields
   * @throws JwtException             If token generation fails due to signing issues
   */
  String generateAccessToken(User user);

  /**
   * Generate a secure refresh token for token renewal without re-authentication.
   *
   * @param user The authenticated user for whom to generate the refresh token
   * @return A signed JWT refresh token string
   * @throws IllegalArgumentException If user is null or missing required fields
   * @throws JwtException             If token generation fails due to signing issues
   */
  String generateRefreshToken(User user);

  /**
   * Validate and decode JWT token with comprehensive security checks.
   *
   * @param token The JWT token string to validate
   * @return Decoded JWT with claims if valid
   * @throws JwtException If token is invalid, expired, or blacklisted
   */
  Jwt validateToken(String token);

  /**
   * Extract user context from JWT token.
   *
   * @param jwt The decoded JWT token
   * @return UserContext containing user information and authorities
   * @throws IllegalArgumentException If JWT is missing required claims
   */
  UserContext extractUserContext(Jwt jwt);

  /**
   * Invalidate token by adding to blacklist.
   *
   * @param token The JWT token to invalidate
   * @throws JwtException If token cannot be decoded for blacklisting
   */
  void invalidateToken(String token);

  /**
   * Revoke all refresh tokens for a specific user.
   *
   * @param userId The user ID whose refresh tokens should be revoked
   */
  void revokeAllRefreshTokensForUser(String userId);

  /**
   * Check if user's refresh tokens have been revoked after a specific time.
   *
   * @param userId        The user ID to check
   * @param tokenIssuedAt The token issued timestamp to compare against
   * @return true if tokens issued before revocation time are invalid
   */
  boolean isUserRefreshRevoked(String userId, Instant tokenIssuedAt);
}
