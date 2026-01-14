package com.iqscaffold.leadservice.lead;

import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.lead.dto.LeadMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST API for lead management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Creating new leads</li>
 *   <li>Retrieving lead details</li>
 *   <li>Updating lead information</li>
 *   <li>Deleting leads</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>Lead creation: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Lead viewing: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Lead updating: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Lead deletion: Requires ADMIN or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Leads", description = "Lead management operations")
@SecurityRequirement(name = "bearerAuth")
public class LeadRestResource {

  private final LeadService leadService;

  public LeadRestResource(final LeadService leadService) {
    this.leadService = leadService;
  }

  /**
   * Creates a new lead.
   *
   * @param request The lead creation request
   * @return The created lead
   */
  @Operation(
      summary = "Create lead",
      description = "Creates a new lead with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Lead created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "409", description = "Lead with email already exists")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<LeadDtos.LeadResponse> createLead(
      @Valid @RequestBody LeadDtos.CreateLeadRequest request) {
    String userId = getCurrentUserId();
    LeadDtos.LeadResponse response = leadService.createLead(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves a lead by ID.
   *
   * @param id The lead ID
   * @return The lead details
   */
  @Operation(
      summary = "Get lead by ID",
      description = "Retrieves a specific lead by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lead found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<LeadDtos.LeadResponse> getLeadById(@PathVariable Long id) {
    LeadDtos.LeadResponse response = leadService.getLeadResponseById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists leads with optional search and filtering.
   *
   * @param search Search term to match against name, email, company, or phone
   * @param source Filter by lead source
   * @param status Filter by lead status
   * @param assignedTo Filter by assigned user
   * @param pageable Pagination parameters (page, size, sort)
   * @return Paginated list of leads
   */
  @Operation(
      summary = "List leads",
      description = "Retrieves a paginated list of leads with optional search and filtering. "
          + "Search term matches against first name, last name, email, company, and phone. "
          + "Multiple filters are combined with AND logic.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Leads retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Page<LeadDtos.LeadResponse>> listLeads(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) LeadStatus status,
      @RequestParam(required = false) String assignedTo,
      Pageable pageable) {
    Page<Lead> leads = leadService.findLeadsWithFilters(search, source, status, assignedTo, pageable);
    Page<LeadDtos.LeadResponse> response = leads.map(LeadMapper::toResponse);
    return ResponseEntity.ok(response);
  }

  /**
   * Updates an existing lead.
   *
   * @param id The lead ID
   * @param request The lead update request
   * @return The updated lead
   */
  @Operation(
      summary = "Update lead",
      description = "Updates an existing lead with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lead updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead not found"),
      @ApiResponse(responseCode = "409", description = "Lead with email already exists")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<LeadDtos.LeadResponse> updateLead(
      @PathVariable Long id,
      @Valid @RequestBody LeadDtos.UpdateLeadRequest request) {
    String userId = getCurrentUserId();
    LeadDtos.LeadResponse response = leadService.updateLead(id, request, userId);
    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a lead.
   *
   * @param id The lead ID
   * @return No content
   */
  @Operation(
      summary = "Delete lead",
      description = "Deletes a lead by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Lead deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Lead not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteLead(@PathVariable Long id) {
    leadService.deleteLead(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets lead counts grouped by source.
   * <p>
   * Returns statistics showing how many leads came from each source.
   * Optionally filter by date range based on lead creation date.
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate Optional end date for filtering (inclusive)
   * @return Map of source to count
   */
  @Operation(
      summary = "Get lead counts by source",
      description = "Retrieves lead counts grouped by source. " +
                    "Optionally filter by date range based on lead creation date.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lead counts retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/stats/by-source")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Map<String, Long>> getLeadCountsBySource(
      @RequestParam(required = false)
      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
      LocalDate startDate,
      @RequestParam(required = false)
      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
      LocalDate endDate) {
    
    LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
    
    Map<String, Long> counts = leadService.getLeadCountsBySource(startDateTime, endDateTime);
    return ResponseEntity.ok(counts);
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
