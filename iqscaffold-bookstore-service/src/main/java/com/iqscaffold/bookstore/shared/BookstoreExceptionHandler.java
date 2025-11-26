package com.iqscaffold.bookstore.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.iqscaffold.bookstore.catalog.BookNotFoundException;
import com.iqscaffold.bookstore.catalog.CategoryInUseException;
import com.iqscaffold.bookstore.catalog.CategoryNotFoundException;
import com.iqscaffold.bookstore.catalog.DuplicateCategoryException;
import com.iqscaffold.bookstore.catalog.DuplicateIsbnException;
import com.iqscaffold.bookstore.catalog.InvalidPriceRangeException;
import com.iqscaffold.bookstore.inventory.BulkOperationException;
import com.iqscaffold.bookstore.inventory.InsufficientInventoryException;
import com.iqscaffold.bookstore.inventory.InvalidInventoryAdjustmentException;
import com.iqscaffold.bookstore.inventory.InvalidInventoryQuantityException;
import com.iqscaffold.bookstore.shared.InvalidIsbnFormatException;
import com.iqscaffold.bookstore.shared.UnauthorizedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class BookstoreExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(BookstoreExceptionHandler.class);

  @ExceptionHandler(BookNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleBookNotFound(
      BookNotFoundException ex,
      HttpServletRequest request) {

    logger.warn("Book not found: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.NOT_FOUND,
        "The requested book could not be found",
        ex.getMessage(),
        request,
        "RESOURCE_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(CategoryNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleCategoryNotFound(
      CategoryNotFoundException ex,
      HttpServletRequest request) {

    logger.warn("Category not found: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.NOT_FOUND,
        "The requested category could not be found",
        ex.getMessage(),
        request,
        "RESOURCE_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(InsufficientInventoryException.class)
  public ResponseEntity<ProblemDetail> handleInsufficientInventory(
      InsufficientInventoryException ex,
      HttpServletRequest request) {

    logger.warn("Insufficient inventory: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.CONFLICT,
        "Not enough inventory available for the requested operation",
        ex.getMessage(),
        request,
        "DOMAIN_INSUFFICIENT_INVENTORY");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(DuplicateIsbnException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateIsbn(
      DuplicateIsbnException ex,
      HttpServletRequest request) {

    logger.warn("Duplicate ISBN: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.CONFLICT,
        "A book with this ISBN already exists in the catalog",
        ex.getMessage(),
        request,
        "DOMAIN_DUPLICATE_ISBN");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(DuplicateCategoryException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateCategory(
      DuplicateCategoryException ex,
      HttpServletRequest request) {

    logger.warn("Duplicate category: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.CONFLICT,
        "A category with this name already exists",
        ex.getMessage(),
        request,
        "DOMAIN_DUPLICATE_CATEGORY");
    problem.setProperty("categoryName", ex.getCategoryName());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(UnauthorizedOperationException.class)
  public ResponseEntity<ProblemDetail> handleUnauthorizedOperation(
      UnauthorizedOperationException ex,
      HttpServletRequest request) {

    logger.warn("Unauthorized operation: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.FORBIDDEN,
        "You do not have sufficient privileges to perform this operation",
        ex.getMessage(),
        request,
        "AUTH_INSUFFICIENT_PRIVILEGES");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(InvalidInventoryQuantityException.class)
  public ResponseEntity<ProblemDetail> handleInvalidInventoryQuantity(
      InvalidInventoryQuantityException ex,
      HttpServletRequest request) {

    logger.warn("Invalid inventory quantity: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Cannot set inventory quantity below reserved amount",
        ex.getMessage(),
        request,
        "DOMAIN_INVALID_INVENTORY_QUANTITY");
    problem.setProperty("bookId", ex.getBookId());
    problem.setProperty("requestedQuantity", ex.getRequestedQuantity());
    problem.setProperty("reservedQuantity", ex.getReservedQuantity());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(InvalidInventoryAdjustmentException.class)
  public ResponseEntity<ProblemDetail> handleInvalidInventoryAdjustment(
      InvalidInventoryAdjustmentException ex,
      HttpServletRequest request) {

    logger.warn("Invalid inventory adjustment: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Inventory adjustment would result in invalid state",
        ex.getMessage(),
        request,
        "DOMAIN_INVALID_INVENTORY_ADJUSTMENT");
    if (ex.getBookId() != null) {
      problem.setProperty("bookId", ex.getBookId());
      problem.setProperty("currentQuantity", ex.getCurrentQuantity());
      problem.setProperty("adjustment", ex.getAdjustment());
      problem.setProperty("resultingQuantity", ex.getResultingQuantity());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(InvalidPriceRangeException.class)
  public ResponseEntity<ProblemDetail> handleInvalidPriceRange(
      InvalidPriceRangeException ex,
      HttpServletRequest request) {

    logger.warn("Invalid price range: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Invalid price range specified",
        ex.getMessage(),
        request,
        "VALIDATION_INVALID_PRICE_RANGE");
    problem.setProperty("minPrice", ex.getMinPrice());
    problem.setProperty("maxPrice", ex.getMaxPrice());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(InvalidIsbnFormatException.class)
  public ResponseEntity<ProblemDetail> handleInvalidIsbnFormat(
      InvalidIsbnFormatException ex,
      HttpServletRequest request) {

    logger.warn("Invalid ISBN format: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "The provided ISBN format is invalid",
        ex.getMessage(),
        request,
        "VALIDATION_INVALID_ISBN");
    problem.setProperty("isbn", ex.getIsbn());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(BulkOperationException.class)
  public ResponseEntity<ProblemDetail> handleBulkOperation(
      BulkOperationException ex,
      HttpServletRequest request) {

    logger.warn("Bulk operation completed with failures: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.MULTI_STATUS,
        "Bulk operation completed with some failures",
        ex.getMessage(),
        request,
        "DOMAIN_BULK_OPERATION_PARTIAL_FAILURE");
    problem.setProperty("totalRequested", ex.getTotalRequested());
    problem.setProperty("successCount", ex.getSuccessCount());
    problem.setProperty("failureCount", ex.getFailureCount());
    problem.setProperty("failures", ex.getFailures().stream()
        .map(f -> new FailureDetail(f.getBookId(), f.getReason()))
        .toList());
    return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(problem);
  }

  @ExceptionHandler(CategoryInUseException.class)
  public ResponseEntity<ProblemDetail> handleCategoryInUse(
      CategoryInUseException ex,
      HttpServletRequest request) {

    logger.warn("Category in use: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.CONFLICT,
        "Cannot delete category that has books associated with it",
        ex.getMessage(),
        request,
        "DOMAIN_CATEGORY_IN_USE");
    problem.setProperty("categoryId", ex.getCategoryId());
    problem.setProperty("categoryName", ex.getCategoryName());
    problem.setProperty("bookCount", ex.getBookCount());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationErrors(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    logger.warn("Validation failed: {}", ex.getMessage());

    var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::mapFieldError)
        .toList();

    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Request validation failed",
        "One or more fields contain invalid values",
        request,
        "VALIDATION_ERROR");
    problem.setProperty("fields", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {

    logger.warn("Constraint violation: {}", ex.getMessage());

    var fieldErrors = ex.getConstraintViolations().stream()
        .map(violation -> new FieldErrorEntry(
            violation.getPropertyPath().toString(),
            violation.getInvalidValue(),
            violation.getMessage()
        ))
        .toList();

    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Request validation failed",
        "One or more constraints were violated",
        request,
        "VALIDATION_ERROR");
    problem.setProperty("fields", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {

    logger.warn("Type mismatch: {}", ex.getMessage());

    var requiredType = ex.getRequiredType();
    var expectedType = requiredType != null ? requiredType.getSimpleName() : "unknown";

    var fieldError = new FieldErrorEntry(
        ex.getName(),
        ex.getValue(),
        "Invalid value type. Expected: " + expectedType
    );

    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "Invalid parameter type",
        "The provided parameter value has an incorrect type",
        request,
        "VALIDATION_ERROR");
    problem.setProperty("fields", List.of(fieldError));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    logger.warn("Illegal argument: {}", ex.getMessage());
    var problem = baseProblem(HttpStatus.BAD_REQUEST,
        "The provided argument is invalid",
        ex.getMessage(),
        request,
        "VALIDATION_ERROR");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex,
      HttpServletRequest request) {

    logger.error("Unexpected error occurred", ex);
    var problem = baseProblem(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        "Please try again later or contact support if the problem persists",
        request,
        "SYSTEM_INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private FieldErrorEntry mapFieldError(FieldError fieldError) {
    return new FieldErrorEntry(
        fieldError.getField(),
        fieldError.getRejectedValue(),
        fieldError.getDefaultMessage()
    );
  }

  private String getCorrelationId() {
    var correlationId = MDC.get("correlationId");
    return correlationId != null ? correlationId : generateCorrelationId();
  }

  private String generateCorrelationId() {
    return "corr-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private String generateRequestId() {
    return "req-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private ProblemDetail baseProblem(HttpStatus status, String title, String detail, HttpServletRequest request, String code) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create("urn:problem:" + code.toLowerCase(Locale.ROOT)));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    problem.setProperty("method", request.getMethod());
    problem.setProperty("correlationId", getCorrelationId());
    problem.setProperty("requestId", generateRequestId());
    return problem;
  }

  private static record FieldErrorEntry(String field, Object rejectedValue, String message) {

  }

  private static record FailureDetail(Long bookId, String reason) {

  }
}