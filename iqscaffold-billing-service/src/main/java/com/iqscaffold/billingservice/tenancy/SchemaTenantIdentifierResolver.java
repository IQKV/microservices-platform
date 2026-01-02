package com.iqscaffold.billingservice.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  @Override
  public String resolveCurrentTenantIdentifier() {
    var tenantId = TenantContext.getCurrentTenantId();
    // Default to "public" if no tenant context (e.g. system background tasks)
    return tenantId != null ? tenantId : "public";
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
