package com.iqscaffold.userservice.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.TestContext;

import com.iqscaffold.userservice.tenancy.TenantLiquibaseRunner;

/**
 * Test execution listener that runs Liquibase migrations before tests.
 * Use with @TestExecutionListeners annotation on test classes.
 *
 * <p>Example:
 * <pre>
 * {@code
 * @TestExecutionListeners(
 *   listeners = LiquibaseTestExecutionListener.class,
 *   mergeMode = MergeMode.MERGE_WITH_DEFAULTS
 * )
 * class MyTest {
 *   // Test methods
 * }
 * }
 * </pre>
 */
public class LiquibaseTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    TenantLiquibaseRunner liquibaseRunner =
        testContext.getApplicationContext().getBean(TenantLiquibaseRunner.class);
    JdbcTemplate jdbcTemplate =
        testContext.getApplicationContext().getBean(JdbcTemplate.class);

    // Run system migrations
    liquibaseRunner.runSystemChangelog();

    // Create and migrate test tenant schema
    String testSchema = "tenant_test";
    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + testSchema);
    liquibaseRunner.runTenantChangelog(testSchema);
  }

  @Override
  public int getOrder() {
    return 3000; // Run after DependencyInjectionTestExecutionListener
  }
}
