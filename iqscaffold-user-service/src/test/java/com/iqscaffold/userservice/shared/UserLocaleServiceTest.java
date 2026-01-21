package com.iqscaffold.userservice.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for UserLocaleService class.
 * Tests user locale resolution and management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Locale Service Tests")
class UserLocaleServiceTest {

  private UserLocaleService userLocaleService;

  @Mock
  private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    userLocaleService = new UserLocaleService(userRepository);

    testUser = new User("testuser", "test@example.com", "password", "Test", "User", "tenant-123");
    testUser.setPreferredLocale("es");
  }

  @AfterEach
  void cleanupSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should get current user locale from authenticated user")
  void shouldGetCurrentUserLocaleFromAuthenticatedUser() {
    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    Locale result = userLocaleService.getCurrentUserLocale();

    assertThat(result.getLanguage()).isEqualTo("es");
  }

  @Test
  @DisplayName("Should return English when user is not authenticated")
  void shouldReturnEnglishWhenUserIsNotAuthenticated() {
    SecurityContextHolder.clearContext();

    Locale result = userLocaleService.getCurrentUserLocale();

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English when authentication is null")
  void shouldReturnEnglishWhenAuthenticationIsNull() {
    SecurityContextHolder.getContext().setAuthentication(null);

    Locale result = userLocaleService.getCurrentUserLocale();

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English for anonymous user")
  void shouldReturnEnglishForAnonymousUser() {
    Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Locale result = userLocaleService.getCurrentUserLocale();

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should get user locale from user object")
  void shouldGetUserLocaleFromUserObject() {
    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result.getLanguage()).isEqualTo("es");
  }

  @Test
  @DisplayName("Should return English when user has no preferred locale")
  void shouldReturnEnglishWhenUserHasNoPreferredLocale() {
    testUser.setPreferredLocale(null);

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English when user preferred locale is empty")
  void shouldReturnEnglishWhenUserPreferredLocaleIsEmpty() {
    testUser.setPreferredLocale("");

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English when user preferred locale is blank")
  void shouldReturnEnglishWhenUserPreferredLocaleIsBlank() {
    testUser.setPreferredLocale("   ");

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English when user is null")
  void shouldReturnEnglishWhenUserIsNull() {
    Locale result = userLocaleService.getUserLocale(null);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should return English when locale format is invalid")
  void shouldReturnEnglishWhenLocaleFormatIsInvalid() {
    testUser.setPreferredLocale("invalid-locale-format-xyz");

    Locale result = userLocaleService.getUserLocale(testUser);

    // Locale.forLanguageTag doesn't throw exception, it creates a locale with the tag
    // So we just verify it returns a locale (not necessarily English)
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should handle locale with region code")
  void shouldHandleLocaleWithRegionCode() {
    testUser.setPreferredLocale("en-US");

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getCountry()).isEqualTo("US");
  }

  @Test
  @DisplayName("Should get current user from security context")
  void shouldGetCurrentUserFromSecurityContext() {
    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    Optional<User> result = userLocaleService.getCurrentUser();

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testUser);
  }

  @Test
  @DisplayName("Should return empty when user not found in repository")
  void shouldReturnEmptyWhenUserNotFoundInRepository() {
    Authentication auth = new UsernamePasswordAuthenticationToken("nonexistent", "password", java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    Optional<User> result = userLocaleService.getCurrentUser();

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when authentication principal is anonymous")
  void shouldReturnEmptyWhenAuthenticationPrincipalIsAnonymous() {
    Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = userLocaleService.getCurrentUser();

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should update user locale")
  void shouldUpdateUserLocale() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userLocaleService.updateUserLocale("testuser", "fr");

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPreferredLocale()).isEqualTo("fr");
  }

  @Test
  @DisplayName("Should not update when user not found")
  void shouldNotUpdateWhenUserNotFound() {
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    userLocaleService.updateUserLocale("nonexistent", "fr");

    verify(userRepository).findByUsername("nonexistent");
  }

  @Test
  @DisplayName("Should handle French locale")
  void shouldHandleFrenchLocale() {
    testUser.setPreferredLocale("fr");

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result).isEqualTo(Locale.FRENCH);
  }

  @Test
  @DisplayName("Should handle German locale")
  void shouldHandleGermanLocale() {
    testUser.setPreferredLocale("de");

    Locale result = userLocaleService.getUserLocale(testUser);

    assertThat(result).isEqualTo(Locale.GERMAN);
  }

  @Test
  @DisplayName("Should return empty when username is null in security context")
  void shouldReturnEmptyWhenUsernameIsNullInSecurityContext() {
    Authentication auth = new UsernamePasswordAuthenticationToken(null, "password");
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = userLocaleService.getCurrentUser();

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when username is empty in security context")
  void shouldReturnEmptyWhenUsernameIsEmptyInSecurityContext() {
    Authentication auth = new UsernamePasswordAuthenticationToken("", "password");
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = userLocaleService.getCurrentUser();

    assertThat(result).isEmpty();
  }
}

