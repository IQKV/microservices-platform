package com.iqscaffold.pipelineservice.pipeline;

import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.pipelineservice.activity.ActivityType;
import com.iqscaffold.pipelineservice.activity.PipelineActivity;
import com.iqscaffold.pipelineservice.activity.PipelineActivityEntityGraphService;
import com.iqscaffold.pipelineservice.followup.FollowUp;
import com.iqscaffold.pipelineservice.followup.FollowUpEntityGraphService;
import com.iqscaffold.pipelineservice.followup.FollowUpStatus;
import com.iqscaffold.pipelineservice.shared.exception.PipelineItemNotFoundException;
import com.iqscaffold.pipelineservice.shared.exception.PipelineStageNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller demonstrating entity graph usage for optimized pipeline data fetching.
 * These endpoints showcase different fetching strategies based on use case requirements.
 */
@RestController
@RequestMapping("/api/v1/pipeline/optimized")
@Tag(name = "Pipeline Entity Graphs", description = "Optimized pipeline data fetching using entity graphs")
public class PipelineEntityGraphController {

  private final PipelineItemEntityGraphService pipelineItemEntityGraphService;
  private final PipelineStageEntityGraphService pipelineStageEntityGraphService;
  private final FollowUpEntityGraphService followUpEntityGraphService;
  private final PipelineActivityEntityGraphService pipelineActivityEntityGraphService;

  public PipelineEntityGraphController(
      final PipelineItemEntityGraphService pipelineItemEntityGraphService,
      final PipelineStageEntityGraphService pipelineStageEntityGraphService,
      final FollowUpEntityGraphService followUpEntityGraphService,
      final PipelineActivityEntityGraphService pipelineActivityEntityGraphService) {
    this.pipelineItemEntityGraphService = pipelineItemEntityGraphService;
    this.pipelineStageEntityGraphService = pipelineStageEntityGraphService;
    this.followUpEntityGraphService = followUpEntityGraphService;
    this.pipelineActivityEntityGraphService = pipelineActivityEntityGraphService;
  }

  // Pipeline Item Entity Graph Endpoints

  @GetMapping("/items/{id}/with-stage")
  @Operation(
      summary = "Get pipeline item with stage",
      description = "Fetches a pipeline item with its associated stage using entity graph optimization. " +
                    "Ideal for pipeline views where stage details are needed."
  )
  public ResponseEntity<PipelineItem> getPipelineItemWithStage(
      @Parameter(description = "Pipeline Item ID") @PathVariable final Long id) {

    return pipelineItemEntityGraphService.getPipelineItemWithStage(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new PipelineItemNotFoundException("Pipeline item not found with id: " + id));
  }

  @GetMapping("/items/lead/{leadId}/with-stage")
  @Operation(
      summary = "Get pipeline item with stage by lead ID",
      description = "Fetches a pipeline item with its associated stage by lead ID using entity graph optimization. " +
                    "Perfect for lead detail views showing pipeline position."
  )
  public ResponseEntity<PipelineItem> getPipelineItemWithStageByLeadId(
      @Parameter(description = "Lead ID") @PathVariable final Long leadId) {

    return pipelineItemEntityGraphService.getPipelineItemWithStageByLeadId(leadId)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new PipelineItemNotFoundException("Pipeline item not found for lead id: " + leadId));
  }

  @GetMapping("/items/{id}/basic")
  @Operation(
      summary = "Get basic pipeline item",
      description = "Fetches a pipeline item without relationships using entity graph optimization. " +
                    "Optimized for scenarios where only basic item data is needed."
  )
  public ResponseEntity<PipelineItem> getBasicPipelineItem(
      @Parameter(description = "Pipeline Item ID") @PathVariable final Long id) {

    return pipelineItemEntityGraphService.getBasicPipelineItem(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new PipelineItemNotFoundException("Pipeline item not found with id: " + id));
  }

  @GetMapping("/stages/{stageId}/items/with-stage")
  @Operation(
      summary = "Get pipeline items with stage for a specific stage",
      description = "Fetches pipeline items with stage information for a specific stage using entity graph optimization. " +
                    "Ideal for stage-based pipeline views."
  )
  public ResponseEntity<Page<PipelineItem>> getPipelineItemsWithStageByStage(
      @Parameter(description = "Stage ID") @PathVariable final Long stageId,
      final Pageable pageable) {

    Page<PipelineItem> items = pipelineItemEntityGraphService.getPipelineItemsWithStageByStage(stageId, pageable);
    return ResponseEntity.ok(items);
  }

  // Pipeline Stage Entity Graph Endpoints

  @GetMapping("/stages/{id}/with-items")
  @Operation(
      summary = "Get pipeline stage with items",
      description = "Fetches a pipeline stage with all associated pipeline items using entity graph optimization. " +
                    "Ideal for stage detail views and pipeline management."
  )
  public ResponseEntity<PipelineStage> getPipelineStageWithItems(
      @Parameter(description = "Stage ID") @PathVariable final Long id) {

    return pipelineStageEntityGraphService.getPipelineStageWithItems(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new PipelineStageNotFoundException("Pipeline stage not found with id: " + id));
  }

  @GetMapping("/stages/{id}/basic")
  @Operation(
      summary = "Get basic pipeline stage",
      description = "Fetches a pipeline stage without relationships using entity graph optimization. " +
                    "Optimized for scenarios where only basic stage data is needed."
  )
  public ResponseEntity<PipelineStage> getBasicPipelineStage(
      @Parameter(description = "Stage ID") @PathVariable final Long id) {

    return pipelineStageEntityGraphService.getBasicPipelineStage(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new PipelineStageNotFoundException("Pipeline stage not found with id: " + id));
  }

  @GetMapping("/stages/active/with-items")
  @Operation(
      summary = "Get active pipeline stages with items",
      description = "Fetches all active pipeline stages with their items using entity graph optimization. " +
                    "Perfect for complete pipeline views and dashboards."
  )
  public ResponseEntity<List<PipelineStage>> getActivePipelineStagesWithItems() {
    List<PipelineStage> stages = pipelineStageEntityGraphService.getActivePipelineStagesWithItems();
    return ResponseEntity.ok(stages);
  }

  @GetMapping("/stages/all/with-items")
  @Operation(
      summary = "Get all pipeline stages with items",
      description = "Fetches all pipeline stages with their items using entity graph optimization. " +
                    "Useful for administrative views and stage management."
  )
  public ResponseEntity<List<PipelineStage>> getAllPipelineStagesWithItems() {
    List<PipelineStage> stages = pipelineStageEntityGraphService.getAllPipelineStagesWithItems();
    return ResponseEntity.ok(stages);
  }

  // Follow-up Entity Graph Endpoints

  @GetMapping("/follow-ups/lead/{leadId}/basic")
  @Operation(
      summary = "Get basic follow-ups for a lead",
      description = "Fetches follow-ups for a lead using entity graph optimization. " +
                    "Perfect for lead detail views showing follow-up tasks."
  )
  public ResponseEntity<Page<FollowUp>> getBasicFollowUpsByLead(
      @Parameter(description = "Lead ID") @PathVariable final Long leadId,
      final Pageable pageable) {

    Page<FollowUp> followUps = followUpEntityGraphService.getBasicFollowUpsByLead(leadId, pageable);
    return ResponseEntity.ok(followUps);
  }

  @GetMapping("/follow-ups/status/{status}/basic")
  @Operation(
      summary = "Get basic follow-ups by status",
      description = "Fetches follow-ups by status using entity graph optimization. " +
                    "Ideal for status-based follow-up management views."
  )
  public ResponseEntity<Page<FollowUp>> getBasicFollowUpsByStatus(
      @Parameter(description = "Follow-up Status") @PathVariable final FollowUpStatus status,
      final Pageable pageable) {

    Page<FollowUp> followUps = followUpEntityGraphService.getBasicFollowUpsByStatus(status, pageable);
    return ResponseEntity.ok(followUps);
  }

  @GetMapping("/follow-ups/overdue/basic")
  @Operation(
      summary = "Get basic overdue follow-ups",
      description = "Fetches overdue follow-ups using entity graph optimization. " +
                    "Essential for overdue task management and notifications."
  )
  public ResponseEntity<List<FollowUp>> getBasicOverdueFollowUps() {
    List<FollowUp> followUps = followUpEntityGraphService.getBasicOverdueFollowUps(
        FollowUpStatus.PENDING, LocalDateTime.now());
    return ResponseEntity.ok(followUps);
  }

  // Pipeline Activity Entity Graph Endpoints

  @GetMapping("/activities/lead/{leadId}/basic")
  @Operation(
      summary = "Get basic pipeline activities for a lead",
      description = "Fetches pipeline activities for a lead using entity graph optimization. " +
                    "Perfect for lead detail views showing activity history."
  )
  public ResponseEntity<Page<PipelineActivity>> getBasicPipelineActivitiesByLead(
      @Parameter(description = "Lead ID") @PathVariable final Long leadId,
      final Pageable pageable) {

    Page<PipelineActivity> activities = pipelineActivityEntityGraphService.getBasicPipelineActivitiesByLead(leadId, pageable);
    return ResponseEntity.ok(activities);
  }

  @GetMapping("/activities/lead/{leadId}/all/basic")
  @Operation(
      summary = "Get all basic pipeline activities for a lead",
      description = "Fetches all pipeline activities for a lead ordered by creation date using entity graph optimization. " +
                    "Ideal for complete activity timelines and audit trails."
  )
  public ResponseEntity<List<PipelineActivity>> getAllBasicPipelineActivitiesByLead(
      @Parameter(description = "Lead ID") @PathVariable final Long leadId) {

    List<PipelineActivity> activities = pipelineActivityEntityGraphService.getAllBasicPipelineActivitiesByLead(leadId);
    return ResponseEntity.ok(activities);
  }

  @GetMapping("/activities/type/{activityType}/basic")
  @Operation(
      summary = "Get basic pipeline activities by type",
      description = "Fetches pipeline activities by type using entity graph optimization. " +
                    "Useful for activity type filtering and reporting."
  )
  public ResponseEntity<Page<PipelineActivity>> getBasicPipelineActivitiesByType(
      @Parameter(description = "Activity Type") @PathVariable final ActivityType activityType,
      final Pageable pageable) {

    Page<PipelineActivity> activities = pipelineActivityEntityGraphService.getBasicPipelineActivitiesByType(activityType, pageable);
    return ResponseEntity.ok(activities);
  }
}
