package com.iqscaffold.contactservice.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.contactservice.company.CompanyStatus;

public class CompanyDtos {

    public record CompanyResponse(
            Long id,
            String name,
            String website,
            String industry,
            String size,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Long parentCompanyId,
            CompanyStatus status,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateCompanyRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String website,
            @Size(max = 100) String industry,
            @Size(max = 50) String size,
            @Size(max = 20) String phone,
            @Email @Size(max = 255) String email,
            @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            Long parentCompanyId,
            CompanyStatus status,
            String notes) {
    }

    public record UpdateCompanyRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String website,
            @Size(max = 100) String industry,
            @Size(max = 50) String size,
            @Size(max = 20) String phone,
            @Email @Size(max = 255) String email,
            @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            Long parentCompanyId,
            CompanyStatus status,
            String notes) {
    }

    public record BulkOperationResponse(
            int successCount,
            int failureCount,
            List<BulkOperationResult> results) {
        public record BulkOperationResult(
                Long id,
                String name,
                boolean success,
                String message,
                CompanyResponse company) {
        }
    }

    public record BulkCreateCompaniesRequest(
            @Size(min = 1, max = 100) List<CreateCompanyRequest> companies) {
    }

    public record BulkUpdateStatusRequest(
            @Size(min = 1, max = 100) List<Long> companyIds,
            CompanyStatus status) {
    }
}
