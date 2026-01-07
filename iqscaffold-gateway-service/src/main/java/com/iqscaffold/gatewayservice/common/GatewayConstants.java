package com.iqscaffold.gatewayservice.common;

/**
 * Common constants used across the Gateway Service.
 * Centralizes repeatable strings for HTTP headers, MDC keys, attribute names,
 * and other shared values to ensure consistency and maintainability.
 */
public final class GatewayConstants {

  private GatewayConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * HTTP Header names used for request/response tracking and context propagation.
   */
  public static final class Headers {

    private Headers() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Correlation and Tracking Headers
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_REQUEST_ID = "X-Request-ID";

    // Tenant Context Headers
    public static final String X_TENANT_ID = "X-Tenant-ID";

    // User Context Headers
    public static final String X_USER_ID = "X-User-ID";
    public static final String X_USERNAME = "X-Username";
    public static final String X_USER_ROLES = "X-User-Roles";
    public static final String X_USER_LOCALE = "X-User-Locale";

    // Gateway Identification Headers
    public static final String X_GATEWAY_SERVICE = "X-Gateway-Service";
    public static final String X_GATEWAY_VERSION = "X-Gateway-Version";
    public static final String X_REQUEST_SOURCE = "X-Request-Source";
    public static final String X_RESPONSE_SOURCE = "X-Response-Source";
    public static final String X_REQUEST_TIMESTAMP = "X-Request-Timestamp";
    public static final String X_RESPONSE_TIMESTAMP = "X-Response-Timestamp";

    // Pagination Headers
    public static final String X_TOTAL_COUNT = "X-Total-Count";
    public static final String X_PAGE_NUMBER = "X-Page-Number";
    public static final String X_PAGE_SIZE = "X-Page-Size";

    // Internal Headers (should be removed from responses)
    public static final String X_INTERNAL_SERVICE = "X-Internal-Service";
    public static final String X_INTERNAL_VERSION = "X-Internal-Version";
    public static final String X_INTERNAL_TOKEN = "X-Internal-Token";
    public static final String X_DATABASE_QUERY_TIME = "X-Database-Query-Time";
    public static final String X_CACHE_STATUS_INTERNAL = "X-Cache-Status-Internal";
    public static final String AUTHORIZATION_INTERNAL = "Authorization-Internal";

    // Security Headers
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_XSS_PROTECTION = "X-XSS-Protection";
    public static final String REFERRER_POLICY = "Referrer-Policy";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String PRAGMA = "Pragma";
    public static final String EXPIRES = "Expires";
    public static final String SERVER = "Server";
    public static final String X_POWERED_BY = "X-Powered-By";
  }

  /**
   * MDC (Mapped Diagnostic Context) keys for structured logging.
   */
  public static final class MdcKeys {

    private MdcKeys() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String REQUEST_PATH = "requestPath";
    public static final String LOCALE = "locale";
  }

  /**
   * Exchange attribute names for storing context in ServerWebExchange.
   */
  public static final class Attributes {

    private Attributes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_CONTEXT = "tenantContext";
    public static final String USER_CONTEXT = "userContext";
    public static final String REQUEST_START_TIME = "requestStartTime";
    public static final String LOCALE = "locale";
  }

  /**
   * Gateway service identification values.
   */
  public static final class ServiceInfo {

    private ServiceInfo() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String SERVICE_NAME = "iqscaffold-gateway";
    public static final String SERVICE_VERSION = "1.0.0";
    public static final String REQUEST_SOURCE_VALUE = "gateway";
    public static final String RESPONSE_SOURCE_VALUE = "gateway";
  }

  /**
   * Security header default values.
   */
  public static final class SecurityHeaderValues {

    private SecurityHeaderValues() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CSP_DEFAULT = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'";
    public static final String X_FRAME_OPTIONS_DENY = "DENY";
    public static final String X_CONTENT_TYPE_OPTIONS_NOSNIFF = "nosniff";
    public static final String X_XSS_PROTECTION_BLOCK = "1; mode=block";
    public static final String REFERRER_POLICY_STRICT = "strict-origin-when-cross-origin";
    public static final String HSTS_MAX_AGE = "max-age=31536000; includeSubDomains";
    public static final String CACHE_CONTROL_NO_CACHE = "no-cache, no-store, must-revalidate";
    public static final String PRAGMA_NO_CACHE = "no-cache";
    public static final String EXPIRES_IMMEDIATE = "0";
  }

  /**
   * Metrics and monitoring constants.
   */
  public static final class Metrics {

    private Metrics() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Metric name prefixes
    public static final String PREFIX = "iqscaffold.gateway";

    // Metric names
    public static final String REQUEST_DURATION = PREFIX + ".request.duration";
    public static final String REQUEST_SUCCESS = PREFIX + ".request.success";
    public static final String REQUEST_TOTAL = PREFIX + ".request.total";
    public static final String AUTHENTICATION_DURATION = PREFIX + ".authentication.duration";
    public static final String AUTHENTICATION_TOTAL = PREFIX + ".authentication.total";
    public static final String RATE_LIMIT_DURATION = PREFIX + ".ratelimit.duration";
    public static final String RATE_LIMIT_HIT = PREFIX + ".ratelimit.hit";
    public static final String CIRCUIT_BREAKER_OPEN = PREFIX + ".circuitbreaker.open";
    public static final String CIRCUIT_BREAKER_CLOSED = PREFIX + ".circuitbreaker.closed";
    public static final String ROUTE_LATENCY = PREFIX + ".route.latency";
    public static final String CONNECTIONS_ACTIVE = PREFIX + ".connections.active";
    public static final String REQUESTS_TOTAL = PREFIX + ".requests.total";
    public static final String RESPONSES_TOTAL = PREFIX + ".responses.total";
    public static final String TENANT_REQUESTS = PREFIX + ".tenant.requests";
    public static final String CORS_REQUESTS = PREFIX + ".cors.requests";
    public static final String TRANSFORMATION_DURATION = PREFIX + ".transformation.duration";
    public static final String LOAD_BALANCING_DECISIONS = PREFIX + ".loadbalancing.decisions";
    public static final String HEALTH_CHECK_RESULTS = PREFIX + ".healthcheck.results";

    // Metric tag names
    public static final String TAG_RESULT = "result";
    public static final String TAG_ROUTE = "route";
    public static final String TAG_REASON = "reason";
    public static final String TAG_STATUS = "status";
    public static final String TAG_STATUS_CODE = "status_code";
    public static final String TAG_STATUS_CLASS = "status_class";
    public static final String TAG_ENDPOINT = "endpoint";
    public static final String TAG_TENANT = "tenant";
    public static final String TAG_SERVICE = "service";
    public static final String TAG_METHOD = "method";
    public static final String TAG_ORIGIN = "origin";
    public static final String TAG_TYPE = "type";
    public static final String TAG_INSTANCE = "instance";

    // Metric tag values
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILURE = "failure";
    public static final String RESULT_HEALTHY = "healthy";
    public static final String RESULT_UNHEALTHY = "unhealthy";
    public static final String TENANT_UNKNOWN = "unknown";
    public static final String ORIGIN_UNKNOWN = "unknown";
  }

  /**
   * Path patterns for sensitive endpoints.
   */
  public static final class SensitivePaths {

    private SensitivePaths() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String AUTH_PATH = "/auth/";
    public static final String USERS_PATH = "/users/";
    public static final String ADMIN_PATH = "/admin/";
  }

  /**
   * Query parameter names.
   */
  public static final class QueryParams {

    private QueryParams() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String TENANT_ID = "tenantId";
  }


  /**
   * Filter order constants.
   */
  public static final class FilterOrder {

    private FilterOrder() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int CORRELATION_ID_FILTER = -300;
    public static final int LOCALE_EXTRACTION_FILTER = -250;
    public static final int TENANT_EXTRACTION_FILTER = -200;
    public static final int JWT_AUTHENTICATION_FILTER = -100;
  }
}
