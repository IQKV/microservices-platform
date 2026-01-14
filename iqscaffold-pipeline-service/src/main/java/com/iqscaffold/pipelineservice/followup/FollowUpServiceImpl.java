package com.iqscaffold.pipelineservice.followup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the FollowUpService interface.
 * <p>
 * Provides business logic for follow-up management including:
 * <ul>
 *   <li>Scheduling and updating follow-ups</li>
 *   <li>Retrieving follow-ups by various criteria</li>
 *   <li>Marking follow-ups as completed</li>
 *   <li>Filtering today's and overdue follow-ups</li>
 * </ul>
 */
@Service
@Transactional
public class FollowUpServiceImpl implements FollowUpService {

  private final FollowUpRepository followUpRepository;

  public FollowUpServiceImpl(final FollowUpRepository followUpRepository) {
    this.followUpRepository = followUpRepository;
  }

  @Override
  public FollowUp scheduleFollowUp(final FollowUp followUp) {
    return followUpRepository.save(followUp);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FollowUp> getFollowUpById(final Long id) {
    return followUpRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<FollowUp> getAllFollowUps(final Pageable pageable) {
    return followUpRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<FollowUp> getFollowUpsByLeadId(final Long leadId, final Pageable pageable) {
    return followUpRepository.findByLeadId(leadId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FollowUp> getFollowUpsByLeadId(final Long leadId) {
    return followUpRepository.findByLeadId(leadId);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<FollowUp> getFollowUpsByStatus(final FollowUpStatus status, final Pageable pageable) {
    return followUpRepository.findByStatus(status, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FollowUp> getTodayFollowUps() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

    return followUpRepository.findByStatusAndDueDateBetween(
        FollowUpStatus.PENDING,
        startOfDay,
        endOfDay
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<FollowUp> getOverdueFollowUps() {
    return followUpRepository.findOverdueFollowUps(
        FollowUpStatus.PENDING,
        LocalDateTime.now()
    );
  }

  @Override
  public FollowUp updateFollowUp(final Long id, final FollowUp followUp) {
    FollowUp existing = followUpRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Follow-up", "id", id));

    // Update fields
    existing.setTitle(followUp.getTitle());
    existing.setDescription(followUp.getDescription());
    existing.setDueDate(followUp.getDueDate());
    existing.setPriority(followUp.getPriority());
    existing.setAssignedTo(followUp.getAssignedTo());
    existing.setUpdatedBy(followUp.getUpdatedBy());

    return followUpRepository.save(existing);
  }

  @Override
  public FollowUp completeFollowUp(final Long id) {
    FollowUp followUp = followUpRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Follow-up", "id", id));

    followUp.setStatus(FollowUpStatus.COMPLETED);
    followUp.setCompletedAt(LocalDateTime.now());

    return followUpRepository.save(followUp);
  }

  @Override
  public void deleteFollowUp(final Long id) {
    if (!followUpRepository.existsById(id)) {
      throw new ResourceNotFoundException("Follow-up", "id", id);
    }
    followUpRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public long countTodayFollowUps() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

    return followUpRepository.countByStatusAndDueDateBetween(
        FollowUpStatus.PENDING,
        startOfDay,
        endOfDay
    );
  }
}
