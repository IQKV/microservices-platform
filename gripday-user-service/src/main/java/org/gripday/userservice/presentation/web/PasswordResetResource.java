package org.gripday.userservice.presentation.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.domain.service.PasswordResetService;
import org.gripday.userservice.presentation.dto.ForgotPasswordRequest;
import org.gripday.userservice.presentation.dto.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for password reset operations using Resource suffix convention.
 * Handles password reset flows and forgot password.
 */
@RestController
@RequestMapping("/api/v1/auth/password")
@Tag(name = "Password Reset API", description = "Password reset operations")
public class PasswordResetResource {

  private final PasswordResetService passwordResetService;

  public PasswordResetResource(final PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/forgot")
  @Operation(
      summary = "Initiate password reset",
      description = """
          Start the password reset flow by sending a reset email if the account exists.
          
          ## Features
          - Email-based password reset
          - Secure reset token generation
          - Rate limiting protection
          - Security-conscious response (no user enumeration)
          
          ## Security Measures
          - Always returns 202 Accepted (prevents user enumeration)
          - Reset tokens expire in 1 hour
          - Rate limiting per IP address
          - Audit logging of reset attempts
          """,
      tags = {"Password Reset API"}
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "If the email exists, a password reset email will be sent"),
      @ApiResponse(responseCode = "400", description = "Invalid input", ref = "#/components/responses/BadRequest"),
      @ApiResponse(responseCode = "429", description = "Too many requests", ref = "#/components/responses/TooManyRequests")
  })
  @Timed(value = "password.endpoint", extraTags = {"endpoint", "forgot"})
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
    passwordResetService.initiatePasswordReset(request.email(), ipAddress, userAgent);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/reset")
  @Operation(
      summary = "Reset password",
      description = """
          Reset user password using a valid reset token received via email.
          
          ## Features
          - Token-based password reset
          - Strong password validation
          - Automatic token invalidation
          - Confirmation email notification
          
          ## Password Requirements
          - Minimum 8 characters
          - Must include uppercase letter
          - Must include lowercase letter
          - Must include number
          - Must include special character
          
          ## Security
          - Reset token is single-use
          - Token expires after 1 hour
          - All existing sessions are invalidated
          - User receives confirmation email
          """,
      tags = {"Password Reset API"}
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Password has been reset successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or token", ref = "#/components/responses/BadRequest")
  })
  @Timed(value = "password.endpoint", extraTags = {"endpoint", "reset"})
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
    passwordResetService.resetPassword(request.token(), request.newPassword(), clientIp);
    return ResponseEntity.noContent().build();
  }

  /**
   * Get client IP address, considering proxy headers.
   */
  private String getClientIpAddress(HttpServletRequest request) {
    // Check for X-Forwarded-For header (common in load balancers)
    var headerxForwardedFor = request.getHeader("X-Forwarded-For");
    if (headerxForwardedFor != null && !headerxForwardedFor.isEmpty()) {
      // Take the first IP in the chain
      return headerxForwardedFor.split(",")[0].trim();
    }

    // Check for X-Real-IP header (nginx)
    var headerxRealIp = request.getHeader("X-Real-IP");
    if (headerxRealIp != null && !headerxRealIp.isEmpty()) {
      return headerxRealIp;
    }

    // Fall back to remote address
    return request.getRemoteAddr();
  }
}
