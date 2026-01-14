package com.iqscaffold.contactservice.shared;

public final class ContactConstants {
  private ContactConstants() {
  }

  public static final class MDC {
    private MDC() {
    }

    public static final String USER_ID = "user_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String CONTACT_ID = "contact_id";
  }

  /**
   * Default values for various operations.
   */
  public static final class Defaults {
    private Defaults() {
    }

    public static final String DEFAULT_TENANT_ID = "default";
  }
}
