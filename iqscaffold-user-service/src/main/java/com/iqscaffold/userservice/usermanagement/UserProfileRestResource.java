package com.iqscaffold.userservice.usermanagement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.iqscaffold.userservice.authentication.AuthenticationService;
import com.iqscaffold.userservice.authentication.JwtService;
import com.iqscaffold.userservice.passwordmanagement.ChangePasswordRequest;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for current user profile operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile", description = "Current authenticated user profile operations")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileRestResource {

  private final JwtService jwtService;
  private final AuthenticationService authenticationService;

  public UserProfileRestResource(
      final JwtService jwtService,
      final AuthenticationService authenticationService) {
    this.jwtService = jwtService;
    this.authenticationService = authenticationService;
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get current authenticated user",
      description = "Return the user context derived from the bearer JWT used to authenticate the request.",
      tags = {"User Profile"}
  )
  @Timed(value = "auth.endpoint", extraTags = {"endpoint", "me"})
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
                        "authorities": ["USER"],
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

  @PatchMapping("/me/password")
  @Operation(
      summary = "Change password",
      description = """
          Change password for authenticated user. Requires current password verification.
          
          ## Features
          - Current password verification
          - Strong password validation
          - Session preservation
          - Confirmation email notification
          
          ## Password Requirements
          - Minimum 8 characters
          - Must include uppercase letter
          - Must include lowercase letter
          - Must include number
          - Must include special character
          - Cannot be same as current password
          
          ## Security
          - Requires valid authentication token
          - Verifies current password
          - Audit logging of password changes
          - User receives confirmation email
          """,
      tags = {"User Profile"}
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Password changed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or current password incorrect", ref = "#/components/responses/BadRequest"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", ref = "#/components/responses/Unauthorized")
  })
  @Timed(value = "password.endpoint", extraTags = {"endpoint", "change"})
  public ResponseEntity<Void> changePassword(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Change password request containing current and new password",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ChangePasswordRequest.class)
          )
      )
      @Valid @RequestBody ChangePasswordRequest request,
      Authentication authentication,
      HttpServletRequest httpRequest) {
    if (authentication instanceof JwtAuthenticationToken token) {
      var subject = token.getToken().getSubject();
      try {
        var userId = Long.parseLong(subject);
        var clientIp = getClientIpAddress(httpRequest);
        authenticationService.changePassword(userId, request.currentPassword(), request.newPassword(), clientIp);
        return ResponseEntity.noContent().build();
      } catch (final NumberFormatException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
