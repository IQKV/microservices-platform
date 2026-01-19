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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceImplTest {

  @Mock
  private SubscriptionPlanRepository planRepository;

  @Mock
  private PaymentProviderFactory paymentProviderFactory;

  @Mock
  private PaymentProviderAdapter paymentProvider;

  private SubscriptionPlanServiceImpl planService;

  @BeforeEach
  void setUp() {
    planService = new SubscriptionPlanServiceImpl(planRepository, paymentProviderFactory);

    // Setup common mocks
    lenient().when(paymentProviderFactory.getProvider(PaymentGatewayProvider.STRIPE))
        .thenReturn(paymentProvider);
  }

  @Test
  void createPlan_shouldCreatePlanSuccessfully() {
    // Given
    SubscriptionDtos.UpsertPlanRequest request = new SubscriptionDtos.UpsertPlanRequest(
        "Premium Plan",
        "Premium subscription with advanced features",
        new BigDecimal("29.99"),
        "USD",
        "MONTH",
        1,
        14,
        true,
        Map.of("tier", "premium")
    );

    SubscriptionPlan savedPlan = createTestPlan();
    savedPlan.setName("Premium Plan"); // Set the expected name
    when(planRepository.existsByName("Premium Plan")).thenReturn(false);
    when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(savedPlan);

    // When
    SubscriptionDtos.PlanResponse response = planService.createPlan(request);

    // Then
    assertNotNull(response);
    assertEquals("Premium Plan", response.name());
    assertEquals(new BigDecimal("29.99"), response.priceAmount());
    assertEquals("USD", response.currency());
    assertEquals("MONTH", response.interval());
    assertEquals(1, response.intervalCount());
    assertEquals(14, response.trialDays());
    assertTrue(response.isActive());

    verify(planRepository).existsByName("Premium Plan");
    verify(planRepository).save(any(SubscriptionPlan.class));
  }

  @Test
  void createPlan_shouldThrowExceptionWhenNameAlreadyExists() {
    // Given
    SubscriptionDtos.UpsertPlanRequest request = new SubscriptionDtos.UpsertPlanRequest(
        "Existing Plan",
        "Description",
        new BigDecimal("19.99"),
        "USD",
        "MONTH",
        1,
        0,
        true,
        null
    );

    when(planRepository.existsByName("Existing Plan")).thenReturn(true);

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> planService.createPlan(request)
    );

    assertEquals("Plan with name 'Existing Plan' already exists", exception.getMessage());
    verify(planRepository, never()).save(any(SubscriptionPlan.class));
  }

  @Test
  void updatePlan_shouldUpdatePlanSuccessfully() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan existingPlan = createTestPlan();
    existingPlan.setId(planId);

    SubscriptionDtos.UpsertPlanRequest request = new SubscriptionDtos.UpsertPlanRequest(
        "Updated Plan Name",
        "Updated description",
        null, // Price should not be updated
        null, // Currency should not be updated
        null, // Interval should not be updated
        null,
        null,
        false,
        Map.of("updated", "true")
    );

    when(planRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
    when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(existingPlan);

    // When
    SubscriptionDtos.PlanResponse response = planService.updatePlan(planId, request);

    // Then
    assertNotNull(response);
    verify(planRepository).findById(planId);
    verify(planRepository).save(existingPlan);
  }

  @Test
  void updatePlan_shouldThrowExceptionWhenPlanNotFound() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionDtos.UpsertPlanRequest request = new SubscriptionDtos.UpsertPlanRequest(
        "Updated Plan",
        "Description",
        new BigDecimal("19.99"),
        "USD",
        "MONTH",
        1,
        0,
        true,
        null
    );

    when(planRepository.findById(planId)).thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> planService.updatePlan(planId, request)
    );

    assertEquals("Plan not found with id: " + planId, exception.getMessage());
  }

  @Test
  void getPlan_shouldReturnPlanSuccessfully() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

    // When
    SubscriptionDtos.PlanResponse response = planService.getPlan(planId);

    // Then
    assertNotNull(response);
    assertEquals(planId, response.id());
    assertEquals("Test Plan", response.name());
  }

  @Test
  void getPlan_shouldThrowExceptionWhenPlanNotFound() {
    // Given
    UUID planId = UUID.randomUUID();
    when(planRepository.findById(planId)).thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> planService.getPlan(planId)
    );

    assertEquals("Plan not found with id: " + planId, exception.getMessage());
  }

  @Test
  void getActivePlans_shouldReturnActivePlansOnly() {
    // Given
    List<SubscriptionPlan> activePlans = List.of(createTestPlan(), createTestPlan());
    when(planRepository.findByIsActiveTrue()).thenReturn(activePlans);

    // When
    List<SubscriptionDtos.PlanResponse> responses = planService.getActivePlans();

    // Then
    assertEquals(2, responses.size());
    verify(planRepository).findByIsActiveTrue();
  }

  @Test
  void getAllPlans_shouldReturnPagedPlans() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<SubscriptionPlan> plans = List.of(createTestPlan(), createTestPlan());
    Page<SubscriptionPlan> planPage = new PageImpl<>(plans, pageable, 2);

    when(planRepository.findAll(pageable)).thenReturn(planPage);

    // When
    Page<SubscriptionDtos.PlanResponse> responses = planService.getAllPlans(pageable);

    // Then
    assertEquals(2, responses.getContent().size());
    assertEquals(2, responses.getTotalElements());
    verify(planRepository).findAll(pageable);
  }

  @Test
  void deactivatePlan_shouldDeactivatePlanSuccessfully() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setIsActive(true);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(planRepository.save(plan)).thenReturn(plan);

    // When
    planService.deactivatePlan(planId);

    // Then
    verify(planRepository).findById(planId);
    verify(planRepository).save(plan);
  }

  @Test
  void activatePlan_shouldActivatePlanSuccessfully() {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setIsActive(false);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(planRepository.save(plan)).thenReturn(plan);

    // When
    planService.activatePlan(planId);

    // Then
    verify(planRepository).findById(planId);
    verify(planRepository).save(plan);
  }

  @Test
  void syncPlanWithStripe_shouldCreateProductAndPrice() throws Exception {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setStripeProductId(null);
    plan.setStripePriceId(null);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(paymentProvider.createProduct(anyString(), anyString(), anyMap()))
        .thenReturn("prod_123");
    when(paymentProvider.createPrice(anyString(), any(BigDecimal.class), anyString(),
        anyString(), any(Integer.class), anyMap()))
        .thenReturn("price_123");
    when(planRepository.save(plan)).thenReturn(plan);

    // When
    planService.syncPlanWithStripe(planId);

    // Then
    verify(paymentProvider).createProduct(eq("Test Plan"), eq("Test Description"), anyMap());
    verify(paymentProvider).createPrice(eq("prod_123"), eq(new BigDecimal("29.99")),
        eq("USD"), eq("MONTH"), eq(1), anyMap());
    verify(planRepository).save(plan);
  }

  @Test
  void syncPlanWithStripe_shouldOnlyCreatePriceWhenProductExists() throws Exception {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setStripeProductId("prod_existing");
    plan.setStripePriceId(null);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(paymentProvider.createPrice(anyString(), any(BigDecimal.class), anyString(),
        anyString(), any(Integer.class), anyMap()))
        .thenReturn("price_123");
    when(planRepository.save(plan)).thenReturn(plan);

    // When
    planService.syncPlanWithStripe(planId);

    // Then
    verify(paymentProvider, never()).createProduct(anyString(), anyString(), anyMap());
    verify(paymentProvider).createPrice(eq("prod_existing"), eq(new BigDecimal("29.99")),
        eq("USD"), eq("MONTH"), eq(1), anyMap());
    verify(planRepository).save(plan);
  }

  @Test
  void syncPlanWithStripe_shouldSkipWhenAlreadySynced() throws Exception {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setStripeProductId("prod_existing");
    plan.setStripePriceId("price_existing");

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(planRepository.save(plan)).thenReturn(plan);

    // When
    planService.syncPlanWithStripe(planId);

    // Then
    verify(paymentProvider, never()).createProduct(anyString(), anyString(), anyMap());
    verify(paymentProvider, never()).createPrice(anyString(), any(BigDecimal.class),
        anyString(), anyString(), any(Integer.class), anyMap());
    verify(planRepository).save(plan);
  }

  @Test
  void syncPlanWithStripe_shouldThrowExceptionOnStripeError() throws Exception {
    // Given
    UUID planId = UUID.randomUUID();
    SubscriptionPlan plan = createTestPlan();
    plan.setId(planId);
    plan.setStripeProductId(null);
    plan.setStripePriceId(null);

    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    when(paymentProvider.createProduct(anyString(), anyString(), anyMap()))
        .thenThrow(new RuntimeException("Stripe API error"));

    // When & Then
    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> planService.syncPlanWithStripe(planId)
    );

    assertTrue(exception.getMessage().contains("Failed to sync plan with Stripe"));
  }

  @Test
  void findByStripePriceId_shouldReturnPlanSuccessfully() {
    // Given
    String stripePriceId = "price_123";
    SubscriptionPlan plan = createTestPlan();
    plan.setStripePriceId(stripePriceId);

    when(planRepository.findByStripePriceId(stripePriceId)).thenReturn(Optional.of(plan));

    // When
    SubscriptionPlan result = planService.findByStripePriceId(stripePriceId);

    // Then
    assertNotNull(result);
    assertEquals(stripePriceId, result.getStripePriceId());
  }

  @Test
  void findByStripePriceId_shouldThrowExceptionWhenNotFound() {
    // Given
    String stripePriceId = "price_nonexistent";
    when(planRepository.findByStripePriceId(stripePriceId)).thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(
        SubscriptionNotFoundException.class,
        () -> planService.findByStripePriceId(stripePriceId)
    );

    assertEquals("Plan not found with Stripe price ID: " + stripePriceId, exception.getMessage());
  }

  private SubscriptionPlan createTestPlan() {
    SubscriptionPlan plan = new SubscriptionPlan();
    plan.setId(UUID.randomUUID());
    plan.setName("Test Plan");
    plan.setDescription("Test Description");
    plan.setAmount(new BigDecimal("29.99"));
    plan.setCurrency("USD");
    plan.setInterval(SubscriptionInterval.MONTH);
    plan.setIntervalCount(1);
    plan.setTrialPeriodDays(14);
    plan.setIsActive(true);
    return plan;
  }
}
