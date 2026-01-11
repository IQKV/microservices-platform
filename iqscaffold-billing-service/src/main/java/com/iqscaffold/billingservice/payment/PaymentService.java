package com.iqscaffold.billingservice.payment;

import java.util.UUID;

import com.iqscaffold.billingservice.payment.dto.PaymentDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for managing the lifecycle of Payments.
 * <p>
 * This interface defines the contract for payment operations including:
 * <ul>
 *   <li>Payment creation and state transitions</li>
 *   <li>Integration with payment providers</li>
 *   <li>Multi-tenant merchant configuration resolution</li>
 *   <li>Payment status synchronization</li>
 * </ul>
 *
 * <h4>Key Features:</h4>
 * <ul>
 *   <li><strong>State Machine Validation</strong> - Enforces valid payment state transitions</li>
 *   <li><strong>Multi-tenant Support</strong> - Handles tenant-specific merchant configurations</li>
 *   <li><strong>Audit Trail</strong> - Comprehensive logging of payment operations</li>
 *   <li><strong>Provider Integration</strong> - Abstracted payment gateway interactions</li>
 * </ul>
 */
public interface PaymentService {

  /**
   * Initiates a new payment flow by creating a local record and interfacing with
   * the payment provider.
   * <p>
   * Steps:
   * <ol>
   * <li>Validates the initial state transition (to PENDING).</li>
   * <li>Resolves the tenant's merchant configuration (if any) for Stripe Connect.</li>
   * <li>Calculates platform fees (application fees) if a connected account is involved.</li>
   * <li>Persists the initial Payment entity to generate a unique ID.</li>
   * <li>Logs the attempt to the audit trail.</li>
   * <li>Calls the payment provider (Stripe) to create a Payment Intent.</li>
   * <li>Updates the local record with the external provider's Intent ID.</li>
   * </ol>
   *
   * @param request The payment creation request containing amount, currency, and customer details
   * @return A response DTO containing the payment status and ID
   */
  PaymentDtos.PaymentResponse createPaymentIntent(PaymentDtos.CreatePaymentRequest request);

  /**
   * Retrieves a payment by its unique identifier.
   *
   * @param id The unique identifier of the payment
   * @return The payment response DTO
   * @throws com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException If payment not found
   */
  PaymentDtos.PaymentResponse getPayment(UUID id);

  /**
   * Retrieves a paginated list of payments for the current tenant.
   *
   * @param pageable The pagination parameters
   * @return A page of payment response DTOs
   */
  Page<PaymentDtos.PaymentResponse> getPayments(Pageable pageable);

  /**
   * Updates the status of a payment based on external webhook events.
   * <p>
   * This method is critical for maintaining consistency between the gateway and
   * local state. It enforces state transitions via the PaymentStateMachine to prevent
   * invalid updates (e.g., preventing a 'COMPLETED' payment from moving back to 'PENDING').
   *
   * @param paymentIntentId The external ID derived from the webhook event (e.g., Stripe PaymentIntent ID)
   * @param newStatus       The new status reported by the gateway
   * @throws com.iqscaffold.billingservice.shared.exception.PaymentNotFoundException     If no matching payment exists
   * @throws com.iqscaffold.billingservice.shared.exception.InvalidPaymentStateException If the transition is illegal
   */
  void updateStatus(String paymentIntentId, String newStatus);
}
