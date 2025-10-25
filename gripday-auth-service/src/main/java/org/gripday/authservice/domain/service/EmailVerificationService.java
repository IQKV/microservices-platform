package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.EmailVerificationToken;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.EmailVerificationTokenRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing email verification tokens and user email activation.
 * Handles token generation, validation, rate limiting, and cleanup operations.
 */
@Service
@Transactional
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    // Rate limiting configuration - maximum 3 emails per hour per user
    private static final int MAX_EMAILS_PER_HOUR = 3;
    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int CLEANUP_EXPIRY_HOURS = 48;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailOperations emailService;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailOperations emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generate a new verification token for the user and send verification email.
     * Implements rate limiting to prevent email spam.
     * 
     * @param user The user to generate verification token for
     * @return The generated verification token
     * @throws EmailVerificationException if rate limit exceeded or user already verified
     */
    public String generateVerificationToken(User user) {
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new EmailVerificationException("User email is already verified");
        }

        var tenantId = user.getTenantId();
        var userId = user.getId();

        // Check rate limiting - max 3 emails per hour
        var oneHourAgo = LocalDateTime.now().minusHours(1);
        var recentTokenCount = tokenRepository.countTokensCreatedSince(userId, tenantId, oneHourAgo);
        
        if (recentTokenCount >= MAX_EMAILS_PER_HOUR) {
            throw new EmailVerificationException("Rate limit exceeded. Maximum " + MAX_EMAILS_PER_HOUR + " verification emails per hour");
        }

        // Invalidate any existing unused tokens for this user
        tokenRepository.markAllUnusedTokensAsUsedByUserIdAndTenantId(userId, tenantId);

        // Generate secure token using UUID
        var token = UUID.randomUUID().toString();
        var expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        // Create and save new verification token
        var verificationToken = new EmailVerificationToken(token, userId, expiresAt, tenantId);
        tokenRepository.save(verificationToken);

        // Send verification email
        try {
            emailService.sendVerificationEmail(user, token);
            logger.info("Verification email sent successfully to user: {} ({})", 
                       user.getUsername(), user.getEmail());
        } catch (Exception e) {
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
     * Verify user email using the provided token.
     * Validates token and activates user account if valid.
     * 
     * @param token The verification token to validate
     * @return The verified user
     * @throws EmailVerificationException if token is invalid, expired, or already used
     */
    @Transactional
    public User verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new EmailVerificationException("Verification token cannot be null or empty");
        }

        // Find the verification token
        var verificationToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new EmailVerificationException("Invalid or already used verification token"));

        // Check if token is expired
        if (verificationToken.isExpired()) {
            throw new EmailVerificationException("Verification token has expired");
        }

        // Get the user
        var user = userRepository.findById(verificationToken.getUserId())
            .orElseThrow(() -> new EmailVerificationException("User not found for verification token"));

        // Verify tenant context matches
        var currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(user.getTenantId())) {
            throw new EmailVerificationException("Token not valid for current tenant");
        }

        // Check if user is already verified
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            // Mark token as used even if user is already verified
            verificationToken.markAsUsed();
            tokenRepository.save(verificationToken);
            throw new EmailVerificationException("User email is already verified");
        }

        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        // Activate user account
        user.setEmailVerified(true);
        var verifiedUser = userRepository.save(user);

        logger.info("Email verification successful for user: {} ({})", 
                   user.getUsername(), user.getEmail());

        return verifiedUser;
    }

    /**
     * Resend verification email for a user.
     * Implements rate limiting and generates new token.
     * 
     * @param email The email address to resend verification to
     * @return The new verification token
     * @throws EmailVerificationException if user not found, already verified, or rate limited
     */
    public String resendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new EmailVerificationException("Email address cannot be null or empty");
        }

        // Find user by email
        var user = userRepository.findByEmail(email.trim())
            .orElseThrow(() -> new EmailVerificationException("User not found with email: " + email));

        // Check tenant context
        var currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(user.getTenantId())) {
            throw new EmailVerificationException("User not found in current tenant");
        }

        // Check if user is already verified
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new EmailVerificationException("User email is already verified");
        }

        // Generate new verification token (this includes rate limiting check)
        return generateVerificationToken(user);
    }

    /**
     * Scheduled cleanup of expired verification tokens.
     * Runs daily at 2 AM to remove tokens older than 48 hours.
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
        } catch (Exception e) {
            logger.error("Failed to cleanup expired verification tokens", e);
        }
    }

    /**
     * Get the number of remaining verification emails a user can send within the rate limit window.
     * 
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @return Number of remaining emails allowed
     */
    public int getRemainingEmailCount(Long userId, String tenantId) {
        var oneHourAgo = LocalDateTime.now().minusHours(1);
        var recentTokenCount = tokenRepository.countTokensCreatedSince(userId, tenantId, oneHourAgo);
        return Math.max(0, MAX_EMAILS_PER_HOUR - (int) recentTokenCount);
    }

    /**
     * Check if a user has any unused verification tokens.
     * 
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @return true if user has unused tokens
     */
    public boolean hasUnusedTokens(Long userId, String tenantId) {
        var unusedTokenCount = tokenRepository.countUnusedTokensByUserIdAndTenantId(userId, tenantId);
        return unusedTokenCount > 0;
    }

    /**
     * Get the most recent unused token for a user (for testing purposes).
     * 
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @return The most recent unused token, or null if none exists
     */
    public EmailVerificationToken getMostRecentUnusedToken(Long userId, String tenantId) {
        return tokenRepository.findMostRecentUnusedTokenByUserIdAndTenantId(userId, tenantId)
            .orElse(null);
    }

    /**
     * Custom exception for email verification operations.
     */
    public static class EmailVerificationException extends RuntimeException {
        public EmailVerificationException(String message) {
            super(message);
        }

        public EmailVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}