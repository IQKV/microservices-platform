package com.iqscaffold.billingservice.subscription;

import java.util.UUID;

/**
 * Exception thrown when a subscription is not found.
 */
public class SubscriptionNotFoundException extends RuntimeException {

  public SubscriptionNotFoundException(UUID id) {
    super("Subscription not found with id: " + id);
  }

  public SubscriptionNotFoundException(String message) {
    super(message);
  }
}
