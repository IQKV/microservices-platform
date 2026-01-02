package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.BillingConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderAdapter paymentProvider;
    private final MerchantStripeConfigRepository merchantConfigRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentAuditTrailService auditService;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentProviderAdapter paymentProvider,
        MerchantStripeConfigRepository merchantConfigRepository,
        PaymentStateMachine stateMachine,
        PaymentAuditTrailService auditService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.merchantConfigRepository = merchantConfigRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentDtos.PaymentResponse createPaymentIntent(PaymentDtos.CreatePaymentRequest request) {
        String tenantId = SecurityContextHelper.getCurrentTenantId();

        // 1. Initial State Validation
        stateMachine.validateTransition(null, BillingConstants.PaymentStatus.PENDING);

        // 2. Resolve Merchant Account (if any)
        Optional<String> connectedAccountId = merchantConfigRepository.findByTenantId(tenantId)
            .map(config -> config.getStripeAccountId());

        // Platform fee logic (simplified for now: 10% if connected account exists)
        BigDecimal applicationFee = connectedAccountId.isPresent() 
            ? request.amount().multiply(BigDecimal.valueOf(0.10)) 
            : BigDecimal.ZERO;

        // 3. Create Record
        Payment payment = new Payment();
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setTenantId(tenantId);
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);
        payment.setApplicationFeeAmount(applicationFee);
        connectedAccountId.ifPresent(payment::setMerchantAccountId);
        
        payment = paymentRepository.save(payment); // Save to get UUID
        
        auditService.logPaymentAttempt(payment.getId(), BillingConstants.PaymentStatus.PENDING);

        // 4. Call Provider
        String intentId = paymentProvider.createPaymentIntent(
            request.amount(), 
            request.currency(), 
            request.description(),
            request.customerEmail(),
            request.customerName(),
            request.metadata(),
            applicationFee, 
            connectedAccountId
        );

        // 5. Update Record
        payment.setPaymentIntentId(intentId);
        paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse getPayment(java.util.UUID id) {
        String tenantId = SecurityContextHelper.getCurrentTenantId();
        Payment payment = paymentRepository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException("Payment not found"));
        return mapToResponse(payment);
    }

    @Transactional
    public void updateStatus(String paymentIntentId, String newStatus) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException("Payment not found for intent: " + paymentIntentId));
        
        try (var ignored = org.slf4j.MDC.putCloseable(BillingConstants.MDC.TENANT_ID, payment.getTenantId())) {
             stateMachine.validateTransition(payment.getStatus(), newStatus);
             payment.setStatus(newStatus);
             paymentRepository.save(payment);
             auditService.logPaymentAttempt(payment.getId(), newStatus);
        }
    }

    private PaymentDtos.PaymentResponse mapToResponse(Payment payment) {
        return new PaymentDtos.PaymentResponse(
            payment.getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getCreatedAt()
        );
    }
}
