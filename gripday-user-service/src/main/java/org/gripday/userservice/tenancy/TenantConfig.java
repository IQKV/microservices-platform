package org.gripday.userservice.tenancy;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
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
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver(
      SchemaTenantIdentifierResolver resolver) {
    return resolver;
  }

  @Bean
  public MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
    return new SchemaPerTenantConnectionProvider(dataSource);
  }

  /**
   * Hibernate properties customizer to configure multi-tenant settings. Enables tenant-aware data filtering at the Hibernate level.
   */
  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      final CurrentTenantIdentifierResolver tenantResolver,
      final MultiTenantConnectionProvider connectionProvider) {

    return hibernateProperties -> {
      hibernateProperties.put("hibernate.multiTenancy", "SCHEMA");
      hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
      hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
      hibernateProperties.put(AvailableSettings.USE_SQL_COMMENTS, true);
    };
  }
}
