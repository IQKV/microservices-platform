package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionStateMachineTest {

  private SubscriptionStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    stateMachine = new SubscriptionStateMachine();
  }

  @Test
  void isTransitionAllowed_shouldAllowValidTransitionsFromIncomplete() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.INCOMPLETE;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidTransitionsFromIncomplete() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.INCOMPLETE;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldAllowValidTransitionsFromTrialing() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.TRIALING;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidTransitionsFromTrialing() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.TRIALING;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldAllowValidTransitionsFromActive() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.ACTIVE;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidTransitionsFromActive() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.ACTIVE;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldAllowValidTransitionsFromPastDue() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.PAST_DUE;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidTransitionsFromPastDue() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.PAST_DUE;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
  }

  @Test
  void isTransitionAllowed_shouldAllowValidTransitionsFromPaused() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.PAUSED;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertTrue(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidTransitionsFromPaused() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.PAUSED;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldRejectAllTransitionsFromCanceled() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.CANCELED;

    // When & Then - Terminal state, no transitions allowed
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldRejectAllTransitionsFromUnpaid() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.UNPAID;

    // When & Then - Terminal state, no transitions allowed
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.ACTIVE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, SubscriptionStatus.CANCELED));
  }

  @Test
  void isTransitionAllowed_shouldAllowInitialCreationFromNull() {
    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(null, SubscriptionStatus.INCOMPLETE));
    assertTrue(stateMachine.isTransitionAllowed(null, SubscriptionStatus.TRIALING));
    assertTrue(stateMachine.isTransitionAllowed(null, SubscriptionStatus.ACTIVE));
  }

  @Test
  void isTransitionAllowed_shouldRejectInvalidInitialStatesFromNull() {
    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(null, SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTransitionAllowed(null, SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isTransitionAllowed(null, SubscriptionStatus.CANCELED));
    assertFalse(stateMachine.isTransitionAllowed(null, SubscriptionStatus.UNPAID));
  }

  @Test
  void isTransitionAllowed_shouldReturnTrueForSameStatus() {
    // Given
    SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    // When & Then
    assertTrue(stateMachine.isTransitionAllowed(status, status));
  }

  @Test
  void isTransitionAllowed_shouldReturnFalseForNullNewStatus() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.ACTIVE;

    // When & Then
    assertFalse(stateMachine.isTransitionAllowed(currentStatus, null));
  }

  @Test
  void validateTransition_shouldPassForValidTransition() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.ACTIVE;
    SubscriptionStatus newStatus = SubscriptionStatus.PAUSED;

    // When & Then - Should not throw exception
    stateMachine.validateTransition(currentStatus, newStatus);
  }

  @Test
  void validateTransition_shouldThrowExceptionForInvalidTransition() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.CANCELED;
    SubscriptionStatus newStatus = SubscriptionStatus.ACTIVE;

    // When & Then
    InvalidSubscriptionStateException exception = assertThrows(
        InvalidSubscriptionStateException.class,
        () -> stateMachine.validateTransition(currentStatus, newStatus)
    );
    
    assertTrue(exception.getMessage().contains("Invalid subscription state transition"));
    assertTrue(exception.getMessage().contains("CANCELED"));
    assertTrue(exception.getMessage().contains("ACTIVE"));
  }

  @Test
  void getAllowedNextStates_shouldReturnCorrectStatesForIncomplete() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.INCOMPLETE;

    // When
    Set<SubscriptionStatus> allowedStates = stateMachine.getAllowedNextStates(currentStatus);

    // Then
    assertEquals(3, allowedStates.size());
    assertTrue(allowedStates.contains(SubscriptionStatus.TRIALING));
    assertTrue(allowedStates.contains(SubscriptionStatus.ACTIVE));
    assertTrue(allowedStates.contains(SubscriptionStatus.CANCELED));
  }

  @Test
  void getAllowedNextStates_shouldReturnCorrectStatesForActive() {
    // Given
    SubscriptionStatus currentStatus = SubscriptionStatus.ACTIVE;

    // When
    Set<SubscriptionStatus> allowedStates = stateMachine.getAllowedNextStates(currentStatus);

    // Then
    assertEquals(3, allowedStates.size());
    assertTrue(allowedStates.contains(SubscriptionStatus.PAST_DUE));
    assertTrue(allowedStates.contains(SubscriptionStatus.PAUSED));
    assertTrue(allowedStates.contains(SubscriptionStatus.CANCELED));
  }

  @Test
  void getAllowedNextStates_shouldReturnEmptySetForTerminalStates() {
    // When
    Set<SubscriptionStatus> canceledStates = stateMachine.getAllowedNextStates(SubscriptionStatus.CANCELED);
    Set<SubscriptionStatus> unpaidStates = stateMachine.getAllowedNextStates(SubscriptionStatus.UNPAID);

    // Then
    assertTrue(canceledStates.isEmpty());
    assertTrue(unpaidStates.isEmpty());
  }

  @Test
  void getAllowedNextStates_shouldReturnInitialStatesForNull() {
    // When
    Set<SubscriptionStatus> allowedStates = stateMachine.getAllowedNextStates(null);

    // Then
    assertEquals(3, allowedStates.size());
    assertTrue(allowedStates.contains(SubscriptionStatus.INCOMPLETE));
    assertTrue(allowedStates.contains(SubscriptionStatus.TRIALING));
    assertTrue(allowedStates.contains(SubscriptionStatus.ACTIVE));
  }

  @Test
  void isTerminalState_shouldReturnTrueForTerminalStates() {
    // When & Then
    assertTrue(stateMachine.isTerminalState(SubscriptionStatus.CANCELED));
    assertTrue(stateMachine.isTerminalState(SubscriptionStatus.UNPAID));
  }

  @Test
  void isTerminalState_shouldReturnFalseForNonTerminalStates() {
    // When & Then
    assertFalse(stateMachine.isTerminalState(SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isTerminalState(SubscriptionStatus.TRIALING));
    assertFalse(stateMachine.isTerminalState(SubscriptionStatus.ACTIVE));
    assertFalse(stateMachine.isTerminalState(SubscriptionStatus.PAST_DUE));
    assertFalse(stateMachine.isTerminalState(SubscriptionStatus.PAUSED));
  }

  @Test
  void isActiveBillingState_shouldReturnTrueForBillingStates() {
    // When & Then
    assertTrue(stateMachine.isActiveBillingState(SubscriptionStatus.ACTIVE));
    assertTrue(stateMachine.isActiveBillingState(SubscriptionStatus.TRIALING));
    assertTrue(stateMachine.isActiveBillingState(SubscriptionStatus.PAST_DUE));
  }

  @Test
  void isActiveBillingState_shouldReturnFalseForNonBillingStates() {
    // When & Then
    assertFalse(stateMachine.isActiveBillingState(SubscriptionStatus.INCOMPLETE));
    assertFalse(stateMachine.isActiveBillingState(SubscriptionStatus.PAUSED));
    assertFalse(stateMachine.isActiveBillingState(SubscriptionStatus.CANCELED));
    assertFalse(stateMachine.isActiveBillingState(SubscriptionStatus.UNPAID));
  }
}