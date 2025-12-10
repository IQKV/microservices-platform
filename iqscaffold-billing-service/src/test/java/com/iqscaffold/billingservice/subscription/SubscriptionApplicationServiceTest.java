package com.iqscaffold.billingservice.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.billing.ProrationCalculator;
import com.iqscaffold.billingservice.billing.ProrationResult;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodType;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.PlanTransition;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.plan.SubscriptionPlanRepository;
import com.iqscaffold.billingservice.plan.ValidPlanTransitionSpecification;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import com.iqscaffold.billingservice.usage.MetricType;
import com.iqscaffold.billingservice.usage.QuotaExceededSpecification;
import com.iqscaffold.billingservice.usage.UsageContext;
import com.iqscaffold.billingservice.usage.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for SubscriptionApplicationService.
 *
 * <p>Tests the orchestration logic of the subscription application service, verifying:
 * <ul>
 *   <li>Delegation to domain aggregates, factories, and services</li>
 *   <li>Transaction management boundaries</li>
 *   <li>DTO translation</li>
 *   <li>Cache eviction and retrieval</li>
 *   <li>Error handling and exception translation</li>
 *   <li>Domain event publishing coordination</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionApplicationService Unit Tests")
class SubscriptionApplicationServiceTest {

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private SubscriptionPlanRepository subscriptionPlanRepository;

  @Mock
  private PaymentMethodRepository paymentMethodRepository;

  @Mock
  private TenantTrialHistoryRepository tenantTrialHistoryRepository;

  @Mock
  private SubscriptionFactory subscriptionFactory;

  @Mock
  private SubscriptionLifecycleManager subscriptionLifecycleManager;

  @Mock
  private TrialEligibilitySpecification trialEligibilitySpecification;

  @Mock
  private MessageService messageService;

  @Mock
  private ProrationCalculator prorationCalculator;

  @Mock
  private ValidPlanTransitionSpecification validPlanTransitionSpecification;

  @Mock
  private QuotaExceededSpecification quotaExceededSpecification;

  @Mock
  private UsageRecordRepository usageRecordRepository;

  @InjectMocks
  private SubscriptionApplicationService subscriptionApplicationService;

  private UUID testTenantId;
  private UUID testUserId;
  private SubscriptionPlan testPlan;
  private SubscriptionPlan higherTierPlan;
  private SubscriptionPlan lowerTierPlan;
  private Subscription testSubscription;
  private PaymentMethod testPaymentMethod;
  private PlanQuotas testQuotas;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    testQuotas = new PlanQuotas(
        100L, 50L, 10000L, 5000L, 100L, 1000L, 5L, 10L
    );

    testPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Plan",
        "Professional features",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("49.99"),
        "USD",
        Map.of("advanced_workflows", true),
        testQuotas,
        14,
        true
    );
    TestEntityUtils.setId(testPlan, 1L);

    higherTierPlan = SubscriptionPlan.create(
        "ENTERPRISE_MONTHLY",
        "Enterprise Plan",
        "Enterprise features",
        PlanTier.ENTERPRISE,
        BillingCycle.MONTHLY,
        new BigDecimal("199.99"),
        "USD",
        Map.of("advanced_workflows", true, "ai_features", true),
        testQuotas,
        0,
        true
    );
    TestEntityUtils.setId(higherTierPlan, 2L);

    lowerTierPlan = SubscriptionPlan.create(
        "FREE_MONTHLY",
        "Free Plan",
        "Basic features",
        PlanTier.FREE,
        BillingCycle.MONTHLY,
        BigDecimal.ZERO,
        "USD",
        Map.of(),
        testQuotas,
        0,
        true
    );
    TestEntityUtils.setId(lowerTierPlan, 3L);

    testSubscription = Subscription.createActive(testTenantId, testUserId, testPlan);
    TestEntityUtils.setId(testSubscription, 1L);

    testPaymentMethod = new PaymentMethod(
        testTenantId,
        testUserId,
        PaymentMethodType.CARD,
        "pm_test123"
    );
    TestEntityUtils.setId(testPaymentMethod, 1L);
  }

  @Nested
  @DisplayName("createSubscription Tests")
  class CreateSubscriptionTests {

    @Test
    @DisplayName("Should create trial subscription successfully")
    void shouldCreateTrialSubscriptionSuccessfully() {
      // Arrange
      CreateSubscriptionRequest request = new CreateSubscriptionRequest(
          testTenantId,
          testUserId,
          "PRO_MONTHLY",
          true,
          null,
          Map.of()
      );

      TenantTrialHistory trialHistory = TenantTrialHistory.create(testTenantId);

      when(subscriptionRepository.existsActiveByTenantId(testTenantId)).thenReturn(false);
      when(subscriptionPlanRepository.findByPlanCode("PRO_MONTHLY"))
          .thenReturn(Optional.of(testPlan));
      when(tenantTrialHistoryRepository.findByTenantId(testTenantId))
          .thenReturn(Optional.of(trialHistory));
      when(trialEligibilitySpecification.isSatisfiedBy(trialHistory)).thenReturn(true);
      when(subscriptionFactory.createTrialSubscription(testTenantId, testUserId, testPlan, trialHistory))
          .thenReturn(testSubscription);
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      SubscriptionDto result = subscriptionApplicationService.createSubscription(request);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).existsActiveByTenantId(testTenantId);
      verify(subscriptionPlanRepository).findByPlanCode("PRO_MONTHLY");
      verify(subscriptionFactory).createTrialSubscription(testTenantId, testUserId, testPlan, trialHistory);
      verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant already has active subscription")
    void shouldThrowExceptionWhenTenantHasActiveSubscription() {
      // Arrange
      CreateSubscriptionRequest request = new CreateSubscriptionRequest(
          testTenantId,
          testUserId,
          "PRO_MONTHLY",
          false,
          1L,
          Map.of()
      );

      when(subscriptionRepository.existsActiveByTenantId(testTenantId)).thenReturn(true);
      when(messageService.getMessage("subscription.already.exists"))
          .thenReturn("Subscription already exists");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.createSubscription(request))
          .isInstanceOf(SubscriptionException.SubscriptionAlreadyExistsException.class);

      verify(subscriptionRepository).existsActiveByTenantId(testTenantId);
      verify(subscriptionPlanRepository, never()).findByPlanCode(anyString());
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      CreateSubscriptionRequest request = new CreateSubscriptionRequest(
          testTenantId,
          testUserId,
          "INVALID_PLAN",
          false,
          1L,
          Map.of()
      );

      when(subscriptionRepository.existsActiveByTenantId(testTenantId)).thenReturn(false);
      when(subscriptionPlanRepository.findByPlanCode("INVALID_PLAN"))
          .thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.createSubscription(request))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findByPlanCode("INVALID_PLAN");
      verify(subscriptionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("extendTrial Tests")
  class ExtendTrialTests {

    @Test
    @DisplayName("Should extend trial successfully")
    void shouldExtendTrialSuccessfully() {
      // Arrange
      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      SubscriptionDto result = subscriptionApplicationService.extendTrial(1L, 7, "Customer request");

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(subscriptionLifecycleManager).extendTrial(testSubscription, 7, "Customer request");
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.extendTrial(999L, 7, "reason"))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(subscriptionLifecycleManager, never()).extendTrial(any(), any(Integer.class), anyString());
    }
  }

  @Nested
  @DisplayName("convertTrialToActive Tests")
  class ConvertTrialToActiveTests {

    @Test
    @DisplayName("Should convert trial to active successfully")
    void shouldConvertTrialToActiveSuccessfully() {
      // Arrange
      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      SubscriptionDto result = subscriptionApplicationService.convertTrialToActive(1L);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(subscriptionLifecycleManager).convertTrialToActive(testSubscription);
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.convertTrialToActive(999L))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(subscriptionLifecycleManager, never()).convertTrialToActive(any());
    }
  }

  @Nested
  @DisplayName("getActiveSubscription Tests")
  class GetActiveSubscriptionTests {

    @Test
    @DisplayName("Should retrieve active subscription successfully")
    void shouldRetrieveActiveSubscriptionSuccessfully() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));

      // Act
      SubscriptionDto result = subscriptionApplicationService.getActiveSubscription(testTenantId);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
    }

    @Test
    @DisplayName("Should throw exception when no active subscription found")
    void shouldThrowExceptionWhenNoActiveSubscription() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.getActiveSubscription(testTenantId))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
    }
  }

  @Nested
  @DisplayName("getSubscription Tests")
  class GetSubscriptionTests {

    @Test
    @DisplayName("Should retrieve subscription by ID successfully")
    void shouldRetrieveSubscriptionById() {
      // Arrange
      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));

      // Act
      SubscriptionDto result = subscriptionApplicationService.getSubscription(1L);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.getSubscription(999L))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
    }
  }

  @Nested
  @DisplayName("upgradeSubscription Tests")
  class UpgradeSubscriptionTests {

    @Test
    @DisplayName("Should upgrade subscription successfully")
    void shouldUpgradeSubscriptionSuccessfully() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "ENTERPRISE_MONTHLY",
          null,
          Map.of()
      );

      ProrationResult prorationResult = ProrationResult.forUpgrade(
          testPlan.getBasePrice(),
          higherTierPlan.getBasePrice(),
          15,
          30,
          testPlan.getName(),
          higherTierPlan.getName()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("ENTERPRISE_MONTHLY"))
          .thenReturn(Optional.of(higherTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);
      when(prorationCalculator.calculateUpgrade(testSubscription, higherTierPlan))
          .thenReturn(prorationResult);
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(messageService.getMessage(eq("subscription.upgraded"), anyString()))
          .thenReturn("Subscription upgraded");

      // Act
      SubscriptionDto result = subscriptionApplicationService.upgradeSubscription(1L, request);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(subscriptionPlanRepository).findByPlanCode("ENTERPRISE_MONTHLY");
      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
      verify(prorationCalculator).calculateUpgrade(testSubscription, higherTierPlan);
      verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "ENTERPRISE_MONTHLY",
          null,
          Map.of()
      );

      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.upgradeSubscription(999L, request))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(prorationCalculator, never()).calculateUpgrade(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "INVALID_PLAN",
          null,
          Map.of()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("INVALID_PLAN"))
          .thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.upgradeSubscription(1L, request))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findByPlanCode("INVALID_PLAN");
      verify(prorationCalculator, never()).calculateUpgrade(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when transition is invalid")
    void shouldThrowExceptionWhenTransitionInvalid() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "ENTERPRISE_MONTHLY",
          null,
          Map.of()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("ENTERPRISE_MONTHLY"))
          .thenReturn(Optional.of(higherTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(false);
      when(messageService.getMessage(eq("subscription.upgrade.invalid.transition"), anyString(), anyString()))
          .thenReturn("Invalid transition");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.upgradeSubscription(1L, request))
          .isInstanceOf(PlanException.InvalidPlanTransitionException.class);

      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
      verify(prorationCalculator, never()).calculateUpgrade(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when new plan is not higher tier")
    void shouldThrowExceptionWhenNotHigherTier() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "FREE_MONTHLY",
          null,
          Map.of()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("FREE_MONTHLY"))
          .thenReturn(Optional.of(lowerTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);
      when(messageService.getMessage("subscription.upgrade.not.higher.tier"))
          .thenReturn("Not a higher tier");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.upgradeSubscription(1L, request))
          .isInstanceOf(PlanException.InvalidPlanTransitionException.class);

      verify(prorationCalculator, never()).calculateUpgrade(any(), any());
    }
  }

  @Nested
  @DisplayName("downgradeSubscription Tests")
  class DowngradeSubscriptionTests {

    @Test
    @DisplayName("Should downgrade subscription immediately with proration")
    void shouldDowngradeImmediatelyWithProration() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "FREE_MONTHLY",
          true,
          Map.of()
      );

      ProrationResult prorationResult = ProrationResult.forDowngrade(
          testPlan.getBasePrice(),
          lowerTierPlan.getBasePrice(),
          15,
          30,
          testPlan.getName(),
          lowerTierPlan.getName()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("FREE_MONTHLY"))
          .thenReturn(Optional.of(lowerTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);
      when(quotaExceededSpecification.isSatisfiedBy(any(UsageContext.class)))
          .thenReturn(false);
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          any(UUID.class), any(MetricType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(0L);
      when(prorationCalculator.calculateDowngrade(testSubscription, lowerTierPlan))
          .thenReturn(prorationResult);
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(messageService.getMessage(eq("subscription.downgraded.immediate"), anyString(), any()))
          .thenReturn("Subscription downgraded");

      // Act
      SubscriptionDto result = subscriptionApplicationService.downgradeSubscription(1L, request);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(subscriptionPlanRepository).findByPlanCode("FREE_MONTHLY");
      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
      verify(prorationCalculator).calculateDowngrade(testSubscription, lowerTierPlan);
      verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should schedule downgrade at period end")
    void shouldScheduleDowngradeAtPeriodEnd() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "FREE_MONTHLY",
          false,
          Map.of()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("FREE_MONTHLY"))
          .thenReturn(Optional.of(lowerTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);
      when(quotaExceededSpecification.isSatisfiedBy(any(UsageContext.class)))
          .thenReturn(false);
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          any(UUID.class), any(MetricType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(0L);
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(messageService.getMessage(eq("subscription.downgraded.scheduled"), anyString(), any()))
          .thenReturn("Subscription downgrade scheduled");

      // Act
      SubscriptionDto result = subscriptionApplicationService.downgradeSubscription(1L, request);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(subscriptionPlanRepository).findByPlanCode("FREE_MONTHLY");
      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
      verify(prorationCalculator, never()).calculateDowngrade(any(), any());
      verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "FREE_MONTHLY",
          true,
          Map.of()
      );

      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.downgradeSubscription(999L, request))
          .isInstanceOf(SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(prorationCalculator, never()).calculateDowngrade(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when new plan is not lower tier")
    void shouldThrowExceptionWhenNotLowerTier() {
      // Arrange
      UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
          "ENTERPRISE_MONTHLY",
          true,
          Map.of()
      );

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(subscriptionPlanRepository.findByPlanCode("ENTERPRISE_MONTHLY"))
          .thenReturn(Optional.of(higherTierPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);
      when(messageService.getMessage("subscription.downgrade.not.lower.tier"))
          .thenReturn("Not a lower tier");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionApplicationService.downgradeSubscription(1L, request))
          .isInstanceOf(PlanException.InvalidPlanTransitionException.class);

      verify(prorationCalculator, never()).calculateDowngrade(any(), any());
    }
  }
}
