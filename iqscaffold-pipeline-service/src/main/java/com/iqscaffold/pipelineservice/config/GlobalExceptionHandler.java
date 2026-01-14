package com.iqscaffold.pipelineservice.config;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.pipelineservice.shared.exception.BusinessException;
import com.iqscaffold.pipelineservice.shared.exception.ConflictException;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
   * @param request The HTTP request
   * @return Problem detail with validation errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      final MethodArgumentNotValidException ex,
      final HttpServletRequest request) {
    logger.warn("Validation error: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed for one or more fields"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/validation-error"));
    problemDetail.setTitle("Validation Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    problemDetail.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * Handles resource not found exceptions.
   *
   * @param ex The not found exception
   * @param request The HTTP request
   * @return Problem detail with error message
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
      final ResourceNotFoundException ex,
      final HttpServletRequest request) {
    logger.warn("Resource not found: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/not-found"));
    problemDetail.setTitle("Resource Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles conflict exceptions (e.g., duplicate resources).
   *
   * @param ex The conflict exception
   * @param request The HTTP request
   * @return Problem detail with error message
   */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ProblemDetail> handleConflictException(
      final ConflictException ex,
      final HttpServletRequest request) {
    logger.warn("Conflict error: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/conflict"));
    problemDetail.setTitle("Resource Conflict");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * Handles business rule violations.
   *
   * @param ex The business exception
   * @param request The HTTP request
   * @return Problem detail with error message
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ProblemDetail> handleBusinessException(
      final BusinessException ex,
      final HttpServletRequest request) {
    logger.warn("Business rule violation: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/business-rule-violation"));
    problemDetail.setTitle("Business Rule Violation");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * Handles authentication exceptions.
   *
   * @param ex The authentication exception
   * @param request The HTTP request
   * @return Problem detail with error message
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      final AuthenticationException ex,
      final HttpServletRequest request) {
    logger.warn("Authentication failed: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Authentication failed"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/unauthorized"));
    problemDetail.setTitle("Unauthorized");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
  }

  /**
   * Handles access denied exceptions.
   *
   * @param ex The access denied exception
   * @param request The HTTP request
   * @return Problem detail with error message
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      final AccessDeniedException ex,
      final HttpServletRequest request) {
    logger.warn("Access denied: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "You do not have permission to access this resource"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/forbidden"));
    problemDetail.setTitle("Access Denied");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  /**
   * Handles all other unexpected exceptions.
   *
   * @param ex The exception
   * @param request The HTTP request
   * @return Problem detail with generic error message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      final Exception ex,
      final HttpServletRequest request) {
    logger.error("Unexpected error occurred", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later."
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/internal-error"));
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }
}
