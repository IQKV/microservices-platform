package org.gripday.userservice.domain.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantResolutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service for extracting tenant information from various sources including JWT tokens, custom headers, and subdomain routing.
 */
@Service
public class TenantExtractionService {

  private static final Logger logger = LoggerFactory.getLogger(TenantExtractionService.class);

  private static final String TENANT_HEADER = "X-Tenant-ID";
  private static final String TENANT_JWT_CLAIM = "tenant_id";
  private static final String TENANT_SUBDOMAIN_PATTERN = "^([a-zA-Z0-9-]+)\\.";

  private final TenantManagementService tenantManagementService;

  public TenantExtractionService(final TenantManagementService tenantManagementService) {
    this.tenantManagementService = tenantManagementService;
  }

  /**
   * Extract tenant ID from HTTP request using multiple resolution strategies. Tries in order: JWT token, custom header, subdomain.
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

    // Strategy 3: Extract from subdomain
    var subdomainResult = extractTenantFromSubdomain(request);
    if (subdomainResult.isPresent()) {
      var subdomain = subdomainResult.get();
      var tenant = tenantManagementService.findTenantByDomain(subdomain);
      if (tenant.isPresent() && tenant.get().isActive()) {
        var tenantId = tenant.get().getTenantId();
        logger.debug("Tenant resolved from subdomain: {} -> {}", subdomain, tenantId);
        return new TenantResolutionResult(tenantId, "SUBDOMAIN", subdomain, true);
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
      var tenantClaim = jwt.getClaimAsString(TENANT_JWT_CLAIM);
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
    var tenantHeader = request.getHeader(TENANT_HEADER);
    if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
      return Optional.of(tenantHeader.trim());
    }
    return Optional.empty();
  }

  /**
   * Extract tenant subdomain from request host.
   *
   * @param request the HTTP request
   * @return optional subdomain
   */
  public Optional<String> extractTenantFromSubdomain(HttpServletRequest request) {
    var host = request.getHeader("Host");
    if (host == null || host.trim().isEmpty()) {
      host = request.getServerName();
    }

    if (host != null && !host.trim().isEmpty()) {
      // Extract subdomain using pattern matching
      var hostLower = host.toLowerCase(java.util.Locale.ROOT).trim();

      // Skip localhost and IP addresses
      if (hostLower.startsWith("localhost")
          || hostLower.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+")) {
        return Optional.empty();
      }

      // Extract first part as potential subdomain
      var parts = hostLower.split("\\.");
      if (parts.length >= 3) { // e.g., tenant.example.com
        var potentialSubdomain = parts[0];
        if (isValidSubdomainFormat(potentialSubdomain)) {
          return Optional.of(potentialSubdomain);
        }
      }
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
    return TENANT_HEADER;
  }

  /**
   * Get JWT claim name for tenant ID.
   *
   * @return the JWT claim name
   */
  public static String getTenantJwtClaim() {
    return TENANT_JWT_CLAIM;
  }

  // Private helper methods

  private boolean isValidTenant(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      return false;
    }
    return tenantManagementService.isValidTenant(tenantId);
  }

  private boolean isValidSubdomainFormat(String subdomain) {
    if (subdomain == null || subdomain.trim().isEmpty()) {
      return false;
    }

    // Basic subdomain validation
    var trimmed = subdomain.trim();
    return trimmed.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]$")
        && trimmed.length() >= 2
        && trimmed.length() <= 63;
  }
}