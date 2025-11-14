package org.gripday.userservice.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Authentication configuration properties with gripday.auth prefix. Handles JWT, security, and OAuth2 configuration for the auth service.
 */
@ConfigurationProperties(prefix = "gripday.auth")
@Validated
public record AuthConfigurationProperties(
    @Valid @NotNull JwtProperties jwt,
    @Valid @NotNull SecurityProperties security,
    @Valid @NotNull OAuth2Properties oauth2
) {

  /**
   * JWT token configuration properties.
   */
  public record JwtProperties(
      @NotBlank(message = "JWT secret key must not be blank")
      String secretKey,

      @NotNull(message = "Access token expiry must not be null")
      Duration accessTokenExpiry,

      @NotNull(message = "Refresh token expiry must not be null")
      Duration refreshTokenExpiry,

      @NotBlank(message = "JWT issuer must not be blank")
      String issuer,

      @NotBlank(message = "JWT audience must not be blank")
      String audience,

      @Pattern(regexp = "HS256|RS256", message = "JWT algorithm must be either HS256 or RS256")
      String algorithm
  ) {

    /**
     * Validates that access token expiry is shorter than refresh token expiry.
     */
    public JwtProperties {
      if (accessTokenExpiry != null && refreshTokenExpiry != null
          && accessTokenExpiry.compareTo(refreshTokenExpiry) >= 0) {
        throw new IllegalArgumentException("Access token expiry must be shorter than refresh token expiry");
      }
    }
  }

  /**
   * Security configuration properties including password policies and rate limiting.
   */
  public record SecurityProperties(
      @Valid @NotNull PasswordProperties password,
      @Valid @NotNull RateLimitingProperties rateLimiting,
      @Valid @NotNull SessionProperties session
  ) {

    /**
     * Password security configuration.
     */
    public record PasswordProperties(
        @Min(value = 4, message = "Password encoder strength must be at least 4")
        @Max(value = 20, message = "Password encoder strength must not exceed 20")
        int encoderStrength,

        boolean requireSpecialChars,

        @Min(value = 6, message = "Minimum password length must be at least 6")
        @Max(value = 128, message = "Minimum password length must not exceed 128")
        int minLength
    ) {

    }

    /**
     * Rate limiting configuration for authentication attempts.
     */
    public record RateLimitingProperties(
        @Min(value = 1, message = "Login attempts must be at least 1")
        @Max(value = 100, message = "Login attempts must not exceed 100")
        int loginAttempts,

        @NotNull(message = "Lockout duration must not be null")
        Duration lockoutDuration
    ) {

    }

    /**
     * Session management configuration.
     */
    public record SessionProperties(
        @NotNull(message = "Session timeout must not be null")
        Duration timeout,

        @Min(value = 1, message = "Concurrent sessions must be at least 1")
        @Max(value = 10, message = "Concurrent sessions must not exceed 10")
        int concurrentSessions
    ) {

    }
  }

  /**
   * OAuth2 provider configuration.
   */
  public record OAuth2Properties(
      boolean enabled,
      Map<String, OAuth2ProviderProperties> providers
  ) {

    /**
     * Individual OAuth2 provider configuration.
     */
    public record OAuth2ProviderProperties(
        String clientId,
        String clientSecret,
        boolean enabled
    ) {

      /**
       * Validates that if enabled, both clientId and clientSecret are provided.
       */
      public OAuth2ProviderProperties {
        if (enabled && (clientId == null || clientId.isBlank()
            || clientSecret == null || clientSecret.isBlank())) {
          throw new IllegalArgumentException("OAuth2 provider requires both clientId and clientSecret when enabled");
        }
      }
    }
  }
}