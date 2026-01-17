package com.iqscaffold.contactservice.company;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.iqscaffold.contactservice.company.dto.CompanyDtos;
import com.iqscaffold.contactservice.company.dto.CompanyMapper;
import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import com.iqscaffold.contactservice.shared.exception.DuplicateResourceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for company management operations.
 */
@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Company management operations")
@SecurityRequirement(name = "bearerAuth")
public class CompanyRestResource {

    private final CompanyService companyService;

    public CompanyRestResource(final CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Create company", description = "Creates a new company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Company created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Company name already exists")
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CompanyDtos.CompanyResponse> createCompany(
            @Valid @RequestBody CompanyDtos.CreateCompanyRequest request) {
        if (companyService.existsByName(request.name())) {
            throw new DuplicateResourceException("Company with name already exists: " + request.name());
        }

        String userId = getCurrentUserId();
        Company company = CompanyMapper.toEntity(request, userId);
        Company savedCompany = companyService.createCompany(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyMapper.toResponse(savedCompany));
    }

    @Operation(summary = "Get company by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CompanyDtos.CompanyResponse> getCompany(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id)
                .orElseThrow(() -> new ContactNotFoundException("Company not found with id: " + id));
        return ResponseEntity.ok(CompanyMapper.toResponse(company));
    }

    @Operation(summary = "List companies")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<CompanyDtos.CompanyResponse>> listCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) CompanyStatus status,
            Pageable pageable) {

        Page<Company> companies;
        if (search != null && !search.isBlank()) {
            companies = companyService.searchCompanies(search, pageable);
        } else if (industry != null) {
            companies = companyService.getCompaniesByIndustry(industry, pageable);
        } else if (status != null) {
            companies = companyService.getCompaniesByStatus(status, pageable);
        } else {
            companies = companyService.getAllCompanies(pageable);
        }

        return ResponseEntity.ok(companies.map(CompanyMapper::toResponse));
    }

    @Operation(summary = "Update company")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CompanyDtos.CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyDtos.UpdateCompanyRequest request) {

        Company existing = companyService.getCompanyById(id)
                .orElseThrow(() -> new ContactNotFoundException("Company not found with id: " + id));

        if (!existing.getName().equals(request.name()) && companyService.existsByName(request.name())) {
            throw new DuplicateResourceException("Company with name already exists: " + request.name());
        }

        String userId = getCurrentUserId();
        CompanyMapper.updateEntity(existing, request, userId);
        Company updated = companyService.updateCompany(id, existing);
        return ResponseEntity.ok(CompanyMapper.toResponse(updated));
    }

    @Operation(summary = "Delete company")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String userId = jwt.getClaimAsString("userId");
            if (userId != null) {
                return userId;
            }
            return jwt.getSubject();
        }
        return "system";
    }
}
