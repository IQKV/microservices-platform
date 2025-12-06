package com.iqscaffold.billingservice.shared;

/**
 * Central constants for the Billing Service.
 * 
 * <p>Organizes constants into nested static classes for better organization and discoverability.
 * All constant fields are public static final with UPPER_SNAKE_CASE naming.
 */
public final class BillingConstants {

    private BillingConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * HTTP header constants for request/response handling.
     */
    public static final class Headers {
        public static final String TENANT_ID = "X-Tenant-ID";
        public static final String CORRELATION_ID = "X-Correlation-ID";
        public static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";
        public static final String API_VERSION = "X-API-Version";
        public static final String WEBHOOK_SIGNATURE = "X-Webhook-Signature";

        private Headers() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * MDC (Mapped Diagnostic Context) keys for structured logging.
     */
    public static final class MDC {
        public static final String TENANT_ID = "tenantId";
        public static final String USER_ID = "userId";
        public static final String CORRELATION_ID = "correlationId";
        public static final String SUBSCRIPTION_ID = "subscriptionId";
        public static final String INVOICE_ID = "invoiceId";
        public static final String PAYMENT_ID = "paymentId";

        private MDC() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Subscription status constants.
     */
    public static final class SubscriptionStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String TRIAL = "TRIAL";
        public static final String PAST_DUE = "PAST_DUE";
        public static final String CANCELED = "CANCELED";
        public static final String EXPIRED = "EXPIRED";
        public static final String PAUSED = "PAUSED";

        private SubscriptionStatus() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Billing event names for RabbitMQ messaging.
     */
    public static final class BillingEvents {
        public static final String SUBSCRIPTION_CREATED = "billing.subscription.created";
        public static final String SUBSCRIPTION_UPGRADED = "billing.subscription.upgraded";
        public static final String SUBSCRIPTION_DOWNGRADED = "billing.subscription.downgraded";
        public static final String SUBSCRIPTION_CANCELED = "billing.subscription.canceled";
        public static final String SUBSCRIPTION_REACTIVATED = "billing.subscription.reactivated";
        public static final String TRIAL_STARTED = "billing.trial.started";
        public static final String TRIAL_ENDING = "billing.trial.ending";
        public static final String TRIAL_ENDED = "billing.trial.ended";
        public static final String INVOICE_GENERATED = "billing.invoice.generated";
        public static final String INVOICE_PAID = "billing.invoice.paid";
        public static final String INVOICE_VOIDED = "billing.invoice.voided";
        public static final String PAYMENT_SUCCEEDED = "billing.payment.succeeded";
        public static final String PAYMENT_FAILED = "billing.payment.failed";
        public static final String PAYMENT_REFUNDED = "billing.payment.refunded";
        public static final String USAGE_RECORDED = "billing.usage.recorded";
        public static final String QUOTA_EXCEEDED = "billing.quota.exceeded";

        private BillingEvents() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Cache names for Redis caching.
     */
    public static final class CacheNames {
        public static final String SUBSCRIPTIONS = "subscriptions";
        public static final String PLANS = "plans";
        public static final String USAGE = "usage";
        public static final String QUOTAS = "quotas";
        public static final String PAYMENT_METHODS = "payment-methods";
        public static final String INVOICES = "invoices";

        private CacheNames() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Payment provider identifiers.
     */
    public static final class PaymentProviders {
        public static final String STRIPE = "stripe";
        public static final String PAYPAL = "paypal";
        public static final String MANUAL = "manual";

        private PaymentProviders() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Invoice format constants.
     */
    public static final class InvoiceFormat {
        public static final String PDF = "PDF";
        public static final String HTML = "HTML";
        public static final String JSON = "JSON";

        private InvoiceFormat() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Default values for billing operations.
     */
    public static final class Defaults {
        public static final String CURRENCY = "USD";
        public static final int TRIAL_DAYS = 14;
        public static final int GRACE_PERIOD_DAYS = 3;
        public static final int INVOICE_DUE_DAYS = 7;
        public static final int USAGE_RETENTION_DAYS = 365;
        public static final int PAYMENT_RETRY_ATTEMPTS = 4;

        private Defaults() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Metric type identifiers for usage tracking.
     */
    public static final class MetricTypes {
        public static final String API_CALLS = "API_CALLS";
        public static final String STORAGE_GB = "STORAGE_GB";
        public static final String EMAIL_SENDS = "EMAIL_SENDS";
        public static final String CAMPAIGN_EXECUTIONS = "CAMPAIGN_EXECUTIONS";
        public static final String SCORING_REQUESTS = "SCORING_REQUESTS";
        public static final String ACTIVE_USERS = "ACTIVE_USERS";
        public static final String CUSTOM_DOMAINS = "CUSTOM_DOMAINS";
        public static final String DATA_EXPORTS = "DATA_EXPORTS";

        private MetricTypes() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Error codes for billing operations.
     */
    public static final class ErrorCodes {
        public static final String SUBSCRIPTION_NOT_FOUND = "BILLING_001";
        public static final String SUBSCRIPTION_ALREADY_EXISTS = "BILLING_002";
        public static final String INVALID_SUBSCRIPTION_STATE = "BILLING_003";
        public static final String PLAN_NOT_FOUND = "BILLING_004";
        public static final String INVALID_PLAN_TRANSITION = "BILLING_005";
        public static final String PAYMENT_FAILED = "BILLING_006";
        public static final String PAYMENT_METHOD_NOT_FOUND = "BILLING_007";
        public static final String INVALID_PAYMENT_METHOD = "BILLING_008";
        public static final String QUOTA_EXCEEDED = "BILLING_009";
        public static final String USAGE_LIMIT_EXCEEDED = "BILLING_010";
        public static final String INVOICE_NOT_FOUND = "BILLING_011";
        public static final String INVOICE_ALREADY_PAID = "BILLING_012";

        private ErrorCodes() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }
}
