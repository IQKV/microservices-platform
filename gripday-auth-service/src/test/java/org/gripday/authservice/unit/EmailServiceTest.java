package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;

import org.gripday.authservice.config.GripdayProperties;
import org.gripday.authservice.domain.service.EmailService;
import org.gripday.authservice.domain.service.EmailVerificationMetricsService;
import org.gripday.authservice.infrastructure.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Unit tests for EmailService. Tests core email functionality including URL building and email sending.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private TemplateEngine templateEngine;

  @Mock
  private MimeMessage mimeMessage;

  private GripdayProperties gripdayProperties;
  private EmailService emailService;

  @BeforeEach
  void setUp() {
    // Create test configuration
    var smtpConfig = new GripdayProperties.Email.Smtp(
        "localhost", 587, "test@example.com", "password", true, true, java.time.Duration.ofSeconds(30)
    );
    var verificationConfig = new GripdayProperties.Email.Verification(
        "noreply@gripday.com", "Gripday Platform", "https://app.gripday.com", java.time.Duration.ofHours(24), 3
    );
    var templatesConfig = new GripdayProperties.Email.Template(
        "Verify your Gripday account", "email/verification.html",
        "Reset your password", "email/password-reset.html"
    );
    var emailConfig = new GripdayProperties.Email(smtpConfig, verificationConfig, templatesConfig);

    // Mock other required configs
    var database = mock(GripdayProperties.Database.class);
    var auth = mock(GripdayProperties.Auth.class);
    var cache = mock(GripdayProperties.Cache.class);
    var observability = mock(GripdayProperties.Observability.class);

    gripdayProperties = new GripdayProperties(database, cache, auth, emailConfig, observability);
    var metricsService = mock(EmailVerificationMetricsService.class);
    var messageService = mock(org.gripday.authservice.infrastructure.i18n.MessageService.class);
    emailService = new EmailService(mailSender, templateEngine, gripdayProperties, metricsService, messageService);
  }

  @Test
  void buildVerificationUrl_ShouldCreateCorrectUrl() {
    // Given
    var token = "test-token-123";

    // When
    var result = emailService.buildVerificationUrl(token);

    // Then
    assertEquals("https://app.gripday.com/api/v1/auth/email/verify?token=test-token-123", result);
  }

  @Test
  void buildVerificationUrl_ShouldHandleBaseUrlWithTrailingSlash() {
    // Given
    var smtpConfig = new GripdayProperties.Email.Smtp(
        "localhost", 587, "test@example.com", "password", true, true, java.time.Duration.ofSeconds(30)
    );
    var verificationConfig = new GripdayProperties.Email.Verification(
        "noreply@gripday.com", "Gripday Platform", "https://app.gripday.com/", java.time.Duration.ofHours(24), 3
    );
    var templatesConfig = new GripdayProperties.Email.Template(
        "Verify your Gripday account", "email/verification.html",
        "Reset your password", "email/password-reset.html"
    );
    var emailConfig = new GripdayProperties.Email(smtpConfig, verificationConfig, templatesConfig);

    var database = mock(GripdayProperties.Database.class);
    var auth = mock(GripdayProperties.Auth.class);
    var cache = mock(GripdayProperties.Cache.class);
    var observability = mock(GripdayProperties.Observability.class);

    var properties = new GripdayProperties(database, cache, auth, emailConfig, observability);
    var metricsService = mock(EmailVerificationMetricsService.class);
    var messageService = mock(org.gripday.authservice.infrastructure.i18n.MessageService.class);
    var service = new EmailService(mailSender, templateEngine, properties, metricsService, messageService);

    var token = "test-token-123";

    // When
    var result = service.buildVerificationUrl(token);

    // Then
    assertEquals("https://app.gripday.com/api/v1/auth/email/verify?token=test-token-123", result);
  }

  @Test
  void sendVerificationEmail_ShouldSendEmailSuccessfully() throws Exception {
    // Given
    var user = new User("testuser", "test@example.com", "hashedPassword",
        "Test", "User", "tenant1");

    var token = "verification-token-123";

    // Mock metrics service to avoid NullPointerException
    var metricsService = mock(EmailVerificationMetricsService.class);
    var timerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
    var emailTimer = mock(io.micrometer.core.instrument.Timer.class);
    when(metricsService.startEmailSendTimer()).thenReturn(timerSample);
    when(metricsService.getEmailSendTimer()).thenReturn(emailTimer);
    
    // Recreate email service with mocked metrics
    var messageService = mock(org.gripday.authservice.infrastructure.i18n.MessageService.class);
    emailService = new EmailService(mailSender, templateEngine, gripdayProperties, metricsService, messageService);

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("email/verification.html"), any(Context.class)))
        .thenReturn("<html><body>Test email content</body></html>");

    // When
    assertDoesNotThrow(() -> emailService.sendVerificationEmail(user, token));

    // Then
    verify(mailSender).createMimeMessage();
    verify(mailSender).send(mimeMessage);
    verify(templateEngine).process(eq("email/verification.html"), any(Context.class));
  }
}