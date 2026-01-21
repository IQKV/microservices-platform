package com.iqscaffold.userservice.usermanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service demonstrating optimal usage of entity graphs for User operations.
 *
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * by eagerly loading specific relationships based on use case requirements. Each method
 * is designed for a specific scenario where certain associations are needed.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Authentication</strong> - Load user with authorities for role checking</li>
 *   <li><strong>Profile Management</strong> - Load user with preferences for settings</li>
 *   <li><strong>Complete Context</strong> - Load user with all associations for admin operations</li>
 * </ul>
 *
 * <h3>Performance Benefits</h3>
 * <ul>
 *   <li>Eliminates N+1 query problems</li>
 *   <li>Reduces database round trips</li>
 *   <li>Optimizes memory usage by loading only needed data</li>
 *   <li>Leverages Hibernate second-level cache effectively</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class UserEntityGraphService {

  private final UserRepository userRepository;

  public UserEntityGraphService(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Find user for authentication with authorities loaded.
   * Use this when you need to check user roles and permissions.
   *
   * @param username the username to search for
   * @return Optional containing user with authorities if found
   */
  public Optional<User> findUserForAuthentication(String username) {
    return userRepository.findByUsernameWithAuthorities(username);
  }

  /**
   * Find user for authentication by email with authorities loaded.
   * Use this when users can login with email address.
   *
   * @param email the email to search for
   * @return Optional containing user with authorities if found
   */
  public Optional<User> findUserForAuthenticationByEmail(String email) {
    return userRepository.findByEmailWithAuthorities(email);
  }

  /**
   * Find user for profile management with preferences loaded.
   * Use this when displaying or updating user profile settings.
   *
   * @param userId the user ID to search for
   * @return Optional containing user with preferences if found
   */
  public Optional<User> findUserForProfile(Long userId) {
    return userRepository.findByIdWithPreferences(userId);
  }

  /**
   * Find user with complete context (authorities and preferences).
   * Use this for admin operations or when you need full user context.
   *
   * @param identifier username or email address
   * @return Optional containing user with complete profile if found
   */
  public Optional<User> findUserWithCompleteProfile(String identifier) {
    return userRepository.findByUsernameOrEmailWithComplete(identifier, identifier);
  }

  /**
   * Find users by role with authorities loaded.
   * Use this when you need to list users with specific roles and their permissions.
   *
   * @param roleName the role name to search for
   * @return List of users with the specified role and their authorities loaded
   */
  public List<User> findUsersByRoleWithAuthorities(String roleName) {
    return userRepository.findByAuthorityNameWithAuthorities(roleName);
  }

  /**
   * Check if user has specific authority.
   * Optimized method that loads only authorities for permission checking.
   *
   * @param username      the username to check
   * @param authorityName the authority name to verify
   * @return true if user has the authority, false otherwise
   */
  public boolean userHasAuthority(String username, String authorityName) {
    return userRepository.findByUsernameWithAuthorities(username)
        .map(user -> user.hasAuthority(authorityName))
        .orElse(false);
  }

  /**
   * Get user's full name and locale for personalization.
   * Loads user with preferences for display purposes.
   *
   * @param userId the user ID
   * @return UserDisplayInfo containing name and locale, or null if not found
   */
  public UserDisplayInfo getUserDisplayInfo(Long userId) {
    return userRepository.findByIdWithPreferences(userId)
        .map(user -> new UserDisplayInfo(
            user.getFullName(),
            user.getPreferredLocale(),
            user.getPreference() != null ? user.getPreference().getTimezone() : "UTC"
        ))
        .orElse(null);
  }

  /**
   * Data transfer object for user display information.
   */
  public record UserDisplayInfo(
      String fullName,
      String locale,
      String timezone
  ) {
  }
}
