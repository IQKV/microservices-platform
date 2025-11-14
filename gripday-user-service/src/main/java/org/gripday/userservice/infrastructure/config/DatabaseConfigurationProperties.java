package org.gripday.userservice.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Database configuration properties with gripday.database prefix. Handles database connection, pooling, and migration configuration.
 */
@ConfigurationProperties(prefix = "gripday.database")
@Validated
public record DatabaseConfigurationProperties(
    @NotBlank(message = "Database URL must not be blank")
    String url,

    @NotBlank(message = "Database username must not be blank")
    String username,

    @NotBlank(message = "Database password must not be blank")
    String password,

    @Valid @NotNull PoolProperties pool,
    @Valid @NotNull MigrationProperties migration
) {

  /**
   * Database connection pool configuration.
   */
  public record PoolProperties(
      @Min(value = 1, message = "Maximum pool size must be at least 1")
      @Max(value = 100, message = "Maximum pool size must not exceed 100")
      int maximumSize,

      @Min(value = 0, message = "Minimum idle connections cannot be negative")
      @Max(value = 50, message = "Minimum idle connections must not exceed 50")
      int minimumIdle,

      @NotNull(message = "Connection timeout must not be null")
      Duration connectionTimeout,

      @NotNull(message = "Idle timeout must not be null")
      Duration idleTimeout,

      @NotNull(message = "Max lifetime must not be null")
      Duration maxLifetime
  ) {

    /**
     * Validates that minimum idle is not greater than maximum size.
     */
    public PoolProperties {
      if (minimumIdle > maximumSize) {
        throw new IllegalArgumentException("Minimum idle connections cannot exceed maximum pool size");
      }

      if (connectionTimeout != null && connectionTimeout.isNegative()) {
        throw new IllegalArgumentException("Connection timeout cannot be negative");
      }

      if (idleTimeout != null && idleTimeout.isNegative()) {
        throw new IllegalArgumentException("Idle timeout cannot be negative");
      }

      if (maxLifetime != null && maxLifetime.isNegative()) {
        throw new IllegalArgumentException("Max lifetime cannot be negative");
      }
    }
  }

  /**
   * Database migration configuration using Liquibase.
   */
  public record MigrationProperties(
      boolean enabled,

      @NotBlank(message = "Migration contexts must not be blank")
      String contexts,

      boolean validateOnMigrate
  ) {

  }

  /**
   * Validates database URL format.
   */
  public DatabaseConfigurationProperties {
    if (url != null && !url.startsWith("jdbc:")) {
      throw new IllegalArgumentException("Database URL must start with 'jdbc:'");
    }
  }
}