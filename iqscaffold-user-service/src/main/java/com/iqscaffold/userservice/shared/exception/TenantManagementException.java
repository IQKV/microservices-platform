package com.iqscaffold.userservice.shared.exception;

/**
 * Exception thrown when tenant management operations fail.
 * This includes tenant creation, updates, deletion, and schema provisioning.
 */
public class TenantManagementException extends RuntimeException {

  public TenantManagementException(final String message) {
    super(message);
  }

  public TenantManagementException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when a tenant already exists.
   */
  public static class TenantAlreadyExistsException extends TenantManagementException {

    private final String tenantId;

    public TenantAlreadyExistsException(final String message, final String tenantId) {
      super(message);
      this.tenantId = tenantId;
    }

    public String getTenantId() {
      return tenantId;
    }
  }

  /**
   * Exception thrown when a tenant is not found.
   */
  public static class TenantNotFoundException extends TenantManagementException {

    private final String tenantId;

    public TenantNotFoundException(final String message, final String tenantId) {
      super(message);
      this.tenantId = tenantId;
    }

    public String getTenantId() {
      return tenantId;
    }
  }

  /**
   * Exception thrown when a tenant domain already exists.
   */
  public static class DomainAlreadyExistsException extends TenantManagementException {

    private final String domain;

    public DomainAlreadyExistsException(final String message, final String domain) {
      super(message);
      this.domain = domain;
    }

    public String getDomain() {
      return domain;
    }
  }

  /**
   * Exception thrown when attempting to delete a tenant with existing users.
   */
  public static class TenantHasUsersException extends TenantManagementException {

    private final String tenantId;
    private final long userCount;

    public TenantHasUsersException(final String message, final String tenantId, final long userCount) {
      super(message);
      this.tenantId = tenantId;
      this.userCount = userCount;
    }

    public String getTenantId() {
      return tenantId;
    }

    public long getUserCount() {
      return userCount;
    }
  }

  /**
   * Exception thrown when tenant schema provisioning fails.
   */
  public static class SchemaProvisioningException extends TenantManagementException {

    private final String schema;

    public SchemaProvisioningException(final String message, final String schema, final Throwable cause) {
      super(message, cause);
      this.schema = schema;
    }

    public String getSchema() {
      return schema;
    }
  }
}
