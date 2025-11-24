package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("AuditLogger Tests")
class AuditLoggerTest {

  private AuditLogger auditLogger;
  private UserContext testUserContext;

  @BeforeEach
  void setUp() {
    auditLogger = new AuditLogger();
    testUserContext = new UserContext(
        1L,
        "john.doe",
        "john@example.com",
        Set.of("ADMIN"),
        null,
        "Engineering",
        "org-123",
        null
    );
    MDC.put("correlationId", "test-correlation-id");
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("Should log admin operation with details")
  void shouldLogAdminOperationWithDetails() {
    // Arrange
    var operation = "CREATE";
    var resourceType = "BOOK";
    var resourceId = "123";
    Map<String, Object> details = Map.of("title", "Test Book", "author", "Test Author");

    // Act
    auditLogger.logAdminOperation(operation, resourceType, resourceId, testUserContext, details);

    // Assert - MDC should be cleaned up after logging
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.OPERATION)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.RESOURCE_TYPE)).isNull();
  }

  @Test
  @DisplayName("Should log admin operation without details")
  void shouldLogAdminOperationWithoutDetails() {
    // Arrange
    var operation = "UPDATE";
    var resourceType = "INVENTORY";
    var resourceId = "456";

    // Act
    auditLogger.logAdminOperation(operation, resourceType, resourceId, testUserContext, null);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should log book creation")
  void shouldLogBookCreation() {
    // Arrange
    var bookId = 123L;
    var title = "New Book";

    // Act
    auditLogger.logBookCreation(bookId, title, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.OPERATION)).isNull();
  }

  @Test
  @DisplayName("Should log book update")
  void shouldLogBookUpdate() {
    // Arrange
    var bookId = 123L;
    var title = "Updated Book";

    // Act
    auditLogger.logBookUpdate(bookId, title, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should log book deletion")
  void shouldLogBookDeletion() {
    // Arrange
    var bookId = 123L;
    var title = "Deleted Book";

    // Act
    auditLogger.logBookDeletion(bookId, title, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should log inventory update")
  void shouldLogInventoryUpdate() {
    // Arrange
    var bookId = 123L;
    var oldQuantity = 10;
    var newQuantity = 15;

    // Act
    auditLogger.logInventoryUpdate(bookId, oldQuantity, newQuantity, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should log bulk inventory update")
  void shouldLogBulkInventoryUpdate() {
    // Arrange
    var recordsUpdated = 50;

    // Act
    auditLogger.logBulkInventoryUpdate(recordsUpdated, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should log unauthorized access")
  void shouldLogUnauthorizedAccess() {
    // Arrange
    var operation = "DELETE";
    var resourceType = "BOOK";

    // Act
    auditLogger.logUnauthorizedAccess(operation, resourceType, testUserContext);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.OPERATION)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.RESOURCE_TYPE)).isNull();
  }

  @Test
  @DisplayName("Should handle user context without department")
  void shouldHandleUserContextWithoutDepartment() {
    // Arrange
    var userContext = new UserContext(
        2L, "jane.doe", "jane@example.com",
        Set.of("USER"), null, null, null, null
    );

    // Act
    auditLogger.logBookCreation(123L, "Test Book", userContext);

    // Assert - Should not throw exception
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should handle user context without organization ID")
  void shouldHandleUserContextWithoutOrganizationId() {
    // Arrange
    var userContext = new UserContext(
        2L, "jane.doe", "jane@example.com",
        Set.of("USER"), null, "Engineering", null, null
    );

    // Act
    auditLogger.logInventoryUpdate(123L, 5, 10, userContext);

    // Assert - Should not throw exception
    assertThat(MDC.get(BookstoreConstants.MdcKeys.AUDIT_EVENT)).isNull();
  }

  @Test
  @DisplayName("Should preserve correlation ID from MDC")
  void shouldPreserveCorrelationIdFromMdc() {
    // Arrange
    var correlationId = "preserved-correlation-id";
    MDC.put("correlationId", correlationId);

    // Act
    auditLogger.logBookCreation(123L, "Test Book", testUserContext);

    // Assert - Correlation ID should still be in MDC
    assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
  }
}
