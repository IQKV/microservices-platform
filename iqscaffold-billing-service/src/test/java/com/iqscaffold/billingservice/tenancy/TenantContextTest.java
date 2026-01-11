package com.iqscaffold.billingservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
