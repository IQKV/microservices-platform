package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.iqscaffold.billingservice.config.LoggingAspect.LogBusinessEvent;
import com.iqscaffold.billingservice.config.LoggingAspect.LogPerformance;
import com.iqscaffold.billingservice.config.LoggingConfiguration.StructuredLogger;
import com.iqscaffold.billingservice.infrastructure.messaging.MessagingService;
import com.iqscaffold.billingservice.shared.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for handling payment-related notifications.
 * Integrates with the payment processing flow to send appropriate notifications.
 */
@Service
public class PaymentNotificationService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationService.class);

  private final NotificationService notificationService;
  private final MessagingService messagingService;

  public PaymentNotificationService(final NotificationService notificationService,
                                  final MessagingService messagingService) {
    this.notificationService = notificationService;
    this.messagingService = messagingService;
  }

  /**
   * Handle successful payment notification
   */
  @LogPerformance(operation = "payment-successful-notification")
  @LogBusinessEvent(eventType = "payment_notification", description = "Processing successful payment notification")
  public void handlePaymentSuccessful(PaymentSuccessfulEvent event) {
    try {
      // Log structured payment event
      StructuredLogger.logPaymentEvent(
          "payment_successful", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of(
              "amount", event.getAmount(),
              "currency", event.getCurrency(),
              "customerEmail", event.getCustomerEmail(),
              "paymentMethod", event.getPaymentMethod()
          )
      );

      // Send email notification
      notificationService.sendPaymentSuccessfulNotification(
          event.getCustomerEmail(),
          event.getCustomerName(),
          event.getPaymentId(),
          event.getAmount(),
          event.getCurrency(),
          event.getDescription(),
          event.getPaymentDate(),
          event.getPaymentMethod(),
          event.getReceiptUrl(),
          event.getTenantId()
      );

      // Publish billing event for other services
      messagingService.publishPaymentSuccessful(
          event.getPaymentId(),
          event.getTenantId(),
          event.getCustomerEmail()
      );

      logger.info("Payment successful notifications sent for payment: {}", event.getPaymentId());
    } catch (Exception e) {
      // Log structured error event
      StructuredLogger.logPaymentEvent(
          "payment_notification_failed", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of("error", e.getMessage(), "eventType", "payment_successful")
      );
      
      logger.error("Failed to handle payment successful notifications for payment: {}", 
          event.getPaymentId(), e);
      // Don't rethrow - notification failures shouldn't break payment processing
    }
  }

  /**
   * Handle failed payment notification
   */
  @LogPerformance(operation = "payment-failed-notification")
  @LogBusinessEvent(eventType = "payment_notification", description = "Processing failed payment notification")
  public void handlePaymentFailed(PaymentFailedEvent event) {
    try {
      // Log structured payment event
      StructuredLogger.logPaymentEvent(
          "payment_failed", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of(
              "amount", event.getAmount(),
              "currency", event.getCurrency(),
              "customerEmail", event.getCustomerEmail(),
              "errorMessage", event.getErrorMessage()
          )
      );

      // Send email notification
      notificationService.sendPaymentFailedNotification(
          event.getCustomerEmail(),
          event.getCustomerName(),
          event.getPaymentId(),
          event.getAmount(),
          event.getCurrency(),
          event.getDescription(),
          event.getAttemptDate(),
          event.getErrorMessage(),
          event.getRetryUrl(),
          event.getTenantId()
      );

      // Publish billing event for other services
      messagingService.publishPaymentFailed(
          event.getPaymentId(),
          event.getTenantId(),
          event.getCustomerEmail()
      );

      logger.info("Payment failed notifications sent for payment: {}", event.getPaymentId());
    } catch (Exception e) {
      // Log structured error event
      StructuredLogger.logPaymentEvent(
          "payment_notification_failed", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of("error", e.getMessage(), "eventType", "payment_failed")
      );
      
      logger.error("Failed to handle payment failed notifications for payment: {}", 
          event.getPaymentId(), e);
      // Don't rethrow - notification failures shouldn't break payment processing
    }
  }

  /**
   * Handle payment refunded notification
   */
  @LogPerformance(operation = "payment-refunded-notification")
  @LogBusinessEvent(eventType = "payment_notification", description = "Processing refunded payment notification")
  public void handlePaymentRefunded(PaymentRefundedEvent event) {
    try {
      // Log structured payment event
      StructuredLogger.logPaymentEvent(
          "payment_refunded", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of(
              "amount", event.getAmount(),
              "currency", event.getCurrency(),
              "customerEmail", event.getCustomerEmail(),
              "refundId", event.getRefundId()
          )
      );

      // Send email notification
      notificationService.sendPaymentRefundedNotification(
          event.getCustomerEmail(),
          event.getCustomerName(),
          event.getPaymentId(),
          event.getAmount(),
          event.getCurrency(),
          event.getRefundId(),
          event.getRefundDate(),
          event.getTenantId()
      );

      // Publish billing event for other services
      messagingService.publishPaymentRefunded(
          event.getPaymentId(),
          event.getTenantId(),
          event.getCustomerEmail()
      );

      logger.info("Payment refunded notifications sent for payment: {}", event.getPaymentId());
    } catch (Exception e) {
      // Log structured error event
      StructuredLogger.logPaymentEvent(
          "payment_notification_failed", 
          event.getPaymentId(), 
          event.getTenantId(),
          Map.of("error", e.getMessage(), "eventType", "payment_refunded")
      );
      
      logger.error("Failed to handle payment refunded notifications for payment: {}", 
          event.getPaymentId(), e);
      // Don't rethrow - notification failures shouldn't break refund processing
    }
  }

  /**
   * Event class for payment successful notifications
   */
  public static class PaymentSuccessfulEvent {
    private String paymentId;
    private String customerEmail;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String receiptUrl;
    private String tenantId;

    // Constructors, getters, and setters
    public PaymentSuccessfulEvent() {}

    public PaymentSuccessfulEvent(String paymentId, String customerEmail, String customerName,
                                BigDecimal amount, String currency, String description,
                                LocalDateTime paymentDate, String paymentMethod, String receiptUrl,
                                String tenantId) {
      this.paymentId = paymentId;
      this.customerEmail = customerEmail;
      this.customerName = customerName;
      this.amount = amount;
      this.currency = currency;
      this.description = description;
      this.paymentDate = paymentDate;
      this.paymentMethod = paymentMethod;
      this.receiptUrl = receiptUrl;
      this.tenantId = tenantId;
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
  }

  /**
   * Event class for payment failed notifications
   */
  public static class PaymentFailedEvent {
    private String paymentId;
    private String customerEmail;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime attemptDate;
    private String errorMessage;
    private String retryUrl;
    private String tenantId;

    // Constructors, getters, and setters
    public PaymentFailedEvent() {}

    public PaymentFailedEvent(String paymentId, String customerEmail, String customerName,
                            BigDecimal amount, String currency, String description,
                            LocalDateTime attemptDate, String errorMessage, String retryUrl,
                            String tenantId) {
      this.paymentId = paymentId;
      this.customerEmail = customerEmail;
      this.customerName = customerName;
      this.amount = amount;
      this.currency = currency;
      this.description = description;
      this.attemptDate = attemptDate;
      this.errorMessage = errorMessage;
      this.retryUrl = retryUrl;
      this.tenantId = tenantId;
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getAttemptDate() { return attemptDate; }
    public void setAttemptDate(LocalDateTime attemptDate) { this.attemptDate = attemptDate; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRetryUrl() { return retryUrl; }
    public void setRetryUrl(String retryUrl) { this.retryUrl = retryUrl; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
  }

  /**
   * Event class for payment refunded notifications
   */
  public static class PaymentRefundedEvent {
    private String paymentId;
    private String customerEmail;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String refundId;
    private LocalDateTime refundDate;
    private String tenantId;

    // Constructors, getters, and setters
    public PaymentRefundedEvent() {}

    public PaymentRefundedEvent(String paymentId, String customerEmail, String customerName,
                              BigDecimal amount, String currency, String refundId,
                              LocalDateTime refundDate, String tenantId) {
      this.paymentId = paymentId;
      this.customerEmail = customerEmail;
      this.customerName = customerName;
      this.amount = amount;
      this.currency = currency;
      this.refundId = refundId;
      this.refundDate = refundDate;
      this.tenantId = tenantId;
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public LocalDateTime getRefundDate() { return refundDate; }
    public void setRefundDate(LocalDateTime refundDate) { this.refundDate = refundDate; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
  }
}