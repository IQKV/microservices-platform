# ADR 0002: Payment Provider Abstraction with Anti-Corruption Layer

## Status

Accepted

## Context

The billing service needs to integrate with multiple payment providers (Stripe, PayPal, manual processing) to support different customer preferences and geographic regions. We need to:

- Support multiple payment providers without coupling to specific implementations
- Protect domain model from external API changes
- Enable easy addition of new payment providers
- Maintain consistent payment processing logic
- Handle provider-specific features and limitations

## Decision

We will implement a **Payment Provider Abstraction** using the Strategy pattern with an Anti-Corruption Layer (ACL) to isolate the domain model from external payment provider APIs.

## Architecture

### Core Interface

```java
public sealed interface PaymentProviderAdapter 
    permits StripePaymentProvider, PayPalPaymentProvider, ManualPaymentProvider {
  
  PaymentResult processPayment(PaymentRequest request);
  PaymentResult refundPayment(String paymentId, BigDecimal amount);
  PaymentMethodDetails addPaymentMethod(String customerId, String token);
  void removePaymentMethod(String paymentMethodId);
  PaymentMethodDetails getPaymentMethodDetails(String paymentMethodId);
  CustomerDetails createCustomer(CustomerRequest request);
  CustomerDetails updateCustomer(String customerId, CustomerUpdateRequest request);
  boolean verifyWebhookSignature(String payload, String signature);
}
```

### Domain Models (Provider-Agnostic)

```java
public record PaymentResult(
    String paymentId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String errorCode,
    String errorMessage,
    Map<String, Object> metadata
) {}

public record PaymentMethodDetails(
    String id,
    PaymentMethodType type,
    String brand,
    String last4,
    int expiryMonth,
    int expiryYear
) {}
```

### Provider Implementations

Each provider implements the interface and translates between provider-specific models and domain models:

```java
@Service
public final class StripePaymentProvider implements PaymentProviderAdapter {
  
  private final StripeClient stripeClient;
  
  @Override
  public PaymentResult processPayment(PaymentRequest request) {
    try {
      // Call Stripe API
      PaymentIntent intent = stripeClient.createPaymentIntent(
          toStripeRequest(request));
      
      // Translate to domain model
      return toPaymentResult(intent);
    } catch (StripeException e) {
      return PaymentResult.failed(e.getCode(), e.getMessage());
    }
  }
  
  private PaymentResult toPaymentResult(PaymentIntent intent) {
    // Translation logic - isolates domain from Stripe models
  }
}
```

## Rationale

### Advantages

1. **Domain Model Protection**
   - Domain model never depends on external APIs
   - Changes to Stripe/PayPal APIs don't affect domain logic
   - Can swap providers without changing business logic

2. **Consistent Interface**
   - All providers implement the same interface
   - Business logic works with any provider
   - Easy to add new providers

3. **Testability**
   - Can mock PaymentProviderAdapter for testing
   - Don't need to mock Stripe/PayPal SDKs
   - Can test with fake provider implementation

4. **Provider-Specific Optimizations**
   - Each provider can optimize for its API
   - Can handle provider-specific features
   - Graceful degradation when features unavailable

5. **Flexibility**
   - Can route payments to different providers based on rules
   - Can implement fallback logic
   - Can A/B test providers

### Trade-offs

**Pros**:
- Clean separation of concerns
- Easy to maintain and extend
- Provider changes don't affect domain
- Testable without external dependencies

**Cons**:
- Additional abstraction layer
- Translation overhead (minimal)
- May not expose all provider-specific features
- Need to maintain multiple implementations

## Implementation Details

### Provider Selection

```java
@Component
public class PaymentProviderFactory {
  
  private final Map<String, PaymentProviderAdapter> providers;
  private final BillingProperties properties;
  
  public PaymentProviderAdapter getProvider(String providerName) {
    return switch (providerName.toUpperCase()) {
      case "STRIPE" -> providers.get("stripe");
      case "PAYPAL" -> providers.get("paypal");
      case "MANUAL" -> providers.get("manual");
      default -> throw new IllegalArgumentException(
          "Unknown provider: " + providerName);
    };
  }
  
  public PaymentProviderAdapter getDefaultProvider() {
    return getProvider(properties.payment().provider());
  }
}
```

### Configuration

```yaml
iqscaffold:
  billing:
    payment:
      provider: stripe  # Default provider
      stripe:
        api-key: ${STRIPE_API_KEY}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      paypal:
        client-id: ${PAYPAL_CLIENT_ID}
        client-secret: ${PAYPAL_CLIENT_SECRET}
```

### Error Handling

All providers translate errors to domain-specific exceptions:

```java
public class PaymentFailedException extends BillingException {
  private final String paymentId;
  private final String errorCode;
  
  public PaymentFailedException(String paymentId, String errorCode, 
                                 String message) {
    super(message);
    this.paymentId = paymentId;
    this.errorCode = errorCode;
  }
}
```

## Consequences

### Positive

- Domain model remains clean and provider-agnostic
- Easy to add new payment providers
- Can switch providers without code changes
- Better testability
- Consistent error handling across providers

### Negative

- Additional abstraction layer to maintain
- Need to implement translation logic for each provider
- May not expose all provider-specific features
- Slight performance overhead from translation

### Mitigation Strategies

1. **Keep Interface Minimal**: Only include common operations
2. **Provider-Specific Extensions**: Use metadata map for provider-specific data
3. **Comprehensive Testing**: Test each provider implementation thoroughly
4. **Documentation**: Document provider-specific behaviors and limitations

## Alternatives Considered

### Alternative 1: Direct Provider Integration

**Approach**: Use Stripe/PayPal SDKs directly in service layer

**Pros**:
- Simpler implementation
- Access to all provider features
- No translation overhead

**Cons**:
- Domain model coupled to external APIs
- Hard to switch providers
- Difficult to test
- Provider changes break domain logic

**Rejected**: Tight coupling to external APIs is unacceptable

### Alternative 2: Generic Payment Gateway

**Approach**: Use a third-party payment gateway (e.g., Adyen, Braintree)

**Pros**:
- Single integration point
- Gateway handles multiple providers

**Cons**:
- Additional cost
- Still need abstraction layer
- Dependency on third-party service
- May not support all desired providers

**Rejected**: Adds unnecessary dependency and cost

### Alternative 3: Event-Driven Integration

**Approach**: Publish payment events, separate service handles providers

**Pros**:
- Complete decoupling
- Can scale payment processing independently

**Cons**:
- Much more complex
- Eventual consistency issues
- Harder to handle synchronous payment flows
- Overkill for current requirements

**Rejected**: Too complex for current needs

## Migration Strategy

### Phase 1: Implement Abstraction (Completed)
- Define PaymentProviderAdapter interface
- Implement StripePaymentProvider
- Update PaymentService to use abstraction

### Phase 2: Add Additional Providers
- Implement PayPalPaymentProvider
- Implement ManualPaymentProvider
- Add provider selection logic

### Phase 3: Provider Routing
- Implement routing rules (by region, amount, etc.)
- Add fallback logic
- Implement A/B testing support

## Testing Strategy

### Unit Tests
- Test each provider implementation with mocked SDK
- Test translation logic
- Test error handling

### Integration Tests
- Test with Stripe test mode
- Test with PayPal sandbox
- Test provider switching

### Contract Tests
- Verify provider implementations match interface contract
- Test with real provider APIs in staging

## References

- [Anti-Corruption Layer Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer)
- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Stripe API Documentation](https://stripe.com/docs/api)
- [PayPal API Documentation](https://developer.paypal.com/docs/api/overview/)

## Date

2024-12-09

## Author

Billing Service Team
