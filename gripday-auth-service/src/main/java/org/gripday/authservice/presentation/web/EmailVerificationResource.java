package org.gripday.authservice.presentation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.gripday.authservice.domain.service.EmailVerificationService;
import org.gripday.authservice.presentation.dto.EmailVerificationResponse;
import org.gripday.authservice.presentation.dto.ResendVerificationRequest;
import org.gripday.authservice.presentation.dto.VerificationStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for email verification endpoints using Resource suffix convention.
 * Handles email verification, resend verification, and status check operations.
 */
@RestController
@RequestMapping("/api/v1/auth/email")
@Tag(name = "Email Verification", description = "User email verification and activation")
public class EmailVerificationResource {
    
    private final EmailVerificationService emailVerificationService;
    
    public EmailVerificationResource(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }
    
    @Operation(
        summary = "Verify email address", 
        description = """
            Verify user email address using the verification token sent via email.
            
            ## Features
            - Token validation and expiration check
            - User account activation
            - Single-use token enforcement
            - Multi-tenant support
            
            ## Process
            1. User clicks verification link in email
            2. Token is validated for authenticity and expiration
            3. User account is activated (emailVerified = true)
            4. Token is invalidated to prevent reuse
            
            ## Security
            - Tokens expire after 24 hours
            - Single-use tokens prevent replay attacks
            - Tenant isolation enforced
            """,
        tags = {"Email Verification"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Email verified successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmailVerificationResponse.class),
                examples = @ExampleObject(
                    name = "Verification Success",
                    summary = "Email verified successfully",
                    value = """
                    {
                      "success": true,
                      "message": "Email verified successfully",
                      "username": "john.doe",
                      "verifiedAt": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid or expired verification token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Token",
                    summary = "Token is invalid or expired",
                    value = """
                    {
                      "code": "EMAIL_VERIFICATION_TOKEN_INVALID",
                      "message": "Email verification failed",
                      "details": "Verification token is invalid or has expired",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "path": "/api/v1/auth/email/verify",
                      "method": "GET",
                      "correlationId": "abc123-def456-ghi789",
                      "requestId": "req-001-2024",
                      "fields": []
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "429", description = "Too many requests", ref = "#/components/responses/TooManyRequests")
    })
    @GetMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(
            @Parameter(
                description = "Email verification token from the verification link",
                required = true,
                example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam String token) {
        
        var result = emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "Resend verification email", 
        description = """
            Resend email verification link to the specified email address.
            
            ## Features
            - Generate new verification token
            - Invalidate existing tokens
            - Rate limiting protection
            - Email validation
            
            ## Rate Limiting
            - Maximum 3 emails per hour per user
            - Prevents email spam and abuse
            - Rate limit resets hourly
            
            ## Process
            1. Validate email address exists and is unverified
            2. Check rate limiting constraints
            3. Generate new verification token
            4. Invalidate any existing tokens
            5. Send new verification email
            """,
        tags = {"Email Verification"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Verification email sent successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmailVerificationResponse.class),
                examples = @ExampleObject(
                    name = "Resend Success",
                    summary = "Verification email sent",
                    value = """
                    {
                      "success": true,
                      "message": "Verification email sent successfully",
                      "username": "john.doe",
                      "verifiedAt": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid email or already verified",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Already Verified",
                    summary = "Email is already verified",
                    value = """
                    {
                      "code": "EMAIL_ALREADY_VERIFIED",
                      "message": "Email verification not required",
                      "details": "Email address is already verified",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "path": "/api/v1/auth/email/resend",
                      "method": "POST",
                      "correlationId": "abc123-def456-ghi789",
                      "requestId": "req-001-2024",
                      "fields": []
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Rate Limited",
                    summary = "Too many resend requests",
                    value = """
                    {
                      "code": "EMAIL_RESEND_RATE_LIMITED",
                      "message": "Rate limit exceeded",
                      "details": "Maximum 3 verification emails per hour. Please try again later.",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "path": "/api/v1/auth/email/resend",
                      "method": "POST",
                      "correlationId": "abc123-def456-ghi789",
                      "requestId": "req-001-2024",
                      "fields": []
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/resend")
    public ResponseEntity<EmailVerificationResponse> resendVerification(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email address to resend verification to",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResendVerificationRequest.class)
                )
            )
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        var ipAddress = getClientIpAddress(httpRequest);
        var result = emailVerificationService.resendVerificationEmail(request.email(), ipAddress);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "Get email verification status", 
        description = """
            Check the email verification status for a specific email address.
            
            ## Features
            - Email verification status check
            - Registration date information
            - Actionable status messages
            - Multi-tenant support
            
            ## Use Cases
            - Check if email needs verification
            - Display verification status in UI
            - Provide next steps to users
            - Support customer service inquiries
            """,
        tags = {"Email Verification"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Verification status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VerificationStatusResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Unverified Status",
                        summary = "Email not yet verified",
                        value = """
                        {
                          "email": "john.doe@example.com",
                          "emailVerified": false,
                          "registrationDate": "2024-01-15T10:30:00",
                          "message": "Email verification pending. Please check your inbox."
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Verified Status",
                        summary = "Email already verified",
                        value = """
                        {
                          "email": "jane.doe@example.com",
                          "emailVerified": true,
                          "registrationDate": "2024-01-14T09:15:00",
                          "message": "Email address is verified and active."
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User Not Found",
                    summary = "Email address not registered",
                    value = """
                    {
                      "code": "USER_NOT_FOUND",
                      "message": "User not found",
                      "details": "No user found with the specified email address",
                      "timestamp": "2024-01-15T10:30:00Z",
                      "path": "/api/v1/auth/email/status",
                      "method": "GET",
                      "correlationId": "abc123-def456-ghi789",
                      "requestId": "req-001-2024",
                      "fields": []
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
            @Parameter(
                description = "Email address to check verification status for",
                required = true,
                example = "john.doe@example.com"
            )
            @RequestParam String email) {
        
        var result = emailVerificationService.getVerificationStatus(email);
        return ResponseEntity.ok(result);
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
}