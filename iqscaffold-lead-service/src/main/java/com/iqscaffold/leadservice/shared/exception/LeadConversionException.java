package com.iqscaffold.leadservice.shared.exception;

/**
 * Exception thrown when lead conversion to contact fails.
 * <p>
 * This exception indicates that the conversion process encountered an error
 * and rollback was attempted. The exception message contains details about
 * the failure and whether rollback was successful.
 */
public class LeadConversionException extends RuntimeException {

  private final Long leadId;
  private final Long contactId;
  private final boolean rollbackSuccessful;

  /**
   * Constructs a new lead conversion exception.
   *
   * @param message            The detail message
   * @param leadId             The ID of the lead that failed to convert
   * @param contactId          The ID of the contact (if created before failure)
   * @param rollbackSuccessful Whether the rollback was successful
   */
  public LeadConversionException(
      final String message,
      final Long leadId,
      final Long contactId,
      final boolean rollbackSuccessful) {
    super(message);
    this.leadId = leadId;
    this.contactId = contactId;
    this.rollbackSuccessful = rollbackSuccessful;
  }

  /**
   * Constructs a new lead conversion exception with a cause.
   *
   * @param message            The detail message
   * @param cause              The cause of the exception
   * @param leadId             The ID of the lead that failed to convert
   * @param contactId          The ID of the contact (if created before failure)
   * @param rollbackSuccessful Whether the rollback was successful
   */
  public LeadConversionException(
      final String message,
      final Throwable cause,
      final Long leadId,
      final Long contactId,
      final boolean rollbackSuccessful) {
    super(message, cause);
    this.leadId = leadId;
    this.contactId = contactId;
    this.rollbackSuccessful = rollbackSuccessful;
  }

  public Long getLeadId() {
    return leadId;
  }

  public Long getContactId() {
    return contactId;
  }

  public boolean isRollbackSuccessful() {
    return rollbackSuccessful;
  }
}
