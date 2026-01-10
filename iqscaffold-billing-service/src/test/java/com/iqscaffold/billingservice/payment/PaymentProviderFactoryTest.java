package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentProviderFactoryTest {

  private PaymentProviderAdapter stripeAdapter;
  private PaymentProviderAdapter paypalAdapter;
  private PaymentProviderFactory factory;

  @BeforeEach
  void setUp() {
    stripeAdapter = mock(PaymentProviderAdapter.class);
    paypalAdapter = mock(PaymentProviderAdapter.class);

    when(stripeAdapter.getProviderType()).thenReturn(PaymentGatewayProvider.STRIPE);
    when(paypalAdapter.getProviderType()).thenReturn(PaymentGatewayProvider.PAYPAL);
  }

  @Test
  void constructor_shouldInitializeWithProviders() {
    // Given
    List<PaymentProviderAdapter> providers = Arrays.asList(stripeAdapter, paypalAdapter);

    // When
    factory = new PaymentProviderFactory(providers);

    // Then
    assertNotNull(factory);
  }

  @Test
  void getProvider_shouldReturnCorrectAdapter() {
    // Given
    List<PaymentProviderAdapter> providers = Arrays.asList(stripeAdapter, paypalAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    PaymentProviderAdapter result = factory.getProvider(PaymentGatewayProvider.STRIPE);

    // Then
    assertEquals(stripeAdapter, result);
  }

  @Test
  void getProvider_shouldThrowExceptionForUnsupportedProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.singletonList(stripeAdapter);
    factory = new PaymentProviderFactory(providers);

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> factory.getProvider(PaymentGatewayProvider.SQUARE)
    );

    assertTrue(exception.getMessage().contains("Unsupported payment provider"));
    assertTrue(exception.getMessage().contains("SQUARE"));
  }

  @Test
  void isProviderSupported_shouldReturnTrueForSupportedProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Arrays.asList(stripeAdapter, paypalAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    boolean result = factory.isProviderSupported(PaymentGatewayProvider.STRIPE);

    // Then
    assertTrue(result);
  }

  @Test
  void isProviderSupported_shouldReturnFalseForUnsupportedProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.singletonList(stripeAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    boolean result = factory.isProviderSupported(PaymentGatewayProvider.BRAINTREE);

    // Then
    assertFalse(result);
  }

  @Test
  void getSupportedProviders_shouldReturnAllProviders() {
    // Given
    List<PaymentProviderAdapter> providers = Arrays.asList(stripeAdapter, paypalAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    Set<PaymentGatewayProvider> result = factory.getSupportedProviders();

    // Then
    assertEquals(2, result.size());
    assertTrue(result.contains(PaymentGatewayProvider.STRIPE));
    assertTrue(result.contains(PaymentGatewayProvider.PAYPAL));
  }

  @Test
  void getSupportedProviders_shouldReturnEmptySetWhenNoProviders() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.emptyList();
    factory = new PaymentProviderFactory(providers);

    // When
    Set<PaymentGatewayProvider> result = factory.getSupportedProviders();

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void getProvider_shouldHandleMultipleProviders() {
    // Given
    List<PaymentProviderAdapter> providers = Arrays.asList(stripeAdapter, paypalAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    PaymentProviderAdapter stripeResult = factory.getProvider(PaymentGatewayProvider.STRIPE);
    PaymentProviderAdapter paypalResult = factory.getProvider(PaymentGatewayProvider.PAYPAL);

    // Then
    assertEquals(stripeAdapter, stripeResult);
    assertEquals(paypalAdapter, paypalResult);
  }

  @Test
  void constructor_shouldHandleEmptyProviderList() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.emptyList();

    // When
    factory = new PaymentProviderFactory(providers);

    // Then
    assertNotNull(factory);
    assertTrue(factory.getSupportedProviders().isEmpty());
  }

  @Test
  void getProvider_shouldThrowExceptionWithNullProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.singletonList(stripeAdapter);
    factory = new PaymentProviderFactory(providers);

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> factory.getProvider(null)
    );
  }

  @Test
  void isProviderSupported_shouldHandleNullProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.singletonList(stripeAdapter);
    factory = new PaymentProviderFactory(providers);

    // When
    boolean result = factory.isProviderSupported(null);

    // Then
    assertFalse(result);
  }

  @Test
  void constructor_shouldHandleSingleProvider() {
    // Given
    List<PaymentProviderAdapter> providers = Collections.singletonList(stripeAdapter);

    // When
    factory = new PaymentProviderFactory(providers);

    // Then
    assertEquals(1, factory.getSupportedProviders().size());
    assertTrue(factory.isProviderSupported(PaymentGatewayProvider.STRIPE));
  }
}
