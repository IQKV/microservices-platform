package org.gripday.userservice.presentation.web;

import jakarta.validation.Valid;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.domain.service.OrganizationManagementService;
import org.gripday.userservice.presentation.dto.CreateOrganizationRequest;
import org.gripday.userservice.presentation.dto.OrganizationDto;
import org.gripday.userservice.presentation.dto.UpdateOrganizationRequest;
import org.gripday.userservice.presentation.dto.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for organization management operations with admin-only access.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organization Management", description = "Admin-only organization management operations with tenant isolation")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationManagementResource {

  private final OrganizationManagementService organizationManagementService;

  public OrganizationManagementResource(final OrganizationManagementService organizationManagementService) {
    this.organizationManagementService = organizationManagementService;
  }

  @Operation(
      summary = "List all organizations",
      description = "Get paginated list of organizations within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.endpoint", extraTags = {"endpoint", "list"})
  public ResponseEntity<Page<OrganizationDto>> getAllOrganizations(
      @PageableDefault(size = 20) Pageable pageable,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var organizations = organizationManagementService.getAllOrganizations(pageable, currentUser);
    return ResponseEntity.ok(organizations);
  }

  @Operation(
      summary = "Get organization by ID",
      description = "Retrieve organization details by ID within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Organization retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or organization not accessible"),
      @ApiResponse(responseCode = "404", description = "Organization not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.endpoint", extraTags = {"endpoint", "get"})
  public ResponseEntity<OrganizationDto> getOrganizationById(
      @Parameter(description = "Organization ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var organization = organizationManagementService.getOrganizationById(id, currentUser);
    return ResponseEntity.ok(organization);
  }

  @Operation(
      summary = "Create new organization",
      description = "Create a new organization within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Organization created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "409", description = "Organization name already exists"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.endpoint", extraTags = {"endpoint", "create"})
  public ResponseEntity<OrganizationDto> createOrganization(
      @Parameter(description = "Organization creation request", required = true)
      @Valid @RequestBody CreateOrganizationRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var createdOrganization = organizationManagementService.createOrganization(request, currentUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdOrganization);
  }

  @Operation(
      summary = "Update organization",
      description = "Update existing organization within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Organization updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or organization not accessible"),
      @ApiResponse(responseCode = "404", description = "Organization not found in current tenant"),
      @ApiResponse(responseCode = "409", description = "Organization name already exists"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.endpoint", extraTags = {"endpoint", "update"})
  public ResponseEntity<OrganizationDto> updateOrganization(
      @Parameter(description = "Organization ID", required = true) @PathVariable Long id,
      @Parameter(description = "Organization update request", required = true)
      @Valid @RequestBody UpdateOrganizationRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var updatedOrganization = organizationManagementService.updateOrganization(id, request, currentUser);
    return ResponseEntity.ok(updatedOrganization);
  }

  @Operation(
      summary = "Delete organization",
      description = "Delete organization from current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Organization deleted successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or organization not accessible"),
      @ApiResponse(responseCode = "404", description = "Organization not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.endpoint", extraTags = {"endpoint", "delete"})
  public ResponseEntity<Void> deleteOrganization(
      @Parameter(description = "Organization ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    organizationManagementService.deleteOrganization(id, currentUser);
    return ResponseEntity.noContent().build();
  }
}
