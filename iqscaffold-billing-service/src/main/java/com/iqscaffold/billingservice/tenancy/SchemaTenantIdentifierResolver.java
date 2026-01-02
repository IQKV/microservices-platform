package com.iqscaffold.billingservice.tenancy;

import com.iqscaffold.billingservice.security.SecurityContextHelper;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  @Override
  public String resolveCurrentTenantIdentifier() {
    var tenantId = SecurityContextHelper.getCurrentTenantId();
    // Default to "public" if no tenant context (e.g. system background tasks, though usually we want to be explicit)
    return tenantId != null ? tenantId : "public";
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
