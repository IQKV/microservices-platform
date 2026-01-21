package com.iqscaffold.leadservice.lead;

import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.lead.dto.LeadMapper;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller demonstrating entity graph usage for optimized lead data fetching.
 * These endpoints showcase different fetching strategies based on use case requirements.
 */
@RestController
@RequestMapping("/api/v1/leads/optimized")
@Tag(name = "Lead Entity Graphs", description = "Optimized lead data fetching using entity graphs")
public class LeadEntityGraphController {

  private final LeadService leadService;
  private final LeadMapper leadMapper;

  public LeadEntityGraphController(final LeadService leadService, final LeadMapper leadMapper) {
    this.leadService = leadService;
    this.leadMapper = leadMapper;
  }

  @GetMapping("/{id}/with-notes")
  @Operation(
      summary = "Get lead with notes",
      description = "Fetches a lead with all associated notes using entity graph optimization. " +
                   "Ideal for lead detail views where note history is needed."
  )
  public ResponseEntity<LeadDtos.LeadResponse> getLeadWithNotes(
      @Parameter(description = "Lead ID") @PathVariable final Long id) {
    
    return leadService.getLeadWithNotes(id)
        .map(leadMapper::toResponse)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));
  }

  @GetMapping("/{id}/with-activities")
  @Operation(
      summary = "Get lead with activities",
      description = "Fetches a lead with all associated activities using entity graph optimization. " +
                   "Perfect for timeline views and audit trails."
  )
  public ResponseEntity<LeadDtos.LeadResponse> getLeadWithActivities(
      @Parameter(description = "Lead ID") @PathVariable final Long id) {
    
    return leadService.getLeadWithActivities(id)
        .map(leadMapper::toResponse)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));
  }

  @GetMapping("/{id}/complete")
  @Operation(
      summary = "Get lead with complete history",
      description = "Fetches a lead with all notes and activities using entity graph optimization. " +
                   "Use for comprehensive views requiring all related data. " +
                   "Note: This is the heaviest operation and should be used sparingly."
  )
  public ResponseEntity<LeadDtos.LeadResponse> getLeadWithCompleteHistory(
      @Parameter(description = "Lead ID") @PathVariable final Long id) {
    
    return leadService.getLeadWithCompleteHistory(id)
        .map(leadMapper::toResponse)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));
  }

  @GetMapping("/assigned/{assignedTo}/basic")
  @Operation(
      summary = "Get basic leads by assigned user",
      description = "Fetches leads assigned to a user with minimal data (no collections). " +
                   "Optimized for listing views and dashboards where performance is critical."
  )
  public ResponseEntity<Page<LeadDtos.LeadResponse>> getBasicLeadsByAssignedTo(
      @Parameter(description = "Assigned user ID") @PathVariable final String assignedTo,
      final Pageable pageable) {
    
    Page<LeadDtos.LeadResponse> leads = leadService.getBasicLeadsByAssignedTo(assignedTo, pageable)
        .map(leadMapper::toResponse);
    
    return ResponseEntity.ok(leads);
  }

  @GetMapping("/search/with-notes")
  @Operation(
      summary = "Search leads with notes",
      description = "Advanced search that includes notes in the result set. " +
                   "Use when search results need note context for decision making. " +
                   "More expensive than basic search but provides richer data."
  )
  public ResponseEntity<Page<LeadDtos.LeadResponse>> searchLeadsWithNotes(
      @Parameter(description = "Search term (searches name, email, company, phone)")
      @RequestParam(required = false) final String searchTerm,
      
      @Parameter(description = "Filter by lead source")
      @RequestParam(required = false) final String source,
      
      @Parameter(description = "Filter by lead status")
      @RequestParam(required = false) final LeadStatus status,
      
      @Parameter(description = "Filter by assigned user")
      @RequestParam(required = false) final String assignedTo,
      
      final Pageable pageable) {
    
    Page<LeadDtos.LeadResponse> leads = leadService.searchLeadsWithNotes(
        searchTerm, source, status, assignedTo, pageable)
        .map(leadMapper::toResponse);
    
    return ResponseEntity.ok(leads);
  }

  @GetMapping("/performance-comparison/{id}")
  @Operation(
      summary = "Performance comparison endpoint",
      description = "Demonstrates the difference between standard and entity graph fetching. " +
                   "This endpoint is for educational purposes and performance testing."
  )
  public ResponseEntity<PerformanceComparisonResponse> performanceComparison(
      @Parameter(description = "Lead ID") @PathVariable final Long id) {
    
    long startTime, endTime;
    
    // Standard fetch (may cause N+1 queries)
    startTime = System.currentTimeMillis();
    var standardLead = leadService.getLeadById(id);
    endTime = System.currentTimeMillis();
    long standardFetchTime = endTime - startTime;
    
    // Entity graph fetch (optimized)
    startTime = System.currentTimeMillis();
    var optimizedLead = leadService.getLeadWithCompleteHistory(id);
    endTime = System.currentTimeMillis();
    long optimizedFetchTime = endTime - startTime;
    
    return ResponseEntity.ok(new PerformanceComparisonResponse(
        standardFetchTime,
        optimizedFetchTime,
        standardLead.isPresent(),
        optimizedLead.isPresent(),
        "Entity graph fetching typically reduces database round trips and improves performance"
    ));
  }

  /**
   * Response object for performance comparison.
   */
  public record PerformanceComparisonResponse(
      long standardFetchTimeMs,
      long optimizedFetchTimeMs,
      boolean standardLeadFound,
      boolean optimizedLeadFound,
      String note
  ) {}
}