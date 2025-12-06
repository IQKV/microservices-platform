package com.iqscaffold.billingservice.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Hibernate tenant identifier resolver for schema-per-tenant multi-tenancy.
 * Resolves the current tenant schema from the TenantContext ThreadLocal.
 */
public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  private final SchemaNameResolver schemaNameResolver;

  public SchemaTenantIdentifierResolver(final SchemaNameResolver schemaNameResolver) {
    this.schemaNameResolver = schemaNameResolver;
  }

  /**
   * Resolve the current tenant identifier (schema name) from TenantContext.
   *
   * @return the schema name for the current tenant
   */
  @Override
  public String resolveCurrentTenantIdentifier() {
    var tenantId = TenantContext.getCurrentTenantId();
    return schemaNameResolver.toSchema(tenantId);
  }

  /**
   * Indicates whether existing sessions should be validated.
   * Returns false as we use stateless session management.
   *
   * @return false
   */
  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
