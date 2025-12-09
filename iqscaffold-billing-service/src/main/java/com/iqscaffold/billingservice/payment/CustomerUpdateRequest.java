package com.iqscaffold.billingservice.payment;

/**
 * Immutable record representing a customer update request for payment providers.
 *
 * <p>This record encapsulates customer information updates that need to be synchronized
 * with the payment provider's system. It provides a provider-agnostic way to update
 * customer details across different payment providers (Stripe, PayPal, etc.).
 *
 * <h2>Design Rationale</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> Java record ensures thread-safe, immutable request data</li>
 *   <li><strong>Provider Agnostic:</strong> Unified format regardless of payment provider</li>
 *   <li><strong>Partial Updates:</strong> Null fields indicate no change (only update provided fields)</li>
 *   <li><strong>Validation:</strong> Ensures data integrity before sending to provider</li>
 * </ul>
 *
 * <h2>Partial Update Semantics</h2>
 * <p>This record supports partial updates where null values indicate that a field
 * should not be updated. Only non-null fields will be sent to the payment provider.
 * This allows updating specific customer attributes without affecting others.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Update only email
 * CustomerUpdateRequest emailUpdate = new CustomerUpdateRequest(
 *     "newemail@example.com",
 *     null,  // name unchanged
 *     null,  // phone unchanged
 *     null   // address unchanged
 * );
 * paymentProvider.updateCustomer(customerId, emailUpdate);
 *
 * // Update multiple fields
 * CustomerUpdateRequest fullUpdate = new CustomerUpdateRequest(
 *     "updated@example.com",
 *     "John Doe",
 *     "+1-555-0123",
 *     "123 Main St, City, State 12345"
 * );
 * paymentProvider.updateCustomer(customerId, fullUpdate);
 *
 * // Check what fields are being updated
 * if (fullUpdate.hasEmail()) {
 *     log.info("Updating customer email to: {}", fullUpdate.email());
 * }
 * }</pre>
 *
 * @param email   the customer's email address (null if not updating)
 * @param name    the customer's full name (null if not updating)
 * @param phone   the customer's phone number (null if not updating)
 * @param address the customer's billing address (null if not updating)
 */
public record CustomerUpdateRequest(
    String email,
    String name,
    String phone,
    String address
) {

  /**
   * Validates the customer update request.
   *
   * <p>Ensures that:
   * <ul>
   *   <li>At least one field is provided (not all null)</li>
   *   <li>Email format is valid (if provided)</li>
   *   <li>Phone number format is reasonable (if provided)</li>
   * </ul>
   *
   * @throws IllegalArgumentException if validation fails
   */
  public CustomerUpdateRequest {
    if (email == null && name == null && phone == null && address == null) {
      throw new IllegalArgumentException(
          "At least one field must be provided for customer update"
      );
    }

    if (email != null && !email.isBlank() && !isValidEmail(email)) {
      throw new IllegalArgumentException("Invalid email format: " + email);
    }

    if (phone != null && !phone.isBlank() && phone.length() < 7) {
      throw new IllegalArgumentException(
          "Phone number must be at least 7 characters: " + phone
      );
    }
  }

  /**
   * Checks if email is being updated.
   *
   * @return true if email is not null and not blank
   */
  public boolean hasEmail() {
    return email != null && !email.isBlank();
  }

  /**
   * Checks if name is being updated.
   *
   * @return true if name is not null and not blank
   */
  public boolean hasName() {
    return name != null && !name.isBlank();
  }

  /**
   * Checks if phone is being updated.
   *
   * @return true if phone is not null and not blank
   */
  public boolean hasPhone() {
    return phone != null && !phone.isBlank();
  }

  /**
   * Checks if address is being updated.
   *
   * @return true if address is not null and not blank
   */
  public boolean hasAddress() {
    return address != null && !address.isBlank();
  }

  /**
   * Creates a builder for constructing CustomerUpdateRequest instances.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for CustomerUpdateRequest to support fluent API.
   */
  public static class Builder {
    private String email;
    private String name;
    private String phone;
    private String address;

    private Builder() {
    }

    /**
     * Sets the email to update.
     *
     * @param email the customer's email address
     * @return this builder
     */
    public Builder email(String email) {
      this.email = email;
      return this;
    }

    /**
     * Sets the name to update.
     *
     * @param name the customer's full name
     * @return this builder
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the phone to update.
     *
     * @param phone the customer's phone number
     * @return this builder
     */
    public Builder phone(String phone) {
      this.phone = phone;
      return this;
    }

    /**
     * Sets the address to update.
     *
     * @param address the customer's billing address
     * @return this builder
     */
    public Builder address(String address) {
      this.address = address;
      return this;
    }

    /**
     * Builds the CustomerUpdateRequest.
     *
     * @return a new CustomerUpdateRequest instance
     * @throws IllegalArgumentException if validation fails
     */
    public CustomerUpdateRequest build() {
      return new CustomerUpdateRequest(email, name, phone, address);
    }
  }

  /**
   * Simple email validation.
   *
   * @param email the email to validate
   * @return true if email format is valid
   */
  private static boolean isValidEmail(String email) {
    // Simple validation: contains @ and has characters before and after
    int atIndex = email.indexOf('@');
    return atIndex > 0 && atIndex < email.length() - 1 && email.indexOf('@', atIndex + 1) == -1;
  }
}
