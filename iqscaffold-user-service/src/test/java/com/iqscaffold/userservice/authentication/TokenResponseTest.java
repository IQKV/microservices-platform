package com.iqscaffold.userservice.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.Set;

import com.iqscaffold.userservice.usermanagement.UserContext;
import org.junit.jupiter.api.Test;


class TokenResponseTest {

  @Test
  void shouldCreateTokenResponseWithAllFields() {
    var userContext = createUserContext();
    var response = new TokenResponse(
        "access-token",
        "refresh-token",
        "Bearer",
        900L,
        userContext,
        "session-123"
    );

    assertEquals("access-token", response.accessToken());
    assertEquals("refresh-token", response.refreshToken());
    assertEquals("Bearer", response.tokenType());
    assertEquals(900L, response.expiresIn());
    assertEquals(userContext, response.user());
    assertEquals("session-123", response.sessionId());
  }

  @Test
  void shouldCreateTokenResponseWithDefaultTokenType() {
    var userContext = createUserContext();
    var response = new TokenResponse(
        "access-token",
        "refresh-token",
        900L,
        userContext
    );

    assertEquals("Bearer", response.tokenType());
    assertNull(response.sessionId());
  }

  @Test
  void shouldCreateTokenResponseWithSessionId() {
    var userContext = createUserContext();
    var response = new TokenResponse(
        "access-token",
        "refresh-token",
        900L,
        userContext,
        "session-456"
    );

    assertEquals("Bearer", response.tokenType());
    assertEquals("session-456", response.sessionId());
  }

  @Test
  void shouldHandleZeroExpiresIn() {
    var userContext = createUserContext();
    var response = new TokenResponse(
        "access-token",
        "refresh-token",
        0L,
        userContext
    );

    assertEquals(0L, response.expiresIn());
  }

  @Test
  void shouldHandleLongExpiresIn() {
    var userContext = createUserContext();
    var response = new TokenResponse(
        "access-token",
        "refresh-token",
        86400L,
        userContext
    );

    assertEquals(86400L, response.expiresIn());
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
        null,
        Map.of()
    );
  }
}
