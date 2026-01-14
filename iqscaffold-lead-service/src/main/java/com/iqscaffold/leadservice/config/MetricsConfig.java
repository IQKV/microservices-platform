package com.iqscaffold.leadservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for Lead Service observability.
 * 
 * <p>This configuration provides custom Prometheus metrics for monitoring
 * lead management operations and business KPIs.
 * 
 * <h3>Custom Metrics:</h3>
 * <ul>
 *   <li><strong>lead.created</strong> - Counter for lead creation events</li>
 *   <li><strong>lead.updated</strong> - Counter for lead update events</li>
 *   <li><strong>lead.deleted</strong> - Counter for lead deletion events</li>
 *   <li><strong>lead.note.added</strong> - Counter for note addition events</li>
 *   <li><strong>lead.creation.time</strong> - Timer for lead creation duration</li>
 *   <li><strong>lead.search.time</strong> - Timer for lead search duration</li>
 * </ul>
 * 
 * @see MeterRegistry
 */
@Configuration
public class MetricsConfig {

  /**
   * Counter for tracking lead creation events.
   * Tagged with source to track lead acquisition channels.
   *
   * @param registry the meter registry
   * @return the lead created counter
   */
  @Bean
  public Counter leadCreatedCounter(MeterRegistry registry) {
    return Counter.builder("lead.created")
        .description("Total number of leads created")
        .tag("service", "lead-service")
        .register(registry);
  }

  /**
   * Counter for tracking lead update events.
   *
   * @param registry the meter registry
   * @return the lead updated counter
   */
  @Bean
  public Counter leadUpdatedCounter(MeterRegistry registry) {
    return Counter.builder("lead.updated")
        .description("Total number of leads updated")
        .tag("service", "lead-service")
        .register(registry);
  }

  /**
   * Counter for tracking lead deletion events.
   *
   * @param registry the meter registry
   * @return the lead deleted counter
   */
  @Bean
  public Counter leadDeletedCounter(MeterRegistry registry) {
    return Counter.builder("lead.deleted")
        .description("Total number of leads deleted")
        .tag("service", "lead-service")
        .register(registry);
  }

  /**
   * Counter for tracking note addition events.
   *
   * @param registry the meter registry
   * @return the note added counter
   */
  @Bean
  public Counter noteAddedCounter(MeterRegistry registry) {
    return Counter.builder("lead.note.added")
        .description("Total number of notes added to leads")
        .tag("service", "lead-service")
        .register(registry);
  }

  /**
   * Timer for measuring lead creation duration.
   *
   * @param registry the meter registry
   * @return the lead creation timer
   */
  @Bean
  public Timer leadCreationTimer(MeterRegistry registry) {
    return Timer.builder("lead.creation.time")
        .description("Time taken to create a lead")
        .tag("service", "lead-service")
        .register(registry);
  }

  /**
   * Timer for measuring lead search duration.
   *
   * @param registry the meter registry
   * @return the lead search timer
   */
  @Bean
  public Timer leadSearchTimer(MeterRegistry registry) {
    return Timer.builder("lead.search.time")
        .description("Time taken to search leads")
        .tag("service", "lead-service")
        .register(registry);
  }
}
