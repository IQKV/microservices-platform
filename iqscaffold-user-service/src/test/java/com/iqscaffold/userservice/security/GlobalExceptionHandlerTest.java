package com.iqscaffold.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import com.iqscaffold.userservice.authentication.AccountLockedException;
import com.iqscaffold.userservice.authentication.AuthenticationException;
import com.iqscaffold.userservice.authentication.EmailVerificationRequiredException;
import com.iqscaffold.userservice.registration.UserRegistrationService;
import com.iqscaffold.userservice.shared.exception.EmailVerificationException;
import com.iqscaffold.userservice.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @InjectMocks
  private GlobalExceptionHandler exceptionHandler;

  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/v1/test");
    when(request.getMethod()).thenReturn("POST");
    MDC.put("correlationId", "test-correlation-id");
  }

  @Test
  @DisplayName("handleValidationException returns 400 with field errors")
  void handleValidationException() throws Exception {
    var bindingResult = mock(BindingResult.class);
    var fieldError = new FieldError("user", "email", "invalid@", false, new String[] {"Email"}, new Object[0], "Email must be valid");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    // Create a proper MethodParameter for the exception
    var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidationException");
    var methodParameter = new org.springframework.core.MethodParameter(method, -1);
    var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
    var response = exceptionHandler.handleValidationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Request validation failed");
    assertThat(problemDetail.getProperties()).containsKey("code");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("VALIDATION_ERROR");
    assertThat(problemDetail.getProperties()).containsKey("fields");
  }

  @Test
  @DisplayName("handleConstraintViolationException returns 400 with constraint violations")
  void handleConstraintViolationException() {
    @SuppressWarnings("unchecked")
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
    when(violation.getMessage()).thenReturn("must not be null");
    when(violation.getInvalidValue()).thenReturn(null);

    Set<ConstraintViolation<?>> violations = Set.of(violation);
    var exception = new ConstraintViolationException(violations);
    var response = exceptionHandler.handleConstraintViolationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Constraint validation failed");
  }

  @Test
  @DisplayName("handleAuthenticationException returns 401 for invalid credentials")
  void handleAuthenticationException() {
    var exception = new AuthenticationException("Invalid username or password");
    var response = exceptionHandler.handleAuthenticationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Authentication failed");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("AUTH_INVALID_CREDENTIALS");
  }

  @Test
  @DisplayName("handleAuthenticationException returns correct code for account disabled")
  void handleAuthenticationExceptionAccountDisabled() {
    var exception = new AuthenticationException("Account is disabled");
    var response = exceptionHandler.handleAuthenticationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("AUTH_ACCOUNT_DISABLED");
  }

  @Test
  @DisplayName("handleAuthenticationException returns correct code for invalid token")
  void handleAuthenticationExceptionInvalidToken() {
    var exception = new AuthenticationException("Invalid token");
    var response = exceptionHandler.handleAuthenticationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("AUTH_INVALID_TOKEN");
  }

  @Test
  @DisplayName("handleAccountLockedException returns 423 LOCKED status")
  void handleAccountLockedException() {
    var exception = new AccountLockedException("Account temporarily locked");
    var response = exceptionHandler.handleAccountLockedException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Account temporarily locked");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("AUTH_ACCOUNT_LOCKED");
  }

  @Test
  @DisplayName("handleEmailVerificationRequiredException returns 401 with actions")
  void handleEmailVerificationRequiredException() {
    var exception = new EmailVerificationRequiredException("Email not verified");
    var response = exceptionHandler.handleEmailVerificationRequiredException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Email verification required");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("EMAIL_VERIFICATION_REQUIRED");
    assertThat(problemDetail.getProperties()).containsKey("actions");
  }

  @Test
  @DisplayName("handleUserRegistrationException returns 409 for existing user")
  void handleUserRegistrationExceptionConflict() {
    var exception = new UserRegistrationService.UserRegistrationException("Username already exists");
    var response = exceptionHandler.handleUserRegistrationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("USER_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("handleUserRegistrationException returns 400 for invalid email")
  void handleUserRegistrationExceptionBadRequest() {
    var exception = new UserRegistrationService.UserRegistrationException("Invalid email format");
    var response = exceptionHandler.handleUserRegistrationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("VALIDATION_INVALID_EMAIL");
  }

  @Test
  @DisplayName("handleAccessDeniedException returns 403")
  void handleAccessDeniedException() {
    var exception = new AccessDeniedException("Insufficient permissions");
    var response = exceptionHandler.handleAccessDeniedException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Insufficient permissions for this operation");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("AUTH_INSUFFICIENT_PERMISSIONS");
  }

  @Test
  @DisplayName("handleUserManagementException returns 409 for existing user")
  void handleUserManagementExceptionConflict() {
    var exception = new UserManagementService.UserManagementException("Email already exists");
    var response = exceptionHandler.handleUserManagementException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("USER_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("handleUserManagementException returns correct code for user not found")
  void handleUserManagementExceptionNotFound() {
    var exception = new UserManagementService.UserManagementException("User not found");
    var response = exceptionHandler.handleUserManagementException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("USER_NOT_FOUND");
  }

  @Test
  @DisplayName("handleEmailVerificationException returns 400 for invalid token")
  void handleEmailVerificationExceptionInvalidToken() {
    var exception = new EmailVerificationException("Invalid verification token");
    var response = exceptionHandler.handleEmailVerificationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("EMAIL_VERIFICATION_TOKEN_INVALID");
  }

  @Test
  @DisplayName("handleEmailVerificationException returns 429 for rate limit")
  void handleEmailVerificationExceptionRateLimit() {
    var exception = new EmailVerificationException("Rate limit exceeded");
    var response = exceptionHandler.handleEmailVerificationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("EMAIL_RESEND_RATE_LIMITED");
  }

  @Test
  @DisplayName("handleEmailVerificationException returns 404 for user not found")
  void handleEmailVerificationExceptionUserNotFound() {
    var exception = new EmailVerificationException("User not found");
    var response = exceptionHandler.handleEmailVerificationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    var problemDetail = response.getBody();
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("USER_NOT_FOUND");
  }

  @Test
  @DisplayName("handleGenericException returns 500")
  void handleGenericException() {
    var exception = new RuntimeException("Unexpected error");
    var response = exceptionHandler.handleGenericException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getTitle()).isEqualTo("Internal system error");
    assertThat(problemDetail.getProperties().get("code")).isEqualTo("SYSTEM_INTERNAL_ERROR");
  }

  @Test
  @DisplayName("problem detail includes request metadata")
  void problemDetailIncludesMetadata() {
    var exception = new RuntimeException("Test error");
    var response = exceptionHandler.handleGenericException(exception, request);

    var problemDetail = response.getBody();
    assertThat(problemDetail).isNotNull();
    assertThat(problemDetail.getProperties()).containsKeys("path", "method", "correlationId", "requestId");
    assertThat(problemDetail.getProperties().get("path")).isEqualTo("/api/v1/test");
    assertThat(problemDetail.getProperties().get("method")).isEqualTo("POST");
  }
}
