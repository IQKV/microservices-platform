package com.iqscaffold.billingservice.tenancy;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantConfig {

  @Bean
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver() {
    return new SchemaTenantIdentifierResolver();
  }

  @Bean
  public MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
    return new SchemaPerTenantConnectionProvider(dataSource);
  }

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
