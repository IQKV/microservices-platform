package com.iqscaffold.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.usermanagement.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for OrganizationPreferenceManagementService.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationPreferenceManagementServiceTest {

  @Mock
  private OrganizationPreferenceRepository preferenceRepository;

  @Mock
  private OrganizationRepository organizationRepository;

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @InjectMocks
  private OrganizationPreferenceManagementService service;

  private UserContext adminUser;
  private Organization testOrganization;
  private OrganizationPreference testPreference;

  @BeforeEach
  void setUp() {
    adminUser = new UserContext(
        1L,
        "admin",
        "admin@test.com",
        Set.of("ADMIN"),
        Set.of(),
        "Admin",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    testOrganization = new Organization("Test Org", "tenant-123");
    testPreference = new OrganizationPreference(testOrganization);
    testPreference.setDefaultLocale("en");
    testPreference.setDefaultTimezone("UTC");
  }

  @Test
  @DisplayName("Should create organization preference successfully")
  void shouldCreateOrganizationPreference() {
    // Arrange
    var request = new CreateOrganizationPreferenceRequest(
        1L,
        "en",
        "UTC",
        "USD",
        true,
        true,
        8,
        true,
        true,
        true,
        true,
        30,
        5,
        15,
        false,
        "notify@test.com"
    );

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(preferenceRepository.existsByOrganizationId(anyLong())).thenReturn(false);
    when(preferenceRepository.save(any(OrganizationPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.createPreference(request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.defaultLocale()).isEqualTo("en");
    assertThat(result.defaultTimezone()).isEqualTo("UTC");
    verify(preferenceRepository).save(any(OrganizationPreference.class));
  }

  @Test
  @DisplayName("Should retrieve organization preference by ID")
  void shouldGetPreferenceById() {
    // Arrange
    when(preferenceRepository.findById(anyLong())).thenReturn(Optional.of(testPreference));

    // Act
    var result = service.getPreferenceById(1L, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.defaultLocale()).isEqualTo("en");
    verify(preferenceRepository).findById(1L);
  }

  @Test
  @DisplayName("Should update organization preference successfully")
  void shouldUpdateOrganizationPreference() {
    // Arrange
    var request = new UpdateOrganizationPreferenceRequest(
        "fr",
        "Europe/Paris",
        "EUR",
        null,
        null,
        null,
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

    when(preferenceRepository.findById(anyLong())).thenReturn(Optional.of(testPreference));
    when(preferenceRepository.save(any(OrganizationPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updatePreference(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(preferenceRepository).save(any(OrganizationPreference.class));
  }

  @Test
  @DisplayName("Should list all organization preferences")
  void shouldGetAllPreferences() {
    // Arrange
    var pageable = PageRequest.of(0, 20);
    when(preferenceRepository.findAll()).thenReturn(List.of(testPreference));

    // Act
    var result = service.getAllPreferences(pageable, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(preferenceRepository).findAll();
  }

  @Test
  @DisplayName("Should delete organization preference successfully")
  void shouldDeleteOrganizationPreference() {
    // Arrange
    when(preferenceRepository.findById(anyLong())).thenReturn(Optional.of(testPreference));

    // Act
    service.deletePreference(1L, adminUser);

    // Assert
    verify(preferenceRepository).delete(testPreference);
  }

  @Test
  @DisplayName("Should retrieve preference by organization ID")
  void shouldGetPreferenceByOrganizationId() {
    // Arrange
    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(preferenceRepository.findByOrganizationId(anyLong())).thenReturn(Optional.of(testPreference));

    // Act
    var result = service.getPreferenceByOrganizationId(1L, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.defaultLocale()).isEqualTo("en");
    verify(preferenceRepository).findByOrganizationId(1L);
  }

  @Test
  @DisplayName("Should throw exception when creating duplicate preference")
  void shouldThrowExceptionWhenCreatingDuplicatePreference() {
    // Arrange
    var request = new CreateOrganizationPreferenceRequest(
        1L,
        "en",
        "UTC",
        "USD",
        true,
        true,
        8,
        true,
        true,
        true,
        true,
        30,
        5,
        15,
        false,
        null
    );

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(preferenceRepository.existsByOrganizationId(anyLong())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createPreference(request, adminUser))
        .isInstanceOf(OrganizationPreferenceManagementService.OrganizationPreferenceManagementException.class)
        .hasMessageContaining("Preference already exists");
  }

  @Test
  @DisplayName("Should throw exception when preference not found")
  void shouldThrowExceptionWhenPreferenceNotFound() {
    // Arrange
    when(preferenceRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getPreferenceById(1L, adminUser))
        .isInstanceOf(OrganizationPreferenceManagementService.OrganizationPreferenceManagementException.class)
        .hasMessageContaining("Preference not found");
  }

  @Test
  @DisplayName("Should throw exception when organization not found for preference")
  void shouldThrowExceptionWhenOrganizationNotFoundForPreference() {
    // Arrange
    var request = new CreateOrganizationPreferenceRequest(
        1L,
        "en",
        "UTC",
        "USD",
        true,
        true,
        8,
        true,
        true,
        true,
        true,
        30,
        5,
        15,
        false,
        null
    );

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.createPreference(request, adminUser))
        .isInstanceOf(OrganizationPreferenceManagementService.OrganizationPreferenceManagementException.class)
        .hasMessageContaining("Organization not found");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for create operation")
  void shouldDenyAccessToNonAdminForCreate() {
    // Arrange
    var regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    var request = new CreateOrganizationPreferenceRequest(
        1L,
        "en",
        "UTC",
        "USD",
        true,
        true,
        8,
        true,
        true,
        true,
        true,
        30,
        5,
        15,
        false,
        null
    );

    // Act & Assert
    assertThatThrownBy(() -> service.createPreference(request, regularUser))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for read operation")
  void shouldDenyAccessToNonAdminForRead() {
    // Arrange
    var regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    // Act & Assert
    assertThatThrownBy(() -> service.getPreferenceById(1L, regularUser))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for update operation")
  void shouldDenyAccessToNonAdminForUpdate() {
    // Arrange
    var regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    var request = new UpdateOrganizationPreferenceRequest(
        "fr",
        null,
        null,
        null,
        null,
        null,
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

    // Act & Assert
    assertThatThrownBy(() -> service.updatePreference(1L, request, regularUser))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for delete operation")
  void shouldDenyAccessToNonAdminForDelete() {
    // Arrange
    var regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    // Act & Assert
    assertThatThrownBy(() -> service.deletePreference(1L, regularUser))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for list operation")
  void shouldDenyAccessToNonAdminForList() {
    // Arrange
    var regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null,
        Map.of()
    );

    var pageable = PageRequest.of(0, 20);

    // Act & Assert
    assertThatThrownBy(() -> service.getAllPreferences(pageable, regularUser))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should update preference with all fields")
  void shouldUpdatePreferenceWithAllFields() {
    // Arrange
    var request = new UpdateOrganizationPreferenceRequest(
        "es",
        "America/New_York",
        "EUR",
        false,
        false,
        12,
        false,
        false,
        false,
        false,
        60,
        3,
        30,
        true,
        "new-notify@test.com"
    );

    when(preferenceRepository.findById(anyLong())).thenReturn(Optional.of(testPreference));
    when(preferenceRepository.save(any(OrganizationPreference.class))).thenReturn(testPreference);

    // Act
    var result = service.updatePreference(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(preferenceRepository).save(any(OrganizationPreference.class));
  }
}
