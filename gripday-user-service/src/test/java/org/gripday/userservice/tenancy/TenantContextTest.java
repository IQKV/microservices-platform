package org.gripday.userservice.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

  @AfterEach
  void cleanup() {
    TenantContext.clear();
  }

  @Test
  void setGetAndClearShouldManageThreadLocalAndMdc() {
    assertNull(TenantContext.getCurrentTenantId());

    TenantContext.setCurrentTenantId("acme");
    assertEquals("acme", TenantContext.getCurrentTenantId());
    assertEquals("acme", MDC.get("tenant.id"));

    TenantContext.clear();
    assertNull(TenantContext.getCurrentTenantId());
    assertNull(MDC.get("tenant.id"));
  }

  @Test
  void defaultTenantAndIsCurrentTenant() {
    assertFalse(TenantContext.hasTenantContext());
    assertEquals("default", TenantContext.getCurrentTenantIdOrDefault());

    TenantContext.setCurrentTenantId("t1");
    assertTrue(TenantContext.hasTenantContext());
    assertTrue(TenantContext.isCurrentTenant("t1"));
    assertFalse(TenantContext.isCurrentTenant("t2"));
  }

  @Test
  void executeInTenantContextRunnableShouldRestorePreviousContext() {
    TenantContext.setCurrentTenantId("base");

    TenantContext.executeInTenantContext("child", () -> {
      assertEquals("child", TenantContext.getCurrentTenantId());
    });

    assertEquals("base", TenantContext.getCurrentTenantId());
  }

  @Test
  void executeInTenantContextSupplierShouldReturnValueAndRestore() {
    assertNull(TenantContext.getCurrentTenantId());

    var result = TenantContext.executeInTenantContext("x", () -> {
      assertEquals("x", TenantContext.getCurrentTenantId());
      return TenantContext.createTenantAwareCacheKey("k");
    });

    assertEquals("x:k", result);
    assertNull(TenantContext.getCurrentTenantId());
  }

  @Test
  void createTenantAwareCacheKeyShouldPrefixTenant() {
    // without context uses default
    assertEquals("default:key", TenantContext.createTenantAwareCacheKey("key"));
    assertEquals("default|key", TenantContext.createTenantAwareCacheKey("key", "|"));

    TenantContext.setCurrentTenantId("acme");
    assertEquals("acme:key", TenantContext.createTenantAwareCacheKey("key"));
    assertEquals("acme/key", TenantContext.createTenantAwareCacheKey("key", "/"));
  }
}
