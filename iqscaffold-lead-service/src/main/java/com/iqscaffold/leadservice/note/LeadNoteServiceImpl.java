package com.iqscaffold.leadservice.note;

import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;
import com.iqscaffold.leadservice.note.dto.LeadNoteMapper;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import com.iqscaffold.leadservice.shared.exception.LeadNoteNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for lead note operations.
 */
@Service
@Transactional
public class LeadNoteServiceImpl implements LeadNoteService {

  private static final Logger logger = LoggerFactory.getLogger(LeadNoteServiceImpl.class);

  private final LeadNoteRepository leadNoteRepository;
  private final LeadRepository leadRepository;

  public LeadNoteServiceImpl(
      final LeadNoteRepository leadNoteRepository,
      final LeadRepository leadRepository) {
    this.leadNoteRepository = leadNoteRepository;
    this.leadRepository = leadRepository;
  }

  @Override
  public LeadNoteDtos.LeadNoteResponse createNote(
      final Long leadId,
      final LeadNoteDtos.CreateLeadNoteRequest request,
      final String createdBy) {
    logger.debug("Creating note for lead ID: {}", leadId);

    // Verify lead exists
    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with ID: " + leadId));

    // Create note
    LeadNote note = new LeadNote();
    note.setLead(lead);
    note.setContent(request.content());
    note.setIsPinned(request.isPinned() != null ? request.isPinned() : false);
    note.setCreatedBy(createdBy);
    note.setUpdatedBy(createdBy);

    LeadNote savedNote = leadNoteRepository.save(note);
    logger.info("Created note ID: {} for lead ID: {}", savedNote.getId(), leadId);

    return LeadNoteMapper.toResponse(savedNote);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LeadNoteDtos.LeadNoteResponse> getNotesByLeadId(final Long leadId) {
    logger.debug("Retrieving notes for lead ID: {}", leadId);

    // Verify lead exists
    if (!leadRepository.existsById(leadId)) {
      throw new LeadNotFoundException("Lead not found with ID: " + leadId);
    }

    List<LeadNote> notes = leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
    logger.debug("Found {} notes for lead ID: {}", notes.size(), leadId);

    return notes.stream()
        .map(LeadNoteMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  public LeadNoteDtos.LeadNoteResponse updateNote(
      final Long leadId,
      final Long noteId,
      final LeadNoteDtos.UpdateLeadNoteRequest request,
      final String updatedBy) {
    logger.debug("Updating note ID: {} for lead ID: {}", noteId, leadId);

    // Verify lead exists
    if (!leadRepository.existsById(leadId)) {
      throw new LeadNotFoundException("Lead not found with ID: " + leadId);
    }

    // Find note
    LeadNote note = leadNoteRepository.findById(noteId)
        .orElseThrow(() -> new LeadNoteNotFoundException("Note not found with ID: " + noteId));

    // Verify note belongs to the lead
    if (!note.getLead().getId().equals(leadId)) {
      throw new LeadNoteNotFoundException("Note ID: " + noteId + " does not belong to lead ID: " + leadId);
    }

    // Update note
    note.setContent(request.content());
    if (request.isPinned() != null) {
      note.setIsPinned(request.isPinned());
    }
    note.setUpdatedBy(updatedBy);

    LeadNote updatedNote = leadNoteRepository.save(note);
    logger.info("Updated note ID: {} for lead ID: {}", noteId, leadId);

    return LeadNoteMapper.toResponse(updatedNote);
  }

  @Override
  public void deleteNote(final Long leadId, final Long noteId) {
    logger.debug("Deleting note ID: {} for lead ID: {}", noteId, leadId);

    // Verify lead exists
    if (!leadRepository.existsById(leadId)) {
      throw new LeadNotFoundException("Lead not found with ID: " + leadId);
    }

    // Find note
    LeadNote note = leadNoteRepository.findById(noteId)
        .orElseThrow(() -> new LeadNoteNotFoundException("Note not found with ID: " + noteId));

    // Verify note belongs to the lead
    if (!note.getLead().getId().equals(leadId)) {
      throw new LeadNoteNotFoundException("Note ID: " + noteId + " does not belong to lead ID: " + leadId);
    }

    leadNoteRepository.delete(note);
    logger.info("Deleted note ID: {} for lead ID: {}", noteId, leadId);
  }
}
