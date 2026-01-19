package com.iqscaffold.billingservice.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Objects for subscription operations.
 */
public class SubscriptionDtos {
  private SubscriptionDtos() {
  }

  /**
   * Request to create a new subscription.
   *
   * @param planId          The UUID of the subscription plan
   * @param paymentMethodId Optional Stripe payment method ID
   * @param trialDays       Optional number of trial days (overrides plan default)
   * @param metadata        Additional metadata for the subscription
   */
  public record CreateSubscriptionRequest(
      @NotNull UUID planId,
      String paymentMethodId,
      Integer trialDays,
      java.util.Map<String, String> metadata) {
  }

  /**
   * Request to update an existing subscription.
   *
   * @param newPlanId       Optional new plan ID to switch to
   * @param paymentMethodId Optional new payment method
   * @param metadata        Optional metadata updates
   */
  public record UpdateSubscriptionRequest(
      UUID newPlanId,
      String paymentMethodId,
      java.util.Map<String, String> metadata) {
  }

  /**
   * Response containing subscription details.
   *
   * @param id                   The subscription UUID
   * @param tenantId             The tenant ID
   * @param planId               The subscription plan UUID
   * @param planName             The name of the subscription plan
   * @param status               The subscription status
   * @param stripeSubscriptionId The Stripe subscription ID
   * @param stripeCustomerId     The Stripe customer ID
   * @param currentPeriodStart   Start of the current billing period
   * @param currentPeriodEnd     End of the current billing period
   * @param canceledAt           When the subscription was canceled (if applicable)
   * @param trialStart           Trial period start (if applicable)
   * @param trialEnd             Trial period end (if applicable)
   * @param createdAt            When the subscription was created
   * @param updatedAt            When the subscription was last updated
   */
  public record SubscriptionResponse(
      UUID id,
      String tenantId,
      UUID planId,
      String planName,
      String status,
      String stripeSubscriptionId,
      String stripeCustomerId,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant canceledAt,
      Instant trialStart,
      Instant trialEnd,
      Instant createdAt,
      Instant updatedAt) {
  }

  /**
   * Subscription plan details response.
   *
   * @param id              The plan UUID
   * @param name            Plan name
   * @param description     Plan description
   * @param priceAmount     Price amount
   * @param currency        Currency code
   * @param interval        Billing interval (monthly, yearly)
   * @param intervalCount   Number of intervals between billings
   * @param trialDays       Number of trial days
   * @param isActive        Whether the plan is active
   * @param stripePriceId   Stripe price ID
   * @param stripeProductId Stripe product ID
   */
  public record PlanResponse(
      UUID id,
      String name,
      String description,
      BigDecimal priceAmount,
      String currency,
      String interval,
      Integer intervalCount,
      Integer trialDays,
      Boolean isActive,
      String stripePriceId,
      String stripeProductId) {
  }

  /**
   * Request to create or update a subscription plan.
   *
   * @param name          Plan name
   * @param description   Plan description
   * @param priceAmount   Price amount
   * @param currency      Currency code
   * @param interval      Billing interval
   * @param intervalCount Number of intervals
   * @param trialDays     Trial days
   * @param isActive      Active status
   * @param metadata      Additional metadata
   */
  public record UpsertPlanRequest(
      @NotBlank String name,
      String description,
      @NotNull BigDecimal priceAmount,
      @NotBlank String currency,
      @NotBlank String interval,
      Integer intervalCount,
      Integer trialDays,
      Boolean isActive,
      java.util.Map<String, String> metadata) {
  }

  /**
   * Invoice response.
   *
   * @param id               Invoice UUID
   * @param subscriptionId   Subscription UUID
   * @param tenantId         Tenant ID
   * @param stripeInvoiceId  Stripe invoice ID
   * @param invoiceNumber    Invoice number
   * @param status           Invoice status
   * @param amountDue        Amount due
   * @param amountPaid       Amount paid
   * @param currency         Currency code
   * @param dueDate          Due date
   * @param paidAt           Paid timestamp
   * @param hostedInvoiceUrl Hosted invoice URL
   * @param invoicePdfUrl    Invoice PDF URL
   * @param createdAt        Created timestamp
   * @param updatedAt        Updated timestamp
   */
  public record InvoiceResponse(
      UUID id,
      UUID subscriptionId,
      String tenantId,
      String stripeInvoiceId,
      String invoiceNumber,
      String status,
      BigDecimal amountDue,
      BigDecimal amountPaid,
      String currency,
      Instant dueDate,
      Instant paidAt,
      String hostedInvoiceUrl,
      String invoicePdfUrl,
      Instant createdAt,
      Instant updatedAt) {
  }
}
