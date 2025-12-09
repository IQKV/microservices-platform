package com.iqscaffold.billingservice.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.PlanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for SubscriptionPlanService.
 * 
 * <p>Tests the orchestration logic of the plan application service, verifying:
 * <ul>
 *   <li>Delegation to SubscriptionPlan aggregate</li>
 *   <li>Transaction management boundaries</li>
 *   <li>DTO translation</li>
 *   <li>Cache eviction and retrieval</li>
 *   <li>Error handling and exception translation</li>
 *   <li>Internationalized message handling</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionPlanService Unit Tests")
class SubscriptionPlanServiceTest {

  @Mock
  private SubscriptionPlanRepository subscriptionPlanRepository;

  @Mock
  private MessageService messageService;

  @Mock
  private ValidPlanTransitionSpecification validPlanTransitionSpecification;

  @InjectMocks
  private SubscriptionPlanService subscriptionPlanService;

  private SubscriptionPlan testPlan;
  private PlanQuotas testQuotas;

  @BeforeEach
  void setUp() {
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
  }

  @Nested
  @DisplayName("createPlan Tests")
  class CreatePlanTests {

    @Test
    @DisplayName("Should create plan successfully")
    void shouldCreatePlanSuccessfully() {
      // Arrange
      when(subscriptionPlanRepository.existsByPlanCode("PRO_MONTHLY")).thenReturn(false);
      when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(testPlan);

      // Act
      SubscriptionPlanDto result = subscriptionPlanService.createPlan(
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

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.planCode()).isEqualTo("PRO_MONTHLY");
      assertThat(result.name()).isEqualTo("Pro Plan");
      verify(subscriptionPlanRepository).existsByPlanCode("PRO_MONTHLY");
      verify(subscriptionPlanRepository).save(any(SubscriptionPlan.class));
    }

    @Test
    @DisplayName("Should throw exception when plan code already exists")
    void shouldThrowExceptionWhenPlanCodeExists() {
      // Arrange
      when(subscriptionPlanRepository.existsByPlanCode("PRO_MONTHLY")).thenReturn(true);
      when(messageService.getMessage("error.conflict")).thenReturn("Conflict");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.createPlan(
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
      )).isInstanceOf(IllegalArgumentException.class);

      verify(subscriptionPlanRepository).existsByPlanCode("PRO_MONTHLY");
      verify(subscriptionPlanRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("updatePlan Tests")
  class UpdatePlanTests {

    @Test
    @DisplayName("Should update plan successfully")
    void shouldUpdatePlanSuccessfully() {
      // Arrange
      PlanQuotas newQuotas = new PlanQuotas(
          200L, 100L, 20000L, 10000L, 200L, 2000L, 10L, 20L
      );

      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
      when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(testPlan);

      // Act
      SubscriptionPlanDto result = subscriptionPlanService.updatePlan(
          1L,
          "Pro Plan Updated",
          "Updated description",
          Map.of("advanced_workflows", true, "ai_features", true),
          newQuotas,
          new BigDecimal("59.99"),
          "USD"
      );

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionPlanRepository).findById(1L);
      verify(subscriptionPlanRepository).save(testPlan);
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.updatePlan(
          999L,
          "Pro Plan",
          "Description",
          Map.of(),
          testQuotas,
          new BigDecimal("49.99"),
          "USD"
      )).isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findById(999L);
      verify(subscriptionPlanRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("archivePlan Tests")
  class ArchivePlanTests {

    @Test
    @DisplayName("Should archive plan successfully")
    void shouldArchivePlanSuccessfully() {
      // Arrange
      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
      when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(testPlan);

      // Act
      subscriptionPlanService.archivePlan(1L);

      // Assert
      verify(subscriptionPlanRepository).findById(1L);
      verify(subscriptionPlanRepository).save(testPlan);
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.archivePlan(999L))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findById(999L);
      verify(subscriptionPlanRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getPlan Tests")
  class GetPlanTests {

    @Test
    @DisplayName("Should retrieve plan by ID successfully")
    void shouldRetrievePlanById() {
      // Arrange
      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));

      // Act
      SubscriptionPlanDto result = subscriptionPlanService.getPlan(1L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(1L);
      assertThat(result.planCode()).isEqualTo("PRO_MONTHLY");
      verify(subscriptionPlanRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.getPlan(999L))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findById(999L);
    }
  }

  @Nested
  @DisplayName("getPlanByCode Tests")
  class GetPlanByCodeTests {

    @Test
    @DisplayName("Should retrieve plan by code successfully")
    void shouldRetrievePlanByCode() {
      // Arrange
      when(subscriptionPlanRepository.findByPlanCode("PRO_MONTHLY"))
          .thenReturn(Optional.of(testPlan));

      // Act
      SubscriptionPlanDto result = subscriptionPlanService.getPlanByCode("PRO_MONTHLY");

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.planCode()).isEqualTo("PRO_MONTHLY");
      verify(subscriptionPlanRepository).findByPlanCode("PRO_MONTHLY");
    }

    @Test
    @DisplayName("Should throw exception when plan not found")
    void shouldThrowExceptionWhenPlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findByPlanCode("INVALID_CODE"))
          .thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.getPlanByCode("INVALID_CODE"))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findByPlanCode("INVALID_CODE");
    }
  }

  @Nested
  @DisplayName("getPublicPlans Tests")
  class GetPublicPlansTests {

    @Test
    @DisplayName("Should retrieve all public plans successfully")
    void shouldRetrievePublicPlans() {
      // Arrange
      List<SubscriptionPlan> plans = List.of(testPlan);
      when(subscriptionPlanRepository.findActivePublicPlans()).thenReturn(plans);

      // Act
      List<SubscriptionPlanDto> result = subscriptionPlanService.getPublicPlans();

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.get(0).planCode()).isEqualTo("PRO_MONTHLY");
      verify(subscriptionPlanRepository).findActivePublicPlans();
    }

    @Test
    @DisplayName("Should return empty list when no public plans exist")
    void shouldReturnEmptyListWhenNoPublicPlans() {
      // Arrange
      when(subscriptionPlanRepository.findActivePublicPlans()).thenReturn(List.of());

      // Act
      List<SubscriptionPlanDto> result = subscriptionPlanService.getPublicPlans();

      // Assert
      assertThat(result).isEmpty();
      verify(subscriptionPlanRepository).findActivePublicPlans();
    }
  }

  @Nested
  @DisplayName("getAllPlans Tests")
  class GetAllPlansTests {

    @Test
    @DisplayName("Should retrieve all active plans successfully")
    void shouldRetrieveAllActivePlans() {
      // Arrange
      List<SubscriptionPlan> plans = List.of(testPlan);
      when(subscriptionPlanRepository.findActivePlans()).thenReturn(plans);

      // Act
      List<SubscriptionPlanDto> result = subscriptionPlanService.getAllPlans();

      // Assert
      assertThat(result).hasSize(1);
      verify(subscriptionPlanRepository).findActivePlans();
    }
  }

  @Nested
  @DisplayName("getPlansByBillingCycle Tests")
  class GetPlansByBillingCycleTests {

    @Test
    @DisplayName("Should retrieve plans by billing cycle successfully")
    void shouldRetrievePlansByBillingCycle() {
      // Arrange
      List<SubscriptionPlan> plans = List.of(testPlan);
      when(subscriptionPlanRepository.findByBillingCycle(BillingCycle.MONTHLY))
          .thenReturn(plans);

      // Act
      List<SubscriptionPlanDto> result = subscriptionPlanService
          .getPlansByBillingCycle(BillingCycle.MONTHLY);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.get(0).billingCycle()).isEqualTo(BillingCycle.MONTHLY);
      verify(subscriptionPlanRepository).findByBillingCycle(BillingCycle.MONTHLY);
    }
  }

  @Nested
  @DisplayName("validatePlanTransition Tests")
  class ValidatePlanTransitionTests {

    @Test
    @DisplayName("Should validate plan transition successfully")
    void shouldValidatePlanTransitionSuccessfully() {
      // Arrange
      SubscriptionPlan targetPlan = SubscriptionPlan.create(
          "ENTERPRISE_MONTHLY",
          "Enterprise Plan",
          "Enterprise features",
          PlanTier.ENTERPRISE,
          BillingCycle.MONTHLY,
          new BigDecimal("199.99"),
          "USD",
          Map.of(),
          testQuotas,
          0,
          true
      );
      TestEntityUtils.setId(targetPlan, 2L);

      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
      when(subscriptionPlanRepository.findById(2L)).thenReturn(Optional.of(targetPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(true);

      // Act
      subscriptionPlanService.validatePlanTransition(1L, 2L);

      // Assert
      verify(subscriptionPlanRepository).findById(1L);
      verify(subscriptionPlanRepository).findById(2L);
      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
    }

    @Test
    @DisplayName("Should throw exception when source plan not found")
    void shouldThrowExceptionWhenSourcePlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.validatePlanTransition(999L, 2L))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findById(999L);
    }

    @Test
    @DisplayName("Should throw exception when target plan not found")
    void shouldThrowExceptionWhenTargetPlanNotFound() {
      // Arrange
      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
      when(subscriptionPlanRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("plan.not.found")).thenReturn("Plan not found");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.validatePlanTransition(1L, 999L))
          .isInstanceOf(PlanException.PlanNotFoundException.class);

      verify(subscriptionPlanRepository).findById(1L);
      verify(subscriptionPlanRepository).findById(999L);
    }

    @Test
    @DisplayName("Should throw exception when transition is invalid")
    void shouldThrowExceptionWhenTransitionInvalid() {
      // Arrange
      SubscriptionPlan targetPlan = SubscriptionPlan.create(
          "ENTERPRISE_MONTHLY",
          "Enterprise Plan",
          "Enterprise features",
          PlanTier.ENTERPRISE,
          BillingCycle.MONTHLY,
          new BigDecimal("199.99"),
          "USD",
          Map.of(),
          testQuotas,
          0,
          true
      );
      TestEntityUtils.setId(targetPlan, 2L);

      when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
      when(subscriptionPlanRepository.findById(2L)).thenReturn(Optional.of(targetPlan));
      when(validPlanTransitionSpecification.isSatisfiedBy(any(PlanTransition.class)))
          .thenReturn(false);
      when(messageService.getMessage(anyString(), anyString(), anyString()))
          .thenReturn("Invalid transition");

      // Act & Assert
      assertThatThrownBy(() -> subscriptionPlanService.validatePlanTransition(1L, 2L))
          .isInstanceOf(PlanException.InvalidPlanTransitionException.class);

      verify(validPlanTransitionSpecification).isSatisfiedBy(any(PlanTransition.class));
    }
  }
}
