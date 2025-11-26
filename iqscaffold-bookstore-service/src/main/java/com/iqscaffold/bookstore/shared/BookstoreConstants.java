package com.iqscaffold.bookstore.shared;

/**
 * Common constants used across the Bookstore Service.
 * Centralizes repeatable strings for HTTP headers, MDC keys, cache names,
 * metric names, and other shared values to ensure consistency and maintainability.
 */
public final class BookstoreConstants {

  private BookstoreConstants() {
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

    // User Context Headers (propagated from Gateway)
    public static final String X_USER_ID = "X-User-ID";
    public static final String X_USERNAME = "X-Username";
    public static final String X_USER_ROLES = "X-User-Roles";

    // Pagination Headers
    public static final String X_TOTAL_COUNT = "X-Total-Count";
    public static final String X_PAGE_NUMBER = "X-Page-Number";
    public static final String X_PAGE_SIZE = "X-Page-Size";

    // Authorization Header
    public static final String AUTHORIZATION = "Authorization";
  }

  /**
   * MDC (Mapped Diagnostic Context) keys for structured logging.
   */
  public static final class MdcKeys {

    private MdcKeys() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Request Tracking
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";

    // User Context
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String USER_ROLES = "userRoles";
    public static final String DEPARTMENT = "department";
    public static final String ORGANIZATION_ID = "organizationId";

    // Audit Context
    public static final String AUDIT_EVENT = "auditEvent";
    public static final String OPERATION = "operation";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String RESOURCE_ID = "resourceId";
  }

  /**
   * Request attribute names for storing context in HttpServletRequest.
   */
  public static final class Attributes {

    private Attributes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String USER_CONTEXT = "userContext";
    public static final String CORRELATION_ID = "correlationId";
  }

  /**
   * Cache names used throughout the application.
   */
  public static final class CacheNames {

    private CacheNames() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String BOOK_CACHE = "books";
    public static final String BOOK_SEARCH_CACHE = "book-search";
    public static final String CATEGORY_CACHE = "categories";
    public static final String AUTHOR_CACHE = "authors";
    public static final String POPULAR_BOOKS_CACHE = "popular-books";
  }

  /**
   * JWT claim names for extracting user context from tokens.
   */
  public static final class JwtClaims {

    private JwtClaims() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Standard JWT Claims
    public static final String ISSUER = "iss";
    public static final String SUBJECT = "sub";
    public static final String AUDIENCE = "aud";
    public static final String EXPIRATION = "exp";
    public static final String NOT_BEFORE = "nbf";
    public static final String ISSUED_AT = "iat";
    public static final String JWT_ID = "jti";

    // Custom IQ Scaffold Claims
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
    public static final String AUTHORITIES = "authorities";
    public static final String PERMISSIONS = "permissions";
    public static final String SCOPE = "scope";
    public static final String DEPARTMENT = "department";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String REALM_ACCESS = "realm_access";
  }

  /**
   * Metric names for monitoring and observability.
   */
  public static final class Metrics {

    private Metrics() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Metric name prefix
    public static final String PREFIX = "bookstore";

    // Counter metrics
    public static final String BOOKS_CREATED = PREFIX + ".books.created";
    public static final String BOOKS_UPDATED = PREFIX + ".books.updated";
    public static final String BOOKS_DELETED = PREFIX + ".books.deleted";
    public static final String BOOKS_SEARCHED = PREFIX + ".books.searched";
    public static final String INVENTORY_UPDATED = PREFIX + ".inventory.updated";
    public static final String UNAUTHORIZED_ACCESS = PREFIX + ".security.unauthorized_access";

    // Timer metrics
    public static final String BOOK_SEARCH_DURATION = PREFIX + ".books.search.duration";
    public static final String BOOK_CREATION_DURATION = PREFIX + ".books.creation.duration";
    public static final String INVENTORY_UPDATE_DURATION = PREFIX + ".inventory.update.duration";

    // Gauge metrics
    public static final String BOOKS_TOTAL = PREFIX + ".books.total";
    public static final String BOOKS_AVAILABLE = PREFIX + ".books.available";
    public static final String INVENTORY_LOW_STOCK = PREFIX + ".inventory.low_stock";
    public static final String INVENTORY_OUT_OF_STOCK = PREFIX + ".inventory.out_of_stock";
    public static final String INVENTORY_TOTAL_QUANTITY = PREFIX + ".inventory.total_quantity";

    // Metric descriptions
    public static final String DESC_BOOKS_CREATED = "Number of books created";
    public static final String DESC_BOOKS_UPDATED = "Number of books updated";
    public static final String DESC_BOOKS_DELETED = "Number of books deleted";
    public static final String DESC_BOOKS_SEARCHED = "Number of book searches performed";
    public static final String DESC_INVENTORY_UPDATED = "Number of inventory updates";
    public static final String DESC_UNAUTHORIZED_ACCESS = "Number of unauthorized access attempts";
    public static final String DESC_BOOK_SEARCH_DURATION = "Time taken for book searches";
    public static final String DESC_BOOK_CREATION_DURATION = "Time taken for book creation";
    public static final String DESC_INVENTORY_UPDATE_DURATION = "Time taken for inventory updates";
    public static final String DESC_BOOKS_TOTAL = "Total number of books in catalog";
    public static final String DESC_BOOKS_AVAILABLE = "Number of available books";
    public static final String DESC_INVENTORY_LOW_STOCK = "Number of books with low stock";
    public static final String DESC_INVENTORY_OUT_OF_STOCK = "Number of books out of stock";
    public static final String DESC_INVENTORY_TOTAL_QUANTITY = "Total inventory quantity across all books";
  }

  /**
   * Audit event types for security and compliance logging.
   */
  public static final class AuditEvents {

    private AuditEvents() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String ADMIN_OPERATION = "ADMIN_OPERATION";
    public static final String UNAUTHORIZED_ACCESS_ATTEMPT = "UNAUTHORIZED_ACCESS_ATTEMPT";
    public static final String BOOK_CREATED = "BOOK_CREATED";
    public static final String BOOK_UPDATED = "BOOK_UPDATED";
    public static final String BOOK_DELETED = "BOOK_DELETED";
    public static final String INVENTORY_UPDATED = "INVENTORY_UPDATED";
    public static final String BULK_INVENTORY_UPDATE = "BULK_INVENTORY_UPDATE";
  }

  /**
   * Resource types for audit logging.
   */
  public static final class ResourceTypes {

    private ResourceTypes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String BOOK = "BOOK";
    public static final String INVENTORY = "INVENTORY";
    public static final String CATEGORY = "CATEGORY";
  }

  /**
   * Operation types for audit logging.
   */
  public static final class Operations {

    private Operations() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String READ = "READ";
    public static final String SEARCH = "SEARCH";
    public static final String BULK_UPDATE = "BULK_UPDATE";
  }

  /**
   * Logger names for different logging categories.
   */
  public static final class Loggers {

    private Loggers() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String AUDIT = "AUDIT";
    public static final String SECURITY = "SECURITY";
    public static final String PERFORMANCE = "PERFORMANCE";
  }

  /**
   * Anonymous user constants for unauthenticated requests.
   */
  public static final class AnonymousUser {

    private AnonymousUser() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String USER_ID = "anonymous";
    public static final String USERNAME = "anonymous";
    public static final String ROLES = "NONE";
  }

  /**
   * Cache key prefixes for building cache keys.
   */
  public static final class CacheKeyPrefixes {

    private CacheKeyPrefixes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String ISBN = "isbn_";
    public static final String TITLE = "title_";
    public static final String AUTHOR = "author_";
    public static final String CATEGORY = "category_";
    public static final String AVAILABLE = "available_";
    public static final String IN_STOCK = "in_stock_";
    public static final String CATALOG_RESPONSE = "catalog_response_";
    public static final String FULLTEXT = "fulltext_";
    public static final String FUZZY = "fuzzy_";
    public static final String PRICE_RANGE = "price_range_";
    public static final String DISTINCT_AUTHORS = "distinct_authors";
    public static final String DISTINCT_CATEGORIES = "distinct_categories";
  }

  /**
   * API versioning constants.
   */
  public static final class ApiVersions {

    private ApiVersions() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String V1 = "v1";
    public static final String V2 = "v2";
    public static final String CURRENT = V1;
  }

  /**
   * Filter order constants for servlet filters.
   */
  public static final class FilterOrder {

    private FilterOrder() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int CORRELATION_ID_FILTER = 1;
    public static final int USER_CONTEXT_MDC_FILTER = 2;
  }
}
