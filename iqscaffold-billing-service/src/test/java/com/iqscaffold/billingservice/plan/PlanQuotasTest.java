package com.iqscaffold.billingservice.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PlanQuotas value object.
 * Tests immutability, factory methods, and query methods.
 */
@DisplayName("PlanQuotas Value Object Tests")
class PlanQuotasTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create unlimited quotas")
    void shouldCreateUnlimitedQuotas() {
      // When
      var quotas = PlanQuotas.unlimited();

      // Then
      assertNotNull(quotas);
      assertNull(quotas.maxUsers());
      assertNull(quotas.storageGb());
      assertNull(quotas.apiCallsPerMonth());
      assertNull(quotas.emailSendsPerMonth());
      assertNull(quotas.campaignExecutionsPerMonth());
      assertNull(quotas.scoringRequestsPerMonth());
      assertNull(quotas.customDomains());
      assertNull(quotas.dataExportsPerMonth());
    }

    @Test
    @DisplayName("Should create free tier quotas")
    void shouldCreateFreeTierQuotas() {
      // When
      var quotas = PlanQuotas.freeTier();

      // Then
      assertNotNull(quotas);
      assertEquals(5L, quotas.maxUsers());
      assertEquals(1L, quotas.storageGb());
      assertEquals(1000L, quotas.apiCallsPerMonth());
      assertEquals(100L, quotas.emailSendsPerMonth());
      assertEquals(10L, quotas.campaignExecutionsPerMonth());
      assertEquals(50L, quotas.scoringRequestsPerMonth());
      assertEquals(0L, quotas.customDomains());
      assertEquals(5L, quotas.dataExportsPerMonth());
    }

    @Test
    @DisplayName("Should create pro tier quotas")
    void shouldCreateProTierQuotas() {
      // When
      var quotas = PlanQuotas.proTier();

      // Then
      assertNotNull(quotas);
      assertEquals(50L, quotas.maxUsers());
      assertEquals(50L, quotas.storageGb());
      assertEquals(100000L, quotas.apiCallsPerMonth());
      assertEquals(10000L, quotas.emailSendsPerMonth());
      assertEquals(1000L, quotas.campaignExecutionsPerMonth());
      assertEquals(5000L, quotas.scoringRequestsPerMonth());
      assertEquals(5L, quotas.customDomains());
      assertEquals(100L, quotas.dataExportsPerMonth());
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("Should check if max users is unlimited")
    void shouldCheckIfMaxUsersIsUnlimited() {
      // Given
      var unlimitedQuotas = PlanQuotas.unlimited();
      var limitedQuotas = PlanQuotas.freeTier();

      // Then
      assertTrue(unlimitedQuotas.isMaxUsersUnlimited());
      assertFalse(limitedQuotas.isMaxUsersUnlimited());
    }

    @Test
    @DisplayName("Should check if storage is unlimited")
    void shouldCheckIfStorageIsUnlimited() {
      // Given
      var unlimitedQuotas = PlanQuotas.unlimited();
      var limitedQuotas = PlanQuotas.freeTier();

      // Then
      assertTrue(unlimitedQuotas.isStorageUnlimited());
      assertFalse(limitedQuotas.isStorageUnlimited());
    }

    @Test
    @DisplayName("Should check if API calls is unlimited")
    void shouldCheckIfApiCallsIsUnlimited() {
      // Given
      var unlimitedQuotas = PlanQuotas.unlimited();
      var limitedQuotas = PlanQuotas.freeTier();

      // Then
      assertTrue(unlimitedQuotas.isApiCallsUnlimited());
      assertFalse(limitedQuotas.isApiCallsUnlimited());
    }

    @Test
    @DisplayName("Should check if quota has limit")
    void shouldCheckIfQuotaHasLimit() {
      // Then
      assertTrue(PlanQuotas.hasLimit(100L));
      assertFalse(PlanQuotas.hasLimit(null));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("Should be immutable record")
    void shouldBeImmutableRecord() {
      // Given
      var quotas = PlanQuotas.freeTier();

      // When - Try to create a new instance with different values
      var newQuotas = new PlanQuotas(
          10L,
          quotas.storageGb(),
          quotas.apiCallsPerMonth(),
          quotas.emailSendsPerMonth(),
          quotas.campaignExecutionsPerMonth(),
          quotas.scoringRequestsPerMonth(),
          quotas.customDomains(),
          quotas.dataExportsPerMonth()
      );

      // Then - Original should be unchanged
      assertEquals(5L, quotas.maxUsers());
      assertEquals(10L, newQuotas.maxUsers());
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should be equal when all fields match")
    void shouldBeEqualWhenAllFieldsMatch() {
      // Given
      var quotas1 = PlanQuotas.freeTier();
      var quotas2 = PlanQuotas.freeTier();

      // Then
      assertEquals(quotas1, quotas2);
      assertEquals(quotas1.hashCode(), quotas2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
      // Given
      var quotas1 = PlanQuotas.freeTier();
      var quotas2 = PlanQuotas.proTier();

      // Then
      assertNotEquals(quotas1, quotas2);
    }
  }
}
