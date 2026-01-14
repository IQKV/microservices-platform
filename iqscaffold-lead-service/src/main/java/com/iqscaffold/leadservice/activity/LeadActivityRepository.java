package com.iqscaffold.leadservice.activity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for LeadActivity entity.
 */
@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

  /**
   * Find all activities for a specific lead, ordered by creation timestamp descending.
   *
   * @param leadId the lead ID
   * @return list of activities
   */
  List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

  /**
   * Find all activities of a specific type for a lead.
   *
   * @param leadId the lead ID
   * @param type   the activity type
   * @return list of activities
   */
  List<LeadActivity> findByLeadIdAndType(Long leadId, ActivityType type);

  /**
   * Count activities for a specific lead.
   *
   * @param leadId the lead ID
   * @return count of activities
   */
  long countByLeadId(Long leadId);
}
