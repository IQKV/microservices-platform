package com.iqscaffold.contactservice.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

  Company createCompany(Company company);

  Optional<Company> getCompanyById(Long id);

  Page<Company> getAllCompanies(Pageable pageable);

  Page<Company> searchCompanies(String name, Pageable pageable);

  Page<Company> getCompaniesByIndustry(String industry, Pageable pageable);

  Page<Company> getCompaniesByStatus(CompanyStatus status, Pageable pageable);

  List<Company> getChildCompanies(Long parentId);

  Company updateCompany(Long id, Company company);

  void deleteCompany(Long id);

  boolean existsByName(String name);
}
