package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.SignupRequest;
import org.gripday.authservice.presentation.dto.UserRegistrationResponse;
import org.gripday.authservice.presentation.validation.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enhanced service for user registration with security measures.
 * Includes input sanitization, audit logging, and security validation.
 */
@Service
@Transactional
public class UserRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);
    
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;
    private final InputSanitizer inputSanitizer;
    private final EmailVerificationService emailVerificationService;
    
    public UserRegistrationService(UserRepository userRepository, 
                                 AuthorityRepository authorityRepository,
                                 PasswordEncoder passwordEncoder,
                                 SecurityAuditService securityAuditService,
                                 InputSanitizer inputSanitizer,
                                 EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditService = securityAuditService;
        this.inputSanitizer = inputSanitizer;
        this.emailVerificationService = emailVerificationService;
    }
    
    /**
     * Register a new user with enhanced security validation.
     * Includes input sanitization, security checks, and audit logging.
     */
    public UserRegistrationResponse registerUser(SignupRequest request, String ipAddress, String userAgent) {
        // Sanitize all inputs to prevent XSS and injection attacks
        var sanitizedUsername = inputSanitizer.sanitizeUsername(request.username());
        var sanitizedEmail = inputSanitizer.sanitizeEmail(request.email());
        var sanitizedFirstName = inputSanitizer.sanitizeName(request.firstName());
        var sanitizedLastName = inputSanitizer.sanitizeName(request.lastName());
        
        // Validate input safety
        if (!inputSanitizer.isInputSafe(request.username()) ||
            !inputSanitizer.isInputSafe(request.email()) ||
            !inputSanitizer.isInputSafe(request.firstName()) ||
            !inputSanitizer.isInputSafe(request.lastName())) {
            
            securityAuditService.logSuspiciousActivity(
                sanitizedUsername, "Potential XSS/injection attempt in registration", ipAddress, userAgent);
            throw new UserRegistrationException("Invalid input detected");
        }
        
        // Check for SQL injection attempts
        if (inputSanitizer.containsSqlInjection(request.username()) ||
            inputSanitizer.containsSqlInjection(request.email()) ||
            inputSanitizer.containsSqlInjection(request.firstName()) ||
            inputSanitizer.containsSqlInjection(request.lastName())) {
            
            securityAuditService.logSuspiciousActivity(
                sanitizedUsername, "SQL injection attempt in registration", ipAddress, userAgent);
            throw new UserRegistrationException("Invalid input detected");
        }
        
        // Check for duplicate username and email using var
        var existingUsername = userRepository.existsByUsername(sanitizedUsername);
        var existingEmail = userRepository.existsByEmail(sanitizedEmail);
        
        if (existingUsername) {
            securityAuditService.logFailedAuthentication(
                sanitizedUsername, "Registration failed - username exists", ipAddress, userAgent);
            throw new UserRegistrationException("Username already exists");
        }
        
        if (existingEmail) {
            securityAuditService.logFailedAuthentication(
                sanitizedEmail, "Registration failed - email exists", ipAddress, userAgent);
            throw new UserRegistrationException("Email already exists");
        }
        
        // Hash password with enhanced security
        var hashedPassword = passwordEncoder.encode(request.password());
        
        // Create new user entity with sanitized inputs
        var user = new User(
            sanitizedUsername,
            sanitizedEmail,
            hashedPassword,
            sanitizedFirstName,
            sanitizedLastName,
            request.tenantId()
        );
        
        // Ensure emailVerified is false for new users
        user.setEmailVerified(false);
        
        // Assign default USER role
        var userRole = findOrCreateUserRole();
        user.addAuthority(userRole);
        
        // Save user
        var savedUser = userRepository.save(user);
        
        // Log successful registration
        securityAuditService.logUserRegistration(
            savedUser.getUsername(), savedUser.getEmail(), ipAddress, userAgent);
        
        // Generate verification token and send verification email
        try {
            emailVerificationService.generateVerificationToken(savedUser);
        } catch (Exception e) {
            logger.warn("Failed to send verification email to user: {} ({})", 
                       savedUser.getUsername(), savedUser.getEmail(), e);
            // Don't fail registration if email sending fails
        }
        
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