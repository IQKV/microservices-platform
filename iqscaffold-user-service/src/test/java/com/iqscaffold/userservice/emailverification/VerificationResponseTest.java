package com.iqscaffold.userservice.emailverification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class VerificationResponseTest {

  @Test
  void shouldCreateSuccessfulVerificationResponse() {
    var verifiedAt = LocalDateTime.now();
    var response = new VerificationResponse(
        true,
        "Email verified successfully",
        "john.doe",
        verifiedAt
    );

    assertTrue(response.success());
    assertEquals("Email verified successfully", response.message());
    assertEquals("john.doe", response.username());
    assertEquals(verifiedAt, response.verifiedAt());
  }

  @Test
  void shouldCreateFailedVerificationResponse() {
    var response = new VerificationResponse(
        false,
        "Invalid or expired verification token",
        null,
        null
    );

    assertFalse(response.success());
    assertEquals("Invalid or expired verification token", response.message());
    assertNull(response.username());
    assertNull(response.verifiedAt());
  }

  @Test
  void shouldCreateResponseWithoutUsername() {
    var response = new VerificationResponse(
        false,
        "Token not found",
        null,
        null
    );

    assertFalse(response.success());
    assertNull(response.username());
    assertNull(response.verifiedAt());
  }

  @Test
  void shouldHandleEmptyMessage() {
    var response = new VerificationResponse(
        true,
        "",
        "john.doe",
        LocalDateTime.now()
    );

    assertEquals("", response.message());
  }

  @Test
  void shouldCompareVerificationResponses() {
    var verifiedAt = LocalDateTime.of(2025, 1, 15, 10, 30);
    var response1 = new VerificationResponse(
        true,
        "Success",
        "john.doe",
        verifiedAt
    );

    var response2 = new VerificationResponse(
        true,
        "Success",
        "john.doe",
        verifiedAt
    );

    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void shouldHandleDifferentTimestamps() {
    var time1 = LocalDateTime.of(2025, 1, 15, 10, 30);
    var time2 = LocalDateTime.of(2025, 1, 15, 10, 31);

    var response1 = new VerificationResponse(true, "Success", "john.doe", time1);
    var response2 = new VerificationResponse(true, "Success", "john.doe", time2);

    assertNotEquals(response1, response2);
  }
}
