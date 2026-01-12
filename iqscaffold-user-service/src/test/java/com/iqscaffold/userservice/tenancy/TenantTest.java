package com.iqscaffold.userservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TenantTest {

  @Test
  void isActiveShouldReflectStatus() {
    var t = new Tenant("t-1", "Tenant One");
    assertTrue(t.isActive());

    t.setStatus(TenantStatus.SUSPENDED);
    assertFalse(t.isActive());
    assertTrue(t.isSuspended());

    t.setStatus(TenantStatus.ARCHIVED);
    assertFalse(t.isActive());
    assertTrue(t.isArchived());
  }

  @Test
  void displayNameShouldPreferNameElseTenantId() {
    var t = new Tenant("t-2", null);
    assertEquals("t-2", t.getDisplayName());

    t.setName("  ");
    assertEquals("t-2", t.getDisplayName());

    t.setName("Acme");
    assertEquals("Acme", t.getDisplayName());
  }

  @Test
  void quotasAndLimitsShouldBeRecognized() {
    var t = new Tenant("t-3", "T3");

    assertFalse(t.hasUserQuota());
    assertFalse(t.hasStorageQuota());
    assertFalse(t.hasRateLimit());

    t.setMaxUsers(10);
    t.setStorageQuotaGb(100);
    t.setApiRateLimitPerMinute(60);

    assertTrue(t.hasUserQuota());
    assertTrue(t.hasStorageQuota());
    assertTrue(t.hasRateLimit());

    t.setMaxUsers(0);
    t.setStorageQuotaGb(0);
    t.setApiRateLimitPerMinute(0);

    assertFalse(t.hasUserQuota());
    assertFalse(t.hasStorageQuota());
    assertFalse(t.hasRateLimit());
  }

  @Test
  void settersAndGettersShouldWork() {
    var t = new Tenant("tid", "Name");
    t.setDescription("desc");
    t.setDomain("example.com");
    t.setCreatedBy("system");

    assertEquals("tid", t.getTenantId());
    assertEquals("Name", t.getName());
    assertEquals("desc", t.getDescription());
    assertEquals("example.com", t.getDomain());
    assertEquals("system", t.getCreatedBy());
  }
}
