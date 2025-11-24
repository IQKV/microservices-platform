package org.gripday.bookstore.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when bulk operations fail partially or completely.
 */
public class BulkOperationException extends RuntimeException {

  private final int totalRequested;
  private final int successCount;
  private final int failureCount;
  private final List<BulkOperationFailure> failures;

  public BulkOperationException(
      final int totalRequested,
      final int successCount,
      final List<BulkOperationFailure> failures) {
    super(String.format(
        "Bulk operation completed with failures: %d succeeded, %d failed out of %d total",
        successCount, failures.size(), totalRequested));
    this.totalRequested = totalRequested;
    this.successCount = successCount;
    this.failureCount = failures.size();
    this.failures = new ArrayList<>(failures);
  }

  public int getTotalRequested() {
    return totalRequested;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public List<BulkOperationFailure> getFailures() {
    return Collections.unmodifiableList(failures);
  }

  /**
   * Represents a single failure in a bulk operation.
   */
  public static class BulkOperationFailure {
    private final Long bookId;
    private final String reason;
    private final Exception exception;

    public BulkOperationFailure(final Long bookId, final String reason, final Exception exception) {
      this.bookId = bookId;
      this.reason = reason;
      this.exception = exception;
    }

    public Long getBookId() {
      return bookId;
    }

    public String getReason() {
      return reason;
    }

    public Exception getException() {
      return exception;
    }

    @Override
    public String toString() {
      return String.format("Book ID %d: %s", bookId, reason);
    }
  }
}
