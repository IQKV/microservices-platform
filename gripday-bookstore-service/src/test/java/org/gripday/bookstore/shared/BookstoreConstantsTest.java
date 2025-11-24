package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BookstoreConstants Tests")
class BookstoreConstantsTest {

  @Test
  @DisplayName("Should not instantiate BookstoreConstants")
  void shouldNotInstantiateBookstoreConstants() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.class);
  }

  @Test
  @DisplayName("Should not instantiate Headers class")
  void shouldNotInstantiateHeaders() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.Headers.class);
  }

  @Test
  @DisplayName("Should not instantiate MdcKeys class")
  void shouldNotInstantiateMdcKeys() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.MdcKeys.class);
  }

  @Test
  @DisplayName("Should not instantiate Attributes class")
  void shouldNotInstantiateAttributes() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.Attributes.class);
  }

  @Test
  @DisplayName("Should not instantiate CacheNames class")
  void shouldNotInstantiateCacheNames() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.CacheNames.class);
  }

  @Test
  @DisplayName("Should not instantiate JwtClaims class")
  void shouldNotInstantiateJwtClaims() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.JwtClaims.class);
  }

  @Test
  @DisplayName("Should not instantiate Metrics class")
  void shouldNotInstantiateMetrics() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.Metrics.class);
  }

  @Test
  @DisplayName("Should not instantiate AuditEvents class")
  void shouldNotInstantiateAuditEvents() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.AuditEvents.class);
  }

  @Test
  @DisplayName("Should not instantiate ResourceTypes class")
  void shouldNotInstantiateResourceTypes() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.ResourceTypes.class);
  }

  @Test
  @DisplayName("Should not instantiate Operations class")
  void shouldNotInstantiateOperations() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.Operations.class);
  }

  @Test
  @DisplayName("Should not instantiate Loggers class")
  void shouldNotInstantiateLoggers() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.Loggers.class);
  }

  @Test
  @DisplayName("Should not instantiate AnonymousUser class")
  void shouldNotInstantiateAnonymousUser() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.AnonymousUser.class);
  }

  @Test
  @DisplayName("Should not instantiate CacheKeyPrefixes class")
  void shouldNotInstantiateCacheKeyPrefixes() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.CacheKeyPrefixes.class);
  }

  @Test
  @DisplayName("Should not instantiate ApiVersions class")
  void shouldNotInstantiateApiVersions() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.ApiVersions.class);
  }

  @Test
  @DisplayName("Should not instantiate FilterOrder class")
  void shouldNotInstantiateFilterOrder() throws Exception {
    assertUtilityClassCannotBeInstantiated(BookstoreConstants.FilterOrder.class);
  }

  @Test
  @DisplayName("Should have correct header constants")
  void shouldHaveCorrectHeaderConstants() {
    assertThat(BookstoreConstants.Headers.X_CORRELATION_ID).isEqualTo("X-Correlation-ID");
    assertThat(BookstoreConstants.Headers.X_REQUEST_ID).isEqualTo("X-Request-ID");
    assertThat(BookstoreConstants.Headers.X_USER_ID).isEqualTo("X-User-ID");
    assertThat(BookstoreConstants.Headers.X_USERNAME).isEqualTo("X-Username");
    assertThat(BookstoreConstants.Headers.X_USER_ROLES).isEqualTo("X-User-Roles");
    assertThat(BookstoreConstants.Headers.AUTHORIZATION).isEqualTo("Authorization");
  }

  @Test
  @DisplayName("Should have correct MDC key constants")
  void shouldHaveCorrectMdcKeyConstants() {
    assertThat(BookstoreConstants.MdcKeys.CORRELATION_ID).isEqualTo("correlationId");
    assertThat(BookstoreConstants.MdcKeys.USER_ID).isEqualTo("userId");
    assertThat(BookstoreConstants.MdcKeys.USERNAME).isEqualTo("username");
    assertThat(BookstoreConstants.MdcKeys.AUDIT_EVENT).isEqualTo("auditEvent");
  }

  @Test
  @DisplayName("Should have correct cache name constants")
  void shouldHaveCorrectCacheNameConstants() {
    assertThat(BookstoreConstants.CacheNames.BOOK_CACHE).isEqualTo("books");
    assertThat(BookstoreConstants.CacheNames.BOOK_SEARCH_CACHE).isEqualTo("book-search");
    assertThat(BookstoreConstants.CacheNames.CATEGORY_CACHE).isEqualTo("categories");
  }

  @Test
  @DisplayName("Should have correct metric name constants")
  void shouldHaveCorrectMetricNameConstants() {
    assertThat(BookstoreConstants.Metrics.PREFIX).isEqualTo("bookstore");
    assertThat(BookstoreConstants.Metrics.BOOKS_CREATED).isEqualTo("bookstore.books.created");
    assertThat(BookstoreConstants.Metrics.BOOKS_UPDATED).isEqualTo("bookstore.books.updated");
  }

  @Test
  @DisplayName("Should have correct operation constants")
  void shouldHaveCorrectOperationConstants() {
    assertThat(BookstoreConstants.Operations.CREATE).isEqualTo("CREATE");
    assertThat(BookstoreConstants.Operations.UPDATE).isEqualTo("UPDATE");
    assertThat(BookstoreConstants.Operations.DELETE).isEqualTo("DELETE");
  }

  @Test
  @DisplayName("Should have correct resource type constants")
  void shouldHaveCorrectResourceTypeConstants() {
    assertThat(BookstoreConstants.ResourceTypes.BOOK).isEqualTo("BOOK");
    assertThat(BookstoreConstants.ResourceTypes.INVENTORY).isEqualTo("INVENTORY");
    assertThat(BookstoreConstants.ResourceTypes.CATEGORY).isEqualTo("CATEGORY");
  }

  private void assertUtilityClassCannotBeInstantiated(Class<?> clazz) throws Exception {
    Constructor<?> constructor = clazz.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(UnsupportedOperationException.class);
  }
}
