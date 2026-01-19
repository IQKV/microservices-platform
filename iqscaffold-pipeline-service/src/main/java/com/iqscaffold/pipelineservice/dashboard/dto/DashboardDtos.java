package com.iqscaffold.pipelineservice.dashboard.dto;

import java.util.Map;

/**
 * DTOs for dashboard statistics and metrics.
 */
public final class DashboardDtos {

  private DashboardDtos() {
    // Utility class
  }

  /**
   * Dashboard statistics response containing pipeline and lead metrics.
   *
   * @param leadsByStage  Count of leads in each pipeline stage (stage name -> count)
   * @param leadsBySource Count of leads by source (source name -> count)
   * @param totalLeads    Total number of leads in the system
   * @param activeLeads   Number of leads in active (non-final) stages
   * @param wonLeads      Number of leads in Won stage
   * @param lostLeads     Number of leads in Lost stage
   */
  public record DashboardStatsResponse(
      Map<String, Long> leadsByStage,
      Map<String, Long> leadsBySource,
      Long totalLeads,
      Long activeLeads,
      Long wonLeads,
      Long lostLeads
  ) {
  }

  /**
   * Conversion metrics response containing conversion rates and velocity.
   *
   * @param conversionRate       Percentage of leads converted to Won (0-100)
   * @param averageTimeToConvert Average days from creation to Won stage
   * @param stageVelocity        Average days spent in each stage (stage name -> days)
   */
  public record ConversionMetricsResponse(
      Double conversionRate,
      Double averageTimeToConvert,
      Map<String, Double> stageVelocity
  ) {
  }
}
