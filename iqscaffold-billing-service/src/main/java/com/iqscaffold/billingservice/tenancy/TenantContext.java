package com.iqscaffold.billingservice.tenancy;

import org.slf4j.MDC;

public final class TenantContext {
  private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
  private static final String MDC_KEY = "tenant_id";

  private TenantContext() {
  }

  public static void setCurrentTenantId(String tenantId) {
    CURRENT_TENANT.set(tenantId);
    if (tenantId != null) {
      MDC.put(MDC_KEY, tenantId);
    } else {
      MDC.remove(MDC_KEY);
    }
  }

  public static String getCurrentTenantId() {
    return CURRENT_TENANT.get();
  }

  public static void clear() {
    CURRENT_TENANT.remove();
    MDC.remove(MDC_KEY);
  }
}
