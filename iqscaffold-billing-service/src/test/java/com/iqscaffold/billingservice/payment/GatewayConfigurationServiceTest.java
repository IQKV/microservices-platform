package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.iqscaffold.billingservice.admin.MerchantPaymentConfig;
import com.iqscaffold.billingservice.admin.MerchantPaymentConfigRepository;
import com.iqscaffold.billingservice.admin.dto.OrganizationDto;
import com.iqscaffold.billingservice.infrastructure.client.UserServiceClient;
import com.iqscaffold.billingservice.payment.GatewayConfigurationService.GatewayConfiguration;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GatewayConfigurationServiceTest {

  @Mock
  private MerchantPaymentConfigRepository merchantConfigRepository;

  @Mock
  private UserServiceClient userServiceClient;

  @Mock
  private PaymentProviderFactory providerFactory;

  private GatewayConfigurationService gatewayConfigurationService;

  @BeforeEach
  void setUp() {
    gatewayConfigurationService = new GatewayConfigurationService(
        merchantConfigRepository,
        userServiceClient,
        providerFactory
    );
  }

  @Test
  void resolveGatewayForCurrentTenant_shouldReturnConfiguredGateway() {
    // Given
    String tenantId = "tenant-123";
    Long organizationId = 456L;

    OrganizationDto organization =
        new OrganizationDto(organizationId, "Test Org", tenantId, null, null, null, null, null, true);

    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setOrganizationId(organizationId);
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);
    merchantConfig.setGatewayAccountId("acct_123");
    merchantConfig.setApplicationFeePercent(new BigDecimal("2.5"));
    merchantConfig.setChargesEnabled(true);
    merchantConfig.setPayoutsEnabled(true);

    try (MockedStatic<SecurityContextHelper> mockedHelper = mockStatic(SecurityContextHelper.class)) {
      mockedHelper.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);

      when(userServiceClient.getOrganizationByTenantId(tenantId))
          .thenReturn(Optional.of(organization));
      when(merchantConfigRepository.findByOrganizationId(organizationId))
          .thenReturn(Optional.of(merchantConfig));

      // When
      GatewayConfiguration result = gatewayConfigurationService.resolveGatewayForCurrentTenant();

      // Then
      assertNotNull(result);
      assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
      assertTrue(result.gatewayAccountId().isPresent());
      assertEquals("acct_123", result.gatewayAccountId().get());
      assertTrue(result.applicationFeePercent().isPresent());
      assertEquals(new BigDecimal("2.5"), result.applicationFeePercent().get());
      assertTrue(result.chargesEnabled());
      assertTrue(result.payoutsEnabled());
      assertTrue(result.hasConfiguration());
    }
  }

  @Test
  void resolveGatewayForCurrentTenant_shouldReturnDefaultWhenNoOrganization() {
    // Given
    String tenantId = "tenant-123";

    try (MockedStatic<SecurityContextHelper> mockedHelper = mockStatic(SecurityContextHelper.class)) {
      mockedHelper.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);

      when(userServiceClient.getOrganizationByTenantId(tenantId))
          .thenReturn(Optional.empty());

      // When
      GatewayConfiguration result = gatewayConfigurationService.resolveGatewayForCurrentTenant();

      // Then
      assertNotNull(result);
      assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
      assertFalse(result.gatewayAccountId().isPresent());
      assertFalse(result.applicationFeePercent().isPresent());
      assertFalse(result.chargesEnabled());
      assertFalse(result.payoutsEnabled());
      assertFalse(result.hasConfiguration());
    }
  }

  @Test
  void resolveGatewayForCurrentTenant_shouldReturnDefaultWhenNoMerchantConfig() {
    // Given
    String tenantId = "tenant-123";
    Long organizationId = 456L;

    OrganizationDto organization =
        new OrganizationDto(organizationId, "Test Org", tenantId, null, null, null, null, null, true);

    try (MockedStatic<SecurityContextHelper> mockedHelper = mockStatic(SecurityContextHelper.class)) {
      mockedHelper.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);

      when(userServiceClient.getOrganizationByTenantId(tenantId))
          .thenReturn(Optional.of(organization));
      when(merchantConfigRepository.findByOrganizationId(organizationId))
          .thenReturn(Optional.empty());

      // When
      GatewayConfiguration result = gatewayConfigurationService.resolveGatewayForCurrentTenant();

      // Then
      assertNotNull(result);
      assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
      assertFalse(result.hasConfiguration());
    }
  }

  @Test
  void resolveGatewayForOrganization_shouldReturnConfiguredGateway() {
    // Given
    Long organizationId = 456L;

    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setOrganizationId(organizationId);
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);
    merchantConfig.setGatewayAccountId("acct_456");
    merchantConfig.setChargesEnabled(true);
    merchantConfig.setPayoutsEnabled(false);

    when(merchantConfigRepository.findByOrganizationId(organizationId))
        .thenReturn(Optional.of(merchantConfig));

    // When
    GatewayConfiguration result = gatewayConfigurationService.resolveGatewayForOrganization(organizationId);

    // Then
    assertNotNull(result);
    assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
    assertTrue(result.gatewayAccountId().isPresent());
    assertEquals("acct_456", result.gatewayAccountId().get());
    assertTrue(result.chargesEnabled());
    assertFalse(result.payoutsEnabled());
    assertTrue(result.hasConfiguration());
  }

  @Test
  void resolveGatewayForOrganization_shouldReturnDefaultWhenNoConfig() {
    // Given
    Long organizationId = 456L;

    when(merchantConfigRepository.findByOrganizationId(organizationId))
        .thenReturn(Optional.empty());

    // When
    GatewayConfiguration result = gatewayConfigurationService.resolveGatewayForOrganization(organizationId);

    // Then
    assertNotNull(result);
    assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
    assertFalse(result.hasConfiguration());
  }

  @Test
  void getProviderForCurrentTenant_shouldReturnCorrectProvider() {
    // Given
    String tenantId = "tenant-123";
    Long organizationId = 456L;

    OrganizationDto organization =
        new OrganizationDto(organizationId, "Test Org", tenantId, null, null, null, null, null, true);

    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);

    PaymentProviderAdapter mockProvider = mock(PaymentProviderAdapter.class);

    try (MockedStatic<SecurityContextHelper> mockedHelper = mockStatic(SecurityContextHelper.class)) {
      mockedHelper.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);

      when(userServiceClient.getOrganizationByTenantId(tenantId))
          .thenReturn(Optional.of(organization));
      when(merchantConfigRepository.findByOrganizationId(organizationId))
          .thenReturn(Optional.of(merchantConfig));
      when(providerFactory.getProvider(PaymentGatewayProvider.STRIPE))
          .thenReturn(mockProvider);

      // When
      PaymentProviderAdapter result = gatewayConfigurationService.getProviderForCurrentTenant();

      // Then
      assertNotNull(result);
      assertEquals(mockProvider, result);
      verify(providerFactory).getProvider(PaymentGatewayProvider.STRIPE);
    }
  }

  @Test
  void getProviderForOrganization_shouldReturnCorrectProvider() {
    // Given
    Long organizationId = 456L;

    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);

    PaymentProviderAdapter mockProvider = mock(PaymentProviderAdapter.class);

    when(merchantConfigRepository.findByOrganizationId(organizationId))
        .thenReturn(Optional.of(merchantConfig));
    when(providerFactory.getProvider(PaymentGatewayProvider.STRIPE))
        .thenReturn(mockProvider);

    // When
    PaymentProviderAdapter result = gatewayConfigurationService.getProviderForOrganization(organizationId);

    // Then
    assertNotNull(result);
    assertEquals(mockProvider, result);
  }

  @Test
  void isProviderSupported_shouldReturnTrueForSupportedProvider() {
    // Given
    when(providerFactory.isProviderSupported(PaymentGatewayProvider.STRIPE))
        .thenReturn(true);

    // When
    boolean result = gatewayConfigurationService.isProviderSupported(PaymentGatewayProvider.STRIPE);

    // Then
    assertTrue(result);
  }

  @Test
  void isProviderSupported_shouldReturnFalseForUnsupportedProvider() {
    // Given
    when(providerFactory.isProviderSupported(any()))
        .thenReturn(false);

    // When
    boolean result = gatewayConfigurationService.isProviderSupported(PaymentGatewayProvider.STRIPE);

    // Then
    assertFalse(result);
  }

  @Test
  void getDefaultGateway_shouldReturnStripe() {
    // When
    PaymentGatewayProvider result = gatewayConfigurationService.getDefaultGateway();

    // Then
    assertEquals(PaymentGatewayProvider.STRIPE, result);
  }

  @Test
  void gatewayConfiguration_defaultConfig_shouldCreateCorrectConfig() {
    // When
    GatewayConfiguration result = GatewayConfiguration.defaultConfig(PaymentGatewayProvider.STRIPE);

    // Then
    assertNotNull(result);
    assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
    assertFalse(result.gatewayAccountId().isPresent());
    assertFalse(result.applicationFeePercent().isPresent());
    assertFalse(result.chargesEnabled());
    assertFalse(result.payoutsEnabled());
    assertFalse(result.hasConfiguration());
  }

  @Test
  void gatewayConfiguration_fromMerchantConfig_shouldCreateCorrectConfig() {
    // Given
    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);
    merchantConfig.setGatewayAccountId("acct_789");
    merchantConfig.setApplicationFeePercent(new BigDecimal("3.0"));
    merchantConfig.setChargesEnabled(true);
    merchantConfig.setPayoutsEnabled(true);

    // When
    GatewayConfiguration result = GatewayConfiguration.fromMerchantConfig(merchantConfig);

    // Then
    assertNotNull(result);
    assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
    assertTrue(result.gatewayAccountId().isPresent());
    assertEquals("acct_789", result.gatewayAccountId().get());
    assertTrue(result.applicationFeePercent().isPresent());
    assertEquals(new BigDecimal("3.0"), result.applicationFeePercent().get());
    assertTrue(result.chargesEnabled());
    assertTrue(result.payoutsEnabled());
    assertTrue(result.hasConfiguration());
  }

  @Test
  void gatewayConfiguration_fromMerchantConfig_shouldHandleNullValues() {
    // Given
    MerchantPaymentConfig merchantConfig = new MerchantPaymentConfig();
    merchantConfig.setGatewayProvider(PaymentGatewayProvider.STRIPE);
    merchantConfig.setGatewayAccountId(null);
    merchantConfig.setApplicationFeePercent(null);
    merchantConfig.setChargesEnabled(false);
    merchantConfig.setPayoutsEnabled(false);

    // When
    GatewayConfiguration result = GatewayConfiguration.fromMerchantConfig(merchantConfig);

    // Then
    assertNotNull(result);
    assertEquals(PaymentGatewayProvider.STRIPE, result.provider());
    assertFalse(result.gatewayAccountId().isPresent());
    assertFalse(result.applicationFeePercent().isPresent());
    assertFalse(result.chargesEnabled());
    assertFalse(result.payoutsEnabled());
    assertTrue(result.hasConfiguration());
  }
}
