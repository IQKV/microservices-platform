package com.iqscaffold.leadservice.lead;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {

  Lead createLead(Lead lead);

  LeadDtos.LeadResponse createLead(LeadDtos.CreateLeadRequest request, String createdBy);

  Optional<Lead> getLeadById(Long id);

  LeadDtos.LeadResponse getLeadResponseById(Long id);

  Optional<Lead> getLeadByEmail(String email);

  Page<Lead> getAllLeads(Pageable pageable);

  Page<Lead> getLeadsByStatus(LeadStatus status, Pageable pageable);

  Page<Lead> getLeadsByAssignedTo(String assignedTo, Pageable pageable);

  Page<Lead> searchLeads(String searchTerm, Pageable pageable);

  Page<Lead> findLeadsWithFilters(
      String searchTerm,
      String source,
      LeadStatus status,
      String assignedTo,
      Pageable pageable
  );

  List<Lead> getLeadsBySource(String source);

  List<Lead> getQualifiedLeads(LeadStatus status);

  Lead updateLead(Long id, Lead lead);

  LeadDtos.LeadResponse updateLead(Long id, LeadDtos.UpdateLeadRequest request, String updatedBy);

  Lead updateLeadStatus(Long id, LeadStatus status);

  Lead updateLeadScore(Long id, Integer score);

  Lead qualifyLead(Long id);

  Lead disqualifyLead(Long id);

  Lead assignLead(Long id, String assignedTo);

  Lead convertLead(Long id, Long contactId);

  void deleteLead(Long id);

  boolean existsByEmail(String email);

  long getLeadCountByStatus(LeadStatus status);

  long getLeadCountBySource(String source);

  long getQualifiedLeadCount();

  long getLeadsCreatedSince(LocalDateTime date);

  /**
   * Gets lead counts grouped by source with optional date filtering.
   *
   * @param startDate Optional start date for filtering (inclusive)
   * @param endDate   Optional end date for filtering (inclusive)
   * @return Map of source to count
   */
  java.util.Map<String, Long> getLeadCountsBySource(LocalDateTime startDate, LocalDateTime endDate);
}
