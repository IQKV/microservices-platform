package com.iqscaffold.userservice.config;

import com.iqscaffold.userservice.tenancy.TenantLiquibaseRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require Liquibase migrations with real PostgreSQL.
 * Uses Testcontainers to spin up a PostgreSQL instance for testing.
 * 
 * <p>This provides the most production-like testing environment but is slower than H2.
 * Use this when you need to test PostgreSQL-specific features or validate production behavior.
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * class MyServiceTest extends AbstractPostgresLiquibaseIntegrationTest {
 *   
 *   @Test
 *   void testWithRealPostgres() {
 *     // Your test code here - PostgreSQL is running with migrated schemas
 *   }
 * }
 * }
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestLiquibaseConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public abstract class AbstractPostgresLiquibaseIntegrationTest {

  @Container
  protected static final PostgreSQLContainer<?> postgres = 
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired
  protected TenantLiquibaseRunner liquibaseRunner;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  /**
   * Override this method to specify which tenant schemas to create and migrate.
   * Default is a single test tenant.
   */
  protected String[] getTestTenantSchemas() {
    return new String[]{"tenant_test"};
  }

  /**
   * Runs once before all tests to ensure schemas are created and migrated.
   */
  @BeforeAll
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
