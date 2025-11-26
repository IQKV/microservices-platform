package com.iqscaffold.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserContext;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for OrganizationManagementService.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationManagementServiceTest {

  @Mock
  private OrganizationRepository organizationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @InjectMocks
  private OrganizationManagementService service;

  private UserContext adminUser;
  private UserContext regularUser;
  private Organization testOrganization;
  private User testOwner;

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

    regularUser = new UserContext(
        2L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null
    );

    testOrganization = new Organization("Test Org", "tenant-123");
    testOwner = new User("owner", "owner@test.com", "password", "Owner", "User", "tenant-123");
  }

  @Test
  @DisplayName("Should create organization successfully")
  void shouldCreateOrganization() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Org",
        "Description",
        "Technology",
        "https://example.com",
        "+1234567890",
        "123 Main St",
        "New York",
        "USA",
        true,
        null
    );

    when(organizationRepository.existsByName(anyString())).thenReturn(false);
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.createOrganization(request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Test Org");
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should throw exception when creating organization with duplicate name")
  void shouldThrowExceptionWhenCreatingDuplicateOrganization() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "Existing Org",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    when(organizationRepository.existsByName(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createOrganization(request, adminUser))
        .isInstanceOf(OrganizationManagementService.OrganizationManagementException.class)
        .hasMessageContaining("Organization name already exists");
  }

  @Test
  @DisplayName("Should create organization with owner")
  void shouldCreateOrganizationWithOwner() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Org",
        "Description",
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        1L
    );

    when(organizationRepository.existsByName(anyString())).thenReturn(false);
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testOwner));
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.createOrganization(request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository).findById(1L);
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should retrieve organization by ID")
  void shouldGetOrganizationById() {
    // Arrange
    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));

    // Act
    var result = service.getOrganizationById(1L, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Test Org");
    verify(organizationRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when organization not found")
  void shouldThrowExceptionWhenOrganizationNotFound() {
    // Arrange
    when(organizationRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getOrganizationById(1L, adminUser))
        .isInstanceOf(OrganizationManagementService.OrganizationManagementException.class)
        .hasMessageContaining("Organization not found");
  }

  @Test
  @DisplayName("Should update organization successfully")
  void shouldUpdateOrganization() {
    // Arrange
    var request = new UpdateOrganizationRequest(
        "Updated Org",
        "Updated Description",
        "Finance",
        "https://updated.com",
        "+9876543210",
        "456 New St",
        "Los Angeles",
        "USA",
        true,
        null
    );

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(organizationRepository.existsByName(anyString())).thenReturn(false);
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.updateOrganization(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should throw exception when updating to duplicate name")
  void shouldThrowExceptionWhenUpdatingToDuplicateName() {
    // Arrange
    var request = new UpdateOrganizationRequest(
        "Existing Org",
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

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(organizationRepository.existsByName(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.updateOrganization(1L, request, adminUser))
        .isInstanceOf(OrganizationManagementService.OrganizationManagementException.class)
        .hasMessageContaining("Organization name already exists");
  }

  @Test
  @DisplayName("Should list all organizations")
  void shouldGetAllOrganizations() {
    // Arrange
    var pageable = PageRequest.of(0, 20);
    when(organizationRepository.findAll()).thenReturn(List.of(testOrganization));

    // Act
    var result = service.getAllOrganizations(pageable, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(organizationRepository).findAll();
  }

  @Test
  @DisplayName("Should delete organization successfully")
  void shouldDeleteOrganization() {
    // Arrange
    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));

    // Act
    service.deleteOrganization(1L, adminUser);

    // Assert
    verify(organizationRepository).delete(testOrganization);
  }

  @Test
  @DisplayName("Should delete organization with owner")
  void shouldDeleteOrganizationWithOwner() {
    // Arrange
    testOrganization.setOwner(testOwner);
    testOwner.setOrganization(testOrganization);
    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));

    // Act
    service.deleteOrganization(1L, adminUser);

    // Assert
    verify(organizationRepository).delete(testOrganization);
  }

  @Test
  @DisplayName("Should deny access to non-admin user for create operation")
  void shouldDenyAccessToNonAdminForCreate() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Org",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    // Act & Assert
    assertThatThrownBy(() -> service.createOrganization(request, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for read operation")
  void shouldDenyAccessToNonAdminForRead() {
    // Act & Assert
    assertThatThrownBy(() -> service.getOrganizationById(1L, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for update operation")
  void shouldDenyAccessToNonAdminForUpdate() {
    // Arrange
    var request = new UpdateOrganizationRequest(
        "Updated Org",
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
    assertThatThrownBy(() -> service.updateOrganization(1L, request, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for delete operation")
  void shouldDenyAccessToNonAdminForDelete() {
    // Act & Assert
    assertThatThrownBy(() -> service.deleteOrganization(1L, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for list operation")
  void shouldDenyAccessToNonAdminForList() {
    // Arrange
    var pageable = PageRequest.of(0, 20);

    // Act & Assert
    assertThatThrownBy(() -> service.getAllOrganizations(pageable, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }
}
