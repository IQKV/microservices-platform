package com.iqscaffold.userservice.usermanagement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for user management service providing comprehensive user administration capabilities.
 *
 * <p>This interface defines the contract for user management operations including:
 * <ul>
 *   <li>User retrieval with pagination and filtering</li>
 *   <li>User creation with role assignment</li>
 *   <li>User profile updates and modifications</li>
 *   <li>User deletion and deactivation</li>
 *   <li>Role-based access control enforcement</li>
 * </ul>
 *
 * <h4>Access Control:</h4>
 * <ul>
 *   <li><strong>SUPER_ADMIN</strong> - Can manage all users across all tenants</li>
 *   <li><strong>ADMIN</strong> - Can manage users within their tenant only</li>
 *   <li><strong>USER</strong> - No access to user management operations</li>
 * </ul>
 *
 * <h4>Security Features:</h4>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - Users can only manage within their tenant</li>
 *   <li><strong>Role Validation</strong> - Strict role-based access control</li>
 *   <li><strong>Audit Logging</strong> - All operations logged for compliance</li>
 *   <li><strong>Data Sanitization</strong> - Input validation and sanitization</li>
 * </ul>
 */
public interface UserManagementService {

  /**
   * Retrieve all users with pagination, role-based filtering, and comprehensive access control.
   *
   * @param pageable    Pagination parameters (page, size, sort)
   * @param currentUser The current user context for access control
   * @return Page of UserDto objects with user information
   * @throws UserManagementException If access is denied or operation fails
   */
  Page<UserDto> getAllUsers(Pageable pageable, UserContext currentUser);

  /**
   * Get user by ID with role-based access control and tenant filtering.
   *
   * @param userId      The ID of the user to retrieve
   * @param currentUser The current user context for access control
   * @return UserDto containing user information
   * @throws UserManagementException If user not found or access denied
   */
  UserDto getUserById(Long userId, UserContext currentUser);

  /**
   * Create a new user with role assignment and tenant association.
   *
   * @param request     The user creation request with user details
   * @param currentUser The current user context for access control
   * @return UserDto of the created user
   * @throws UserManagementException If creation fails or access denied
   */
  UserDto createUser(CreateUserRequest request, UserContext currentUser);

  /**
   * Update existing user with role and profile modifications.
   *
   * @param userId      The ID of the user to update
   * @param request     The update request with modified user details
   * @param currentUser The current user context for access control
   * @return UserDto of the updated user
   * @throws UserManagementException If update fails or access denied
   */
  UserDto updateUser(Long userId, UpdateUserRequest request, UserContext currentUser);

  /**
   * Delete user with proper cleanup and audit logging.
   *
   * @param userId      The ID of the user to delete
   * @param currentUser The current user context for access control
   * @throws UserManagementException If deletion fails or access denied
   */
  void deleteUser(Long userId, UserContext currentUser);

  /**
   * Custom exception for user management operations.
   */
  class UserManagementException extends RuntimeException {
    public UserManagementException(final String message) {
      super(message);
    }

    public UserManagementException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
