package com.iqscaffold.pipelineservice.followup;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowUpService {

  FollowUp scheduleFollowUp(FollowUp followUp);

  Optional<FollowUp> getFollowUpById(Long id);

  Page<FollowUp> getAllFollowUps(Pageable pageable);

  Page<FollowUp> getFollowUpsByLeadId(Long leadId, Pageable pageable);

  List<FollowUp> getFollowUpsByLeadId(Long leadId);

  Page<FollowUp> getFollowUpsByStatus(FollowUpStatus status, Pageable pageable);

  List<FollowUp> getTodayFollowUps();

  List<FollowUp> getOverdueFollowUps();

  FollowUp updateFollowUp(Long id, FollowUp followUp);

  FollowUp completeFollowUp(Long id);

  void deleteFollowUp(Long id);

  long countTodayFollowUps();
}
