package com.iqscaffold.billingservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SchemaTenantIdentifierResolverTest {

  private final SchemaTenantIdentifierResolver resolver = new SchemaTenantIdentifierResolver();

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void resolveCurrentTenantIdentifier_shouldReturnTenantIdWhenSet() {
    // Given
    String tenantId = "tenant-123";
    TenantContext.setCurrentTenantId(tenantId);

    // When
    String result = resolver.resolveCurrentTenantIdentifier();

    // Then
    assertEquals(tenantId, result);
  }

  @Test
  void resolveCurrentTenantIdentifier_shouldReturnPublicWhenNoTenantSet() {
    // Given
    TenantContext.clear();

    // When
    String result = resolver.resolveCurrentTenantIdentifier();

    // Then
    assertEquals("public", result);
  }

  @Test
  void resolveCurrentTenantIdentifier_shouldReturnPublicWhenTenantIsNull() {
    // Given
    TenantContext.setCurrentTenantId(null);

    // When
    String result = resolver.resolveCurrentTenantIdentifier();

    // Then
    assertEquals("public", result);
  }

  @Test
  void validateExistingCurrentSessions_shouldReturnFalse() {
    // When
    boolean result = resolver.validateExistingCurrentSessions();

    // Then
    assertFalse(result);
  }

  @Test
  void resolveCurrentTenantIdentifier_shouldHandleMultipleTenants() {
    // Given
    String tenant1 = "tenant-1";
    String tenant2 = "tenant-2";

    // When & Then
    TenantContext.setCurrentTenantId(tenant1);
    assertEquals(tenant1, resolver.resolveCurrentTenantIdentifier());

    TenantContext.setCurrentTenantId(tenant2);
    assertEquals(tenant2, resolver.resolveCurrentTenantIdentifier());
  }

  @Test
  void resolveCurrentTenantIdentifier_shouldHandleEmptyString() {
    // Given
    TenantContext.setCurrentTenantId("");

    // When
    String result = resolver.resolveCurrentTenantIdentifier();

    // Then - empty string is treated as a valid tenant ID, not null
    assertEquals("", result);
  }
}
