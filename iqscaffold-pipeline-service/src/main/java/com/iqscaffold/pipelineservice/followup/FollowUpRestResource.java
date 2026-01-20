package com.iqscaffold.pipelineservice.followup;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

import com.iqscaffold.pipelineservice.followup.dto.FollowUpDtos;
import com.iqscaffold.pipelineservice.followup.dto.FollowUpMapper;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST API for follow-up management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 * <li>Scheduling follow-ups with leads</li>
 * <li>Listing follow-ups with pagination</li>
 * <li>Getting today's follow-ups</li>
 * <li>Getting overdue follow-ups</li>
 * <li>Marking follow-ups as completed</li>
 * <li>Deleting follow-ups</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 * <li>All operations require USER, ADMIN, or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pipeline/follow-ups")
@Tag(name = "Follow-Ups", description = "Follow-up management operations")
@SecurityRequirement(name = "bearerAuth")
public class FollowUpRestResource {

  private final FollowUpService followUpService;

  public FollowUpRestResource(final FollowUpService followUpService) {
    this.followUpService = followUpService;
  }

  /**
   * Schedules a new follow-up for a lead.
   * <p>
   * Creates a follow-up with the specified due date, title, and optional
   * description.
   * The follow-up is created in PENDING status.
   *
   * @param request The follow-up creation request
   * @return The created follow-up
   */
  @Operation(summary = "Schedule a follow-up", description = "Creates a new follow-up for a lead with the specified due date and details")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Follow-up scheduled successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<FollowUpDtos.FollowUpResponse> scheduleFollowUp(
      @Valid @RequestBody FollowUpDtos.CreateFollowUpRequest request) {

    String userId = getCurrentUserId();

    // Convert request to entity
    FollowUp followUp = FollowUpMapper.toEntity(request, userId);

    // Schedule the follow-up
    FollowUp created = followUpService.scheduleFollowUp(followUp);

    // Build location URI
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getId())
        .toUri();

    FollowUpDtos.FollowUpResponse response = FollowUpMapper.toResponse(created);
    return ResponseEntity.created(location).body(response);
  }

  /**
   * Lists all follow-ups with pagination.
   * <p>
   * Returns paginated results sorted by due date ascending by default.
   *
   * @param pageable Pagination parameters
   * @return Page of follow-ups
   */
  @Operation(summary = "List follow-ups", description = "Retrieves all follow-ups with pagination. Results are sorted by due date ascending by default.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Follow-ups retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Page<FollowUpDtos.FollowUpResponse>> listFollowUps(
      @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {

    Page<FollowUp> followUps = followUpService.getAllFollowUps(pageable);
    Page<FollowUpDtos.FollowUpResponse> response = followUps.map(FollowUpMapper::toResponse);

    return ResponseEntity.ok(response);
  }

  /**
   * Gets today's follow-ups.
   * <p>
   * Returns all pending follow-ups with due dates matching the current date.
   *
   * @return List of today's follow-ups
   */
  @Operation(summary = "Get today's follow-ups", description = "Retrieves all pending follow-ups with due dates matching the current date")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Today's follow-ups retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/today")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<FollowUpDtos.FollowUpResponse>> getTodaysFollowUps() {
    List<FollowUp> followUps = followUpService.getTodayFollowUps();
    List<FollowUpDtos.FollowUpResponse> response = followUps.stream()
        .map(FollowUpMapper::toResponse)
        .toList();

    return ResponseEntity.ok(response);
  }

  /**
   * Gets overdue follow-ups.
   * <p>
   * Returns all pending follow-ups with due dates before the current date and
   * time.
   *
   * @return List of overdue follow-ups
   */
  @Operation(summary = "Get overdue follow-ups", description = "Retrieves all pending follow-ups with due dates before the current date and time")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Overdue follow-ups retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/overdue")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<FollowUpDtos.FollowUpResponse>> getOverdueFollowUps() {
    List<FollowUp> followUps = followUpService.getOverdueFollowUps();
    List<FollowUpDtos.FollowUpResponse> response = followUps.stream()
        .map(FollowUpMapper::toResponse)
        .toList();

    return ResponseEntity.ok(response);
  }

  /**
   * Updates a follow-up.
   * <p>
   * Updates the follow-up's title, description, due date, priority, or assigned
   * user.
   *
   * @param id      The follow-up ID
   * @param request The update request
   * @return The updated follow-up
   */
  @Operation(summary = "Update follow-up", description = "Updates the follow-up's title, description, due date, priority, or assigned user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Follow-up updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Follow-up not found")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<FollowUpDtos.FollowUpResponse> updateFollowUp(
      @Parameter(description = "Follow-up ID") @PathVariable Long id,
      @Valid @RequestBody FollowUpDtos.UpdateFollowUpRequest request) {

    String userId = getCurrentUserId();

    // Get existing follow-up
    FollowUp existing = followUpService.getFollowUpById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Follow-up", "id", id));

    // Update from request
    FollowUpMapper.updateFromRequest(existing, request, userId);

    // Save updated follow-up
    FollowUp updated = followUpService.updateFollowUp(id, existing);

    FollowUpDtos.FollowUpResponse response = FollowUpMapper.toResponse(updated);
    return ResponseEntity.ok(response);
  }

  /**
   * Marks a follow-up as completed.
   * <p>
   * Updates the follow-up status to COMPLETED and sets the completion timestamp.
   *
   * @param id The follow-up ID
   * @return The updated follow-up
   */
  @Operation(summary = "Mark follow-up as completed", description = "Updates the follow-up status to COMPLETED and records the completion timestamp")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Follow-up marked as completed successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Follow-up not found")
  })
  @PutMapping("/{id}/complete")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<FollowUpDtos.FollowUpResponse> completeFollowUp(
      @Parameter(description = "Follow-up ID") @PathVariable Long id) {

    FollowUp completed = followUpService.completeFollowUp(id);
    FollowUpDtos.FollowUpResponse response = FollowUpMapper.toResponse(completed);

    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a follow-up.
   * <p>
   * Permanently removes the follow-up from the system.
   *
   * @param id The follow-up ID
   * @return No content response
   */
  @Operation(summary = "Delete follow-up", description = "Permanently removes a follow-up from the system")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Follow-up deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Follow-up not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteFollowUp(
      @Parameter(description = "Follow-up ID") @PathVariable Long id) {

    followUpService.deleteFollowUp(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Extracts the current user ID from the JWT token.
   *
   * @return The user ID, or "system" if not available
   */
  private String getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String userId = jwt.getClaimAsString("userId");
      if (userId != null) {
        return userId;
      }
      // Fallback to subject if userId claim not present
      return jwt.getSubject();
    }
    return "system";
  }
}
