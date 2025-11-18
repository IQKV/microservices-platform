package org.gripday.userservice.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.gripday.userservice.usermanagement.UserContext;
import org.junit.jupiter.api.Test;

class AuthenticationResultTest {

  @Test
  void shouldCreateSuccessResult() {
    var userContext = createUserContext();
    var timestamp = Instant.now();

    var result = new AuthenticationResult.Success(
        userContext,
        "access-token",
        "refresh-token",
        "correlation-123",
        timestamp
    );

    assertInstanceOf(AuthenticationResult.class, result);
    assertEquals(userContext, result.user());
    assertEquals("access-token", result.accessToken());
    assertEquals("refresh-token", result.refreshToken());
    assertEquals("correlation-123", result.correlationId());
    assertEquals(timestamp, result.timestamp());
  }

  @Test
  void shouldCreateFailureResult() {
    var timestamp = Instant.now();

    var result = new AuthenticationResult.Failure(
        "Invalid credentials",
        "AUTH_001",
        "correlation-456",
        timestamp
    );

    assertInstanceOf(AuthenticationResult.class, result);
    assertEquals("Invalid credentials", result.reason());
    assertEquals("AUTH_001", result.errorCode());
    assertEquals("correlation-456", result.correlationId());
    assertEquals(timestamp, result.timestamp());
  }

  @Test
  void shouldBeSealed() {
    var userContext = createUserContext();
    var success = new AuthenticationResult.Success(
        userContext,
        "token",
        "refresh",
        "corr-1",
        Instant.now()
    );

    var failure = new AuthenticationResult.Failure(
        "Failed",
        "ERR_001",
        "corr-2",
        Instant.now()
    );

    assertTrue(success instanceof AuthenticationResult);
    assertTrue(failure instanceof AuthenticationResult);
  }

  @Test
  void shouldHandleSuccessWithNullTokens() {
    var userContext = createUserContext();

    var result = new AuthenticationResult.Success(
        userContext,
        null,
        null,
        "correlation-789",
        Instant.now()
    );

    assertNull(result.accessToken());
    assertNull(result.refreshToken());
    assertNotNull(result.user());
  }

  @Test
  void shouldHandleFailureWithEmptyReason() {
    var result = new AuthenticationResult.Failure(
        "",
        "ERR_002",
        "correlation-999",
        Instant.now()
    );

    assertEquals("", result.reason());
    assertEquals("ERR_002", result.errorCode());
  }

  @Test
  void shouldCompareSuccessResults() {
    var userContext = createUserContext();
    var timestamp = Instant.now();

    var result1 = new AuthenticationResult.Success(
        userContext,
        "token",
        "refresh",
        "corr-1",
        timestamp
    );

    var result2 = new AuthenticationResult.Success(
        userContext,
        "token",
        "refresh",
        "corr-1",
        timestamp
    );

    assertEquals(result1, result2);
    assertEquals(result1.hashCode(), result2.hashCode());
  }

  @Test
  void shouldCompareFailureResults() {
    var timestamp = Instant.now();

    var result1 = new AuthenticationResult.Failure(
        "Failed",
        "ERR_001",
        "corr-1",
        timestamp
    );

    var result2 = new AuthenticationResult.Failure(
        "Failed",
        "ERR_001",
        "corr-1",
        timestamp
    );

    assertEquals(result1, result2);
    assertEquals(result1.hashCode(), result2.hashCode());
  }

  private UserContext createUserContext() {
    return new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER"),
        Set.of("READ_PROFILE"),
        "John",
        "Doe",
        "tenant-1",
        Map.of()
    );
  }
}
