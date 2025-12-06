package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when usage-related operations fail.
 */
public class UsageException extends BillingException {

    public UsageException(String message) {
        super(message);
    }

    public UsageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when a quota is exceeded.
     */
    public static class QuotaExceededException extends UsageException {
        
        private final String metricType;
        private final long limit;
        private final long used;

        public QuotaExceededException(String metricType, long limit, long used) {
            super("Quota exceeded for " + metricType + ": used " + used + " of " + limit);
            this.metricType = metricType;
            this.limit = limit;
            this.used = used;
        }

        public QuotaExceededException(String metricType, long limit, long used, Throwable cause) {
            super("Quota exceeded for " + metricType + ": used " + used + " of " + limit, cause);
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

        public UsageLimitExceededException(String metricType, long limit) {
            super("Usage limit exceeded for " + metricType + ": limit is " + limit);
            this.metricType = metricType;
            this.limit = limit;
        }

        public UsageLimitExceededException(String metricType, long limit, Throwable cause) {
            super("Usage limit exceeded for " + metricType + ": limit is " + limit, cause);
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
