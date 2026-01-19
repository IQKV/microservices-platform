package com.iqscaffold.pipelineservice.pipeline;

import jakarta.validation.Valid;
import java.util.List;

import com.iqscaffold.pipelineservice.pipeline.dto.PipelineStageDtos;
import com.iqscaffold.pipelineservice.pipeline.dto.PipelineStageMapper;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for pipeline stage management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Listing pipeline stages</li>
 *   <li>Creating new stages</li>
 *   <li>Updating stage information</li>
 *   <li>Deleting stages (with validation)</li>
 *   <li>Reordering stages</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>Stage listing: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Stage creation: Requires ADMIN or SUPER_ADMIN role</li>
 *   <li>Stage updating: Requires ADMIN or SUPER_ADMIN role</li>
 *   <li>Stage deletion: Requires ADMIN or SUPER_ADMIN role</li>
 *   <li>Stage reordering: Requires ADMIN or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pipeline/stages")
@Tag(name = "Pipeline Stages", description = "Pipeline stage management operations")
@SecurityRequirement(name = "bearerAuth")
public class PipelineStageRestResource {

  private final PipelineStageService stageService;

  public PipelineStageRestResource(final PipelineStageService stageService) {
    this.stageService = stageService;
  }

  /**
   * Lists all pipeline stages ordered by display order.
   *
   * @return List of all pipeline stages
   */
  @Operation(
      summary = "List pipeline stages",
      description = "Retrieves all pipeline stages ordered by display order")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Stages retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<PipelineStageDtos.StageResponse>> listStages() {
    List<PipelineStage> stages = stageService.getAllStages();
    List<PipelineStageDtos.StageResponse> response = stages.stream()
        .map(PipelineStageMapper::toResponse)
        .toList();
    return ResponseEntity.ok(response);
  }

  /**
   * Creates a new pipeline stage.
   *
   * @param request The stage creation request
   * @return The created stage
   */
  @Operation(
      summary = "Create pipeline stage",
      description = "Creates a new pipeline stage with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Stage created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "409", description = "Stage with name already exists")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PipelineStageDtos.StageResponse> createStage(
      @Valid @RequestBody PipelineStageDtos.CreateStageRequest request) {
    String userId = getCurrentUserId();
    PipelineStage stage = PipelineStageMapper.toEntity(request, userId);
    PipelineStage created = stageService.createStage(stage);
    PipelineStageDtos.StageResponse response = PipelineStageMapper.toResponse(created);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Updates an existing pipeline stage.
   *
   * @param id      The stage ID
   * @param request The stage update request
   * @return The updated stage
   */
  @Operation(
      summary = "Update pipeline stage",
      description = "Updates an existing pipeline stage with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Stage updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Stage not found"),
      @ApiResponse(responseCode = "409", description = "Stage with name already exists")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PipelineStageDtos.StageResponse> updateStage(
      @PathVariable Long id,
      @Valid @RequestBody PipelineStageDtos.UpdateStageRequest request) {
    String userId = getCurrentUserId();

    // Fetch existing stage to update
    PipelineStage existingStage = stageService.getStageById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage", "id", id));

    // Update the stage with request data
    PipelineStageMapper.updateEntity(existingStage, request, userId);

    // Save the updated stage
    PipelineStage updated = stageService.updateStage(id, existingStage);
    PipelineStageDtos.StageResponse response = PipelineStageMapper.toResponse(updated);
    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a pipeline stage.
   * <p>
   * Deletion is only allowed if no leads are currently in this stage.
   *
   * @param id The stage ID
   * @return No content
   */
  @Operation(
      summary = "Delete pipeline stage",
      description = "Deletes a pipeline stage by its ID. Deletion is only allowed if no leads are in this stage.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Stage deleted successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot delete stage with leads in it"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Stage not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteStage(@PathVariable Long id) {
    stageService.deleteStage(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Reorders a pipeline stage to a new position.
   * <p>
   * Updates the display order of the specified stage and adjusts other stages accordingly.
   *
   * @param id       The stage ID
   * @param newOrder The new display order position
   * @return The updated stage
   */
  @Operation(
      summary = "Reorder pipeline stage",
      description = "Updates the display order of a pipeline stage and adjusts other stages accordingly")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Stage reordered successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid order value"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Stage not found")
  })
  @PutMapping("/{id}/order")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PipelineStageDtos.StageResponse> reorderStage(
      @PathVariable Long id,
      @RequestParam Integer newOrder) {
    PipelineStage updated = stageService.updateStageOrder(id, newOrder);
    PipelineStageDtos.StageResponse response = PipelineStageMapper.toResponse(updated);
    return ResponseEntity.ok(response);
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
