package com.iqscaffold.userservice.shared;

import java.util.Locale;
import java.util.Optional;

import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for resolving user locale preferences from authentication context.
 * Provides methods to get the current authenticated user's preferred locale.
 */
@Service
public class UserLocaleService {

  private final UserRepository userRepository;

  public UserLocaleService(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Get the current authenticated user's preferred locale.
   * Falls back to English if user is not authenticated or has no preference.
   */
  public Locale getCurrentUserLocale() {
    return getCurrentUser()
        .map(this::getUserLocale)
        .orElse(Locale.ENGLISH);
  }

  /**
   * Get locale for a specific user.
   * Falls back to English if user has no preference set.
   */
  public Locale getUserLocale(User user) {
    if (user != null && StringUtils.hasText(user.getPreferredLocale())) {
      try {
        return Locale.forLanguageTag(user.getPreferredLocale());
      } catch (final Exception e) {
        // Invalid locale format, fall back to English
      }
    }
    return Locale.ENGLISH;
  }

  /**
   * Get the current authenticated user from security context.
   */
  public Optional<User> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return Optional.empty();
    }

    // Extract username from authentication (assuming JWT contains username)
    String username = authentication.getName();
    if (StringUtils.hasText(username)) {
      return userRepository.findByUsername(username);
    }

    return Optional.empty();
  }

  /**
   * Update user's preferred locale.
   */
  public void updateUserLocale(String username, String locale) {
    userRepository.findByUsername(username)
        .ifPresent(user -> {
          user.setPreferredLocale(locale);
          userRepository.save(user);
        });
  }
}
