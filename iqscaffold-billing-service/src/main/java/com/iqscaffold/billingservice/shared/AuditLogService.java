package com.iqscaffold.billingservice.shared;

import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import com.iqscaffold.billingservice.usage.BillingEvent;
import com.iqscaffold.billingservice.usage.BillingEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for audit logging of billing operations.
 * 
 * <p>This service creates immutable audit records for all significant billing
 * operations to satisfy compliance requirements (REQ-SEC-011, REQ-SEC-012, REQ-REL-007).
 * 
 * <p>Audit logs include:
 * <ul>
 *   <li>Tenant and user identification</li>
 *   <li>Operation type and entity affected</li>
 *   <li>Changes made (before/after values)</li>
 *   <li>Request metadata (IP address, user agent)</li>
 *   <li>Timestamp of operation</li>
 * </ul>
 * 
 * <p><strong>Immutability:</strong> All audit records are immutable once created.
 * The BillingEvent entity is marked with @Immutable and provides no update or delete methods.
 * 
 * <p><strong>Transaction Management:</strong> Audit logging uses REQUIRES_NEW propagation
 * to ensure audit records are persisted even if the main transaction rolls back.
 */
@Service
public class AuditLogService {

  private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

  private final BillingEventRepository billingEventRepository;

  public AuditLogService(final BillingEventRepository billingEventRepository) {
    this.billingEventRepository = billingEventRepository;
  }

  /**
   * Log a subscription operation.
   * 
   * @param subscriptionId the subscription identifier
   * @param eventType the event type (e.g., SUBSCRIPTION_CREATED, SUBSCRIPTION_UPGRADED)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logSubscriptionOperation(
      final Long subscriptionId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.SUBSCRIPTION, subscriptionId, eventType, description, changes);
  }

  /**
   * Log an invoice operation.
   * 
   * @param invoiceId the invoice identifier
   * @param eventType the event type (e.g., INVOICE_GENERATED, INVOICE_PAID)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logInvoiceOperation(
      final Long invoiceId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.INVOICE, invoiceId, eventType, description, changes);
  }

  /**
   * Log a payment operation.
   * 
   * @param paymentId the payment identifier
   * @param eventType the event type (e.g., PAYMENT_SUCCEEDED, PAYMENT_FAILED)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logPaymentOperation(
      final Long paymentId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.PAYMENT, paymentId, eventType, description, changes);
  }

  /**
   * Log a payment method operation.
   * 
   * @param paymentMethodId the payment method identifier
   * @param eventType the event type (e.g., PAYMENT_METHOD_ADDED, PAYMENT_METHOD_REMOVED)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logPaymentMethodOperation(
      final Long paymentMethodId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.PAYMENT_METHOD, paymentMethodId, eventType, description, changes);
  }

  /**
   * Log a usage operation.
   * 
   * @param usageRecordId the usage record identifier
   * @param eventType the event type (e.g., USAGE_RECORDED, QUOTA_EXCEEDED)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logUsageOperation(
      final Long usageRecordId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.USAGE_RECORD, usageRecordId, eventType, description, changes);
  }

  /**
   * Log a plan operation.
   * 
   * @param planId the plan identifier
   * @param eventType the event type (e.g., PLAN_CREATED, PLAN_UPDATED)
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logPlanOperation(
      final Long planId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    logOperation(BillingConstants.EntityTypes.PLAN, planId, eventType, description, changes);
  }

  /**
   * Log a generic billing operation.
   * 
   * @param entityType the entity type
   * @param entityId the entity identifier
   * @param eventType the event type
   * @param description human-readable description
   * @param changes map of changes made
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logOperation(
      final String entityType,
      final Long entityId,
      final String eventType,
      final String description,
      final Map<String, Object> changes) {
    try {
      // Get tenant context
      String tenantId = TenantContext.getCurrentTenantIdOrDefault();
      if (tenantId == null) {
        tenantId = "system"; // For system-level operations
      }

      // Get user context
      Long userId = SecurityContextHelper.getCurrentUserId();

      // Get request metadata
      String ipAddress = getCurrentIpAddress();
      String userAgent = getCurrentUserAgent();

      // Create immutable audit record
      BillingEvent event = new BillingEvent(
          tenantId,
          userId,
          eventType,
          entityType,
          entityId,
          description,
          changes != null ? new HashMap<>(changes) : null,
          ipAddress,
          userAgent
      );

      billingEventRepository.save(event);

      logger.info(
          "Audit log created: tenant={}, user={}, eventType={}, entityType={}, entityId={}",
          tenantId, userId, eventType, entityType, entityId
      );

    } catch (Exception e) {
      // Log error but don't fail the operation
      logger.error("Failed to create audit log: eventType={}, entityType={}, entityId={}",
          eventType, entityType, entityId, e);
    }
  }

  /**
   * Get the current request's IP address.
   * 
   * @return the IP address, or null if not available
   */
  private String getCurrentIpAddress() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();

        // Check for proxy headers first
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty()) {
          // X-Forwarded-For can contain multiple IPs, take the first one
          return ipAddress.split(",")[0].trim();
        }

        ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress != null && !ipAddress.isEmpty()) {
          return ipAddress;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
      }
    } catch (Exception e) {
      logger.debug("Failed to extract IP address", e);
    }

    return null;
  }

  /**
   * Get the current request's user agent.
   * 
   * @return the user agent, or null if not available
   */
  private String getCurrentUserAgent() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("User-Agent");
      }
    } catch (Exception e) {
      logger.debug("Failed to extract user agent", e);
    }

    return null;
  }
}
