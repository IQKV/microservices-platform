package com.iqscaffold.leadservice.activity;

import java.util.List;

import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.note.LeadNote;

/**
 * Service interface for activity logging operations.
 */
public interface ActivityLogService {

  /**
   * Log lead creation activity.
   *
   * @param lead the created lead
   */
  void logLeadCreated(Lead lead);

  /**
   * Log lead update activity.
   *
   * @param lead the updated lead
   */
  void logLeadUpdated(Lead lead);

  /**
   * Log lead deletion activity.
   *
   * @param leadId    the ID of the deleted lead
   * @param deletedBy the user who deleted the lead
   */
  void logLeadDeleted(Long leadId, String deletedBy);

  /**
   * Log note added activity.
   *
   * @param lead the lead to which the note was added
   * @param note the added note
   */
  void logNoteAdded(Lead lead, LeadNote note);

  /**
   * Log note updated activity.
   *
   * @param lead the lead to which the note belongs
   * @param note the updated note
   */
  void logNoteUpdated(Lead lead, LeadNote note);

  /**
   * Log note deleted activity.
   *
   * @param lead      the lead from which the note was deleted
   * @param noteId    the ID of the deleted note
   * @param deletedBy the user who deleted the note
   */
  void logNoteDeleted(Lead lead, Long noteId, String deletedBy);

  /**
   * Log stage change activity (event listener).
   *
   * @param leadId     the lead ID
   * @param oldStageId the old stage ID
   * @param newStageId the new stage ID
   * @param changedBy  the user who changed the stage
   */
  void logStageChange(Long leadId, Long oldStageId, Long newStageId, String changedBy);

  /**
   * Log follow-up scheduled activity.
   *
   * @param leadId      the lead ID
   * @param followUpId  the follow-up ID
   * @param scheduledBy the user who scheduled the follow-up
   */
  void logFollowUpScheduled(Long leadId, Long followUpId, String scheduledBy);

  /**
   * Log follow-up completed activity.
   *
   * @param leadId      the lead ID
   * @param followUpId  the follow-up ID
   * @param completedBy the user who completed the follow-up
   */
  void logFollowUpCompleted(Long leadId, Long followUpId, String completedBy);

  /**
   * Log lead converted to contact activity.
   *
   * @param leadId      the lead ID
   * @param description the conversion description
   */
  void logLeadConverted(Long leadId, String description);

  /**
   * Get activity timeline for a lead.
   *
   * @param leadId the lead ID
   * @return list of activities sorted by timestamp descending
   */
  List<LeadActivity> getLeadActivityTimeline(Long leadId);
}
