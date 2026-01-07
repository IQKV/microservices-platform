package com.iqscaffold.billingservice.config;

import javax.sql.DataSource;

import com.iqscaffold.billingservice.tenancy.TenantLiquibaseRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that enables Liquibase migrations for integration tests.
 * Use this when you need real database schema for testing.
 */
@TestConfiguration
public class TestLiquibaseConfiguration {

  /**
   * Creates a test-specific TenantLiquibaseRunner that can run migrations
   * for test tenant schemas.
   */
  @Bean
  @Primary
  public TenantLiquibaseRunner testLiquibaseRunner(DataSource dataSource) {
    return new TenantLiquibaseRunner(
        dataSource,
        "classpath:db/changelog/tenant/master.xml",
        "classpath:db/changelog/system/master.xml"
    );
  }
}
