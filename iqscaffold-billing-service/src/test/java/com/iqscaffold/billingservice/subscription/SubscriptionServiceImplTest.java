package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.payment.GatewayConfigurationService;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import com.iqscaffold.billingservice.subscription.event.SubscriptionEvent;
import com.iqscaffold.billingservice.subscription.event.SubscriptionEventPublisher;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

  @Mock
  private TenantSubscriptionRepository subscriptionRepository;

  @Mock
  private SubscriptionPlanRepository planRepository;

  @Mock
  private TenantSubscriptionAuditTrailRepository auditTrailRepository;

  @Mock
  private GatewayConfigurationService gatewayConfigService;

  @Mock
  private SubscriptionStateMachine stateMachine;

  @Mock
  private SubscriptionEventPublisher eventPublisher;

  @Mock
  private PaymentProviderAdapter paymentProvider;

  private SubscriptionServiceImpl subscriptionService;
  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    subscriptionService = new SubscriptionServiceImpl(
        subscriptionRepository,
        planRepository,
        auditTrailRepository,
        gatewayConfigService,
        stateMachine,
        eventPublisher
    );

    // Mock TenantContext
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::hasTenantContext).thenReturn(true);
    tenantContextMock.when(TenantContext::getCurrentTenantId).thenReturn("tenant-123-456-789");

    // Setup common mocks
    lenient().when(gatewayConfigService.getProviderForCurrentTenant()).thenReturn(paymentProvider);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  void createSubscription_shouldCreateSubscriptionSuccessfully() throws Exception {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionDtos.CreateSubscriptionRequest request = new SubscriptionDtos.CreateSubscriptionRequest(
        planId,
        "pm_123",
        14,
        Map.of("source", "web")
    );

    SubscriptionPlan plan = createTestPlan(planId);
    TenantSubscription savedSubscription = createTestSubscription(plan);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(subscriptionRepository.findActive()).thenReturn(Optional.empty());
    when(paymentProvider.createSubscription(anyString(), anyString(), any(Integer.class),
        anyMap(), anyString())).thenReturn("sub_123");
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(savedSubscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.createSubscription(request);

    // Then
    assertNotNull(response);
    assertEquals(savedSubscription.getId(), response.id());
    assertEquals("tenant-123-456-789", response.tenantId());
    assertEquals(planId, response.planId());
    assertEquals("ACTIVE", response.status());

    verify(stateMachine).validateTransition(null, SubscriptionStatus.INCOMPLETE);
    verify(paymentProvider).createSubscription(anyString(), eq(plan.getStripePriceId()),
        eq(14), anyMap(), anyString());
    verify(subscriptionRepository).save(any(TenantSubscription.class));
    verify(auditTrailRepository).save(any(TenantSubscriptionAuditTrail.class));
    verify(eventPublisher).publishSubscriptionCreated(any(SubscriptionEvent.class));
  }

  @Test
  void createSubscription_shouldThrowExceptionWhenNoTenantContext() {
    // Given
    tenantContextMock.when(TenantContext::hasTenantContext).thenReturn(false);

    SubscriptionDtos.CreateSubscriptionRequest request = new SubscriptionDtos.CreateSubscriptionRequest(
        UUID.randomUUID(), null, null, null
    );

    // When & Then
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> subscriptionService.createSubscription(request)
    );

    assertEquals("Tenant context is required", exception.getMessage());
  }

  @Test
  void createSubscription_shouldThrowExceptionWhenPlanNotFound() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionDtos.CreateSubscriptionRequest request = new SubscriptionDtos.CreateSubscriptionRequest(
        planId, null, null, null
    );

    when(planRepository.findById(planId)).thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> subscriptionService.createSubscription(request)
    );

    assertEquals("Subscription plan not found: " + planId, exception.getMessage());
  }

  @Test
  void createSubscription_shouldThrowExceptionWhenPlanNotActive() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan inactivePlan = createTestPlan(planId);
    inactivePlan.setIsActive(false);

    SubscriptionDtos.CreateSubscriptionRequest request = new SubscriptionDtos.CreateSubscriptionRequest(
        planId, null, null, null
    );

    when(planRepository.findById(planId)).thenReturn(Optional.of(inactivePlan));

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> subscriptionService.createSubscription(request)
    );

    assertEquals("Subscription plan is not active: " + planId, exception.getMessage());
  }

  @Test
  void createSubscription_shouldThrowExceptionWhenActiveSubscriptionExists() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan(planId);
    TenantSubscription existingSubscription = createTestSubscription(plan);

    SubscriptionDtos.CreateSubscriptionRequest request = new SubscriptionDtos.CreateSubscriptionRequest(
        planId, null, null, null
    );

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(subscriptionRepository.findActive()).thenReturn(Optional.of(existingSubscription));

    // When & Then
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> subscriptionService.createSubscription(request)
    );

    assertEquals("Tenant already has an active subscription", exception.getMessage());
  }

  @Test
  void getSubscription_shouldReturnSubscriptionSuccessfully() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.getSubscription(subscriptionId);

    // Then
    assertNotNull(response);
    assertEquals(subscriptionId, response.id());
    assertEquals("tenant-123-456-789", response.tenantId());
  }

  @Test
  void getSubscription_shouldThrowExceptionWhenNotFound() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> subscriptionService.getSubscription(subscriptionId)
    );

    assertEquals("Subscription not found: " + subscriptionId, exception.getMessage());
  }

  @Test
  void getActiveSubscription_shouldReturnActiveSubscription() {
    // Given
    TenantSubscription activeSubscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    when(subscriptionRepository.findActive()).thenReturn(Optional.of(activeSubscription));

    // When
    Optional<SubscriptionDtos.SubscriptionResponse> response = subscriptionService.getActiveSubscription();

    // Then
    assertTrue(response.isPresent());
    assertEquals("tenant-123-456-789", response.get().tenantId());
  }

  @Test
  void getActiveSubscription_shouldReturnEmptyWhenNoActiveSubscription() {
    // Given
    when(subscriptionRepository.findActive()).thenReturn(Optional.empty());

    // When
    Optional<SubscriptionDtos.SubscriptionResponse> response = subscriptionService.getActiveSubscription();

    // Then
    assertTrue(response.isEmpty());
  }

  @Test
  void getSubscriptions_shouldReturnPagedSubscriptions() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<TenantSubscription> subscriptions = List.of(
        createTestSubscription(createTestPlan(UUID.randomUUID())),
        createTestSubscription(createTestPlan(UUID.randomUUID()))
    );
    Page<TenantSubscription> subscriptionPage = new PageImpl<>(subscriptions, pageable, 2);

    when(subscriptionRepository.findAll(pageable)).thenReturn(subscriptionPage);

    // When
    Page<SubscriptionDtos.SubscriptionResponse> responses = subscriptionService.getSubscriptions(pageable);

    // Then
    assertEquals(2, responses.getContent().size());
    assertEquals(2, responses.getTotalElements());
  }

  @Test
  void cancelSubscription_shouldCancelSubscriptionSuccessfully() throws Exception {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(subscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId);

    // Then
    assertNotNull(response);
    verify(stateMachine).validateTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED);
    verify(paymentProvider).cancelSubscription(subscription.getStripeSubscriptionId(), true);
    verify(subscriptionRepository).save(subscription);
    verify(auditTrailRepository).save(any(TenantSubscriptionAuditTrail.class));
    verify(eventPublisher).publishSubscriptionCanceled(any(SubscriptionEvent.class));
  }

  @Test
  void cancelSubscriptionImmediately_shouldCancelImmediately() throws Exception {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(subscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.cancelSubscriptionImmediately(subscriptionId);

    // Then
    assertNotNull(response);
    verify(paymentProvider).cancelSubscription(subscription.getStripeSubscriptionId(), false);
  }

  @Test
  void pauseSubscription_shouldPauseSubscriptionSuccessfully() throws Exception {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(subscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.pauseSubscription(subscriptionId);

    // Then
    assertNotNull(response);
    verify(stateMachine).validateTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED);
    verify(paymentProvider).pauseSubscription(subscription.getStripeSubscriptionId());
    verify(subscriptionRepository).save(subscription);
    verify(auditTrailRepository).save(any(TenantSubscriptionAuditTrail.class));
    verify(eventPublisher).publishSubscriptionPaused(any(SubscriptionEvent.class));
  }

  @Test
  void resumeSubscription_shouldResumeSubscriptionSuccessfully() throws Exception {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);
    subscription.setStatus(SubscriptionStatus.PAUSED);

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(subscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.resumeSubscription(subscriptionId);

    // Then
    assertNotNull(response);
    verify(stateMachine).validateTransition(SubscriptionStatus.PAUSED, SubscriptionStatus.ACTIVE);
    verify(paymentProvider).resumeSubscription(subscription.getStripeSubscriptionId());
    verify(subscriptionRepository).save(subscription);
    verify(auditTrailRepository).save(any(TenantSubscriptionAuditTrail.class));
    verify(eventPublisher).publishSubscriptionResumed(any(SubscriptionEvent.class));
  }

  @Test
  void syncSubscriptionFromStripe_shouldSyncSuccessfully() throws Exception {
    // Given
    String stripeSubscriptionId = "sub_123";
    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setStripeSubscriptionId(stripeSubscriptionId);

    when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
        .thenReturn(Optional.of(subscription));
    when(paymentProvider.getSubscription(stripeSubscriptionId)).thenReturn(new Object());
    when(subscriptionRepository.save(subscription)).thenReturn(subscription);

    // When
    subscriptionService.syncSubscriptionFromStripe(stripeSubscriptionId);

    // Then
    verify(paymentProvider).getSubscription(stripeSubscriptionId);
    verify(subscriptionRepository).save(subscription);
  }

  @Test
  void syncSubscriptionFromStripe_shouldThrowExceptionWhenNotFound() {
    // Given
    String stripeSubscriptionId = "sub_nonexistent";
    when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
        .thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> subscriptionService.syncSubscriptionFromStripe(stripeSubscriptionId)
    );

    assertEquals("Subscription not found for Stripe ID: " + stripeSubscriptionId, exception.getMessage());
  }

  @Test
  void updateSubscription_shouldUpdatePlanSuccessfully() throws Exception {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();

    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);

    SubscriptionPlan newPlan = createTestPlan(newPlanId);
    newPlan.setName("New Plan");

    SubscriptionDtos.UpdateSubscriptionRequest request = new SubscriptionDtos.UpdateSubscriptionRequest(
        newPlanId, "pm_new", Map.of("updated", "true")
    );

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(planRepository.findById(newPlanId)).thenReturn(Optional.of(newPlan));
    when(subscriptionRepository.save(any(TenantSubscription.class))).thenReturn(subscription);
    when(auditTrailRepository.save(any(TenantSubscriptionAuditTrail.class)))
        .thenReturn(new TenantSubscriptionAuditTrail());

    // When
    SubscriptionDtos.SubscriptionResponse response = subscriptionService.updateSubscription(subscriptionId, request);

    // Then
    assertNotNull(response);
    verify(paymentProvider).updateSubscription(subscription.getStripeSubscriptionId(),
        newPlan.getStripePriceId(), request.metadata());
    verify(subscriptionRepository).save(subscription);
    verify(auditTrailRepository).save(any(TenantSubscriptionAuditTrail.class));
    verify(eventPublisher).publishSubscriptionUpdated(any(SubscriptionEvent.class));
  }

  @Test
  void updateSubscription_shouldThrowExceptionWhenNewPlanNotActive() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();

    TenantSubscription subscription = createTestSubscription(createTestPlan(UUID.randomUUID()));
    subscription.setId(subscriptionId);

    SubscriptionPlan inactivePlan = createTestPlan(newPlanId);
    inactivePlan.setIsActive(false);

    SubscriptionDtos.UpdateSubscriptionRequest request = new SubscriptionDtos.UpdateSubscriptionRequest(
        newPlanId, null, null
    );

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(planRepository.findById(newPlanId)).thenReturn(Optional.of(inactivePlan));

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> subscriptionService.updateSubscription(subscriptionId, request)
    );

    assertEquals("Target plan is not active: " + newPlanId, exception.getMessage());
    verify(paymentProvider, never()).updateSubscription(anyString(), anyString(), anyMap());
  }

  private SubscriptionPlan createTestPlan(UUID planId) {
    SubscriptionPlan plan = new SubscriptionPlan();
    plan.setId(planId);
    plan.setName("Test Plan");
    plan.setDescription("Test Description");
    plan.setIsActive(true);
    plan.setStripePriceId("price_123");
    return plan;
  }

  private TenantSubscription createTestSubscription(SubscriptionPlan plan) {
    TenantSubscription subscription = new TenantSubscription();
    subscription.setId(UUID.randomUUID());
    subscription.setPlan(plan);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setStripeSubscriptionId("sub_123");
    subscription.setStripeCustomerId("cus_123");
    subscription.setCurrentPeriodStart(Instant.now());
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(2592000));
    return subscription;
  }
}
