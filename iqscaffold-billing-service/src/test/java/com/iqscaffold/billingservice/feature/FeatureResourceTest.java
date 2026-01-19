package com.iqscaffold.billingservice.feature;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.subscription.SubscriptionService;
import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeatureResource.class)
@TestPropertySource(properties = {
    "iqscaffold.billing.security.jwt.jwk-set-uri=http://localhost:8080/.well-known/jwks.json"
})
class FeatureResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private FeatureEnablementService featureEnablementService;

  @MockBean
  private SubscriptionService subscriptionService;

  @MockBean
  private MessageService messageService;

  private static final String TENANT_ID = "test-tenant";
  private static final String PLAN_NAME = "Pro Plan";

  @BeforeEach
  void setUp() {
    TenantContext.setCurrentTenantId(TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should return user features when authenticated user has active subscription")
  @WithMockUser(authorities = "USER")
  void shouldReturnUserFeaturesWhenActiveSubscription() throws Exception {
    // Given
    var subscription = createMockSubscription();
    var featureContext = createMockFeatureContext();
    var features = createMockFeatures();

    when(subscriptionService.getActiveSubscription()).thenReturn(Optional.of(subscription));
    when(featureEnablementService.getFeatureContext(TENANT_ID)).thenReturn(featureContext);
    when(featureEnablementService.getAllFeatures()).thenReturn(features);

    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.planName").value(PLAN_NAME))
        .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
        .andExpect(jsonPath("$.enabledFeatures").isArray())
        .andExpect(jsonPath("$.enabledFeatures.length()").value(2))
        .andExpect(jsonPath("$.enabledFeatures[0].code").value("advanced_analytics"))
        .andExpect(jsonPath("$.enabledFeatures[0].name").value("Advanced Analytics"))
        .andExpect(jsonPath("$.enabledFeatures[0].enabled").value(true))
        .andExpect(jsonPath("$.enabledFeatures[1].code").value("api_calls"))
        .andExpect(jsonPath("$.enabledFeatures[1].name").value("API Calls"))
        .andExpect(jsonPath("$.enabledFeatures[1].enabled").value(true))
        .andExpect(jsonPath("$.allFeatures").isArray())
        .andExpect(jsonPath("$.allFeatures.length()").value(3))
        .andExpect(jsonPath("$.allFeatures[2].code").value("premium_support"))
        .andExpect(jsonPath("$.allFeatures[2].enabled").value(false));
  }

  @Test
  @DisplayName("Should return enabled features only when requested")
  @WithMockUser(authorities = "USER")
  void shouldReturnEnabledFeaturesOnly() throws Exception {
    // Given
    var subscription = createMockSubscription();
    var featureContext = createMockFeatureContext();
    var features = createMockFeatures();

    when(subscriptionService.getActiveSubscription()).thenReturn(Optional.of(subscription));
    when(featureEnablementService.getFeatureContext(TENANT_ID)).thenReturn(featureContext);
    when(featureEnablementService.getAllFeatures()).thenReturn(features);

    // When & Then
    mockMvc.perform(get("/api/v1/features/enabled")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].code").value("advanced_analytics"))
        .andExpect(jsonPath("$[0].enabled").value(true))
        .andExpect(jsonPath("$[1].code").value("api_calls"))
        .andExpect(jsonPath("$[1].enabled").value(true));
  }

  @Test
  @DisplayName("Should return 404 when no active subscription")
  @WithMockUser(authorities = "USER")
  void shouldReturn404WhenNoActiveSubscription() throws Exception {
    // Given
    when(subscriptionService.getActiveSubscription()).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 for enabled features when no active subscription")
  @WithMockUser(authorities = "USER")
  void shouldReturn404ForEnabledFeaturesWhenNoActiveSubscription() throws Exception {
    // Given
    when(subscriptionService.getActiveSubscription()).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/v1/features/enabled")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when not authenticated")
  void shouldReturn401WhenNotAuthenticated() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 404 when user lacks required authority")
  @WithMockUser(authorities = "GUEST")
  void shouldReturn404WhenInsufficientAuthority() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when no tenant context")
  @WithMockUser(authorities = "USER")
  void shouldReturn400WhenNoTenantContext() throws Exception {
    // Given - clear tenant context
    TenantContext.clear();

    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should handle service exceptions gracefully")
  @WithMockUser(authorities = "USER")
  void shouldHandleServiceExceptionsGracefully() throws Exception {
    // Given
    when(subscriptionService.getActiveSubscription()).thenThrow(new RuntimeException("Service error"));

    // When & Then
    mockMvc.perform(get("/api/v1/features/my-features")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }

  private SubscriptionDtos.SubscriptionResponse createMockSubscription() {
    return new SubscriptionDtos.SubscriptionResponse(
        UUID.randomUUID(),
        TENANT_ID,
        UUID.randomUUID(),
        PLAN_NAME,
        "ACTIVE",
        "stripe_sub_123",
        "stripe_cust_123",
        Instant.now(),
        Instant.now().plusSeconds(86400 * 30), // 30 days from now
        null,
        null,
        null,
        Instant.now(),
        Instant.now()
    );
  }

  private FeatureContext createMockFeatureContext() {
    return FeatureContext.builder(TENANT_ID)
        .planId(UUID.randomUUID().toString())
        .planName(PLAN_NAME)
        .enabledFeatures(Set.of("advanced_analytics", "api_calls"))
        .addQuota("api_calls", 10000L)
        .build();
  }

  private List<FeatureDefinition> createMockFeatures() {
    var feature1 = new FeatureDefinition();
    feature1.setFeatureKey("advanced_analytics");
    feature1.setDisplayName("Advanced Analytics");
    feature1.setDescription("Access to advanced reporting and analytics");
    feature1.setType(FeatureType.BOOLEAN);
    feature1.setCategory("ANALYTICS");

    var feature2 = new FeatureDefinition();
    feature2.setFeatureKey("api_calls");
    feature2.setDisplayName("API Calls");
    feature2.setDescription("Monthly API call quota");
    feature2.setType(FeatureType.QUOTA);
    feature2.setCategory("API");

    var feature3 = new FeatureDefinition();
    feature3.setFeatureKey("premium_support");
    feature3.setDisplayName("Premium Support");
    feature3.setDescription("24/7 premium customer support");
    feature3.setType(FeatureType.BOOLEAN);
    feature3.setCategory("SUPPORT");

    return List.of(feature1, feature2, feature3);
  }
}