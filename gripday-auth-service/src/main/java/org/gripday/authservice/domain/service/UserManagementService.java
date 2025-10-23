package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.entity.UserAuditLog;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserAuditLogRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for user management operations with admin-only access and tenant isolation.
 * Implements role-based access control and comprehensive audit logging.
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
            UserRepository userRepository,
            AuthorityRepository authorityRepository,
            UserAuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all users with pagination and tenant filtering.
     * Only accessible by ADMIN and SUPER_ADMIN roles.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable, UserContext currentUser) {
        validateAdminAccess(currentUser, "LIST_USERS");
        
        var tenantId = currentUser.tenantId();
        var users = userRepository.findByTenantId(tenantId);
        
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
     * Get user by ID with tenant isolation.
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId, UserContext currentUser) {
        validateAdminAccess(currentUser, "GET_USER");
        
        var user = findUserByIdWithTenantCheck(userId, currentUser.tenantId());
        validateUserAccess(user, currentUser);
        
        logAuditEvent("GET_USER", "Retrieved user: " + user.getUsername(), currentUser);
        
        return convertToDto(user);
    }

    /**
     * Create new user with admin privileges and tenant isolation.
     */
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
        var authorities = getAuthoritiesByNames(request.roles());
        validateRoleAssignment(authorities, currentUser);
        user.setAuthorities(authorities);
        
        var savedUser = userRepository.save(user);
        
        logAuditEvent("CREATE_USER", "Created user: " + savedUser.getUsername(), currentUser);
        
        return convertToDto(savedUser);
    }

    /**
     * Update existing user with admin privileges and tenant isolation.
     */
    public UserDto updateUser(Long userId, UpdateUserRequest request, UserContext currentUser) {
        validateAdminAccess(currentUser, "UPDATE_USER");
        
        var user = findUserByIdWithTenantCheck(userId, currentUser.tenantId());
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
        
        // Update roles if provided
        if (request.roles() != null) {
            var authorities = getAuthoritiesByNames(request.roles());
            validateRoleAssignment(authorities, currentUser);
            user.setAuthorities(authorities);
        }
        
        var updatedUser = userRepository.save(user);
        
        logAuditEvent("UPDATE_USER", "Updated user: " + updatedUser.getUsername(), currentUser);
        
        return convertToDto(updatedUser);
    }

    /**
     * Delete user with admin privileges and tenant isolation.
     */
    public void deleteUser(Long userId, UserContext currentUser) {
        validateAdminAccess(currentUser, "DELETE_USER");
        
        var user = findUserByIdWithTenantCheck(userId, currentUser.tenantId());
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
        if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
        }
    }

    /**
     * Apply role-based filtering for hierarchical access control.
     */
    private List<User> applyRoleBasedFiltering(List<User> users, UserContext currentUser) {
        // SUPER_ADMIN can see all users
        if (currentUser.hasRole("SUPER_ADMIN")) {
            return users;
        }
        
        // ADMIN can see non-admin users and other admins (but not super admins)
        if (currentUser.hasRole("ADMIN")) {
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
        if (currentUser.hasRole("SUPER_ADMIN")) {
            return;
        }
        
        // ADMIN cannot access SUPER_ADMIN users
        if (currentUser.hasRole("ADMIN") && user.hasAuthority("SUPER_ADMIN")) {
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
        if (roleNames.contains("SUPER_ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Only super admin can assign super admin role");
        }
        
        // ADMIN cannot assign roles higher than their own level
        if (currentUser.hasRole("ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
            if (roleNames.contains("SUPER_ADMIN")) {
                throw new AccessDeniedException("Admin cannot assign super admin role");
            }
        }
    }

    /**
     * Find user by ID with tenant check.
     */
    private User findUserByIdWithTenantCheck(Long userId, String tenantId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserManagementException("User not found: " + userId));
        
        if (!user.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("User not found in current tenant");
        }
        
        return user;
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
            throw new UserManagementException("Unknown roles: " + missingNames);
        }
        
        return authorities.stream().collect(Collectors.toSet());
    }

    /**
     * Convert User entity to UserDto.
     */
    private UserDto convertToDto(User user) {
        var roles = user.getAuthorities().stream()
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
            roles,
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
        } catch (Exception e) {
            logger.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Exception for user management operations.
     */
    public static class UserManagementException extends RuntimeException {
        public UserManagementException(String message) {
            super(message);
        }
        
        public UserManagementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}