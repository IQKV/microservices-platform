package com.iqscaffold.contactservice.contact.dto;

import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.contact.ContactStatus;

/**
 * Mapper utility for converting between Contact entities and DTOs.
 */
public final class ContactMapper {

  private ContactMapper() {
    // Utility class
  }

  /**
   * Converts a Contact entity to a ContactResponse DTO.
   *
   * @param contact The contact entity to convert
   * @return ContactResponse DTO
   */
  public static ContactDtos.ContactResponse toResponse(final Contact contact) {
    if (contact == null) {
      return null;
    }

    return new ContactDtos.ContactResponse(
        contact.getId(),
        contact.getFirstName(),
        contact.getLastName(),
        contact.getEmail(),
        contact.getPhone(),
        contact.getJobTitle(),
        contact.getCompanyId(),
        contact.getStatus(),
        contact.getLeadScore(),
        contact.getNotes(),
        contact.getConvertedFromLeadId(),
        contact.getConvertedAt(),
        contact.getCreatedAt(),
        contact.getUpdatedAt(),
        contact.getCreatedBy(),
        contact.getUpdatedBy()
    );
  }

  /**
   * Converts a CreateContactRequest DTO to a Contact entity.
   *
   * @param request   The create request DTO
   * @param createdBy User ID creating the contact
   * @return Contact entity
   */
  public static Contact toEntity(
      final ContactDtos.CreateContactRequest request,
      final String createdBy) {
    if (request == null) {
      return null;
    }

    Contact contact = new Contact();
    contact.setFirstName(request.firstName());
    contact.setLastName(request.lastName());
    contact.setEmail(request.email());
    contact.setPhone(request.phone());
    contact.setJobTitle(request.jobTitle());
    contact.setCompanyId(request.companyId());
    contact.setStatus(request.status() != null ? request.status() : ContactStatus.ACTIVE);
    contact.setLeadScore(request.leadScore() != null ? request.leadScore() : 0);
    contact.setNotes(request.notes());
    contact.setCreatedBy(createdBy);
    contact.setUpdatedBy(createdBy);

    return contact;
  }

  /**
   * Updates an existing Contact entity with data from UpdateContactRequest DTO.
   *
   * @param contact   The contact entity to update
   * @param request   The update request DTO
   * @param updatedBy User ID updating the contact
   */
  public static void updateEntity(
      final Contact contact,
      final ContactDtos.UpdateContactRequest request,
      final String updatedBy) {
    if (contact == null || request == null) {
      return;
    }

    contact.setFirstName(request.firstName());
    contact.setLastName(request.lastName());
    contact.setEmail(request.email());
    contact.setPhone(request.phone());
    contact.setJobTitle(request.jobTitle());
    contact.setCompanyId(request.companyId());
    if (request.status() != null) {
      contact.setStatus(request.status());
    }
    contact.setNotes(request.notes());
    contact.setUpdatedBy(updatedBy);
  }
}
