package com.iqscaffold.leadservice.note;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for LeadNote entity operations.
 */
@Repository
public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {

  /**
   * Finds all notes for a specific lead, ordered by creation date descending.
   *
   * @param leadId The lead ID
   * @return List of notes for the lead
   */
  @Query("SELECT n FROM LeadNote n WHERE n.lead.id = :leadId ORDER BY n.createdAt DESC")
  List<LeadNote> findByLeadIdOrderByCreatedAtDesc(@Param("leadId") Long leadId);

  /**
   * Finds all pinned notes for a specific lead.
   *
   * @param leadId The lead ID
   * @return List of pinned notes for the lead
   */
  @Query("SELECT n FROM LeadNote n WHERE n.lead.id = :leadId AND n.isPinned = true ORDER BY n.createdAt DESC")
  List<LeadNote> findPinnedNotesByLeadId(@Param("leadId") Long leadId);

  /**
   * Counts the number of notes for a specific lead.
   *
   * @param leadId The lead ID
   * @return Count of notes
   */
  long countByLeadId(Long leadId);
}
