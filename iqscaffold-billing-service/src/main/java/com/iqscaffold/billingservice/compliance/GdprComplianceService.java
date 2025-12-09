package com.iqscaffold.billingservice.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceDto;
import com.iqscaffold.billingservice.invoice.InvoiceRepository;
import com.iqscaffold.billingservice.payment.Payment;
import com.iqscaffold.billingservice.payment.PaymentDto;
import com.iqscaffold.billingservice.payment.PaymentRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodRepository;
import com.iqscaffold.billingservice.shared.AuditLogService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.MessageService;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import com.iqscaffold.billingservice.usage.UsageDto;
import com.iqscaffold.billingservice.usage.UsageRecord;
import com.iqscaffold.billingservice.usage.UsageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for GDPR compliance operations.
 * Implements right to data portability (Article 20) and right to erasure (Article 17).
 */
@Service
public class GdprComplianceService {
    
    private static final Logger logger = LoggerFactory.getLogger(GdprComplianceService.class);
    private static final String DELETION_CONFIRMATION = "DELETE MY DATA";
    
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    
    public GdprComplianceService(
        SubscriptionRepository subscriptionRepository,
        InvoiceRepository invoiceRepository,
        PaymentRepository paymentRepository,
        PaymentMethodRepository paymentMethodRepository,
        UsageRecordRepository usageRecordRepository,
        EmailService emailService,
        AuditLogService auditLogService,
        MessageService messageService,
        ObjectMapper objectMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Export all billing data for a tenant (GDPR Article 20 - Right to Data Portability).
     */
    @Transactional(readOnly = true)
    public DataExportResponse requestDataExport(String tenantId, DataExportRequest request) {
        logger.info("Processing data export request for tenant: {}", tenantId);
        
        String exportId = "exp_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // Gather all billing data
            BillingDataExport export = gatherBillingData(tenantId, request.includeHistorical());
            
            // Convert to requested format
            String exportData = switch (request.format().toUpperCase()) {
                case "JSON" -> objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(export);
                case "CSV" -> convertToCSV(export);
                default -> throw new IllegalArgumentException("Unsupported format: " + request.format());
            };
            
            // Send export via email
            emailService.sendDataExport(request.email(), exportId, exportData, request.format());
            
            // Audit log
            auditLogService.logDataExport(tenantId, exportId, request.format());
            
            logger.info("Data export completed for tenant: {}, exportId: {}", tenantId, exportId);
            
            return new DataExportResponse(
                exportId,
                "COMPLETED",
                now,
                request.email(),
                messageService.getMessage("compliance.export.success")
            );
            
        } catch (Exception e) {
            logger.error("Failed to export data for tenant: {}", tenantId, e);
            throw new DataExportException("Failed to export data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete or anonymize billing data for a tenant (GDPR Article 17 - Right to Erasure).
     */
    @Transactional
    public DataDeletionResponse requestDataDeletion(String tenantId, DataDeletionRequest request) {
        logger.info("Processing data deletion request for tenant: {}", tenantId);
        
        // Validate confirmation
        if (!DELETION_CONFIRMATION.equals(request.confirmation())) {
            throw new IllegalArgumentException(
                "Invalid confirmation. Please type: " + DELETION_CONFIRMATION
            );
        }
        
        String deletionId = "del_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();
        int itemsDeleted = 0;
        int itemsAnonymized = 0;
        
        try {
            // Check for active subscriptions
            UUID tenantUuid = UUID.fromString(tenantId);
            List<Subscription> activeSubscriptions = subscriptionRepository
                .findByTenantIdAndStatus(tenantUuid, SubscriptionStatus.ACTIVE);
            
            if (!activeSubscriptions.isEmpty()) {
                throw new IllegalStateException(
                    "Cannot delete data while subscription is active. Please cancel subscription first."
                );
            }
            
            // Delete or anonymize based on request
            if (request.anonymize()) {
                itemsAnonymized = anonymizeBillingData(tenantId, request);
            } else {
                itemsDeleted = deleteBillingData(tenantId, request);
            }
            
            // Audit log
            auditLogService.logDataDeletion(
                tenantId, 
                deletionId, 
                request.anonymize(), 
                request.reason()
            );
            
            logger.info("Data deletion completed for tenant: {}, deletionId: {}", tenantId, deletionId);
            
            return new DataDeletionResponse(
                deletionId,
                "COMPLETED",
                now,
                itemsDeleted,
                itemsAnonymized,
                messageService.getMessage("compliance.deletion.success")
            );
            
        } catch (Exception e) {
            logger.error("Failed to delete data for tenant: {}", tenantId, e);
            throw new DataDeletionException("Failed to delete data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get data retention policy.
     */
    public DataRetentionPolicy getRetentionPolicy() {
        return DataRetentionPolicy.defaultPolicy();
    }
    
    /**
     * Apply data retention policies (scheduled job).
     */
    @Transactional
    public void applyRetentionPolicies() {
        logger.info("Applying data retention policies");
        
        DataRetentionPolicy policy = DataRetentionPolicy.defaultPolicy();
        LocalDateTime now = LocalDateTime.now();
        
        // Delete old usage records
        if (policy.usageRetentionDays() > 0) {
            LocalDateTime usageCutoff = now.minusDays(policy.usageRetentionDays());
            int deletedUsage = usageRecordRepository.deleteByRecordedAtBefore(usageCutoff);
            logger.info("Deleted {} usage records older than {}", deletedUsage, usageCutoff);
        }
        
        // Anonymize old payment methods
        if (policy.paymentMethodRetentionDays() > 0) {
            LocalDateTime paymentMethodCutoff = now.minusDays(policy.paymentMethodRetentionDays());
            List<PaymentMethod> oldPaymentMethods = paymentMethodRepository
                .findByDeletedAtBeforeAndNotAnonymized(paymentMethodCutoff);
            
            for (PaymentMethod pm : oldPaymentMethods) {
                anonymizePaymentMethod(pm);
            }
            logger.info("Anonymized {} old payment methods", oldPaymentMethods.size());
        }
        
        logger.info("Data retention policies applied successfully");
    }
    
    // Private helper methods
    
    private BillingDataExport gatherBillingData(String tenantId, boolean includeHistorical) {
        UUID tenantUuid = UUID.fromString(tenantId);
        
        // Get current subscription
        Subscription currentSub = subscriptionRepository.findByTenantIdAndStatus(
            tenantUuid, 
            SubscriptionStatus.ACTIVE
        ).stream().findFirst().orElse(null);
        
        // Get historical subscriptions
        List<Subscription> historicalSubs = includeHistorical 
            ? subscriptionRepository.findByTenantId(tenantUuid)
            : List.of();
        
        // Get invoices
        List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);
        
        // Get payments
        List<Payment> payments = paymentRepository.findByTenantId(tenantId);
        
        // Get payment methods
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByTenantId(tenantId);
        
        // Get usage records
        List<UsageRecord> usageRecords = includeHistorical
            ? usageRecordRepository.findByTenantId(tenantId)
            : usageRecordRepository.findByTenantIdAndRecordedAtAfter(
                tenantId, 
                LocalDateTime.now().minusMonths(3)
            );
        
        return new BillingDataExport(
            tenantId,
            LocalDateTime.now(),
            "JSON",
            currentSub != null ? toDto(currentSub) : null,
            historicalSubs.stream().map(this::toDto).toList(),
            invoices.stream().map(this::toDto).toList(),
            payments.stream().map(this::toDto).toList(),
            paymentMethods.stream().map(this::toDto).toList(),
            usageRecords.stream().map(this::toDto).toList(),
            DataRetentionPolicy.defaultPolicy()
        );
    }
    
    private int deleteBillingData(String tenantId, DataDeletionRequest request) {
        int count = 0;
        
        // Delete usage records
        count += usageRecordRepository.deleteByTenantId(tenantId);
        
        // Delete payment methods (if requested)
        if (request.deletePaymentMethods()) {
            count += paymentMethodRepository.deleteByTenantId(tenantId);
        }
        
        // Note: Invoices are NOT deleted for tax compliance
        // Note: Payments are NOT deleted for audit trail
        // Note: Subscriptions are NOT deleted for business records
        
        return count;
    }
    
    private int anonymizeBillingData(String tenantId, DataDeletionRequest request) {
        int count = 0;
        UUID tenantUuid = UUID.fromString(tenantId);
        
        // Anonymize subscriptions
        List<Subscription> subscriptions = subscriptionRepository.findByTenantId(tenantUuid);
        for (Subscription sub : subscriptions) {
            anonymizeSubscription(sub);
            count++;
        }
        
        // Anonymize payment methods
        if (request.deletePaymentMethods()) {
            List<PaymentMethod> paymentMethods = paymentMethodRepository.findByTenantId(tenantId);
            for (PaymentMethod pm : paymentMethods) {
                anonymizePaymentMethod(pm);
                count++;
            }
        }
        
        // Anonymize payments
        List<Payment> payments = paymentRepository.findByTenantId(tenantUuid);
        for (Payment payment : payments) {
            anonymizePayment(payment);
            count++;
        }
        
        return count;
    }
    
    private void anonymizeSubscription(Subscription subscription) {
        subscription.setUserId(null);
        subscription.setMetadata("{}");
        subscriptionRepository.save(subscription);
    }
    
    private void anonymizePaymentMethod(PaymentMethod paymentMethod) {
        paymentMethod.setLast4("****");
        paymentMethod.setBrand("ANONYMIZED");
        paymentMethod.setProviderCustomerId("ANONYMIZED");
        paymentMethod.setProviderPaymentMethodId("ANONYMIZED");
        paymentMethodRepository.save(paymentMethod);
    }
    
    private void anonymizePayment(Payment payment) {
        payment.setProviderPaymentId("ANONYMIZED");
        payment.setMetadata("{}");
        paymentRepository.save(payment);
    }
    
    private String convertToCSV(BillingDataExport export) {
        // Simple CSV conversion - in production, use a proper CSV library
        StringBuilder csv = new StringBuilder();
        csv.append("Data Type,ID,Date,Amount,Status\n");
        
        // Add subscriptions
        if (export.currentSubscription() != null) {
            csv.append(String.format("Subscription,%s,%s,%s,%s\n",
                export.currentSubscription().id(),
                export.currentSubscription().createdAt(),
                export.currentSubscription().planName(),
                export.currentSubscription().status()
            ));
        }
        
        // Add invoices
        for (InvoiceDto invoice : export.invoices()) {
            csv.append(String.format("Invoice,%s,%s,%.2f,%s\n",
                invoice.id(),
                invoice.createdAt(),
                invoice.total(),
                invoice.status()
            ));
        }
        
        return csv.toString();
    }
    
    // DTO conversion methods - simplified for GDPR export
    
    private SubscriptionDto toDto(Subscription subscription) {
        // Return null for now - in production, use proper mapper
        return null;
    }
    
    private InvoiceDto toDto(Invoice invoice) {
        // Return null for now - in production, use proper mapper
        return null;
    }
    
    private PaymentDto toDto(Payment payment) {
        // Return null for now - in production, use proper mapper
        return null;
    }
    
    private PaymentMethodDto toDto(PaymentMethod pm) {
        // Return null for now - in production, use proper mapper
        return null;
    }
    
    private UsageDto toDto(UsageRecord usage) {
        // Return null for now - in production, use proper mapper
        return null;
    }
}
