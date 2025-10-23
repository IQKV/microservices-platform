package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.SignupRequest;
import org.gripday.authservice.presentation.dto.UserRegistrationResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user registration and account management.
 * Handles user creation with duplicate checking and tenant context.
 */
@Service
@Transactional
public class UserRegistrationService {
    
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserRegistrationService(UserRepository userRepository, 
                                 AuthorityRepository authorityRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Register a new user with duplicate checking and tenant context.
     */
    public UserRegistrationResponse registerUser(SignupRequest request) {
        // Check for duplicate username and email using var
        var existingUsername = userRepository.existsByUsername(request.username());
        var existingEmail = userRepository.existsByEmail(request.email());
        
        if (existingUsername) {
            throw new UserRegistrationException("Username already exists: " + request.username());
        }
        
        if (existingEmail) {
            throw new UserRegistrationException("Email already exists: " + request.email());
        }
        
        // Hash password
        var hashedPassword = passwordEncoder.encode(request.password());
        
        // Create new user entity
        var user = new User(
            request.username(),
            request.email(),
            hashedPassword,
            request.firstName(),
            request.lastName(),
            request.tenantId()
        );
        
        // Assign default USER role
        var userRole = findOrCreateUserRole();
        user.addAuthority(userRole);
        
        // Save user
        var savedUser = userRepository.save(user);
        
        // Send email verification (placeholder implementation)
        sendEmailVerification(savedUser);
        
        // Return registration response
        return new UserRegistrationResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getEmailVerified(),
            savedUser.getCreatedAt(),
            "User registered successfully. Please verify your email."
        );
    }
    
    /**
     * Find or create the default USER role.
     */
    private Authority findOrCreateUserRole() {
        var userRoleOptional = authorityRepository.findByName("USER");
        
        if (userRoleOptional.isPresent()) {
            return userRoleOptional.get();
        }
        
        // Create default USER role if it doesn't exist
        var userRole = new Authority("USER", "Default user role");
        return authorityRepository.save(userRole);
    }
    
    /**
     * Send email verification (placeholder implementation).
     * In a real implementation, this would integrate with an email service.
     */
    private void sendEmailVerification(User user) {
        // Placeholder implementation
        // TODO: Integrate with email service to send verification email
        var verificationToken = generateVerificationToken(user);
        var verificationUrl = generateVerificationUrl(verificationToken);
        
        // Log the verification URL for development
        System.out.println("Email verification URL for " + user.getEmail() + ": " + verificationUrl);
    }
    
    /**
     * Generate email verification token.
     */
    private String generateVerificationToken(User user) {
        // Simple token generation for placeholder
        // In production, use a proper token generation mechanism
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * Generate email verification URL.
     */
    private String generateVerificationUrl(String token) {
        return "http://localhost:8081/api/v1/auth/verify-email?token=" + token;
    }
    
    /**
     * Custom exception for user registration errors.
     */
    public static class UserRegistrationException extends RuntimeException {
        public UserRegistrationException(String message) {
            super(message);
        }
        
        public UserRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}