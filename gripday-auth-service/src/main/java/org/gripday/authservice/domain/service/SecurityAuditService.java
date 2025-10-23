package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.UserAuditLog;
import org.gripday.authservice.infrastructure.repository.UserAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for security audit logging.
 * Logs authentication events, account lockouts, and security-related operations.
 */
@Service
@Transactional
public class SecurityAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    private final UserAuditLogRepository auditLogRepository;
    public SecurityAuditService(UserAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Log successful authentication attempt.
     */
    public void logSuccessfulAuthentication(String username, String ipAddress, String userAgent) {
        var details = String.format("""
            Authentication successful for user: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, username, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("AUTHENTICATION_SUCCESS", username, details, ipAddress, userAgent);
        
        securityLogger.info("AUTHENTICATION_SUCCESS: user={}, ip={}, userAgent={}", 
            username, ipAddress, userAgent);
    }
    
    /**
     * Log failed authentication attempt.
     */
    public void logFailedAuthentication(String username, String reason, String ipAddress, String userAgent) {
        var details = String.format("""
            Authentication failed for user: %s
            Reason: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, username, reason, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("AUTHENTICATION_FAILURE", username, details, ipAddress, userAgent);
        
        securityLogger.warn("AUTHENTICATION_FAILURE: user={}, reason={}, ip={}, userAgent={}", 
            username, reason, ipAddress, userAgent);
    }
    
    /**
     * Log account lockout event.
     */
    public void logAccountLockout(String username, int failedAttempts, String ipAddress, String userAgent) {
        var details = String.format("""
            Account locked due to excessive failed login attempts
            User: %s
            Failed Attempts: %d
            IP Address: %s
            User Agent: %s
            Lockout Duration: 15 minutes
            Timestamp: %s
            """, username, failedAttempts, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("ACCOUNT_LOCKOUT", username, details, ipAddress, userAgent);
        
        securityLogger.error("ACCOUNT_LOCKOUT: user={}, failedAttempts={}, ip={}, userAgent={}", 
            username, failedAttempts, ipAddress, userAgent);
    }
    
    /**
     * Log password change event.
     */
    public void logPasswordChange(String username, String ipAddress, String userAgent) {
        var details = String.format("""
            Password changed for user: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, username, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("PASSWORD_CHANGE", username, details, ipAddress, userAgent);
        
        securityLogger.info("PASSWORD_CHANGE: user={}, ip={}, userAgent={}", 
            username, ipAddress, userAgent);
    }
    
    /**
     * Log user registration event.
     */
    public void logUserRegistration(String username, String email, String ipAddress, String userAgent) {
        var details = String.format("""
            New user registration
            Username: %s
            Email: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, username, email, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("USER_REGISTRATION", username, details, ipAddress, userAgent);
        
        securityLogger.info("USER_REGISTRATION: user={}, email={}, ip={}, userAgent={}", 
            username, email, ipAddress, userAgent);
    }
    
    /**
     * Log rate limiting event.
     */
    public void logRateLimitExceeded(String ipAddress, String userAgent, String endpoint) {
        var details = String.format("""
            Rate limit exceeded
            IP Address: %s
            User Agent: %s
            Endpoint: %s
            Timestamp: %s
            """, ipAddress, userAgent, endpoint, Instant.now());
        
        logSecurityEvent("RATE_LIMIT_EXCEEDED", null, details, ipAddress, userAgent);
        
        securityLogger.warn("RATE_LIMIT_EXCEEDED: ip={}, userAgent={}, endpoint={}", 
            ipAddress, userAgent, endpoint);
    }
    
    /**
     * Log suspicious activity.
     */
    public void logSuspiciousActivity(String username, String activity, String ipAddress, String userAgent) {
        var details = String.format("""
            Suspicious activity detected
            User: %s
            Activity: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, username, activity, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("SUSPICIOUS_ACTIVITY", username, details, ipAddress, userAgent);
        
        securityLogger.error("SUSPICIOUS_ACTIVITY: user={}, activity={}, ip={}, userAgent={}", 
            username, activity, ipAddress, userAgent);
    }
    
    /**
     * Log JWT token events.
     */
    public void logTokenEvent(String username, String action, String ipAddress, String userAgent) {
        var details = String.format("""
            JWT Token %s
            User: %s
            IP Address: %s
            User Agent: %s
            Timestamp: %s
            """, action, username, ipAddress, userAgent, Instant.now());
        
        logSecurityEvent("TOKEN_" + action.toUpperCase(), username, details, ipAddress, userAgent);
        
        securityLogger.info("TOKEN_{}: user={}, ip={}, userAgent={}", 
            action.toUpperCase(), username, ipAddress, userAgent);
    }
    
    /**
     * Generic method to log security events to database and structured logs.
     */
    private void logSecurityEvent(String action, String username, String details, String ipAddress, String userAgent) {
        try {
            // Set correlation ID for tracing
            var correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            
            // Create audit log entry
            var auditLog = new UserAuditLog(action, "default");
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            
            // Set tenant context if available
            var currentTenant = TenantContext.getCurrentTenantIdOrDefault();
            auditLog.setTenantId(currentTenant);
            
            // Try to find user ID if username is provided
            if (username != null) {
                // Note: In a real implementation, you might want to look up the user ID
                // For now, we'll store the username in the details
                auditLog.setDetails(auditLog.getDetails() + "\nUsername: " + username);
            }
            
            // Save to database
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            // Don't let audit logging failures break the main flow
            logger.error("Failed to save security audit log: {}", e.getMessage(), e);
        }
    }
}