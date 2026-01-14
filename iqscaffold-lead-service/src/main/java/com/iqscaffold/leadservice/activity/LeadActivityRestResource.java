package com.iqscaffold.leadservice.activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for lead activity operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Retrieving activity timeline for a lead</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>Activity viewing: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/leads/{leadId}/activities")
@Tag(name = "Lead Activities", description = "Lead activity log operations")
@SecurityRequirement(name = "bearerAuth")
public class LeadActivityRestResource {

  private final ActivityLogService activityLogService;

  public LeadActivityRestResource(final ActivityLogService activityLogService) {
    this.activityLogService = activityLogService;
  }

  /**
   * Retrieves the activity timeline for a specific lead.
   * <p>
   * Returns all activities for the lead sorted by creation timestamp in descending order
   * (newest first). Activities include lead creation, updates, notes, stage changes,
   * and follow-up events.
   *
   * @param leadId The lead ID
   * @return List of activities for the lead
   */
  @Operation(
      summary = "Get lead activity timeline",
      description = "Retrieves the complete activity timeline for a lead, including all "
          + "interactions and state changes. Activities are sorted by timestamp in descending "
          + "order (newest first).")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Activities retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead not found")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<LeadActivityDto>> getLeadActivities(@PathVariable Long leadId) {
    List<LeadActivity> activities = activityLogService.getLeadActivityTimeline(leadId);
    List<LeadActivityDto> response = activities.stream()
        .map(LeadActivityMapper::toDto)
        .toList();
    return ResponseEntity.ok(response);
  }
}
