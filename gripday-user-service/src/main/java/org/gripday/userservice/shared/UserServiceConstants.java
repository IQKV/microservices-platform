package org.gripday.userservice.shared;

/**
 * Common constants used across the User Service.
 * Centralizes repeatable strings for HTTP headers, MDC keys, security events,
 * tenant resolution, and other shared values to ensure consistency and maintainability.
 * <p>
 * Note: JWT claim names are maintained in {@link JwtClaimNames} for backward compatibility.
 */
public final class UserServiceConstants {

  private UserServiceConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * JWT claim names for token processing.
   */
  public static final class JwtClaims {

    private JwtClaims() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String TENANT_ID = "tenant_id";
    public static final String USER_ID = "user_id";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
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

    // Rate Limiting Headers
    public static final String X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    public static final String X_RATE_LIMIT_LIMIT = "X-Rate-Limit-Limit";
    public static final String X_RATE_LIMIT_RESET = "X-Rate-Limit-Reset";

    // Standard HTTP Headers
    public static final String HOST = "Host";
    public static final String USER_AGENT = "User-Agent";
    public static final String AUTHORIZATION = "Authorization";
  }

  /**
   * MDC (Mapped Diagnostic Context) keys for structured logging.
   */
  public static final class MDC {

    private MDC() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Request Tracking
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";

    // Tenant Context
    public static final String TENANT_ID = "tenant.id";

    // User Context
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
  }

  /**
   * Security audit event types for compliance and monitoring.
   */
  public static final class SecurityEvents {

    private SecurityEvents() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Authentication Events
    public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String ACCOUNT_LOCKOUT = "ACCOUNT_LOCKOUT";

    // Password Events
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";

    // User Lifecycle Events
    public static final String USER_REGISTRATION = "USER_REGISTRATION";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";

    // Security Events
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";

    // Token Events
    public static final String TOKEN_ISSUED = "TOKEN_ISSUED";
    public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";
    public static final String TOKEN_REVOKED = "TOKEN_REVOKED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    // Event Prefixes for pattern matching
    public static final String LOGIN_PREFIX = "LOGIN_";
    public static final String LOGOUT_PREFIX = "LOGOUT_";
    public static final String PASSWORD_PREFIX = "PASSWORD_";
    public static final String ACCOUNT_PREFIX = "ACCOUNT_";
    public static final String ROLE_PREFIX = "ROLE_";

    // Event Categories
    public static final String CATEGORY_AUTHENTICATION = "AUTHENTICATION";
    public static final String CATEGORY_SECURITY = "SECURITY";
    public static final String CATEGORY_ACCOUNT_MANAGEMENT = "ACCOUNT_MANAGEMENT";
    public static final String CATEGORY_AUTHORIZATION = "AUTHORIZATION";
    public static final String CATEGORY_GENERAL = "GENERAL";
    public static final String CATEGORY_UNKNOWN = "UNKNOWN";
  }

  /**
   * Logger names for different logging categories.
   */
  public static final class Loggers {

    private Loggers() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String SECURITY = "SECURITY";
    public static final String AUDIT = "AUDIT";
    public static final String PERFORMANCE = "PERFORMANCE";
  }

  /**
   * Cache names used throughout the application.
   */
  public static final class CacheNames {

    private CacheNames() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String USERS = "users";
    public static final String TENANTS = "tenants";
    public static final String ORGANIZATIONS = "organizations";
    public static final String AUTHORITIES = "authorities";
  }

  /**
   * Tenant resolution methods indicating how tenant was identified.
   */
  public static final class TenantResolution {

    private TenantResolution() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String JWT = "JWT";
    public static final String HEADER = "HEADER";
    public static final String SUBDOMAIN = "SUBDOMAIN";
    public static final String NONE = "NONE";
  }

  /**
   * Regex patterns for validation and extraction.
   */
  public static final class Patterns {

    private Patterns() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Subdomain extraction pattern
    public static final String SUBDOMAIN_EXTRACTION = "^([a-zA-Z0-9-]+)\\.";

    // Subdomain validation pattern
    public static final String SUBDOMAIN_VALIDATION = "^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]$";

    // IP address pattern
    public static final String IP_ADDRESS = "^\\d+\\.\\d+\\.\\d+\\.\\d+";

    // Localhost pattern
    public static final String LOCALHOST = "^localhost";
  }

  /**
   * String prefixes used for ID generation.
   */
  public static final class Prefixes {

    private Prefixes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String REQUEST_ID = "req-";
    public static final String TOKEN_ID = "tok-";
  }

  /**
   * Filter order constants for servlet filters.
   */
  public static final class FilterOrder {

    private FilterOrder() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int CORRELATION_ID_FILTER = 1;
    public static final int TENANT_EXTRACTION_FILTER = 2;
    public static final int RATE_LIMITING_FILTER = 3;
  }

  /**
   * Tenant-related constants.
   */
  public static final class Tenant {

    private Tenant() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String DEFAULT_TENANT = "default";
    public static final String PUBLIC_SCHEMA = "public";
    public static final int MIN_SUBDOMAIN_LENGTH = 2;
    public static final int MAX_SUBDOMAIN_LENGTH = 63;
  }

  /**
   * Default values for various operations.
   */
  public static final class Defaults {

    private Defaults() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String DEFAULT_TENANT_ID = "default";
    public static final String LOCKOUT_DURATION_MESSAGE = "15 minutes";
  }
}
