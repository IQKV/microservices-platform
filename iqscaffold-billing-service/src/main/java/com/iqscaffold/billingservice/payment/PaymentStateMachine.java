package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.InvalidPaymentStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentStateMachine {
  private static final Logger logger = LoggerFactory.getLogger(PaymentStateMachine.class);
  private final MessageService messageService;

  public PaymentStateMachine(final MessageService messageService) {
    this.messageService = messageService;
  }

  public void validateTransition(String currentStatus, String targetStatus) {
    if (isValidTransition(currentStatus, targetStatus)) {
      return;
    }

    String msg = messageService.getMessage(
        BillingConstants.ErrorKeys.INVALID_STATUS_TRANSITION,
        new Object[] {currentStatus, targetStatus}
    );
    logger.warn("Invalid transition attempt: {} -> {}", currentStatus, targetStatus);
    throw new InvalidPaymentStateException(msg);
  }

  private boolean isValidTransition(String from, String to) {
    if (from == null) {
      return BillingConstants.PaymentStatus.PENDING.equals(to); // Initial creation
    }

    return switch (from) {
      case BillingConstants.PaymentStatus.PENDING -> to.equals(BillingConstants.PaymentStatus.SUCCEEDED)
                                                     || to.equals(BillingConstants.PaymentStatus.FAILED);

      case BillingConstants.PaymentStatus.SUCCEEDED -> to.equals(BillingConstants.PaymentStatus.REFUNDED)
                                                       || to.equals(BillingConstants.PaymentStatus.PARTIALLY_REFUNDED);

      case BillingConstants.PaymentStatus.FAILED -> false; // Terminal state
      case BillingConstants.PaymentStatus.REFUNDED -> false; // Terminal state
      default -> false;
    };
  }
}
