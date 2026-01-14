package com.iqscaffold.leadservice.note.dto;

import com.iqscaffold.leadservice.note.LeadNote;

/**
 * Mapper for converting between LeadNote entities and DTOs.
 */
public final class LeadNoteMapper {

  private LeadNoteMapper() {
    // Utility class
  }

  /**
   * Converts a LeadNote entity to a LeadNoteResponse DTO.
   *
   * @param note The LeadNote entity
   * @return The LeadNoteResponse DTO
   */
  public static LeadNoteDtos.LeadNoteResponse toResponse(final LeadNote note) {
    if (note == null) {
      return null;
    }

    return new LeadNoteDtos.LeadNoteResponse(
        note.getId(),
        note.getLead() != null ? note.getLead().getId() : null,
        note.getContent(),
        note.getIsPinned(),
        note.getCreatedAt(),
        note.getUpdatedAt(),
        note.getCreatedBy(),
        note.getUpdatedBy()
    );
  }
}
