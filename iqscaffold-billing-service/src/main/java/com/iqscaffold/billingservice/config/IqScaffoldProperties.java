package com.iqscaffold.billingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for IQ Scaffold Billing Service. All custom configuration properties use the 'iqscaffold.' prefix for clear namespace separation.
 */
@ConfigurationProperties(prefix = "iqscaffold")
@Validated
public record IqScaffoldProperties(
    @Valid @NotNull Email email,
    @Valid @NotNull I18n i18n,
    @Valid @NotNull Billing billing,
    @NotBlank String tenantIdHeader,
    @NotBlank String userServiceUrl
) {

  /**
   * Email configuration properties with iqscaffold.email prefix.
   */
  public record Email(
      @Valid @NotNull Smtp smtp,
      @Valid @NotNull Sender sender,
      @Valid @NotNull Templates templates
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

    public record Templates(
        @NotBlank String merchantOnboardingTemplate,
        @NotBlank String paymentSuccessfulTemplate,
        @NotBlank String paymentFailedTemplate,
        @NotBlank String paymentRefundedTemplate,
        @NotBlank String invoiceGeneratedTemplate
    ) {

    }
  }

  /**
   * Internationalization configuration properties with iqscaffold.i18n prefix.
   */
  public record I18n(
      @NotNull List<@Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$",
                             message = "Locale must be in format 'xx' or 'xx-XX'") String> supportedLocales,
      @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$",
                         message = "Default locale must be in format 'xx' or 'xx-XX'") String defaultLocale,
      @NotBlank String messageBasename,
      @NotNull Duration messageCacheDuration,
      boolean fallbackToSystemLocale,
      boolean useCodeAsDefaultMessage
  ) {

    public I18n {
      // Validation: default locale must be in supported locales
      if (supportedLocales != null && !supportedLocales.contains(defaultLocale)) {
        throw new IllegalArgumentException("Default locale '" + defaultLocale + "' must be included in supported locales");
      }
    }

    /**
     * Get supported locales as Locale objects.
     */
    public List<Locale> getSupportedLocaleObjects() {
      return supportedLocales.stream()
          .map(Locale::forLanguageTag)
          .toList();
    }

    /**
     * Get default locale as Locale object.
     */
    public Locale getDefaultLocaleObject() {
      return Locale.forLanguageTag(defaultLocale);
    }

    /**
     * Check if a locale is supported.
     */
    public boolean isLocaleSupported(String locale) {
      return supportedLocales.contains(locale);
    }

    /**
     * Check if a Locale object is supported.
     */
    public boolean isLocaleSupported(Locale locale) {
      return supportedLocales.contains(locale.toLanguageTag())
             || supportedLocales.contains(locale.getLanguage());
    }
  }

  /**
   * Billing-specific configuration properties with iqscaffold.billing prefix.
   */
  public record Billing(
      @Valid @NotNull Security security,
      @Valid @NotNull Payment payment,
      @Valid @NotNull Stripe stripe,
      @Valid @NotNull Notifications notifications,
      @Valid @NotNull Subscription subscription
  ) {

    public record Security(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Encryption encryption
    ) {
      public record Jwt(
          @NotBlank String jwkSetUri,
          @NotBlank String issuer
      ) {
      }

      public record Encryption(
          @NotBlank String masterKey,
          boolean useTenantSpecificConfig
      ) {
      }
    }

    public record Payment(
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "stripe|manual") String provider,
        boolean saasMode,
        @Valid @NotNull Stripe stripe
    ) {
      public record Stripe(
          @NotBlank String apiKey,
          @NotBlank String webhookSecret,
          @NotBlank String clientId // For Connect
      ) {
      }
    }

    public record Stripe(
        @NotBlank String publicKey,
        @NotBlank String secretKey,
        @NotBlank String webhookSecret,
        @NotBlank String connectClientId
    ) {

    }

    public record Notifications(
        boolean enableEmailNotifications,
        boolean enableWebhookNotifications,
        @NotNull Duration retryDelay,
        @Min(1) @Max(10) int maxRetries
    ) {

    }

    public record Subscription(
        boolean enableSubscriptions,
        @Min(0) @Max(365) int trialPeriodDays,
        @Min(0) @Max(30) int gracePeriodDays,
        @Min(1) @Max(10) int maxRetryAttempts,
        @Min(1) @Max(90) int autoCancelAfterDays,
        @NotBlank @jakarta.validation.constraints.Pattern(
            regexp = "CREATE_PRORATIONS|NONE|ALWAYS_INVOICE",
            message = "Proration behavior must be CREATE_PRORATIONS, NONE, or ALWAYS_INVOICE"
        ) String prorationBehavior,
        @Valid @NotNull SubscriptionNotifications notifications
    ) {
      public record SubscriptionNotifications(
          @Min(1) @Max(30) int trialEndingDaysNotice,
          @NotNull List<@Min(1) @Max(30) Integer> paymentRetrySchedule
      ) {
        public SubscriptionNotifications {
          // Validation: payment retry schedule must not be empty
          if (paymentRetrySchedule == null || paymentRetrySchedule.isEmpty()) {
            throw new IllegalArgumentException("Payment retry schedule must contain at least one value");
          }
          // Validation: payment retry schedule must be in ascending order
          for (int i = 1; i < paymentRetrySchedule.size(); i++) {
            if (paymentRetrySchedule.get(i) <= paymentRetrySchedule.get(i - 1)) {
              throw new IllegalArgumentException("Payment retry schedule must be in ascending order");
            }
          }
        }
      }
    }
  }
}
