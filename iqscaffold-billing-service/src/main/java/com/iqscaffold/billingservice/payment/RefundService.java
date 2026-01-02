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

  @Transactional
  public void processRefund(UUID paymentId) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    Payment payment = paymentRepository.findById(paymentId)
        .filter(p -> p.getTenantId().equals(tenantId))
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

    if (!BillingConstants.PaymentStatus.SUCCEEDED.equals(payment.getStatus())) {
        throw new InvalidPaymentStateException("Cannot refund payment in state: " + payment.getStatus());
    }

    try {
      paymentProvider.refundPayment(
          payment.getPaymentIntentId(), 
          Optional.empty(), // Full refund
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
