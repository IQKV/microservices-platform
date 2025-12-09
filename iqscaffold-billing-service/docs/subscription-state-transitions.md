# Subscription State Transition Tests

## Overview

Comprehensive unit tests for the Subscription aggregate's state machine, covering all valid transitions, invalid transition rejection, and side effects.

## Test Coverage Summary

- **Total Tests**: 57
- **Test Categories**: 10 nested test classes
- **All Tests Passing**: ✅

## State Machine

```
INCOMPLETE → TRIAL, ACTIVE, EXPIRED
TRIAL → ACTIVE, EXPIRED, CANCELED
ACTIVE → PAST_DUE, CANCELED, SUSPENDED, EXPIRED
PAST_DUE → ACTIVE, EXPIRED, SUSPENDED
CANCELED → ACTIVE, EXPIRED
SUSPENDED → ACTIVE, EXPIRED
EXPIRED → (terminal state, no transitions allowed)
```

## Test Categories

### 1. INCOMPLETE State Transitions (3 tests)

- ✅ INCOMPLETE → TRIAL: When trial is activated
- ✅ INCOMPLETE → ACTIVE: When payment is completed
- ✅ INCOMPLETE → EXPIRED: When setup times out

### 2. TRIAL State Transitions (5 tests)

- ✅ TRIAL → ACTIVE: When trial converts to paid
- ✅ TRIAL → EXPIRED: When trial expires without payment
- ✅ TRIAL → CANCELED: When user cancels during trial
- ✅ TRIAL → PAST_DUE: Rejected (invalid transition)
- ✅ TRIAL → SUSPENDED: Rejected (invalid transition)

### 3. ACTIVE State Transitions (7 tests)

- ✅ ACTIVE → PAST_DUE: When payment fails
- ✅ ACTIVE → CANCELED: When user cancels at period end
- ✅ ACTIVE → EXPIRED: When canceled immediately
- ✅ ACTIVE → EXPIRED: When period ends
- ✅ ACTIVE → SUSPENDED: When admin suspends
- ✅ ACTIVE → TRIAL: Rejected (invalid transition)
- ✅ ACTIVE → INCOMPLETE: Rejected (invalid transition)

### 4. PAST_DUE State Transitions (5 tests)

- ✅ PAST_DUE → ACTIVE: When payment succeeds
- ✅ PAST_DUE → EXPIRED: When payment fails permanently
- ✅ PAST_DUE → SUSPENDED: When admin suspends
- ✅ PAST_DUE → CANCELED: Rejected (invalid transition)
- ✅ PAST_DUE → TRIAL: Rejected (invalid transition)

### 5. CANCELED State Transitions (5 tests)

- ✅ CANCELED → ACTIVE: When user reactivates
- ✅ CANCELED → EXPIRED: When period ends
- ✅ CANCELED → PAST_DUE: Rejected (invalid transition)
- ✅ CANCELED → SUSPENDED: Rejected (invalid transition)
- ✅ CANCELED → TRIAL: Rejected (invalid transition)

### 6. SUSPENDED State Transitions (5 tests)

- ✅ SUSPENDED → ACTIVE: When admin unsuspends
- ✅ SUSPENDED → EXPIRED: When admin expires
- ✅ SUSPENDED → PAST_DUE: Rejected (invalid transition)
- ✅ SUSPENDED → CANCELED: Rejected (invalid transition)
- ✅ SUSPENDED → TRIAL: Rejected (invalid transition)

### 7. EXPIRED State Transitions (2 tests)

- ✅ EXPIRED → Any: All transitions rejected (terminal state)
- ✅ EXPIRED: Remains in EXPIRED state

### 8. State Transition Side Effects (11 tests)

- ✅ convertTrialToActive: Resets billing period
- ✅ cancelAtPeriodEnd: Sets canceledAt timestamp
- ✅ cancelAtPeriodEnd: Sets cancelAtPeriodEnd flag
- ✅ cancelImmediately: Sets canceledAt but not flag
- ✅ reactivate: Clears canceledAt timestamp
- ✅ reactivate: Clears cancelAtPeriodEnd flag
- ✅ changePlan: Resets billing period
- ✅ changePlan: Updates plan reference
- ✅ extendTrial: Extends trial end date
- ✅ extendTrial: Extends current period end
- ✅ renew: Advances billing period

### 9. Access Control Based on State (7 tests)

- ✅ TRIAL: Allows access
- ✅ ACTIVE: Allows access
- ✅ PAST_DUE: Allows access (grace period)
- ✅ CANCELED: Allows access until period end
- ✅ EXPIRED: Denies access
- ✅ SUSPENDED: Denies access
- ✅ INCOMPLETE: Denies access

### 10. Plan Change Restrictions Based on State (7 tests)

- ✅ TRIAL: Allows plan changes
- ✅ ACTIVE: Allows plan changes
- ✅ PAST_DUE: Allows plan changes
- ✅ CANCELED: Rejects plan changes
- ✅ EXPIRED: Rejects plan changes
- ✅ SUSPENDED: Rejects plan changes
- ✅ INCOMPLETE: Rejects plan changes

## Key Invariants Tested

1. **State Machine Integrity**: All valid transitions succeed, all invalid transitions are rejected
2. **Terminal State**: EXPIRED is a terminal state with no outgoing transitions
3. **Side Effects**: State transitions properly update related fields (timestamps, flags, periods)
4. **Access Control**: Each state correctly determines feature access
5. **Plan Changes**: Only certain states allow plan modifications
6. **Cancellation Semantics**:
   - `cancelAtPeriodEnd()` → CANCELED with access until period end
   - `cancelImmediately()` → EXPIRED with immediate access revocation

## Test File Location

`backend/iqscaffold-billing-service/src/test/java/com/iqscaffold/billingservice/subscription/SubscriptionStateTransitionTest.java`

## Running the Tests

```bash
# Run only state transition tests
mvn test -Dtest=SubscriptionStateTransitionTest

# Run all subscription tests
mvn test -Dtest=Subscription*Test
```

## Design Notes

- Tests use JUnit 5 nested test classes for logical grouping
- Each state has its own nested class for transitions
- Invalid transitions are explicitly tested to ensure proper rejection
- Side effects are tested separately to verify state changes
- Access control and plan change restrictions are tested comprehensively
