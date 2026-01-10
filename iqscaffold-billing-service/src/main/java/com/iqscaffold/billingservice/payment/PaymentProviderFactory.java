package com.iqscaffold.billingservice.payment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.springframework.stereotype.Component;

/**
 * Factory for obtaining payment provider adapters based on gateway type.
 * This enables runtime selection of payment providers and easy addition of new gateways.
 */
@Component
public class PaymentProviderFactory {

  private final Map<PaymentGatewayProvider, PaymentProviderAdapter> providers;

  public PaymentProviderFactory(final List<PaymentProviderAdapter> providerList) {
    this.providers = providerList.stream()
        .collect(Collectors.toMap(
            PaymentProviderAdapter::getProviderType,
            Function.identity()
        ));
  }

  /**
   * Get payment provider adapter for the specified gateway type.
   *
   * @param provider The payment gateway provider type
   * @return The provider adapter implementation
   * @throws IllegalArgumentException if provider is not supported
   */
  public PaymentProviderAdapter getProvider(PaymentGatewayProvider provider) {
    PaymentProviderAdapter adapter = providers.get(provider);
    if (adapter == null) {
      throw new IllegalArgumentException("Unsupported payment provider: " + provider);
    }
    return adapter;
  }

  /**
   * Check if a payment provider is supported.
   */
  public boolean isProviderSupported(PaymentGatewayProvider provider) {
    return providers.containsKey(provider);
  }

  /**
   * Get all supported payment providers.
   */
  public java.util.Set<PaymentGatewayProvider> getSupportedProviders() {
    return providers.keySet();
  }
}
