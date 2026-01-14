package com.iqscaffold.leadservice.note;

import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for lead note management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Adding notes to leads</li>
 *   <li>Retrieving lead notes</li>
 *   <li>Updating note content</li>
 *   <li>Deleting notes</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>All operations: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/leads/{leadId}/notes")
@Tag(name = "Lead Notes", description = "Lead note management operations")
@SecurityRequirement(name = "bearerAuth")
public class LeadNoteRestResource {

  private final LeadNoteService leadNoteService;

  public LeadNoteRestResource(final LeadNoteService leadNoteService) {
    this.leadNoteService = leadNoteService;
  }

  /**
   * Adds a new note to a lead.
   *
   * @param leadId The lead ID
   * @param request The note creation request
   * @return The created note
   */
  @Operation(
      summary = "Add note to lead",
      description = "Creates a new note for the specified lead")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Note created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead not found")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<LeadNoteDtos.LeadNoteResponse> addNote(
      @PathVariable Long leadId,
      @Valid @RequestBody LeadNoteDtos.CreateLeadNoteRequest request) {
    String userId = getCurrentUserId();
    LeadNoteDtos.LeadNoteResponse response = leadNoteService.createNote(leadId, request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves all notes for a lead.
   *
   * @param leadId The lead ID
   * @return List of notes ordered by creation date descending
   */
  @Operation(
      summary = "Get lead notes",
      description = "Retrieves all notes for the specified lead, ordered by creation date descending")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notes retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead not found")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<LeadNoteDtos.LeadNoteResponse>> getLeadNotes(
      @PathVariable Long leadId) {
    List<LeadNoteDtos.LeadNoteResponse> notes = leadNoteService.getNotesByLeadId(leadId);
    return ResponseEntity.ok(notes);
  }

  /**
   * Updates an existing note.
   *
   * @param leadId The lead ID
   * @param noteId The note ID
   * @param request The note update request
   * @return The updated note
   */
  @Operation(
      summary = "Update note",
      description = "Updates an existing note for the specified lead")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Note updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead or note not found")
  })
  @PutMapping("/{noteId}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<LeadNoteDtos.LeadNoteResponse> updateNote(
      @PathVariable Long leadId,
      @PathVariable Long noteId,
      @Valid @RequestBody LeadNoteDtos.UpdateLeadNoteRequest request) {
    String userId = getCurrentUserId();
    LeadNoteDtos.LeadNoteResponse response = leadNoteService.updateNote(leadId, noteId, request, userId);
    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a note.
   *
   * @param leadId The lead ID
   * @param noteId The note ID
   * @return No content
   */
  @Operation(
      summary = "Delete note",
      description = "Deletes a note from the specified lead")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Note deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Lead or note not found")
  })
  @DeleteMapping("/{noteId}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteNote(
      @PathVariable Long leadId,
      @PathVariable Long noteId) {
    leadNoteService.deleteNote(leadId, noteId);
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
