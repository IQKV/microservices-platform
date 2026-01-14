package com.iqscaffold.pipelineservice.followup.dto;

import com.iqscaffold.pipelineservice.followup.FollowUp;
import com.iqscaffold.pipelineservice.followup.FollowUpPriority;

/**
 * Mapper for converting between FollowUp entities and DTOs.
 */
public final class FollowUpMapper {

  private FollowUpMapper() {
    // Utility class
  }

  /**
   * Converts a FollowUp entity to a response DTO.
   *
   * @param followUp The follow-up entity
   * @return The response DTO
   */
  public static FollowUpDtos.FollowUpResponse toResponse(final FollowUp followUp) {
    if (followUp == null) {
      return null;
    }

    return new FollowUpDtos.FollowUpResponse(
        followUp.getId(),
        followUp.getLeadId(),
        followUp.getTitle(),
        followUp.getDescription(),
        followUp.getDueDate(),
        followUp.getPriority(),
        followUp.getStatus(),
        followUp.getCompletedAt(),
        followUp.getAssignedTo(),
        followUp.isOverdue(),
        followUp.getCreatedAt(),
        followUp.getUpdatedAt(),
        followUp.getCreatedBy(),
        followUp.getUpdatedBy()
    );
  }

  /**
   * Converts a create request DTO to a FollowUp entity.
   *
   * @param request The create request
   * @param userId The user creating the follow-up
   * @return The follow-up entity
   */
  public static FollowUp toEntity(final FollowUpDtos.CreateFollowUpRequest request, final String userId) {
    if (request == null) {
      return null;
    }

    FollowUp followUp = new FollowUp();
    followUp.setLeadId(request.leadId());
    followUp.setTitle(request.title());
    followUp.setDescription(request.description());
    followUp.setDueDate(request.dueDate());
    followUp.setPriority(request.priority() != null ? request.priority() : FollowUpPriority.MEDIUM);
    followUp.setAssignedTo(request.assignedTo());
    followUp.setCreatedBy(userId);
    followUp.setUpdatedBy(userId);

    return followUp;
  }

  /**
   * Updates a FollowUp entity from an update request DTO.
   *
   * @param followUp The follow-up entity to update
   * @param request The update request
   * @param userId The user updating the follow-up
   */
  public static void updateFromRequest(final FollowUp followUp, 
                                       final FollowUpDtos.UpdateFollowUpRequest request,
                                       final String userId) {
    if (followUp == null || request == null) {
      return;
    }

    if (request.title() != null) {
      followUp.setTitle(request.title());
    }
    if (request.description() != null) {
      followUp.setDescription(request.description());
    }
    if (request.dueDate() != null) {
      followUp.setDueDate(request.dueDate());
    }
    if (request.priority() != null) {
      followUp.setPriority(request.priority());
    }
    if (request.assignedTo() != null) {
      followUp.setAssignedTo(request.assignedTo());
    }
    followUp.setUpdatedBy(userId);
  }
}
