package com.iqscaffold.contactservice.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

  Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Page<Company> findByIndustry(String industry, Pageable pageable);

  Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

  List<Company> findByParentCompanyId(Long parentCompanyId);

  boolean existsByNameIgnoreCase(String name);

  // Entity Graph Methods for optimized queries

  /**
   * Find company by ID with contacts loaded.
   * Use this when you need company and its contacts together.
   *
   * @param id the company ID
   * @return Optional containing company with contacts if found
   */
  @EntityGraph("company-with-contacts")
  @Query("SELECT c FROM Company c WHERE c.id = :id")
  Optional<Company> findByIdWithContacts(@Param("id") Long id);

  /**
   * Find company by ID with parent company loaded.
   * Use this when you need company and parent company information.
   *
   * @param id the company ID
   * @return Optional containing company with parent if found
   */
  @EntityGraph("company-with-parent")
  @Query("SELECT c FROM Company c WHERE c.id = :id")
  Optional<Company> findByIdWithParent(@Param("id") Long id);

  /**
   * Find company by ID with child companies loaded.
   * Use this when you need company and its subsidiaries.
   *
   * @param id the company ID
   * @return Optional containing company with children if found
   */
  @EntityGraph("company-with-children")
  @Query("SELECT c FROM Company c WHERE c.id = :id")
  Optional<Company> findByIdWithChildren(@Param("id") Long id);

  /**
   * Find company by ID with complete hierarchy loaded.
   * Use this when you need company with both parent and child relationships.
   *
   * @param id the company ID
   * @return Optional containing company with hierarchy if found
   */
  @EntityGraph("company-with-hierarchy")
  @Query("SELECT c FROM Company c WHERE c.id = :id")
  Optional<Company> findByIdWithHierarchy(@Param("id") Long id);

  /**
   * Find company by ID with all relationships loaded.
   * Use this when you need complete company information including contacts and hierarchy.
   *
   * @param id the company ID
   * @return Optional containing company with all relationships if found
   */
  @EntityGraph("company-complete")
  @Query("SELECT c FROM Company c WHERE c.id = :id")
  Optional<Company> findByIdComplete(@Param("id") Long id);

  /**
   * Find companies by parent company ID with parent details loaded.
   * Use this when displaying subsidiary companies with parent context.
   *
   * @param parentCompanyId the parent company ID
   * @return List of child companies with parent details loaded
   */
  @EntityGraph("company-with-parent")
  @Query("SELECT c FROM Company c WHERE c.parentCompanyId = :parentCompanyId")
  List<Company> findByParentCompanyIdWithParent(@Param("parentCompanyId") Long parentCompanyId);

  /**
   * Find companies by status with contacts loaded.
   * Use this when filtering companies and need contact information.
   *
   * @param status the company status
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  @EntityGraph("company-with-contacts")
  @Query("SELECT c FROM Company c WHERE c.status = :status")
  Page<Company> findByStatusWithContacts(@Param("status") CompanyStatus status, Pageable pageable);

  /**
   * Find companies by industry with contacts loaded.
   * Use this when filtering by industry and need contact information.
   *
   * @param industry the industry
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  @EntityGraph("company-with-contacts")
  @Query("SELECT c FROM Company c WHERE c.industry = :industry")
  Page<Company> findByIndustryWithContacts(@Param("industry") String industry, Pageable pageable);

  /**
   * Search companies by name with contacts loaded.
   * Use this when searching companies and need contact information.
   *
   * @param name the company name search term
   * @param pageable pagination information
   * @return Page of companies with contacts loaded
   */
  @EntityGraph("company-with-contacts")
  @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  Page<Company> findByNameContainingIgnoreCaseWithContacts(@Param("name") String name, Pageable pageable);

  /**
   * Find all parent companies (companies with no parent) with child companies loaded.
   * Use this for displaying company hierarchies.
   *
   * @return List of parent companies with children loaded
   */
  @EntityGraph("company-with-children")
  @Query("SELECT c FROM Company c WHERE c.parentCompanyId IS NULL")
  List<Company> findParentCompaniesWithChildren();

  /**
   * Find multiple companies by IDs in a single query.
   * Use this for bulk operations to avoid N+1 query problems.
   *
   * @param ids list of company IDs
   * @return List of companies
   */
  @Query("SELECT c FROM Company c WHERE c.id IN :ids")
  List<Company> findAllByIdIn(@Param("ids") List<Long> ids);
}
