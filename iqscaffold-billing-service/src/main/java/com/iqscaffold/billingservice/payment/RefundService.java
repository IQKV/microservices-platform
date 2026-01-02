package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.security.UserContext;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.exception.InvalidPaymentStateException;
import com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the refund process for payments.
 * <p>
 * This service ensures that strictly business-valid refunds are processed:
 * <ul>
 *     <li>Only 'SUCCEEDED' payments can be refunded.</li>
 *     <li>Interacts with Stripe to process the actual money movement.</li>
 *     <li>Updates local payment state to 'REFUNDED'.</li>
 *     <li>Triggers customer notification emails.</li>
 * </ul>
 */
@Service
public class RefundService {
  private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

  private final PaymentRepository paymentRepository;
  private final PaymentProviderAdapter paymentProvider;
  private final PaymentAuditTrailService auditService;
  private final EmailService emailService;

  public RefundService(
      PaymentRepository paymentRepository,
      PaymentProviderAdapter paymentProvider,
      PaymentAuditTrailService auditService,
      EmailService emailService
  ) {
    this.paymentRepository = paymentRepository;
    this.paymentProvider = paymentProvider;
    this.auditService = auditService;
    this.emailService = emailService;
  }

  /**
   * Processes a full refund for a specific payment.
   * <p>
   * Validation:
   * <ul>
   *     <li>Checks if payment exists for the current tenant.</li>
   *     <li>Ensures payment is in {@link BillingConstants.PaymentStatus#SUCCEEDED} state.</li>
   * </ul>
   * <p>
   * Execution Flow:
   * 1. Retrieve Payment.
   * 2. Call Stripe API to refund the associated PaymentIntent.
   * 3. Update local status to {@code REFUNDED}.
   * 4. Audit the transaction.
   * 5. Send confirmation email to the user.
   *
   * @param paymentId The internal UUID of the payment to refund.
   * @throws InvalidPaymentStateException If the payment is not in a refundable state.
   */
  @Transactional
  public void processRefund(UUID paymentId) {
    // Schema isolation ensures we only find payments for the current tenant
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

    if (!BillingConstants.PaymentStatus.SUCCEEDED.equals(payment.getStatus())) {
        throw new InvalidPaymentStateException("Cannot refund payment in state: " + payment.getStatus());
    }

    try {
      paymentProvider.refundPayment(
          payment.getPaymentIntentId(), 
          Optional.empty(), // Full refund
          payment.getCurrency(),
          Optional.ofNullable(payment.getMerchantAccountId())
      );

      payment.setStatus(BillingConstants.PaymentStatus.REFUNDED);
      paymentRepository.save(payment);
      auditService.logPaymentAttempt(payment.getId(), BillingConstants.PaymentStatus.REFUNDED);

      sendRefundEmail(payment);

    } catch (Exception e) {
        logger.error("Refund failed for payment: {}", paymentId, e);
        // We might want to rethrow or handle specifically, but letting it bubble up is fine for now
        throw e;
    }
  }

  private void sendRefundEmail(Payment payment) {
      // In a real system, we'd look up the user email associated with the payment or order
      // Here we grab the current user's email if available, or skip
      UserContext user = SecurityContextHelper.getCurrentUserContext();
      if (user != null && user.email() != null) {
          emailService.sendEmail(
              user.email(),
              "email.payment.refunded.subject",
              "payment-refunded", 
              Map.of(
                  "amount", payment.getAmount(),
                  "currency", payment.getCurrency(),
                  "paymentId", payment.getId()
              ),
              java.util.Locale.ROOT
          );
      }
  }
}
