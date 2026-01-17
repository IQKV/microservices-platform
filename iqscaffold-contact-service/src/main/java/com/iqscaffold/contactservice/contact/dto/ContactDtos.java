package com.iqscaffold.contactservice.contact.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iqscaffold.contactservice.contact.ContactStatus;

/**
 * DTOs for Contact REST API operations.
 * Contains request and response records for contact management.
 */
public final class ContactDtos {

  private ContactDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a new contact.
   *
   * @param firstName Contact's first name (required, max 100 chars)
   * @param lastName  Contact's last name (required, max 100 chars)
   * @param email     Contact's email address (optional, valid email format, max 255 chars)
   * @param phone     Contact's phone number (optional, max 20 chars)
   * @param jobTitle  Contact's job title (optional, max 100 chars)
   * @param companyId Company ID (optional)
   * @param status    Contact status (optional, defaults to ACTIVE)
   * @param leadScore Lead score 0-100 (optional, defaults to 0)
   * @param notes     Additional notes (optional, max 1000 chars)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateContactRequest(
      @NotBlank(message = "First name is required")
      @Size(max = 100, message = "First name must not exceed 100 characters")
      String firstName,

      @NotBlank(message = "Last name is required")
      @Size(max = 100, message = "Last name must not exceed 100 characters")
      String lastName,

      @Email(message = "Email must be a valid email address")
      @Size(max = 255, message = "Email must not exceed 255 characters")
      String email,

      @Size(max = 20, message = "Phone must not exceed 20 characters")
      String phone,

      @Size(max = 100, message = "Job title must not exceed 100 characters")
      String jobTitle,

      Long companyId,

      ContactStatus status,

      @Min(value = 0, message = "Lead score must be at least 0")
      @Max(value = 100, message = "Lead score must not exceed 100")
      Integer leadScore,

      @Size(max = 1000, message = "Notes must not exceed 1000 characters")
      String notes
  ) {
  }

  /**
   * Request DTO for updating an existing contact.
   *
   * @param firstName Contact's first name (required, max 100 chars)
   * @param lastName  Contact's last name (required, max 100 chars)
   * @param email     Contact's email address (optional, valid email format, max 255 chars)
   * @param phone     Contact's phone number (optional, max 20 chars)
   * @param jobTitle  Contact's job title (optional, max 100 chars)
   * @param companyId Company ID (optional)
   * @param status    Contact status (optional)
   * @param notes     Additional notes (optional, max 1000 chars)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateContactRequest(
      @NotBlank(message = "First name is required")
      @Size(max = 100, message = "First name must not exceed 100 characters")
      String firstName,

      @NotBlank(message = "Last name is required")
      @Size(max = 100, message = "Last name must not exceed 100 characters")
      String lastName,

      @Email(message = "Email must be a valid email address")
      @Size(max = 255, message = "Email must not exceed 255 characters")
      String email,

      @Size(max = 20, message = "Phone must not exceed 20 characters")
      String phone,

      @Size(max = 100, message = "Job title must not exceed 100 characters")
      String jobTitle,

      Long companyId,

      ContactStatus status,

      @Size(max = 1000, message = "Notes must not exceed 1000 characters")
      String notes
  ) {
  }

  /**
   * Request DTO for updating contact lead score.
   *
   * @param score Lead score 0-100 (required)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateLeadScoreRequest(
      @NotNull(message = "Lead score is required")
      @Min(value = 0, message = "Lead score must be at least 0")
      @Max(value = 100, message = "Lead score must not exceed 100")
      Integer score
  ) {
  }

  /**
   * Response DTO for contact information.
   *
   * @param id                  Contact's unique identifier
   * @param firstName           Contact's first name
   * @param lastName            Contact's last name
   * @param email               Contact's email address
   * @param phone               Contact's phone number
   * @param jobTitle            Contact's job title
   * @param companyId           Company ID
   * @param status              Contact status
   * @param leadScore           Lead score (0-100)
   * @param notes               Additional notes
   * @param convertedFromLeadId Lead ID if converted from lead
   * @param convertedAt         Timestamp when converted from lead
   * @param createdAt           Creation timestamp
   * @param updatedAt           Last update timestamp
   * @param createdBy           User who created the contact
   * @param updatedBy           User who last updated the contact
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ContactResponse(
      Long id,
      String firstName,
      String lastName,
      String email,
      String phone,
      String jobTitle,
      Long companyId,
      ContactStatus status,
      Integer leadScore,
      String notes,
      Long convertedFromLeadId,
      LocalDateTime convertedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy
  ) {
  }

  /**
   * Request DTO for bulk creating contacts.
   *
   * @param contacts List of contacts to create (max 100)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BulkCreateContactsRequest(
      @NotEmpty(message = "Contacts list is required")
      @Size(min = 1, max = 100, message = "Contacts list must contain between 1 and 100 items")
      @Valid
      List<CreateContactRequest> contacts
  ) {
  }

  /**
   * Request DTO for bulk updating contact status.
   *
   * @param contactIds List of contact IDs to update (max 100)
   * @param status     New status to apply
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BulkUpdateStatusRequest(
      @NotEmpty(message = "Contact IDs list is required")
      @Size(min = 1, max = 100, message = "Contact IDs list must contain between 1 and 100 items")
      List<Long> contactIds,

      @NotNull(message = "Status is required")
      ContactStatus status
  ) {
  }

  /**
   * Request DTO for bulk deleting contacts.
   *
   * @param contactIds List of contact IDs to delete (max 100)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BulkDeleteContactsRequest(
      @NotEmpty(message = "Contact IDs list is required")
      @Size(min = 1, max = 100, message = "Contact IDs list must contain between 1 and 100 items")
      List<Long> contactIds
  ) {
  }

  /**
   * Request DTO for bulk updating lead scores.
   *
   * @param updates List of contact ID and score pairs (max 100)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BulkUpdateLeadScoresRequest(
      @NotEmpty(message = "Updates list is required")
      @Size(min = 1, max = 100, message = "Updates list must contain between 1 and 100 items")
      @Valid
      List<LeadScoreUpdate> updates
  ) {
    public record LeadScoreUpdate(
        @NotNull(message = "Contact ID is required")
        Long contactId,

        @NotNull(message = "Lead score is required")
        @Min(value = 0, message = "Lead score must be at least 0")
        @Max(value = 100, message = "Lead score must not exceed 100")
        Integer score
    ) {
    }
  }

  /**
   * Response DTO for bulk operations.
   *
   * @param successCount Number of successful operations
   * @param failureCount Number of failed operations
   * @param results      List of individual operation results
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BulkOperationResponse(
      int successCount,
      int failureCount,
      List<BulkOperationResult> results
  ) {
    public record BulkOperationResult(
        Long contactId,
        String email,
        boolean success,
        String message,
        ContactResponse contact
    ) {
    }
  }
}
