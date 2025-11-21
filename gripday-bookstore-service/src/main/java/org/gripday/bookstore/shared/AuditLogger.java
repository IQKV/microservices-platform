package org.gripday.bookstore.shared;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

  private static final Logger auditLog = LoggerFactory.getLogger(BookstoreConstants.Loggers.AUDIT);

  public void logAdminOperation(String operation, String resourceType, String resourceId,
                                UserContext userContext, Map<String, Object> details) {

    // Add audit-specific MDC entries
    MDC.put(BookstoreConstants.MdcKeys.AUDIT_EVENT, BookstoreConstants.AuditEvents.ADMIN_OPERATION);
    MDC.put(BookstoreConstants.MdcKeys.OPERATION, operation);
    MDC.put(BookstoreConstants.MdcKeys.RESOURCE_TYPE, resourceType);
    MDC.put("resourceId", resourceId);

    try {
      var auditEntry = Map.<String, Object>of(
          "timestamp", Instant.now().toString(),
          "operation", operation,
          "resourceType", resourceType,
          "resourceId", resourceId,
          "userId", userContext.userId(),
          "username", userContext.username(),
          "userRoles", userContext.roles(),
          "department", userContext.department() != null ? userContext.department() : "N/A",
          "organizationId", userContext.organizationId() != null ? userContext.organizationId() : "N/A",
          "correlationId", MDC.get("correlationId")
      );

      auditLog.info("Admin operation performed: {} Details: {}", auditEntry, details != null ? details : Map.of());
    } finally {
      // Clean up audit-specific MDC entries
      MDC.remove(BookstoreConstants.MdcKeys.AUDIT_EVENT);
      MDC.remove(BookstoreConstants.MdcKeys.OPERATION);
      MDC.remove(BookstoreConstants.MdcKeys.RESOURCE_TYPE);
      MDC.remove(BookstoreConstants.MdcKeys.RESOURCE_ID);
    }
  }

  public void logBookCreation(Long bookId, String title, UserContext userContext) {
    logAdminOperation(BookstoreConstants.Operations.CREATE, BookstoreConstants.ResourceTypes.BOOK,
        String.valueOf(bookId), userContext, Map.of("title", title));
  }

  public void logBookUpdate(Long bookId, String title, UserContext userContext) {
    logAdminOperation(BookstoreConstants.Operations.UPDATE, BookstoreConstants.ResourceTypes.BOOK,
        String.valueOf(bookId), userContext, Map.of("title", title));
  }

  public void logBookDeletion(Long bookId, String title, UserContext userContext) {
    logAdminOperation(BookstoreConstants.Operations.DELETE, BookstoreConstants.ResourceTypes.BOOK,
        String.valueOf(bookId), userContext, Map.of("title", title));
  }

  public void logInventoryUpdate(Long bookId, int oldQuantity, int newQuantity, UserContext userContext) {
    logAdminOperation(BookstoreConstants.Operations.UPDATE, BookstoreConstants.ResourceTypes.INVENTORY,
        String.valueOf(bookId), userContext, Map.of("oldQuantity", oldQuantity, "newQuantity", newQuantity));
  }

  public void logBulkInventoryUpdate(int recordsUpdated, UserContext userContext) {
    logAdminOperation(BookstoreConstants.Operations.BULK_UPDATE, BookstoreConstants.ResourceTypes.INVENTORY,
        "MULTIPLE", userContext, Map.of("recordsUpdated", recordsUpdated));
  }

  public void logUnauthorizedAccess(String operation, String resourceType, UserContext userContext) {
    // Add security audit MDC entries
    MDC.put(BookstoreConstants.MdcKeys.AUDIT_EVENT, BookstoreConstants.AuditEvents.UNAUTHORIZED_ACCESS_ATTEMPT);
    MDC.put(BookstoreConstants.MdcKeys.OPERATION, operation);
    MDC.put(BookstoreConstants.MdcKeys.RESOURCE_TYPE, resourceType);

    try {
      var auditEntry = Map.<String, Object>of(
          "timestamp", Instant.now().toString(),
          "event", "UNAUTHORIZED_ACCESS_ATTEMPT",
          "operation", operation,
          "resourceType", resourceType,
          "userId", userContext.userId(),
          "username", userContext.username(),
          "userRoles", userContext.roles(),
          "department", userContext.department() != null ? userContext.department() : "N/A",
          "organizationId", userContext.organizationId() != null ? userContext.organizationId() : "N/A",
          "correlationId", MDC.get("correlationId")
      );

      auditLog.warn("Unauthorized access attempt: {}", auditEntry);
    } finally {
      // Clean up security audit MDC entries
      MDC.remove(BookstoreConstants.MdcKeys.AUDIT_EVENT);
      MDC.remove(BookstoreConstants.MdcKeys.OPERATION);
      MDC.remove(BookstoreConstants.MdcKeys.RESOURCE_TYPE);
    }
  }
}
