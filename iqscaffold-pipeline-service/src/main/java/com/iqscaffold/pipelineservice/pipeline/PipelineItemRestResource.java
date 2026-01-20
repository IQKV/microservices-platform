package com.iqscaffold.pipelineservice.pipeline;

import jakarta.validation.Valid;

import com.iqscaffold.pipelineservice.pipeline.dto.PipelineItemDtos;
import com.iqscaffold.pipelineservice.pipeline.dto.PipelineItemMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for pipeline item management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Listing pipeline items with optional stage filtering</li>
 *   <li>Getting a specific pipeline item by ID</li>
 *   <li>Moving leads to different pipeline stages</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>All operations require USER, ADMIN, or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pipeline/items")
@Tag(name = "Pipeline Items", description = "Pipeline item management operations")
@SecurityRequirement(name = "bearerAuth")
public class PipelineItemRestResource {

  private final PipelineItemService pipelineItemService;

  public PipelineItemRestResource(final PipelineItemService pipelineItemService) {
    this.pipelineItemService = pipelineItemService;
  }

  /**
   * Lists pipeline items with optional filtering by stage.
   * <p>
   * Returns paginated results sorted by creation date descending by default.
   *
   * @param stageId  Optional stage ID to filter by
   * @param pageable Pagination parameters
   * @return Page of pipeline items
   */
  @Operation(
      summary = "List pipeline items",
      description = "Retrieves pipeline items with optional filtering by stage. Results are paginated.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Pipeline items retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Page<PipelineItemDtos.PipelineItemResponse>> listPipelineItems(
      @Parameter(description = "Optional stage ID to filter by")
      @RequestParam(required = false) Long stageId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<PipelineItem> items;
    if (stageId != null) {
      items = pipelineItemService.getPipelineItemsByStage(stageId, pageable);
    } else {
      items = pipelineItemService.getAllPipelineItems(pageable);
    }

    Page<PipelineItemDtos.PipelineItemResponse> response = items.map(PipelineItemMapper::toResponse);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets a specific pipeline item by ID.
   *
   * @param id The pipeline item ID
   * @return The pipeline item
   */
  @Operation(
      summary = "Get pipeline item by ID",
      description = "Retrieves a specific pipeline item by its unique identifier")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Pipeline item retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Pipeline item not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PipelineItemDtos.PipelineItemResponse> getPipelineItem(
      @Parameter(description = "Pipeline item ID")
      @PathVariable Long id) {

    PipelineItem item = pipelineItemService.getPipelineItemById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline item", "id", id));

    PipelineItemDtos.PipelineItemResponse response = PipelineItemMapper.toResponse(item);
    return ResponseEntity.ok(response);
  }

  /**
   * Moves a lead to a different pipeline stage.
   * <p>
   * Updates the pipeline item's stage and resets the entered_stage_at timestamp.
   * This operation logs the stage change in the activity log.
   *
   * @param id      The pipeline item ID
   * @param request The move request containing the target stage ID
   * @return The updated pipeline item
   */
  @Operation(
      summary = "Move lead to different stage",
      description = "Moves a lead to a different pipeline stage and updates the stage entry timestamp")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lead moved to new stage successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid stage ID"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Pipeline item or stage not found")
  })
  @PutMapping("/{id}/stage")
  @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_MANAGER', 'CRM_ACCESS', 'CRM_ADMIN', 'USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<PipelineItemDtos.PipelineItemResponse> moveToStage(
      @Parameter(description = "Pipeline item ID")
      @PathVariable Long id,
      @Valid @RequestBody PipelineItemDtos.MoveToStageRequest request) {

    String userId = getCurrentUserId();

    // Move the item to the new stage
    PipelineItem updated = pipelineItemService.moveToStage(id, request.stageId());
    updated.setUpdatedBy(userId);

    PipelineItemDtos.PipelineItemResponse response = PipelineItemMapper.toResponse(updated);
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
