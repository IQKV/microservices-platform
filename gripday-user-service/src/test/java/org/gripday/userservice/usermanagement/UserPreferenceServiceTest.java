package org.gripday.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.gripday.userservice.security.UserAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for UserPreferenceService.
 */
@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  @Mock
  private UserPreferenceRepository preferenceRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @InjectMocks
  private UserPreferenceService service;

  private UserContext testUser;
  private User user;
  private UserPreference testPreference;

  @BeforeEach
  void setUp() {
    testUser = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("USER"),
        Set.of(),
        "Test",
        "User",
        "tenant-123",
        null
    );

    user = new User("testuser", "test@example.com", "hash", "Test", "User", "tenant-123");
    testPreference = new UserPreference(user, "tenant-123");
    testPreference.setLocale("en");
    testPreference.setTimezone("UTC");
    testPreference.setTheme("light");
  }

  @Test
  @DisplayName("Should get user preferences successfully")
  void shouldGetUserPreferences() {
    // Arrange
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPreference));

    // Act
    var result = service.getMyPreferences(testUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.locale()).isEqualTo("en");
    assertThat(result.timezone()).isEqualTo("UTC");
    assertThat(result.theme()).isEqualTo("light");
    verify(preferenceRepository).findByUserId(1L);
  }

  @Test
  @DisplayName("Should create default preferences when not found")
  void shouldCreateDefaultPreferencesWhenNotFound() {
    // Arrange
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.getMyPreferences(testUser);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository).findById(1L);
    verify(preferenceRepository).save(any(UserPreference.class));
  }

  @Test
  @DisplayName("Should update user preferences successfully")
  void shouldUpdateUserPreferences() {
    // Arrange
    var request = new UpdateUserPreferenceRequest(
        "fr",
        "Europe/Paris",
        "EUR",
        null,
        null,
        "dark",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPreference));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updateMyPreferences(request, testUser);

    // Assert
    assertThat(result).isNotNull();
    verify(preferenceRepository).save(any(UserPreference.class));
  }

  @Test
  @DisplayName("Should delete user preferences successfully")
  void shouldDeleteUserPreferences() {
    // Arrange
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPreference));

    // Act
    service.deleteMyPreferences(testUser);

    // Assert
    verify(preferenceRepository).delete(testPreference);
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent preferences")
  void shouldThrowExceptionWhenDeletingNonExistentPreferences() {
    // Arrange
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteMyPreferences(testUser))
        .isInstanceOf(UserPreferenceService.UserPreferenceException.class)
        .hasMessageContaining("Preference not found");
  }

  @Test
  @DisplayName("Should update all preference fields")
  void shouldUpdateAllPreferenceFields() {
    // Arrange
    var request = new UpdateUserPreferenceRequest(
        "es",
        "America/New_York",
        "EUR",
        "dd/MM/yyyy",
        "HH:mm",
        "dark",
        "https://example.com/photo.jpg",
        "+1234567890",
        "Test bio",
        false,
        true,
        false,
        true,
        "sms",
        "{\"key\":\"value\"}"
    );

    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPreference));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updateMyPreferences(request, testUser);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    verify(preferenceRepository).save(any(UserPreference.class));
  }

  @Test
  @DisplayName("Should update partial preference fields")
  void shouldUpdatePartialPreferenceFields() {
    // Arrange
    var request = new UpdateUserPreferenceRequest(
        "fr",
        null,
        null,
        null,
        null,
        "dark",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPreference));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updateMyPreferences(request, testUser);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    verify(preferenceRepository).save(any(UserPreference.class));
  }

  @Test
  @DisplayName("Should create default preferences with correct defaults")
  void shouldCreateDefaultPreferencesWithCorrectDefaults() {
    // Arrange
    var newPreference = new UserPreference(user, "tenant-123");
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(newPreference);

    // Act
    var result = service.getMyPreferences(testUser);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    org.assertj.core.api.Assertions.assertThat(result.locale()).isEqualTo("en");
    org.assertj.core.api.Assertions.assertThat(result.timezone()).isEqualTo("UTC");
    org.assertj.core.api.Assertions.assertThat(result.theme()).isEqualTo("light");
    verify(preferenceRepository).save(any(UserPreference.class));
  }

  @Test
  @DisplayName("Should throw exception when user not found during preference creation")
  void shouldThrowExceptionWhenUserNotFoundDuringPreferenceCreation() {
    // Arrange
    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getMyPreferences(testUser))
        .isInstanceOf(UserPreferenceService.UserPreferenceException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  @DisplayName("Should update preferences when they don't exist yet")
  void shouldUpdatePreferencesWhenTheyDontExistYet() {
    // Arrange
    var request = new UpdateUserPreferenceRequest(
        "fr",
        "Europe/Paris",
        "EUR",
        null,
        null,
        "dark",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
    when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updateMyPreferences(request, testUser);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    verify(userRepository).findById(1L);
    verify(preferenceRepository, org.mockito.Mockito.times(2)).save(any(UserPreference.class));
  }
}
