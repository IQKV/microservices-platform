package com.iqscaffold.userservice.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

/**
 * Annotation to enable Liquibase migrations for integration tests.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @ActiveProfiles("test")
 * @WithLiquibaseMigrations
 * class MyIntegrationTest {
 *   // Test methods - schema will be migrated automatically
 * }
 * }
 * </pre>
 * 
 * <p>You can also specify custom tenant schemas:
 * <pre>
 * {@code
 * @WithLiquibaseMigrations(tenantSchemas = {"tenant_a", "tenant_b"})
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TestLiquibaseConfiguration.class)
@TestExecutionListeners(
    listeners = LiquibaseTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
public @interface WithLiquibaseMigrations {
  
  /**
   * Tenant schemas to create and migrate. Defaults to "tenant_test".
   */
  String[] tenantSchemas() default {"tenant_test"};
}
