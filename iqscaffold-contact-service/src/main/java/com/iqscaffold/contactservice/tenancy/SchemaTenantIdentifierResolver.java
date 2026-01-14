package com.iqscaffold.contactservice.tenancy;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

@Component
public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

  private static final String DEFAULT_TENANT_ID = "public";

  @Override
  public String resolveCurrentTenantIdentifier() {
    String tenantId = TenantContext.getCurrentTenant();
    return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}