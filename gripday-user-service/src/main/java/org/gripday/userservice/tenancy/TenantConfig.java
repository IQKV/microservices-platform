package org.gripday.userservice.tenancy;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for tenant-aware JPA and Hibernate settings. Provides tenant context resolution for multi-tenant data isolation.
 */
@Configuration
public class TenantConfig {

  /**
   * Custom tenant identifier resolver for Hibernate multi-tenancy. Resolves tenant ID from ThreadLocal context.
   */
  @Bean
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver() {
    return new CurrentTenantIdentifierResolver() {

      @Override
      public String resolveCurrentTenantIdentifier() {
        var tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
          return tenantId;
        }

        // Return default tenant if no context is set
        return TenantContext.getDefaultTenantId();
      }

      @Override
      public boolean validateExistingCurrentSessions() {
        // Don't validate existing sessions to allow tenant switching
        return false;
      }
    };
  }

  /**
   * Hibernate properties customizer to configure multi-tenant settings. Enables tenant-aware data filtering at the Hibernate level.
   */
  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      CurrentTenantIdentifierResolver tenantResolver) {

    return hibernateProperties -> {
      // Configure tenant identifier resolver
      hibernateProperties.put(
          AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
          tenantResolver
      );

      // Enable Hibernate filters for tenant isolation
      hibernateProperties.put(
          AvailableSettings.USE_SQL_COMMENTS,
          true
      );

      // Configure tenant-aware connection handling
      hibernateProperties.put(
          "hibernate.tenant.identifier_resolver",
          tenantResolver
      );
    };
  }
}
