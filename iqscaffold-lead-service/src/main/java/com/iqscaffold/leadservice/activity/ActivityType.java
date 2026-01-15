package com.iqscaffold.leadservice.activity;

/**
 * Enumeration of activity types for lead activity logging.
 */
public enum ActivityType {
  LEAD_CREATED,
  LEAD_UPDATED,
  LEAD_DELETED,
  LEAD_CONVERTED,
  NOTE_ADDED,
  NOTE_UPDATED,
  NOTE_DELETED,
  STAGE_CHANGED,
  FOLLOWUP_SCHEDULED,
  FOLLOWUP_COMPLETED
}
