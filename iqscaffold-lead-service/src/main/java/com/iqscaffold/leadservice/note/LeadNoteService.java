package com.iqscaffold.leadservice.note;

import java.util.List;

import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;

/**
 * Service interface for lead note operations.
 */
public interface LeadNoteService {

  /**
   * Creates a new note for a lead.
   *
   * @param leadId    The lead ID
   * @param request   The note creation request
   * @param createdBy The user creating the note
   * @return The created note response
   */
  LeadNoteDtos.LeadNoteResponse createNote(Long leadId, LeadNoteDtos.CreateLeadNoteRequest request, String createdBy);

  /**
   * Retrieves all notes for a lead, ordered by creation date descending.
   *
   * @param leadId The lead ID
   * @return List of note responses
   */
  List<LeadNoteDtos.LeadNoteResponse> getNotesByLeadId(Long leadId);

  /**
   * Updates an existing note.
   *
   * @param leadId    The lead ID
   * @param noteId    The note ID
   * @param request   The note update request
   * @param updatedBy The user updating the note
   * @return The updated note response
   */
  LeadNoteDtos.LeadNoteResponse updateNote(Long leadId, Long noteId, LeadNoteDtos.UpdateLeadNoteRequest request, String updatedBy);

  /**
   * Deletes a note.
   *
   * @param leadId The lead ID
   * @param noteId The note ID
   */
  void deleteNote(Long leadId, Long noteId);
}
