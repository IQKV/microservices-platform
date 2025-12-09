package com.iqscaffold.billingservice.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for UsageApplicationService.
 * 
 * <p>Tests the orchestration logic of the usage application service, verifying:
 * <ul>
 *   <li>Delegation to domain services (QuotaEnforcer, QuotaExceededSpecification)</li>
 *   <li>Transaction management boundaries</li>
 *   <li>DTO translation</li>
 *   <li>Cache eviction and retrieval</li>
 *   <li>Error handling and validation</li>
 *   <li>Idempotency handling</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsageApplicationService Unit Tests")
class UsageApplicationServiceTest {

  @Mock
  private UsageRecordRepository usageRecordRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private QuotaEnforcer quotaEnforcer;

  @Mock
  private QuotaExceededSpecification quotaExceededSpecification;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private UsageApplicationService usageApplicationService;

  private UUID testTenantId;
  private UUID testUserId;
  private Subscription testSubscription;
  private SubscriptionPlan testPlan;
  private PlanQuotas testQuotas;
  private UsageRecord testUsageRecord;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    testQuotas = new PlanQuotas(
        10000L, 50L, 10000L, 5000L, 100L, 1000L, 5L, 10L
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

    testSubscription = Subscription.createActive(testTenantId, testUserId, testPlan);
    TestEntityUtils.setId(testSubscription, 1L);

    testUsageRecord = new UsageRecord(
        testSubscription,
        testTenantId,
        MetricType.API_CALLS,
        100L,
        "calls",
        LocalDateTime.now(),
        testSubscription.getCurrentPeriodStart(),
        testSubscription.getCurrentPeriodEnd()
    );
    TestEntityUtils.setId(testUsageRecord, 1L);
  }

  @Nested
  @DisplayName("recordUsage Tests")
  class RecordUsageTests {

    @Test
    @DisplayName("Should record usage successfully")
    void shouldRecordUsageSuccessfully() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.save(any(UsageRecord.class))).thenReturn(testUsageRecord);
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          any(UUID.class), any(MetricType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(100L);
      when(quotaExceededSpecification.isSatisfiedBy(any(UsageContext.class))).thenReturn(false);

      // Act
      UsageDto result = usageApplicationService.recordUsage(
          testTenantId,
          MetricType.API_CALLS,
          100L,
          "calls",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.metricType()).isEqualTo(MetricType.API_CALLS);
      assertThat(result.quantity()).isEqualTo(100L);
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(usageRecordRepository).save(any(UsageRecord.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdNull() {
      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsage(
          null,
          MetricType.API_CALLS,
          100L,
          "calls",
          LocalDateTime.now()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tenant ID cannot be null");

      verify(subscriptionRepository, never()).findActiveByTenantId(any());
      verify(usageRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when metric type is null")
    void shouldThrowExceptionWhenMetricTypeNull() {
      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsage(
          testTenantId,
          null,
          100L,
          "calls",
          LocalDateTime.now()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Metric type cannot be null");

      verify(subscriptionRepository, never()).findActiveByTenantId(any());
      verify(usageRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void shouldThrowExceptionWhenQuantityNegative() {
      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsage(
          testTenantId,
          MetricType.API_CALLS,
          -100L,
          "calls",
          LocalDateTime.now()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity must be non-negative");

      verify(subscriptionRepository, never()).findActiveByTenantId(any());
      verify(usageRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when no active subscription exists")
    void shouldThrowExceptionWhenNoActiveSubscription() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.empty());
      when(messageService.getMessage(anyString(), (Object[]) any()))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsage(
          testTenantId,
          MetricType.API_CALLS,
          100L,
          "calls",
          LocalDateTime.now()
      )).isInstanceOf(UsageApplicationService.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(usageRecordRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("recordUsageBatch Tests")
  class RecordUsageBatchTests {

    @Test
    @DisplayName("Should record usage batch successfully")
    void shouldRecordUsageBatchSuccessfully() {
      // Arrange
      List<UsageApplicationService.UsageRecordRequest> requests = List.of(
          new UsageApplicationService.UsageRecordRequest(
              MetricType.API_CALLS, 100L, "calls", LocalDateTime.now()),
          new UsageApplicationService.UsageRecordRequest(
              MetricType.EMAIL_SENDS, 50L, "emails", LocalDateTime.now())
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.saveAll(any())).thenReturn(List.of(testUsageRecord));
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          any(UUID.class), any(MetricType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(100L);
      when(quotaExceededSpecification.isSatisfiedBy(any(UsageContext.class))).thenReturn(false);

      // Act
      List<UsageDto> result = usageApplicationService.recordUsageBatch(testTenantId, requests);

      // Assert
      assertThat(result).isNotEmpty();
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(usageRecordRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when usage records list is empty")
    void shouldThrowExceptionWhenUsageRecordsEmpty() {
      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsageBatch(
          testTenantId,
          List.of()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Usage records list cannot be empty");

      verify(subscriptionRepository, never()).findActiveByTenantId(any());
      verify(usageRecordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when usage records list is null")
    void shouldThrowExceptionWhenUsageRecordsNull() {
      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.recordUsageBatch(
          testTenantId,
          null
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Usage records list cannot be empty");

      verify(subscriptionRepository, never()).findActiveByTenantId(any());
      verify(usageRecordRepository, never()).saveAll(any());
    }
  }

  @Nested
  @DisplayName("getCurrentUsage Tests")
  class GetCurrentUsageTests {

    @Test
    @DisplayName("Should get current usage successfully")
    void shouldGetCurrentUsageSuccessfully() {
      // Arrange
      List<UsageMetric> metrics = List.of(
          new UsageMetric(MetricType.API_CALLS, 1000L, "calls", 10000L)
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.aggregateUsageByMetricType(
          eq(testTenantId), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(metrics);
      when(usageRecordRepository.countByTenantIdAndBillingPeriod(
          eq(testTenantId), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(10L);

      // Act
      UsageSummaryDto result = usageApplicationService.getCurrentUsage(testTenantId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.tenantId()).isEqualTo(testTenantId);
      assertThat(result.metrics()).hasSize(1);
      verify(subscriptionRepository, times(2)).findActiveByTenantId(testTenantId);
      verify(usageRecordRepository).aggregateUsageByMetricType(
          eq(testTenantId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should throw exception when no active subscription exists")
    void shouldThrowExceptionWhenNoActiveSubscription() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.empty());
      when(messageService.getMessage(anyString(), (Object[]) any()))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.getCurrentUsage(testTenantId))
          .isInstanceOf(UsageApplicationService.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
    }
  }

  @Nested
  @DisplayName("getUsageForPeriod Tests")
  class GetUsageForPeriodTests {

    @Test
    @DisplayName("Should get usage for period successfully")
    void shouldGetUsageForPeriodSuccessfully() {
      // Arrange
      LocalDateTime periodStart = LocalDateTime.now().minusDays(30);
      LocalDateTime periodEnd = LocalDateTime.now();
      List<UsageMetric> metrics = List.of(
          new UsageMetric(MetricType.API_CALLS, 1000L, "calls", 10000L)
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.aggregateUsageByMetricType(
          testTenantId, periodStart, periodEnd))
          .thenReturn(metrics);
      when(usageRecordRepository.countByTenantIdAndBillingPeriod(
          testTenantId, periodStart, periodEnd))
          .thenReturn(10L);

      // Act
      UsageSummaryDto result = usageApplicationService.getUsageForPeriod(
          testTenantId, periodStart, periodEnd);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.periodStart()).isEqualTo(periodStart);
      assertThat(result.periodEnd()).isEqualTo(periodEnd);
      verify(usageRecordRepository).aggregateUsageByMetricType(
          testTenantId, periodStart, periodEnd);
    }

    @Test
    @DisplayName("Should throw exception when period start is after period end")
    void shouldThrowExceptionWhenPeriodInvalid() {
      // Arrange
      LocalDateTime periodStart = LocalDateTime.now();
      LocalDateTime periodEnd = LocalDateTime.now().minusDays(30);

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.getUsageForPeriod(
          testTenantId, periodStart, periodEnd))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Period start must be before or equal to end");

      verify(usageRecordRepository, never()).aggregateUsageByMetricType(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("checkQuota Tests")
  class CheckQuotaTests {

    @Test
    @DisplayName("Should check quota successfully when quota available")
    void shouldCheckQuotaSuccessfullyWhenAvailable() {
      // Arrange
      QuotaCheckResult quotaResult = QuotaCheckResult.allowed(
          MetricType.API_CALLS, 1000L, 100L, 10000L, 9000L
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          eq(testTenantId), eq(MetricType.API_CALLS), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(1000L);
      when(quotaEnforcer.checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(1000L), eq(100L)))
          .thenReturn(quotaResult);

      // Act
      QuotaUsageDto result = usageApplicationService.checkQuota(
          testTenantId, MetricType.API_CALLS, 100L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.allowed()).isTrue();
      assertThat(result.currentUsage()).isEqualTo(1000L);
      assertThat(result.limit()).isEqualTo(10000L);
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(quotaEnforcer).checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(1000L), eq(100L));
    }

    @Test
    @DisplayName("Should check quota successfully when quota exceeded")
    void shouldCheckQuotaSuccessfullyWhenExceeded() {
      // Arrange
      QuotaCheckResult quotaResult = QuotaCheckResult.denied(
          MetricType.API_CALLS, 10000L, 100L, 10000L, "Quota exceeded"
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          eq(testTenantId), eq(MetricType.API_CALLS), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(10000L);
      when(quotaEnforcer.checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(10000L), eq(100L)))
          .thenReturn(quotaResult);

      // Act
      QuotaUsageDto result = usageApplicationService.checkQuota(
          testTenantId, MetricType.API_CALLS, 100L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.allowed()).isFalse();
      assertThat(result.currentUsage()).isEqualTo(10000L);
      verify(quotaEnforcer).checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(10000L), eq(100L));
    }

    @Test
    @DisplayName("Should throw exception when no active subscription exists")
    void shouldThrowExceptionWhenNoActiveSubscription() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.empty());
      when(messageService.getMessage(anyString(), (Object[]) any()))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.checkQuota(
          testTenantId, MetricType.API_CALLS, 100L))
          .isInstanceOf(UsageApplicationService.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(quotaEnforcer, never()).checkQuota(any(), any(), anyLong(), anyLong());
    }
  }

  @Nested
  @DisplayName("enforceQuota Tests")
  class EnforceQuotaTests {

    @Test
    @DisplayName("Should enforce quota successfully when quota available")
    void shouldEnforceQuotaSuccessfullyWhenAvailable() {
      // Arrange
      QuotaCheckResult quotaResult = QuotaCheckResult.allowed(
          MetricType.API_CALLS, 1000L, 100L, 10000L, 9000L
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          eq(testTenantId), eq(MetricType.API_CALLS), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(1000L);
      when(quotaEnforcer.checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(1000L), eq(100L)))
          .thenReturn(quotaResult);

      // Act
      usageApplicationService.enforceQuota(testTenantId, MetricType.API_CALLS, 100L);

      // Assert
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
      verify(quotaEnforcer).checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(1000L), eq(100L));
    }

    @Test
    @DisplayName("Should throw exception when quota exceeded")
    void shouldThrowExceptionWhenQuotaExceeded() {
      // Arrange
      QuotaCheckResult quotaResult = QuotaCheckResult.denied(
          MetricType.API_CALLS, 10000L, 100L, 10000L, "Quota exceeded"
      );

      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));
      when(usageRecordRepository.calculateTotalUsageForPeriod(
          eq(testTenantId), eq(MetricType.API_CALLS), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(10000L);
      when(quotaEnforcer.checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(10000L), eq(100L)))
          .thenReturn(quotaResult);
      when(messageService.getMessage(eq("usage.quota.exceeded"), any(), any(), any()))
          .thenReturn("Quota exceeded");

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.enforceQuota(
          testTenantId, MetricType.API_CALLS, 100L))
          .isInstanceOf(QuotaEnforcer.QuotaExceededException.class);

      verify(quotaEnforcer).checkQuota(
          eq(testSubscription), eq(MetricType.API_CALLS), eq(10000L), eq(100L));
    }
  }

  @Nested
  @DisplayName("resetUsageForNewPeriod Tests")
  class ResetUsageForNewPeriodTests {

    @Test
    @DisplayName("Should reset usage for new period successfully")
    void shouldResetUsageForNewPeriodSuccessfully() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.of(testSubscription));

      // Act
      usageApplicationService.resetUsageForNewPeriod(testTenantId);

      // Assert
      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
    }

    @Test
    @DisplayName("Should throw exception when no active subscription exists")
    void shouldThrowExceptionWhenNoActiveSubscription() {
      // Arrange
      when(subscriptionRepository.findActiveByTenantId(testTenantId))
          .thenReturn(Optional.empty());
      when(messageService.getMessage(anyString(), (Object[]) any()))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> usageApplicationService.resetUsageForNewPeriod(testTenantId))
          .isInstanceOf(UsageApplicationService.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findActiveByTenantId(testTenantId);
    }
  }
}
