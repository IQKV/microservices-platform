package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository;
import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.BillingConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of PaymentService providing core payment lifecycle management.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Coordination of payment creation, state transitions, and auditing.</li>
 * <li>Validation of transitions via {@link PaymentStateMachine}.</li>
 * <li>Integration with {@link PaymentProviderAdapter} for external gateway
 * interaction.</li>
 * <li>Resolution of multi-tenant merchant configurations.</li>
 * </ul>
 * 
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderAdapter paymentProvider;
    private final MerchantStripeConfigRepository merchantConfigRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentAuditTrailService auditService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentProviderAdapter paymentProvider,
            MerchantStripeConfigRepository merchantConfigRepository,
            PaymentStateMachine stateMachine,
            PaymentAuditTrailService auditService) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.merchantConfigRepository = merchantConfigRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public PaymentDtos.PaymentResponse createPaymentIntent(PaymentDtos.CreatePaymentRequest request) {
        // String tenantId = SecurityContextHelper.getCurrentTenantId(); // Not needed
        // for entity field

        // 1. Initial State Validation
        stateMachine.validateTransition(null, BillingConstants.PaymentStatus.PENDING);

        // 2. Resolve Merchant Account (if any)
        // In schema-per-tenant, the merchant config is stored in the public schema
        String tenantId = SecurityContextHelper.getCurrentTenantId();
        var merchantConfig = merchantConfigRepository.findByTenantId(tenantId);

        Optional<String> connectedAccountId = merchantConfig
                .map(com.iqscaffold.billingservice.admin.MerchantStripeConfig::getStripeAccountId);

        // Platform fee logic (use config or default to 0)
        BigDecimal applicationFee = BigDecimal.ZERO;
        if (connectedAccountId.isPresent()) {
            BigDecimal feePercent = merchantConfig
                    .map(com.iqscaffold.billingservice.admin.MerchantStripeConfig::getApplicationFeePercent)
                    .orElse(BigDecimal.valueOf(10.0)); // Default 10%
            applicationFee = request.amount().multiply(feePercent).divide(BigDecimal.valueOf(100), 2,
                    java.math.RoundingMode.HALF_UP);
        }

        Payment payment = new Payment();
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(BillingConstants.PaymentStatus.PENDING);
        payment.setApplicationFeeAmount(applicationFee);
        connectedAccountId.ifPresent(payment::setMerchantAccountId);

        payment = paymentRepository.save(payment); // Save to get UUID

        auditService.logPaymentAttempt(payment.getId(), BillingConstants.PaymentStatus.PENDING);

        // 4. Call Provider with idempotency key
        PaymentProviderAdapter.ProviderPaymentIntent providerIntent = paymentProvider.createPaymentIntent(
                request.amount(),
                request.currency(),
                request.description(),
                request.customerEmail(),
                request.customerName(),
                request.metadata(),
                applicationFee,
                connectedAccountId,
                payment.getId().toString());

        // 5. Update Record
        payment.setPaymentIntentId(providerIntent.id());
        payment.setClientSecret(providerIntent.clientSecret());
        payment.setStatus(BillingConstants.PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse getPayment(UUID id) {
        // No need to filter by tenantId, schema is already isolated
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException(
                        "Payment not found"));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDtos.PaymentResponse> getPayments(Pageable pageable) {
        // Simply findAll, scoped to current schema
        return paymentRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void updateStatus(String paymentIntentId, String newStatus) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException(
                        "Payment not found for intent: " + paymentIntentId));

        // Context setup logic might be complex for webhooks if we don't know the tenant
        // from the payload
        // But assuming the WebhookService or Filter sets up the context (via header or
        // meta lookup)
        // Actually, for schema-per-tenant, we MUST know the tenant to even FIND the
        // payment
        // So this method assumes the correct context is ALREADY active.

        try (var ignored = org.slf4j.MDC.putCloseable("paymentId", payment.getId().toString())) {
            stateMachine.validateTransition(payment.getStatus(), newStatus);
            payment.setStatus(newStatus);
            paymentRepository.save(payment);
            auditService.logPaymentAttempt(payment.getId(), newStatus);
        }
    }

    private PaymentDtos.PaymentResponse mapToResponse(Payment payment) {
        return new PaymentDtos.PaymentResponse(
                payment.getId(),
                payment.getClientSecret(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt());
    }
}