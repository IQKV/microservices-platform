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

  /**
   * Converts a lead to a contact by creating a contact in the Contact Service.
   *
   * @param id          The lead ID to convert
   * @param request     The conversion request with optional company and notes
   * @param bearerToken JWT bearer token for authentication
   * @return Conversion response with lead and contact IDs
   */
  LeadDtos.ConvertLeadResponse convertLeadToContact(
      Long id,
      LeadDtos.ConvertLeadRequest request,
      String bearerToken
  );

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

  // Entity Graph Optimized Methods

  /**
   * Gets a lead with notes for detail view.
   * Uses entity graph to optimize note loading.
   *
   * @param id The lead ID
   * @return Lead with notes loaded
   */
  Optional<Lead> getLeadWithNotes(Long id);

  /**
   * Gets a lead with activities for timeline view.
   * Uses entity graph to optimize activity loading.
   *
   * @param id The lead ID
   * @return Lead with activities loaded
   */
  Optional<Lead> getLeadWithActivities(Long id);

  /**
   * Gets a lead with complete history (notes and activities).
   * Uses entity graph to optimize loading of all related data.
   *
   * @param id The lead ID
   * @return Lead with all related data loaded
   */
  Optional<Lead> getLeadWithCompleteHistory(Long id);

  /**
   * Gets leads by assigned user with basic data only.
   * Optimized for listing views without heavy collections.
   *
   * @param assignedTo The assigned user
   * @param pageable   Pagination parameters
   * @return Page of leads with basic data
   */
  Page<Lead> getBasicLeadsByAssignedTo(String assignedTo, Pageable pageable);

  /**
   * Advanced search with notes loaded for detailed results.
   * Use when search results need note context.
   *
   * @param searchTerm Search term
   * @param source     Lead source filter
   * @param status     Lead status filter
   * @param assignedTo Assigned user filter
   * @param pageable   Pagination parameters
   * @return Page of leads with notes loaded
   */
  Page<Lead> searchLeadsWithNotes(
      String searchTerm,
      String source,
      LeadStatus status,
      String assignedTo,
      Pageable pageable
  );
}
