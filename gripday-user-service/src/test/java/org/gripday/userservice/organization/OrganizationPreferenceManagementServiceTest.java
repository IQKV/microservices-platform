package org.gripday.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.gripday.userservice.security.UserAuditLogRepository;
import org.gripday.userservice.usermanagement.UserContext;
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
        null
    );

    testOrganization = new Organization("Test Org", "tenant-123");
    testPreference = new OrganizationPreference(testOrganization, "tenant-123");
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
        null,
        null,
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
        false,
        "notify@test.com",
        "support@test.com",
        null
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
}
