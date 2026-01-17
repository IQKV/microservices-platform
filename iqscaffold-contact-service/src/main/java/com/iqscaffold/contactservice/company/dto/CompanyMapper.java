package com.iqscaffold.contactservice.company.dto;

import com.iqscaffold.contactservice.company.Company;
import com.iqscaffold.contactservice.company.dto.CompanyDtos.CompanyResponse;
import com.iqscaffold.contactservice.company.dto.CompanyDtos.CreateCompanyRequest;
import com.iqscaffold.contactservice.company.dto.CompanyDtos.UpdateCompanyRequest;

public class CompanyMapper {

    public static CompanyResponse toResponse(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                company.getSize(),
                company.getPhone(),
                company.getEmail(),
                company.getAddressLine1(),
                company.getAddressLine2(),
                company.getCity(),
                company.getState(),
                company.getPostalCode(),
                company.getCountry(),
                company.getParentCompanyId(),
                company.getStatus(),
                company.getNotes(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }

    public static Company toEntity(CreateCompanyRequest request, String userId) {
        if (request == null) {
            return null;
        }
        Company company = new Company();
        company.setName(request.name());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setSize(request.size());
        company.setPhone(request.phone());
        company.setEmail(request.email());
        company.setAddressLine1(request.addressLine1());
        company.setAddressLine2(request.addressLine2());
        company.setCity(request.city());
        company.setState(request.state());
        company.setPostalCode(request.postalCode());
        company.setCountry(request.country());
        company.setParentCompanyId(request.parentCompanyId());
        if (request.status() != null) {
            company.setStatus(request.status());
        }
        company.setNotes(request.notes());
        company.setCreatedBy(userId);
        company.setUpdatedBy(userId);
        return company;
    }

    public static void updateEntity(Company company, UpdateCompanyRequest request, String userId) {
        if (company == null || request == null) {
            return;
        }
        company.setName(request.name());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setSize(request.size());
        company.setPhone(request.phone());
        company.setEmail(request.email());
        company.setAddressLine1(request.addressLine1());
        company.setAddressLine2(request.addressLine2());
        company.setCity(request.city());
        company.setState(request.state());
        company.setPostalCode(request.postalCode());
        company.setCountry(request.country());
        company.setParentCompanyId(request.parentCompanyId());
        if (request.status() != null) {
            company.setStatus(request.status());
        }
        company.setNotes(request.notes());
        company.setUpdatedBy(userId);
    }
}
