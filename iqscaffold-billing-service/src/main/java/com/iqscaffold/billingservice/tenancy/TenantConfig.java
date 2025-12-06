package com.iqscaffold.billingservice.tenancy;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for tenant-aware JPA and Hibernate settings.
 * Provides tenant context resolution for multi-tenant data isolation using schema-per-tenant strategy.
 */
@Configuration
public class TenantConfig {

  /**
   * Custom tenant identifier resolver for Hibernate multi-tenancy.
   * Resolves tenant ID from ThreadLocal context and converts to schema name.
   *
   * @param schemaNameResolver the schema name resolver
   * @return the tenant identifier resolver
   */
  @Bean
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver(
      SchemaNameResolver schemaNameResolver) {
    return new SchemaTenantIdentifierResolver(schemaNameResolver);
  }

  /**
   * Multi-tenant connection provider for schema-per-tenant strategy.
   * Manages database connections and sets the appropriate schema for each tenant.
   *
   * @param dataSource the data source
   * @return the multi-tenant connection provider
   */
  @Bean
  public MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
    return new SchemaPerTenantConnectionProvider(dataSource);
  }

  /**
   * Hibernate properties customizer to configure multi-tenant settings.
   * Enables tenant-aware data filtering at the Hibernate level.
   *
   * @param tenantResolver       the tenant identifier resolver
   * @param connectionProvider   the multi-tenant connection provider
   * @return the Hibernate properties customizer
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
