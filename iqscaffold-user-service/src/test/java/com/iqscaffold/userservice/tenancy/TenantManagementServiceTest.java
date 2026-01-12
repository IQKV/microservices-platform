package com.iqscaffold.userservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.CreateTenantRequest;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.UpdateTenantRequest;
import com.iqscaffold.userservice.shared.exception.TenantManagementException;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for TenantManagementService.
 */
@ExtendWith(MockitoExtension.class)
class TenantManagementServiceTest {

  @Mock
  private TenantRepository tenantRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SchemaNameResolver schemaNameResolver;

  @Mock
  private TenantLiquibaseRunner liquibaseRunner;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private TenantManagementService service;

  private Tenant testTenant;

  @BeforeEach
  void setUp() {
    TenantContext.clear();
    testTenant = new Tenant("tenant-123", "Test Tenant", "Test Description", "admin");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should create tenant successfully")
  void shouldCreateTenant() {
    // Arrange
    var request = new CreateTenantRequest(
        "tenant-123",
        "Test Tenant",
        "Test Description",
        "test.com",
        100,
        50,
        1000
    );

    when(tenantRepository.existsByTenantId(anyString())).thenReturn(false);
    when(tenantRepository.existsByDomain(anyString())).thenReturn(false);
    when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);
    when(schemaNameResolver.toSchema(anyString())).thenReturn("tenant_123");

    // Act
    var result = service.createTenant(request, "admin");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantId()).isEqualTo("tenant-123");
    verify(tenantRepository).save(any(Tenant.class));
    verify(jdbcTemplate).execute(anyString());
  }

  @Test
  @DisplayName("Should throw exception when creating duplicate tenant")
  void shouldThrowExceptionWhenCreatingDuplicateTenant() {
    // Arrange
    var request = new CreateTenantRequest(
        "tenant-123",
        "Test Tenant",
        "Test Description",
        null,
        null,
        null,
        null
    );

    when(tenantRepository.existsByTenantId(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createTenant(request, "admin"))
        .isInstanceOf(TenantManagementException.TenantAlreadyExistsException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  @DisplayName("Should throw exception when creating tenant with duplicate domain")
  void shouldThrowExceptionWhenCreatingTenantWithDuplicateDomain() {
    // Arrange
    var request = new CreateTenantRequest(
        "tenant-123",
        "Test Tenant",
        "Test Description",
        "existing.com",
        null,
        null,
        null
    );

    when(tenantRepository.existsByTenantId(anyString())).thenReturn(false);
    when(tenantRepository.existsByDomain(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createTenant(request, "admin"))
        .isInstanceOf(TenantManagementException.DomainAlreadyExistsException.class)
        .hasMessageContaining("domain");
  }

  @Test
  @DisplayName("Should update tenant successfully")
  void shouldUpdateTenant() {
    // Arrange
    var request = new UpdateTenantRequest(
        "Updated Tenant",
        "Updated Description",
        "updated.com",
        200,
        100,
        2000
    );

    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));
    when(tenantRepository.existsByDomain(anyString())).thenReturn(false);
    when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

    // Act
    var result = service.updateTenant("tenant-123", request);

    // Assert
    assertThat(result).isNotNull();
    verify(tenantRepository).save(any(Tenant.class));
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent tenant")
  void shouldThrowExceptionWhenUpdatingNonExistentTenant() {
    // Arrange
    var request = new UpdateTenantRequest(
        "Updated Tenant",
        null,
        null,
        null,
        null,
        null
    );

    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.updateTenant("tenant-123", request))
        .isInstanceOf(TenantManagementException.TenantNotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  @DisplayName("Should get tenant by ID")
  void shouldGetTenantById() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));

    // Act
    var result = service.getTenant("tenant-123");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantId()).isEqualTo("tenant-123");
    verify(tenantRepository).findByTenantId("tenant-123");
  }

  @Test
  @DisplayName("Should throw exception when tenant not found")
  void shouldThrowExceptionWhenTenantNotFound() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getTenant("tenant-123"))
        .isInstanceOf(TenantManagementException.TenantNotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  @DisplayName("Should get all tenants")
  void shouldGetAllTenants() {
    // Arrange
    when(tenantRepository.findAll()).thenReturn(List.of(testTenant));
    when(userRepository.countByEnabledTrue()).thenReturn(5L);

    // Act
    var result = service.getAllTenants(false);

    // Assert
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(1);
    verify(tenantRepository).findAll();
  }

  @Test
  @DisplayName("Should get only active tenants")
  void shouldGetOnlyActiveTenants() {
    // Arrange
    when(tenantRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(testTenant));
    when(userRepository.countByEnabledTrue()).thenReturn(5L);

    // Act
    var result = service.getAllTenants(true);

    // Assert
    assertThat(result).isNotEmpty();
    verify(tenantRepository).findByStatus(TenantStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should delete tenant successfully")
  void shouldDeleteTenant() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));
    when(userRepository.count()).thenReturn(0L);

    // Act
    service.deleteTenant("tenant-123");

    // Assert
    verify(tenantRepository).delete(testTenant);
  }

  @Test
  @DisplayName("Should throw exception when deleting tenant with users")
  void shouldThrowExceptionWhenDeletingTenantWithUsers() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));
    when(userRepository.count()).thenReturn(5L);

    // Act & Assert
    assertThatThrownBy(() -> service.deleteTenant("tenant-123"))
        .isInstanceOf(TenantManagementException.TenantHasUsersException.class)
        .hasMessageContaining("existing users");
  }

  @Test
  @DisplayName("Should suspend tenant")
  void shouldSuspendTenant() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));
    when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

    // Act
    var result = service.suspendTenant("tenant-123", "Payment overdue", "admin");

    // Assert
    assertThat(result).isNotNull();
    verify(tenantRepository).save(any(Tenant.class));
  }

  @Test
  @DisplayName("Should validate tenant exists and is active")
  void shouldValidateTenantExistsAndIsActive() {
    // Arrange
    testTenant.setStatus(TenantStatus.ACTIVE);
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));

    // Act
    var result = service.isValidTenant("tenant-123");

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false for invalid tenant")
  void shouldReturnFalseForInvalidTenant() {
    // Arrange
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.empty());

    // Act
    var result = service.isValidTenant("tenant-123");

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should return false for suspended tenant")
  void shouldReturnFalseForSuspendedTenant() {
    // Arrange
    testTenant.setStatus(TenantStatus.SUSPENDED);
    when(tenantRepository.findByTenantId(anyString())).thenReturn(Optional.of(testTenant));

    // Act
    var result = service.isValidTenant("tenant-123");

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should find tenant by domain")
  void shouldFindTenantByDomain() {
    // Arrange
    when(tenantRepository.findByDomain(anyString())).thenReturn(Optional.of(testTenant));

    // Act
    var result = service.findTenantByDomain("test.com");

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getTenantId()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should return empty when domain is null")
  void shouldReturnEmptyWhenDomainIsNull() {
    // Act
    var result = service.findTenantByDomain(null);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when domain is empty")
  void shouldReturnEmptyWhenDomainIsEmpty() {
    // Act
    var result = service.findTenantByDomain("");

    // Assert
    assertThat(result).isEmpty();
  }
}
