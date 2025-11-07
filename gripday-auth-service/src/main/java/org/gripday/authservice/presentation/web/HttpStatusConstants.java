package org.gripday.authservice.presentation.web;

import org.springframework.http.HttpStatus;

/**
 * HTTP status code constants and utilities for consistent API responses. Provides standardized status codes and response patterns.
 */
public final class HttpStatusConstants {

  // Success responses (2xx)
  public static final HttpStatus OK = HttpStatus.OK;                           // 200
  public static final HttpStatus CREATED = HttpStatus.CREATED;                 // 201
  public static final HttpStatus ACCEPTED = HttpStatus.ACCEPTED;               // 202
  public static final HttpStatus NO_CONTENT = HttpStatus.NO_CONTENT;           // 204

  // Client error responses (4xx)
  public static final HttpStatus BAD_REQUEST = HttpStatus.BAD_REQUEST;         // 400
  public static final HttpStatus UNAUTHORIZED = HttpStatus.UNAUTHORIZED;       // 401
  public static final HttpStatus FORBIDDEN = HttpStatus.FORBIDDEN;             // 403
  public static final HttpStatus NOT_FOUND = HttpStatus.NOT_FOUND;             // 404
  public static final HttpStatus METHOD_NOT_ALLOWED = HttpStatus.METHOD_NOT_ALLOWED; // 405
  public static final HttpStatus CONFLICT = HttpStatus.CONFLICT;               // 409
  public static final HttpStatus UNPROCESSABLE_ENTITY = HttpStatus.UNPROCESSABLE_ENTITY; // 422
  public static final HttpStatus LOCKED = HttpStatus.LOCKED;                   // 423
  public static final HttpStatus TOO_MANY_REQUESTS = HttpStatus.TOO_MANY_REQUESTS; // 429

  // Server error responses (5xx)
  public static final HttpStatus INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR; // 500
  public static final HttpStatus BAD_GATEWAY = HttpStatus.BAD_GATEWAY;         // 502
  public static final HttpStatus SERVICE_UNAVAILABLE = HttpStatus.SERVICE_UNAVAILABLE; // 503
  public static final HttpStatus GATEWAY_TIMEOUT = HttpStatus.GATEWAY_TIMEOUT; // 504

  private HttpStatusConstants() {
    // Utility class - prevent instantiation
  }

  /**
   * Get appropriate HTTP status for authentication errors.
   */
  public static HttpStatus getAuthenticationErrorStatus(String errorCode) {
    return switch (errorCode) {
      case "AUTH_INVALID_CREDENTIALS", "AUTH_INVALID_TOKEN", "AUTH_TOKEN_EXPIRED" -> UNAUTHORIZED;
      case "AUTH_ACCOUNT_LOCKED" -> LOCKED;
      case "AUTH_INSUFFICIENT_PERMISSIONS" -> FORBIDDEN;
      case "AUTH_ACCOUNT_DISABLED" -> FORBIDDEN;
      default -> UNAUTHORIZED;
    };
  }

  /**
   * Get appropriate HTTP status for validation errors.
   */
  public static HttpStatus getValidationErrorStatus(String errorCode) {
    return switch (errorCode) {
      case "VALIDATION_ERROR", "VALIDATION_INVALID_INPUT" -> BAD_REQUEST;
      case "VALIDATION_INVALID_EMAIL", "VALIDATION_INVALID_PASSWORD" -> BAD_REQUEST;
      case "USER_ALREADY_EXISTS" -> CONFLICT;
      default -> BAD_REQUEST;
    };
  }

  /**
   * Get appropriate HTTP status for resource errors.
   */
  public static HttpStatus getResourceErrorStatus(String errorCode) {
    return switch (errorCode) {
      case "RESOURCE_NOT_FOUND", "USER_NOT_FOUND" -> NOT_FOUND;
      case "RESOURCE_ALREADY_EXISTS" -> CONFLICT;
      case "RESOURCE_CONFLICT" -> CONFLICT;
      default -> BAD_REQUEST;
    };
  }

  /**
   * Check if status code indicates success (2xx).
   */
  public static boolean isSuccess(HttpStatus status) {
    return status.is2xxSuccessful();
  }

  /**
   * Check if status code indicates client error (4xx).
   */
  public static boolean isClientError(HttpStatus status) {
    return status.is4xxClientError();
  }

  /**
   * Check if status code indicates server error (5xx).
   */
  public static boolean isServerError(HttpStatus status) {
    return status.is5xxServerError();
  }
}