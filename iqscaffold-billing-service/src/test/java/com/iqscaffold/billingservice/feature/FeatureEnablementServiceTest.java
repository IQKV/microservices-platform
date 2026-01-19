package com.iqscaffold.billingservice.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.SubscriptionInterval;
import com.iqscaffold.billingservice.subscription.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureEnablementServiceTest {

  @Mock
  private TenantSubscriptionRepository subscriptionRepository;

  @Mock
  private PlanFeatureRepository planFeatureRepository;

  @Mock
  private FeatureDefinitionRepository featureDefinitionRepository;

  @Mock
  private FeatureUsageTrackingService usageTrackingService;

  @InjectMocks
  private FeatureEnablementService featureEnablementService;

  private SubscriptionPlan testPlan;
  private TenantSubscription testSubscription;
  private FeatureDefinition booleanFeature;
  private FeatureDefinition quotaFeature;
  private PlanFeature enabledPlanFeature;
  private PlanFeature quotaPlanFeature;

  @BeforeEach
  void setUp() {
    // Create test plan
    testPlan = new SubscriptionPlan();
    testPlan.setId(UUID.randomUUID());
    testPlan.setName("Pro Plan");
    testPlan.setAmount(new BigDecimal("29.99"));
    testPlan.setCurrency("USD");
    testPlan.setInterval(SubscriptionInterval.MONTH);

    // Create test subscription
    testSubscription = new TenantSubscription();
    testSubscription.setId(UUID.randomUUID());
    testSubscription.setPlan(testPlan);
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);

    // Create test features
    booleanFeature = new FeatureDefinition("advanced_analytics", "Advanced Analytics", 
        "Access to advanced reporting", FeatureType.BOOLEAN);
    
    quotaFeature = new FeatureDefinition("api_calls_monthly", "API Calls per Month", 
        "Monthly API call quota", FeatureType.QUOTA);
    quotaFeature.setMetadata(Map.of("defaultQuota", 10000));

    // Create plan features
    enabledPlanFeature = new PlanFeature(testPlan, booleanFeature, true);
    
    quotaPlanFeature = new PlanFeature(testPlan, quotaFeature, true, 
        Map.of("quota", 50000L));
  }

  @Test
  @DisplayName("Should return true when feature is enabled for tenant")
  void shouldReturnTrueWhenFeatureIsEnabled() {
    // Arrange
    String tenantId = "tenant-123";
    String featureKey = "advanced_analytics";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.of(testSubscription));
    when(planFeatureRepository.isFeatureEnabledForPlan(testPlan.getId(), featureKey))
        .thenReturn(true);
    when(featureDefinitionRepository.findByFeatureKey(featureKey))
        .thenReturn(Optional.of(booleanFeature));

    // Act
    boolean result = featureEnablementService.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false when tenant has no active subscription")
  void shouldReturnFalseWhenNoActiveSubscription() {
    // Arrange
    String tenantId = "tenant-123";
    String featureKey = "advanced_analytics";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.empty());

    // Act
    boolean result = featureEnablementService.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should return false when feature is not enabled in plan")
  void shouldReturnFalseWhenFeatureNotEnabledInPlan() {
    // Arrange
    String tenantId = "tenant-123";
    String featureKey = "advanced_analytics";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.of(testSubscription));
    when(planFeatureRepository.isFeatureEnabledForPlan(testPlan.getId(), featureKey))
        .thenReturn(false);

    // Act
    boolean result = featureEnablementService.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should build complete feature context for tenant")
  void shouldBuildCompleteFeatureContext() {
    // Arrange
    String tenantId = "tenant-123";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.of(testSubscription));
    when(planFeatureRepository.findEnabledFeaturesByPlanId(testPlan.getId()))
        .thenReturn(List.of(enabledPlanFeature, quotaPlanFeature));

    // Act
    FeatureContext context = featureEnablementService.getFeatureContext(tenantId);

    // Assert
    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.getPlanId()).isEqualTo(testPlan.getId().toString());
    assertThat(context.getPlanName()).isEqualTo("Pro Plan");
    assertThat(context.getEnabledFeatures()).contains("advanced_analytics");
    assertThat(context.getQuota("api_calls_monthly")).isEqualTo(50000L);
    assertThat(context.hasAnyFeatures()).isTrue();
  }

  @Test
  @DisplayName("Should return empty context when tenant has no subscription")
  void shouldReturnEmptyContextWhenNoSubscription() {
    // Arrange
    String tenantId = "tenant-123";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.empty());

    // Act
    FeatureContext context = featureEnablementService.getFeatureContext(tenantId);

    // Assert
    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.hasAnyFeatures()).isFalse();
    assertThat(context.getEnabledFeatures()).isEmpty();
    assertThat(context.getQuotas()).isEmpty();
  }

  @Test
  @DisplayName("Should record feature usage")
  void shouldRecordFeatureUsage() {
    // Arrange
    String tenantId = "tenant-123";
    String featureKey = "advanced_analytics";
    String endpoint = "/api/v1/analytics/reports";

    // Act
    featureEnablementService.recordFeatureUsage(tenantId, featureKey, endpoint);

    // Assert
    verify(usageTrackingService).recordUsage(tenantId, featureKey, endpoint);
  }

  @Test
  @DisplayName("Should get feature quota from context")
  void shouldGetFeatureQuotaFromContext() {
    // Arrange
    String tenantId = "tenant-123";
    String featureKey = "api_calls_monthly";

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.of(testSubscription));
    when(planFeatureRepository.findEnabledFeaturesByPlanId(testPlan.getId()))
        .thenReturn(List.of(quotaPlanFeature));

    // Act
    Long quota = featureEnablementService.getFeatureQuota(tenantId, featureKey);

    // Assert
    assertThat(quota).isEqualTo(50000L);
  }

  @Test
  @DisplayName("Should validate feature dependencies")
  void shouldValidateFeatureDependencies() {
    // Arrange
    String tenantId = "tenant-123";

    // Create feature with dependency
    FeatureDefinition dependentFeature = new FeatureDefinition("premium_reports", "Premium Reports", 
        "Advanced reporting features", FeatureType.BOOLEAN);
    dependentFeature.setDependencies(List.of("advanced_analytics"));

    PlanFeature dependentPlanFeature = new PlanFeature(testPlan, dependentFeature, true);

    when(subscriptionRepository.findActiveSubscriptionByTenantId(tenantId))
        .thenReturn(Optional.of(testSubscription));
    when(planFeatureRepository.findEnabledFeaturesByPlanId(testPlan.getId()))
        .thenReturn(List.of(enabledPlanFeature, dependentPlanFeature));
    
    // Mock the dependency validation calls
    when(featureDefinitionRepository.findByFeatureKey("advanced_analytics"))
        .thenReturn(Optional.of(booleanFeature));
    when(featureDefinitionRepository.findByFeatureKey("premium_reports"))
        .thenReturn(Optional.of(dependentFeature));
    when(planFeatureRepository.isFeatureEnabledForPlan(testPlan.getId(), "advanced_analytics"))
        .thenReturn(true);

    // Act
    FeatureContext context = featureEnablementService.getFeatureContext(tenantId);

    // Assert
    assertThat(context.getEnabledFeatures()).contains("advanced_analytics", "premium_reports");
  }
}