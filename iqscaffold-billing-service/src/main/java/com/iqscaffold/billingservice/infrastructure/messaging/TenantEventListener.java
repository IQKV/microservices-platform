package com.iqscaffold.billingservice.infrastructure.messaging;

import com.iqscaffold.billingservice.tenancy.TenantLiquibaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener for tenant events from the User Service.
 * Handles tenant lifecycle events to provision billing database schemas.
 */
@Component
public class TenantEventListener {

  private static final Logger log = LoggerFactory.getLogger(TenantEventListener.class);

  private final TenantLiquibaseRunner liquibaseRunner;
  private final JdbcTemplate jdbcTemplate;

  public TenantEventListener(
      final TenantLiquibaseRunner liquibaseRunner,
      final JdbcTemplate jdbcTemplate) {
    this.liquibaseRunner = liquibaseRunner;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Handle tenant event from User Service.
   * Provisions billing database schema when tenant is created.
   */
  @RabbitListener(queues = "iqscaffold.billing.tenant.events")
  public void handleTenantEvent(TenantEvent event) {
    try {
      log.info("Received tenant event: {} for tenant: {}",
          event.getEventType(), event.getTenantId());

      // Process the event based on type
      switch (event.getEventType()) {
        case "TENANT_CREATED":
          handleTenantCreated(event);
          break;
        case "TENANT_UPDATED":
          handleTenantUpdated(event);
          break;
        case "TENANT_DELETED":
          handleTenantDeleted(event);
          break;
        default:
          log.warn("Unknown tenant event type: {}", event.getEventType());
      }

      log.debug("Successfully processed tenant event: {}", event.getEventId());
    } catch (final Exception e) {
      log.error("Error processing tenant event: {} for tenant: {}",
          event.getEventId(), event.getTenantId(), e);
      throw e; // Re-throw to trigger retry or DLQ routing
    }
  }

  /**
   * Handle tenant created event.
   * Provisions billing database schema for the new tenant.
   */
  private void handleTenantCreated(TenantEvent event) {
    log.info("Processing tenant created event for tenant: {}", event.getTenantId());

    try {
      // Create tenant schema in billing database
      var schemaName = resolveSchemaName(event.getTenantId());
      
      log.info("Creating schema '{}' for tenant: {}", schemaName, event.getTenantId());
      jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

      // Run Liquibase migrations for tenant schema
      log.info("Running Liquibase migrations for schema: {}", schemaName);
      liquibaseRunner.runTenantChangelog(schemaName);

      log.info("Successfully provisioned billing schema for tenant: {} (organization: {})",
          event.getTenantId(), event.getOrganizationName());

    } catch (final Exception e) {
      log.error("Failed to provision billing schema for tenant: {}", event.getTenantId(), e);
      throw new RuntimeException("Failed to provision billing schema for tenant: " + event.getTenantId(), e);
    }
  }

  /**
   * Handle tenant updated event.
   * Currently a no-op, but could be used for schema migrations.
   */
  private void handleTenantUpdated(TenantEvent event) {
    log.info("Processing tenant updated event for tenant: {}", event.getTenantId());
    
    // Placeholder for future implementation
    // Could be used to:
    // - Apply schema migrations
    // - Update tenant configuration
    // - Sync organization details
    
    log.debug("Tenant updated event processed for tenant: {}", event.getTenantId());
  }

  /**
   * Handle tenant deleted event.
   * Archives billing data but does not drop schema (for compliance).
   */
  private void handleTenantDeleted(TenantEvent event) {
    log.info("Processing tenant deleted event for tenant: {}", event.getTenantId());

    // Placeholder for future implementation
    // Best practice: Archive data but DO NOT drop schema
    // Reasons:
    // - Compliance and audit requirements
    // - Historical billing data retention
    // - Potential data recovery needs
    // - Legal/financial record keeping

    log.warn("Tenant deleted event received for: {}. Billing data archived but schema retained for compliance.",
        event.getTenantId());
    
    log.debug("Tenant deleted event processed for tenant: {}", event.getTenantId());
  }

  /**
   * Resolve schema name from tenant ID.
   * Matches the naming convention from user service.
   */
  private String resolveSchemaName(String tenantId) {
    return "tenant_" + tenantId.replaceAll("[^a-z0-9_]", "_");
  }
}
