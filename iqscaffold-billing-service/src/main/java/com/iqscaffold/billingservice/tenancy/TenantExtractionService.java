package com.iqscaffold.billingservice.tenancy;

import jakarta.servlet.http.HttpServletRequest;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for extracting tenant context from HTTP requests.
 *
 * <p>Extracts tenant ID from X-Tenant-ID header, which is set by the API Gateway
 * after JWT validation. This service provides a centralized mechanism for tenant
 * resolution in the billing service.
 */
@Service
public class TenantExtractionService {

  private static final Logger logger = LoggerFactory.getLogger(TenantExtractionService.class);

  /**
   * Extracts tenant ID from the X-Tenant-ID header.
   *
   * <p>The API Gateway validates JWT tokens and propagates the tenant ID
   * via the X-Tenant-ID header to downstream services.
   *
   * @param request the HTTP request
   * @return the tenant ID, or null if not found
   */
  public String extractTenantFromRequest(HttpServletRequest request) {
    var tenantHeader = request.getHeader(BillingConstants.Headers.TENANT_ID);

    if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
      var tenantId = tenantHeader.trim();

      // Enforce maximum length (same as user service)
      if (tenantId.length() > 100) {
        tenantId = tenantId.substring(0, 100);
        logger.warn("Tenant ID truncated to 100 characters: {}", tenantId);
      }

      logger.debug("Extracted tenant ID from header: {}", tenantId);
      return tenantId;
    }

    logger.debug("No tenant ID found in request headers");
    return null;
  }

  /**
   * Extracts tenant ID and sets it in the TenantContext.
   *
   * @param request the HTTP request
   * @return true if tenant context was set, false otherwise
   */
  public boolean extractAndSetTenantContext(HttpServletRequest request) {
    var tenantId = extractTenantFromRequest(request);

    if (tenantId != null) {
      TenantContext.setCurrentTenantId(tenantId);
      logger.debug("Tenant context set: {}", tenantId);
      return true;
    }

    logger.debug("No tenant context set - tenant ID not found");
    return false;
  }
}
