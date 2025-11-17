package org.gripday.userservice.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.registration.RegistrationService;
import org.gripday.userservice.registration.SignupRequest;
import org.gripday.userservice.registration.RegistrationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller - handles authentication operations.
 * Part of the Authentication bounded context.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication, registration, and token management")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final RegistrationService registrationService;
  private final JwtTokenService jwtTokenService;

  public AuthenticationController(
      final AuthenticationService authenticationService,
      final RegistrationService registrationService,
      final JwtTokenService jwtTokenService) {
    this.authenticationService = authenticationService;
    this.registrationService = registrationService;
    this.jwtTokenService = jwtTokenService;
  }

  @Operation(summary = "User signup", description = "Register a new user account")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User registered successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data"),
      @ApiResponse(responseCode = "409", description = "Username or email already exists")
  })
  @PostMapping("/signup")
  public ResponseEntity<RegistrationResponse> signup(
      @Valid @RequestBody SignupRequest request,
      HttpServletRequest httpRequest) {

    var ipAddress = getClientIpAddress(httpRequest);
    var userAgent = httpRequest.getHeader("User-Agent");

    var result = registrationService.registerUser(request, ipAddress, userAgent);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Authentication successful"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "423", description = "Account locked")
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

  @Operation(summary = "Refresh token", description = "Refresh JWT access token using refresh token")
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

  @Operation(summary = "User logout", description = "Logout user and invalidate JWT tokens")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Logout successful"),
      @ApiResponse(responseCode = "401", description = "Invalid or expired token")
  })
  @PostMapping("/logout")
  @SecurityRequirement(name = "bearerAuth")
  @Timed(value = "auth.endpoint", extraTags = {"endpoint", "logout"})
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      var token = authHeader.substring(7);
      var sessionId = request.getHeader("X-Session-ID");
      authenticationService.logoutUser(token, sessionId);
    }

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-all")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Logout from all devices", description = "Invalidate all refresh tokens and sessions")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "All sessions invalidated"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @Timed(value = "auth.endpoint", extraTags = {"endpoint", "logout-all"})
  public ResponseEntity<Void> logoutFromAllDevices(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken token) {
      var subject = token.getToken().getSubject();
      try {
        var userId = Long.parseLong(subject);
        authenticationService.logoutFromAllDevices(userId);
        return ResponseEntity.noContent().build();
      } catch (final NumberFormatException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @PostMapping("/validate")
  @Operation(summary = "Validate JWT token", description = "Validate a JWT and return its status and user context")
  @Timed(value = "auth.endpoint", extraTags = {"endpoint", "validate"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Validation result returned")
  })
  public ResponseEntity<ValidateTokenResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
    try {
      Jwt jwt = jwtTokenService.validateToken(request.token());
      var user = jwtTokenService.extractUserContext(jwt);
      var response = new ValidateTokenResponse(
          true,
          jwt.getId(),
          jwt.getClaimAsString("type"),
          jwt.getIssuedAt(),
          jwt.getExpiresAt(),
          user
      );
      return ResponseEntity.ok(response);
    } catch (final Exception ex) {
      var response = new ValidateTokenResponse(false, null, null, null, null, null);
      return ResponseEntity.ok(response);
    }
  }

  @Operation(summary = "Health check", description = "Check authentication service health")
  @ApiResponse(responseCode = "200", description = "Service is healthy")
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("UP", "Authentication service is running"));
  }

  private String getClientIpAddress(HttpServletRequest request) {
    var headerxForwardedFor = request.getHeader("X-Forwarded-For");
    if (headerxForwardedFor != null && !headerxForwardedFor.isEmpty()) {
      return headerxForwardedFor.split(",")[0].trim();
    }

    var headerxRealIp = request.getHeader("X-Real-IP");
    if (headerxRealIp != null && !headerxRealIp.isEmpty()) {
      return headerxRealIp;
    }

    return request.getRemoteAddr();
  }

  public record HealthResponse(String status, String message) {
  }
}
