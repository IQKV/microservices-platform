package com.iqscaffold.leadservice.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iqscaffold.leadservice.lead.LeadStatus;
import com.iqscaffold.leadservice.shared.validation.ValidLeadSource;
import com.iqscaffold.leadservice.shared.validation.ValidPhone;

/**
 * DTOs for Lead REST API operations.
 * Contains request and response records for lead management.
 */
public final class LeadDtos {

  private LeadDtos() {
    // Utility class
  }

  /**
   * Request DTO for creating a new lead.
   *
   * @param firstName  Lead's first name (required, max 100 chars)
   * @param lastName   Lead's last name (required, max 100 chars)
   * @param email      Lead's email address (required, valid email format, max 255 chars)
   * @param phone      Lead's phone number (optional, max 20 chars, valid phone format)
   * @param company    Lead's company name (optional, max 255 chars)
   * @param jobTitle   Lead's job title (optional, max 100 chars)
   * @param source     Lead source (required, max 100 chars, must be valid source)
   * @param notes      Additional notes (optional, max 2000 chars)
   * @param assignedTo User ID to assign the lead to (optional, max 100 chars)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateLeadRequest(
      @NotBlank(message = "First name is required")
      @Size(max = 100, message = "First name must not exceed 100 characters")
      String firstName,

      @NotBlank(message = "Last name is required")
      @Size(max = 100, message = "Last name must not exceed 100 characters")
      String lastName,

      @NotBlank(message = "Email is required")
      @Email(message = "Email must be a valid email address")
      @Size(max = 255, message = "Email must not exceed 255 characters")
      String email,

      @Size(max = 20, message = "Phone must not exceed 20 characters")
      @ValidPhone
      String phone,

      @Size(max = 255, message = "Company must not exceed 255 characters")
      String company,

      @Size(max = 100, message = "Job title must not exceed 100 characters")
      String jobTitle,

      @NotBlank(message = "Source is required")
      @Size(max = 100, message = "Source must not exceed 100 characters")
      @ValidLeadSource
      String source,

      @Size(max = 2000, message = "Notes must not exceed 2000 characters")
      String notes,

      @Size(max = 100, message = "Assigned to must not exceed 100 characters")
      String assignedTo
  ) {
  }

  /**
   * Request DTO for updating an existing lead.
   *
   * @param firstName  Lead's first name (required, max 100 chars)
   * @param lastName   Lead's last name (required, max 100 chars)
   * @param email      Lead's email address (required, valid email format, max 255 chars)
   * @param phone      Lead's phone number (optional, max 20 chars, valid phone format)
   * @param company    Lead's company name (optional, max 255 chars)
   * @param jobTitle   Lead's job title (optional, max 100 chars)
   * @param source     Lead source (required, max 100 chars, must be valid source)
   * @param notes      Additional notes (optional, max 2000 chars)
   * @param assignedTo User ID to assign the lead to (optional, max 100 chars)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateLeadRequest(
      @NotBlank(message = "First name is required")
      @Size(max = 100, message = "First name must not exceed 100 characters")
      String firstName,

      @NotBlank(message = "Last name is required")
      @Size(max = 100, message = "Last name must not exceed 100 characters")
      String lastName,

      @NotBlank(message = "Email is required")
      @Email(message = "Email must be a valid email address")
      @Size(max = 255, message = "Email must not exceed 255 characters")
      String email,

      @Size(max = 20, message = "Phone must not exceed 20 characters")
      @ValidPhone
      String phone,

      @Size(max = 255, message = "Company must not exceed 255 characters")
      String company,

      @Size(max = 100, message = "Job title must not exceed 100 characters")
      String jobTitle,

      @NotBlank(message = "Source is required")
      @Size(max = 100, message = "Source must not exceed 100 characters")
      @ValidLeadSource
      String source,

      @Size(max = 2000, message = "Notes must not exceed 2000 characters")
      String notes,

      @Size(max = 100, message = "Assigned to must not exceed 100 characters")
      String assignedTo
  ) {
  }

  /**
   * Response DTO for lead information.
   *
   * @param id                   Lead's unique identifier
   * @param firstName            Lead's first name
   * @param lastName             Lead's last name
   * @param email                Lead's email address
   * @param phone                Lead's phone number
   * @param company              Lead's company name
   * @param jobTitle             Lead's job title
   * @param source               Lead source
   * @param status               Lead status
   * @param score                Lead score (0-100)
   * @param qualified            Whether the lead is qualified
   * @param notes                Additional notes
   * @param convertedAt          Timestamp when lead was converted
   * @param convertedToContactId Contact ID if converted
   * @param createdAt            Creation timestamp
   * @param updatedAt            Last update timestamp
   * @param createdBy            User who created the lead
   * @param updatedBy            User who last updated the lead
   * @param assignedTo           User ID the lead is assigned to
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LeadResponse(
      Long id,
      String firstName,
      String lastName,
      String email,
      String phone,
      String company,
      String jobTitle,
      String source,
      LeadStatus status,
      Integer score,
      Boolean qualified,
      String notes,
      LocalDateTime convertedAt,
      Long convertedToContactId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy,
      String assignedTo
  ) {
  }

  /**
   * Request DTO for converting a lead to a contact.
   *
   * @param companyId Optional company ID to associate with the contact
   * @param notes     Optional additional notes for the conversion
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ConvertLeadRequest(
      Long companyId,
      @Size(max = 1000, message = "Notes must not exceed 1000 characters")
      String notes
  ) {
  }

  /**
   * Response DTO for lead conversion.
   *
   * @param leadId      The ID of the converted lead
   * @param contactId   The ID of the created contact
   * @param convertedAt Timestamp when the conversion occurred
   * @param message     Success message
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ConvertLeadResponse(
      Long leadId,
      Long contactId,
      LocalDateTime convertedAt,
      String message
  ) {
  }
}
