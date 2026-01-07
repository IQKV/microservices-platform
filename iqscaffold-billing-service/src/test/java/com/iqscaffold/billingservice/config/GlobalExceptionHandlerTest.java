package com.iqscaffold.billingservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iqscaffold.billingservice.shared.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock
  private MessageService messageService;

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler(messageService);
  }

  @Test
  void handleUnhandled_shouldReturnProblemDetailWithInternalServerError() {
    // Given
    Exception exception = new Exception("Unexpected error");
    when(messageService.getMessage("error.unexpected"))
        .thenReturn("An unexpected error occurred");

    // When
    ProblemDetail result = exceptionHandler.handleUnhandled(exception);

    // Then
    assertNotNull(result);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
    assertEquals("An unexpected error occurred", result.getDetail());
    assertEquals("Internal Server Error", result.getTitle());
    assertNotNull(result.getType());
    assertEquals("urn:problem-type:internal-server-error", result.getType().toString());
    assertNotNull(result.getProperties());
    assertTrue(result.getProperties().containsKey("timestamp"));
    verify(messageService).getMessage("error.unexpected");
  }

  @Test
  void handleUnhandled_shouldIncludeTimestamp() {
    // Given
    Exception exception = new RuntimeException("Test error");
    when(messageService.getMessage("error.unexpected"))
        .thenReturn("Error message");

    // When
    ProblemDetail result = exceptionHandler.handleUnhandled(exception);

    // Then
    assertNotNull(result);
    assertNotNull(result.getProperties());
    Object timestamp = result.getProperties().get("timestamp");
    assertNotNull(timestamp);
  }

  @Test
  void handleUnhandled_shouldHandleNullPointerException() {
    // Given
    NullPointerException exception = new NullPointerException("Null value");
    when(messageService.getMessage("error.unexpected"))
        .thenReturn("An unexpected error occurred");

    // When
    ProblemDetail result = exceptionHandler.handleUnhandled(exception);

    // Then
    assertNotNull(result);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
  }

  @Test
  void handleUnhandled_shouldHandleIllegalArgumentException() {
    // Given
    IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
    when(messageService.getMessage("error.unexpected"))
        .thenReturn("An unexpected error occurred");

    // When
    ProblemDetail result = exceptionHandler.handleUnhandled(exception);

    // Then
    assertNotNull(result);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
  }

  @Test
  void handleUnhandled_shouldUseMessageServiceForLocalization() {
    // Given
    Exception exception = new Exception("Test");
    String localizedMessage = "Erreur inattendue";
    when(messageService.getMessage("error.unexpected")).thenReturn(localizedMessage);

    // When
    ProblemDetail result = exceptionHandler.handleUnhandled(exception);

    // Then
    assertEquals(localizedMessage, result.getDetail());
    verify(messageService, times(1)).getMessage("error.unexpected");
  }

  @Test
  void handlePayoutNotFound_shouldReturnProblemDetailWithNotFound() {
    // Given
    com.iqscaffold.billingservice.payout.PayoutNotFoundException exception =
        new com.iqscaffold.billingservice.payout.PayoutNotFoundException("Payout with ID 123 not found");

    // When
    ProblemDetail result = exceptionHandler.handlePayoutNotFound(exception);

    // Then
    assertNotNull(result);
    assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    assertEquals("Payout with ID 123 not found", result.getDetail());
    assertEquals("Payout Not Found", result.getTitle());
    assertNotNull(result.getType());
    assertEquals("urn:problem-type:payout-not-found", result.getType().toString());
    assertNotNull(result.getProperties());
    assertTrue(result.getProperties().containsKey("timestamp"));
  }

  @Test
  void handlePayoutNotFound_shouldIncludeTimestamp() {
    // Given
    com.iqscaffold.billingservice.payout.PayoutNotFoundException exception =
        new com.iqscaffold.billingservice.payout.PayoutNotFoundException("Not found");

    // When
    ProblemDetail result = exceptionHandler.handlePayoutNotFound(exception);

    // Then
    assertNotNull(result);
    assertNotNull(result.getProperties());
    Object timestamp = result.getProperties().get("timestamp");
    assertNotNull(timestamp);
  }
}
