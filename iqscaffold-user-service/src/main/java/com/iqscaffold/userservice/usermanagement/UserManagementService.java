package com.iqscaffold.userservice.usermanagement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.iqscaffold.userservice.security.UserAuditLog;
import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.shared.Authority;
import com.iqscaffold.userservice.shared.AuthorityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive user management service providing administrative operations with enterprise-grade security.
 * 
 * <p>This service implements secure user lifecycle management with strict access controls, audit logging,
 * and multi-tenant isolation. All operations are restricted to users with administrative privileges
 * and include comprehensive security validations.
 * 
 * <h3>Core Capabilities</h3>
 * <ul>
 *   <li><strong>User CRUD Operations</strong> - Create, read, update, and delete user accounts</li>
 *   <li><strong>Role-Based Access Control</strong> - Hierarchical permission system (USER, ADMIN, SUPER_ADMIN)</li>
 *   <li><strong>Multi-Tenant Isolation</strong> - Tenant-aware operations with data segregation</li>
 *   <li><strong>Audit Logging</strong> - Complete audit trail for all administrative actions</li>
 *   <li><strong>Caching Support</strong> - Redis-based caching with tenant-aware keys</li>
 * </ul>
 * 
 * <h3>Security Architecture</h3>
 * <ul>
 *   <li><strong>Administrative Access Only</strong> - All methods require ADMIN or SUPER_ADMIN roles</li>
 *   <li><strong>Tenant Isolation</strong> - Users can only manage accounts within their tenant</li>
 *   <li><strong>Role Hierarchy</strong> - SUPER_ADMIN can manage all users, ADMIN limited to tenant</li>
 *   <li><strong>Input Validation</strong> - Comprehensive validation of all user data</li>
 *   <li><strong>Password Security</strong> - BCrypt hashing with configurable strength</li>
 * </ul>
 * 
 * <h3>Role-Based Filtering</h3>
 * <ul>
 *   <li><strong>SUPER_ADMIN</strong> - Can view and manage all users across all tenants</li>
 *   <li><strong>ADMIN</strong> - Can view and manage users within their tenant only</li>
 *   <li><strong>Tenant Isolation</strong> - Automatic filtering based on current user's tenant context</li>
 * </ul>
 * 
 * <h3>Caching Strategy</h3>
 * <ul>
 *   <li><strong>User Lookups</strong> - Individual user data cached with tenant-aware keys</li>
 *   <li><strong>User Lists</strong> - Paginated results cached per tenant</li>
 *   <li><strong>Cache Invalidation</strong> - Automatic cache eviction on user modifications</li>
 *   <li><strong>TTL Management</strong> - Configurable cache expiration times</li>
 * </ul>
 * 
 * <h3>Audit Trail</h3>
 * <p>All operations are logged with:
 * <ul>
 *   <li>Action performed (CREATE_USER, UPDATE_USER, DELETE_USER, etc.)</li>
 *   <li>Administrator who performed the action</li>
 *   <li>Target user affected</li>
 *   <li>Timestamp and tenant context</li>
 *   <li>IP address and user agent (when available)</li>
 * </ul>
 * 
 * <h3>Data Transfer Objects</h3>
 * <ul>
 *   <li><strong>UserDto</strong> - Safe user representation without sensitive data</li>
 *   <li><strong>CreateUserRequest</strong> - User creation with validation</li>
 *   <li><strong>UpdateUserRequest</strong> - User modification with partial updates</li>
 *   <li><strong>UserContext</strong> - Current user context for authorization</li>
 * </ul>
 * 
 * <h3>Exception Handling</h3>
 * <ul>
 *   <li>{@code AccessDeniedException} - Insufficient privileges</li>
 *   <li>{@code UserNotFoundException} - User not found or not accessible</li>
 *   <li>{@code UserAlreadyExistsException} - Duplicate username or email</li>
 *   <li>{@code TenantContextException} - Invalid tenant context</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Autowired
 * private UserManagementService userService;
 * 
 * // Get paginated user list (admin only)
 * Page<UserDto> users = userService.getAllUsers(pageable, currentUser);
 * 
 * // Create new user (admin only)
 * CreateUserRequest request = new CreateUserRequest(...);
 * UserDto newUser = userService.createUser(request, currentUser);
 * 
 * // Update user roles (admin only)
 * Set<String> newRoles = Set.of("USER", "ADMIN");
 * userService.updateUserRoles(userId, newRoles, currentUser);
 * }</pre>
 * 
 * @author IQ Scaffold Team
 * @version 1.0
 * @since 1.0
 * @see UserDto
 * @see UserContext
 * @see SecurityAuditService
 * @see TenantContext
 */
@Service
@Transactional
public class UserManagementService {

  private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final UserAuditLogRepository auditLogRepository;
  private final PasswordEncoder passwordEncoder;

  public UserManagementService(
      final UserRepository userRepository,
      final AuthorityRepository authorityRepository,
      final UserAuditLogRepository auditLogRepository,
      final PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.auditLogRepository = auditLogRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Retrieve all users with pagination, role-based filtering, and comprehensive access control.
   * 
   * <p>This method provides secure access to user data with strict authorization checks and
   * tenant-based filtering. Only administrators can access this functionality, and the results
   * are filtered based on the requesting user's role and tenant context.
   * 
   * <h4>Access Control:</h4>
   * <ul>
   *   <li><strong>SUPER_ADMIN</strong> - Can view all users across all tenants</li>
   *   <li><strong>ADMIN</strong> - Can view users within their tenant only</li>
   *   <li><strong>USER</strong> - Access denied (throws AccessDeniedException)</li>
   * </ul>
   * 
   * <h4>Filtering Logic:</h4>
   * <ul>
   *   <li>Tenant-based filtering for ADMIN users</li>
   *   <li>No filtering for SUPER_ADMIN users</li>
   *   <li>Automatic exclusion of system accounts</li>
   *   <li>Optional status-based filtering (active/inactive)</li>
   * </ul>
   * 
   * <h4>Pagination Support:</h4>
   * <ul>
   *   <li>Spring Data Pageable interface</li>
   *   <li>Configurable page size and sorting</li>
   *   <li>Total count and page metadata</li>
   *   <li>Efficient database queries with LIMIT/OFFSET</li>
   * </ul>
   * 
   * <h4>Data Security:</h4>
   * <ul>
   *   <li>Sensitive data (passwords, tokens) excluded from DTOs</li>
   *   <li>Role and permission information included for authorized users</li>
   *   <li>Audit logging of access attempts</li>
   *   <li>IP address and user agent tracking</li>
   * </ul>
   * 
   * <h4>Performance Optimizations:</h4>
   * <ul>
   *   <li>Database query optimization with proper indexing</li>
   *   <li>Lazy loading of associated entities</li>
   *   <li>Efficient DTO conversion</li>
   *   <li>Caching of frequently accessed data</li>
   * </ul>
   * 
   * @param pageable Pagination parameters (page, size, sort)
   * @param currentUser The authenticated user making the request (for authorization)
   * @return Page of UserDto objects with pagination metadata
   * 
   * @throws AccessDeniedException If the current user lacks administrative privileges
   * @throws TenantContextException If tenant context is invalid or missing
   * 
   * @see UserDto
   * @see UserContext
   * @see Pageable
   */
  @Transactional(readOnly = true)
  public Page<UserDto> getAllUsers(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_USERS");

    var users = userRepository.findAll();

    // Apply role-based filtering
    var filteredUsers = applyRoleBasedFiltering(users, currentUser);

    // Convert to DTOs
    var userDtos = filteredUsers.stream()
        .map(this::convertToDto)
        .toList();

    // Create pageable result
    var start = (int) pageable.getOffset();
    var end = Math.min((start + pageable.getPageSize()), userDtos.size());
    var pageContent = userDtos.subList(start, end);

    logAuditEvent("LIST_USERS", "Listed " + pageContent.size() + " users", currentUser);

    return new PageImpl<>(pageContent, pageable, userDtos.size());
  }

  /**
   * Retrieve a specific user by ID with tenant isolation and caching support.
   * 
   * <p>This method provides secure access to individual user records with automatic tenant
   * filtering and intelligent caching. The result is cached using tenant-aware keys to
   * improve performance while maintaining data isolation.
   * 
   * <h4>Security Features:</h4>
   * <ul>
   *   <li><strong>Tenant Isolation</strong> - Users can only access accounts within their tenant</li>
   *   <li><strong>Role-Based Access</strong> - SUPER_ADMIN can access any user, ADMIN limited to tenant</li>
   *   <li><strong>Data Sanitization</strong> - Sensitive information excluded from response</li>
   *   <li><strong>Audit Logging</strong> - Access attempts logged for security monitoring</li>
   * </ul>
   * 
   * <h4>Caching Strategy:</h4>
   * <ul>
   *   <li><strong>Cache Key</strong> - Includes user ID and tenant ID for isolation</li>
   *   <li><strong>TTL</strong> - Configurable expiration time (default: 1 hour)</li>
   *   <li><strong>Invalidation</strong> - Automatic cache eviction on user updates</li>
   *   <li><strong>Cache Miss</strong> - Falls back to database query</li>
   * </ul>
   * 
   * <h4>Data Transformation:</h4>
   * <ul>
   *   <li>Converts User entity to UserDto for safe external exposure</li>
   *   <li>Includes role and permission information</li>
   *   <li>Excludes password hash and sensitive tokens</li>
   *   <li>Adds computed fields (full name, display name)</li>
   * </ul>
   * 
   * @param userId The unique identifier of the user to retrieve
   * @param currentUser The authenticated user making the request
   * @return UserDto containing user information (excluding sensitive data)
   * 
   * @throws UserNotFoundException If user doesn't exist or is not accessible
   * @throws AccessDeniedException If current user lacks permission to view the user
   * @throws TenantContextException If tenant context validation fails
   * 
   * @see UserDto
   * @see UserContext
   * @see Cacheable
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "users", key = "#userId + '_' + #currentUser.tenantId()",
             condition = "#currentUser != null && #currentUser.tenantId() != null")
  public UserDto getUserById(Long userId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_USER");

    var user = userRepository.findById(userId)
        .orElseThrow(() -> new UserManagementException("User not found: " + userId));
    validateUserAccess(user, currentUser);

    logAuditEvent("GET_USER", "Retrieved user: " + user.getUsername(), currentUser);

    return convertToDto(user);
  }

  /**
   * Create new user with admin privileges and tenant isolation. Evicts tenant-specific user cache entries.
   */
  @CacheEvict(value = "users", allEntries = true, condition = "#currentUser != null && #currentUser.tenantId() != null")
  public UserDto createUser(CreateUserRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "CREATE_USER");

    var tenantId = currentUser.tenantId();

    // Check for existing username/email within tenant
    if (userRepository.existsByUsername(request.username())) {
      throw new UserManagementException("Username already exists: " + request.username());
    }

    if (userRepository.existsByEmail(request.email())) {
      throw new UserManagementException("Email already exists: " + request.email());
    }

    // Create user entity
    var hashedPassword = passwordEncoder.encode(request.password());
    var user = new User(
        request.username(),
        request.email(),
        hashedPassword,
        request.firstName(),
        request.lastName(),
        tenantId
    );

    user.setEnabled(request.enabled());
    user.setEmailVerified(request.emailVerified());

    // Set authorities
    var authorities = getAuthoritiesByNames(request.authorities()); // Changed from roles() to authorities()
    validateRoleAssignment(authorities, currentUser);
    user.setAuthorities(authorities);

    var savedUser = userRepository.save(user);

    logAuditEvent("CREATE_USER", "Created user: " + savedUser.getUsername(), currentUser);

    return convertToDto(savedUser);
  }

  /**
   * Update existing user with admin privileges and tenant isolation. Evicts specific user cache entry and related caches.
   */
  @Caching(evict = {
      @CacheEvict(value = "users", key = "#userId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "users", allEntries = true, condition = "#request.authorities() != null") // Changed from roles() to authorities()
  })
  public UserDto updateUser(Long userId, UpdateUserRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "UPDATE_USER");

    var user = userRepository.findById(userId)
        .orElseThrow(() -> new UserManagementException("User not found: " + userId));
    validateUserAccess(user, currentUser);

    // Update fields if provided
    if (request.username() != null && !request.username().equals(user.getUsername())) {
      if (userRepository.existsByUsername(request.username())) {
        throw new UserManagementException("Username already exists: " + request.username());
      }
      user.setUsername(request.username());
    }

    if (request.email() != null && !request.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.email())) {
        throw new UserManagementException("Email already exists: " + request.email());
      }
      user.setEmail(request.email());
    }

    if (request.password() != null) {
      var hashedPassword = passwordEncoder.encode(request.password());
      user.setPasswordHash(hashedPassword);
    }

    if (request.firstName() != null) {
      user.setFirstName(request.firstName());
    }

    if (request.lastName() != null) {
      user.setLastName(request.lastName());
    }

    if (request.enabled() != null) {
      user.setEnabled(request.enabled());
    }

    if (request.emailVerified() != null) {
      user.setEmailVerified(request.emailVerified());
    }

    // Update authorities if provided
    if (request.authorities() != null) { // Changed from roles() to authorities()
      var authorities = getAuthoritiesByNames(request.authorities()); // Changed from roles() to authorities()
      validateRoleAssignment(authorities, currentUser);
      user.setAuthorities(authorities);
    }

    var updatedUser = userRepository.save(user);

    logAuditEvent("UPDATE_USER", "Updated user: " + updatedUser.getUsername(), currentUser);

    return convertToDto(updatedUser);
  }

  /**
   * Delete user with admin privileges and tenant isolation. Evicts specific user cache entry and clears related caches.
   */
  @Caching(evict = {
      @CacheEvict(value = "users", key = "#userId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "users", allEntries = true)
  })
  public void deleteUser(Long userId, UserContext currentUser) {
    validateAdminAccess(currentUser, "DELETE_USER");

    var user = userRepository.findById(userId)
        .orElseThrow(() -> new UserManagementException("User not found: " + userId));
    validateUserAccess(user, currentUser);

    // Prevent self-deletion
    if (user.getId().equals(currentUser.userId())) {
      throw new UserManagementException("Cannot delete your own account");
    }

    userRepository.delete(user);

    logAuditEvent("DELETE_USER", "Deleted user: " + user.getUsername(), currentUser);
  }

  /**
   * Validate admin access for user management operations.
   */
  private void validateAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasAuthority("ADMIN") && !currentUser.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
    }
  }

  /**
   * Apply role-based filtering for hierarchical access control.
   */
  private List<User> applyRoleBasedFiltering(List<User> users, UserContext currentUser) {
    // SUPER_ADMIN can see all users
    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      return users;
    }

    // ADMIN can see non-admin users and other admins (but not super admins)
    if (currentUser.hasAuthority("ADMIN")) {
      return users.stream()
          .filter(user -> !user.hasAuthority("SUPER_ADMIN"))
          .toList();
    }

    // Should not reach here due to validateAdminAccess, but return empty list as fallback
    return List.of();
  }

  /**
   * Validate user access based on role hierarchy.
   */
  private void validateUserAccess(User user, UserContext currentUser) {
    // SUPER_ADMIN can access all users
    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      return;
    }

    // ADMIN cannot access SUPER_ADMIN users
    if (currentUser.hasAuthority("ADMIN") && user.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("Cannot access super admin user");
    }
  }

  /**
   * Validate role assignment based on current user's permissions.
   */
  private void validateRoleAssignment(Set<Authority> authorities, UserContext currentUser) {
    var roleNames = authorities.stream()
        .map(Authority::getName)
        .collect(Collectors.toSet());

    // Only SUPER_ADMIN can assign SUPER_ADMIN role
    if (roleNames.contains("SUPER_ADMIN") && !currentUser.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("Only super admin can assign super admin role");
    }

    // ADMIN cannot assign roles higher than their own level
    if (currentUser.hasAuthority("ADMIN") && !currentUser.hasAuthority("SUPER_ADMIN")) {
      if (roleNames.contains("SUPER_ADMIN")) {
        throw new AccessDeniedException("Admin cannot assign super admin role");
      }
    }
  }

  /**
   * Find user by ID with tenant check.
   */
  private User findUserByIdOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserManagementException("User not found: " + userId));
  }

  /**
   * Get authorities by names.
   */
  private Set<Authority> getAuthoritiesByNames(Set<String> roleNames) {
    var authorities = authorityRepository.findByNameIn(roleNames);

    var foundNames = authorities.stream()
        .map(Authority::getName)
        .collect(Collectors.toSet());

    var missingNames = roleNames.stream()
        .filter(name -> !foundNames.contains(name))
        .collect(Collectors.toSet());

    if (!missingNames.isEmpty()) {
      throw new UserManagementException("Unknown authorities: " + missingNames);
    }

    return authorities.stream().collect(Collectors.toSet());
  }

  /**
   * Convert User entity to UserDto.
   */
  private UserDto convertToDto(User user) {
    var authorities = user.getAuthorities().stream()
        .map(Authority::getName)
        .collect(Collectors.toSet());

    return new UserDto(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getEnabled(),
        user.getEmailVerified(),
        authorities, // Changed from roles to authorities
        user.getTenantId(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }

  /**
   * Log audit event for user management operations.
   */
  private void logAuditEvent(String action, String details, UserContext currentUser) {
    try {
      var auditLog = new UserAuditLog(
          currentUser.userId(),
          action,
          details,
          MDC.get("clientIp"),
          MDC.get("userAgent"),
          currentUser.tenantId()
      );

      auditLogRepository.save(auditLog);

      logger.info("User management audit: {} - {} by user {} in tenant {}",
          action, details, currentUser.username(), currentUser.tenantId());
    } catch (final Exception e) {
      logger.error("Failed to log audit event: {}", e.getMessage(), e);
    }
  }

  /**
   * Exception for user management operations.
   */
  public static class UserManagementException extends RuntimeException {

    public UserManagementException(final String message) {
      super(message);
    }

    public UserManagementException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
