package com.iqscaffold.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.shared.PaymentGatewayProvider;
import com.iqscaffold.userservice.usermanagement.UserContext;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for OrganizationManagementService with payment gateway abstraction.
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
  private UserContext superAdminUser;
  private UserContext regularUser;
  private Organization testOrganization;

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

    superAdminUser = new UserContext(
        2L,
        "superadmin",
        "superadmin@test.com",
        Set.of("SUPER_ADMIN"),
        Set.of(),
        "Super",
        "Admin",
        "tenant-system",
        null,
        Map.of()
    );

    regularUser = new UserContext(
        3L,
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

    testOrganization = new Organization("Test Organization", "tenant-123");
    testOrganization.setDescription("Test Description");
    testOrganization.setIndustry("Technology");
    testOrganization.setWebsite("https://test.com");
    testOrganization.setPhone("+1-555-0100");
    testOrganization.setAddress("123 Main St");
    testOrganization.setCity("San Francisco");
    testOrganization.setCountry("USA");
    testOrganization.setEnabled(true);
    testOrganization.setBillingEmail("billing@test.com");
    testOrganization.setPaymentGatewayAccountId("acct_test123");
    testOrganization.setPaymentGatewayProvider(PaymentGatewayProvider.STRIPE);
    testOrganization.setChargesEnabled(true);
    testOrganization.setPayoutsEnabled(true);
    testOrganization.setSubscriptionStatus("active");
    testOrganization.setSubscriptionPlan("pro");
    testOrganization.setMaxUsers(100);
    testOrganization.setOwnerUserId(1L);
  }

  @Test
  @DisplayName("Should create organization successfully as super admin")
  void shouldCreateOrganization() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Organization",
        "tenant-456",
        "New Description",
        "Technology",
        "https://new.com",
        "+1-555-0200",
        "456 Oak St",
        "New York",
        "USA",
        1L,
        "billing@new.com",
        "acct_new123",
        PaymentGatewayProvider.STRIPE,
        "active",
        "enterprise",
        200
    );

    when(organizationRepository.existsByTenantId(anyString())).thenReturn(false);
    when(userRepository.existsById(anyLong())).thenReturn(true);
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.createOrganization(request, superAdminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Test Organization");
    verify(organizationRepository).existsByTenantId("tenant-456");
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should throw exception when creating organization with duplicate tenant")
  void shouldThrowExceptionWhenCreatingDuplicateTenant() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Organization",
        "tenant-123",
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

    when(organizationRepository.existsByTenantId(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createOrganization(request, superAdminUser))
        .isInstanceOf(OrganizationManagementService.OrganizationManagementException.class)
        .hasMessageContaining("Organization already exists for tenant");
  }

  @Test
  @DisplayName("Should deny organization creation for non-super admin")
  void shouldDenyOrganizationCreationForNonSuperAdmin() {
    // Arrange
    var request = new CreateOrganizationRequest(
        "New Organization",
        "tenant-456",
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
    assertThatThrownBy(() -> service.createOrganization(request, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("SUPER_ADMIN role required");
  }

  @Test
  @DisplayName("Should get organization by ID as admin")
  void shouldGetOrganizationById() throws Exception {
    // Arrange
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(testOrganization, 1L);

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));

    // Act
    var result = service.getOrganizationById(1L, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Test Organization");
    assertThat(result.tenantId()).isEqualTo("tenant-123");
    assertThat(result.paymentGatewayProvider()).isEqualTo(PaymentGatewayProvider.STRIPE);
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
  @DisplayName("Should deny access to organization from different tenant")
  void shouldDenyAccessToOrganizationFromDifferentTenant() throws Exception {
    // Arrange
    var otherOrg = new Organization("Other Organization", "tenant-999");
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(otherOrg, 2L);

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(otherOrg));

    // Act & Assert
    assertThatThrownBy(() -> service.getOrganizationById(2L, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Access denied to organization");
  }

  @Test
  @DisplayName("Should allow super admin to access any organization")
  void shouldAllowSuperAdminToAccessAnyOrganization() throws Exception {
    // Arrange
    var otherOrg = new Organization("Other Organization", "tenant-999");
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(otherOrg, 2L);

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(otherOrg));

    // Act
    var result = service.getOrganizationById(2L, superAdminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantId()).isEqualTo("tenant-999");
  }

  @Test
  @DisplayName("Should get organization for current tenant")
  void shouldGetOrganizationForCurrentTenant() {
    // Arrange
    when(organizationRepository.findByTenantId(anyString())).thenReturn(Optional.of(testOrganization));

    // Act
    var result = service.getOrganizationForCurrentTenant(adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantId()).isEqualTo("tenant-123");
    verify(organizationRepository).findByTenantId("tenant-123");
  }

  @Test
  @DisplayName("Should get organization by tenant ID")
  void shouldGetOrganizationByTenantId() {
    // Arrange
    when(organizationRepository.findByTenantId(anyString())).thenReturn(Optional.of(testOrganization));

    // Act
    var result = service.getOrganizationByTenantId("tenant-123");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantId()).isEqualTo("tenant-123");
    verify(organizationRepository).findByTenantId("tenant-123");
  }

  @Test
  @DisplayName("Should update organization successfully")
  void shouldUpdateOrganization() throws Exception {
    // Arrange
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(testOrganization, 1L);

    var request = new UpdateOrganizationRequest(
        "Updated Organization",
        "Updated Description",
        "Finance",
        "https://updated.com",
        "+1-555-0300",
        "789 Pine St",
        "Boston",
        "USA",
        true,
        "billing@updated.com",
        "acct_updated123",
        PaymentGatewayProvider.STRIPE,
        true,
        true,
        "active",
        "enterprise",
        150
    );

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.updateOrganization(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should update organization with partial data")
  void shouldUpdateOrganizationWithPartialData() throws Exception {
    // Arrange
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(testOrganization, 1L);

    var request = new UpdateOrganizationRequest(
        "Updated Name",
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

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));
    when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

    // Act
    var result = service.updateOrganization(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(organizationRepository).save(any(Organization.class));
  }

  @Test
  @DisplayName("Should deny update to organization from different tenant")
  void shouldDenyUpdateToOrganizationFromDifferentTenant() throws Exception {
    // Arrange
    var otherOrg = new Organization("Other Organization", "tenant-999");
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(otherOrg, 2L);

    var request = new UpdateOrganizationRequest(
        "Updated Name",
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

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(otherOrg));

    // Act & Assert
    assertThatThrownBy(() -> service.updateOrganization(2L, request, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Access denied to organization");
  }

  @Test
  @DisplayName("Should delete organization as super admin")
  void shouldDeleteOrganization() throws Exception {
    // Arrange
    var idField = Organization.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(testOrganization, 1L);

    when(organizationRepository.findById(anyLong())).thenReturn(Optional.of(testOrganization));

    // Act
    service.deleteOrganization(1L, superAdminUser);

    // Assert
    verify(organizationRepository).delete(testOrganization);
  }

  @Test
  @DisplayName("Should deny organization deletion for non-super admin")
  void shouldDenyOrganizationDeletionForNonSuperAdmin() {
    // Act & Assert
    assertThatThrownBy(() -> service.deleteOrganization(1L, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("SUPER_ADMIN role required");
  }

  @Test
  @DisplayName("Should list all organizations for super admin")
  void shouldListAllOrganizationsForSuperAdmin() {
    // Arrange
    var pageable = PageRequest.of(0, 20);
    var page = new PageImpl<>(List.of(testOrganization), pageable, 1);
    when(organizationRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

    // Act
    var result = service.getAllOrganizations(pageable, superAdminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(organizationRepository).findAll(any(org.springframework.data.domain.Pageable.class));
  }

  @Test
  @DisplayName("Should list only own organization for admin")
  void shouldListOnlyOwnOrganizationForAdmin() {
    // Arrange
    var pageable = PageRequest.of(0, 20);
    when(organizationRepository.findByTenantId(anyString())).thenReturn(Optional.of(testOrganization));

    // Act
    var result = service.getAllOrganizations(pageable, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).tenantId()).isEqualTo("tenant-123");
    verify(organizationRepository).findByTenantId("tenant-123");
  }

  @Test
  @DisplayName("Should deny list organizations for non-admin")
  void shouldDenyListOrganizationsForNonAdmin() {
    // Arrange
    var pageable = PageRequest.of(0, 20);

    // Act & Assert
    assertThatThrownBy(() -> service.getAllOrganizations(pageable, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny get organization for non-admin")
  void shouldDenyGetOrganizationForNonAdmin() {
    // Act & Assert
    assertThatThrownBy(() -> service.getOrganizationById(1L, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny update organization for non-admin")
  void shouldDenyUpdateOrganizationForNonAdmin() {
    // Arrange
    var request = new UpdateOrganizationRequest(
        "Updated Name",
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

    // Act & Assert
    assertThatThrownBy(() -> service.updateOrganization(1L, request, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }
}
