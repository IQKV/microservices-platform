package org.gripday.userservice.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantResolutionResult;
import org.gripday.userservice.shared.UserServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service for extracting tenant information from various sources including JWT tokens and custom headers.
 */
@Service
public class TenantExtractionService {

  private static final Logger logger = LoggerFactory.getLogger(TenantExtractionService.class);

  private final TenantManagementService tenantManagementService;

  public TenantExtractionService(final TenantManagementService tenantManagementService) {
    this.tenantManagementService = tenantManagementService;
  }

  /**
   * Extract tenant ID from HTTP request using multiple resolution strategies. Tries in order: JWT token, custom header.
   *
   * @param request        the HTTP request
   * @param authentication the authentication object (may be null)
   * @return tenant resolution result with tenant ID and resolution method
   */
  public TenantResolutionResult extractTenantFromRequest(HttpServletRequest request,
                                                         Authentication authentication) {

    // Strategy 1: Extract from JWT token claims
    if (authentication != null) {
      var jwtResult = extractTenantFromJwt(authentication);
      if (jwtResult.isPresent()) {
        var tenantId = jwtResult.get();
        if (isValidTenant(tenantId)) {
          logger.debug("Tenant resolved from JWT: {}", tenantId);
          return new TenantResolutionResult(tenantId, "JWT", tenantId, true);
        }
      }
    }

    // Strategy 2: Extract from custom header
    var headerResult = extractTenantFromHeader(request);
    if (headerResult.isPresent()) {
      var tenantId = headerResult.get();
      if (isValidTenant(tenantId)) {
        logger.debug("Tenant resolved from header: {}", tenantId);
        return new TenantResolutionResult(tenantId, "HEADER", tenantId, true);
      }
    }

    // No valid tenant found
    logger.debug("No valid tenant found in request");
    return new TenantResolutionResult(null, "NONE", null, false);
  }

  /**
   * Extract tenant ID from JWT token claims.
   *
   * @param authentication the authentication object
   * @return optional tenant ID from JWT claims
   */
  public Optional<String> extractTenantFromJwt(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      return Optional.empty();
    }

    // Handle JWT authentication
    if (authentication.getPrincipal() instanceof Jwt jwt) {
      var tenantClaim = jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID);
      if (tenantClaim != null && !tenantClaim.trim().isEmpty()) {
        return Optional.of(tenantClaim.trim());
      }
    }

    return Optional.empty();
  }

  /**
   * Extract tenant ID from custom HTTP header.
   *
   * @param request the HTTP request
   * @return optional tenant ID from header
   */
  public Optional<String> extractTenantFromHeader(HttpServletRequest request) {
    var tenantHeader = request.getHeader(UserServiceConstants.Headers.X_TENANT_ID);
    if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
      return Optional.of(tenantHeader.trim());
    }
    return Optional.empty();
  }


  /**
   * Set tenant context from resolution result.
   *
   * @param resolutionResult the tenant resolution result
   */
  public void setTenantContext(TenantResolutionResult resolutionResult) {
    if (resolutionResult.isValid() && resolutionResult.tenantId() != null) {
      TenantContext.setCurrentTenantId(resolutionResult.tenantId());
      logger.debug("Tenant context set: {} (method: {})",
          resolutionResult.tenantId(), resolutionResult.resolutionMethod());
    }
  }

  /**
   * Extract and set tenant context from request.
   *
   * @param request        the HTTP request
   * @param authentication the authentication object
   * @return true if tenant context was successfully set
   */
  public boolean extractAndSetTenantContext(HttpServletRequest request,
                                            Authentication authentication) {
    var resolutionResult = extractTenantFromRequest(request, authentication);

    if (resolutionResult.isValid()) {
      setTenantContext(resolutionResult);
      return true;
    }

    return false;
  }

  /**
   * Get tenant header name for client usage.
   *
   * @return the tenant header name
   */
  public static String getTenantHeaderName() {
    return UserServiceConstants.Headers.X_TENANT_ID;
  }

  /**
   * Get JWT claim name for tenant ID.
   *
   * @return the JWT claim name
   */
  public static String getTenantJwtClaim() {
    return UserServiceConstants.JwtClaims.TENANT_ID;
  }

  // Private helper methods

  private boolean isValidTenant(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      return false;
    }
    return tenantManagementService.isValidTenant(tenantId);
  }
}
