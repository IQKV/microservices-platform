package com.iqscaffold.billingservice.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for subscription business logic.
 */
@Service
public class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  private final SubscriptionRepository subscriptionRepository;

  public SubscriptionService(final SubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  // Service methods will be added in subsequent tasks
}
