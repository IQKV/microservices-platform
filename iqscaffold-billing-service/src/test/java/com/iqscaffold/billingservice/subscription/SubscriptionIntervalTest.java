package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SubscriptionIntervalTest {

  @Test
  void subscriptionInterval_shouldHaveAllExpectedValues() {
    // When & Then
    assertEquals(5, SubscriptionInterval.values().length);
    assertEquals(SubscriptionInterval.DAY, SubscriptionInterval.valueOf("DAY"));
    assertEquals(SubscriptionInterval.WEEK, SubscriptionInterval.valueOf("WEEK"));
    assertEquals(SubscriptionInterval.MONTH, SubscriptionInterval.valueOf("MONTH"));
    assertEquals(SubscriptionInterval.QUARTER, SubscriptionInterval.valueOf("QUARTER"));
    assertEquals(SubscriptionInterval.YEAR, SubscriptionInterval.valueOf("YEAR"));
  }

  @Test
  void subscriptionInterval_shouldHaveCorrectNames() {
    // When & Then
    assertEquals("DAY", SubscriptionInterval.DAY.name());
    assertEquals("WEEK", SubscriptionInterval.WEEK.name());
    assertEquals("MONTH", SubscriptionInterval.MONTH.name());
    assertEquals("QUARTER", SubscriptionInterval.QUARTER.name());
    assertEquals("YEAR", SubscriptionInterval.YEAR.name());
  }

  @Test
  void subscriptionInterval_shouldSupportValueOfConversion() {
    // Given
    String[] intervalNames = {"DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
    SubscriptionInterval[] expectedIntervals = {
        SubscriptionInterval.DAY,
        SubscriptionInterval.WEEK,
        SubscriptionInterval.MONTH,
        SubscriptionInterval.QUARTER,
        SubscriptionInterval.YEAR
    };

    // When & Then
    for (int i = 0; i < intervalNames.length; i++) {
      assertEquals(expectedIntervals[i], SubscriptionInterval.valueOf(intervalNames[i]));
    }
  }

  @Test
  void subscriptionInterval_shouldSupportOrdinalValues() {
    // When & Then
    assertEquals(0, SubscriptionInterval.DAY.ordinal());
    assertEquals(1, SubscriptionInterval.WEEK.ordinal());
    assertEquals(2, SubscriptionInterval.MONTH.ordinal());
    assertEquals(3, SubscriptionInterval.QUARTER.ordinal());
    assertEquals(4, SubscriptionInterval.YEAR.ordinal());
  }
}