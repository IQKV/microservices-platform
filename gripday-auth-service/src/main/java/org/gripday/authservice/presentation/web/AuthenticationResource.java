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
import org.gripday.authservice.domain.service.JwtService;
import org.gripday.authservice.domain.service.AuthenticationService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.gripday.authservice.presentation.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.annotation.Timed;

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
    private final JwtService jwtService;
    
    public AuthenticationResource(
            AuthenticationService authenticationService,
            UserRegistrationService userRegistrationService,
            JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.userRegistrationService = userRegistrationService;
        this.jwtService = jwtService;
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
        @ApiResponse(responseCode = "204", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","logout"})
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
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Reset user password using a valid reset token.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Password has been reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or token", ref = "#/components/responses/BadRequest")
    })
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","reset-password"})
    public ResponseEntity<Void> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Reset password request containing token and new password",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResetPasswordRequest.class)
                )
            )
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        var clientIp = getClientIpAddress(httpRequest);
        authenticationService.resetPassword(request.token(), request.newPassword(), clientIp);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Logout from all devices",
        description = "Invalidate all refresh tokens and sessions for the currently authenticated user.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "All sessions and refresh tokens invalidated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", ref = "#/components/responses/Unauthorized")
    })
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","logout-all"})
    public ResponseEntity<Void> logoutFromAllDevices(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            var subject = token.getToken().getSubject();
            try {
                var userId = Long.parseLong(subject);
                authenticationService.logoutFromAllDevices(userId);
                return ResponseEntity.noContent().build();
            } catch (NumberFormatException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Validate a JWT (access or refresh) and return its status, metadata, and extracted user context.",
        tags = {"Authentication"}
    )
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","validate"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Validation result returned",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ValidateTokenResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Valid Access Token",
                        summary = "Active access token with user context",
                        value = """
                        {
                          "active": true,
                          "tokenId": "a1b2c3d4",
                          "tokenType": "access",
                          "issuedAt": "2025-10-26T09:45:12Z",
                          "expiresAt": "2025-10-26T10:00:12Z",
                          "user": {
                            "userId": 1,
                            "username": "john.doe",
                            "email": "john.doe@example.com",
                            "roles": ["USER"],
                            "permissions": [],
                            "firstName": "John",
                            "lastName": "Doe",
                            "tenantId": "tenant-123",
                            "customClaims": {}
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Invalid Token",
                        summary = "Inactive/invalid token",
                        value = """
                        {
                          "active": false,
                          "tokenId": null,
                          "tokenType": null,
                          "issuedAt": null,
                          "expiresAt": null,
                          "user": null
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Token validation request",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ValidateTokenRequest.class)
                )
            )
            @Valid @RequestBody ValidateTokenRequest request) {
        try {
            Jwt jwt = jwtService.validateToken(request.token());
            var user = jwtService.extractUserContext(jwt);
            var response = new ValidateTokenResponse(
                true,
                jwt.getId(),
                jwt.getClaimAsString("type"),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                user
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            var response = new ValidateTokenResponse(false, null, null, null, null, null);
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get current authenticated user",
        description = "Return the user context derived from the bearer JWT used to authenticate the request.",
        tags = {"Authentication"}
    )
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","me"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User context returned",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserContext.class),
                examples = @ExampleObject(
                    name = "User Context",
                    summary = "Authenticated user information",
                    value = """
                    {
                      "userId": 1,
                      "username": "john.doe",
                      "email": "john.doe@example.com",
                      "roles": ["USER"],
                      "permissions": [],
                      "firstName": "John",
                      "lastName": "Doe",
                      "tenantId": "tenant-123",
                      "customClaims": {}
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", ref = "#/components/responses/Unauthorized")
    })
    public ResponseEntity<UserContext> getCurrentUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            var user = jwtService.extractUserContext(token.getToken());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Initiate password reset",
        description = "Start the password reset flow by sending a reset email if the account exists.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "If the email exists, a password reset email will be sent"),
        @ApiResponse(responseCode = "400", description = "Invalid input", ref = "#/components/responses/BadRequest")
    })
    @Timed(value = "auth.endpoint", extraTags = {"endpoint","forgot-password"})
    public ResponseEntity<Void> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Forgot password request containing user email",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ForgotPasswordRequest.class)
                )
            )
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        var ipAddress = getClientIpAddress(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        authenticationService.initiatePasswordReset(request.email(), ipAddress, userAgent);
        return ResponseEntity.accepted().build();
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