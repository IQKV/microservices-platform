# ADR 0004: Domain-Driven Design Architecture

## Status

Accepted

## Context

The billing service is a complex domain with intricate business rules around subscriptions, payments, invoicing, and usage tracking. We need an architecture that:

- Keeps business logic isolated and testable
- Prevents coupling to infrastructure concerns
- Makes domain concepts explicit in code
- Supports complex business rules and invariants
- Enables independent evolution of different parts of the system

## Decision

We will implement the billing service using **Domain-Driven Design (DDD)** tactical patterns with a layered architecture.

## Architecture Layers

### 1. Domain Layer (Core)

**Purpose**: Contains pure business logic, independent of infrastructure

**Components**:
- **Entities**: Objects with identity (Subscription, Invoice, Payment)
- **Value Objects**: Immutable objects defined by attributes (PlanQuotas, ProrationResult)
- **Aggregates**: Consistency boundaries (Subscription aggregate, Invoice aggregate)
- **Domain Services**: Business logic spanning multiple aggregates
- **Repositories (Interfaces)**: Data access abstractions
- **Domain Events**: Significant business occurrences
- **Specifications**: Reusable business rules
- **Factories**: Complex object construction

**Example**:
```java
// Subscription Aggregate Root
@Entity
public class Subscription {
  @Id
  private Long id;
  
  private String tenantId;
  private SubscriptionStatus status;
  private LocalDateTime currentPeriodEnd;
  
  // Business logic methods
  public void cancel(boolean immediately) {
    validateCanCancel();
    
    if (immediately) {
      this.status = SubscriptionStatus.CANCELED;
      this.currentPeriodEnd = LocalDateTime.now();
    } else {
      this.cancelAtPeriodEnd = true;
    }
    
    // Publish domain event
    registerEvent(new SubscriptionCanceled(this.id, immediately));
  }
  
  private void validateCanCancel() {
    if (status == SubscriptionStatus.CANCELED) {
      throw new InvalidSubscriptionStateException(
        "Cannot cancel already canceled subscription");
    }
  }
}
```

### 2. Application Layer

**Purpose**: Orchestrates use cases, manages transactions

**Components**:
- **Application Services**: Thin orchestration layer
- **DTOs**: Data transfer objects for API
- **Use Case Implementations**: Coordinate domain objects

**Example**:
```java
@Service
@Transactional
public class SubscriptionApplicationService {
  
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionFactory subscriptionFactory;
  private final DomainEventPublisher eventPublisher;
  
  public SubscriptionDto createSubscription(CreateSubscriptionRequest request) {
    // 1. Load dependencies
    SubscriptionPlan plan = planRepository.findById(request.planId())
      .orElseThrow(() -> new PlanNotFoundException(request.planId()));
    
    // 2. Use factory for complex creation
    Subscription subscription = subscriptionFactory.createTrialSubscription(
      TenantContext.getCurrentTenantId(),
      request.userId(),
      plan
    );
    
    // 3. Save aggregate
    subscription = subscriptionRepository.save(subscription);
    
    // 4. Publish events
    eventPublisher.publish(subscription.getDomainEvents());
    
    // 5. Return DTO
    return toDto(subscription);
  }
}
```

### 3. Infrastructure Layer

**Purpose**: Implements technical concerns

**Components**:
- **Repository Implementations**: JPA repositories
- **External Service Adapters**: Payment providers, email service
- **Database Configuration**: Hibernate, Liquibase
- **Message Queue**: RabbitMQ integration

**Example**:
```java
@Repository
public class JpaSubscriptionRepository implements SubscriptionRepository {
  
  @PersistenceContext
  private EntityManager entityManager;
  
  @Override
  public Optional<Subscription> findById(Long id) {
    return Optional.ofNullable(
      entityManager.find(Subscription.class, id));
  }
  
  @Override
  public Subscription save(Subscription subscription) {
    if (subscription.getId() == null) {
      entityManager.persist(subscription);
      return subscription;
    } else {
      return entityManager.merge(subscription);
    }
  }
}
```

### 4. Presentation Layer

**Purpose**: Exposes APIs, handles HTTP concerns

**Components**:
- **REST Controllers**: HTTP endpoints
- **Request/Response DTOs**: API contracts
- **Exception Handlers**: Error responses
- **OpenAPI Documentation**: API docs

**Example**:
```java
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription management APIs")
public class SubscriptionRestResource {
  
  private final SubscriptionApplicationService subscriptionService;
  
  @PostMapping
  @Operation(summary = "Create subscription")
  public ResponseEntity<SubscriptionDto> createSubscription(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    
    SubscriptionDto subscription = subscriptionService.createSubscription(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
  }
}
```

## Key DDD Patterns

### Aggregates

**Definition**: Cluster of entities and value objects with a consistency boundary

**Rules**:
1. One aggregate root per aggregate
2. External objects can only reference the root
3. Modifications go through the root
4. Aggregates are transaction boundaries

**Example**:
```java
// Invoice Aggregate
@Entity
public class Invoice {
  @Id
  private Long id;
  
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceLineItem> lineItems = new ArrayList<>();
  
  private BigDecimal total;
  
  // All line item modifications go through aggregate root
  public void addLineItem(InvoiceLineItem item) {
    validateLineItem(item);
    lineItems.add(item);
    recalculateTotal();
  }
  
  public void removeLineItem(int index) {
    lineItems.remove(index);
    recalculateTotal();
  }
  
  private void recalculateTotal() {
    this.total = lineItems.stream()
      .map(InvoiceLineItem::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
```

### Value Objects

**Definition**: Immutable objects defined by their attributes

**Implementation**: Use Java records

**Example**:
```java
public record PlanQuotas(
    long apiCalls,
    long storageGb,
    long emailSends,
    long activeUsers
) {
  public boolean isUnlimited(String metricType) {
    return switch (metricType) {
      case "API_CALLS" -> apiCalls == -1;
      case "STORAGE_GB" -> storageGb == -1;
      case "EMAIL_SENDS" -> emailSends == -1;
      case "ACTIVE_USERS" -> activeUsers == -1;
      default -> false;
    };
  }
}
```

### Domain Services

**Definition**: Stateless services for business logic that doesn't fit in entities

**Example**:
```java
@Service
public class ProrationCalculator {
  
  public ProrationResult calculate(
      Subscription subscription,
      SubscriptionPlan newPlan,
      LocalDateTime effectiveDate) {
    
    // Complex calculation logic spanning multiple aggregates
    BigDecimal unusedTime = calculateUnusedTime(subscription, effectiveDate);
    BigDecimal creditAmount = calculateCredit(subscription, unusedTime);
    BigDecimal chargeAmount = calculateCharge(newPlan, unusedTime);
    BigDecimal netAmount = chargeAmount.subtract(creditAmount);
    
    return new ProrationResult(creditAmount, chargeAmount, netAmount,
      "Proration for plan change");
  }
}
```

### Specifications

**Definition**: Reusable business rules that can be combined

**Example**:
```java
public class ActiveSubscriptionSpecification implements Specification<Subscription> {
  
  @Override
  public boolean isSatisfiedBy(Subscription subscription) {
    return subscription.getStatus() == SubscriptionStatus.ACTIVE
        && subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());
  }
}

// Usage
var spec = new ActiveSubscriptionSpecification()
    .and(new HasPaymentMethodSpecification())
    .and(new NotCanceledSpecification());

if (spec.isSatisfiedBy(subscription)) {
  // Process subscription
}
```

### Factories

**Definition**: Encapsulate complex object creation

**Example**:
```java
@Component
public class SubscriptionFactory {
  
  public Subscription createTrialSubscription(
      String tenantId,
      Long userId,
      SubscriptionPlan plan) {
    
    // Validate business rules
    if (!plan.hasTrialPeriod()) {
      throw new IllegalArgumentException("Plan does not support trials");
    }
    
    // Create subscription with proper initialization
    Subscription subscription = new Subscription();
    subscription.setTenantId(tenantId);
    subscription.setUserId(userId);
    subscription.setPlan(plan);
    subscription.setStatus(SubscriptionStatus.TRIAL);
    subscription.setTrialStart(LocalDateTime.now());
    subscription.setTrialEnd(LocalDateTime.now().plusDays(plan.getTrialDays()));
    subscription.setCurrentPeriodStart(LocalDateTime.now());
    subscription.setCurrentPeriodEnd(subscription.getTrialEnd());
    
    // Register domain event
    subscription.registerEvent(new TrialStarted(subscription.getId()));
    
    return subscription;
  }
}
```

### Domain Events

**Definition**: Significant business occurrences

**Example**:
```java
public record SubscriptionCreated(
    Long subscriptionId,
    String tenantId,
    Long planId,
    LocalDateTime createdAt
) implements DomainEvent {}

// Publishing
@Service
public class DomainEventPublisher {
  
  private final RabbitTemplate rabbitTemplate;
  
  public void publish(List<DomainEvent> events) {
    events.forEach(event -> {
      rabbitTemplate.convertAndSend(
        BillingConstants.EXCHANGE_NAME,
        event.getClass().getSimpleName(),
        event
      );
    });
  }
}
```

### Anti-Corruption Layer

**Definition**: Protect domain model from external systems

**Example**:
```java
// Domain interface
public interface PaymentProviderAdapter {
  PaymentResult processPayment(PaymentRequest request);
}

// Implementation translates between Stripe and domain models
@Service
public class StripePaymentProvider implements PaymentProviderAdapter {
  
  @Override
  public PaymentResult processPayment(PaymentRequest request) {
    // Translate domain model to Stripe model
    PaymentIntentCreateParams params = toStripeParams(request);
    
    // Call Stripe API
    PaymentIntent intent = PaymentIntent.create(params);
    
    // Translate Stripe model back to domain model
    return toPaymentResult(intent);
  }
  
  private PaymentResult toPaymentResult(PaymentIntent intent) {
    return new PaymentResult(
      intent.getId(),
      mapStatus(intent.getStatus()),
      BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100)),
      intent.getCurrency().toUpperCase(),
      intent.getLastPaymentError() != null ? 
        intent.getLastPaymentError().getCode() : null,
      intent.getLastPaymentError() != null ? 
        intent.getLastPaymentError().getMessage() : null,
      Map.of()
    );
  }
}
```

## Rationale

### Advantages

1. **Business Logic Isolation**
   - Domain logic independent of frameworks
   - Easy to test without infrastructure
   - Clear separation of concerns

2. **Explicit Domain Concepts**
   - Business concepts visible in code
   - Ubiquitous language shared with business
   - Self-documenting code

3. **Maintainability**
   - Changes localized to appropriate layers
   - Infrastructure changes don't affect domain
   - Easy to understand and modify

4. **Testability**
   - Domain logic testable without mocks
   - Clear boundaries for unit tests
   - Integration tests focus on infrastructure

5. **Flexibility**
   - Can swap infrastructure implementations
   - Domain model evolves independently
   - Easy to add new features

### Trade-offs

**Pros**:
- Clean architecture
- Testable business logic
- Explicit domain model
- Maintainable codebase

**Cons**:
- More layers and abstractions
- Steeper learning curve
- More initial setup
- Requires discipline to maintain

## Implementation Guidelines

### Package Structure

```
com.iqscaffold.billingservice/
├── domain/
│   ├── subscription/
│   │   ├── Subscription.java (Entity)
│   │   ├── SubscriptionRepository.java (Interface)
│   │   ├── SubscriptionStatus.java (Enum)
│   │   └── SubscriptionFactory.java
│   ├── invoice/
│   │   ├── Invoice.java (Aggregate Root)
│   │   ├── InvoiceLineItem.java (Entity)
│   │   ├── InvoiceRepository.java (Interface)
│   │   └── InvoiceFactory.java
│   ├── payment/
│   │   ├── Payment.java (Aggregate Root)
│   │   ├── PaymentRepository.java (Interface)
│   │   └── PaymentProviderAdapter.java (Interface)
│   ├── shared/
│   │   ├── DomainEvent.java
│   │   ├── Specification.java
│   │   └── ValueObject.java
│   └── services/
│       ├── ProrationCalculator.java
│       ├── QuotaEnforcer.java
│       └── InvoiceGenerator.java
├── application/
│   ├── subscription/
│   │   ├── SubscriptionApplicationService.java
│   │   ├── SubscriptionDto.java
│   │   └── CreateSubscriptionRequest.java
│   ├── payment/
│   │   ├── PaymentApplicationService.java
│   │   └── PaymentDto.java
│   └── invoice/
│       ├── InvoiceApplicationService.java
│       └── InvoiceDto.java
├── infrastructure/
│   ├── persistence/
│   │   ├── JpaSubscriptionRepository.java
│   │   ├── JpaInvoiceRepository.java
│   │   └── JpaPaymentRepository.java
│   ├── payment/
│   │   ├── StripePaymentProvider.java
│   │   ├── PayPalPaymentProvider.java
│   │   └── ManualPaymentProvider.java
│   └── messaging/
│       ├── RabbitMQEventPublisher.java
│       └── DomainEventConsumer.java
└── presentation/
    ├── rest/
    │   ├── SubscriptionRestResource.java
    │   ├── PaymentRestResource.java
    │   └── InvoiceRestResource.java
    └── dto/
        ├── ErrorResponse.java
        └── ApiResponse.java
```

### Dependency Rules

1. **Domain** depends on nothing
2. **Application** depends on Domain
3. **Infrastructure** depends on Domain and Application
4. **Presentation** depends on Application

### Testing Strategy

**Domain Layer**:
- Pure unit tests
- No mocks needed
- Test business logic in isolation

**Application Layer**:
- Unit tests with mocked repositories
- Test use case orchestration

**Infrastructure Layer**:
- Integration tests with real infrastructure
- Test repository implementations
- Test external service adapters

**Presentation Layer**:
- Controller tests with MockMvc
- Test request/response handling

## Consequences

### Positive

- Clean separation of business logic
- Highly testable code
- Explicit domain model
- Easy to maintain and extend
- Infrastructure-independent domain

### Negative

- More layers and abstractions
- Requires team training
- More initial setup
- Need discipline to maintain boundaries

### Mitigation Strategies

1. **Training**: Educate team on DDD patterns
2. **Code Reviews**: Enforce layer boundaries
3. **ArchUnit Tests**: Automated architecture validation
4. **Documentation**: Document patterns and examples

## Alternatives Considered

### Alternative 1: Transaction Script

**Approach**: Service methods contain all logic

**Rejected**: Business logic scattered, hard to test, no domain model

### Alternative 2: Anemic Domain Model

**Approach**: Entities are just data holders, services have all logic

**Rejected**: Loses benefits of OOP, domain model not expressive

### Alternative 3: CQRS/Event Sourcing

**Approach**: Separate read/write models, event-sourced aggregates

**Rejected**: Too complex for current requirements, can add later if needed

## References

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/iddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

## Date

2024-12-09

## Author

Billing Service Team
