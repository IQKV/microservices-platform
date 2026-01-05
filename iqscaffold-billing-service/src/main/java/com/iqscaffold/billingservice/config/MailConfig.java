package com.iqscaffold.billingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configuration for email services. Sets up JavaMailSender with SMTP configuration from IqScaffoldProperties.
 */
@Configuration
public class MailConfig {

  private final IqScaffoldProperties properties;

  public MailConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public JavaMailSender javaMailSender() {
    var mailSender = new JavaMailSenderImpl();
    var smtpConfig = properties.email().smtp();

    // Basic SMTP configuration
    mailSender.setHost(smtpConfig.host());
    mailSender.setPort(smtpConfig.port());

    // Authentication configuration
    if (smtpConfig.username() != null && !smtpConfig.username().isEmpty()) {
      mailSender.setUsername(smtpConfig.username());
    }

    if (smtpConfig.password() != null && !smtpConfig.password().isEmpty()) {
      mailSender.setPassword(smtpConfig.password());
    }

    // Mail properties
    var props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", smtpConfig.auth());
    props.put("mail.smtp.starttls.enable", smtpConfig.starttls());
    props.put("mail.debug", "false");

    // Additional SMTP properties for better compatibility
    props.put("mail.smtp.connectiontimeout", "10000");
    props.put("mail.smtp.timeout", "10000");
    props.put("mail.smtp.writetimeout", "10000");

    return mailSender;
  }
}