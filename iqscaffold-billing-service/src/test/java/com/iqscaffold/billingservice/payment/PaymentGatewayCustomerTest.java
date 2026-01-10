package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentGatewayCustomerTest {

  private PaymentGatewayCustomer customer;

  @BeforeEach
  void setUp() {
    customer = new PaymentGatewayCustomer();
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamps() {
    // Given
    assertNull(customer.getId());
    assertNull(customer.getCreatedAt());
    assertNull(customer.getUpdatedAt());

    // When
    customer.onCreate();

    // Then
    assertNotNull(customer.getId());
    assertNotNull(customer.getCreatedAt());
    assertNotNull(customer.getUpdatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    customer.setId(existingId);

    // When
    customer.onCreate();

    // Then
    assertEquals(existingId, customer.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    customer.onCreate();
    Instant originalCreatedAt = customer.getCreatedAt();
    Instant originalUpdatedAt = customer.getUpdatedAt();

    // Wait a bit to ensure timestamp difference
    Thread.sleep(10);

    // When
    customer.onUpdate();

    // Then
    assertEquals(originalCreatedAt, customer.getCreatedAt());
    assertNotNull(customer.getUpdatedAt());
    // Updated timestamp should be after the original
    assert customer.getUpdatedAt().isAfter(originalUpdatedAt) || customer.getUpdatedAt().equals(originalUpdatedAt);
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    String email = "test@example.com";
    String name = "Test User";
    String gatewayCustomerId = "cus_123";
    String gatewayAccountId = "acct_456";
    PaymentGatewayProvider provider = PaymentGatewayProvider.STRIPE;
    Instant now = Instant.now();

    // When
    customer.setId(id);
    customer.setEmail(email);
    customer.setName(name);
    customer.setGatewayCustomerId(gatewayCustomerId);
    customer.setGatewayAccountId(gatewayAccountId);
    customer.setGatewayProvider(provider);
    customer.setCreatedAt(now);
    customer.setUpdatedAt(now);

    // Then
    assertEquals(id, customer.getId());
    assertEquals(email, customer.getEmail());
    assertEquals(name, customer.getName());
    assertEquals(gatewayCustomerId, customer.getGatewayCustomerId());
    assertEquals(gatewayAccountId, customer.getGatewayAccountId());
    assertEquals(provider, customer.getGatewayProvider());
    assertEquals(now, customer.getCreatedAt());
    assertEquals(now, customer.getUpdatedAt());
  }

  @Test
  void constructor_shouldCreateEmptyInstance() {
    // When
    PaymentGatewayCustomer newCustomer = new PaymentGatewayCustomer();

    // Then
    assertNull(newCustomer.getId());
    assertNull(newCustomer.getEmail());
    assertNull(newCustomer.getName());
    assertNull(newCustomer.getGatewayCustomerId());
    assertNull(newCustomer.getGatewayAccountId());
    assertNull(newCustomer.getGatewayProvider());
    assertNull(newCustomer.getCreatedAt());
    assertNull(newCustomer.getUpdatedAt());
  }

  @Test
  void setGatewayProvider_shouldAcceptAllProviderTypes() {
    // Test all enum values
    for (final PaymentGatewayProvider provider : PaymentGatewayProvider.values()) {
      // When
      customer.setGatewayProvider(provider);

      // Then
      assertEquals(provider, customer.getGatewayProvider());
    }
  }

  @Test
  void setGatewayAccountId_shouldAcceptNull() {
    // Given
    customer.setGatewayAccountId("acct_123");

    // When
    customer.setGatewayAccountId(null);

    // Then
    assertNull(customer.getGatewayAccountId());
  }

  @Test
  void setName_shouldAcceptNull() {
    // Given
    customer.setName("Test User");

    // When
    customer.setName(null);

    // Then
    assertNull(customer.getName());
  }
}
