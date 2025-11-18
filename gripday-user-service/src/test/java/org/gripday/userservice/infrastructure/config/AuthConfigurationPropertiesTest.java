package org.gripday.userservice.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthConfigurationPropertiesTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidAuthConfiguration() {
    var jwt = createValidJwtProperties();
    var security = createValidSecurityProperties();
    var oauth2 = createValidOAuth2Properties();

    var config = new AuthConfigurationProperties(jwt, security, oauth2);

    assertNotNull(config.jwt());
    assertNotNull(config.security());
    assertNotNull(config.oauth2());
  }

  @Test
  void shouldValidateJwtSecretKeyNotBlank() {
    var jwt = new AuthConfigurationProperties.JwtProperties(
        "",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "issuer",
        "audience",
        "HS256"
    );

    Set<ConstraintViolation<AuthConfigurationProperties.JwtProperties>> violations = validator.validate(jwt);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateAccessTokenShorterThanRefreshToken() {
    assertThrows(IllegalArgumentException.class, () ->
        new AuthConfigurationProperties.JwtProperties(
            "secret",
            Duration.ofDays(7),
            Duration.ofMinutes(15),
            "issuer",
            "audience",
            "HS256"
        )
    );
  }

  @Test
  void shouldValidateJwtAlgorithm() {
    var jwt = new AuthConfigurationProperties.JwtProperties(
        "secret",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "issuer",
        "audience",
        "INVALID"
    );

    Set<ConstraintViolation<AuthConfigurationProperties.JwtProperties>> violations = validator.validate(jwt);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptHS256Algorithm() {
    var jwt = new AuthConfigurationProperties.JwtProperties(
        "secret",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "issuer",
        "audience",
        "HS256"
    );

    assertTrue(validator.validate(jwt).isEmpty());
  }

  @Test
  void shouldAcceptRS256Algorithm() {
    var jwt = new AuthConfigurationProperties.JwtProperties(
        "secret",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "issuer",
        "audience",
        "RS256"
    );

    assertTrue(validator.validate(jwt).isEmpty());
  }

  @Test
  void shouldValidatePasswordEncoderStrength() {
    var password = new AuthConfigurationProperties.SecurityProperties.PasswordProperties(
        3,
        true,
        8
    );

    Set<ConstraintViolation<AuthConfigurationProperties.SecurityProperties.PasswordProperties>> violations = validator.validate(password);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePasswordMinLength() {
    var password = new AuthConfigurationProperties.SecurityProperties.PasswordProperties(
        10,
        true,
        5
    );

    Set<ConstraintViolation<AuthConfigurationProperties.SecurityProperties.PasswordProperties>> violations = validator.validate(password);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateRateLimitingAttempts() {
    var rateLimiting = new AuthConfigurationProperties.SecurityProperties.RateLimitingProperties(
        0,
        Duration.ofMinutes(15)
    );

    Set<ConstraintViolation<AuthConfigurationProperties.SecurityProperties.RateLimitingProperties>> violations = validator.validate(rateLimiting);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateSessionConcurrentSessions() {
    var session = new AuthConfigurationProperties.SecurityProperties.SessionProperties(
        Duration.ofMinutes(30),
        0
    );

    Set<ConstraintViolation<AuthConfigurationProperties.SecurityProperties.SessionProperties>> violations = validator.validate(session);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateOAuth2ProviderWhenEnabled() {
    assertThrows(IllegalArgumentException.class, () ->
        new AuthConfigurationProperties.OAuth2Properties.OAuth2ProviderProperties(
            null,
            null,
            true
        )
    );
  }

  @Test
  void shouldAllowDisabledOAuth2ProviderWithoutCredentials() {
    var provider = new AuthConfigurationProperties.OAuth2Properties.OAuth2ProviderProperties(
        null,
        null,
        false
    );

    assertFalse(provider.enabled());
  }

  @Test
  void shouldValidateOAuth2ProviderWithBlankClientId() {
    assertThrows(IllegalArgumentException.class, () ->
        new AuthConfigurationProperties.OAuth2Properties.OAuth2ProviderProperties(
            "",
            "secret",
            true
        )
    );
  }

  @Test
  void shouldCreateValidOAuth2Provider() {
    var provider = new AuthConfigurationProperties.OAuth2Properties.OAuth2ProviderProperties(
        "client-id",
        "client-secret",
        true
    );

    assertTrue(provider.enabled());
    assertEquals("client-id", provider.clientId());
    assertEquals("client-secret", provider.clientSecret());
  }

  private AuthConfigurationProperties.JwtProperties createValidJwtProperties() {
    return new AuthConfigurationProperties.JwtProperties(
        "my-secret-key",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "gripday-issuer",
        "gripday-audience",
        "HS256"
    );
  }

  private AuthConfigurationProperties.SecurityProperties createValidSecurityProperties() {
    var password = new AuthConfigurationProperties.SecurityProperties.PasswordProperties(
        10,
        true,
        8
    );

    var rateLimiting = new AuthConfigurationProperties.SecurityProperties.RateLimitingProperties(
        5,
        Duration.ofMinutes(15)
    );

    var session = new AuthConfigurationProperties.SecurityProperties.SessionProperties(
        Duration.ofMinutes(30),
        3
    );

    return new AuthConfigurationProperties.SecurityProperties(password, rateLimiting, session);
  }

  private AuthConfigurationProperties.OAuth2Properties createValidOAuth2Properties() {
    var provider = new AuthConfigurationProperties.OAuth2Properties.OAuth2ProviderProperties(
        "client-id",
        "client-secret",
        true
    );

    return new AuthConfigurationProperties.OAuth2Properties(
        true,
        Map.of("google", provider)
    );
  }
}
