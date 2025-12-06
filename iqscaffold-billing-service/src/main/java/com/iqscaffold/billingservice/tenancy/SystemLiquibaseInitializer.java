package com.iqscaffold.billingservice.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes system-level Liquibase migrations on application startup.
 * Runs migrations in the public schema for shared data like subscription plans.
 */
@Component
public class SystemLiquibaseInitializer {

  private final TenantLiquibaseRunner runner;
  private static final Logger logger = LoggerFactory.getLogger(SystemLiquibaseInitializer.class);

  public SystemLiquibaseInitializer(final TenantLiquibaseRunner runner) {
    this.runner = runner;
  }

  /**
   * Run system Liquibase migrations when application is ready.
   * This ensures subscription plans and other shared data structures are created.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    try {
      logger.info("Running system Liquibase migrations in public schema");
      runner.runSystemChangelog();
      logger.info("System Liquibase migrations completed successfully");
    } catch (final Exception e) {
      logger.error("Failed to run system Liquibase changelog", e);
      throw new RuntimeException("System schema initialization failed", e);
    }
  }
}
