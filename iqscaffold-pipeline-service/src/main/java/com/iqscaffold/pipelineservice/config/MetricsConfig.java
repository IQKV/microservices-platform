package com.iqscaffold.pipelineservice.config;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for Pipeline Service observability.
 *
 * <p>
 * This configuration provides custom Prometheus metrics for monitoring
 * pipeline operations, conversion tracking, and follow-up management.
 *
 * <h3>Custom Metrics:</h3>
 * <ul>
 * <li><strong>pipeline.stage.changed</strong> - Counter for stage transition
 * events</li>
 * <li><strong>pipeline.lead.converted</strong> - Counter for lead conversion
 * events (Won stage)</li>
 * <li><strong>pipeline.lead.lost</strong> - Counter for lost lead events</li>
 * <li><strong>pipeline.followup.scheduled</strong> - Counter for follow-up
 * scheduling</li>
 * <li><strong>pipeline.followup.completed</strong> - Counter for completed
 * follow-ups</li>
 * <li><strong>pipeline.conversion.rate</strong> - Gauge for conversion rate
 * percentage</li>
 * <li><strong>pipeline.stage.duration</strong> - Timer for time spent in
 * stages</li>
 * </ul>
 *
 * @see MeterRegistry
 */
@Configuration
public class MetricsConfig {

  private final AtomicInteger totalLeads = new AtomicInteger(0);
  private final AtomicInteger convertedLeads = new AtomicInteger(0);

  /**
   * Counter for tracking stage change events.
   *
   * @param registry the meter registry
   * @return the stage changed counter
   */
  @Bean
  public Counter stageChangedCounter(MeterRegistry registry) {
    return Counter.builder("pipeline.stage.changed")
        .description("Total number of pipeline stage changes")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Counter for tracking lead conversion events (Won stage).
   *
   * @param registry the meter registry
   * @return the lead converted counter
   */
  @Bean
  public Counter leadConvertedCounter(MeterRegistry registry) {
    return Counter.builder("pipeline.lead.converted")
        .description("Total number of leads converted (Won)")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Counter for tracking lost lead events.
   *
   * @param registry the meter registry
   * @return the lead lost counter
   */
  @Bean
  public Counter leadLostCounter(MeterRegistry registry) {
    return Counter.builder("pipeline.lead.lost")
        .description("Total number of leads lost")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Counter for tracking follow-up scheduling events.
   *
   * @param registry the meter registry
   * @return the follow-up scheduled counter
   */
  @Bean
  public Counter followUpScheduledCounter(MeterRegistry registry) {
    return Counter.builder("pipeline.followup.scheduled")
        .description("Total number of follow-ups scheduled")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Counter for tracking completed follow-up events.
   *
   * @param registry the meter registry
   * @return the follow-up completed counter
   */
  @Bean
  public Counter followUpCompletedCounter(MeterRegistry registry) {
    return Counter.builder("pipeline.followup.completed")
        .description("Total number of follow-ups completed")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Gauge for tracking conversion rate percentage.
   * This is calculated as (converted leads / total leads) * 100.
   *
   * @param registry the meter registry
   * @return the conversion rate gauge
   */
  @Bean
  public Gauge conversionRateGauge(final MeterRegistry registry) {
    return Gauge.builder("pipeline.conversion.rate", this, config -> {
      int total = config.totalLeads.get();
      int converted = config.convertedLeads.get();
      return total > 0 ? (converted * 100.0 / total) : 0.0;
    })
        .description("Lead conversion rate percentage")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Timer for measuring time spent in pipeline stages.
   *
   * @param registry the meter registry
   * @return the stage duration timer
   */
  @Bean
  public Timer stageDurationTimer(MeterRegistry registry) {
    return Timer.builder("pipeline.stage.duration")
        .description("Time spent in pipeline stages")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Timer for measuring dashboard statistics calculation time.
   *
   * @param registry the meter registry
   * @return the dashboard stats timer
   */
  @Bean
  public Timer dashboardStatsTimer(MeterRegistry registry) {
    return Timer.builder("pipeline.dashboard.stats.time")
        .description("Time taken to calculate dashboard statistics")
        .tag("service", "pipeline-service")
        .register(registry);
  }

  /**
   * Increment the total leads counter for conversion rate calculation.
   */
  public void incrementTotalLeads() {
    totalLeads.incrementAndGet();
  }

  /**
   * Increment the converted leads counter for conversion rate calculation.
   */
  public void incrementConvertedLeads() {
    convertedLeads.incrementAndGet();
  }

  /**
   * Get the current total leads count.
   *
   * @return the total leads count
   */
  public int getTotalLeads() {
    return totalLeads.get();
  }

  /**
   * Get the current converted leads count.
   *
   * @return the converted leads count
   */
  public int getConvertedLeads() {
    return convertedLeads.get();
  }
}
