package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.*;
import org.gripday.authservice.presentation.validation.InputSanitizer;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced service for user authentication with security measures.
 * Includes account lockout, audit logging, and input sanitization.
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
    
    public AuthenticationService(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService,
                               AccountLockoutService accountLockoutService,
                               SecurityAuditService securityAuditService,
                               InputSanitizer inputSanitizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountLockoutService = accountLockoutService;
        this.securityAuditService = securityAuditService;
        this.inputSanitizer = inputSanitizer;
    }
    
    /**
     * Authenticate user with enhanced security measures.
     * Includes account lockout, audit logging, and input sanitization.
     */
    public TokenResponse authenticateUser(LoginRequest request, String ipAddress, String userAgent) {
        var correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            // Sanitize input to prevent injection attacks
            var sanitizedUsername = inputSanitizer.sanitizeInput(request.username());
            
            // Check for suspicious input patterns
            if (!inputSanitizer.isInputSafe(request.username()) || 
                inputSanitizer.containsSqlInjection(request.username())) {
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
            
            // Clear failed attempts on successful authentication
            accountLockoutService.clearFailedAttempts(user.getUsername());
            
            // Generate tokens
            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            
            // Log successful authentication
            securityAuditService.logSuccessfulAuthentication(user.getUsername(), ipAddress, userAgent);
            securityAuditService.logTokenEvent(user.getUsername(), "generated", ipAddress, userAgent);
            
            // Create user context
            var userContext = createUserContext(user);
            
            // Determine token expiry based on rememberMe flag
            var expiresIn = request.rememberMe() ? 604800L : 900L; // 7 days or 15 minutes
            
            return new TokenResponse(accessToken, refreshToken, expiresIn, userContext);
            
        } catch (AuthenticationException | AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            securityAuditService.logFailedAuthentication(
                request.username(), "System error: " + e.getMessage(), ipAddress, userAgent);
            throw new AuthenticationException("Authentication failed", e);
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
            
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid or expired refresh token", e);
        } catch (Exception e) {
            throw new AuthenticationException("Token refresh failed", e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Logout user and invalidate tokens.
     */
    public void logoutUser(String accessToken) {
        var correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            // Invalidate the access token
            jwtService.invalidateToken(accessToken);
            
        } catch (Exception e) {
            // Log error but don't throw exception for logout
            System.err.println("Error during logout: " + e.getMessage());
        } finally {
            MDC.remove("correlationId");
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
     * Generate correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Custom exception for authentication errors.
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
        
        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Custom exception for account lockout scenarios.
     */
    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(String message) {
            super(message);
        }
    }
}