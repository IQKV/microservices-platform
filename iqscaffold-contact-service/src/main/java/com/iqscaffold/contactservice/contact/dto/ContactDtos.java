package com.iqscaffold.contactservice.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

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
   * @param firstName  Contact's first name (required, max 100 chars)
   * @param lastName   Contact's last name (required, max 100 chars)
   * @param email      Contact's email address (optional, valid email format, max 255 chars)
   * @param phone      Contact's phone number (optional, max 20 chars)
   * @param jobTitle   Contact's job title (optional, max 100 chars)
   * @param companyId  Company ID (optional)
   * @param status     Contact status (optional, defaults to ACTIVE)
   * @param leadScore  Lead score 0-100 (optional, defaults to 0)
   * @param notes      Additional notes (optional, max 1000 chars)
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
   * @param firstName  Contact's first name (required, max 100 chars)
   * @param lastName   Contact's last name (required, max 100 chars)
   * @param email      Contact's email address (optional, valid email format, max 255 chars)
   * @param phone      Contact's phone number (optional, max 20 chars)
   * @param jobTitle   Contact's job title (optional, max 100 chars)
   * @param companyId  Company ID (optional)
   * @param status     Contact status (optional)
   * @param notes      Additional notes (optional, max 1000 chars)
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
      @NotBlank(message = "Lead score is required")
      @Min(value = 0, message = "Lead score must be at least 0")
      @Max(value = 100, message = "Lead score must not exceed 100")
      Integer score
  ) {
  }

  /**
   * Response DTO for contact information.
   *
   * @param id                   Contact's unique identifier
   * @param firstName            Contact's first name
   * @param lastName             Contact's last name
   * @param email                Contact's email address
   * @param phone                Contact's phone number
   * @param jobTitle             Contact's job title
   * @param companyId            Company ID
   * @param status               Contact status
   * @param leadScore            Lead score (0-100)
   * @param notes                Additional notes
   * @param convertedFromLeadId  Lead ID if converted from lead
   * @param convertedAt          Timestamp when converted from lead
   * @param createdAt            Creation timestamp
   * @param updatedAt            Last update timestamp
   * @param createdBy            User who created the contact
   * @param updatedBy            User who last updated the contact
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
}
