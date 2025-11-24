package org.gripday.userservice.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

import io.micrometer.core.instrument.Timer;
import org.gripday.userservice.config.GripdayProperties;
import org.gripday.userservice.emailverification.VerificationMetrics;
import org.gripday.userservice.usermanagement.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Unit tests for EmailService.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private TemplateEngine templateEngine;

  @Mock
  private GripdayProperties gripdayProperties;

  @Mock
  private VerificationMetrics metricsService;

  @Mock
  private MessageService messageService;

  @Mock
  private MimeMessage mimeMessage;

  @Mock
  private Timer.Sample timerSample;

  @Mock
  private Timer timer;

  @InjectMocks
  private EmailService emailService;

  private User testUser;
  private GripdayProperties.Email emailConfig;
  private GripdayProperties.Email.Sender senderConfig;
  private GripdayProperties.Email.Template templatesConfig;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "test@example.com", "hashedpass", "Test", "User", "tenant-123");
    testUser.setPreferredLocale("en");

    senderConfig = new GripdayProperties.Email.Sender(
        "noreply@gripday.com",
        "Gripday",
        "https://app.gripday.com"
    );

    templatesConfig = new GripdayProperties.Email.Template(
        "Verify Email",
        "email-verification",
        "Reset Password",
        "password-reset",
        "Password Reset Confirmed",
        "password-reset-confirmed",
        "Registration Confirmed",
        "registration-confirmed"
    );

    emailConfig = new GripdayProperties.Email(null, senderConfig, null, templatesConfig);
  }

  @Test
  @DisplayName("Should send verification email successfully")
  void shouldSendVerificationEmail() {
    // Arrange
    var token = "test-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email</html>");

    // Act
    emailService.sendVerificationEmail(testUser, token);

    // Assert
    verify(mailSender).send(mimeMessage);
    verify(metricsService).recordEmailSent();
    verify(timerSample).stop(any());
  }

  @Test
  @DisplayName("Should build verification URL correctly")
  void shouldBuildVerificationUrl() {
    // Arrange
    var token = "test-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);

    // Act
    var url = emailService.buildVerificationUrl(token);

    // Assert
    assertThat(url).isEqualTo("https://app.gripday.com/api/v1/auth/email/verify?token=test-token-123");
  }

  @Test
  @DisplayName("Should handle base URL with trailing slash")
  void shouldHandleBaseUrlWithTrailingSlash() {
    // Arrange
    var senderConfigWithSlash = new GripdayProperties.Email.Sender(
        "noreply@gripday.com",
        "Gripday",
        "https://app.gripday.com/"
    );
    var emailConfigWithSlash = new GripdayProperties.Email(null, senderConfigWithSlash, null, templatesConfig);
    when(gripdayProperties.email()).thenReturn(emailConfigWithSlash);
    var token = "test-token-123";

    // Act
    var url = emailService.buildVerificationUrl(token);

    // Assert
    assertThat(url).isEqualTo("https://app.gripday.com/api/v1/auth/email/verify?token=test-token-123");
  }

  @Test
  @DisplayName("Should throw exception when email sending fails")
  void shouldThrowExceptionWhenEmailSendingFails() {
    // Arrange
    var token = "test-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email</html>");
    doThrow(new MailException("SMTP error") {
    }).when(mailSender).send(any(MimeMessage.class));

    // Act & Assert
    assertThatThrownBy(() -> emailService.sendVerificationEmail(testUser, token))
        .isInstanceOf(EmailService.EmailServiceException.class)
        .hasMessageContaining("Failed to send verification email");

    verify(metricsService).recordEmailSendFailed();
    verify(timerSample).stop(any());
  }

  @Test
  @DisplayName("Should send password reset email successfully")
  void shouldSendPasswordResetEmail() {
    // Arrange
    var token = "reset-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Reset Email</html>");

    // Act
    emailService.sendPasswordResetEmail(testUser, token);

    // Assert
    verify(mailSender).send(mimeMessage);
  }

  @Test
  @DisplayName("Should build password reset URL correctly")
  void shouldBuildPasswordResetUrl() {
    // Arrange
    var token = "reset-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);

    // Act
    var url = emailService.buildPasswordResetUrl(token);

    // Assert
    assertThat(url).isEqualTo("https://app.gripday.com/reset-password?token=reset-token-123");
  }

  @Test
  @DisplayName("Should send registration confirmed email successfully")
  void shouldSendRegistrationConfirmedEmail() {
    // Arrange
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Confirmed Email</html>");

    // Act
    emailService.sendRegistrationConfirmedEmail(testUser);

    // Assert
    verify(mailSender).send(mimeMessage);
    verify(metricsService).recordEmailSent();
  }

  @Test
  @DisplayName("Should send password reset confirmed email successfully")
  void shouldSendPasswordResetConfirmedEmail() {
    // Arrange
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Reset Confirmed Email</html>");

    // Act
    emailService.sendPasswordResetConfirmedEmail(testUser);

    // Assert
    verify(mailSender).send(mimeMessage);
    verify(metricsService).recordEmailSent();
  }

  @Test
  @DisplayName("Should use English locale when user locale is invalid")
  void shouldUseEnglishLocaleWhenUserLocaleIsInvalid() {
    // Arrange
    testUser.setPreferredLocale("invalid-locale");
    var token = "test-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email</html>");

    // Act
    emailService.sendVerificationEmail(testUser, token);

    // Assert
    verify(mailSender).send(mimeMessage);
    verify(metricsService).recordEmailSent();
  }

  @Test
  @DisplayName("Should use English locale when user locale is null")
  void shouldUseEnglishLocaleWhenUserLocaleIsNull() {
    // Arrange
    testUser.setPreferredLocale(null);
    var token = "test-token-123";
    when(gripdayProperties.email()).thenReturn(emailConfig);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(timer);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("Test Message");
    when(messageService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("Test Message");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email</html>");

    // Act
    emailService.sendVerificationEmail(testUser, token);

    // Assert
    verify(mailSender).send(mimeMessage);
    verify(metricsService).recordEmailSent();
  }
}
