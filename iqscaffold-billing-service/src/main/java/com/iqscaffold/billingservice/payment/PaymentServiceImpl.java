package com.iqscaffold.billingservice.payment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import com.iqscaffold.billingservice.shared.BillingConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final GatewayConfigurationService gatewayConfigService;
  private final PaymentStateMachine stateMachine;
  private final PaymentAuditTrailService auditService;

  public PaymentServiceImpl(
      final PaymentRepository paymentRepository,
      final GatewayConfigurationService gatewayConfigService,
      final PaymentStateMachine stateMachine,
      final PaymentAuditTrailService auditService) {
    this.paymentRepository = paymentRepository;
    this.gatewayConfigService = gatewayConfigService;
    this.stateMachine = stateMachine;
    this.auditService = auditService;
  }

  @Override
  @Transactional
  public PaymentDtos.PaymentResponse createPaymentIntent(PaymentDtos.CreatePaymentRequest request) {
    // 1. Initial State Validation
    stateMachine.validateTransition(null, BillingConstants.PaymentStatus.PENDING);

    // 2. Resolve Gateway Configuration for Current Tenant
    // This determines which payment gateway to use (Stripe, PayPal, etc.)
    GatewayConfigurationService.GatewayConfiguration gatewayConfig = 
        gatewayConfigService.resolveGatewayForCurrentTenant();
    
    // 3. Get the appropriate payment provider adapter
    PaymentProviderAdapter paymentProvider = gatewayConfigService.getProviderForCurrentTenant();
    
    // 4. Extract gateway configuration details
    Optional<String> connectedAccountId = gatewayConfig.gatewayAccountId();
    
    // Platform fee logic (use config or default to 0)
    BigDecimal applicationFee = BigDecimal.ZERO;
    if (gatewayConfig.hasConfiguration() && connectedAccountId.isPresent()) {
      BigDecimal feePercent = gatewayConfig.applicationFeePercent()
          .orElse(BigDecimal.valueOf(10.0)); // Default 10%
      applicationFee = request.amount().multiply(feePercent)
          .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    // 5. Create payment entity
    Payment payment = new Payment();
    payment.setAmount(request.amount());
    payment.setCurrency(request.currency());
    payment.setStatus(BillingConstants.PaymentStatus.PENDING);
    payment.setApplicationFeeAmount(applicationFee);
    connectedAccountId.ifPresent(payment::setMerchantAccountId);

    payment = paymentRepository.save(payment); // Save to get UUID

    auditService.logPaymentAttempt(payment.getId(), BillingConstants.PaymentStatus.PENDING);

    // 6. Call selected payment provider with idempotency key
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

    // 7. Update payment record with provider response
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
