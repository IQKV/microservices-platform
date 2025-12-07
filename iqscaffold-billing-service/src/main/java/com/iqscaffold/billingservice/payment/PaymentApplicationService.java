package com.iqscaffold.billingservice.payment;

import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodRepository;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.PaymentFailed;
import com.iqscaffold.billingservice.shared.event.PaymentRefunded;
import com.iqscaffold.billingservice.shared.event.PaymentSucceeded;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for payment processing orchestration.
 * 
 * <p>This service provides a thin orchestration layer for payment operations,
 * delegating business logic to Payment and PaymentMethod aggregates while
 * coordinating with external payment providers through the anti-corruption layer.
 * 
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Thin Orchestration:</strong> Coordinates operations without business logic</li>
 *   <li><strong>Aggregate Delegation:</strong> Business rules enforced by aggregates</li>
 *   <li><strong>Anti-Corruption Layer:</strong> External calls through PaymentProviderAdapter</li>
 *   <li><strong>Transaction Management:</strong> Manages transaction boundaries</li>
 *   <li><strong>Event Publishing:</strong> Publishes domain events after persistence</li>
 *   <li><strong>Idempotency:</strong> Prevents duplicate payment processing</li>
 *   <li><strong>Resilience:</strong> Circuit breaker and timeout for external calls</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final DomainEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String PAYMENT_METHOD_CACHE = "payment-methods";
    
    /**
     * Adds a new payment method for a tenant.
     * 
     * <p>This method creates a payment method with the payment provider and stores
     * it in the database. The token should be obtained from the provider's client-side
     * SDK to ensure PCI compliance.
     * 
     * <p><strong>Sync Processing:</strong> Payment method creation is synchronous
     * because users need immediate feedback to continue with payment.
     * 
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param token the payment method token from provider's client SDK
     * @param customerId the provider customer ID (created if null)
     * @return the created payment method DTO
     * @throws PaymentException if payment method creation fails
     */
    @Transactional
    @CacheEvict(value = PAYMENT_METHOD_CACHE, key = "#tenantId")
    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "addPaymentMethodFallback")
    @TimeLimiter(name = "paymentProvider")
    public CompletableFuture<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> addPaymentMethod(
        UUID tenantId,
        UUID userId,
        String token,
        String customerId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Adding payment method for tenant: {}, user: {}", tenantId, userId);
            
            try {
                PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
                
                // Create payment method with provider
                PaymentMethodDetails details = provider.createPaymentMethod(customerId, token);
                
                // Convert type string to enum
                com.iqscaffold.billingservice.paymentmethod.PaymentMethodType methodType = 
                    com.iqscaffold.billingservice.paymentmethod.PaymentMethodType.valueOf(
                        details.type().toUpperCase()
                    );
                
                // Create domain entity
                PaymentMethod paymentMethod = new PaymentMethod(
                    tenantId,
                    userId,
                    methodType,
                    details.providerPaymentMethodId()
                );
                
                // Set card details if applicable
                if (methodType == com.iqscaffold.billingservice.paymentmethod.PaymentMethodType.CARD) {
                    paymentMethod.setCardDetails(
                        details.last4(),
                        details.brand(),
                        details.expiryMonth(),
                        details.expiryYear()
                    );
                }
                
                // Save to database
                PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
                
                log.info("Payment method created successfully: {}", saved.getId());
                
                return mapToDto(saved);
                
            } catch (Exception e) {
                log.error("Failed to add payment method for tenant: {}", tenantId, e);
                throw new PaymentException.InvalidPaymentMethodException(
                    token,
                    "Failed to create payment method: " + e.getMessage(),
                    e
                );
            }
        });
    }
    
    /**
     * Fallback method for addPaymentMethod when circuit breaker opens.
     */
    private CompletableFuture<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> addPaymentMethodFallback(
        UUID tenantId,
        UUID userId,
        String token,
        String customerId,
        Exception e
    ) {
        log.error("Circuit breaker opened for addPaymentMethod, tenant: {}", tenantId, e);
        return CompletableFuture.failedFuture(
            new PaymentException("Payment provider temporarily unavailable. Please try again later.", e)
        );
    }
    
    /**
     * Removes a payment method.
     * 
     * @param tenantId the tenant ID
     * @param paymentMethodId the payment method ID
     * @throws PaymentException if payment method not found or removal fails
     */
    @Transactional
    @CacheEvict(value = PAYMENT_METHOD_CACHE, key = "#tenantId")
    @CircuitBreaker(name = "paymentProvider")
    public void removePaymentMethod(UUID tenantId, Long paymentMethodId) {
        log.info("Removing payment method: {} for tenant: {}", paymentMethodId, tenantId);
        
        PaymentMethod paymentMethod = paymentMethodRepository
            .findByIdAndTenantId(paymentMethodId, tenantId)
            .orElseThrow(() -> new PaymentException.PaymentMethodNotFoundException(
                paymentMethodId.toString()
            ));
        
        // Deactivate in domain
        paymentMethod.deactivate();
        paymentMethodRepository.save(paymentMethod);
        
        // Delete from provider
        try {
            PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
            provider.deletePaymentMethod(paymentMethod.getProviderPaymentMethodId());
        } catch (Exception e) {
            log.warn("Failed to delete payment method from provider: {}", paymentMethodId, e);
            // Continue - already deactivated in our system
        }
        
        log.info("Payment method removed successfully: {}", paymentMethodId);
    }
    
    /**
     * Sets a payment method as the default for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param paymentMethodId the payment method ID
     * @throws PaymentException if payment method not found
     */
    @Transactional
    @CacheEvict(value = PAYMENT_METHOD_CACHE, key = "#tenantId")
    public void setDefaultPaymentMethod(UUID tenantId, Long paymentMethodId) {
        log.info("Setting default payment method: {} for tenant: {}", paymentMethodId, tenantId);
        
        PaymentMethod paymentMethod = paymentMethodRepository
            .findByIdAndTenantId(paymentMethodId, tenantId)
            .orElseThrow(() -> new PaymentException.PaymentMethodNotFoundException(
                paymentMethodId.toString()
            ));
        
        // Remove default from all other payment methods
        List<PaymentMethod> currentDefaults = paymentMethodRepository
            .findByTenantIdAndIsDefaultTrue(tenantId);
        
        for (PaymentMethod current : currentDefaults) {
            current.removeDefaultStatus();
            paymentMethodRepository.save(current);
        }
        
        // Set new default
        paymentMethod.markAsDefault();
        paymentMethodRepository.save(paymentMethod);
        
        log.info("Default payment method set successfully: {}", paymentMethodId);
    }
    
    /**
     * Lists all payment methods for a tenant.
     * 
     * <p>Results are cached for 10 minutes to reduce database load.
     * 
     * @param tenantId the tenant ID
     * @return list of payment method DTOs
     */
    @Transactional(readOnly = true)
    @Cacheable(value = PAYMENT_METHOD_CACHE, key = "#tenantId")
    public List<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> listPaymentMethods(UUID tenantId) {
        log.debug("Listing payment methods for tenant: {}", tenantId);
        
        return paymentMethodRepository.findByTenantIdAndActiveTrue(tenantId)
            .stream()
            .map(this::mapToDto)
            .toList();
    }
    
    /**
     * Processes a payment for an invoice.
     * 
     * <p><strong>Sync Processing:</strong> Payment processing is synchronous because
     * users need immediate feedback on payment success or failure.
     * 
     * <p><strong>Timeout Handling:</strong> Payment provider calls have a 10-second timeout.
     * 
     * <p><strong>Circuit Breaker:</strong> Protects against cascading failures when
     * payment provider is down.
     * 
     * <p><strong>Idempotency:</strong> Uses idempotency keys to prevent duplicate charges.
     * If a payment with the same idempotency key has already been processed, the
     * cached result is returned.
     * 
     * @param invoiceId the invoice ID
     * @param paymentMethodId the payment method ID
     * @param idempotencyKey unique key to ensure idempotent processing
     * @return the payment DTO
     * @throws PaymentException if payment processing fails
     * @throws InvoiceException if invoice not found
     */
    @Transactional
    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "processPaymentFallback")
    @TimeLimiter(name = "paymentProvider")
    public CompletableFuture<PaymentDto> processPayment(
        Long invoiceId,
        Long paymentMethodId,
        String idempotencyKey
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing payment for invoice: {}, payment method: {}, idempotency key: {}",
                invoiceId, paymentMethodId, idempotencyKey);
            
            // Check idempotency
            String idempotencyRedisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Object cachedResult = redisTemplate.opsForValue().get(idempotencyRedisKey);
            
            if (cachedResult != null) {
                log.info("Returning cached payment result for idempotency key: {}", idempotencyKey);
                return (PaymentDto) cachedResult;
            }
            
            // Load invoice
            Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceException.InvoiceNotFoundException(invoiceId.toString()));
            
            // Load payment method
            PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentException.PaymentMethodNotFoundException(
                    paymentMethodId.toString()
                ));
            
            // Create payment entity
            Payment payment = new Payment(
                invoice,
                invoice.getTenantId(),
                invoice.getTotal(),
                invoice.getCurrency(),
                paymentMethod
            );
            
            // Save payment in PENDING status
            payment = paymentRepository.save(payment);
            
            try {
                // Process payment with provider
                PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
                PaymentResult result = provider.processPayment(
                    paymentMethod.getProviderPaymentMethodId(),
                    invoice.getTotal(),
                    invoice.getCurrency(),
                    idempotencyKey
                );
                
                if (result.success()) {
                    // Mark payment as succeeded
                    payment.markAsSucceeded(result.providerPaymentId());
                    payment = paymentRepository.save(payment);
                    
                    // Publish PaymentSucceeded event
                    eventPublisher.publish(new PaymentSucceeded(
                        null, // eventId - auto-generated
                        null, // occurredAt - auto-generated
                        payment.getId(),
                        payment.getTenantId(),
                        payment.getInvoice().getId(),
                        payment.getInvoice().getSubscription().getId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getPaymentMethod().getId(),
                        result.providerPaymentId()
                    ));
                    
                    log.info("Payment processed successfully: {}", payment.getId());
                    
                } else {
                    // Mark payment as failed
                    payment.markAsFailed(result.failureReason());
                    payment = paymentRepository.save(payment);
                    
                    // Publish PaymentFailed event (triggers async retry scheduling)
                    eventPublisher.publish(new PaymentFailed(
                        null, // eventId - auto-generated
                        null, // occurredAt - auto-generated
                        payment.getId(),
                        payment.getTenantId(),
                        payment.getInvoice().getId(),
                        payment.getInvoice().getSubscription().getId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getPaymentMethod().getId(),
                        result.failureReason(),
                        0 // retryAttempt - initial failure
                    ));
                    
                    log.warn("Payment failed: {}, reason: {}", payment.getId(), result.failureReason());
                }
                
                PaymentDto dto = mapToDto(payment);
                
                // Cache result for idempotency (24-hour TTL)
                redisTemplate.opsForValue().set(idempotencyRedisKey, dto, IDEMPOTENCY_TTL);
                
                return dto;
                
            } catch (Exception e) {
                // Mark payment as failed
                payment.markAsFailed("Payment provider error: " + e.getMessage());
                payment = paymentRepository.save(payment);
                
                // Publish PaymentFailed event
                eventPublisher.publish(new PaymentFailed(
                    null, // eventId - auto-generated
                    null, // occurredAt - auto-generated
                    payment.getId(),
                    payment.getTenantId(),
                    payment.getInvoice().getId(),
                    payment.getInvoice().getSubscription().getId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getPaymentMethod().getId(),
                    e.getMessage(),
                    0 // retryAttempt - initial failure
                ));
                
                log.error("Payment processing failed: {}", payment.getId(), e);
                throw new PaymentException.PaymentFailedException(
                    payment.getId().toString(),
                    e.getMessage(),
                    e
                );
            }
        });
    }
    
    /**
     * Fallback method for processPayment when circuit breaker opens.
     */
    private CompletableFuture<PaymentDto> processPaymentFallback(
        Long invoiceId,
        Long paymentMethodId,
        String idempotencyKey,
        Exception e
    ) {
        log.error("Circuit breaker opened for processPayment, invoice: {}", invoiceId, e);
        return CompletableFuture.failedFuture(
            new PaymentException("Payment provider temporarily unavailable. Please try again later.", e)
        );
    }
    
    /**
     * Retries a failed payment.
     * 
     * <p><strong>Async Processing:</strong> Payment retries are scheduled via
     * PaymentRetryConsumer with exponential backoff (Day 1, Day 3, Day 7, Day 14).
     * 
     * <p>This method is called by the retry consumer to attempt payment again.
     * 
     * @param paymentId the payment ID to retry
     * @return the updated payment DTO
     * @throws PaymentException if payment not found or retry fails
     */
    @Transactional
    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "retryPaymentFallback")
    @TimeLimiter(name = "paymentProvider")
    public CompletableFuture<PaymentDto> retryPayment(Long paymentId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Retrying payment: {}", paymentId);
            
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(
                    "Payment not found: " + paymentId
                ));
            
            if (payment.getStatus() != PaymentStatus.FAILED) {
                log.warn("Cannot retry payment that is not in FAILED status: {}", paymentId);
                return mapToDto(payment);
            }
            
            // Reset to PENDING for retry
            Payment retryPayment = new Payment(
                payment.getInvoice(),
                payment.getTenantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod()
            );
            
            retryPayment = paymentRepository.save(retryPayment);
            
            try {
                // Process payment with provider
                PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
                PaymentResult result = provider.processPayment(
                    payment.getPaymentMethod().getProviderPaymentMethodId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    "retry-" + paymentId + "-" + System.currentTimeMillis()
                );
                
                if (result.success()) {
                    retryPayment.markAsSucceeded(result.providerPaymentId());
                    retryPayment = paymentRepository.save(retryPayment);
                    
                    // Publish PaymentSucceeded event
                    eventPublisher.publish(new PaymentSucceeded(
                        null, // eventId - auto-generated
                        null, // occurredAt - auto-generated
                        retryPayment.getId(),
                        retryPayment.getTenantId(),
                        retryPayment.getInvoice().getId(),
                        retryPayment.getInvoice().getSubscription().getId(),
                        retryPayment.getAmount(),
                        retryPayment.getCurrency(),
                        retryPayment.getPaymentMethod().getId(),
                        result.providerPaymentId()
                    ));
                    
                    log.info("Payment retry succeeded: {}", retryPayment.getId());
                    
                } else {
                    retryPayment.markAsFailed(result.failureReason());
                    retryPayment = paymentRepository.save(retryPayment);
                    
                    // Publish PaymentFailed event
                    eventPublisher.publish(new PaymentFailed(
                        null, // eventId - auto-generated
                        null, // occurredAt - auto-generated
                        retryPayment.getId(),
                        retryPayment.getTenantId(),
                        retryPayment.getInvoice().getId(),
                        retryPayment.getInvoice().getSubscription().getId(),
                        retryPayment.getAmount(),
                        retryPayment.getCurrency(),
                        retryPayment.getPaymentMethod().getId(),
                        result.failureReason(),
                        1 // retryAttempt - this is a retry
                    ));
                    
                    log.warn("Payment retry failed: {}, reason: {}", 
                        retryPayment.getId(), result.failureReason());
                }
                
                return mapToDto(retryPayment);
                
            } catch (Exception e) {
                retryPayment.markAsFailed("Payment provider error: " + e.getMessage());
                retryPayment = paymentRepository.save(retryPayment);
                
                // Publish PaymentFailed event
                eventPublisher.publish(new PaymentFailed(
                    null, // eventId - auto-generated
                    null, // occurredAt - auto-generated
                    retryPayment.getId(),
                    retryPayment.getTenantId(),
                    retryPayment.getInvoice().getId(),
                    retryPayment.getInvoice().getSubscription().getId(),
                    retryPayment.getAmount(),
                    retryPayment.getCurrency(),
                    retryPayment.getPaymentMethod().getId(),
                    e.getMessage(),
                    1 // retryAttempt - this is a retry
                ));
                
                log.error("Payment retry failed: {}", retryPayment.getId(), e);
                throw new PaymentException.PaymentFailedException(
                    retryPayment.getId().toString(),
                    e.getMessage(),
                    e
                );
            }
        });
    }
    
    /**
     * Fallback method for retryPayment when circuit breaker opens.
     */
    private CompletableFuture<PaymentDto> retryPaymentFallback(
        Long paymentId,
        Exception e
    ) {
        log.error("Circuit breaker opened for retryPayment, payment: {}", paymentId, e);
        return CompletableFuture.failedFuture(
            new PaymentException("Payment provider temporarily unavailable. Retry will be rescheduled.", e)
        );
    }
    
    /**
     * Refunds a payment.
     * 
     * <p><strong>Sync Processing:</strong> Refund processing is synchronous because
     * users need immediate confirmation.
     * 
     * @param paymentId the payment ID
     * @param amount the refund amount (null for full refund)
     * @param reason the refund reason
     * @param refundedBy the user ID who initiated the refund
     * @return the updated payment DTO
     * @throws PaymentException if payment not found or refund fails
     */
    @Transactional
    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "refundPaymentFallback")
    @TimeLimiter(name = "paymentProvider")
    public CompletableFuture<PaymentDto> refundPayment(
        Long paymentId,
        BigDecimal amount,
        String reason,
        UUID refundedBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Refunding payment: {}, amount: {}, reason: {}", paymentId, amount, reason);
            
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(
                    "Payment not found: " + paymentId
                ));
            
            if (payment.getStatus() != PaymentStatus.SUCCEEDED && 
                payment.getStatus() != PaymentStatus.REFUNDED) {
                throw new PaymentException(
                    "Can only refund SUCCEEDED or partially REFUNDED payments. Current status: " + 
                    payment.getStatus()
                );
            }
            
            // Use full refundable amount if not specified
            BigDecimal refundAmount = amount != null ? amount : payment.getRefundableAmount();
            
            try {
                // Process refund with provider
                PaymentProviderAdapter provider = paymentProviderFactory.getProvider();
                PaymentResult result = provider.refundPayment(
                    payment.getProviderPaymentId(),
                    refundAmount,
                    reason
                );
                
                if (result.success()) {
                    // Process refund in domain
                    payment.processRefund(refundAmount);
                    payment = paymentRepository.save(payment);
                    
                    // Publish PaymentRefunded event (triggers async notification)
                    eventPublisher.publish(new PaymentRefunded(
                        null, // eventId - auto-generated
                        null, // occurredAt - auto-generated
                        payment.getId(),
                        payment.getTenantId(),
                        payment.getInvoice().getId(),
                        payment.getInvoice().getSubscription().getId(),
                        refundAmount,
                        payment.getCurrency(),
                        reason,
                        refundedBy,
                        result.providerPaymentId() // providerRefundId
                    ));
                    
                    log.info("Payment refunded successfully: {}, amount: {}", 
                        payment.getId(), refundAmount);
                    
                } else {
                    log.error("Refund failed: {}, reason: {}", paymentId, result.failureReason());
                    throw new PaymentException.PaymentFailedException(
                        paymentId.toString(),
                        "Refund failed: " + result.failureReason()
                    );
                }
                
                return mapToDto(payment);
                
            } catch (Exception e) {
                log.error("Refund processing failed: {}", paymentId, e);
                throw new PaymentException.PaymentFailedException(
                    paymentId.toString(),
                    "Refund failed: " + e.getMessage(),
                    e
                );
            }
        });
    }
    
    /**
     * Fallback method for refundPayment when circuit breaker opens.
     */
    private CompletableFuture<PaymentDto> refundPaymentFallback(
        Long paymentId,
        BigDecimal amount,
        String reason,
        UUID refundedBy,
        Exception e
    ) {
        log.error("Circuit breaker opened for refundPayment, payment: {}", paymentId, e);
        return CompletableFuture.failedFuture(
            new PaymentException("Payment provider temporarily unavailable. Please try again later.", e)
        );
    }
    
    /**
     * Gets a payment by ID.
     * 
     * @param paymentId the payment ID
     * @return the payment DTO
     * @throws PaymentException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long paymentId) {
        log.debug("Getting payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentException(
                "Payment not found: " + paymentId
            ));
        
        return mapToDto(payment);
    }
    
    /**
     * Lists payments for an invoice.
     * 
     * @param invoiceId the invoice ID
     * @return list of payment DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> listPaymentsByInvoice(Long invoiceId) {
        log.debug("Listing payments for invoice: {}", invoiceId);
        
        return paymentRepository.findByInvoiceId(invoiceId)
            .stream()
            .map(this::mapToDto)
            .toList();
    }
    
    /**
     * Lists payments for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of payment DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> listPaymentsByTenant(UUID tenantId) {
        log.debug("Listing payments for tenant: {}", tenantId);
        
        return paymentRepository.findByTenantId(tenantId)
            .stream()
            .map(this::mapToDto)
            .toList();
    }
    
    /**
     * Maps Payment entity to PaymentDto.
     */
    private PaymentDto mapToDto(Payment payment) {
        PaymentMethod paymentMethod = payment.getPaymentMethod();
        
        return new PaymentDto(
            payment.getId(),
            payment.getInvoice().getId(),
            payment.getTenantId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            paymentMethod != null ? paymentMethod.getId() : null,
            paymentMethod != null ? paymentMethod.getType().name() : null,
            paymentMethod != null ? paymentMethod.getLast4() : null,
            payment.getProviderPaymentId(),
            payment.getFailureReason(),
            payment.getRefundedAmount(),
            payment.getMetadata(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
    
    /**
     * Maps PaymentMethod entity to PaymentMethodDto.
     */
    private com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto mapToDto(PaymentMethod paymentMethod) {
        return new com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto(
            paymentMethod.getId(),
            paymentMethod.getTenantId(),
            paymentMethod.getUserId(),
            paymentMethod.getType(),
            paymentMethod.getProviderPaymentMethodId(),
            paymentMethod.getLast4(),
            paymentMethod.getBrand(),
            paymentMethod.getExpiryMonth(),
            paymentMethod.getExpiryYear(),
            paymentMethod.getIsDefault(),
            paymentMethod.getActive(),
            paymentMethod.getDisplayName(),
            paymentMethod.getStatusMessage(),
            paymentMethod.isExpired(),
            paymentMethod.getCreatedAt(),
            paymentMethod.getUpdatedAt()
        );
    }
}
