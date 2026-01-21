package com.iqscaffold.userservice.config;

import com.iqscaffold.userservice.tenancy.TenantLiquibaseRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that require Liquibase migrations.
 * This class automatically runs migrations for test tenant schemas.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * class MyServiceTest extends AbstractLiquibaseIntegrationTest {
 *
 *   @Test
 *   void testWithRealSchema() {
 *     // Your test code here - schema is already migrated
 *   }
 * }
 * }
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestLiquibaseConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractLiquibaseIntegrationTest {

  @Autowired
  protected TenantLiquibaseRunner liquibaseRunner;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  /**
   * Override this method to specify which tenant schemas to create and migrate.
   * Default is a single test tenant.
   */
  protected String[] getTestTenantSchemas() {
    return new String[] {"tenant_test"};
  }

  /**
   * Runs before each test to ensure schemas are created and migrated.
   */
  @BeforeEach
  void setupLiquibaseMigrations() throws Exception {
    // Run system migrations first (creates tenants table in public schema)
    liquibaseRunner.runSystemChangelog();

    // Run tenant migrations for each test tenant
    for (final String schema : getTestTenantSchemas()) {
      jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
      liquibaseRunner.runTenantChangelog(schema);
    }
  }

  /**
   * Helper method to switch to a specific tenant schema for testing.
   */
  protected void switchToTenantSchema(String schema) {
    jdbcTemplate.execute("SET search_path TO " + schema);
  }

  /**
   * Helper method to reset to public schema.
   */
  protected void resetToPublicSchema() {
    jdbcTemplate.execute("SET search_path TO public");
  }
}
