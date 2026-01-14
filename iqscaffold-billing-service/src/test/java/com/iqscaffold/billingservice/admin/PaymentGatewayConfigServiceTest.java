package com.iqscaffold.billingservice.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.admin.dto.GatewayConfigDtos;
import com.iqscaffold.billingservice.security.GatewayConfigEncryptionService;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayConfigServiceTest {

  @Mock
  private PaymentGatewayConfigRepository repository;

  @Mock
  private GatewayConfigEncryptionService encryptionService;

  private ObjectMapper objectMapper;

  private PaymentGatewayConfigService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new PaymentGatewayConfigService(repository, encryptionService, objectMapper);
  }

  @Test
  void createGatewayConfig_shouldCreateNewConfiguration() {
    // Given
    String tenantId = "tenant-123";
    GatewayConfigDtos.StripeGatewayConfigData configData = new GatewayConfigDtos.StripeGatewayConfigData(
        "sk_test_123", "whsec_456", "ca_123", "pk_test_789"
    );
    GatewayConfigDtos.CreateGatewayConfigRequest request = new GatewayConfigDtos.CreateGatewayConfigRequest(
        PaymentGatewayProvider.STRIPE, configData, "test", true, true, "Test Stripe", "Test config"
    );

    String encryptedConfig = "encrypted-data";
    PaymentGatewayConfig savedConfig = createMockConfig(tenantId);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);

      when(repository.existsByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(false);
      when(encryptionService.encrypt(anyString(), eq(tenantId))).thenReturn(encryptedConfig);
      when(repository.save(any(PaymentGatewayConfig.class))).thenReturn(savedConfig);
      when(encryptionService.decrypt(anyString(), eq(tenantId))).thenReturn("{}");
      when(repository.findOtherPrimaryGateways(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(List.of());

      // When
      GatewayConfigDtos.GatewayConfigResponse response = service.createGatewayConfig(request);

      // Then
      assertNotNull(response);
      assertEquals(savedConfig.getId(), response.id());
      verify(repository).save(any(PaymentGatewayConfig.class));
    }
  }

  @Test
  void createGatewayConfig_shouldThrowExceptionWhenConfigExists() {
    // Given
    String tenantId = "tenant-123";
    GatewayConfigDtos.StripeGatewayConfigData configData = new GatewayConfigDtos.StripeGatewayConfigData(
        "sk_test_123", "whsec_456", null, null
    );
    GatewayConfigDtos.CreateGatewayConfigRequest request = new GatewayConfigDtos.CreateGatewayConfigRequest(
        PaymentGatewayProvider.STRIPE, configData, "test", false, false, null, null
    );

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.existsByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(true);

      // When & Then
      assertThrows(IllegalStateException.class, () -> service.createGatewayConfig(request));
      verify(repository, never()).save(any());
    }
  }

  @Test
  void updateGatewayConfig_shouldUpdateExistingConfiguration() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig existingConfig = createMockConfig(tenantId);
    GatewayConfigDtos.UpdateGatewayConfigRequest request = new GatewayConfigDtos.UpdateGatewayConfigRequest(
        null, "live", true, false, "Updated name", null
    );

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(existingConfig));
      when(repository.save(any(PaymentGatewayConfig.class))).thenReturn(existingConfig);
      when(encryptionService.decrypt(anyString(), eq(tenantId))).thenReturn("{}");

      // When
      GatewayConfigDtos.GatewayConfigResponse response = service.updateGatewayConfig(
          PaymentGatewayProvider.STRIPE, request
      );

      // Then
      assertNotNull(response);
      verify(repository).save(existingConfig);
    }
  }

  @Test
  void getGatewayConfig_shouldReturnConfiguration() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(encryptionService.decrypt(anyString(), eq(tenantId))).thenReturn("{}");

      // When
      GatewayConfigDtos.GatewayConfigResponse response = service.getGatewayConfig(PaymentGatewayProvider.STRIPE);

      // Then
      assertNotNull(response);
      assertEquals(config.getId(), response.id());
    }
  }

  @Test
  void listGatewayConfigs_shouldReturnAllConfigurations() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config1 = createMockConfig(tenantId);
    PaymentGatewayConfig config2 = createMockConfig(tenantId);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantId(tenantId)).thenReturn(List.of(config1, config2));

      // When
      List<GatewayConfigDtos.GatewayConfigSummary> summaries = service.listGatewayConfigs();

      // Then
      assertNotNull(summaries);
      assertEquals(2, summaries.size());
    }
  }

  @Test
  void activateGateway_shouldActivateConfiguration() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(false);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(repository.save(any(PaymentGatewayConfig.class))).thenReturn(config);

      // When
      GatewayConfigDtos.GatewayStatusResponse response = service.activateGateway(PaymentGatewayProvider.STRIPE);

      // Then
      assertNotNull(response);
      assertTrue(response.isActive());
      verify(repository).save(config);
    }
  }

  @Test
  void deactivateGateway_shouldDeactivateConfiguration() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(true);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(repository.countByTenantIdAndIsActive(tenantId, true)).thenReturn(2L);
      when(repository.save(any(PaymentGatewayConfig.class))).thenReturn(config);

      // When
      GatewayConfigDtos.GatewayStatusResponse response = service.deactivateGateway(PaymentGatewayProvider.STRIPE);

      // Then
      assertNotNull(response);
      assertFalse(response.isActive());
      verify(repository).save(config);
    }
  }

  @Test
  void deactivateGateway_shouldThrowExceptionWhenOnlyActiveGateway() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(true);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(repository.countByTenantIdAndIsActive(tenantId, true)).thenReturn(1L);

      // When & Then
      assertThrows(IllegalStateException.class, () -> service.deactivateGateway(PaymentGatewayProvider.STRIPE));
      verify(repository, never()).save(any());
    }
  }

  @Test
  void setPrimaryGateway_shouldSetAsPrimary() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(true);
    config.setPrimary(false);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(repository.findOtherPrimaryGateways(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(List.of());
      when(repository.save(any(PaymentGatewayConfig.class))).thenReturn(config);

      // When
      GatewayConfigDtos.GatewayStatusResponse response = service.setPrimaryGateway(PaymentGatewayProvider.STRIPE);

      // Then
      assertNotNull(response);
      assertTrue(response.isPrimary());
      verify(repository).save(config);
    }
  }

  @Test
  void setPrimaryGateway_shouldThrowExceptionWhenInactive() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(false);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));

      // When & Then
      assertThrows(IllegalStateException.class, () -> service.setPrimaryGateway(PaymentGatewayProvider.STRIPE));
      verify(repository, never()).save(any());
    }
  }

  @Test
  void deleteGatewayConfig_shouldDeleteConfiguration() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(false);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));

      // When
      service.deleteGatewayConfig(PaymentGatewayProvider.STRIPE);

      // Then
      verify(repository).delete(config);
    }
  }

  @Test
  void deleteGatewayConfig_shouldThrowExceptionWhenOnlyActiveGateway() {
    // Given
    String tenantId = "tenant-123";
    PaymentGatewayConfig config = createMockConfig(tenantId);
    config.setActive(true);

    try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
      mockedStatic.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
      when(repository.findByTenantIdAndGatewayProvider(tenantId, PaymentGatewayProvider.STRIPE))
          .thenReturn(Optional.of(config));
      when(repository.countByTenantIdAndIsActive(tenantId, true)).thenReturn(1L);

      // When & Then
      assertThrows(IllegalStateException.class, () -> service.deleteGatewayConfig(PaymentGatewayProvider.STRIPE));
      verify(repository, never()).delete(any());
    }
  }

  private PaymentGatewayConfig createMockConfig(String tenantId) {
    PaymentGatewayConfig config = new PaymentGatewayConfig();
    config.setId(UUID.randomUUID());
    config.setTenantId(tenantId);
    config.setGatewayProvider(PaymentGatewayProvider.STRIPE);
    config.setActive(true);
    config.setPrimary(false);
    config.setMode("test");
    config.setConfigData("encrypted-config-data");
    config.setDisplayName("Test Gateway");
    config.setDescription("Test Description");
    config.setCreatedAt(Instant.now());
    config.setUpdatedAt(Instant.now());
    return config;
  }
}
