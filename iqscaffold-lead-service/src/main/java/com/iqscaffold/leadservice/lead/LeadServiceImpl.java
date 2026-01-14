package com.iqscaffold.leadservice.lead;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeadServiceImpl implements LeadService {

  private final LeadRepository leadRepository;

  public LeadServiceImpl(final LeadRepository leadRepository) {
    this.leadRepository = leadRepository;
  }

  @Override
  public Lead createLead(final Lead lead) {
    return leadRepository.save(lead);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Lead> getLeadById(final Long id) {
    return leadRepository.findById(id);
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
}
