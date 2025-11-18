package org.gripday.userservice.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserRegistrationResponseTest {

  @Test
  void shouldCreateUserRegistrationResponse() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        "User registered successfully. Please verify your email."
    );

    assertEquals(1L, response.userId());
    assertEquals("john_doe", response.username());
    assertEquals("john.doe@example.com", response.email());
    assertEquals("John", response.firstName());
    assertEquals("Doe", response.lastName());
    assertFalse(response.emailVerified());
    assertEquals(createdAt, response.createdAt());
    assertEquals("User registered successfully. Please verify your email.", response.message());
  }

  @Test
  void shouldCreateResponseWithVerifiedEmail() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        createdAt,
        "User registered and email verified."
    );

    assertTrue(response.emailVerified());
  }

  @Test
  void shouldCreateResponseWithUnverifiedEmail() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        "Please verify your email."
    );

    assertFalse(response.emailVerified());
  }

  @Test
  void shouldHandleDifferentUserIds() {
    var createdAt = LocalDateTime.now();
    var response1 = new UserRegistrationResponse(
        1L,
        "user1",
        "user1@example.com",
        "User",
        "One",
        false,
        createdAt,
        "Success"
    );

    var response2 = new UserRegistrationResponse(
        2L,
        "user2",
        "user2@example.com",
        "User",
        "Two",
        false,
        createdAt,
        "Success"
    );

    assertNotEquals(response1.userId(), response2.userId());
  }

  @Test
  void shouldHandleDifferentTimestamps() {
    var time1 = LocalDateTime.of(2024, 1, 15, 10, 30);
    var time2 = LocalDateTime.of(2024, 1, 15, 10, 31);

    var response1 = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        time1,
        "Success"
    );

    var response2 = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        time2,
        "Success"
    );

    assertNotEquals(response1.createdAt(), response2.createdAt());
  }

  @Test
  void shouldCompareEqualResponses() {
    var createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
    var response1 = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        "Success"
    );

    var response2 = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        "Success"
    );

    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void shouldHandleVariousMessages() {
    var createdAt = LocalDateTime.now();
    var messages = new String[]{
        "User registered successfully. Please verify your email.",
        "Registration complete. Check your inbox for verification link.",
        "Welcome! Please verify your email to activate your account.",
        "Account created. Verification email sent."
    };

    for (var message : messages) {
      var response = new UserRegistrationResponse(
          1L,
          "john_doe",
          "john.doe@example.com",
          "John",
          "Doe",
          false,
          createdAt,
          message
      );
      assertEquals(message, response.message());
    }
  }

  @Test
  void shouldHandleEmptyMessage() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        ""
    );

    assertEquals("", response.message());
  }

  @Test
  void shouldHandleNullMessage() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        createdAt,
        null
    );

    assertNull(response.message());
  }

  @Test
  void shouldHandleSpecialCharactersInNames() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "Jean-Pierre",
        "O'Brien",
        false,
        createdAt,
        "Success"
    );

    assertEquals("Jean-Pierre", response.firstName());
    assertEquals("O'Brien", response.lastName());
  }

  @Test
  void shouldHandleUnicodeCharactersInNames() {
    var createdAt = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "José",
        "Müller",
        false,
        createdAt,
        "Success"
    );

    assertEquals("José", response.firstName());
    assertEquals("Müller", response.lastName());
  }

  @Test
  void shouldHandleLongNames() {
    var createdAt = LocalDateTime.now();
    var longFirstName = "VeryLongFirstNameThatSomeoneActuallyHas";
    var longLastName = "VeryLongLastNameThatSomeoneActuallyHas";

    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        longFirstName,
        longLastName,
        false,
        createdAt,
        "Success"
    );

    assertEquals(longFirstName, response.firstName());
    assertEquals(longLastName, response.lastName());
  }

  @Test
  void shouldHandleRecentCreationTime() {
    var now = LocalDateTime.now();
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        now,
        "Success"
    );

    assertTrue(response.createdAt().isBefore(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void shouldHandlePastCreationTime() {
    var pastTime = LocalDateTime.now().minusDays(30);
    var response = new UserRegistrationResponse(
        1L,
        "john_doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        pastTime,
        "Success"
    );

    assertTrue(response.createdAt().isBefore(LocalDateTime.now()));
  }
}
