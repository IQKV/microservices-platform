package com.iqscaffold.billingservice.payment;

/**
 * Immutable record representing payment method details from a payment provider.
 *
 * <p>This record encapsulates payment method information returned by payment providers
 * after tokenization. It provides a provider-agnostic representation of payment methods,
 * containing only the information needed by the domain model while maintaining PCI compliance.
 *
 * <h2>PCI Compliance</h2>
 * <p>This record never contains full credit card numbers or sensitive payment data.
 * Only the following safe information is included:
 * <ul>
 *   <li>Provider-generated payment method ID (token)</li>
 *   <li>Last 4 digits of card number (for display purposes)</li>
 *   <li>Card brand (Visa, Mastercard, etc.)</li>
 *   <li>Expiration date (month and year)</li>
 *   <li>Payment method type (card, bank account, etc.)</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> Java record ensures thread-safe, immutable payment method data</li>
 *   <li><strong>Provider Agnostic:</strong> Unified format regardless of payment provider</li>
 *   <li><strong>PCI Compliant:</strong> No sensitive payment data stored</li>
 *   <li><strong>Display Ready:</strong> Contains information suitable for UI display</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create payment method via provider
 * PaymentMethodDetails details = paymentProvider.createPaymentMethod(
 *     customerId,
 *     tokenFromClientSdk
 * );
 *
 * // Store in domain model
 * PaymentMethod paymentMethod = new PaymentMethod();
 * paymentMethod.setProviderPaymentMethodId(details.providerPaymentMethodId());
 * paymentMethod.setType(details.type());
 * paymentMethod.setLast4(details.last4());
 * paymentMethod.setBrand(details.brand());
 * paymentMethod.setExpiryMonth(details.expiryMonth());
 * paymentMethod.setExpiryYear(details.expiryYear());
 *
 * // Display to user
 * String displayText = String.format("%s ending in %s (expires %02d/%d)",
 *     details.brand(),
 *     details.last4(),
 *     details.expiryMonth(),
 *     details.expiryYear()
 * );
 * // Output: "Visa ending in 4242 (expires 12/2025)"
 * }</pre>
 *
 * @param providerPaymentMethodId the payment provider's unique payment method identifier (token)
 * @param type                    the payment method type (CARD, BANK_ACCOUNT, PAYPAL, etc.)
 * @param last4                   the last 4 digits of the card/account number (for display)
 * @param brand                   the card brand (Visa, Mastercard, Amex, etc.) or bank name
 * @param expiryMonth             the expiration month (1-12) for cards, null for other types
 * @param expiryYear              the expiration year (4 digits) for cards, null for other types
 */
public record PaymentMethodDetails(
    String providerPaymentMethodId,
    String type,
    String last4,
    String brand,
    Integer expiryMonth,
    Integer expiryYear
) {

  /**
   * Validates the payment method details.
   *
   * <p>Ensures that:
   * <ul>
   *   <li>Provider payment method ID is not null or blank</li>
   *   <li>Type is not null or blank</li>
   *   <li>Last 4 digits is exactly 4 characters (if provided)</li>
   *   <li>Expiry month is between 1 and 12 (if provided)</li>
   *   <li>Expiry year is a valid 4-digit year (if provided)</li>
   * </ul>
   *
   * @throws IllegalArgumentException if validation fails
   */
  public PaymentMethodDetails {
    if (providerPaymentMethodId == null || providerPaymentMethodId.isBlank()) {
      throw new IllegalArgumentException(
          "Provider payment method ID must not be null or blank"
      );
    }

    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Payment method type must not be null or blank");
    }

    if (last4 != null && last4.length() != 4) {
      throw new IllegalArgumentException("Last 4 digits must be exactly 4 characters");
    }

    if (expiryMonth != null && (expiryMonth < 1 || expiryMonth > 12)) {
      throw new IllegalArgumentException("Expiry month must be between 1 and 12");
    }

    if (expiryYear != null && (expiryYear < 1000 || expiryYear > 9999)) {
      throw new IllegalArgumentException("Expiry year must be a 4-digit year");
    }
  }

  /**
   * Checks if the payment method is a card.
   *
   * @return true if type is "CARD", false otherwise
   */
  public boolean isCard() {
    return "CARD".equalsIgnoreCase(type);
  }

  /**
   * Checks if the payment method is a bank account.
   *
   * @return true if type is "BANK_ACCOUNT", false otherwise
   */
  public boolean isBankAccount() {
    return "BANK_ACCOUNT".equalsIgnoreCase(type);
  }

  /**
   * Checks if the payment method is PayPal.
   *
   * @return true if type is "PAYPAL", false otherwise
   */
  public boolean isPayPal() {
    return "PAYPAL".equalsIgnoreCase(type);
  }

  /**
   * Checks if the payment method has expiration information.
   *
   * @return true if both expiry month and year are present
   */
  public boolean hasExpiration() {
    return expiryMonth != null && expiryYear != null;
  }

  /**
   * Returns a display-friendly string representation of the payment method.
   *
   * <p>Format examples:
   * <ul>
   *   <li>Card: "Visa ending in 4242"</li>
   *   <li>Bank Account: "Chase ending in 6789"</li>
   *   <li>PayPal: "PayPal"</li>
   * </ul>
   *
   * @return a user-friendly display string
   */
  public String toDisplayString() {
    if (brand != null && last4 != null) {
      return String.format("%s ending in %s", brand, last4);
    } else if (brand != null) {
      return brand;
    } else if (last4 != null) {
      return String.format("ending in %s", last4);
    } else {
      return type;
    }
  }

  /**
   * Returns a display-friendly string with expiration information.
   *
   * <p>Format: "Visa ending in 4242 (expires 12/2025)"
   *
   * @return a user-friendly display string with expiration, or just display string if no expiration
   */
  public String toDisplayStringWithExpiration() {
    String display = toDisplayString();

    if (hasExpiration()) {
      return String.format("%s (expires %02d/%d)", display, expiryMonth, expiryYear);
    }

    return display;
  }
}
