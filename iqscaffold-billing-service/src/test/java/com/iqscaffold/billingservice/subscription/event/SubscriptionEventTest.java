package com.iqscaffold.billingservice.subscription.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubscriptionEventTest {

  @Test
  void constructor_shouldCreateEventWithAllFields() {
    // Given
    String eventId = "event-123";
    SubscriptionEvent.SubscriptionEventType eventType = SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED;
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    UUID planId = UUID.randomUUID();
    String planName = "Premium Plan";
    String subscriptionStatus = "ACTIVE";
    String stripeSubscriptionId = "sub_123";
    Map<String, Object> metadata = Map.of("key", "value");
    Instant timestamp = Instant.now();

    // When
    SubscriptionEvent event = new SubscriptionEvent(
        eventId, eventType, subscriptionId, tenantId, planId, planName,
        subscriptionStatus, stripeSubscriptionId, metadata, timestamp
    );

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(eventType, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(planId, event.getPlanId());
    assertEquals(planName, event.getPlanName());
    assertEquals(subscriptionStatus, event.getSubscriptionStatus());
    assertEquals(stripeSubscriptionId, event.getStripeSubscriptionId());
    assertEquals(metadata, event.getMetadata());
    assertEquals(timestamp, event.getTimestamp());
  }

  @Test
  void defaultConstructor_shouldCreateEmptyEvent() {
    // When
    SubscriptionEvent event = new SubscriptionEvent();

    // Then
    assertNull(event.getEventId());
    assertNull(event.getEventType());
    assertNull(event.getSubscriptionId());
    assertNull(event.getTenantId());
    assertNull(event.getPlanId());
    assertNull(event.getPlanName());
    assertNull(event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNull(event.getTimestamp());
  }

  @Test
  void created_shouldCreateSubscriptionCreatedEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    UUID planId = UUID.randomUUID();
    String planName = "Premium Plan";
    String stripeSubscriptionId = "sub_123";

    // When
    SubscriptionEvent event = SubscriptionEvent.created(
        subscriptionId, tenantId, planId, planName, stripeSubscriptionId
    );

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(planId, event.getPlanId());
    assertEquals(planName, event.getPlanName());
    assertEquals("ACTIVE", event.getSubscriptionStatus());
    assertEquals(stripeSubscriptionId, event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void updated_shouldCreateSubscriptionUpdatedEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    UUID planId = UUID.randomUUID();
    String planName = "Premium Plan";
    String status = "PAUSED";

    // When
    SubscriptionEvent event = SubscriptionEvent.updated(
        subscriptionId, tenantId, planId, planName, status
    );

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_UPDATED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(planId, event.getPlanId());
    assertEquals(planName, event.getPlanName());
    assertEquals(status, event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void canceled_shouldCreateSubscriptionCanceledEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    String status = "CANCELED";

    // When
    SubscriptionEvent event = SubscriptionEvent.canceled(subscriptionId, tenantId, status);

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CANCELED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertNull(event.getPlanId());
    assertNull(event.getPlanName());
    assertEquals(status, event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void paused_shouldCreateSubscriptionPausedEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";

    // When
    SubscriptionEvent event = SubscriptionEvent.paused(subscriptionId, tenantId);

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PAUSED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertNull(event.getPlanId());
    assertNull(event.getPlanName());
    assertEquals("PAUSED", event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void resumed_shouldCreateSubscriptionResumedEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";

    // When
    SubscriptionEvent event = SubscriptionEvent.resumed(subscriptionId, tenantId);

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_RESUMED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertNull(event.getPlanId());
    assertNull(event.getPlanName());
    assertEquals("ACTIVE", event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNull(event.getMetadata());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void trialEnding_shouldCreateSubscriptionTrialEndingEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    Instant trialEnd = Instant.now().plusSeconds(86400); // 1 day from now

    // When
    SubscriptionEvent event = SubscriptionEvent.trialEnding(subscriptionId, tenantId, trialEnd);

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_TRIAL_ENDING, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertNull(event.getPlanId());
    assertNull(event.getPlanName());
    assertEquals("TRIALING", event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNotNull(event.getMetadata());
    assertEquals(trialEnd.toString(), event.getMetadata().get("trial_end"));
    assertNotNull(event.getTimestamp());
  }

  @Test
  void planChanged_shouldCreateSubscriptionPlanChangedEvent() {
    // Given
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "tenant-123";
    UUID oldPlanId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();
    String newPlanName = "Enterprise Plan";

    // When
    SubscriptionEvent event = SubscriptionEvent.planChanged(
        subscriptionId, tenantId, oldPlanId, newPlanId, newPlanName
    );

    // Then
    assertNotNull(event.getEventId());
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PLAN_CHANGED, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(newPlanId, event.getPlanId());
    assertEquals(newPlanName, event.getPlanName());
    assertEquals("ACTIVE", event.getSubscriptionStatus());
    assertNull(event.getStripeSubscriptionId());
    assertNotNull(event.getMetadata());
    assertEquals(oldPlanId.toString(), event.getMetadata().get("old_plan_id"));
    assertEquals(newPlanId.toString(), event.getMetadata().get("new_plan_id"));
    assertNotNull(event.getTimestamp());
  }

  @Test
  void setters_shouldUpdateEventFields() {
    // Given
    SubscriptionEvent event = new SubscriptionEvent();
    String eventId = "new-event-123";
    SubscriptionEvent.SubscriptionEventType eventType = SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_UPDATED;
    UUID subscriptionId = UUID.randomUUID();
    String tenantId = "new-tenant-123";
    UUID planId = UUID.randomUUID();
    String planName = "New Plan";
    String subscriptionStatus = "UPDATED";
    String stripeSubscriptionId = "sub_new_123";
    Map<String, Object> metadata = Map.of("updated", "true");
    Instant timestamp = Instant.now();

    // When
    event.setEventId(eventId);
    event.setEventType(eventType);
    event.setSubscriptionId(subscriptionId);
    event.setTenantId(tenantId);
    event.setPlanId(planId);
    event.setPlanName(planName);
    event.setSubscriptionStatus(subscriptionStatus);
    event.setStripeSubscriptionId(stripeSubscriptionId);
    event.setMetadata(metadata);
    event.setTimestamp(timestamp);

    // Then
    assertEquals(eventId, event.getEventId());
    assertEquals(eventType, event.getEventType());
    assertEquals(subscriptionId, event.getSubscriptionId());
    assertEquals(tenantId, event.getTenantId());
    assertEquals(planId, event.getPlanId());
    assertEquals(planName, event.getPlanName());
    assertEquals(subscriptionStatus, event.getSubscriptionStatus());
    assertEquals(stripeSubscriptionId, event.getStripeSubscriptionId());
    assertEquals(metadata, event.getMetadata());
    assertEquals(timestamp, event.getTimestamp());
  }

  @Test
  void subscriptionEventType_shouldHaveAllExpectedValues() {
    // When & Then
    assertEquals(7, SubscriptionEvent.SubscriptionEventType.values().length);
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CREATED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_CREATED"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_UPDATED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_UPDATED"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_CANCELED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_CANCELED"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PAUSED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_PAUSED"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_RESUMED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_RESUMED"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_TRIAL_ENDING, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_TRIAL_ENDING"));
    assertEquals(SubscriptionEvent.SubscriptionEventType.SUBSCRIPTION_PLAN_CHANGED, 
        SubscriptionEvent.SubscriptionEventType.valueOf("SUBSCRIPTION_PLAN_CHANGED"));
  }
}