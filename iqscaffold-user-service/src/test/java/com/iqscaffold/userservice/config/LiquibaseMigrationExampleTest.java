package com.iqscaffold.userservice.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Example test demonstrating different ways to use Liquibase migrations in tests.
 */
class LiquibaseMigrationExampleTest {

  /**
   * Example 1: Using AbstractLiquibaseIntegrationTest base class.
   * This is the simplest approach - just extend the base class.
   */
  @SpringBootTest
  @Disabled("Enable to run this test manually")
  @ActiveProfiles("test")
  static class UsingBaseClassTest extends AbstractLiquibaseIntegrationTest {

    @Test
    void shouldHaveMigratedSchema() {
      // Verify that users table exists in tenant_test schema
      switchToTenantSchema("tenant_test");

      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'",
          Integer.class
      );

      assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldHaveAuthoritiesTable() {
      switchToTenantSchema("tenant_test");

      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM authorities",
          Integer.class
      );

      // Should have 4 default authorities (SUPER_ADMIN, ADMIN, TENANT_OWNER, USER)
      assertThat(count).isGreaterThanOrEqualTo(4);
    }
  }

  /**
   * Example 2: Using @WithLiquibaseMigrations annotation.
   * This approach gives you more control over which schemas to create.
   */
  @SpringBootTest
  @ActiveProfiles("test")
  @Disabled("Enable to run this test manually")
  @WithLiquibaseMigrations(tenantSchemas = {"tenant_a", "tenant_b"})
  static class UsingAnnotationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveMultipleTenantSchemas() {
      // Verify tenant_a schema
      jdbcTemplate.execute("SET search_path TO tenant_a");
      Integer countA = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'",
          Integer.class
      );
      assertThat(countA).isEqualTo(1);

      // Verify tenant_b schema
      jdbcTemplate.execute("SET search_path TO tenant_b");
      Integer countB = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'",
          Integer.class
      );
      assertThat(countB).isEqualTo(1);
    }
  }

  /**
   * Example 3: Manual migration control for specific test scenarios.
   */
  @SpringBootTest
  @ActiveProfiles("test")
  @Disabled("Enable to run this test manually")
  @org.springframework.context.annotation.Import({
      TestRedisConfiguration.class,
      TestLiquibaseConfiguration.class
  })
  static class ManualMigrationTest {

    @Autowired
    private com.iqscaffold.userservice.tenancy.TenantLiquibaseRunner liquibaseRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMigrateCustomSchema() throws Exception {
      // Create and migrate a custom schema for this specific test
      String customSchema = "tenant_custom_" + System.currentTimeMillis();

      jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + customSchema);
      liquibaseRunner.runTenantChangelog(customSchema);

      // Verify migration
      jdbcTemplate.execute("SET search_path TO " + customSchema);
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'",
          Integer.class
      );

      assertThat(count).isEqualTo(1);

      // Cleanup
      jdbcTemplate.execute("DROP SCHEMA " + customSchema + " CASCADE");
    }
  }
}

