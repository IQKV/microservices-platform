package com.iqscaffold.billingservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.iqscaffold.billingservice.shared.exception.TenantContextException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TenantContextTest {

  @BeforeEach
  void setUp() {
    TenantContext.clear();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    MDC.clear();
  }

  @Test
  void setCurrentTenantId_shouldSetTenantId() {
    // When
    TenantContext.setCurrentTenantId("tenant123");

    // Then
    assertEquals("tenant123", TenantContext.getCurrentTenantId());
  }

  @Test
  void setCurrentTenantId_shouldSetMDC() {
    // When
    TenantContext.setCurrentTenantId("tenant456");

    // Then
    assertEquals("tenant456", MDC.get("tenant_id"));
  }

  @Test
  void setCurrentTenantId_shouldHandleNull() {
    // Given
    TenantContext.setCurrentTenantId("tenant123");

    // When / Then
    assertThrows(TenantContextException.InvalidTenantIdException.class, () -> {
      TenantContext.setCurrentTenantId(null);
    });
    
    // Verify original context remains unchanged
    assertEquals("tenant123", TenantContext.getCurrentTenantId());
  }

  @Test
  void setCurrentTenantId_shouldOverwritePreviousValue() {
    // Given
    TenantContext.setCurrentTenantId("tenant1");

    // When
    TenantContext.setCurrentTenantId("tenant2");

    // Then
    assertEquals("tenant2", TenantContext.getCurrentTenantId());
    assertEquals("tenant2", MDC.get("tenant_id"));
  }

  @Test
  void getCurrentTenantId_shouldReturnNullWhenNotSet() {
    // When
    String result = TenantContext.getCurrentTenantId();

    // Then
    assertNull(result);
  }

  @Test
  void clear_shouldRemoveTenantId() {
    // Given
    TenantContext.setCurrentTenantId("tenant123");

    // When
    TenantContext.clear();

    // Then
    assertNull(TenantContext.getCurrentTenantId());
  }

  @Test
  void clear_shouldRemoveMDC() {
    // Given
    TenantContext.setCurrentTenantId("tenant123");

    // When
    TenantContext.clear();

    // Then
    assertNull(MDC.get("tenant_id"));
  }

  @Test
  void clear_shouldBeIdempotent() {
    // Given
    TenantContext.setCurrentTenantId("tenant123");
    TenantContext.clear();

    // When
    TenantContext.clear();

    // Then
    assertNull(TenantContext.getCurrentTenantId());
    assertNull(MDC.get("tenant_id"));
  }

  @Test
  void tenantContext_shouldBeThreadLocal() throws InterruptedException {
    // Given
    TenantContext.setCurrentTenantId("main-tenant");

    // When
    Thread thread = new Thread(() -> {
      TenantContext.setCurrentTenantId("thread-tenant");
      assertEquals("thread-tenant", TenantContext.getCurrentTenantId());
    });
    thread.start();
    thread.join();

    // Then
    assertEquals("main-tenant", TenantContext.getCurrentTenantId());
  }

  @Test
  void setCurrentTenantId_shouldHandleEmptyString() {
    // When / Then
    assertThrows(TenantContextException.InvalidTenantIdException.class, () -> {
      TenantContext.setCurrentTenantId("");
    });
  }

  @Test
  void setCurrentTenantId_shouldHandleSpecialCharacters() {
    // When
    String specialTenantId = "tenant-123_ABC.xyz";
    TenantContext.setCurrentTenantId(specialTenantId);

    // Then
    assertEquals(specialTenantId, TenantContext.getCurrentTenantId());
    assertEquals(specialTenantId, MDC.get("tenant_id"));
  }

  @Test
  void getCurrentTenantIdOrDefault_shouldReturnTenantIdWhenSet() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    String result = TenantContext.getCurrentTenantIdOrDefault();

    // Then
    assertEquals("tenant-123", result);
  }

  @Test
  void getCurrentTenantIdOrDefault_shouldReturnDefaultWhenNotSet() {
    // When
    String result = TenantContext.getCurrentTenantIdOrDefault();

    // Then
    assertEquals(TenantContext.getDefaultTenantId(), result);
  }

  @Test
  void hasTenantContext_shouldReturnTrueWhenSet() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    boolean result = TenantContext.hasTenantContext();

    // Then
    assertTrue(result);
  }

  @Test
  void hasTenantContext_shouldReturnFalseWhenNotSet() {
    // When
    boolean result = TenantContext.hasTenantContext();

    // Then
    assertFalse(result);
  }

  @Test
  void isCurrentTenant_shouldReturnTrueForMatchingTenant() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    boolean result = TenantContext.isCurrentTenant("tenant-123");

    // Then
    assertTrue(result);
  }

  @Test
  void isCurrentTenant_shouldReturnFalseForDifferentTenant() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    boolean result = TenantContext.isCurrentTenant("tenant-456");

    // Then
    assertFalse(result);
  }

  @Test
  void isCurrentTenant_shouldReturnFalseWhenNoContext() {
    // When
    boolean result = TenantContext.isCurrentTenant("tenant-123");

    // Then
    assertFalse(result);
  }

  @Test
  void executeInTenantContext_shouldExecuteWithSpecifiedTenant() {
    // Given
    TenantContext.setCurrentTenantId("original-tenant");
    final String[] capturedTenant = new String[1];

    // When
    TenantContext.executeInTenantContext("temp-tenant", () -> {
      capturedTenant[0] = TenantContext.getCurrentTenantId();
    });

    // Then
    assertEquals("temp-tenant", capturedTenant[0]);
    assertEquals("original-tenant", TenantContext.getCurrentTenantId());
  }

  @Test
  void executeInTenantContext_shouldRestoreOriginalContext() {
    // Given
    TenantContext.setCurrentTenantId("original-tenant");

    // When
    TenantContext.executeInTenantContext("temp-tenant", () -> {
      // Execute some operation
    });

    // Then
    assertEquals("original-tenant", TenantContext.getCurrentTenantId());
  }

  @Test
  void executeInTenantContext_shouldClearContextWhenNoPrevious() {
    // When
    TenantContext.executeInTenantContext("temp-tenant", () -> {
      // Execute some operation
    });

    // Then
    assertNull(TenantContext.getCurrentTenantId());
  }

  @Test
  void executeInTenantContextWithSupplier_shouldReturnResult() {
    // Given
    TenantContext.setCurrentTenantId("original-tenant");

    // When
    String result = TenantContext.executeInTenantContext("temp-tenant", () -> {
      return "result-from-" + TenantContext.getCurrentTenantId();
    });

    // Then
    assertEquals("result-from-temp-tenant", result);
    assertEquals("original-tenant", TenantContext.getCurrentTenantId());
  }

  @Test
  void createTenantAwareCacheKey_shouldPrefixWithTenantId() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    String cacheKey = TenantContext.createTenantAwareCacheKey("payment:12345");

    // Then
    assertEquals("tenant-123:payment:12345", cacheKey);
  }

  @Test
  void createTenantAwareCacheKey_shouldUseDefaultTenantWhenNotSet() {
    // When
    String cacheKey = TenantContext.createTenantAwareCacheKey("payment:12345");

    // Then
    assertEquals(TenantContext.getDefaultTenantId() + ":payment:12345", cacheKey);
  }

  @Test
  void createTenantAwareCacheKeyWithSeparator_shouldUseCustomSeparator() {
    // Given
    TenantContext.setCurrentTenantId("tenant-123");

    // When
    String cacheKey = TenantContext.createTenantAwareCacheKey("payment:12345", "_");

    // Then
    assertEquals("tenant-123_payment:12345", cacheKey);
  }

  @Test
  void getDefaultTenantId_shouldReturnDefaultValue() {
    // When
    String defaultId = TenantContext.getDefaultTenantId();

    // Then
    assertNotNull(defaultId);
  }
}
