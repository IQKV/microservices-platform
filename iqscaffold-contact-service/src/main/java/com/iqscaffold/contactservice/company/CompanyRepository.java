package com.iqscaffold.contactservice.company;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Company> findByIndustry(String industry, Pageable pageable);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    List<Company> findByParentCompanyId(Long parentCompanyId);

    boolean existsByNameIgnoreCase(String name);
}
