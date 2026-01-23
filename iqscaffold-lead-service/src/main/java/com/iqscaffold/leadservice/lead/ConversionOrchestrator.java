package com.iqscaffold.leadservice.lead;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iqscaffold.leadservice.event.LeadEventPublisher;
import com.iqscaffold.leadservice.infrastructure.client.ContactServiceClient;
import com.iqscaffold.leadservice.infrastructure.client.PipelineServiceClient;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.shared.exception.LeadConversionException;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the lead conversion process across multiple services.
 * <p>
 * Handles the complex workflow of converting a lead to a contact and creating
 * a corresponding pipeline item, with proper rollback capabilities in case of failures.
 * <p>
 * The conversion process follows these steps:
 * <ol>
 *   <li>Validate lead can be converted</li>
 *   <li>Create contact in Contact Service</li>
 *   <li>Create pipeline item in Pipeline Service</li>
 *   <li>Update lead status to CONVERTED</li>
 *   <li>Publish lead.converted event</li>
 * </ol>
 * <p>
 * If any step fails, the orchestrator performs compensation actions to maintain data consistency.
 */
@Component
@Transactional
public class ConversionOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(ConversionOrchestrator.class);

  private final LeadRepository leadRepository;
  private final ContactServiceClient contactServiceClient;
  private final PipelineServiceClient pipelineServiceClient;
  private final LeadEventPublisher leadEventPublisher;

  public ConversionOrchestrator(
      final LeadRepository leadRepository,
      final ContactServiceClient contactServiceClient,
      final PipelineServiceClient pipelineServiceClient,
      final LeadEventPublisher leadEventPublisher) {
    this.leadRepository = leadRepository;
    this.contactServiceClient = contactServiceClient;
    this.pipelineServiceClient = pipelineServiceClient;
    this.leadEventPublisher = leadEventPublisher;
  }

  /**
   * Converts a lead to a contact and creates a pipeline item.
   * <p>
   * This method implements the saga pattern for distributed transactions,
   * ensuring data consistency across multiple services.
   *
   * @param leadId      The ID of the lead to convert
   * @param request     The conversion request containing additional data
   * @param bearerToken JWT bearer token for authentication
   * @param convertedBy The user performing the conversion
   * @return The conversion result
   * @throws LeadConversionException if the conversion fails
   */
  public LeadDtos.ConvertLeadResponse convertLead(
      final Long leadId,
      final LeadDtos.ConvertLeadRequest request,
      final String bearerToken,
      final String convertedBy) {

    log.info("Starting lead conversion process for lead ID: {}", leadId);

    // Step 1: Validate lead can be converted
    Lead lead = validateLeadForConversion(leadId);

    // Store original lead state for rollback
    ConversionState originalState = captureOriginalState(lead);

    Long contactId = null;
    Long pipelineItemId = null;
    boolean contactCreated = false;
    boolean pipelineItemCreated = false;

    try {
      // Step 2: Create contact in Contact Service
      contactId = createContact(lead, request, bearerToken);
      contactCreated = true;
      log.info("Successfully created contact {} from lead {}", contactId, leadId);

      // Step 3: Create pipeline item in Pipeline Service
      pipelineItemId = createPipelineItem(lead, contactId, convertedBy, bearerToken);
      pipelineItemCreated = true;
      log.info("Successfully created pipeline item {} for lead {}", pipelineItemId, leadId);

      // Step 4: Update lead status to CONVERTED
      updateLeadToConverted(lead, contactId, pipelineItemId, convertedBy);
      log.info("Successfully updated lead {} status to CONVERTED", leadId);

      // Step 5: Publish lead.converted event
      leadEventPublisher.publishLeadConverted(lead, contactId);
      log.info("Lead {} successfully converted to contact {} with pipeline item {}",
          leadId, contactId, pipelineItemId);

      return new LeadDtos.ConvertLeadResponse(
          leadId,
          contactId,
          lead.getConvertedAt(),
          "Lead successfully converted to contact with pipeline item created"
      );

    } catch (final Exception e) {
      log.error("Error during lead conversion for lead {}", leadId, e);

      // Perform compensation actions (rollback)
      boolean rollbackSuccessful = performRollback(
          lead, originalState, contactId, pipelineItemId,
          contactCreated, pipelineItemCreated, bearerToken);

      // Throw custom exception with rollback status
      String errorMessage = buildErrorMessage(rollbackSuccessful, contactId, pipelineItemId);
      throw new LeadConversionException(
          errorMessage,
          e,
          leadId,
          contactId,
          rollbackSuccessful
      );
    }
  }

  /**
   * Validates that a lead can be converted.
   *
   * @param leadId The lead ID to validate
   * @return The lead entity
   * @throws LeadNotFoundException if lead not found
   * @throws IllegalStateException if lead cannot be converted
   */
  private Lead validateLeadForConversion(final Long leadId) {
    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + leadId));

    if (lead.getStatus() == LeadStatus.CONVERTED) {
      throw new IllegalStateException(
          "Lead " + leadId + " has already been converted to contact " + lead.getConvertedToContactId());
    }

    return lead;
  }

  /**
   * Captures the original state of the lead for rollback purposes.
   *
   * @param lead The lead entity
   * @return The original state
   */
  private ConversionState captureOriginalState(final Lead lead) {
    return new ConversionState(
        lead.getStatus(),
        lead.getConvertedAt(),
        lead.getConvertedToContactId(),
        lead.getPipelineItemId()
    );
  }

  /**
   * Creates a contact in the Contact Service.
   *
   * @param lead        The lead to convert
   * @param request     The conversion request
   * @param bearerToken JWT bearer token
   * @return The created contact ID
   */
  private Long createContact(
      final Lead lead,
      final LeadDtos.ConvertLeadRequest request,
      final String bearerToken) {

    String contactNotes = request.notes() != null ? request.notes() : lead.getNotes();
    ContactServiceClient.CreateContactRequest contactRequest =
        new ContactServiceClient.CreateContactRequest(
            lead.getFirstName(),
            lead.getLastName(),
            lead.getEmail(),
            lead.getPhone(),
            lead.getJobTitle(),
            request.companyId(),
            "CUSTOMER", // Set status to CUSTOMER for converted leads
            lead.getScore(),
            contactNotes
        );

    ContactServiceClient.ContactResponse contactResponse =
        contactServiceClient.createContact(contactRequest, bearerToken);

    return contactResponse.id();
  }

  /**
   * Creates a pipeline item in the Pipeline Service.
   *
   * @param lead        The lead being converted
   * @param contactId   The created contact ID
   * @param convertedBy The user performing the conversion
   * @param bearerToken JWT bearer token
   * @return The created pipeline item ID
   */
  private Long createPipelineItem(
      final Lead lead,
      final Long contactId,
      final String convertedBy,
      final String bearerToken) {

    // Create pipeline item with initial stage (will be determined by Pipeline Service)
    // Use lead score to estimate probability and expected value
    BigDecimal probability = calculateProbability(lead.getScore());
    BigDecimal expectedValue = calculateExpectedValue(lead.getScore());

    PipelineServiceClient.CreatePipelineItemRequest pipelineRequest =
        new PipelineServiceClient.CreatePipelineItemRequest(
            lead.getId(),
            null, // Let Pipeline Service determine initial stage
            expectedValue,
            probability,
            convertedBy
        );

    PipelineServiceClient.PipelineItemResponse pipelineResponse =
        pipelineServiceClient.createPipelineItem(pipelineRequest, bearerToken);

    return pipelineResponse.id();
  }

  /**
   * Updates the lead to CONVERTED status.
   *
   * @param lead           The lead to update
   * @param contactId      The created contact ID
   * @param pipelineItemId The created pipeline item ID
   * @param convertedBy    The user performing the conversion
   */
  private void updateLeadToConverted(
      final Lead lead,
      final Long contactId,
      final Long pipelineItemId,
      final String convertedBy) {

    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedAt(LocalDateTime.now());
    lead.setConvertedToContactId(contactId);
    lead.setPipelineItemId(pipelineItemId);
    lead.setUpdatedBy(convertedBy);

    leadRepository.save(lead);
  }

  /**
   * Performs rollback operations to maintain data consistency.
   *
   * @param lead                The lead being converted
   * @param originalState       The original lead state
   * @param contactId           The created contact ID (if any)
   * @param pipelineItemId      The created pipeline item ID (if any)
   * @param contactCreated      Whether contact was created
   * @param pipelineItemCreated Whether pipeline item was created
   * @param bearerToken         JWT bearer token
   * @return true if rollback was successful, false otherwise
   */
  private boolean performRollback(
      final Lead lead,
      final ConversionState originalState,
      final Long contactId,
      final Long pipelineItemId,
      final boolean contactCreated,
      final boolean pipelineItemCreated,
      final String bearerToken) {

    boolean rollbackSuccessful = true;

    // Rollback pipeline item creation
    if (pipelineItemCreated && pipelineItemId != null) {
      try {
        pipelineServiceClient.deletePipelineItem(pipelineItemId, bearerToken);
        log.info("Successfully rolled back pipeline item creation for item {}", pipelineItemId);
      } catch (final Exception rollbackException) {
        log.error("Failed to rollback pipeline item creation for item {}. Manual cleanup may be required.",
            pipelineItemId, rollbackException);
        rollbackSuccessful = false;
      }
    }

    // Rollback contact creation
    if (contactCreated && contactId != null) {
      try {
        contactServiceClient.deleteContact(contactId, bearerToken);
        log.info("Successfully rolled back contact creation for contact {}", contactId);
      } catch (final Exception rollbackException) {
        log.error("Failed to rollback contact creation for contact {}. Manual cleanup may be required.",
            contactId, rollbackException);
        rollbackSuccessful = false;
      }
    }

    // Restore lead to original state
    try {
      lead.setStatus(originalState.status());
      lead.setConvertedAt(originalState.convertedAt());
      lead.setConvertedToContactId(originalState.convertedToContactId());
      lead.setPipelineItemId(originalState.pipelineItemId());
      leadRepository.save(lead);
      log.info("Lead {} restored to original state after failed conversion", lead.getId());
    } catch (final Exception restoreException) {
      log.error("Failed to restore lead {} to original state", lead.getId(), restoreException);
      rollbackSuccessful = false;
    }

    return rollbackSuccessful;
  }

  /**
   * Builds an appropriate error message based on rollback status.
   *
   * @param rollbackSuccessful Whether rollback was successful
   * @param contactId          The contact ID (if any)
   * @param pipelineItemId     The pipeline item ID (if any)
   * @return The error message
   */
  private String buildErrorMessage(
      final boolean rollbackSuccessful,
      final Long contactId,
      final Long pipelineItemId) {

    if (rollbackSuccessful) {
      return "Failed to convert lead. Rollback successful - lead restored to original state.";
    } else {
      StringBuilder message = new StringBuilder("Failed to convert lead. Rollback partially failed - manual cleanup may be required");
      if (contactId != null) {
        message.append(" for contact ").append(contactId);
      }
      if (pipelineItemId != null) {
        message.append(" and pipeline item ").append(pipelineItemId);
      }
      return message.toString();
    }
  }

  /**
   * Calculates probability based on lead score.
   *
   * @param score The lead score
   * @return The calculated probability (0-100)
   */
  private BigDecimal calculateProbability(final Integer score) {
    if (score == null || score <= 0) {
      return BigDecimal.valueOf(10); // Default low probability
    }

    // Simple scoring algorithm: score out of 100 becomes probability percentage
    int probability = Math.min(score, 100);
    return BigDecimal.valueOf(probability);
  }

  /**
   * Calculates expected value based on lead score.
   *
   * @param score The lead score
   * @return The calculated expected value
   */
  private BigDecimal calculateExpectedValue(final Integer score) {
    if (score == null || score <= 0) {
      return BigDecimal.valueOf(1000); // Default low value
    }

    // Simple algorithm: higher score means higher expected value
    // Score 0-25: $1,000-$5,000
    // Score 26-50: $5,000-$15,000
    // Score 51-75: $15,000-$50,000
    // Score 76-100: $50,000-$100,000
    if (score <= 25) {
      return BigDecimal.valueOf(1000 + (score * 160)); // $1,000 + $160 per point
    } else if (score <= 50) {
      return BigDecimal.valueOf(5000 + ((score - 25) * 400)); // $5,000 + $400 per point
    } else if (score <= 75) {
      return BigDecimal.valueOf(15000 + ((score - 50) * 1400)); // $15,000 + $1,400 per point
    } else {
      return BigDecimal.valueOf(50000 + ((score - 75) * 2000)); // $50,000 + $2,000 per point
    }
  }

  /**
   * Record to capture the original state of a lead for rollback purposes.
   */
  private record ConversionState(
      LeadStatus status,
      LocalDateTime convertedAt,
      Long convertedToContactId,
      Long pipelineItemId
  ) {
  }
}
