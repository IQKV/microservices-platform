package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class StripeCustomerTest {

  @Test
  void constructor_shouldCreateEmptyStripeCustomer() {
    // When
    StripeCustomer customer = new StripeCustomer();

    // Then
    assertNotNull(customer);
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    StripeCustomer customer = new StripeCustomer();
    UUID id = UUID.randomUUID();
    String email = "test@example.com";
    String name = "Test User";
    String stripeCustomerId = "cus_123";
    String stripeAccountId = "acct_123";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    customer.setId(id);
    customer.setEmail(email);
    customer.setName(name);
    customer.setStripeCustomerId(stripeCustomerId);
    customer.setStripeAccountId(stripeAccountId);
    customer.setCreatedAt(createdAt);
    customer.setUpdatedAt(updatedAt);

    // Then
    assertEquals(id, customer.getId());
    assertEquals(email, customer.getEmail());
    assertEquals(name, customer.getName());
    assertEquals(stripeCustomerId, customer.getStripeCustomerId());
    assertEquals(stripeAccountId, customer.getStripeAccountId());
    assertEquals(createdAt, customer.getCreatedAt());
    assertEquals(updatedAt, customer.getUpdatedAt());
  }

  @Test
  void onCreate_shouldSetIdAndTimestamps() {
    // Given
    StripeCustomer customer = new StripeCustomer();
    customer.setEmail("test@example.com");
    customer.setStripeCustomerId("cus_123");

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
    StripeCustomer customer = new StripeCustomer();
    UUID existingId = UUID.randomUUID();
    customer.setId(existingId);
    customer.setEmail("test@example.com");
    customer.setStripeCustomerId("cus_123");

    // When
    customer.onCreate();

    // Then
    assertEquals(existingId, customer.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    StripeCustomer customer = new StripeCustomer();
    customer.onCreate();
    Instant originalUpdatedAt = customer.getUpdatedAt();

    // Wait a bit to ensure timestamp difference
    Thread.sleep(10);

    // When
    customer.onUpdate();

    // Then
    assertNotNull(customer.getUpdatedAt());
    assert customer.getUpdatedAt().isAfter(originalUpdatedAt);
  }

  @Test
  void stripeAccountId_canBeNull() {
    // Given
    StripeCustomer customer = new StripeCustomer();
    customer.setEmail("test@example.com");
    customer.setStripeCustomerId("cus_123");

    // When
    customer.setStripeAccountId(null);

    // Then
    assertNull(customer.getStripeAccountId());
  }

  @Test
  void name_canBeNull() {
    // Given
    StripeCustomer customer = new StripeCustomer();
    customer.setEmail("test@example.com");
    customer.setStripeCustomerId("cus_123");

    // When
    customer.setName(null);

    // Then
    assertNull(customer.getName());
  }
}
