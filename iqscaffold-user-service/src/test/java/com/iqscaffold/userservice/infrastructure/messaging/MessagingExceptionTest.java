package com.iqscaffold.userservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessagingExceptionTest {

  @Test
  void shouldCreateExceptionWithMessage() {
    var exception = new MessagingException("Test message");

    assertEquals("Test message", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void shouldCreateExceptionWithMessageAndCause() {
    var cause = new RuntimeException("Root cause");
    var exception = new MessagingException("Test message", cause);

    assertEquals("Test message", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void shouldBeRuntimeException() {
    var exception = new MessagingException("Test");

    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void shouldPreserveCauseStackTrace() {
    var cause = new IllegalArgumentException("Invalid argument");
    var exception = new MessagingException("Messaging failed", cause);

    assertNotNull(exception.getCause());
    assertEquals("Invalid argument", exception.getCause().getMessage());
  }
}

