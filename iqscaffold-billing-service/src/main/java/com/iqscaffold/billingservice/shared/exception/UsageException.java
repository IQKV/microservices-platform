package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when usage-related operations fail.
 */
public class UsageException extends BillingException {

  public UsageException(final String message) {
    super(message);
  }

  public UsageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public UsageException(final String errorCode, final String message) {
    super(errorCode, message);
  }

  public UsageException(final String errorCode, final String message, final Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Exception thrown when a quota is exceeded.
   */
  public static class QuotaExceededException extends UsageException {

    private final String metricType;
    private final long limit;
    private final long used;

    public QuotaExceededException(final String metricType, final long limit, final long used) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.QUOTA_EXCEEDED,
          "Quota exceeded for " + metricType + ": used " + used + " of " + limit);
      this.metricType = metricType;
      this.limit = limit;
      this.used = used;
    }

    public QuotaExceededException(final String metricType, final long limit, final long used, final Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.QUOTA_EXCEEDED,
          "Quota exceeded for " + metricType + ": used " + used + " of " + limit, cause);
      this.metricType = metricType;
      this.limit = limit;
      this.used = used;
    }

    public String getMetricType() {
      return metricType;
    }

    public long getLimit() {
      return limit;
    }

    public long getUsed() {
      return used;
    }
  }

  /**
   * Exception thrown when a usage limit is exceeded.
   */
  public static class UsageLimitExceededException extends UsageException {

    private final String metricType;
    private final long limit;

    public UsageLimitExceededException(final String metricType, final long limit) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.USAGE_LIMIT_EXCEEDED,
          "Usage limit exceeded for " + metricType + ": limit is " + limit);
      this.metricType = metricType;
      this.limit = limit;
    }

    public UsageLimitExceededException(final String metricType, final long limit, final Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.USAGE_LIMIT_EXCEEDED,
          "Usage limit exceeded for " + metricType + ": limit is " + limit, cause);
      this.metricType = metricType;
      this.limit = limit;
    }

    public String getMetricType() {
      return metricType;
    }

    public long getLimit() {
      return limit;
    }
  }
}
