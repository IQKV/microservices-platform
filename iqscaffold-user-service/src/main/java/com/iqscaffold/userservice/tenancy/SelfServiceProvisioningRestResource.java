package com.iqscaffold.userservice.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.iqscaffold.userservice.shared.exception.TenantManagementException;
import com.iqscaffold.userservice.tenancy.SelfServiceProvisioningService.SelfServiceProvisioningException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for self-service tenant provisioning.
 * Provides a public endpoint for tenant signup without authentication.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Self-Service Provisioning", description = "Public tenant signup and provisioning operations")
public class SelfServiceProvisioningRestResource {

  private static final Logger logger = LoggerFactory.getLogger(SelfServiceProvisioningRestResource.class);

  private final SelfServiceProvisioningService provisioningService;

  public SelfServiceProvisioningRestResource(final SelfServiceProvisioningService provisioningService) {
    this.provisioningService = provisioningService;
  }

  /**
   * Self-service tenant signup endpoint.
   * Creates a complete tenant environment with organization and admin user.
   * No authentication required - this is a public endpoint.
   */
  @PostMapping("/signup")
  @Operation(
      summary = "Self-service tenant signup",
      description = """
          Provisions a complete tenant environment including:
          - Tenant creation with database schema
          - Organization setup with default configuration
          - Admin user creation with TENANT_ADMIN authority
          - Email verification workflow initiation
          
          This is a public endpoint that requires no authentication.
          Rate limiting is applied to prevent abuse (3 requests per hour per IP).
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Tenant provisioned successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SelfServiceSignupResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data or validation error",
          content = @Content(mediaType = "application/json")
      ),
      @ApiResponse(
          responseCode = "409",
          description = "Tenant ID, domain, username, or email already exists",
          content = @Content(mediaType = "application/json")
      ),
      @ApiResponse(
          responseCode = "429",
          description = "Rate limit exceeded - too many signup requests",
          content = @Content(mediaType = "application/json")
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error during provisioning",
          content = @Content(mediaType = "application/json")
      )
  })
  public ResponseEntity<SelfServiceSignupResponse> signup(
      @Valid @RequestBody SelfServiceSignupRequest request,
      HttpServletRequest httpRequest) {

    logger.info("Received self-service signup request for organization: {}", request.organizationName());

    try {
      // Extract IP address and user agent for security audit
      var ipAddress = extractIpAddress(httpRequest);
      var userAgent = extractUserAgent(httpRequest);

      logger.debug("Signup request from IP: {}, User-Agent: {}", ipAddress, userAgent);

      // Provision tenant with admin user
      var response = provisioningService.provisionTenantWithAdmin(request, ipAddress, userAgent);

      logger.info("Successfully provisioned tenant: {} for organization: {}",
          response.tenantId(), response.organizationName());

      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (final SelfServiceProvisioningException e) {
      logger.warn("Self-service provisioning failed for organization {}: {}",
          request.organizationName(), e.getMessage());

      // Determine appropriate status code
      if (e.getMessage().contains("already exists")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      } else if (e.getMessage().contains("Invalid input")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }

    } catch (final TenantManagementException.TenantAlreadyExistsException e) {
      logger.warn("Tenant already exists: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).build();

    } catch (final TenantManagementException.DomainAlreadyExistsException e) {
      logger.warn("Domain already exists: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).build();

    } catch (final Exception e) {
      logger.error("Unexpected error during self-service provisioning for organization: {}",
          request.organizationName(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Extract client IP address from HTTP request.
   * Checks X-Forwarded-For header for proxied requests.
   */
  private String extractIpAddress(HttpServletRequest request) {
    var forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
      // X-Forwarded-For can contain multiple IPs, take the first one
      return forwardedFor.split(",")[0].trim();
    }

    var realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isEmpty()) {
      return realIp;
    }

    return request.getRemoteAddr();
  }

  /**
   * Extract user agent from HTTP request.
   */
  private String extractUserAgent(HttpServletRequest request) {
    var userAgent = request.getHeader("User-Agent");
    return userAgent != null ? userAgent : "Unknown";
  }
}
