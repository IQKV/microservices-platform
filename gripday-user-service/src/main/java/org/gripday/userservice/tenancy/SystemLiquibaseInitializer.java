package org.gripday.userservice.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemLiquibaseInitializer {

  private final TenantLiquibaseRunner runner;
  private static final Logger logger = LoggerFactory.getLogger(SystemLiquibaseInitializer.class);

  public SystemLiquibaseInitializer(final TenantLiquibaseRunner runner) {
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    try {
      runner.runSystemChangelog();
    } catch (final Exception e) {
      logger.error("Failed to run system Liquibase changelog", e);
    }
  }
}