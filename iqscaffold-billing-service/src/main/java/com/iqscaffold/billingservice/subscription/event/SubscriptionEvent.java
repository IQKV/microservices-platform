package com.iqscaffold.billingservice.subscription.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Domain event for subscription lifecycle changes.
 * <p>
 * Published to RabbitMQ when subscription state changes occur,
 * allowing other services to react to subscription events.
 */
public class SubscriptionEvent {

  private String eventId;
  private SubscriptionEventType eventType;
  private UUID subscriptionId;
  private String tenantId;
  private UUID planId;
  private String planName;
  private String subscriptionStatus;
  private String stripeSubscriptionId;
  private Map<String, Object> metadata;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant timestamp;

  public SubscriptionEvent() {
  }

  public SubscriptionEvent(
      final String eventId,
      final SubscriptionEventType eventType,
      final UUID subscriptionId,
      final String tenantId,
      final UUID planId,
      final String planName,
      final String subscriptionStatus,
      final String stripeSubscriptionId,
      final Map<String, Object> metadata,
      final Instant timestamp) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.subscriptionId = subscriptionId;
    this.tenantId = tenantId;
    this.planId = planId;
    this.planName = planName;
    this.subscriptionStatus = subscriptionStatus;
    this.stripeSubscriptionId = stripeSubscriptionId;
    this.metadata = metadata;
    this.timestamp = timestamp;
  }

  // Static factory methods for common events

  public static SubscriptionEvent created(UUID subscriptionId, String tenantId, UUID planId,
                                          String planName, String stripeSubscriptionId) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_CREATED,
        subscriptionId,
        tenantId,
        planId,
        planName,
        "ACTIVE",
        stripeSubscriptionId,
        null,
        Instant.now()
    );
  }

  public static SubscriptionEvent updated(UUID subscriptionId, String tenantId, UUID planId,
                                          String planName, String status) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_UPDATED,
        subscriptionId,
        tenantId,
        planId,
        planName,
        status,
        null,
        null,
        Instant.now()
    );
  }

  public static SubscriptionEvent canceled(UUID subscriptionId, String tenantId, String status) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_CANCELED,
        subscriptionId,
        tenantId,
        null,
        null,
        status,
        null,
        null,
        Instant.now()
    );
  }

  public static SubscriptionEvent paused(UUID subscriptionId, String tenantId) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_PAUSED,
        subscriptionId,
        tenantId,
        null,
        null,
        "PAUSED",
        null,
        null,
        Instant.now()
    );
  }

  public static SubscriptionEvent resumed(UUID subscriptionId, String tenantId) {
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_RESUMED,
        subscriptionId,
        tenantId,
        null,
        null,
        "ACTIVE",
        null,
        null,
        Instant.now()
    );
  }

  public static SubscriptionEvent trialEnding(UUID subscriptionId, String tenantId, Instant trialEnd) {
    Map<String, Object> metadata = Map.of("trial_end", trialEnd.toString());
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_TRIAL_ENDING,
        subscriptionId,
        tenantId,
        null,
        null,
        "TRIALING",
        null,
        metadata,
        Instant.now()
    );
  }

  public static SubscriptionEvent planChanged(UUID subscriptionId, String tenantId,
                                              UUID oldPlanId, UUID newPlanId, String newPlanName) {
    Map<String, Object> metadata = Map.of(
        "old_plan_id", oldPlanId.toString(),
        "new_plan_id", newPlanId.toString()
    );
    return new SubscriptionEvent(
        UUID.randomUUID().toString(),
        SubscriptionEventType.SUBSCRIPTION_PLAN_CHANGED,
        subscriptionId,
        tenantId,
        newPlanId,
        newPlanName,
        "ACTIVE",
        null,
        metadata,
        Instant.now()
    );
  }

  // Getters and setters

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public SubscriptionEventType getEventType() {
    return eventType;
  }

  public void setEventType(SubscriptionEventType eventType) {
    this.eventType = eventType;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(UUID subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getPlanId() {
    return planId;
  }

  public void setPlanId(UUID planId) {
    this.planId = planId;
  }

  public String getPlanName() {
    return planName;
  }

  public void setPlanName(String planName) {
    this.planName = planName;
  }

  public String getSubscriptionStatus() {
    return subscriptionStatus;
  }

  public void setSubscriptionStatus(String subscriptionStatus) {
    this.subscriptionStatus = subscriptionStatus;
  }

  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  public void setStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Subscription event types enum.
   */
  public enum SubscriptionEventType {
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELED,
    SUBSCRIPTION_PAUSED,
    SUBSCRIPTION_RESUMED,
    SUBSCRIPTION_TRIAL_ENDING,
    SUBSCRIPTION_PLAN_CHANGED
  }
}
