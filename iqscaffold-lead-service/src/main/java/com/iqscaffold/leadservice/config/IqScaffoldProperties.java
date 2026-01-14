package com.iqscaffold.leadservice.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "iqscaffold")
public record IqScaffoldProperties(
    String tenantIdHeader,
    String userServiceUrl,
    @NestedConfigurationProperty I18nProperties i18n,
    @NestedConfigurationProperty LiquibaseProperties liquibase,
    @NestedConfigurationProperty LeadProperties lead
) {

  public record I18nProperties(
      List<String> supportedLocales,
      String defaultLocale,
      String messageBasename,
      Duration messageCacheDuration,
      boolean fallbackToSystemLocale,
      boolean useCodeAsDefaultMessage
  ) {}

  public record LiquibaseProperties(
      String systemChangeLog,
      String tenantChangeLog
  ) {}

  public record LeadProperties(
      @NestedConfigurationProperty SecurityProperties security,
      @NestedConfigurationProperty ScoringProperties scoring,
      @NestedConfigurationProperty QualificationProperties qualification
  ) {

    public record SecurityProperties(
        @NestedConfigurationProperty JwtProperties jwt
    ) {
      public record JwtProperties(
          String jwkSetUri,
          String issuer
      ) {}
    }

    public record ScoringProperties(
        boolean enableAutoScoring,
        int defaultScore,
        int maxScore,
        int minQualificationScore
    ) {}

    public record QualificationProperties(
        boolean enableAutoQualification,
        boolean requireEmail,
        boolean requirePhone,
        boolean requireCompany
    ) {}
  }
}
