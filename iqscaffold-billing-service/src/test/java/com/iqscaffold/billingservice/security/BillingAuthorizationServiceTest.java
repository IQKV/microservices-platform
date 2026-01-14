package com.iqscaffold.billingservice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.SubscriptionInvoiceRepository;
import com.iqscaffold.billingservice.subscription.SubscriptionNotFoundException;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BillingAuthorizationServiceTest {

  @Mock
  private TenantSubscriptionRepository subscriptionRepository;

  @Mock
  private SubscriptionInvoiceRepository invoiceRepository;

  private BillingAuthorizationService authorizationService;
  private MockedStatic<SecurityContextHelper> securityContextHelperMock;

  @BeforeEach
  void setUp() {
    authorizationService = new BillingAuthorizationService(subscriptionRepository, invoiceRepository);
    securityContextHelperMock = Mockito.mockStatic(SecurityContextHelper.class);
  }

  @AfterEach
  void tearDown() {
    securityContextHelperMock.close();
  }

  @Test
  void requireSubscriptionCreatePermission_WithBillingAdmin_ShouldPass() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("BILLING_ADMIN"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionCreatePermission());
  }

  @Test
  void requireSubscriptionCreatePermission_WithTenantOwner_ShouldPass() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("TENANT_OWNER"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionCreatePermission());
  }

  @Test
  void requireSubscriptionCreatePermission_WithSuperAdmin_ShouldPass() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("SUPER_ADMIN"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionCreatePermission());
  }

  @Test
  void requireSubscriptionCreatePermission_WithoutPermission_ShouldThrow() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("USER"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(AccessDeniedException.class,
        () -> authorizationService.requireSubscriptionCreatePermission());
  }

  @Test
  void requireSubscriptionManagePermission_WithSuperAdmin_ShouldPass() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("SUPER_ADMIN"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionManagePermission(subscriptionId));
  }

  @Test
  void requireSubscriptionManagePermission_WithBillingAdmin_ShouldPass() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("BILLING_ADMIN"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionManagePermission(subscriptionId));
  }

  @Test
  void requireSubscriptionManagePermission_WithTenantOwner_ShouldPass() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("TENANT_OWNER"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionManagePermission(subscriptionId));
  }

  @Test
  void requireSubscriptionManagePermission_WithoutPermission_ShouldThrow() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("USER"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(AccessDeniedException.class,
        () -> authorizationService.requireSubscriptionManagePermission(subscriptionId));
  }

  @Test
  void requireSubscriptionManagePermission_SubscriptionNotFound_ShouldThrow() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("BILLING_ADMIN"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(false);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(SubscriptionNotFoundException.class,
        () -> authorizationService.requireSubscriptionManagePermission(subscriptionId));
  }

  @Test
  void requireSubscriptionViewPermission_WithFinanceViewer_ShouldPass() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("FINANCE_VIEWER"), "tenant1", 1L, "John", "Doe");

    when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireSubscriptionViewPermission(subscriptionId));
  }

  @Test
  void requirePlanManagePermission_WithSuperAdmin_ShouldPass() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("SUPER_ADMIN"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requirePlanManagePermission());
  }

  @Test
  void requirePlanManagePermission_WithoutPermission_ShouldThrow() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("USER"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(AccessDeniedException.class,
        () -> authorizationService.requirePlanManagePermission());
  }

  @Test
  void requireInvoiceViewPermission_WithBillingAccess_ShouldPass() {
    // Given
    UUID invoiceId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("BILLING_ADMIN"), "tenant1", 1L, "John", "Doe");

    when(invoiceRepository.existsById(invoiceId)).thenReturn(true);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireInvoiceViewPermission(invoiceId));
  }

  @Test
  void requireInvoiceViewPermission_InvoiceNotFound_ShouldThrow() {
    // Given
    UUID invoiceId = UUID.randomUUID();
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("BILLING_ADMIN"), "tenant1", 1L, "John", "Doe");

    when(invoiceRepository.existsById(invoiceId)).thenReturn(false);
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(IllegalArgumentException.class,
        () -> authorizationService.requireInvoiceViewPermission(invoiceId));
  }

  @Test
  void requireInvoiceListPermission_WithBillingAccess_ShouldPass() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("FINANCE_VIEWER"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertDoesNotThrow(() -> authorizationService.requireInvoiceListPermission());
  }

  @Test
  void requireInvoiceListPermission_WithoutPermission_ShouldThrow() {
    // Given
    UserContext userContext = new UserContext(1L, "user", "user@test.com",
        Set.of("USER"), "tenant1", 1L, "John", "Doe");
    securityContextHelperMock.when(SecurityContextHelper::getCurrentUserContextOrThrow)
        .thenReturn(userContext);

    // When & Then
    assertThrows(AccessDeniedException.class,
        () -> authorizationService.requireInvoiceListPermission());
  }
}
