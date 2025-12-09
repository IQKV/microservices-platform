package com.iqscaffold.billingservice.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.billing.ProrationResult;
import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for InvoiceApplicationService.
 * 
 * <p>Tests the orchestration logic of the invoice application service, verifying:
 * <ul>
 *   <li>Delegation to InvoiceGenerator and InvoiceFactory</li>
 *   <li>Transaction management boundaries</li>
 *   <li>DTO translation</li>
 *   <li>Domain event publishing</li>
 *   <li>Error handling and exception translation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceApplicationService Unit Tests")
class InvoiceApplicationServiceTest {

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private InvoiceFactory invoiceFactory;

  @Mock
  private InvoiceGenerator invoiceGenerator;

  @Mock
  private DomainEventPublisher eventPublisher;

  @Mock
  private MessageService messageService;

  @Mock
  private BillingProperties billingProperties;

  @InjectMocks
  private InvoiceApplicationService invoiceApplicationService;

  private UUID testTenantId;
  private UUID testUserId;
  private SubscriptionPlan testPlan;
  private Subscription testSubscription;
  private Invoice testInvoice;
  private LocalDateTime periodStart;
  private LocalDateTime periodEnd;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    PlanQuotas testQuotas = new PlanQuotas(
        100L, 50L, 10000L, 5000L, 100L, 1000L, 5L, 10L
    );

    testPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Plan",
        "Professional features",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("49.99"),
        "USD",
        Map.of("advanced_workflows", true),
        testQuotas,
        14,
        true
    );
    TestEntityUtils.setId(testPlan, 1L);

    testSubscription = Subscription.createActive(testTenantId, testUserId, testPlan);
    TestEntityUtils.setId(testSubscription, 1L);

    periodStart = LocalDateTime.now();
    periodEnd = periodStart.plusMonths(1);

    testInvoice = Invoice.createDraft(
        testSubscription,
        testTenantId,
        "INV-202412-00001",
        "USD",
        periodStart,
        periodEnd,
        30
    );
    TestEntityUtils.setId(testInvoice, 1L);
  }

  @Nested
  @DisplayName("generateInvoice Tests")
  class GenerateInvoiceTests {

    @Test
    @DisplayName("Should generate invoice successfully")
    void shouldGenerateInvoiceSuccessfully() {
      // Arrange
      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(invoiceGenerator.generate(testSubscription, periodStart, periodEnd))
          .thenReturn(testInvoice);
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.generateInvoice(1L, periodStart, periodEnd);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.invoiceNumber()).isEqualTo("INV-202412-00001");
      verify(subscriptionRepository).findById(1L);
      verify(invoiceGenerator).generate(testSubscription, periodStart, periodEnd);
      verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.subscription.not.found"), any(Object[].class)))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> 
          invoiceApplicationService.generateInvoice(999L, periodStart, periodEnd))
          .isInstanceOf(InvoiceException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(invoiceGenerator, never()).generate(any(), any(), any());
      verify(invoiceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("generateProrationInvoice Tests")
  class GenerateProrationInvoiceTests {

    @Test
    @DisplayName("Should generate proration invoice successfully")
    void shouldGenerateProrationInvoiceSuccessfully() {
      // Arrange
      ProrationResult prorationResult = ProrationResult.forUpgrade(
          new BigDecimal("49.99"),
          new BigDecimal("99.99"),
          15,
          30,
          "Pro Plan",
          "Enterprise Plan"
      );

      LocalDateTime effectiveDate = LocalDateTime.now();

      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(invoiceFactory.createProrationInvoice(testSubscription, prorationResult, effectiveDate))
          .thenReturn(testInvoice);
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.generateProrationInvoice(
          1L, prorationResult, effectiveDate);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(invoiceFactory).createProrationInvoice(testSubscription, prorationResult, effectiveDate);
      verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      ProrationResult prorationResult = ProrationResult.forUpgrade(
          new BigDecimal("49.99"),
          new BigDecimal("99.99"),
          15,
          30,
          "Pro Plan",
          "Enterprise Plan"
      );

      LocalDateTime effectiveDate = LocalDateTime.now();

      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.subscription.not.found"), any(Object[].class)))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> 
          invoiceApplicationService.generateProrationInvoice(999L, prorationResult, effectiveDate))
          .isInstanceOf(InvoiceException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(invoiceFactory, never()).createProrationInvoice(any(), any(), any());
      verify(invoiceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("finalizeInvoice Tests")
  class FinalizeInvoiceTests {

    @Test
    @DisplayName("Should finalize invoice successfully")
    void shouldFinalizeInvoiceSuccessfully() {
      // Arrange
      testInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Pro Plan - Monthly",
          new BigDecimal("49.99")
      ));

      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.finalizeInvoice(1L);

      // Assert
      assertThat(result).isNotNull();
      verify(invoiceRepository).findById(1L);
      verify(invoiceRepository).save(testInvoice);
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
      // Arrange
      when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.not.found"), any(Object[].class)))
          .thenReturn("Invoice not found");

      // Act & Assert
      assertThatThrownBy(() -> invoiceApplicationService.finalizeInvoice(999L))
          .isInstanceOf(InvoiceException.InvoiceNotFoundException.class);

      verify(invoiceRepository).findById(999L);
      verify(invoiceRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("voidInvoice Tests")
  class VoidInvoiceTests {

    @Test
    @DisplayName("Should void invoice successfully")
    void shouldVoidInvoiceSuccessfully() {
      // Arrange
      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.voidInvoice(1L, "Customer requested");

      // Assert
      assertThat(result).isNotNull();
      verify(invoiceRepository).findById(1L);
      verify(invoiceRepository).save(testInvoice);
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
      // Arrange
      when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.not.found"), any(Object[].class)))
          .thenReturn("Invoice not found");

      // Act & Assert
      assertThatThrownBy(() -> invoiceApplicationService.voidInvoice(999L, "reason"))
          .isInstanceOf(InvoiceException.InvoiceNotFoundException.class);

      verify(invoiceRepository).findById(999L);
      verify(invoiceRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("markInvoiceAsPaid Tests")
  class MarkInvoiceAsPaidTests {

    @Test
    @DisplayName("Should mark invoice as paid successfully")
    void shouldMarkInvoiceAsPaidSuccessfully() {
      // Arrange
      testInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
          "Pro Plan - Monthly",
          new BigDecimal("49.99")
      ));
      testInvoice.finalize();

      LocalDateTime paidAt = LocalDateTime.now();

      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.markInvoiceAsPaid(1L, 1L, paidAt);

      // Assert
      assertThat(result).isNotNull();
      verify(invoiceRepository).findById(1L);
      verify(invoiceRepository).save(testInvoice);
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
      // Arrange
      LocalDateTime paidAt = LocalDateTime.now();

      when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.not.found"), any(Object[].class)))
          .thenReturn("Invoice not found");

      // Act & Assert
      assertThatThrownBy(() -> 
          invoiceApplicationService.markInvoiceAsPaid(999L, 1L, paidAt))
          .isInstanceOf(InvoiceException.InvoiceNotFoundException.class);

      verify(invoiceRepository).findById(999L);
      verify(invoiceRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("getInvoice Tests")
  class GetInvoiceTests {

    @Test
    @DisplayName("Should retrieve invoice by ID successfully")
    void shouldRetrieveInvoiceById() {
      // Arrange
      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

      // Act
      InvoiceDto result = invoiceApplicationService.getInvoice(1L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(1L);
      verify(invoiceRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
      // Arrange
      when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("invoice.not.found"), any(Object[].class)))
          .thenReturn("Invoice not found");

      // Act & Assert
      assertThatThrownBy(() -> invoiceApplicationService.getInvoice(999L))
          .isInstanceOf(InvoiceException.InvoiceNotFoundException.class);

      verify(invoiceRepository).findById(999L);
    }
  }

  @Nested
  @DisplayName("getInvoicesBySubscription Tests")
  class GetInvoicesBySubscriptionTests {

    @Test
    @DisplayName("Should retrieve invoices by subscription successfully")
    void shouldRetrieveInvoicesBySubscription() {
      // Arrange
      List<Invoice> invoices = List.of(testInvoice);
      when(invoiceRepository.findBySubscriptionId(1L)).thenReturn(invoices);

      // Act
      List<InvoiceDto> result = invoiceApplicationService.getInvoicesBySubscription(1L);

      // Assert
      assertThat(result).hasSize(1);
      verify(invoiceRepository).findBySubscriptionId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no invoices exist")
    void shouldReturnEmptyListWhenNoInvoices() {
      // Arrange
      when(invoiceRepository.findBySubscriptionId(1L)).thenReturn(List.of());

      // Act
      List<InvoiceDto> result = invoiceApplicationService.getInvoicesBySubscription(1L);

      // Assert
      assertThat(result).isEmpty();
      verify(invoiceRepository).findBySubscriptionId(1L);
    }
  }

  @Nested
  @DisplayName("getInvoicesByTenant Tests")
  class GetInvoicesByTenantTests {

    @Test
    @DisplayName("Should retrieve invoices by tenant successfully")
    void shouldRetrieveInvoicesByTenant() {
      // Arrange
      List<Invoice> invoices = List.of(testInvoice);
      when(invoiceRepository.findByTenantId(testTenantId.toString())).thenReturn(invoices);

      // Act
      List<InvoiceDto> result = invoiceApplicationService.getInvoicesByTenant(testTenantId);

      // Assert
      assertThat(result).hasSize(1);
      verify(invoiceRepository).findByTenantId(testTenantId.toString());
    }

    @Test
    @DisplayName("Should return empty list when no invoices exist")
    void shouldReturnEmptyListWhenNoInvoices() {
      // Arrange
      when(invoiceRepository.findByTenantId(testTenantId.toString())).thenReturn(List.of());

      // Act
      List<InvoiceDto> result = invoiceApplicationService.getInvoicesByTenant(testTenantId);

      // Assert
      assertThat(result).isEmpty();
      verify(invoiceRepository).findByTenantId(testTenantId.toString());
    }
  }

  @Nested
  @DisplayName("generateInvoiceNumber Tests")
  class GenerateInvoiceNumberTests {

    @Test
    @DisplayName("Should generate invoice number successfully")
    void shouldGenerateInvoiceNumberSuccessfully() {
      // Arrange
      String expectedNumber = "INV-202412-00001";
      when(invoiceGenerator.generateInvoiceNumber(any(LocalDateTime.class)))
          .thenReturn(expectedNumber);

      // Act
      String result = invoiceApplicationService.generateInvoiceNumber();

      // Assert
      assertThat(result).isEqualTo(expectedNumber);
      verify(invoiceGenerator).generateInvoiceNumber(any(LocalDateTime.class));
    }
  }

  @Nested
  @DisplayName("generateInvoiceForSubscription Tests")
  class GenerateInvoiceForSubscriptionTests {

    @Test
    @DisplayName("Should generate invoice for subscription successfully")
    void shouldGenerateInvoiceForSubscriptionSuccessfully() {
      // Arrange
      when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
      when(invoiceGenerator.generate(
          eq(testSubscription),
          any(LocalDateTime.class),
          any(LocalDateTime.class)))
          .thenReturn(testInvoice);
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

      // Act
      InvoiceDto result = invoiceApplicationService.generateInvoiceForSubscription(1L);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(1L);
      verify(invoiceGenerator).generate(
          eq(testSubscription),
          any(LocalDateTime.class),
          any(LocalDateTime.class));
      verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
      // Arrange
      when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage("subscription.not.found"))
          .thenReturn("Subscription not found");

      // Act & Assert
      assertThatThrownBy(() -> 
          invoiceApplicationService.generateInvoiceForSubscription(999L))
          .isInstanceOf(com.iqscaffold.billingservice.shared.exception.SubscriptionException.SubscriptionNotFoundException.class);

      verify(subscriptionRepository).findById(999L);
      verify(invoiceGenerator, never()).generate(any(), any(), any());
      verify(invoiceRepository, never()).save(any());
    }
  }
}
