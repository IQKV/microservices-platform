package com.iqscaffold.contactservice.company;

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
 * Service demonstrating optimal usage of entity graphs for Company operations.
 * 
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * for company-related operations by eagerly loading specific relationships based
 * on business requirements.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Company Details</strong> - Load company with contacts for comprehensive view</li>
 *   <li><strong>Company Hierarchy</strong> - Load parent/child relationships for organizational structure</li>
 *   <li><strong>Complete Context</strong> - Load all relationships for detailed company management</li>
 *   <li><strong>Batch Operations</strong> - Use optimized queries for bulk company operations</li>
 * </ul>
 *
 * <h3>Business Use Cases</h3>
 * <ul>
 *   <li>Company profile pages with contact listings</li>
 *   <li>Organizational hierarchy visualization</li>
 *   <li>Company analytics and reporting</li>
 *   <li>Bulk company management operations</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CompanyEntityGraphService {

  private final CompanyRepository companyRepository;

  public CompanyEntityGraphService(final CompanyRepository companyRepository) {
    this.companyRepository = companyRepository;
  }

  /**
   * Find company by ID with contacts loaded.
   * Use this when displaying company profile with contact listings.
   *
   * @param id the company ID
   * @return Optional containing company with contacts if found
   */
  public Optional<Company> findCompanyWithContacts(final Long id) {
    return companyRepository.findByIdWithContacts(id);
  }

  /**
   * Find company by ID with parent company loaded.
   * Use this when displaying company details with parent organization context.
   *
   * @param id the company ID
   * @return Optional containing company with parent if found
   */
  public Optional<Company> findCompanyWithParent(final Long id) {
    return companyRepository.findByIdWithParent(id);
  }

  /**
   * Find company by ID with child companies loaded.
   * Use this when displaying company with its subsidiaries.
   *
   * @param id the company ID
   * @return Optional containing company with children if found
   */
  public Optional<Company> findCompanyWithChildren(final Long id) {
    return companyRepository.findByIdWithChildren(id);
  }

  /**
   * Find company by ID with complete hierarchy loaded.
   * Use this when displaying full organizational structure.
   *
   * @param id the company ID
   * @return Optional containing company with hierarchy if found
   */
  public Optional<Company> findCompanyWithHierarchy(final Long id) {
    return companyRepository.findByIdWithHierarchy(id);
  }

  /**
   * Find company by ID with all relationships loaded.
   * Use this for comprehensive company management operations.
   *
   * @param id the company ID
   * @return Optional containing company with all relationships if found
   */
  public Optional<Company> findCompanyComplete(final Long id) {
    return companyRepository.findByIdComplete(id);
  }

  /**
   * Find subsidiary companies with parent context.
   * Use this when displaying subsidiary companies with parent information.
   *
   * @param parentCompanyId the parent company ID
   * @return List of child companies with parent details loaded
   */
  public List<Company> findSubsidiariesWithParent(final Long parentCompanyId) {
    return companyRepository.findByParentCompanyIdWithParent(parentCompanyId);
  }

  /**
   * Find companies by status with contacts loaded.
   * Use this when filtering companies and need contact information.
   *
   * @param status the company status
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  public Page<Company> findCompaniesByStatusWithContacts(final CompanyStatus status, final Pageable pageable) {
    return companyRepository.findByStatusWithContacts(status, pageable);
  }

  /**
   * Find companies by industry with contacts loaded.
   * Use this for industry analysis with contact information.
   *
   * @param industry the industry
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  public Page<Company> findCompaniesByIndustryWithContacts(final String industry, final Pageable pageable) {
    return companyRepository.findByIndustryWithContacts(industry, pageable);
  }

  /**
   * Search companies by name with contacts loaded.
   * Use this when searching companies and need contact information.
   *
   * @param name the company name search term
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  public Page<Company> searchCompaniesWithContacts(final String name, final Pageable pageable) {
    return companyRepository.findByNameContainingIgnoreCaseWithContacts(name, pageable);
  }

  /**
   * Get all parent companies with their subsidiaries.
   * Use this for displaying complete organizational hierarchies.
   *
   * @return List of parent companies with children loaded
   */
  public List<Company> getCompanyHierarchies() {
    return companyRepository.findParentCompaniesWithChildren();
  }

  // Batch Operations

  /**
   * Find multiple companies by IDs in a single optimized query.
   * Use this for bulk operations to avoid N+1 query problems.
   *
   * @param companyIds list of company IDs
   * @return Map of company ID to Company for easy lookup
   */
  public Map<Long, Company> findCompaniesBatch(final List<Long> companyIds) {
    return companyRepository.findAllByIdIn(companyIds)
        .stream()
        .collect(Collectors.toMap(Company::getId, Function.identity()));
  }

  // Analytics and Reporting Methods

  /**
   * Get company profile with comprehensive information.
   * Loads company with all relationships for detailed profile display.
   *
   * @param companyId the company ID
   * @return CompanyProfile containing comprehensive company information, or null if not found
   */
  public CompanyProfile getCompanyProfile(final Long companyId) {
    return companyRepository.findByIdComplete(companyId)
        .map(company -> new CompanyProfile(
            company.getId(),
            company.getName(),
            company.getWebsite(),
            company.getIndustry(),
            company.getSize(),
            company.getStatus(),
            company.getContacts().size(),
            company.getContacts().stream()
                .mapToInt(c -> c.getStatus().name().equals("ACTIVE") ? 1 : 0)
                .sum(),
            company.getParentCompany() != null ? company.getParentCompany().getName() : null,
            company.getChildCompanies().size(),
            company.getChildCompanies().stream()
                .map(Company::getName)
                .collect(Collectors.toList())
        ))
        .orElse(null);
  }

  /**
   * Get company hierarchy summary.
   * Provides organizational structure information for a company.
   *
   * @param companyId the company ID
   * @return CompanyHierarchy containing hierarchy information, or null if not found
   */
  public CompanyHierarchy getCompanyHierarchy(final Long companyId) {
    return companyRepository.findByIdWithHierarchy(companyId)
        .map(company -> new CompanyHierarchy(
            company.getId(),
            company.getName(),
            company.getParentCompany() != null 
                ? new CompanyHierarchy.CompanyInfo(
                    company.getParentCompany().getId(),
                    company.getParentCompany().getName(),
                    company.getParentCompany().getIndustry()
                ) : null,
            company.getChildCompanies().stream()
                .map(child -> new CompanyHierarchy.CompanyInfo(
                    child.getId(),
                    child.getName(),
                    child.getIndustry()
                ))
                .collect(Collectors.toList())
        ))
        .orElse(null);
  }

  /**
   * Get company statistics with contact information.
   * Provides analytics data for company performance metrics.
   *
   * @param companyId the company ID
   * @return CompanyStats containing statistical information, or null if not found
   */
  public CompanyStats getCompanyStats(final Long companyId) {
    return companyRepository.findByIdWithContacts(companyId)
        .map(company -> {
          var contacts = company.getContacts();
          var activeContacts = contacts.stream()
              .mapToInt(c -> c.getStatus().name().equals("ACTIVE") ? 1 : 0)
              .sum();
          
          var averageLeadScore = contacts.stream()
              .mapToInt(c -> c.getLeadScore() != null ? c.getLeadScore() : 0)
              .average()
              .orElse(0.0);
          
          var highValueContacts = contacts.stream()
              .mapToInt(c -> c.getLeadScore() != null && c.getLeadScore() >= 80 ? 1 : 0)
              .sum();

          return new CompanyStats(
              company.getId(),
              company.getName(),
              company.getIndustry(),
              contacts.size(),
              activeContacts,
              averageLeadScore,
              highValueContacts
          );
        })
        .orElse(null);
  }

  /**
   * Data transfer object for comprehensive company profile information.
   */
  public record CompanyProfile(
      Long id,
      String name,
      String website,
      String industry,
      String size,
      CompanyStatus status,
      int totalContacts,
      int activeContacts,
      String parentCompanyName,
      int subsidiaryCount,
      List<String> subsidiaryNames
  ) {}

  /**
   * Data transfer object for company hierarchy information.
   */
  public record CompanyHierarchy(
      Long id,
      String name,
      CompanyInfo parentCompany,
      List<CompanyInfo> childCompanies
  ) {
    public record CompanyInfo(
        Long id,
        String name,
        String industry
    ) {}
  }

  /**
   * Data transfer object for company statistics.
   */
  public record CompanyStats(
      Long id,
      String name,
      String industry,
      int totalContacts,
      int activeContacts,
      double averageLeadScore,
      int highValueContacts
  ) {}
}