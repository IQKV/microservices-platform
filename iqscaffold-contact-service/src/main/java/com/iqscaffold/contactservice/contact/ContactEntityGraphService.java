package com.iqscaffold.contactservice.contact;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service demonstrating optimal usage of entity graphs for Contact operations.
 * 
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * for contact-related operations by eagerly loading specific relationships based
 * on business requirements.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Contact Details</strong> - Load contact with company for display purposes</li>
 *   <li><strong>Bulk Operations</strong> - Use batch queries to avoid N+1 problems</li>
 *   <li><strong>Search Operations</strong> - Load contacts with company context for search results</li>
 *   <li><strong>Lead Management</strong> - Load contacts with company for lead scoring operations</li>
 * </ul>
 *
 * <h3>Performance Benefits</h3>
 * <ul>
 *   <li>Eliminates N+1 query problems in bulk contact operations</li>
 *   <li>Reduces database round trips for contact-company queries</li>
 *   <li>Optimizes memory usage by loading only needed associations</li>
 *   <li>Leverages Hibernate second-level cache effectively</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ContactEntityGraphService {

  private final ContactRepository contactRepository;

  public ContactEntityGraphService(final ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  /**
   * Find contact by ID with company details loaded.
   * Use this when displaying contact details that include company information.
   *
   * @param id the contact ID
   * @return Optional containing contact with company if found
   */
  public Optional<Contact> findContactWithCompany(final Long id) {
    return contactRepository.findByIdWithCompany(id);
  }

  /**
   * Find contacts by company with company details loaded.
   * Use this when displaying all contacts for a specific company.
   *
   * @param companyId the company ID
   * @return List of contacts with company details loaded
   */
  public List<Contact> findContactsByCompanyWithDetails(final Long companyId) {
    return contactRepository.findByCompanyIdWithCompany(companyId);
  }

  /**
   * Find contacts by status with company details loaded.
   * Use this when filtering contacts by status and displaying company information.
   *
   * @param status the contact status
   * @param pageable pagination information
   * @return Page of contacts with company details loaded
   */
  public Page<Contact> findContactsByStatusWithCompany(final ContactStatus status, final Pageable pageable) {
    return contactRepository.findByStatusWithCompany(status, pageable);
  }

  /**
   * Search contacts with company details loaded.
   * Use this when performing contact search and displaying company information.
   *
   * @param searchTerm the search term
   * @param pageable pagination information
   * @return Page of contacts with company details loaded
   */
  public Page<Contact> searchContactsWithCompany(final String searchTerm, final Pageable pageable) {
    return contactRepository.searchContactsWithCompany(searchTerm, pageable);
  }

  /**
   * Find high-value contacts with company details loaded.
   * Use this for lead scoring operations that need company context.
   *
   * @param minScore minimum lead score threshold
   * @return List of high-value contacts with company details loaded
   */
  public List<Contact> findHighValueContactsWithCompany(final Integer minScore) {
    return contactRepository.findByLeadScoreGreaterThanEqualWithCompany(minScore);
  }

  // Bulk Operations with Entity Graphs

  /**
   * Find multiple contacts by IDs in a single optimized query.
   * Use this for bulk operations to avoid N+1 query problems.
   *
   * @param contactIds list of contact IDs
   * @return Map of contact ID to Contact for easy lookup
   */
  public Map<Long, Contact> findContactsBatch(final List<Long> contactIds) {
    return contactRepository.findAllByIdIn(contactIds)
        .stream()
        .collect(Collectors.toMap(Contact::getId, Function.identity()));
  }

  /**
   * Find multiple contacts by IDs with company details loaded.
   * Use this for bulk operations that need company information.
   *
   * @param contactIds list of contact IDs
   * @return Map of contact ID to Contact with company details loaded
   */
  public Map<Long, Contact> findContactsBatchWithCompany(final List<Long> contactIds) {
    return contactRepository.findAllByIdInWithCompany(contactIds)
        .stream()
        .collect(Collectors.toMap(Contact::getId, Function.identity()));
  }

  /**
   * Get contact summary with company information.
   * Optimized method for displaying contact cards or summaries.
   *
   * @param contactId the contact ID
   * @return ContactSummary containing essential contact and company details, or null if not found
   */
  public ContactSummary getContactSummary(final Long contactId) {
    return contactRepository.findByIdWithCompany(contactId)
        .map(contact -> new ContactSummary(
            contact.getId(),
            contact.getFullName(),
            contact.getEmail(),
            contact.getPhone(),
            contact.getJobTitle(),
            contact.getStatus(),
            contact.getLeadScore(),
            contact.getCompany() != null ? contact.getCompany().getName() : null,
            contact.getCompany() != null ? contact.getCompany().getIndustry() : null,
            contact.getCompany() != null ? contact.getCompany().getWebsite() : null
        ))
        .orElse(null);
  }

  /**
   * Get contact summaries for multiple contacts.
   * Optimized batch method for displaying contact lists with company information.
   *
   * @param contactIds list of contact IDs
   * @return List of ContactSummary objects
   */
  public List<ContactSummary> getContactSummaries(final List<Long> contactIds) {
    return contactRepository.findAllByIdInWithCompany(contactIds)
        .stream()
        .map(contact -> new ContactSummary(
            contact.getId(),
            contact.getFullName(),
            contact.getEmail(),
            contact.getPhone(),
            contact.getJobTitle(),
            contact.getStatus(),
            contact.getLeadScore(),
            contact.getCompany() != null ? contact.getCompany().getName() : null,
            contact.getCompany() != null ? contact.getCompany().getIndustry() : null,
            contact.getCompany() != null ? contact.getCompany().getWebsite() : null
        ))
        .collect(Collectors.toList());
  }

  /**
   * Check if contacts exist for a company.
   * Optimized method for company validation.
   *
   * @param companyId the company ID
   * @return true if company has contacts, false otherwise
   */
  public boolean hasContactsForCompany(final Long companyId) {
    return !contactRepository.findByCompanyId(companyId).isEmpty();
  }

  /**
   * Get contact count by company with company details.
   * Use this for company analytics that need contact counts.
   *
   * @param companyId the company ID
   * @return ContactCompanyStats containing contact count and company details
   */
  public ContactCompanyStats getContactStatsForCompany(final Long companyId) {
    var contacts = contactRepository.findByCompanyIdWithCompany(companyId);
    
    if (contacts.isEmpty()) {
      return null;
    }

    var firstContact = contacts.get(0);
    var company = firstContact.getCompany();
    
    var activeCount = contacts.stream()
        .mapToInt(c -> c.getStatus() == ContactStatus.ACTIVE ? 1 : 0)
        .sum();
    
    var averageLeadScore = contacts.stream()
        .mapToInt(c -> c.getLeadScore() != null ? c.getLeadScore() : 0)
        .average()
        .orElse(0.0);

    return new ContactCompanyStats(
        companyId,
        company != null ? company.getName() : null,
        company != null ? company.getIndustry() : null,
        contacts.size(),
        activeCount,
        averageLeadScore
    );
  }

  /**
   * Data transfer object for contact summary information.
   */
  public record ContactSummary(
      Long id,
      String fullName,
      String email,
      String phone,
      String jobTitle,
      ContactStatus status,
      Integer leadScore,
      String companyName,
      String companyIndustry,
      String companyWebsite
  ) {}

  /**
   * Data transfer object for contact statistics by company.
   */
  public record ContactCompanyStats(
      Long companyId,
      String companyName,
      String companyIndustry,
      int totalContacts,
      int activeContacts,
      double averageLeadScore
  ) {}
}