package org.gripday.userservice.shared;

import org.gripday.userservice.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareTest {

  static class TestEntity extends TenantAware {
    TestEntity(String tenantId) { super(tenantId); }
  }

  @AfterEach
  void cleanup() {
    TenantContext.clear();
  }

  @Test
  void belongsToTenantShouldMatchExactTenant() {
    var entity = new TestEntity("tenant-A");

    assertTrue(entity.belongsToTenant("tenant-A"));
    assertFalse(entity.belongsToTenant("tenant-B"));
    assertFalse(entity.belongsToTenant(null));
  }

  @Test
  void belongsToCurrentTenantShouldUseTenantContext() {
    var entity = new TestEntity("tenant-42");

    TenantContext.setCurrentTenantId("tenant-42");
    assertTrue(entity.belongsToCurrentTenant());

    TenantContext.setCurrentTenantId("other");
    assertFalse(entity.belongsToCurrentTenant());
  }
}
