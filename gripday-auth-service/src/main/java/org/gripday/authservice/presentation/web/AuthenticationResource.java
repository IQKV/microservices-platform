package org.gripday.authservice.presentation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
        description = """
            Register a new user account with comprehensive validation and security measures.
            
            ## Features
            - Username uniqueness validation
            - Email format and uniqueness validation  
            - Strong password requirements
            - Multi-tenant support
            - Rate limiting protection
            - Input sanitization
            
            ## Password Requirements
            - Minimum 8 characters
            - Must include uppercase letter
            - Must include lowercase letter
            - Must include number
            - Must include special character
            """,
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRegistrationResponse.class),
                examples = @ExampleObject(
                    name = "Successful Registration",
                    summary = "User created successfully",
                    value = """
                    {
                      "userId": 1,
                      "username": "john.doe",
                      "email": "john.doe@example.com",
                      "firstName": "John",
                      "lastName": "Doe",
                      "emailVerified": false,
                      "createdAt": "2024-01-15T10:30:00",
                      "message": "User registered successfully. Please verify your email."
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input data", ref = "#/components/responses/BadRequest"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists", ref = "#/components/responses/Conflict"),
        @ApiResponse(responseCode = "429", description = "Too many requests", ref = "#/components/responses/TooManyRequests")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserRegistrationResponse> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User registration details with validation",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SignupRequest.class)
                )
            )
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        
        var ipAddress = getClientIpAddress(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @Operation(
        summary = "User login", 
        description = """
            Authenticate user with username/email and password, returning JWT tokens.
            
            ## Features
            - Username or email authentication
            - JWT access and refresh tokens
            - Remember me functionality
            - Account lockout protection
            - Rate limiting
            - Audit logging
            
            ## Security Measures
            - Account lockout after 5 failed attempts
            - 15-minute lockout duration
            - Rate limiting (5 attempts per minute per IP)
            - Suspicious activity detection
            """,
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class),
                examples = @ExampleObject(
                    name = "Successful Login",
                    summary = "User authenticated successfully",
                    value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "tokenType": "Bearer",
                      "expiresIn": 900,
                      "user": {
                        "userId": 1,
                        "username": "john.doe",
                        "email": "john.doe@example.com",
                        "roles": ["USER"],
                        "permissions": ["READ_PROFILE"],
                        "firstName": "John",
                        "lastName": "Doe",
                        "tenantId": "tenant-123",
                        "customClaims": {}
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "423", description = "Account locked", ref = "#/components/responses/Locked"),
        @ApiResponse(responseCode = "429", description = "Too many requests", ref = "#/components/responses/TooManyRequests")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User login credentials",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class)
                )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        var ipAddress = getClientIpAddress(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        
        var result = authenticationService.authenticateUser(request, ipAddress, userAgent);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "Refresh token", 
        description = """
            Refresh JWT access token using a valid refresh token.
            
            ## Features
            - Generate new access token
            - Extend session without re-authentication
            - Maintain user context
            - Automatic token rotation
            
            ## Token Lifecycle
            - Access tokens expire in 15 minutes
            - Refresh tokens expire in 7 days
            - New refresh token issued on each refresh
            - Old refresh token is invalidated
            """,
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token refreshed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class),
                examples = @ExampleObject(
                    name = "Token Refreshed",
                    summary = "New tokens issued successfully",
                    value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "tokenType": "Bearer",
                      "expiresIn": 900,
                      "user": {
                        "userId": 1,
                        "username": "john.doe",
                        "email": "john.doe@example.com",
                        "roles": ["USER"],
                        "permissions": ["READ_PROFILE"],
                        "firstName": "John",
                        "lastName": "Doe",
                        "tenantId": "tenant-123",
                        "customClaims": {}
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TokenResponse> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Refresh token request",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshTokenRequest.class)
                )
            )
            @Valid @RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "User logout", 
        description = """
            Logout user and invalidate JWT tokens for security.
            
            ## Features
            - Invalidate current access token
            - Invalidate current refresh token
            - Add tokens to blacklist
            - Audit log logout event
            
            ## Security
            - Tokens are immediately invalidated
            - Blacklist prevents token reuse
            - Logout events are audited
            """,
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Logout successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Logout Success",
                    summary = "User logged out successfully",
                    value = "{}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(
            @io.swagger.v3.oas.annotations.Parameter(
                description = "HTTP request containing Authorization header with Bearer token",
                hidden = true
            )
            HttpServletRequest request) {
        // Extract JWT token from Authorization header
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            var token = authHeader.substring(7);
            // Extract session ID from request header (optional)
            var sessionId = request.getHeader("X-Session-ID");
            authenticationService.logoutUser(token, sessionId);
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