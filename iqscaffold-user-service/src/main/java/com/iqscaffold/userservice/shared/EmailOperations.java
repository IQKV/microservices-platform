package com.iqscaffold.userservice.shared;

import com.iqscaffold.userservice.usermanagement.User;

/**
 * Service interface for email operations. Handles sending verification emails and building verification URLs.
 */
public interface EmailOperations {

  /**
   * Send verification email to the user.
   *
   * @param user  The user to send the verification email to
   * @param token The verification token to include in the email
   */
  void sendVerificationEmail(User user, String token);

  /**
   * Build verification URL with the provided token.
   *
   * @param token The verification token
   * @return The complete verification URL
   */
  String buildVerificationUrl(String token);

  /**
   * Send welcome email after successful email verification.
   *
   * @param user The user whose email was verified
   */
  void sendRegistrationConfirmedEmail(User user);

  /**
   * Send confirmation email after successful password reset.
   *
   * @param user The user whose password was reset
   */
  void sendPasswordResetConfirmedEmail(User user);
}