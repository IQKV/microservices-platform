package com.iqscaffold.contactservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import com.iqscaffold.contactservice.shared.exception.DuplicateResourceException;
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
 * Global exception handler for the Contact Service.
 * Provides centralized error handling with RFC 7807 Problem Details compliance.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles validation errors from @Valid annotations.
   *
   * @param ex      The validation exception
   * @param request The HTTP request
   * @return Problem detail response with field errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed for one or more fields"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/validation-error"));
    problemDetail.setTitle("Validation Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    // Add field errors
    Map<String, String> fieldErrors = new HashMap<>();
    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }
    problemDetail.setProperty("errors", fieldErrors);

    logger.warn("Validation error on {}: {}", request.getRequestURI(), fieldErrors);
    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * Handles constraint violation exceptions.
   *
   * @param ex      The constraint violation exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/validation-error"));
    problemDetail.setTitle("Constraint Violation");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.warn("Constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * Handles contact not found exceptions.
   *
   * @param ex      The not found exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(ContactNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleContactNotFoundException(
      ContactNotFoundException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/not-found"));
    problemDetail.setTitle("Resource Not Found");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.warn("Contact not found on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /**
   * Handles duplicate resource exceptions.
   *
   * @param ex      The duplicate resource exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
      DuplicateResourceException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/conflict"));
    problemDetail.setTitle("Resource Conflict");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.warn("Duplicate resource on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * Handles authentication exceptions.
   *
   * @param ex      The authentication exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Authentication failed"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/unauthorized"));
    problemDetail.setTitle("Unauthorized");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
  }

  /**
   * Handles access denied exceptions.
   *
   * @param ex      The access denied exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      AccessDeniedException ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "Access denied"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/forbidden"));
    problemDetail.setTitle("Access Denied");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  /**
   * Handles all other unhandled exceptions.
   *
   * @param ex      The exception
   * @param request The HTTP request
   * @return Problem detail response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex,
      HttpServletRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred"
    );
    problemDetail.setType(URI.create("https://api.iqscaffold.com/errors/internal-error"));
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    logger.error("Unexpected error on {}: ", request.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }
}
