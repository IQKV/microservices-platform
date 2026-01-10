package com.iqscaffold.userservice.usermanagement;

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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of UserManagementService providing comprehensive user administration capabilities.
 *
 * <p>This service implements secure user lifecycle management with strict access controls, audit logging,
 * and multi-tenant isolation. All operations are restricted to users with administrative privileges
 * and include comprehensive security validations.
 *
 * @author IQ Scaffold Team
 * @version 1.0
 * @see UserManagementService
 * @see UserDto
 * @see UserContext
 * @since 1.0
 */
@Service
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

  private static final Logger logger = LoggerFactory.getLogger(UserManagementServiceImpl.class);

  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final UserAuditLogRepository auditLogRepository;
  private final PasswordEncoder passwordEncoder;

  public UserManagementServiceImpl(
      final UserRepository userRepository,
      final AuthorityRepository authorityRepository,
      final UserAuditLogRepository auditLogRepository,
      final PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.auditLogRepository = auditLogRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserDto> getAllUsers(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_USERS");

    Page<User> users;

    // SUPER_ADMIN can see all users
    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      users = userRepository.findAll(pageable);
    } else {
      // ADMIN can see non-super-admin users
      users = userRepository.findByAuthoritiesNameNot("SUPER_ADMIN", pageable);
    }

    // Convert to DTOs
    var userDtos = users.map(this::convertToDto);

    logAuditEvent("LIST_USERS", "Listed " + userDtos.getNumberOfElements() + " users", currentUser);

    return userDtos;
  }

  @Override
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

  @Override
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
    var authorities = getAuthoritiesByNames(request.authorities());
    validateRoleAssignment(authorities, currentUser);
    user.setAuthorities(authorities);

    var savedUser = userRepository.save(user);

    logAuditEvent("CREATE_USER", "Created user: " + savedUser.getUsername(), currentUser);

    return convertToDto(savedUser);
  }

  @Override
  @Caching(evict = {
      @CacheEvict(value = "users", key = "#userId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "users", allEntries = true, condition = "#request.authorities() != null")
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
    if (request.authorities() != null) {
      var authorities = getAuthoritiesByNames(request.authorities());
      validateRoleAssignment(authorities, currentUser);
      user.setAuthorities(authorities);
    }

    var updatedUser = userRepository.save(user);

    logAuditEvent("UPDATE_USER", "Updated user: " + updatedUser.getUsername(), currentUser);

    return convertToDto(updatedUser);
  }

  @Override
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
   * Validate user access based on role hierarchy and tenant ownership.
   */
  private void validateUserAccess(User user, UserContext currentUser) {
    // Validate tenant ownership
    if (!user.getTenantId().equals(currentUser.tenantId())) {
      throw new AccessDeniedException("User does not belong to current tenant");
    }

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
   * Get authorities by names.
   * 
   * <p>Authorities are stored in PUBLIC schema (system-wide), so all tenants
   * share the same set of roles. This ensures consistency and simplifies
   * role management across the platform.
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
        authorities,
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
}
