package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QueryOptimizationConfig Tests")
class QueryOptimizationConfigTest {

  @Test
  @DisplayName("Should configure Hibernate properties for non-production")
  void shouldConfigureHibernatePropertiesForNonProduction() {
    // Arrange
    var config = new QueryOptimizationConfig();
    var customizer = config.hibernatePropertiesCustomizer();
    var properties = new HashMap<String, Object>();

    // Act
    customizer.customize(properties);

    // Assert
    assertThat(properties).containsEntry(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, 2048);
    assertThat(properties).containsEntry(AvailableSettings.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, 128);
    assertThat(properties).containsEntry(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
    assertThat(properties).containsEntry(AvailableSettings.USE_QUERY_CACHE, true);
    assertThat(properties).containsEntry(AvailableSettings.STATEMENT_BATCH_SIZE, 25);
    assertThat(properties).containsEntry(AvailableSettings.ORDER_INSERTS, true);
    assertThat(properties).containsEntry(AvailableSettings.ORDER_UPDATES, true);
    assertThat(properties).containsEntry(AvailableSettings.BATCH_VERSIONED_DATA, true);
    assertThat(properties).containsEntry(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, true);
    assertThat(properties).containsEntry(AvailableSettings.USE_GET_GENERATED_KEYS, true);
    assertThat(properties).containsEntry(AvailableSettings.STATEMENT_FETCH_SIZE, 50);
    assertThat(properties).containsEntry(AvailableSettings.GENERATE_STATISTICS, true);
    assertThat(properties).containsEntry(AvailableSettings.LOG_SLOW_QUERY, 1000);
  }

  @Test
  @DisplayName("Should configure Hibernate properties for production")
  void shouldConfigureHibernatePropertiesForProduction() {
    // Arrange
    var config = new QueryOptimizationConfig();
    var customizer = config.productionHibernatePropertiesCustomizer();
    var properties = new HashMap<String, Object>();

    // Act
    customizer.customize(properties);

    // Assert
    assertThat(properties).containsEntry(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, 4096);
    assertThat(properties).containsEntry(AvailableSettings.STATEMENT_BATCH_SIZE, 50);
    assertThat(properties).containsEntry(AvailableSettings.GENERATE_STATISTICS, false);
    assertThat(properties).containsEntry(AvailableSettings.LOG_SLOW_QUERY, 2000);
    assertThat(properties).containsEntry("hibernate.hikari.maximumPoolSize", 20);
    assertThat(properties).containsEntry("hibernate.hikari.minimumIdle", 5);
    assertThat(properties).containsEntry("hibernate.hikari.connectionTimeout", 30000);
    assertThat(properties).containsEntry("hibernate.hikari.idleTimeout", 600000);
    assertThat(properties).containsEntry("hibernate.hikari.maxLifetime", 1800000);
  }
}
