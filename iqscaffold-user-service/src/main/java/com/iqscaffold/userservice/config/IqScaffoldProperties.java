package com.iqscaffold.userservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for IQ Scaffold User Service. All custom configuration properties use the 'iqscaffold.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "iqscaffold")
@Validated
public record IqScaffoldProperties(
    @Valid @NotNull Database database,
    @Valid @NotNull Cache cache,
    @Valid @NotNull Auth auth,
    @Valid @NotNull Email email,
    @Valid @NotNull Observability observability,
    @Valid @NotNull Tenancy tenancy,
    @Valid @NotNull Liquibase liquibase,
    @NotBlank String tenantIdHeader
) {

  /**
   * Database configuration properties with iqscaffold.database prefix.
   */
  public record Database(
      @NotBlank String url,
      @NotBlank String username,
      @NotBlank String password,
      @Valid @NotNull Pool pool
  ) {

    public record Pool(
        @Min(1) @Max(100) int maximumSize,
        @Min(0) @Max(50) int minimumIdle,
        @NotNull Duration connectionTimeout,
        @NotNull Duration idleTimeout,
        @NotNull Duration maxLifetime
    ) {

    }
  }

  /**
   * Cache configuration properties with iqscaffold.cache prefix.
   */
  public record Cache(
      @Valid @NotNull IqScaffoldProperties.Cache.Redis redis
  ) {

    public record Redis(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        String password,
        @Min(0) @Max(15) int database,
        @NotNull Duration timeout,
        @Valid @NotNull Pool pool,
        @NotBlank String keyPrefix,
        @NotNull Duration defaultTtl,
        boolean enableStatistics
    ) {

      public record Pool(
          @Min(1) @Max(100) int maxActive,
          @Min(0) @Max(50) int maxIdle,
          @Min(0) @Max(25) int minIdle,
          @NotNull Duration maxWait
      ) {

      }
    }
  }

  /**
   * Authentication configuration properties with iqscaffold.auth prefix.
   */
  public record Auth(
      @Valid @NotNull Jwt jwt,
      @Valid @NotNull Security security,
      @Valid @NotNull OAuth2 oauth2
  ) {

    public record Jwt(
        @NotBlank String secretKey,
        @NotNull Duration accessTokenExpiry,
        @NotNull Duration refreshTokenExpiry,
        @NotBlank String issuer,
        @NotBlank String audience,
        @Pattern(regexp = "HS256|RS256") String algorithm
    ) {

    }

    public record Security(
        @Valid @NotNull Password password,
        @Valid @NotNull RateLimiting rateLimiting,
        @Valid @NotNull Session session
    ) {

      public record Password(
          @Min(4) @Max(20) int encoderStrength,
          boolean requireSpecialChars,
          @Min(6) @Max(128) int minLength
      ) {

      }

      public record RateLimiting(
          @Min(1) @Max(100) int loginAttempts,
          @NotNull Duration lockoutDuration
      ) {

      }

      public record Session(
          @NotNull Duration timeout,
          @Min(1) @Max(10) int concurrentSessions
      ) {

      }
    }

    public record OAuth2(
        boolean enabled,
        Map<String, OAuth2Provider> providers
    ) {

      public record OAuth2Provider(
          String clientId,
          String clientSecret,
          boolean enabled
      ) {

      }
    }
  }

  /**
   * Email configuration properties with iqscaffold.email prefix.
   */
  public record Email(
      @Valid @NotNull Smtp smtp,
      @Valid @NotNull Sender sender,
      @Valid @NotNull Verification verification,
      @Valid @NotNull Template templates
  ) {

    public record Smtp(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        String username,
        String password,
        boolean auth,
        boolean starttls,
        @NotNull Duration timeout
    ) {

    }

    public record Sender(
        @NotBlank String fromEmail,
        @NotBlank String fromName,
        @NotBlank String baseUrl
    ) {

    }

    public record Verification(
        @NotNull Duration tokenExpiry,
        @Min(1) @Max(10) int rateLimit
    ) {

    }

    public record Template(
        @NotBlank String verificationSubject,
        @NotBlank String verificationTemplate,
        @NotBlank String passwordResetSubject,
        @NotBlank String passwordResetTemplate,
        @NotBlank String passwordResetConfirmedSubject,
        @NotBlank String passwordResetConfirmedTemplate,
        @NotBlank String registrationConfirmedSubject,
        @NotBlank String registrationConfirmedTemplate
    ) {

    }

  }


  /**
   * Observability configuration properties with iqscaffold.observability prefix.
   */
  public record Observability(
      @Valid @NotNull Tracing tracing,
      @Valid @NotNull Metrics metrics,
      @Valid @NotNull Logging logging
  ) {

    public record Tracing(
        boolean enabled,
        @NotBlank String serviceName,
        @DecimalMin("0.0") @DecimalMax("1.0") double samplingRate,
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @NotNull Duration exportTimeout,
        @Positive int batchSize
    ) {

    }

    public record Metrics(
        boolean enabled,
        @NotBlank String path,
        @NotBlank String prefix,
        boolean includeHostTag,
        boolean includeApplicationTag,
        boolean includeEnvironmentTag,
        Map<String, String> customTags
    ) {

    }

    public record Logging(
        @Pattern(regexp = "DEBUG|INFO|WARN|ERROR") String level,
        @Pattern(regexp = "console|json") String format,
        boolean includeCorrelationId,
        boolean includeTraceId,
        boolean includeSpanId,
        boolean includeUserId,
        boolean includeTenantId,
        @NotBlank String correlationIdHeader,
        @NotBlank String requestIdHeader,
        @NotBlank String tenantIdHeader,
        boolean enableSqlLogging,
        boolean enableSecurityEvents,
        boolean enablePerformanceLogging
    ) {

    }
  }

  /**
   * Multi-tenancy configuration properties with iqscaffold.tenancy prefix.
   */
  public record Tenancy(
      @Valid @NotNull Schema schema
  ) {

    /**
     * Schema configuration for multi-tenant isolation.
     * Prefix is used to generate tenant schema names (e.g., tenant_acme).
     * Default is the fallback schema when no tenant context is set (typically 'public').
     */
    public record Schema(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                           message = "Schema prefix must start with lowercase letter"
                                     + " and contain only lowercase letters, numbers, and underscores") String prefix,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                           message = "Default schema must start with lowercase letter"
                                     + " and contain only lowercase letters, numbers, and underscores")
        @Name("default") String defaultSchema
    ) {

    }
  }

  /**
   * Liquibase migration configuration properties with iqscaffold.liquibase prefix.
   * System changelog runs once on startup for public schema (tenants table, etc.).
   * Tenant changelog runs per-tenant schema during tenant provisioning.
   */
  public record Liquibase(
      @NotBlank @Pattern(regexp = "^classpath:.+\\.xml$",
                         message = "System changelog must be a classpath XML file")
      String systemChangeLog,
      @NotBlank @Pattern(regexp = "^classpath:.+\\.xml$",
                         message = "Tenant changelog must be a classpath XML file")
      String tenantChangeLog
  ) {

  }
}
