# Billing Service Messaging Infrastructure

Complete RabbitMQ messaging implementation for event-driven communication in the IQScaffold Billing Service.

## 📧 Architecture Overview

```
infrastructure/messaging/
├── MessagingService.java           # Event publishing service
├── BillingEvent.java              # Billing event model
├── NotificationEvent.java         # Notification event model  
├── UserEvent.java                 # User event model (from User Service)
├── MessagingException.java        # Messaging exceptions
├── UserEventListener.java         # ✨ NEW: Consumes user events
├── BillingEventListener.java      # ✨ NEW: Consumes billing events
├── NotificationEventListener.java # ✨ NEW: Consumes notification events
└── README.md                      # This file
```

## 🎯 Event Listeners (NEW)

### UserEventListener

**Consumes**: `iqscaffold.user.events` queue  
**Purpose**: Maintains billing data consistency with User Service

**Handles**:

- `USER_CREATED` → Creates customer record in billing system
- `USER_UPDATED` → Updates customer information
- `USER_DELETED` → Cancels subscriptions and archives data
- `USER_VERIFIED` → Enables trial subscriptions
- `PASSWORD_RESET` → Logs security events

**Status**: ⚠️ Structure complete, implementation TODO

### BillingEventListener

**Consumes**: `iqscaffold.billing.events` queue  
**Purpose**: Async processing of billing operations

**Handles**:

- `PAYMENT_SUCCESSFUL` → Updates analytics, generates invoices
- `PAYMENT_FAILED` → Schedules retries, suspends subscriptions
- `PAYMENT_REFUNDED` → Adjusts metrics, handles refunds
- `MERCHANT_ONBOARDING` → Sets up merchant accounts
- `INVOICE_GENERATED` → Sends invoices, schedules reminders

**Status**: ⚠️ Structure complete, implementation TODO

### NotificationEventListener

**Consumes**: `iqscaffold.notifications` queue  
**Purpose**: Tracks notification delivery and engagement

**Handles**:

- `EMAIL` → Tracks email delivery
- `SMS` → Tracks SMS delivery and costs
- `PUSH` → Tracks push notifications

**Status**: ⚠️ Structure complete, implementation TODO

## 🔄 Event Flow

### Cross-Service Communication

```
User Service                    RabbitMQ                    Billing Service
    |                              |                              |
    |-- USER_CREATED ------------->|                              |
    |                              |-- USER_CREATED ------------->|
    |                              |                              |
    |                              |                    UserEventListener
    |                              |                    handleUserCreated()
    |                              |                    TODO: Create customer
    |                              |                              |
    |                              |<-- PAYMENT_SUCCESSFUL -------|
    |<-- PAYMENT_SUCCESSFUL -------|                              |
```

### Internal Event Processing

```
Payment Processing              RabbitMQ                    Async Processing
    |                              |                              |
    |-- publishPaymentSuccessful ->|                              |
    |                              |-- PAYMENT_SUCCESSFUL ------->|
    |                              |                              |
    |                              |                    BillingEventListener
    |                              |                    handlePaymentSuccessful()
    |                              |                    TODO: Generate invoice
    |                              |                    TODO: Update analytics
```

## 🚀 Usage Examples

### Publishing Events (Already Working)

```java
@Service
public class PaymentService {
    
    private final MessagingService messagingService;
    
    public void processPayment(Payment payment) {
        // Process payment...
        
        // Publish event
        messagingService.publishPaymentSuccessful(
            payment.getId(),
            payment.getTenantId(),
            payment.getCustomerEmail()
        );
    }
}
```

### Consuming Events (NEW - TODO Implementation)

```java
@Component
public class UserEventListener {
    
    @RabbitListener(queues = "iqscaffold.user.events")
    public void handleUserEvent(UserEvent event) {
        switch (event.getEventType()) {
            case "USER_CREATED":
                // TODO: Implement customer creation
                // Customer customer = new Customer();
                // customer.setUserId(event.getUserId());
                // customerRepository.save(customer);
                break;
        }
    }
}
```

## ⚙️ Configuration

### RabbitMQ Settings

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: iqscaffold
    password: iqscaffold_password
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: auto
        prefetch: 10
        retry:
          enabled: true
          max-attempts: 3
```

### Exchanges and Queues

| Exchange            | Queue                       | Routing Pattern  | Listener                  |
|---------------------|-----------------------------|------------------|---------------------------|
| `iqscaffold.events` | `iqscaffold.user.events`    | `user.*`         | UserEventListener         |
| `iqscaffold.events` | `iqscaffold.billing.events` | `billing.*`      | BillingEventListener      |
| `iqscaffold.events` | `iqscaffold.notifications`  | `notification.*` | NotificationEventListener |
| `iqscaffold.dlx`    | `iqscaffold.dlq`            | `#`              | Manual processing         |

## 📋 Implementation Checklist

### UserEventListener

- [ ] Implement customer creation logic
- [ ] Implement Stripe customer sync
- [ ] Implement subscription cancellation
- [ ] Implement fraud detection integration
- [ ] Add idempotency handling
- [ ] Add correlation ID tracking
- [ ] Write integration tests

### BillingEventListener

- [ ] Implement analytics integration
- [ ] Implement retry scheduling logic
- [ ] Implement subscription management
- [ ] Implement accounting software sync
- [ ] Add idempotency handling
- [ ] Add correlation ID tracking
- [ ] Write integration tests

### NotificationEventListener

- [ ] Implement notification audit logging
- [ ] Implement delivery tracking
- [ ] Implement customer preference management
- [ ] Implement analytics integration
- [ ] Add idempotency handling
- [ ] Write integration tests

## 🧪 Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private UserEventListener listener;
    
    @Test
    void shouldHandleUserCreatedEvent() {
        // Arrange
        UserEvent event = new UserEvent();
        event.setEventType("USER_CREATED");
        event.setUserId("user-123");
        event.setTenantId("tenant-1");
        event.setEmail("user@example.com");
        
        // Act
        listener.handleUserEvent(event);
        
        // Assert
        // TODO: Verify customer was created
        // verify(customerRepository).save(any(Customer.class));
    }
}
```

## 🔒 Error Handling

All listeners follow this pattern:

```java
@RabbitListener(queues = "queue.name")
public void handleEvent(Event event) {
    try {
        log.info("Processing event: {}", event.getEventId());
        // Business logic...
        log.debug("Successfully processed event");
    } catch (final Exception e) {
        log.error("Error processing event: {}", event.getEventId(), e);
        throw e; // Triggers retry or DLQ routing
    }
}
```

### Retry Mechanism

- Initial interval: 1 second
- Max attempts: 3
- Multiplier: 2.0
- Failed messages → Dead Letter Queue

## 📊 Monitoring

### Metrics to Track

- Event processing rates
- Event processing latency
- Error rates by event type
- Queue depths
- DLQ message count

### Logging

- Structured JSON logging
- Correlation IDs (TODO: implement)
- Tenant context
- Performance metrics

## 🎯 Next Steps

### Priority 1: Critical

1. Implement `UserEventListener.handleUserCreated()`
2. Implement `UserEventListener.handleUserDeleted()`
3. Add idempotency keys to all events
4. Add correlation IDs for distributed tracing

### Priority 2: High

5. Implement `BillingEventListener.handlePaymentSuccessful()`
6. Implement `BillingEventListener.handlePaymentFailed()`
7. Implement notification tracking
8. Add comprehensive integration tests

### Priority 3: Medium

9. Implement subscription event handling
10. Implement accounting software sync
11. Add event versioning
12. Implement saga patterns for complex workflows

## 📚 Related Documentation

- [RabbitMQ Configuration Summary](../../../RABBITMQ_CONFIGURATION_SUMMARY.md)
- [Billing Service Messaging Completeness Analysis](../../../BILLING_SERVICE_MESSAGING_COMPLETENESS_ANALYSIS.md)
- [User Service RabbitMQ Usage](../../../RABBITMQ_USAGE_IN_USER_SERVICE.md)

## 🤝 Contributing

When implementing TODO items:

1. Remove the TODO comment
2. Implement the business logic
3. Add error handling
4. Add logging
5. Write unit tests
6. Write integration tests
7. Update this README

---

**Status**: Event listeners created with TODO placeholders. Ready for implementation.
