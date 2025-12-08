package com.iqscaffold.billingservice.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iqscaffold.billingservice.tenancy.TenantContext;
import com.iqscaffold.billingservice.usage.BillingEvent;
import com.iqscaffold.billingservice.usage.BillingEventRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AuditLogService.
 * 
 * <p>Tests audit logging functionality for billing operations.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock
  private BillingEventRepository billingEventRepository;

  @InjectMocks
  private AuditLogService auditLogService;

  private static final String TEST_TENANT_ID = "test-tenant-123";
  private static final Long TEST_SUBSCRIPTION_ID = 1L;
  private static final String TEST_EVENT_TYPE = "SUBSCRIPTION_CREATED";
  private static final String TEST_DESCRIPTION = "Subscription created for PRO plan";

  @BeforeEach
  void setUp() {
    TenantContext.setCurrentTenantId(TEST_TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void testLogSubscriptionOperation_CreatesAuditRecord() {
    // Arrange
    Map<String, Object> changes = new HashMap<>();
    changes.put("planId", 1L);
    changes.put("status", "ACTIVE");

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logSubscriptionOperation(
        TEST_SUBSCRIPTION_ID,
        TEST_EVENT_TYPE,
        TEST_DESCRIPTION,
        changes
    );

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.SUBSCRIPTION);
    assertThat(savedEvent.getEntityId()).isEqualTo(TEST_SUBSCRIPTION_ID);
    assertThat(savedEvent.getEventType()).isEqualTo(TEST_EVENT_TYPE);
    assertThat(savedEvent.getDescription()).isEqualTo(TEST_DESCRIPTION);
    assertThat(savedEvent.getChanges()).containsEntry("planId", 1L);
    assertThat(savedEvent.getChanges()).containsEntry("status", "ACTIVE");
    assertThat(savedEvent.getCreatedAt()).isNotNull();
  }

  @Test
  void testLogInvoiceOperation_CreatesAuditRecord() {
    // Arrange
    Long invoiceId = 100L;
    String eventType = "INVOICE_GENERATED";
    String description = "Invoice generated for subscription renewal";
    Map<String, Object> changes = new HashMap<>();
    changes.put("amount", 99.99);
    changes.put("currency", "USD");

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logInvoiceOperation(invoiceId, eventType, description, changes);

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.INVOICE);
    assertThat(savedEvent.getEntityId()).isEqualTo(invoiceId);
    assertThat(savedEvent.getEventType()).isEqualTo(eventType);
  }

  @Test
  void testLogPaymentOperation_CreatesAuditRecord() {
    // Arrange
    Long paymentId = 200L;
    String eventType = "PAYMENT_SUCCEEDED";
    String description = "Payment processed successfully";
    Map<String, Object> changes = new HashMap<>();
    changes.put("amount", 99.99);
    changes.put("provider", "stripe");

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logPaymentOperation(paymentId, eventType, description, changes);

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.PAYMENT);
    assertThat(savedEvent.getEntityId()).isEqualTo(paymentId);
    assertThat(savedEvent.getEventType()).isEqualTo(eventType);
  }

  @Test
  void testLogPaymentMethodOperation_CreatesAuditRecord() {
    // Arrange
    Long paymentMethodId = 300L;
    String eventType = "PAYMENT_METHOD_ADDED";
    String description = "Credit card added";
    Map<String, Object> changes = new HashMap<>();
    changes.put("type", "CARD");
    changes.put("last4", "4242");

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logPaymentMethodOperation(paymentMethodId, eventType, description, changes);

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.PAYMENT_METHOD);
    assertThat(savedEvent.getEntityId()).isEqualTo(paymentMethodId);
    assertThat(savedEvent.getEventType()).isEqualTo(eventType);
  }

  @Test
  void testLogUsageOperation_CreatesAuditRecord() {
    // Arrange
    Long usageRecordId = 400L;
    String eventType = "USAGE_RECORDED";
    String description = "API usage recorded";
    Map<String, Object> changes = new HashMap<>();
    changes.put("metricType", "API_CALLS");
    changes.put("quantity", 100);

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logUsageOperation(usageRecordId, eventType, description, changes);

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.USAGE_RECORD);
    assertThat(savedEvent.getEntityId()).isEqualTo(usageRecordId);
    assertThat(savedEvent.getEventType()).isEqualTo(eventType);
  }

  @Test
  void testLogPlanOperation_CreatesAuditRecord() {
    // Arrange
    Long planId = 500L;
    String eventType = "PLAN_UPDATED";
    String description = "Plan pricing updated";
    Map<String, Object> changes = new HashMap<>();
    changes.put("oldPrice", 49.99);
    changes.put("newPrice", 59.99);

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logPlanOperation(planId, eventType, description, changes);

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEntityType()).isEqualTo(BillingConstants.EntityTypes.PLAN);
    assertThat(savedEvent.getEntityId()).isEqualTo(planId);
    assertThat(savedEvent.getEventType()).isEqualTo(eventType);
  }

  @Test
  void testLogOperation_WithNullChanges_CreatesAuditRecord() {
    // Arrange
    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logSubscriptionOperation(
        TEST_SUBSCRIPTION_ID,
        TEST_EVENT_TYPE,
        TEST_DESCRIPTION,
        null
    );

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getChanges()).isNull();
  }

  @Test
  void testLogOperation_WithoutTenantContext_UsesDefaultTenant() {
    // Arrange
    TenantContext.clear(); // Clear tenant context
    Map<String, Object> changes = new HashMap<>();
    changes.put("test", "value");

    when(billingEventRepository.save(any(BillingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    auditLogService.logSubscriptionOperation(
        TEST_SUBSCRIPTION_ID,
        TEST_EVENT_TYPE,
        TEST_DESCRIPTION,
        changes
    );

    // Assert
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(billingEventRepository).save(eventCaptor.capture());

    BillingEvent savedEvent = eventCaptor.getValue();
    // TenantContext returns "default" when no tenant is set
    assertThat(savedEvent.getTenantId()).isIn("system", "default");
  }
}
