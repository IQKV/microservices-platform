package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.*;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for user authentication, token management, and logout operations.
 * Uses modern Java syntax and handles JWT token lifecycle.
 */
@Service
@Transactional
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthenticationService(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    
    /**
     * Authenticate user with username/email and password.
     */
    public TokenResponse authenticateUser(LoginRequest request) {
        var correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            // Find user by username or email using var
            var userOptional = userRepository.findByUsernameOrEmail(
                request.username(), 
                request.username()
            );
            
            if (userOptional.isEmpty()) {
                throw new AuthenticationException("Invalid username or password");
            }
            
            var user = userOptional.get();
            
            // Verify password
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new AuthenticationException("Invalid username or password");
            }
            
            // Check if user is enabled
            if (!user.getEnabled()) {
                throw new AuthenticationException("Account is disabled");
            }
            
            // Generate tokens
            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            
            // Create user context
            var userContext = createUserContext(user);
            
            // Determine token expiry based on rememberMe flag
            var expiresIn = request.rememberMe() ? 604800L : 900L; // 7 days or 15 minutes
            
            return new TokenResponse(accessToken, refreshToken, expiresIn, userContext);
            
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
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
}