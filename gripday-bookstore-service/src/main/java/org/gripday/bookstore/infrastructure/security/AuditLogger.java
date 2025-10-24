package org.gripday.bookstore.infrastructure.security;

import org.gripday.bookstore.domain.dto.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class AuditLogger {
    
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    public void logAdminOperation(String operation, String resourceType, String resourceId, 
                                UserContext userContext, Map<String, Object> details) {
        
        var auditEntry = Map.of(
            "timestamp", LocalDateTime.now(),
            "operation", operation,
            "resourceType", resourceType,
            "resourceId", resourceId,
            "userId", userContext.userId(),
            "username", userContext.username(),
            "userRoles", userContext.roles(),
            "department", userContext.department(),
            "organizationId", userContext.organizationId(),
            "details", details != null ? details : Map.of()
        );
        
        auditLog.info("Admin operation performed: {}", auditEntry);
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
        var auditEntry = Map.of(
            "timestamp", LocalDateTime.now(),
            "event", "UNAUTHORIZED_ACCESS_ATTEMPT",
            "operation", operation,
            "resourceType", resourceType,
            "userId", userContext.userId(),
            "username", userContext.username(),
            "userRoles", userContext.roles(),
            "department", userContext.department(),
            "organizationId", userContext.organizationId()
        );
        
        auditLog.warn("Unauthorized access attempt: {}", auditEntry);
    }
}