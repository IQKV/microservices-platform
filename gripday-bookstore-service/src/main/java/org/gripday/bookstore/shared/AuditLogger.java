package org.gripday.bookstore.shared;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

  private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

  public void logAdminOperation(String operation, String resourceType, String resourceId,
      UserContext userContext, Map<String, Object> details) {

    // Add audit-specific MDC entries
    MDC.put("auditEvent", "ADMIN_OPERATION");
    MDC.put("operation", operation);
    MDC.put("resourceType", resourceType);
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
      MDC.remove("auditEvent");
      MDC.remove("operation");
      MDC.remove("resourceType");
      MDC.remove("resourceId");
    }
  }

  public void logBookCreation(Long bookId, String title, UserContext userContext) {
    logAdminOperation("CREATE", "BOOK", String.valueOf(bookId), userContext,
        Map.of("title", title));
  }

  public void logBookUpdate(Long bookId, String title, UserContext userContext) {
    logAdminOperation("UPDATE", "BOOK", String.valueOf(bookId), userContext,
        Map.of("title", title));
  }

  public void logBookDeletion(Long bookId, String title, UserContext userContext) {
    logAdminOperation("DELETE", "BOOK", String.valueOf(bookId), userContext,
        Map.of("title", title));
  }

  public void logInventoryUpdate(Long bookId, int oldQuantity, int newQuantity, UserContext userContext) {
    logAdminOperation("UPDATE", "INVENTORY", String.valueOf(bookId), userContext,
        Map.of("oldQuantity", oldQuantity, "newQuantity", newQuantity));
  }

  public void logBulkInventoryUpdate(int recordsUpdated, UserContext userContext) {
    logAdminOperation("BULK_UPDATE", "INVENTORY", "MULTIPLE", userContext,
        Map.of("recordsUpdated", recordsUpdated));
  }

  public void logUnauthorizedAccess(String operation, String resourceType, UserContext userContext) {
    // Add security audit MDC entries
    MDC.put("auditEvent", "UNAUTHORIZED_ACCESS_ATTEMPT");
    MDC.put("operation", operation);
    MDC.put("resourceType", resourceType);

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
      MDC.remove("auditEvent");
      MDC.remove("operation");
      MDC.remove("resourceType");
    }
  }
}
