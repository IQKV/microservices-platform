package org.gripday.userservice.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  private final SchemaNameResolver schemaNameResolver;

  public SchemaTenantIdentifierResolver(final SchemaNameResolver schemaNameResolver) {
    this.schemaNameResolver = schemaNameResolver;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    var tenantId = TenantContext.getCurrentTenantId();
    return schemaNameResolver.toSchema(tenantId);
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}