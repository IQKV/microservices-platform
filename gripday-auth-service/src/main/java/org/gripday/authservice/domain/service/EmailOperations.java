package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.User;

/**
 * Service interface for email operations.
 * Handles sending verification emails and building verification URLs.
 */
public interface EmailOperations {
    
    /**
     * Send verification email to the user.
     * 
     * @param user The user to send the verification email to
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
}