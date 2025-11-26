package com.iqscaffold.userservice.emailverification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class VerificationStatusResponseTest {

  @Test
  void shouldCreateVerifiedStatusResponse() {
    var registrationDate = LocalDateTime.now().minusDays(5);
    var response = new VerificationStatusResponse(
        "john.doe@example.com",
        true,
        registrationDate,
        "Email has been verified"
    );

    assertEquals("john.doe@example.com", response.email());
    assertTrue(response.emailVerified());
    assertEquals(registrationDate, response.registrationDate());
    assertEquals("Email has been verified", response.message());
  }

  @Test
  void shouldCreateUnverifiedStatusResponse() {
    var registrationDate = LocalDateTime.now().minusHours(2);
    var response = new VerificationStatusResponse(
        "jane.doe@example.com",
        false,
        registrationDate,
        "Email verification pending. Please check your inbox."
    );

    assertEquals("jane.doe@example.com", response.email());
    assertFalse(response.emailVerified());
    assertEquals(registrationDate, response.registrationDate());
    assertEquals("Email verification pending. Please check your inbox.", response.message());
  }

  @Test
  void shouldHandleRecentRegistration() {
    var registrationDate = LocalDateTime.now().minusMinutes(5);
    var response = new VerificationStatusResponse(
        "new.user@example.com",
        false,
        registrationDate,
        "Verification email sent. Please check your inbox."
    );

    assertFalse(response.emailVerified());
    assertTrue(registrationDate.isAfter(LocalDateTime.now().minusHours(1)));
  }

  @Test
  void shouldHandleOldRegistration() {
    var registrationDate = LocalDateTime.now().minusMonths(6);
    var response = new VerificationStatusResponse(
        "old.user@example.com",
        true,
        registrationDate,
        "Email verified"
    );

    assertTrue(response.emailVerified());
    assertTrue(registrationDate.isBefore(LocalDateTime.now().minusDays(30)));
  }

  @Test
  void shouldCompareStatusResponses() {
    var registrationDate = LocalDateTime.of(2024, 1, 15, 10, 30);
    var response1 = new VerificationStatusResponse(
        "user@example.com",
        true,
        registrationDate,
        "Verified"
    );

    var response2 = new VerificationStatusResponse(
        "user@example.com",
        true,
        registrationDate,
        "Verified"
    );

    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void shouldHandleDifferentEmails() {
    var registrationDate = LocalDateTime.now();
    var response1 = new VerificationStatusResponse(
        "user1@example.com",
        true,
        registrationDate,
        "Verified"
    );

    var response2 = new VerificationStatusResponse(
        "user2@example.com",
        true,
        registrationDate,
        "Verified"
    );

    assertNotEquals(response1, response2);
  }

  @Test
  void shouldHandleEmptyMessage() {
    var response = new VerificationStatusResponse(
        "user@example.com",
        false,
        LocalDateTime.now(),
        ""
    );

    assertEquals("", response.message());
  }

  @Test
  void shouldHandleLongMessage() {
    var longMessage = "Your email verification is pending. " +
                      "We have sent a verification link to your email address. " +
                      "Please check your inbox and spam folder.";
    var response = new VerificationStatusResponse(
        "user@example.com",
        false,
        LocalDateTime.now(),
        longMessage
    );

    assertEquals(longMessage, response.message());
  }
}
