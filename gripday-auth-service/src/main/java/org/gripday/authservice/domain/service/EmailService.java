package org.gripday.authservice.domain.service;

import jakarta.mail.MessagingException;
import java.util.Locale;

import org.gripday.authservice.config.GripdayProperties;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.i18n.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending verification emails. Uses Spring Boot Mail with SMTP integration and Thymeleaf templates.
 */
@Service
public class EmailService implements EmailOperations {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final GripdayProperties gripdayProperties;
  private final EmailVerificationMetricsService metricsService;
  private final MessageService messageService;

  public EmailService(final JavaMailSender mailSender,
      final TemplateEngine templateEngine,
      final GripdayProperties gripdayProperties,
      final EmailVerificationMetricsService metricsService,
      final MessageService messageService) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.gripdayProperties = gripdayProperties;
    this.metricsService = metricsService;
    this.messageService = messageService;
  }

  @Override
  public void sendVerificationEmail(User user, String token) {
    var timerSample = metricsService.startEmailSendTimer();

    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      var emailConfig = gripdayProperties.email();
      var verificationConfig = emailConfig.verification();

      // Determine user's locale
      var userLocale = getUserLocale(user);

      // Set email properties with localized subject
      helper.setFrom(verificationConfig.fromEmail(), verificationConfig.fromName());
      helper.setTo(user.getEmail());
      helper.setSubject(messageService.getMessage("email.verification.subject", userLocale));

      // Build verification URL
      var verificationUrl = buildVerificationUrl(token);

      // Create template context with localized messages
      var context = new Context(userLocale);
      context.setVariable("user", user);
      context.setVariable("verificationUrl", verificationUrl);
      context.setVariable("fromName", verificationConfig.fromName());
      context.setVariable("greeting", messageService.getMessage("email.verification.greeting", new Object[]{user.getFirstName()}, userLocale));
      context.setVariable("body", messageService.getMessage("email.verification.body", userLocale));
      context.setVariable("buttonText", messageService.getMessage("email.verification.button", userLocale));
      context.setVariable("linkText", messageService.getMessage("email.verification.link.text", userLocale));
      context.setVariable("footer", messageService.getMessage("email.verification.footer", userLocale));
      context.setVariable("regards", messageService.getMessage("email.verification.regards", userLocale));
      context.setVariable("team", messageService.getMessage("email.verification.team", userLocale));

      // Process HTML template
      var templatesConfig = emailConfig.templates();
      var htmlContent = templateEngine.process(templatesConfig.verificationTemplate(), context);
      helper.setText(htmlContent, true);

      // Send email
      mailSender.send(mimeMessage);

      // Record successful send
      metricsService.recordEmailSent();

      logger.info("Verification email sent successfully to user: {} ({}) with locale: {}",
          user.getUsername(), user.getEmail(), userLocale);

    } catch (final MessagingException e) {
      metricsService.recordEmailSendFailed();
      logger.error("Failed to create verification email for user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      throw new EmailServiceException("Failed to create verification email", e);
    } catch (final MailException e) {
      metricsService.recordEmailSendFailed();
      logger.error("Failed to send verification email to user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      throw new EmailServiceException("Failed to send verification email", e);
    } catch (final Exception e) {
      metricsService.recordEmailSendFailed();
      logger.error("Unexpected error sending verification email to user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      throw new EmailServiceException("Unexpected error sending verification email", e);
    } finally {
      timerSample.stop(metricsService.getEmailSendTimer());
    }
  }

  @Override
  public String buildVerificationUrl(String token) {
    var verificationConfig = gripdayProperties.email().verification();
    var baseUrl = verificationConfig.baseUrl();

    // Ensure base URL doesn't end with slash
    var cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

    return cleanBaseUrl + "/api/v1/auth/email/verify?token=" + token;
  }

  /**
   * Send password reset email with a reset link.
   */
  public void sendPasswordResetEmail(User user, String token) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      // Use verification fromEmail settings for outbound emails
      var emailConfig = gripdayProperties.email();
      var verificationConfig = emailConfig.verification();

      // Determine user's locale
      var userLocale = getUserLocale(user);

      helper.setFrom(verificationConfig.fromEmail(), verificationConfig.fromName());
      helper.setTo(user.getEmail());
      helper.setSubject(messageService.getMessage("email.password.reset.subject", userLocale));

      var resetUrl = buildPasswordResetUrl(token);

      // Create template context with localized messages
      var context = new Context(userLocale);
      context.setVariable("user", user);
      context.setVariable("resetUrl", resetUrl);
      context.setVariable("fromName", verificationConfig.fromName());
      context.setVariable("greeting", messageService.getMessage("email.password.reset.greeting", new Object[]{user.getFirstName()}, userLocale));
      context.setVariable("body", messageService.getMessage("email.password.reset.body", userLocale));
      context.setVariable("buttonText", messageService.getMessage("email.password.reset.button", userLocale));
      context.setVariable("linkText", messageService.getMessage("email.password.reset.link.text", userLocale));
      context.setVariable("footer", messageService.getMessage("email.password.reset.footer", userLocale));
      context.setVariable("regards", messageService.getMessage("email.password.reset.regards", userLocale));
      context.setVariable("team", messageService.getMessage("email.password.reset.team", userLocale));
      context.setVariable("expiry", messageService.getMessage("email.password.reset.expiry", userLocale));

      var templatesConfig = emailConfig.templates();
      var templateName = templatesConfig.passwordResetTemplate() != null && !templatesConfig.passwordResetTemplate().isBlank()
          ? templatesConfig.passwordResetTemplate()
          : "password-reset";
      var htmlContent = templateEngine.process(templateName, context);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      logger.info("Password reset email sent successfully to user: {} ({}) with locale: {}",
          user.getUsername(), user.getEmail(), userLocale);

    } catch (final Exception e) {
      logger.error("Failed to send password reset email to user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      throw new EmailServiceException("Failed to send password reset email", e);
    }
  }

  /**
   * Determines the user's preferred locale for email localization.
   * Falls back to English if no preference is set or if the locale is invalid.
   *
   * @param user the user
   * @return the user's locale
   */
  private Locale getUserLocale(User user) {
    if (user.getPreferredLocale() != null && !user.getPreferredLocale().isBlank()) {
      try {
        return Locale.forLanguageTag(user.getPreferredLocale());
      } catch (final Exception e) {
        logger.warn("Invalid locale '{}' for user: {}, falling back to English",
            user.getPreferredLocale(), user.getUsername());
      }
    }
    return Locale.ENGLISH;
  }

  /**
   * Build password reset URL based on base URL configuration.
   */
  public String buildPasswordResetUrl(String token) {
    var baseUrl = gripdayProperties.email().verification().baseUrl();
    var cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return cleanBaseUrl + "/reset-password?token=" + token;
  }

  /**
   * Custom exception for email service errors.
   */
  public static class EmailServiceException extends RuntimeException {

    public EmailServiceException(final String message) {
      super(message);
    }

    public EmailServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}