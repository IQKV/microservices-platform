package com.iqscaffold.billingservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TenantStatusTest {

  @Test
  void shouldHaveAllExpectedValues() {
    // Given & When
    TenantStatus[] values = TenantStatus.values();

    // Then
    assertNotNull(values);
    assertEquals(3, values.length);
    assertArrayEquals(
        new TenantStatus[]{TenantStatus.ACTIVE, TenantStatus.SUSPENDED, TenantStatus.ARCHIVED},
        values
    );
  }

  @Test
  void shouldRetrieveActiveStatus() {
    // When
    TenantStatus status = TenantStatus.valueOf("ACTIVE");

    // Then
    assertEquals(TenantStatus.ACTIVE, status);
  }

  @Test
  void shouldRetrieveSuspendedStatus() {
    // When
    TenantStatus status = TenantStatus.valueOf("SUSPENDED");

    // Then
    assertEquals(TenantStatus.SUSPENDED, status);
  }

  @Test
  void shouldRetrieveArchivedStatus() {
    // When
    TenantStatus status = TenantStatus.valueOf("ARCHIVED");

    // Then
    assertEquals(TenantStatus.ARCHIVED, status);
  }

  @Test
  void shouldCompareEnumValues() {
    // Given
    TenantStatus active1 = TenantStatus.ACTIVE;
    TenantStatus active2 = TenantStatus.ACTIVE;
    TenantStatus suspended = TenantStatus.SUSPENDED;

    // Then
    assertEquals(active1, active2);
    assertEquals(TenantStatus.ACTIVE, active1);
    assertEquals(0, active1.compareTo(active2));
    assertEquals(-1, active1.compareTo(suspended));
    assertEquals(1, suspended.compareTo(active1));
  }

  @Test
  void shouldConvertToString() {
    // When & Then
    assertEquals("ACTIVE", TenantStatus.ACTIVE.toString());
    assertEquals("SUSPENDED", TenantStatus.SUSPENDED.toString());
    assertEquals("ARCHIVED", TenantStatus.ARCHIVED.toString());
  }

  @Test
  void shouldGetName() {
    // When & Then
    assertEquals("ACTIVE", TenantStatus.ACTIVE.name());
    assertEquals("SUSPENDED", TenantStatus.SUSPENDED.name());
    assertEquals("ARCHIVED", TenantStatus.ARCHIVED.name());
  }

  @Test
  void shouldGetOrdinal() {
    // When & Then
    assertEquals(0, TenantStatus.ACTIVE.ordinal());
    assertEquals(1, TenantStatus.SUSPENDED.ordinal());
    assertEquals(2, TenantStatus.ARCHIVED.ordinal());
  }
}
