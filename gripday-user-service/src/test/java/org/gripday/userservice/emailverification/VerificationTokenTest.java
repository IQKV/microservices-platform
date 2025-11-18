package org.gripday.userservice.emailverification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class VerificationTokenTest {

  @Test
  void shouldCreateVerificationToken() {
    var expiresAt = LocalDateTime.now().plusHours(24);
    var token = new VerificationToken(
        "abc123token",
        1L,
        expiresAt,
        "tenant-1"
    );

    assertEquals("abc123token", token.getToken());
    assertEquals(1L, token.getUserId());
    assertEquals(expiresAt, token.getExpiresAt());
    assertEquals("tenant-1", token.getTenantId());
    assertFalse(token.getUsed());
  }

  @Test
  void shouldCheckIfTokenIsExpired() {
    var expiredToken = new VerificationToken(
        "expired-token",
        1L,
        LocalDateTime.now().minusHours(1),
        "tenant-1"
    );

    assertTrue(expiredToken.isExpired());
  }

  @Test
  void shouldCheckIfTokenIsNotExpired() {
    var validToken = new VerificationToken(
        "valid-token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertFalse(validToken.isExpired());
  }

  @Test
  void shouldCheckIfTokenIsValid() {
    var validToken = new VerificationToken(
        "valid-token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertTrue(validToken.isValid());
  }

  @Test
  void shouldCheckIfUsedTokenIsInvalid() {
    var usedToken = new VerificationToken(
        "used-token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );
    usedToken.markAsUsed();

    assertFalse(usedToken.isValid());
  }

  @Test
  void shouldCheckIfExpiredTokenIsInvalid() {
    var expiredToken = new VerificationToken(
        "expired-token",
        1L,
        LocalDateTime.now().minusHours(1),
        "tenant-1"
    );

    assertFalse(expiredToken.isValid());
  }

  @Test
  void shouldMarkTokenAsUsed() {
    var token = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertFalse(token.getUsed());
    token.markAsUsed();
    assertTrue(token.getUsed());
  }

  @Test
  void shouldCheckIfTokenIsUnused() {
    var token = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertTrue(token.isUnused());
  }

  @Test
  void shouldCheckIfUsedTokenIsNotUnused() {
    var token = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );
    token.markAsUsed();

    assertFalse(token.isUnused());
  }

  @Test
  void shouldHandleNullExpirationDate() {
    var token = new VerificationToken(
        "token",
        1L,
        null,
        "tenant-1"
    );

    assertFalse(token.isExpired());
  }

  @Test
  void shouldCheckTokenExpirationAtBoundary() {
    var expiresAt = LocalDateTime.now().plusSeconds(1);
    var token = new VerificationToken(
        "token",
        1L,
        expiresAt,
        "tenant-1"
    );

    assertFalse(token.isExpired());
  }

  @Test
  void shouldUpdateTokenProperties() {
    var token = new VerificationToken(
        "original-token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    token.setToken("new-token");
    token.setUserId(2L);
    var newExpiry = LocalDateTime.now().plusHours(48);
    token.setExpiresAt(newExpiry);

    assertEquals("new-token", token.getToken());
    assertEquals(2L, token.getUserId());
    assertEquals(newExpiry, token.getExpiresAt());
  }

  @Test
  void shouldCheckEqualityBasedOnIdTokenAndUserId() {
    var token1 = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    var token2 = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertEquals(token1, token2);
    assertEquals(token1.hashCode(), token2.hashCode());
  }

  @Test
  void shouldNotBeEqualWithDifferentTokens() {
    var token1 = new VerificationToken(
        "token1",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    var token2 = new VerificationToken(
        "token2",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertNotEquals(token1, token2);
  }

  @Test
  void shouldNotBeEqualWithDifferentUserIds() {
    var token1 = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    var token2 = new VerificationToken(
        "token",
        2L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertNotEquals(token1, token2);
  }

  @Test
  void shouldGenerateToStringWithMaskedToken() {
    var token = new VerificationToken(
        "verylongtokenstring123456789",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    var toString = token.toString();
    // The token is masked to first 8 characters
    assertTrue(toString.contains("EmailVerificationToken") || toString.contains("VerificationToken"));
    assertTrue(toString.contains("verylongt...") || toString.contains("verylong..."));
    assertTrue(toString.contains("userId=1"));
    assertTrue(toString.contains("tenantId='tenant-1'"));
  }

  @Test
  void shouldHandleShortTokenInToString() {
    var token = new VerificationToken(
        "short",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    var toString = token.toString();
    assertTrue(toString.contains("short..."));
  }

  @Test
  void shouldBelongToTenant() {
    var token = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertTrue(token.belongsToTenant("tenant-1"));
    assertFalse(token.belongsToTenant("tenant-2"));
  }

  @Test
  void shouldHandleNullTenantCheck() {
    var token = new VerificationToken(
        "token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    assertFalse(token.belongsToTenant(null));
  }

  @Test
  void shouldValidateTokenLifecycle() {
    var token = new VerificationToken(
        "lifecycle-token",
        1L,
        LocalDateTime.now().plusHours(24),
        "tenant-1"
    );

    // Initially valid and unused
    assertTrue(token.isValid());
    assertTrue(token.isUnused());
    assertFalse(token.isExpired());

    // After marking as used
    token.markAsUsed();
    assertFalse(token.isValid());
    assertFalse(token.isUnused());
    assertTrue(token.getUsed());
  }

  @Test
  void shouldHandleExpiredButUnusedToken() {
    var token = new VerificationToken(
        "expired-unused",
        1L,
        LocalDateTime.now().minusHours(1),
        "tenant-1"
    );

    assertTrue(token.isExpired());
    assertTrue(token.isUnused());
    assertFalse(token.isValid());
  }
}
