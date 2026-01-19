package com.iqscaffold.pipelineservice.dashboard;

import java.time.LocalDate;

import com.iqscaffold.pipelineservice.dashboard.dto.DashboardDtos;

/**
 * Service interface for dashboard statistics and metrics.
 */
public interface DashboardService {

  /**
   * Gets dashboard statistics including lead counts by stage and source.
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate   Optional end date for filtering (inclusive)
   * @return Dashboard statistics
   */
  DashboardDtos.DashboardStatsResponse getDashboardStats(LocalDate startDate, LocalDate endDate);

  /**
   * Gets conversion metrics including conversion rate and velocity.
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate   Optional end date for filtering (inclusive)
   * @return Conversion metrics
   */
  DashboardDtos.ConversionMetricsResponse getConversionMetrics(LocalDate startDate, LocalDate endDate);
}
