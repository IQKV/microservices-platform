package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Subscription aggregate root entity.
 * Manages subscription lifecycle and state transitions.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Entity implementation will be added in subsequent tasks
}
