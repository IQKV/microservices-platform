package com.iqscaffold.userservice.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for structured logging utilities and helpers. Provides consistent logging patterns and correlation ID management.
 */
@Configuration
@EnableConfigurationProperties(IqScaffoldProperties.class)
public class StructuredLoggingConfig {

  /**
   * Structured logging utility for consistent log formatting.
   */
  @Bean
  public StructuredLogger structuredLogger() {
    return new StructuredLogger();
  }

  /**
   * Security event logger for audit trails.
   */
  @Bean
  public SecurityLogger securityLogger() {
    return new SecurityLogger();
  }

  /**
   * Structured logging utility implementation.
   */
  public static class StructuredLogger {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);

    public void logAuthenticationAttempt(String username, String result, String reason, String ipAddress, String userAgent) {
      try {
        MDC.put("event", "authentication_attempt");
        MDC.put("username", username);
        MDC.put("result", result);
        MDC.put("ipAddress", ipAddress);
        MDC.put("userAgent", userAgent);

        if ("success".equals(result)) {
          logger.info("Authentication successful for user: {}", username);
        } else {
          MDC.put("reason", reason);
          logger.warn("Authentication failed for user: {} - {}", username, reason);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("username");
        MDC.remove("result");
        MDC.remove("reason");
        MDC.remove("ipAddress");
        MDC.remove("userAgent");
      }
    }

    public void logUserRegistration(String username, String email, String result, String reason) {
      try {
        MDC.put("event", "user_registration");
        MDC.put("username", username);
        MDC.put("email", email);
        MDC.put("result", result);

        if ("success".equals(result)) {
          logger.info("User registration successful: {}", username);
        } else {
          MDC.put("reason", reason);
          logger.warn("User registration failed: {} - {}", username, reason);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("username");
        MDC.remove("email");
        MDC.remove("result");
        MDC.remove("reason");
      }
    }

    public void logTokenOperation(String operation, String username, String result, String tokenType) {
      try {
        MDC.put("event", "token_operation");
        MDC.put("operation", operation);
        MDC.put("username", username);
        MDC.put("result", result);
        MDC.put("tokenType", tokenType);

        if ("success".equals(result)) {
          logger.info("Token {} successful for user: {}", operation, username);
        } else {
          logger.warn("Token {} failed for user: {}", operation, username);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("operation");
        MDC.remove("username");
        MDC.remove("result");
        MDC.remove("tokenType");
      }
    }

    public void logDatabaseOperation(String operation, String table, long durationMs, String result) {
      try {
        MDC.put("event", "database_operation");
        MDC.put("operation", operation);
        MDC.put("table", table);
        MDC.put("durationMs", String.valueOf(durationMs));
        MDC.put("result", result);

        if (durationMs > 1000) {
          logger.warn("Slow database operation: {} on {} took {}ms", operation, table, durationMs);
        } else {
          logger.debug("Database operation: {} on {} completed in {}ms", operation, table, durationMs);
        }
      } finally {
        MDC.remove("event");
        MDC.remove("operation");
        MDC.remove("table");
        MDC.remove("durationMs");
        MDC.remove("result");
      }
    }

    public void logBusinessEvent(String eventType, Map<String, String> context) {
      try {
        MDC.put("event", eventType);
        context.forEach(MDC::put);

        logger.info("Business event: {} with context: {}", eventType, context);
      } finally {
        MDC.remove("event");
        context.keySet().forEach(MDC::remove);
      }
    }
  }

  /**
   * Security event logger for audit trails.
   */
  public static class SecurityLogger {

    private static final Logger securityLogger = LoggerFactory.getLogger("com.iqscaffold.userservice.security");

    public void logSecurityEvent(String eventType, String username, String action, String result,
                                 String ipAddress, String userAgent, Map<String, String> additionalContext) {
      try {
        MDC.put("securityEvent", eventType);
        MDC.put("username", username);
        MDC.put("action", action);
        MDC.put("result", result);
        MDC.put("ipAddress", ipAddress);
        MDC.put("userAgent", userAgent);

        if (additionalContext != null) {
          additionalContext.forEach(MDC::put);
        }

        if ("failure".equals(result) || "suspicious".equals(result)) {
          securityLogger.warn("Security event: {} - {} by {} from {} - {}",
              eventType, action, username, ipAddress, result);
        } else {
          securityLogger.info("Security event: {} - {} by {} from {}",
              eventType, action, username, ipAddress);
        }
      } finally {
        MDC.remove("securityEvent");
        MDC.remove("username");
        MDC.remove("action");
        MDC.remove("result");
        MDC.remove("ipAddress");
        MDC.remove("userAgent");

        if (additionalContext != null) {
          additionalContext.keySet().forEach(MDC::remove);
        }
      }
    }

    public void logAccountLockout(String username, String reason, String ipAddress, int attemptCount) {
      try {
        MDC.put("securityEvent", "account_lockout");
        MDC.put("username", username);
        MDC.put("reason", reason);
        MDC.put("ipAddress", ipAddress);
        MDC.put("attemptCount", String.valueOf(attemptCount));

        securityLogger.warn("Account locked: {} from {} after {} attempts - {}",
            username, ipAddress, attemptCount, reason);
      } finally {
        MDC.remove("securityEvent");
        MDC.remove("username");
        MDC.remove("reason");
        MDC.remove("ipAddress");
        MDC.remove("attemptCount");
      }
    }

    public void logPrivilegeEscalation(String username, String fromRole, String toRole, String authorizedBy) {
      try {
        MDC.put("securityEvent", "privilege_escalation");
        MDC.put("username", username);
        MDC.put("fromRole", fromRole);
        MDC.put("toRole", toRole);
        MDC.put("authorizedBy", authorizedBy);

        securityLogger.info("Privilege escalation: {} from {} to {} authorized by {}",
            username, fromRole, toRole, authorizedBy);
      } finally {
        MDC.remove("securityEvent");
        MDC.remove("username");
        MDC.remove("fromRole");
        MDC.remove("toRole");
        MDC.remove("authorizedBy");
      }
    }
  }
}
