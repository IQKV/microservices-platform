package org.gripday.userservice.usermanagement;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.usermanagement.UserManagementService;
import org.gripday.userservice.usermanagement.CreateUserRequest;
import org.gripday.userservice.usermanagement.UpdateUserRequest;
import org.gripday.userservice.usermanagement.UserContext;
import org.gripday.userservice.usermanagement.UserDto;
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
 * REST controller for user management operations with admin-only access. Implements role-based access control and tenant isolation.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "User Management", description = "Admin-only user management operations with tenant isolation")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementRestResource {

  private final UserManagementService userManagementService;

  public UserManagementRestResource(final UserManagementService userManagementService) {
    this.userManagementService = userManagementService;
  }

  @Operation(
      summary = "List all users",
      description = "Get paginated list of users within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<Page<UserDto>> getAllUsers(
      @PageableDefault(size = 20) Pageable pageable,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var users = userManagementService.getAllUsers(pageable, currentUser);
    return ResponseEntity.ok(users);
  }

  @Operation(
      summary = "Get user by ID",
      description = "Retrieve user details by ID within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or user not accessible"),
      @ApiResponse(responseCode = "404", description = "User not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<UserDto> getUserById(
      @Parameter(description = "User ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var user = userManagementService.getUserById(id, currentUser);
    return ResponseEntity.ok(user);
  }

  @Operation(
      summary = "Create new user",
      description = "Create a new user account within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions for role assignment"),
      @ApiResponse(responseCode = "409", description = "Username or email already exists"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<UserDto> createUser(
      @Parameter(description = "User creation request", required = true)
      @Valid @RequestBody CreateUserRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var createdUser = userManagementService.createUser(request, currentUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @Operation(
      summary = "Update user",
      description = "Update existing user within current tenant. Requires ADMIN or SUPER_ADMIN role."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions or user not accessible"),
      @ApiResponse(responseCode = "404", description = "User not found in current tenant"),
      @ApiResponse(responseCode = "409", description = "Username or email already exists"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<UserDto> updateUser(
      @Parameter(description = "User ID", required = true) @PathVariable Long id,
      @Parameter(description = "User update request", required = true)
      @Valid @RequestBody UpdateUserRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var updatedUser = userManagementService.updateUser(id, request, currentUser);
    return ResponseEntity.ok(updatedUser);
  }

  @Operation(
      summary = "Delete user",
      description = "Delete user from current tenant. Requires ADMIN or SUPER_ADMIN role. Cannot delete own account."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "User deleted successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions, user not accessible, or attempting self-deletion"),
      @ApiResponse(responseCode = "404", description = "User not found in current tenant"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User ID", required = true) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    userManagementService.deleteUser(id, currentUser);
    return ResponseEntity.noContent().build();
  }
}
