# ADR 0003: Use Java Records for DTOs and Value Objects

## Status

Accepted

## Context

The billing service needs to represent data transfer objects (DTOs) and value objects throughout the codebase. We need to choose an approach that:

- Minimizes boilerplate code
- Ensures immutability
- Provides clear semantics
- Supports modern Java practices
- Integrates well with Spring Boot and Jackson

Java 21 provides records as a language feature specifically designed for immutable data carriers.

## Decision

We will use **Java records** for all DTOs, value objects, and immutable data carriers in the billing service.

## Examples

### DTOs (Request/Response)

```java
public record CreateSubscriptionRequest(@NotNull Long planId, String paymentMethodId, boolean startTrial) {}

public record SubscriptionDto(
  Long id,
  String tenantId,
  Long planId,
  String planCode,
  SubscriptionStatus status,
  LocalDateTime currentPeriodStart,
  LocalDateTime currentPeriodEnd,
  boolean cancelAtPeriodEnd
) {}
```

### Value Objects

```java
public record PlanQuotas(long apiCalls, long storageGb, long emailSends, long activeUsers) {
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

public record ProrationResult(BigDecimal creditAmount, BigDecimal chargeAmount, BigDecimal netAmount, String description) {
  public ProrationResult {
    // Compact constructor for validation
    if (creditAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Credit amount cannot be negative");
    }
    if (chargeAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Charge amount cannot be negative");
    }
  }
}
```

### Configuration Properties

```java
@ConfigurationProperties(prefix = "iqscaffold.billing")
@Validated
public record BillingProperties(@Valid @NotNull Payment payment, @Valid @NotNull Subscription subscription, @Valid @NotNull Usage usage) {
  public record Payment(@NotBlank String provider, @Valid Stripe stripe, @Valid PayPal paypal) {}

  public record Stripe(@NotBlank String apiKey, @NotBlank String webhookSecret) {}

  public record Subscription(@NotBlank String defaultCurrency, @Min(0) int trialDays, @Min(0) int gracePeriodDays) {}
}
```

### Domain Events

```java
public record SubscriptionCreated(Long subscriptionId, String tenantId, Long planId, LocalDateTime createdAt) implements DomainEvent {}

public record PaymentSucceeded(Long paymentId, Long invoiceId, BigDecimal amount, LocalDateTime processedAt) implements DomainEvent {}
```

## Rationale

### Advantages of Java Records

1. **Conciseness**
   - Eliminates boilerplate (getters, equals, hashCode, toString)
   - Clear intent: "This is immutable data"
   - Reduces code by 70-80% compared to traditional classes

2. **Immutability by Default**
   - All fields are final
   - No setters generated
   - Thread-safe by design
   - Prevents accidental mutation

3. **Value Semantics**
   - Automatic equals() based on field values
   - Automatic hashCode() based on field values
   - Perfect for DTOs and value objects

4. **Modern Java**
   - Native language feature (Java 14+, standard in Java 16+)
   - First-class support in IDEs
   - Excellent tooling support

5. **Framework Integration**
   - Jackson serialization/deserialization works out of the box
   - Spring Boot @ConfigurationProperties support
   - Bean Validation annotations work seamlessly
   - OpenAPI/Swagger documentation generation

6. **Pattern Matching**
   - Can use pattern matching with records
   - Deconstruction in switch expressions
   - Better type safety

### Comparison with Traditional Classes

**Traditional Class** (30+ lines):

```java
public class SubscriptionDto {

  private final Long id;
  private final String tenantId;
  private final SubscriptionStatus status;

  public SubscriptionDto(Long id, String tenantId, SubscriptionStatus status) {
    this.id = id;
    this.tenantId = tenantId;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  @Override
  public boolean equals(Object o) {
    // 10+ lines of equals logic
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tenantId, status);
  }

  @Override
  public String toString() {
    // toString logic
  }
}
```

**Record** (3 lines):

```java
public record SubscriptionDto(Long id, String tenantId, SubscriptionStatus status) {}
```

## Implementation Guidelines

### When to Use Records

✅ **Use records for**:

- DTOs (request/response objects)
- Value objects (immutable domain concepts)
- Configuration properties
- Domain events
- Query results
- API responses

❌ **Don't use records for**:

- JPA entities (need mutable state for Hibernate)
- Objects requiring inheritance (records are final)
- Objects needing custom setters
- Objects with complex initialization logic

### Validation

Records work seamlessly with Bean Validation:

```java
public record CreatePaymentRequest(
  @NotNull @Positive Long invoiceId,
  @NotBlank String paymentMethodId,
  @NotNull @DecimalMin("0.01") BigDecimal amount,
  @NotBlank @Size(min = 3, max = 3) String currency
) {}
```

### Custom Methods

Records can have custom methods:

```java
public record UsageSummary(long currentUsage, long limit, MetricType metricType) {
  public long remaining() {
    return limit - currentUsage;
  }

  public double percentageUsed() {
    return ((double) currentUsage / limit) * 100;
  }

  public boolean isApproachingLimit() {
    return percentageUsed() >= 90.0;
  }
}
```

### Compact Constructors

Use compact constructors for validation:

```java
public record PaymentRequest(Long invoiceId, BigDecimal amount, String currency) {
  public PaymentRequest {
    // Compact constructor - no parameter list
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    if (currency == null || currency.length() != 3) {
      throw new IllegalArgumentException("Invalid currency code");
    }
  }
}
```

### OpenAPI Documentation

Records work with OpenAPI annotations:

```java
@Schema(description = "Request to create a new subscription")
public record CreateSubscriptionRequest(
  @Schema(description = "ID of the subscription plan", example = "2") @NotNull Long planId,

  @Schema(description = "Payment method ID from payment provider", example = "pm_1234567890") String paymentMethodId,

  @Schema(description = "Whether to start with trial period", example = "true") boolean startTrial
) {}
```

## Consequences

### Positive

- **Reduced Boilerplate**: 70-80% less code for DTOs and value objects
- **Immutability**: Thread-safe by default, prevents bugs
- **Clear Intent**: Records signal "immutable data carrier"
- **Better Readability**: Compact syntax, easy to understand
- **Type Safety**: Compiler-enforced immutability
- **Framework Support**: Works seamlessly with Spring Boot, Jackson, Bean Validation

### Negative

- **Limited Flexibility**: Cannot extend records, cannot add mutable state
- **Learning Curve**: Team needs to understand record semantics
- **Serialization**: Some serialization libraries may need configuration
- **Debugging**: Stack traces show record methods differently

### Mitigation Strategies

1. **Documentation**: Document when to use records vs classes
2. **Code Reviews**: Ensure records are used appropriately
3. **Training**: Educate team on record features and limitations
4. **Linting**: Use ArchUnit to enforce record usage for DTOs

## Alternatives Considered

### Alternative 1: Lombok @Value

**Approach**: Use Lombok's @Value annotation for immutable classes

**Pros**:

- Similar conciseness to records
- More flexibility (can extend classes)
- Familiar to many developers

**Cons**:

- Requires Lombok dependency
- Not a language feature
- IDE support varies
- Annotation processing overhead
- Less clear intent than records

**Rejected**: Records are a native language feature with better tooling support

### Alternative 2: Traditional Immutable Classes

**Approach**: Write traditional classes with final fields

**Pros**:

- Maximum flexibility
- No new concepts to learn
- Works everywhere

**Cons**:

- Massive boilerplate
- Error-prone (easy to forget final)
- Harder to maintain
- Less clear intent

**Rejected**: Too much boilerplate, records are superior

### Alternative 3: Kotlin Data Classes

**Approach**: Use Kotlin instead of Java

**Pros**:

- Excellent data class support
- Many modern language features
- Concise syntax

**Cons**:

- Requires adopting Kotlin
- Mixed language codebase
- Team training required
- Build complexity

**Rejected**: Not adopting Kotlin at this time

## Migration Strategy

### Phase 1: New Code (Completed)

- All new DTOs use records
- All new value objects use records
- All new configuration properties use records

### Phase 2: Gradual Migration

- Convert existing DTOs to records during refactoring
- Convert value objects to records
- No rush to convert everything

### Phase 3: Enforcement

- Add ArchUnit rules to enforce record usage
- Document patterns in coding standards

## Testing

Records are easy to test:

```java
@Test
void testProrationResult() {
  var result = new ProrationResult(new BigDecimal("25.00"), new BigDecimal("49.00"), new BigDecimal("24.00"), "Upgrade proration");

  assertEquals(new BigDecimal("25.00"), result.creditAmount());
  assertEquals(new BigDecimal("49.00"), result.chargeAmount());
  assertEquals(new BigDecimal("24.00"), result.netAmount());
}

@Test
void testRecordEquality() {
  var result1 = new ProrationResult(new BigDecimal("25.00"), new BigDecimal("49.00"), new BigDecimal("24.00"), "Test");
  var result2 = new ProrationResult(new BigDecimal("25.00"), new BigDecimal("49.00"), new BigDecimal("24.00"), "Test");

  assertEquals(result1, result2); // Value equality
}
```

## References

- [JEP 395: Records](https://openjdk.org/jeps/395)
- [Java Records Tutorial](https://docs.oracle.com/en/java/javase/21/language/records.html)
- [Spring Boot Records Support](https://spring.io/blog/2020/03/05/spring-boot-2-3-0-m3-available-now)
- [Jackson Records Support](https://github.com/FasterXML/jackson-databind/wiki/JDK-14-Record-types)

## Date

2024-12-09

## Author

Billing Service Team
