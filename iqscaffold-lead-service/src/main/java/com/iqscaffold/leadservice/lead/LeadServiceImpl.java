package com.iqscaffold.leadservice.lead;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.infrastructure.client.ContactServiceClient;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.lead.dto.LeadMapper;
import com.iqscaffold.leadservice.shared.exception.DuplicateResourceException;
import com.iqscaffold.leadservice.shared.exception.LeadConversionException;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeadServiceImpl implements LeadService {

  private static final Logger log = LoggerFactory.getLogger(LeadServiceImpl.class);

  private final LeadRepository leadRepository;
  private final ContactServiceClient contactServiceClient;

  public LeadServiceImpl(
      final LeadRepository leadRepository,
      final ContactServiceClient contactServiceClient) {
    this.leadRepository = leadRepository;
    this.contactServiceClient = contactServiceClient;
  }

  @Override
  public Lead createLead(final Lead lead) {
    return leadRepository.save(lead);
  }

  @Override
  public LeadDtos.LeadResponse createLead(
      final LeadDtos.CreateLeadRequest request,
      final String createdBy) {
    // Check for duplicate email
    if (leadRepository.existsByEmail(request.email())) {
      throw new DuplicateResourceException(
          "Lead with email " + request.email() + " already exists");
    }

    Lead lead = LeadMapper.toEntity(request, createdBy);
    Lead savedLead = leadRepository.save(lead);
    return LeadMapper.toResponse(savedLead);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Lead> getLeadById(final Long id) {
    return leadRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public LeadDtos.LeadResponse getLeadResponseById(final Long id) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));
    return LeadMapper.toResponse(lead);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Lead> getLeadByEmail(final String email) {
    return leadRepository.findByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Lead> getAllLeads(final Pageable pageable) {
    return leadRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Lead> getLeadsByStatus(final LeadStatus status, final Pageable pageable) {
    return leadRepository.findByStatus(status, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Lead> getLeadsByAssignedTo(final String assignedTo, final Pageable pageable) {
    return leadRepository.findByAssignedTo(assignedTo, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Lead> searchLeads(final String searchTerm, final Pageable pageable) {
    return leadRepository.searchLeads(searchTerm, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Lead> findLeadsWithFilters(
      final String searchTerm,
      final String source,
      final LeadStatus status,
      final String assignedTo,
      final Pageable pageable) {
    return leadRepository.findLeadsWithFilters(searchTerm, source, status, assignedTo, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Lead> getLeadsBySource(final String source) {
    return leadRepository.findBySource(source);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Lead> getQualifiedLeads(final LeadStatus status) {
    return leadRepository.findQualifiedLeadsByStatus(status);
  }

  @Override
  public Lead updateLead(final Long id, final Lead lead) {
    Lead existingLead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    existingLead.setFirstName(lead.getFirstName());
    existingLead.setLastName(lead.getLastName());
    existingLead.setEmail(lead.getEmail());
    existingLead.setPhone(lead.getPhone());
    existingLead.setCompany(lead.getCompany());
    existingLead.setJobTitle(lead.getJobTitle());
    existingLead.setSource(lead.getSource());
    existingLead.setNotes(lead.getNotes());
    existingLead.setUpdatedBy(lead.getUpdatedBy());

    return leadRepository.save(existingLead);
  }

  @Override
  public LeadDtos.LeadResponse updateLead(
      final Long id,
      final LeadDtos.UpdateLeadRequest request,
      final String updatedBy) {
    Lead existingLead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    // Check for duplicate email if email is being changed
    if (!existingLead.getEmail().equals(request.email())
        && leadRepository.existsByEmail(request.email())) {
      throw new DuplicateResourceException(
          "Lead with email " + request.email() + " already exists");
    }

    LeadMapper.updateEntity(existingLead, request, updatedBy);
    Lead savedLead = leadRepository.save(existingLead);
    return LeadMapper.toResponse(savedLead);
  }

  @Override
  public Lead updateLeadStatus(final Long id, final LeadStatus status) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setStatus(status);
    return leadRepository.save(lead);
  }

  @Override
  public Lead updateLeadScore(final Long id, final Integer score) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setScore(score);
    return leadRepository.save(lead);
  }

  @Override
  public Lead qualifyLead(final Long id) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setQualified(true);
    lead.setStatus(LeadStatus.QUALIFIED);
    return leadRepository.save(lead);
  }

  @Override
  public Lead disqualifyLead(final Long id) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setQualified(false);
    lead.setStatus(LeadStatus.UNQUALIFIED);
    return leadRepository.save(lead);
  }

  @Override
  public Lead assignLead(final Long id, final String assignedTo) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setAssignedTo(assignedTo);
    return leadRepository.save(lead);
  }

  @Override
  public Lead convertLead(final Long id, final Long contactId) {
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedAt(LocalDateTime.now());
    lead.setConvertedToContactId(contactId);
    return leadRepository.save(lead);
  }

  @Override
  public LeadDtos.ConvertLeadResponse convertLeadToContact(
      final Long id,
      final LeadDtos.ConvertLeadRequest request,
      final String bearerToken) {
    log.info("Converting lead {} to contact", id);

    // Get the lead
    Lead lead = leadRepository.findById(id)
        .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

    // Validate lead can be converted
    if (lead.getStatus() == LeadStatus.CONVERTED) {
      throw new IllegalStateException(
          "Lead " + id + " has already been converted to contact " + lead.getConvertedToContactId());
    }

    // Store original lead state for rollback
    LeadStatus originalStatus = lead.getStatus();
    LocalDateTime originalConvertedAt = lead.getConvertedAt();
    Long originalContactId = lead.getConvertedToContactId();

    // Prepare contact creation request
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

    // Create contact in Contact Service
    ContactServiceClient.ContactResponse contactResponse = null;
    boolean contactCreated = false;

    try {
      contactResponse = contactServiceClient.createContact(contactRequest, bearerToken);
      contactCreated = true;
      log.info("Successfully created contact {} from lead {}", contactResponse.id(), id);

      // Update lead status
      lead.setStatus(LeadStatus.CONVERTED);
      lead.setConvertedAt(LocalDateTime.now());
      lead.setConvertedToContactId(contactResponse.id());
      leadRepository.save(lead);

      log.info("Lead {} successfully converted to contact {}", id, contactResponse.id());

      return new LeadDtos.ConvertLeadResponse(
          id,
          contactResponse.id(),
          lead.getConvertedAt(),
          "Lead successfully converted to contact"
      );

    } catch (final Exception e) {
      log.error("Error during lead conversion for lead {}", id, e);

      // Rollback: Delete the created contact if it was created
      boolean rollbackSuccessful = false;
      if (contactCreated && contactResponse != null) {
        log.warn("Rolling back conversion: deleting contact {}", contactResponse.id());
        try {
          contactServiceClient.deleteContact(contactResponse.id(), bearerToken);
          rollbackSuccessful = true;
          log.info("Successfully rolled back contact creation for contact {}", contactResponse.id());
        } catch (final Exception rollbackException) {
          log.error("Failed to rollback contact creation for contact {}. Manual cleanup may be required.",
              contactResponse.id(), rollbackException);
          // Continue to restore lead state even if contact deletion fails
        }
      } else {
        // No contact was created, so rollback is considered successful
        rollbackSuccessful = true;
      }

      // Restore lead to original state
      lead.setStatus(originalStatus);
      lead.setConvertedAt(originalConvertedAt);
      lead.setConvertedToContactId(originalContactId);
      leadRepository.save(lead);
      log.info("Lead {} restored to original state after failed conversion", id);

      // Throw custom exception with rollback status
      String errorMessage = rollbackSuccessful
          ? "Failed to convert lead to contact. Rollback successful - lead restored to original state."
          : "Failed to convert lead to contact. Rollback partially failed - manual cleanup may be required for contact "
              + (contactResponse != null ? contactResponse.id() : "unknown");

      throw new LeadConversionException(
          errorMessage,
          e,
          id,
          contactResponse != null ? contactResponse.id() : null,
          rollbackSuccessful
      );
    }
  }

  @Override
  public void deleteLead(final Long id) {
    if (!leadRepository.existsById(id)) {
      throw new LeadNotFoundException("Lead not found with id: " + id);
    }
    leadRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByEmail(final String email) {
    return leadRepository.existsByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public long getLeadCountByStatus(final LeadStatus status) {
    return leadRepository.countByStatus(status);
  }

  @Override
  @Transactional(readOnly = true)
  public long getLeadCountBySource(final String source) {
    return leadRepository.countBySource(source);
  }

  @Override
  @Transactional(readOnly = true)
  public long getQualifiedLeadCount() {
    return leadRepository.countByQualified(true);
  }

  @Override
  @Transactional(readOnly = true)
  public long getLeadsCreatedSince(final LocalDateTime date) {
    return leadRepository.countLeadsCreatedSince(date);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Map<String, Long> getLeadCountsBySource(
      final LocalDateTime startDate,
      final LocalDateTime endDate) {
    List<LeadRepository.LeadSourceCount> counts =
        leadRepository.countLeadsBySource(startDate, endDate);

    return counts.stream()
        .collect(java.util.stream.Collectors.toMap(
            LeadRepository.LeadSourceCount::getSource,
            LeadRepository.LeadSourceCount::getCount
        ));
  }
}
