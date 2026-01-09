package com.iqscaffold.billingservice.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantOnboardingServiceImplTest {

  @Mock
  private MerchantStripeConfigRepository repository;

  @Mock
  private PaymentProviderAdapter paymentProvider;

  @Mock
  private EmailService emailService;

  private MerchantOnboardingServiceImpl service;
  private MockedStatic<SecurityContextHelper> securityContextMock;

  @BeforeEach
  void setUp() {
    service = new MerchantOnboardingServiceImpl(repository, paymentProvider, emailService);
    securityContextMock = Mockito.mockStatic(SecurityContextHelper.class);
  }

  @AfterEach
  void tearDown() {
    securityContextMock.close();
  }

  @Test
  void initiateOnboarding_shouldCreateNewAccountAndConfig() {
    // Given
    String tenantId = "tenant_123";
    String accountId = "acct_123";
    String accountLink = "https://connect.stripe.com/setup/123";
    UserContext user = new UserContext(1L, "john", "john@example.com", 
        java.util.Set.of("ROLE_ADMIN"), tenantId, null, "John", "Doe");

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    securityContextMock.when(SecurityContextHelper::getCurrentUserContextOrThrow).thenReturn(user);

    when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
    when(paymentProvider.createConnectAccount()).thenReturn(accountId);
    when(paymentProvider.createAccountLink(accountId, "refresh", "return")).thenReturn(accountLink);
    when(repository.save(any(MerchantStripeConfig.class))).thenAnswer(i -> i.getArgument(0));
    doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap(), any());

    // When
    String result = service.initiateOnboarding("refresh", "return");

    // Then
    assertEquals(accountLink, result);
    verify(paymentProvider).createConnectAccount();
    verify(paymentProvider).createAccountLink(accountId, "refresh", "return");
    verify(repository).save(any(MerchantStripeConfig.class));
    verify(emailService).sendEmail(eq("john@example.com"), anyString(), eq("merchant-onboarding"), anyMap(), any());
  }

  @Test
  void initiateOnboarding_shouldReuseExistingAccountIfNotFullyOnboarded() {
    // Given
    String tenantId = "tenant_123";
    String accountId = "acct_existing";
    String accountLink = "https://connect.stripe.com/setup/456";
    UserContext user = new UserContext(2L, "jane", "jane@example.com", 
        java.util.Set.of("ROLE_ADMIN"), tenantId, null, "Jane", "Smith");

    MerchantStripeConfig existingConfig = new MerchantStripeConfig();
    existingConfig.setTenantId(tenantId);
    existingConfig.setStripeAccountId(accountId);
    existingConfig.setChargesEnabled(false);
    existingConfig.setPayoutsEnabled(false);

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    securityContextMock.when(SecurityContextHelper::getCurrentUserContextOrThrow).thenReturn(user);

    when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(existingConfig));
    when(paymentProvider.createAccountLink(accountId, "refresh", "return")).thenReturn(accountLink);
    doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap(), any());

    // When
    String result = service.initiateOnboarding("refresh", "return");

    // Then
    assertEquals(accountLink, result);
    verify(paymentProvider, never()).createConnectAccount();
    verify(paymentProvider).createAccountLink(accountId, "refresh", "return");
    verify(repository, never()).save(any());
  }

  @Test
  void initiateOnboarding_shouldThrowExceptionIfAlreadyFullyOnboarded() {
    // Given
    String tenantId = "tenant_123";
    UserContext user = new UserContext(3L, "admin", "admin@example.com", 
        java.util.Set.of("ROLE_ADMIN"), tenantId, null, "Admin", "User");

    MerchantStripeConfig existingConfig = new MerchantStripeConfig();
    existingConfig.setTenantId(tenantId);
    existingConfig.setStripeAccountId("acct_123");
    existingConfig.setChargesEnabled(true);
    existingConfig.setPayoutsEnabled(true);

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    securityContextMock.when(SecurityContextHelper::getCurrentUserContextOrThrow).thenReturn(user);

    when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(existingConfig));

    // When & Then
    assertThrows(IllegalStateException.class, () ->
        service.initiateOnboarding("refresh", "return")
    );
    verify(paymentProvider, never()).createConnectAccount();
    verify(paymentProvider, never()).createAccountLink(anyString(), anyString(), anyString());
  }

  @Test
  void initiateOnboarding_shouldHandleUserWithoutEmail() {
    // Given
    String tenantId = "tenant_123";
    String accountId = "acct_123";
    String accountLink = "https://connect.stripe.com/setup/123";
    UserContext user = new UserContext(4L, "user", null, 
        java.util.Set.of("ROLE_ADMIN"), tenantId, null, "John", "Doe");

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    securityContextMock.when(SecurityContextHelper::getCurrentUserContextOrThrow).thenReturn(user);

    when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
    when(paymentProvider.createConnectAccount()).thenReturn(accountId);
    when(paymentProvider.createAccountLink(accountId, "refresh", "return")).thenReturn(accountLink);
    when(repository.save(any(MerchantStripeConfig.class))).thenAnswer(i -> i.getArgument(0));

    // When
    String result = service.initiateOnboarding("refresh", "return");

    // Then
    assertEquals(accountLink, result);
    verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap(), any());
  }

  @Test
  void initiateOnboarding_shouldHandleUserWithoutFirstName() {
    // Given
    String tenantId = "tenant_123";
    String accountId = "acct_123";
    String accountLink = "https://connect.stripe.com/setup/123";
    UserContext user = new UserContext(5L, "user", "user@example.com", 
        java.util.Set.of("ROLE_ADMIN"), tenantId, null, null, "Doe");

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    securityContextMock.when(SecurityContextHelper::getCurrentUserContextOrThrow).thenReturn(user);

    when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
    when(paymentProvider.createConnectAccount()).thenReturn(accountId);
    when(paymentProvider.createAccountLink(accountId, "refresh", "return")).thenReturn(accountLink);
    when(repository.save(any(MerchantStripeConfig.class))).thenAnswer(i -> i.getArgument(0));
    doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap(), any());

    // When
    String result = service.initiateOnboarding("refresh", "return");

    // Then
    assertEquals(accountLink, result);
    verify(emailService).sendEmail(eq("user@example.com"), anyString(), eq("merchant-onboarding"), anyMap(), any());
  }

  @Test
  void getMerchantStatus_shouldReturnConfigWhenExists() {
    // Given
    String tenantId = "tenant_123";
    MerchantStripeConfig config = new MerchantStripeConfig();
    config.setTenantId(tenantId);
    config.setStripeAccountId("acct_123");

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

    // When
    Optional<MerchantStripeConfig> result = service.getMerchantStatus();

    // Then
    assertTrue(result.isPresent());
    assertEquals(config, result.get());
  }

  @Test
  void getMerchantStatus_shouldReturnEmptyWhenNotExists() {
    // Given
    String tenantId = "tenant_123";

    securityContextMock.when(SecurityContextHelper::getCurrentTenantId).thenReturn(tenantId);
    when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());

    // When
    Optional<MerchantStripeConfig> result = service.getMerchantStatus();

    // Then
    assertTrue(result.isEmpty());
  }
}
