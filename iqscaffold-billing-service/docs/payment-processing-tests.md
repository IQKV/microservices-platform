# Payment Processing Unit Tests

## Overview

This document describes the comprehensive unit test suite for payment processing logic in the billing service. The tests cover payment processing success/failure scenarios, retry logic, refund processing, and idempotency guarantees.

## Test Coverage

### 1. Payment Domain Entity Tests (`PaymentTest.java`)

Tests the core business logic of the Payment aggregate root entity.

#### Payment Creation Tests
- ✅ Create payment with valid parameters
- ✅ Validate invoice is not null
- ✅ Validate tenant ID is not null
- ✅ Validate amount is not null, zero, or negative
- ✅ Validate currency is not null or blank
- ✅ Initialize payment in PENDING status
- ✅ Initialize refunded amount to zero

#### Payment Success Tests
- ✅ Mark payment as succeeded with provider payment ID
- ✅ Clear failure reason on success
- ✅ Prevent marking non-pending payment as succeeded
- ✅ Prevent marking already succeeded payment as succeeded again (idempotency)

#### Payment Failure Tests
- ✅ Mark payment as failed with failure reason
- ✅ Store failure reason
- ✅ Prevent marking non-pending payment as failed
- ✅ Prevent marking already failed payment as failed again

#### Refund Processing Tests
- ✅ Process full refund successfully
- ✅ Process partial refund successfully
- ✅ Process multiple partial refunds
- ✅ Update payment status to REFUNDED
- ✅ Track total refunded amount
- ✅ Prevent refunding non-succeeded payments
- ✅ Prevent refunding failed payments
- ✅ Prevent refund amount exceeding payment amount
- ✅ Prevent total refunds exceeding payment amount
- ✅ Validate refund amount is not null, zero, or negative

#### Refundable Amount Tests
- ✅ Return full amount for succeeded payment
- ✅ Return remaining amount after partial refund
- ✅ Return zero for fully refunded payment
- ✅ Return zero for pending payment
- ✅ Return zero for failed payment

#### Status Message Tests
- ✅ Generate correct message for pending payment
- ✅ Generate correct message for succeeded payment
- ✅ Generate correct message for failed payment with reason
- ✅ Generate correct message for fully refunded payment
- ✅ Generate correct message for partially refunded payment

#### Metadata Tests
- ✅ Add metadata to payment
- ✅ Return defensive copy of metadata (prevent external modification)

### 2. Payment Application Service Tests (`PaymentApplicationServiceTest.java`)

Tests the orchestration logic and coordination with external systems.

#### Process Payment Tests
- ✅ Process payment successfully
- ✅ Return cached result for duplicate idempotency key
- ✅ Handle payment failure from provider
- ✅ Throw exception when invoice not found
- ✅ Throw exception when payment method not found
- ✅ Save payment in PENDING status before provider call
- ✅ Update payment to SUCCEEDED on provider success
- ✅ Update payment to FAILED on provider failure
- ✅ Publish PaymentSucceeded event on success
- ✅ Publish PaymentFailed event on failure
- ✅ Cache result with 24-hour TTL for idempotency
- ✅ Handle provider exceptions gracefully

#### Retry Payment Tests
- ✅ Retry failed payment successfully
- ✅ Create new payment entity for retry
- ✅ Process retry with payment provider
- ✅ Update payment status based on retry result
- ✅ Publish events for retry success/failure
- ✅ Skip retry for non-failed payments
- ✅ Throw exception when payment not found
- ✅ Generate unique idempotency key for retry

#### Refund Payment Tests
- ✅ Refund payment successfully
- ✅ Process full refund
- ✅ Process partial refund
- ✅ Call payment provider refund API
- ✅ Update payment refunded amount
- ✅ Publish PaymentRefunded event
- ✅ Throw exception when payment not found
- ✅ Throw exception when payment not in SUCCEEDED status
- ✅ Use full refundable amount when amount not specified

#### Payment Method Management Tests
- ✅ Add payment method successfully
- ✅ Remove payment method successfully
- ✅ Set default payment method successfully
- ✅ List payment methods with caching
- ✅ Handle payment provider failures
- ✅ Clear cache on payment method changes

#### Payment Retrieval Tests
- ✅ Get payment by ID
- ✅ List payments by invoice
- ✅ List payments by tenant
- ✅ Throw exception when payment not found

### 3. Payment Result Tests (`PaymentResultTest.java`)

Tests the immutable payment result record.

#### Success Result Tests
- ✅ Create successful payment result
- ✅ Validate provider payment ID is not null or blank
- ✅ Validate amount is not null or negative
- ✅ Allow zero amount for successful payment
- ✅ Validate currency is not null or blank
- ✅ Validate processed timestamp is not null

#### Failure Result Tests
- ✅ Create failed payment result
- ✅ Validate failure reason is not null or blank
- ✅ Validate amount is not null or negative
- ✅ Validate currency is not null or blank
- ✅ Auto-generate processed timestamp

#### Direct Constructor Tests
- ✅ Create result with direct constructor
- ✅ Validate successful payment has provider ID
- ✅ Validate failed payment has failure reason

#### Immutability Tests
- ✅ Verify record immutability
- ✅ Support equality comparison
- ✅ Support toString method

#### Common Payment Scenarios Tests
- ✅ Handle card declined scenario
- ✅ Handle expired card scenario
- ✅ Handle fraud detection scenario
- ✅ Handle different currencies (USD, EUR, GBP)
- ✅ Handle large payment amounts
- ✅ Handle small payment amounts

#### Provider-Agnostic Tests
- ✅ Work with Stripe-style payment IDs
- ✅ Work with PayPal-style payment IDs
- ✅ Work with generic UUID payment IDs

## Test Architecture

### Domain-Driven Design Principles

The tests follow DDD principles:

1. **Aggregate Root Testing**: Payment entity tests focus on business invariants
2. **Application Service Testing**: Tests orchestration without business logic
3. **Anti-Corruption Layer**: PaymentResult provides provider-agnostic interface
4. **Event Publishing**: Verify domain events are published correctly

### Test Structure

```
PaymentTest (Domain Entity)
├── Payment Creation Tests
├── Payment Success Tests
├── Payment Failure Tests
├── Refund Processing Tests
├── Refundable Amount Tests
├── Status Message Tests
└── Metadata Tests

PaymentApplicationServiceTest (Application Service)
├── Process Payment Tests
├── Retry Payment Tests
├── Refund Payment Tests
├── Payment Method Management Tests
└── Payment Retrieval Tests

PaymentResultTest (Value Object)
├── Success Result Tests
├── Failure Result Tests
├── Direct Constructor Tests
├── Immutability Tests
├── Common Payment Scenarios Tests
└── Provider-Agnostic Tests
```

## Key Testing Patterns

### 1. Idempotency Testing

```java
@Test
@DisplayName("Should return cached result for duplicate idempotency key")
void shouldReturnCachedResultForDuplicateIdempotencyKey() {
  // Arrange
  String idempotencyKey = "idem_test123";
  PaymentDto cachedDto = createCachedPaymentDto();
  when(valueOperations.get("payment:idempotency:" + idempotencyKey))
      .thenReturn(cachedDto);

  // Act
  CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
      1L, 1L, idempotencyKey
  );
  PaymentDto result = future.get();

  // Assert
  assertThat(result).isEqualTo(cachedDto);
  verify(paymentProviderAdapter, never()).processPayment(any(), any(), any(), any());
}
```

### 2. State Transition Testing

```java
@Test
@DisplayName("Should not allow marking succeeded payment as succeeded again")
void shouldNotAllowMarkingSucceededPaymentAsSucceededAgain() {
  // Arrange
  Payment payment = createTestPayment();
  payment.markAsSucceeded("pi_test123");

  // Act & Assert
  assertThatThrownBy(() -> payment.markAsSucceeded("pi_test456"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Can only mark PENDING payments as succeeded");
}
```

### 3. Refund Logic Testing

```java
@Test
@DisplayName("Should process multiple partial refunds")
void shouldProcessMultiplePartialRefunds() {
  // Arrange
  Payment payment = createTestPayment(new BigDecimal("100.00"));
  payment.markAsSucceeded("pi_test123");

  // Act
  payment.processRefund(new BigDecimal("30.00"));
  payment.processRefund(new BigDecimal("20.00"));
  payment.processRefund(new BigDecimal("50.00"));

  // Assert
  assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
  assertThat(payment.isFullyRefunded()).isTrue();
  assertThat(payment.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
}
```

### 4. Event Publishing Testing

```java
@Test
@DisplayName("Should publish PaymentSucceeded event on success")
void shouldPublishPaymentSucceededEventOnSuccess() {
  // Arrange
  setupSuccessfulPaymentScenario();

  // Act
  paymentApplicationService.processPayment(1L, 1L, "idem_test123").get();

  // Assert
  verify(eventPublisher).publish(any(PaymentSucceeded.class));
}
```

## Running the Tests

### Run all payment tests:
```bash
mvn test -Dtest=Payment*Test
```

### Run specific test class:
```bash
mvn test -Dtest=PaymentTest
mvn test -Dtest=PaymentApplicationServiceTest
mvn test -Dtest=PaymentResultTest
```

### Run with coverage:
```bash
mvn clean test jacoco:report
```

## Test Data Setup

Tests use `TestEntityUtils` for setting entity IDs and creating test fixtures:

```java
@BeforeEach
void setUp() {
  testTenantId = UUID.randomUUID();
  testUserId = UUID.randomUUID();
  
  testPaymentMethod = new PaymentMethod(
      testTenantId, testUserId, PaymentMethodType.CARD, "pm_test123"
  );
  TestEntityUtils.setId(testPaymentMethod, 1L);
  
  testInvoice = createTestInvoice();
  TestEntityUtils.setId(testInvoice, 1L);
}
```

## Mocking Strategy

### Payment Provider Adapter
- Mock external payment provider calls
- Return success/failure results
- Simulate provider exceptions

### Redis Template
- Mock idempotency cache operations
- Verify cache hits/misses
- Test TTL configuration

### Event Publisher
- Verify domain events are published
- Check event payload correctness

### Repositories
- Mock database operations
- Return test entities
- Simulate not found scenarios

## Coverage Goals

- **Line Coverage**: > 90%
- **Branch Coverage**: > 85%
- **Method Coverage**: 100%

## Best Practices

1. **Test Naming**: Use descriptive names that explain the scenario
2. **Arrange-Act-Assert**: Follow AAA pattern consistently
3. **One Assertion Per Concept**: Focus each test on a single behavior
4. **Test Isolation**: Each test is independent and can run in any order
5. **Mock External Dependencies**: Don't call real payment providers
6. **Verify Side Effects**: Check event publishing and cache updates
7. **Test Edge Cases**: Null values, negative amounts, boundary conditions
8. **Test Invariants**: Verify business rules are enforced

## Related Documentation

- [Subscription State Transitions](./subscription-state-transitions.md)
- [Quota Enforcement Tests](./quota-enforcement-tests.md)
- [Payment Processing Architecture](./payment-processing-architecture.md)

## Future Enhancements

- [ ] Add integration tests with real payment provider sandbox
- [ ] Add performance tests for high-volume payment processing
- [ ] Add chaos engineering tests for resilience
- [ ] Add contract tests for payment provider API
- [ ] Add mutation testing to verify test quality
