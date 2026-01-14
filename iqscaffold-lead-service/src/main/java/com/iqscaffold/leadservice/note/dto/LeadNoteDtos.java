package com.iqscaffold.leadservice.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTOs for Lead Note REST API operations.
 * Contains request and response records for lead note management.
 */
public final class LeadNoteDtos {

  private LeadNoteDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a new lead note.
   *
   * @param content  Note content (required, max 5000 chars)
   * @param isPinned Whether the note should be pinned (optional, defaults to false)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateLeadNoteRequest(
      @NotBlank(message = "Note content is required")
      @Size(max = 5000, message = "Note content must not exceed 5000 characters")
      String content,

      Boolean isPinned
  ) {
  }

  /**
   * Request DTO for updating an existing lead note.
   *
   * @param content  Note content (required, max 5000 chars)
   * @param isPinned Whether the note should be pinned (optional)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateLeadNoteRequest(
      @NotBlank(message = "Note content is required")
      @Size(max = 5000, message = "Note content must not exceed 5000 characters")
      String content,

      Boolean isPinned
  ) {
  }

  /**
   * Response DTO for lead note information.
   *
   * @param id        Note's unique identifier
   * @param leadId    Associated lead's ID
   * @param content   Note content
   * @param isPinned  Whether the note is pinned
   * @param createdAt Creation timestamp
   * @param updatedAt Last update timestamp
   * @param createdBy User who created the note
   * @param updatedBy User who last updated the note
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LeadNoteResponse(
      Long id,
      Long leadId,
      String content,
      Boolean isPinned,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy
  ) {
  }
}
