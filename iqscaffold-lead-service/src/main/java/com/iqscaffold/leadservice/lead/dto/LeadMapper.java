package com.iqscaffold.leadservice.lead.dto;

import com.iqscaffold.leadservice.lead.Lead;

/**
 * Mapper utility for converting between Lead entities and DTOs.
 */
public final class LeadMapper {

  private LeadMapper() {
    // Utility class
  }

  /**
   * Converts a Lead entity to a LeadResponse DTO.
   *
   * @param lead The lead entity to convert
   * @return LeadResponse DTO
   */
  public static LeadDtos.LeadResponse toResponse(final Lead lead) {
    if (lead == null) {
      return null;
    }

    return new LeadDtos.LeadResponse(
        lead.getId(),
        lead.getFirstName(),
        lead.getLastName(),
        lead.getEmail(),
        lead.getPhone(),
        lead.getCompany(),
        lead.getJobTitle(),
        lead.getSource(),
        lead.getStatus(),
        lead.getScore(),
        lead.getQualified(),
        lead.getNotes(),
        lead.getConvertedAt(),
        lead.getConvertedToContactId(),
        lead.getCreatedAt(),
        lead.getUpdatedAt(),
        lead.getCreatedBy(),
        lead.getUpdatedBy(),
        lead.getAssignedTo()
    );
  }

  /**
   * Converts a CreateLeadRequest DTO to a Lead entity.
   *
   * @param request   The create request DTO
   * @param createdBy User ID creating the lead
   * @return Lead entity
   */
  public static Lead toEntity(final LeadDtos.CreateLeadRequest request, final String createdBy) {
    if (request == null) {
      return null;
    }

    Lead lead = new Lead();
    lead.setFirstName(request.firstName());
    lead.setLastName(request.lastName());
    lead.setEmail(request.email());
    lead.setPhone(request.phone());
    lead.setCompany(request.company());
    lead.setJobTitle(request.jobTitle());
    lead.setSource(request.source());
    lead.setNotes(request.notes());
    lead.setAssignedTo(request.assignedTo());
    lead.setCreatedBy(createdBy);
    lead.setUpdatedBy(createdBy);

    return lead;
  }

  /**
   * Updates an existing Lead entity with data from UpdateLeadRequest DTO.
   *
   * @param lead      The lead entity to update
   * @param request   The update request DTO
   * @param updatedBy User ID updating the lead
   */
  public static void updateEntity(
      final Lead lead,
      final LeadDtos.UpdateLeadRequest request,
      final String updatedBy) {
    if (lead == null || request == null) {
      return;
    }

    lead.setFirstName(request.firstName());
    lead.setLastName(request.lastName());
    lead.setEmail(request.email());
    lead.setPhone(request.phone());
    lead.setCompany(request.company());
    lead.setJobTitle(request.jobTitle());
    lead.setSource(request.source());
    lead.setNotes(request.notes());
    lead.setAssignedTo(request.assignedTo());
    lead.setUpdatedBy(updatedBy);
  }
}
