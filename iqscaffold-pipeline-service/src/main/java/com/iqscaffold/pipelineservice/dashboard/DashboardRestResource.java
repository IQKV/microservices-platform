package com.iqscaffold.pipelineservice.dashboard;

import java.time.LocalDate;

import com.iqscaffold.pipelineservice.dashboard.dto.DashboardDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for dashboard statistics and metrics.
 * <p>
 * Provides endpoints for:
 * <ul>
 * <li>Getting pipeline statistics (leads by stage and source)</li>
 * <li>Getting conversion metrics (conversion rate and velocity)</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 * <li>All operations require USER, ADMIN, or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pipeline/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and metrics operations")
@SecurityRequirement(name = "bearerAuth")
public class DashboardRestResource {

  private final DashboardService dashboardService;

  public DashboardRestResource(final DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /**
   * Gets dashboard statistics including lead counts by stage and source.
   * <p>
   * Returns aggregated statistics for the pipeline including:
   * <ul>
   * <li>Lead counts by pipeline stage</li>
   * <li>Lead counts by source</li>
   * <li>Total, active, won, and lost lead counts</li>
   * </ul>
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate   Optional end date for filtering (inclusive)
   * @return Dashboard statistics
   */
  @Operation(summary = "Get dashboard statistics", description = "Retrieves pipeline statistics including lead counts by stage and source. "
      +
      "Optionally filter by date range based on lead creation date.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/stats")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<DashboardDtos.DashboardStatsResponse> getDashboardStats(
      @Parameter(description = "Start date for filtering (inclusive, format: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @Parameter(description = "End date for filtering (inclusive, format: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    DashboardDtos.DashboardStatsResponse stats = dashboardService.getDashboardStats(startDate, endDate);
    return ResponseEntity.ok(stats);
  }

  /**
   * Gets conversion metrics including conversion rate and velocity.
   * <p>
   * Returns conversion analytics including:
   * <ul>
   * <li>Conversion rate (percentage of leads converted to Won)</li>
   * <li>Average time to convert (days from creation to Won)</li>
   * <li>Stage velocity (average days spent in each stage)</li>
   * </ul>
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate   Optional end date for filtering (inclusive)
   * @return Conversion metrics
   */
  @Operation(summary = "Get conversion metrics", description = "Retrieves conversion metrics including conversion rate, average time to convert, "
      +
      "and stage velocity. Optionally filter by date range based on lead creation date.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Conversion metrics retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/conversion")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<DashboardDtos.ConversionMetricsResponse> getConversionMetrics(
      @Parameter(description = "Start date for filtering (inclusive, format: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @Parameter(description = "End date for filtering (inclusive, format: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    DashboardDtos.ConversionMetricsResponse metrics = dashboardService.getConversionMetrics(startDate, endDate);
    return ResponseEntity.ok(metrics);
  }
}
