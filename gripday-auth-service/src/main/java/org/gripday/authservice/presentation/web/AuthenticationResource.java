package org.gripday.authservice.presentation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.gripday.authservice.domain.service.AuthenticationService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.gripday.authservice.presentation.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints using Resource suffix convention.
 * Handles user registration, login, token refresh, and logout operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication, registration, and token management")
public class AuthenticationResource {
    
    private final AuthenticationService authenticationService;
    private final UserRegistrationService userRegistrationService;
    
    public AuthenticationResource(
            AuthenticationService authenticationService,
            UserRegistrationService userRegistrationService) {
        this.authenticationService = authenticationService;
        this.userRegistrationService = userRegistrationService;
    }
    
    @Operation(
        summary = "User signup", 
        description = "Register a new user account with username, email, and password"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists"),
        @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserRegistrationResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        
        var ipAddress = getClientIpAddress(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @Operation(
        summary = "User login", 
        description = "Authenticate user with username/email and password"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked"),
        @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        var ipAddress = getClientIpAddress(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        
        var result = authenticationService.authenticateUser(request, ipAddress, userAgent);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "Refresh token", 
        description = "Refresh JWT access token using refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "User logout", 
        description = "Logout user and invalidate JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Extract JWT token from Authorization header
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            var token = authHeader.substring(7);
            authenticationService.logoutUser(token);
        }
        
        return ResponseEntity.ok().build();
    }
    
    @Operation(
        summary = "Health check", 
        description = "Check authentication service health"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "Authentication service is running"));
    }
    
    /**
     * Get client IP address, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (common in load balancers)
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header (nginx)
        var xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }
    
    /**
     * Simple health response record.
     */
    public record HealthResponse(
        String status,
        String message
    ) {}
}