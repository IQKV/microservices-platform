package com.iqscaffold.contactservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for Contact Service observability.
 *
 * <p>This configuration provides custom Prometheus metrics for monitoring
 * contact and company management operations.
 *
 * <h3>Custom Metrics:</h3>
 * <ul>
 *   <li><strong>contact.created</strong> - Counter for contact creation events</li>
 *   <li><strong>contact.updated</strong> - Counter for contact update events</li>
 *   <li><strong>contact.deleted</strong> - Counter for contact deletion events</li>
 *   <li><strong>company.created</strong> - Counter for company creation events</li>
 *   <li><strong>contact.from.lead</strong> - Counter for contacts created from leads</li>
 *   <li><strong>contact.creation.time</strong> - Timer for contact creation duration</li>
 * </ul>
 *
 * @see MeterRegistry
 */
@Configuration
public class MetricsConfig {

  /**
   * Counter for tracking contact creation events.
   *
   * @param registry the meter registry
   * @return the contact created counter
   */
  @Bean
  public Counter contactCreatedCounter(MeterRegistry registry) {
    return Counter.builder("contact.created")
        .description("Total number of contacts created")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Counter for tracking contact update events.
   *
   * @param registry the meter registry
   * @return the contact updated counter
   */
  @Bean
  public Counter contactUpdatedCounter(MeterRegistry registry) {
    return Counter.builder("contact.updated")
        .description("Total number of contacts updated")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Counter for tracking contact deletion events.
   *
   * @param registry the meter registry
   * @return the contact deleted counter
   */
  @Bean
  public Counter contactDeletedCounter(MeterRegistry registry) {
    return Counter.builder("contact.deleted")
        .description("Total number of contacts deleted")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Counter for tracking company creation events.
   *
   * @param registry the meter registry
   * @return the company created counter
   */
  @Bean
  public Counter companyCreatedCounter(MeterRegistry registry) {
    return Counter.builder("company.created")
        .description("Total number of companies created")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Counter for tracking contacts created from lead conversions.
   *
   * @param registry the meter registry
   * @return the contact from lead counter
   */
  @Bean
  public Counter contactFromLeadCounter(MeterRegistry registry) {
    return Counter.builder("contact.from.lead")
        .description("Total number of contacts created from lead conversions")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Timer for measuring contact creation duration.
   *
   * @param registry the meter registry
   * @return the contact creation timer
   */
  @Bean
  public Timer contactCreationTimer(MeterRegistry registry) {
    return Timer.builder("contact.creation.time")
        .description("Time taken to create a contact")
        .tag("service", "contact-service")
        .register(registry);
  }

  /**
   * Timer for measuring contact search duration.
   *
   * @param registry the meter registry
   * @return the contact search timer
   */
  @Bean
  public Timer contactSearchTimer(MeterRegistry registry) {
    return Timer.builder("contact.search.time")
        .description("Time taken to search contacts")
        .tag("service", "contact-service")
        .register(registry);
  }
}
