package com.iqscaffold.leadservice.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.note.LeadNote;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for activity logging operations.
 */
@Service
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

  private static final Logger logger = LoggerFactory.getLogger(ActivityLogServiceImpl.class);

  private final LeadActivityRepository activityRepository;
  private final LeadRepository leadRepository;
  private final ObjectMapper objectMapper;

  public ActivityLogServiceImpl(
      final LeadActivityRepository activityRepository,
      final LeadRepository leadRepository,
      final ObjectMapper objectMapper) {
    this.activityRepository = activityRepository;
    this.leadRepository = leadRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void logLeadCreated(final Lead lead) {
    logger.debug("Logging lead created activity for lead ID: {}", lead.getId());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", lead.getId());
    metadata.put("email", lead.getEmail());
    metadata.put("source", lead.getSource());
    metadata.put("status", lead.getStatus().name());

    String description = String.format(
        "Lead created: %s (%s) from source %s",
        lead.getFullName(),
        lead.getEmail(),
        lead.getSource()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.LEAD_CREATED,
        description,
        lead.getCreatedBy()
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged LEAD_CREATED activity for lead ID: {}", lead.getId());
  }

  @Override
  public void logLeadUpdated(final Lead lead) {
    logger.debug("Logging lead updated activity for lead ID: {}", lead.getId());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", lead.getId());
    metadata.put("email", lead.getEmail());
    metadata.put("status", lead.getStatus().name());

    String description = String.format(
        "Lead updated: %s (%s)",
        lead.getFullName(),
        lead.getEmail()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.LEAD_UPDATED,
        description,
        lead.getUpdatedBy()
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged LEAD_UPDATED activity for lead ID: {}", lead.getId());
  }

  @Override
  public void logLeadDeleted(final Long leadId, final String deletedBy) {
    logger.debug("Logging lead deleted activity for lead ID: {}", leadId);

    // Note: Since the lead is being deleted, we need to fetch it before deletion
    // This method should be called BEFORE the actual deletion
    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with ID: " + leadId));

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", leadId);
    metadata.put("email", lead.getEmail());
    metadata.put("deletedBy", deletedBy);

    String description = String.format(
        "Lead deleted: %s (%s)",
        lead.getFullName(),
        lead.getEmail()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.LEAD_DELETED,
        description,
        deletedBy
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged LEAD_DELETED activity for lead ID: {}", leadId);
  }

  @Override
  public void logNoteAdded(final Lead lead, final LeadNote note) {
    logger.debug("Logging note added activity for lead ID: {}, note ID: {}",
        lead.getId(), note.getId());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", lead.getId());
    metadata.put("noteId", note.getId());
    metadata.put("isPinned", note.getIsPinned());
    metadata.put("contentPreview", truncate(note.getContent(), 100));

    String description = String.format(
        "Note added to lead %s",
        lead.getFullName()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.NOTE_ADDED,
        description,
        note.getCreatedBy()
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged NOTE_ADDED activity for lead ID: {}, note ID: {}",
        lead.getId(), note.getId());
  }

  @Override
  public void logNoteUpdated(final Lead lead, final LeadNote note) {
    logger.debug("Logging note updated activity for lead ID: {}, note ID: {}",
        lead.getId(), note.getId());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", lead.getId());
    metadata.put("noteId", note.getId());
    metadata.put("isPinned", note.getIsPinned());

    String description = String.format(
        "Note updated for lead %s",
        lead.getFullName()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.NOTE_UPDATED,
        description,
        note.getUpdatedBy()
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged NOTE_UPDATED activity for lead ID: {}, note ID: {}",
        lead.getId(), note.getId());
  }

  @Override
  public void logNoteDeleted(final Lead lead, final Long noteId, final String deletedBy) {
    logger.debug("Logging note deleted activity for lead ID: {}, note ID: {}",
        lead.getId(), noteId);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", lead.getId());
    metadata.put("noteId", noteId);
    metadata.put("deletedBy", deletedBy);

    String description = String.format(
        "Note deleted from lead %s",
        lead.getFullName()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.NOTE_DELETED,
        description,
        deletedBy
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged NOTE_DELETED activity for lead ID: {}, note ID: {}",
        lead.getId(), noteId);
  }

  @Override
  public void logStageChange(
      final Long leadId,
      final Long oldStageId,
      final Long newStageId,
      final String changedBy) {
    logger.debug("Logging stage change activity for lead ID: {} from stage {} to stage {}",
        leadId, oldStageId, newStageId);

    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with ID: " + leadId));

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", leadId);
    metadata.put("oldStageId", oldStageId);
    metadata.put("newStageId", newStageId);
    metadata.put("changedBy", changedBy);

    String description = String.format(
        "Lead %s moved from stage %d to stage %d",
        lead.getFullName(),
        oldStageId,
        newStageId
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.STAGE_CHANGED,
        description,
        changedBy
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged STAGE_CHANGED activity for lead ID: {}", leadId);
  }

  @Override
  public void logFollowUpScheduled(
      final Long leadId,
      final Long followUpId,
      final String scheduledBy) {
    logger.debug("Logging follow-up scheduled activity for lead ID: {}, follow-up ID: {}",
        leadId, followUpId);

    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with ID: " + leadId));

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", leadId);
    metadata.put("followUpId", followUpId);
    metadata.put("scheduledBy", scheduledBy);

    String description = String.format(
        "Follow-up scheduled for lead %s",
        lead.getFullName()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.FOLLOWUP_SCHEDULED,
        description,
        scheduledBy
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged FOLLOWUP_SCHEDULED activity for lead ID: {}, follow-up ID: {}",
        leadId, followUpId);
  }

  @Override
  public void logFollowUpCompleted(
      final Long leadId,
      final Long followUpId,
      final String completedBy) {
    logger.debug("Logging follow-up completed activity for lead ID: {}, follow-up ID: {}",
        leadId, followUpId);

    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with ID: " + leadId));

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("leadId", leadId);
    metadata.put("followUpId", followUpId);
    metadata.put("completedBy", completedBy);

    String description = String.format(
        "Follow-up completed for lead %s",
        lead.getFullName()
    );

    LeadActivity activity = new LeadActivity(
        lead,
        ActivityType.FOLLOWUP_COMPLETED,
        description,
        completedBy
    );
    activity.setMetadata(toJson(metadata));

    activityRepository.save(activity);
    logger.info("Logged FOLLOWUP_COMPLETED activity for lead ID: {}, follow-up ID: {}",
        leadId, followUpId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LeadActivity> getLeadActivityTimeline(final Long leadId) {
    logger.debug("Retrieving activity timeline for lead ID: {}", leadId);

    // Verify lead exists
    if (!leadRepository.existsById(leadId)) {
      throw new LeadNotFoundException("Lead not found with ID: " + leadId);
    }

    List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
    logger.debug("Found {} activities for lead ID: {}", activities.size(), leadId);

    return activities;
  }

  /**
   * Convert a map to JSON string.
   *
   * @param data the data to convert
   * @return JSON string or null if conversion fails
   */
  private String toJson(final Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      logger.error("Failed to convert metadata to JSON", e);
      return null;
    }
  }

  /**
   * Truncate a string to a maximum length.
   *
   * @param text      the text to truncate
   * @param maxLength the maximum length
   * @return truncated text
   */
  private String truncate(final String text, final int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
