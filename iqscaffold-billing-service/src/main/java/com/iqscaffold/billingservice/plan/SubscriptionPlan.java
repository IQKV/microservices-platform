package com.iqscaffold.billingservice.plan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * SubscriptionPlan aggregate root entity (public schema).
 * Defines subscription tiers, pricing, and quotas.
 */
@Entity
@Table(name = "subscription_plans", schema = "public")
public class SubscriptionPlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Entity implementation will be added in subsequent tasks
}
