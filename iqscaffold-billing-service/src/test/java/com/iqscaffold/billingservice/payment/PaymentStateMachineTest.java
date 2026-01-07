package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentStateMachineTest {

  @Mock
  private MessageService messageService;

  private PaymentStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    stateMachine = new PaymentStateMachine(messageService);
    lenient().when(messageService.getMessage(any(String.class), any(Object[].class)))
        .thenReturn("Invalid transition");
  }

  @Test
  void shouldAllowInitialTransitionToPending() {
    assertDoesNotThrow(() ->
        stateMachine.validateTransition(null, BillingConstants.PaymentStatus.PENDING)
    );
  }

  @Test
  void shouldRejectInitialTransitionToNonPending() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition(null, BillingConstants.PaymentStatus.SUCCEEDED)
    );
  }

  @Test
  void shouldAllowPendingToSucceeded() {
    assertDoesNotThrow(() ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.PENDING,
            BillingConstants.PaymentStatus.SUCCEEDED
        )
    );
  }

  @Test
  void shouldAllowPendingToFailed() {
    assertDoesNotThrow(() ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.PENDING,
            BillingConstants.PaymentStatus.FAILED
        )
    );
  }

  @Test
  void shouldRejectPendingToRefunded() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.PENDING,
            BillingConstants.PaymentStatus.REFUNDED
        )
    );
  }

  @Test
  void shouldAllowSucceededToRefunded() {
    assertDoesNotThrow(() ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.SUCCEEDED,
            BillingConstants.PaymentStatus.REFUNDED
        )
    );
  }

  @Test
  void shouldAllowSucceededToPartiallyRefunded() {
    assertDoesNotThrow(() ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.SUCCEEDED,
            BillingConstants.PaymentStatus.PARTIALLY_REFUNDED
        )
    );
  }

  @Test
  void shouldRejectSucceededToFailed() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.SUCCEEDED,
            BillingConstants.PaymentStatus.FAILED
        )
    );
  }

  @Test
  void shouldRejectTransitionsFromFailed() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.FAILED,
            BillingConstants.PaymentStatus.SUCCEEDED
        )
    );
  }

  @Test
  void shouldRejectTransitionsFromRefunded() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition(
            BillingConstants.PaymentStatus.REFUNDED,
            BillingConstants.PaymentStatus.SUCCEEDED
        )
    );
  }

  @Test
  void shouldRejectUnknownStatus() {
    assertThrows(InvalidPaymentStateException.class, () ->
        stateMachine.validateTransition("UNKNOWN", BillingConstants.PaymentStatus.SUCCEEDED)
    );
  }
}
