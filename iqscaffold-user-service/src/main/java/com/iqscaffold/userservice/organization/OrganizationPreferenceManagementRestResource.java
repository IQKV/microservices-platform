package com.iqscaffold.userservice.organization;

import jakarta.validation.Valid;

import com.iqscaffold.userservice.usermanagement.UserContext;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * REST controller for organization preference management operations with admin-only access.
 */
@RestController
@RequestMapping("/api/v1/admin/organization-preferences")
@Tag(name = "Organization Preference Management", description = "Admin-only organization preference management operations with tenant isolation")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationPreferenceManagementRestResource {

  private final OrganizationPreferenceManagementService preferenceManagementService;

  public OrganizationPreferenceManagementRestResource(final OrganizationPreferenceManagementService preferenceManagementService) {
    this.preferenceManagementService = preferenceManagementService;
  }

  @Operation(
      summary = "List all organization preferences",
      description = "Get paginated list of organization preferences within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "list"})
  public ResponseEntity<Page<OrganizationPreferenceDto>> getAllPreferences(
      @PageableDefault(size = 20) Pageable pageable,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var preferences = preferenceManagementService.getAllPreferences(pageable, currentUser);
    return ResponseEntity.ok(preferences);
  }

  @Operation(
      summary = "Get organization preference by ID",
      description = "Retrieve organization preference details by ID within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preference retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or preference not accessible"),
      @ApiResponse(responseCode = "404", description = "Preference not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "get"})
  public ResponseEntity<OrganizationPreferenceDto> getPreferenceById(
      @Parameter(description = "Preference ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var preference = preferenceManagementService.getPreferenceById(id, currentUser);
    return ResponseEntity.ok(preference);
  }

  @Operation(
      summary = "Get organization preference by organization ID",
      description = "Retrieve organization preference by organization ID within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preference retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or preference not accessible"),
      @ApiResponse(responseCode = "404", description = "Preference not found for organization in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping("/organization/{organizationId}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "getByOrganization"})
  public ResponseEntity<OrganizationPreferenceDto> getPreferenceByOrganizationId(
      @Parameter(description = "Organization ID", required = true) @PathVariable Long organizationId,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var preference = preferenceManagementService.getPreferenceByOrganizationId(organizationId, currentUser);
    return ResponseEntity.ok(preference);
  }

  @Operation(
      summary = "Create new organization preference",
      description = "Create a new organization preference within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Preference created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "409", description = "Preference already exists for organization"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "create"})
  public ResponseEntity<OrganizationPreferenceDto> createPreference(
      @Parameter(description = "Preference creation request", required = true)
      @Valid @RequestBody CreateOrganizationPreferenceRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var createdPreference = preferenceManagementService.createPreference(request, currentUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdPreference);
  }

  @Operation(
      summary = "Update organization preference",
      description = "Update existing organization preference within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preference updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or preference not accessible"),
      @ApiResponse(responseCode = "404", description = "Preference not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "update"})
  public ResponseEntity<OrganizationPreferenceDto> updatePreference(
      @Parameter(description = "Preference ID", required = true) @PathVariable Long id,
      @Parameter(description = "Preference update request", required = true)
      @Valid @RequestBody UpdateOrganizationPreferenceRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var updatedPreference = preferenceManagementService.updatePreference(id, request, currentUser);
    return ResponseEntity.ok(updatedPreference);
  }

  @Operation(
      summary = "Delete organization preference",
      description = "Delete organization preference from current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Preference deleted successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or preference not accessible"),
      @ApiResponse(responseCode = "404", description = "Preference not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  @Timed(value = "organization.preference.endpoint", extraTags = {"endpoint", "delete"})
  public ResponseEntity<Void> deletePreference(
      @Parameter(description = "Preference ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    preferenceManagementService.deletePreference(id, currentUser);
    return ResponseEntity.noContent().build();
  }
}
