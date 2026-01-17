package com.iqscaffold.contactservice.company;

import java.util.List;
import java.util.Optional;

import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(final CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    @CachePut(value = "companies", key = "#result.id")
    public Company createCompany(Company company) {
        logger.info("Creating new company: {}", company.getName());
        return companyRepository.save(company);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "companies", key = "#id")
    public Optional<Company> getCompanyById(Long id) {
        logger.debug("Retrieving company by id: {}", id);
        return companyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> getAllCompanies(Pageable pageable) {
        logger.debug("Retrieving all companies with pagination");
        return companyRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> searchCompanies(String name, Pageable pageable) {
        logger.debug("Searching companies by name: {}", name);
        return companyRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> getCompaniesByIndustry(String industry, Pageable pageable) {
        logger.debug("Retrieving companies by industry: {}", industry);
        return companyRepository.findByIndustry(industry, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> getCompaniesByStatus(CompanyStatus status, Pageable pageable) {
        logger.debug("Retrieving companies by status: {}", status);
        return companyRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> getChildCompanies(Long parentId) {
        logger.debug("Retrieving child companies for parent id: {}", parentId);
        return companyRepository.findByParentCompanyId(parentId);
    }

    @Override
    @CachePut(value = "companies", key = "#id")
    public Company updateCompany(Long id, Company company) {
        logger.info("Updating company with id: {}", id);
        if (!companyRepository.existsById(id)) {
            throw new ContactNotFoundException("Company not found with id: " + id);
        }
        company.setId(id);
        return companyRepository.save(company);
    }

    @Override
    @CacheEvict(value = "companies", key = "#id")
    public void deleteCompany(Long id) {
        logger.info("Deleting company with id: {}", id);
        companyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return companyRepository.existsByNameIgnoreCase(name);
    }
}
