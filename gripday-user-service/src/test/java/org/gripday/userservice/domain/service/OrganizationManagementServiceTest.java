package org.gripday.userservice.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.gripday.userservice.infrastructure.entity.Organization;
import org.gripday.userservice.infrastructure.entity.User;
import org.gripday.userservice.infrastructure.repository.OrganizationRepository;
import org.gripday.userservice.infrastructure.repository.UserAuditLogRepository;
import org.gripday.userservice.infrastructure.repository.UserRepository;
import org.gripday.userservice.presentation.dto.CreateOrganizationRequest;
import org.gripday.userservice.presentation.dto.UpdateOrganizationRequest;
import org.gripday.userservice.presentation.dto.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrganizationManagementServiceTest {

  @Mock
  private OrganizationRepository organizationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @InjectMocks
  private OrganizationManagementService organizationManagementService;

  private UserContext adminUser;
  private Organization testOrganization;
  private User testUser;

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
    testOrganization.setDescription("Test Description");
    testOrganization.setIndustry("Technology");

    testUser = new User("testuser", "test@test.com", "hash", "Test", "User", "tenant-123");
  }

  @Test
  void getAllOrganizations_Success() {
    var pageable = PageRequest.of(0, 20);
    when(organizationRepository.findByTenantId("tenant-123")).thenReturn(List.of(testOrganization));

    var result = organizationManagementService.getAllOrganizations(pageable, adminUser);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(organizationRepository).findByTenantId("tenant-123");
  }

  @Test
  void getOrganizationById_Success() {
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

    var result = organizationManagementService.getOrganizationById(1L, adminUser);

    assertNotNull(result);
    assertEquals("Test Org", result.name());
    verify(organizationRepository).findById(1L);
  }

  @Test
  void createOrganization_Success() {
    var request = new CreateOrganizationRequest(
        "New Org",
        "Description",
        "Tech",
        "https://example.com",
        "123-456",
        "123 Street",
        "City",
        "Country",
        true,
        null
    );

    when(organizationRepository.existsByName("New Org")).thenReturn(false);
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    var result = organizationManagementService.createOrganization(request, adminUser);

    assertNotNull(result);
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  void createOrganization_WithOwner_Success() {
    var request = new CreateOrganizationRequest(
        "New Org",
        "Description",
        "Tech",
        null,
        null,
        null,
        null,
        null,
        true,
        1L
    );

    when(organizationRepository.existsByName("New Org")).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    var result = organizationManagementService.createOrganization(request, adminUser);

    assertNotNull(result);
    verify(userRepository).findById(1L);
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  void updateOrganization_Success() {
    var request = new UpdateOrganizationRequest(
        "Updated Org",
        "Updated Description",
        "Finance",
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
    when(organizationRepository.existsByName("Updated Org")).thenReturn(false);
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    var result = organizationManagementService.updateOrganization(1L, request, adminUser);

    assertNotNull(result);
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  void deleteOrganization_Success() {
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

    organizationManagementService.deleteOrganization(1L, adminUser);

    verify(organizationRepository).delete(testOrganization);
  }
}
