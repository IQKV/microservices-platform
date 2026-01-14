package com.iqscaffold.pipelineservice.config;

import com.iqscaffold.pipelineservice.shared.exception.BusinessException;
import com.iqscaffold.pipelineservice.shared.exception.ConflictException;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Pipeline Service.
 * <p>
 * Handles exceptions and converts them to RFC 7807 Problem Details format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles validation errors from @Valid annotations.
   *
   * @param ex The validation exception
   * @return Problem detail with validation errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleValidationException(final MethodArgumentNotValidException ex) {
    logger.warn("Validation error: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed for one or more fields"
    );
    problemDetail.setTitle("Validation Error");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    problemDetail.setProperty("errors", errors);

    return problemDetail;
  }

  /**
   * Handles resource not found exceptions.
   *
   * @param ex The not found exception
   * @return Problem detail with error message
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ProblemDetail handleResourceNotFoundException(final ResourceNotFoundException ex) {
    logger.warn("Resource not found: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setTitle("Resource Not Found");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return problemDetail;
  }

  /**
   * Handles conflict exceptions (e.g., duplicate resources).
   *
   * @param ex The conflict exception
   * @return Problem detail with error message
   */
  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleConflictException(final ConflictException ex) {
    logger.warn("Conflict error: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setTitle("Resource Conflict");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return problemDetail;
  }

  /**
   * Handles business rule violations.
   *
   * @param ex The business exception
   * @return Problem detail with error message
   */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleBusinessException(final BusinessException ex) {
    logger.warn("Business rule violation: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setTitle("Business Rule Violation");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return problemDetail;
  }

  /**
   * Handles access denied exceptions.
   *
   * @param ex The access denied exception
   * @return Problem detail with error message
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ProblemDetail handleAccessDeniedException(final AccessDeniedException ex) {
    logger.warn("Access denied: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "You do not have permission to access this resource"
    );
    problemDetail.setTitle("Access Denied");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return problemDetail;
  }

  /**
   * Handles all other unexpected exceptions.
   *
   * @param ex The exception
   * @return Problem detail with generic error message
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ProblemDetail handleGenericException(final Exception ex) {
    logger.error("Unexpected error occurred", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later."
    );
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return problemDetail;
  }
}
