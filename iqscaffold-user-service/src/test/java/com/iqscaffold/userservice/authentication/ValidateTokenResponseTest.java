package com.iqscaffold.userservice.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.iqscaffold.userservice.usermanagement.UserContext;
import org.junit.jupiter.api.Test;

class ValidateTokenResponseTest {

  @Test
  void shouldCreateValidateTokenResponseWithActiveToken() {
    var userContext = createUserContext();
    var issuedAt = Instant.now().minusSeconds(300);
    var expiresAt = Instant.now().plusSeconds(600);

    var response = new ValidateTokenResponse(
        true,
        "token-123",
        "access",
        issuedAt,
        expiresAt,
        userContext
    );

    assertTrue(response.active());
    assertEquals("token-123", response.tokenId());
    assertEquals("access", response.tokenType());
    assertEquals(issuedAt, response.issuedAt());
    assertEquals(expiresAt, response.expiresAt());
    assertEquals(userContext, response.user());
  }

  @Test
  void shouldCreateValidateTokenResponseWithInactiveToken() {
    var response = new ValidateTokenResponse(
        false,
        null,
        null,
        null,
        null,
        null
    );

    assertFalse(response.active());
    assertNull(response.tokenId());
    assertNull(response.tokenType());
    assertNull(response.issuedAt());
    assertNull(response.expiresAt());
    assertNull(response.user());
  }

  @Test
  void shouldHandleRefreshTokenType() {
    var userContext = createUserContext();
    var issuedAt = Instant.now();
    var expiresAt = Instant.now().plusSeconds(86400);

    var response = new ValidateTokenResponse(
        true,
        "refresh-token-456",
        "refresh",
        issuedAt,
        expiresAt,
        userContext
    );

    assertEquals("refresh", response.tokenType());
  }

  @Test
  void shouldHandleExpiredToken() {
    var userContext = createUserContext();
    var issuedAt = Instant.now().minusSeconds(1000);
    var expiresAt = Instant.now().minusSeconds(100);

    var response = new ValidateTokenResponse(
        false,
        "expired-token",
        "access",
        issuedAt,
        expiresAt,
        userContext
    );

    assertFalse(response.active());
    assertTrue(response.expiresAt().isBefore(Instant.now()));
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
