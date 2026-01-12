package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SubscriptionItem} entities.
 */
@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItem, UUID> {

  /**
   * Find subscription items by tenant subscription ID.
   *
   * @param tenantSubscriptionId Tenant subscription ID
   * @return List of subscription items
   */
  @Query("SELECT si FROM SubscriptionItem si WHERE si.tenantSubscription.id = :tenantSubscriptionId")
  List<SubscriptionItem> findByTenantSubscriptionId(@Param("tenantSubscriptionId") UUID tenantSubscriptionId);

  /**
   * Find a subscription item by Stripe subscription item ID.
   *
   * @param stripeSubscriptionItemId Stripe subscription item ID
   * @return Optional subscription item
   */
  Optional<SubscriptionItem> findByStripeSubscriptionItemId(String stripeSubscriptionItemId);

  /**
   * Delete all items for a tenant subscription.
   *
   * @param tenantSubscriptionId Tenant subscription ID
   */
  @Query("DELETE FROM SubscriptionItem si WHERE si.tenantSubscription.id = :tenantSubscriptionId")
  void deleteByTenantSubscriptionId(@Param("tenantSubscriptionId") UUID tenantSubscriptionId);
}
