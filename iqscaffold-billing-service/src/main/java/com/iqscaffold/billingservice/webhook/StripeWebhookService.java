package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.payment.PaymentService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {
  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);
  
  private final PaymentService paymentService;
  private final com.iqscaffold.billingservice.payout.PayoutService payoutService;
  private final BillingProperties billingProperties;

  public StripeWebhookService(
      PaymentService paymentService, 
      com.iqscaffold.billingservice.payout.PayoutService payoutService,
      BillingProperties billingProperties
  ) {
    this.paymentService = paymentService;
    this.payoutService = payoutService;
    this.billingProperties = billingProperties;
  }

  public void processWebhook(String payload, String sigHeader) {
    Event event;
    try {
      event = Webhook.constructEvent(
          payload, sigHeader, billingProperties.payment().stripe().webhookSecret()
      );
    } catch (SignatureVerificationException e) {
      logger.error("Invalid signature for webhook", e);
      throw new IllegalArgumentException("Invalid signature");
    } catch (Exception e) {
       logger.error("Webhook parsing failed", e);
       throw new IllegalArgumentException("Webhook parsing failed");
    }

    // Handle the event
    switch (event.getType()) {
      case "payment_intent.succeeded" -> handlePaymentSuccess(event);
      case "payment_intent.payment_failed" -> handlePaymentFailure(event);
      case "charge.refunded" -> handleRefund(event);
      case "payout.paid" -> handlePayout(event);
      default -> logger.debug("Unhandled event type: {}", event.getType());
    }
  }

  private void handlePaymentSuccess(Event event) {
    PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
    if (intent != null) {
        paymentService.updateStatus(intent.getId(), BillingConstants.PaymentStatus.SUCCEEDED);
    }
  }

  private void handlePaymentFailure(Event event) {
    PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
    if (intent != null) {
        paymentService.updateStatus(intent.getId(), BillingConstants.PaymentStatus.FAILED);
    }
  }

  private void handleRefund(Event event) {
      // Logic to sync refund status to Payment entity if not already done via API
      // For now, simplistically relying on PaymentService to guard state overrides if needed,
      // or we can explicitly look up payment by charge ID.
      // Given Payment Entity stores PaymentIntentID, we might need to fetch PI from Charge.
      // But typically charge.refunded comes with a Charge object which refers to PI.
      // For simplicity in this demo, skipping complex reverse lookups.
  }

  private void handlePayout(Event event) {
      com.stripe.model.Payout stripePayout = (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject().orElse(null);
      if (stripePayout != null) {
          payoutService.processPayout(stripePayout);
      }
  }
}
