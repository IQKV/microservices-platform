package com.iqscaffold.contactservice.config;

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
    @NestedConfigurationProperty CrmProperties crm
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

  public record CrmProperties(
      @NestedConfigurationProperty SecurityProperties security,
      @NestedConfigurationProperty ContactProperties contact,
      @NestedConfigurationProperty CompanyProperties company,
      @NestedConfigurationProperty ActivityProperties activity
  ) {

    public record SecurityProperties(
        @NestedConfigurationProperty JwtProperties jwt
    ) {
      public record JwtProperties(
          String jwkSetUri,
          String issuer
      ) {}
    }

    public record ContactProperties(
        boolean enableLeadScoring,
        boolean enableActivityTracking,
        boolean enableEmailIntegration,
        int defaultLeadScore,
        int maxLeadScore
    ) {}

    public record CompanyProperties(
        boolean enableCompanyHierarchy,
        boolean enableCompanyScoring
    ) {}

    public record ActivityProperties(
        boolean enableAutoLogging,
        int retentionDays,
        int maxActivitiesPerContact
    ) {}
  }
}