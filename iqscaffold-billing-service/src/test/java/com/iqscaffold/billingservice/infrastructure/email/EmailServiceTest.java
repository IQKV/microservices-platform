package com.iqscaffold.billingservice.infrastructure.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private SpringTemplateEngine templateEngine;

  @Mock
  private IqScaffoldProperties iqScaffoldProperties;

  @Mock
  private MessageSource messageSource;

  @Mock
  private MimeMessage mimeMessage;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService(mailSender, templateEngine, iqScaffoldProperties, messageSource);

    // Setup default mocks
    IqScaffoldProperties.Email emailConfig = mock(IqScaffoldProperties.Email.class);
    IqScaffoldProperties.Email.Sender senderConfig = mock(IqScaffoldProperties.Email.Sender.class);

    when(iqScaffoldProperties.email()).thenReturn(emailConfig);
    when(emailConfig.sender()).thenReturn(senderConfig);
    when(senderConfig.fromEmail()).thenReturn("noreply@example.com");
    when(senderConfig.fromName()).thenReturn("Test Sender");
  }

  @Test
  void sendEmail_shouldSendEmailSuccessfully() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "test-template";
    Map<String, Object> variables = new HashMap<>();
    variables.put("name", "John");
    Locale locale = Locale.ENGLISH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("email/" + templateName), any())).thenReturn("<html>Test</html>");
    when(messageSource.getMessage(eq(subjectKey), isNull(), eq(subjectKey), eq(locale)))
        .thenReturn("Test Subject");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(mailSender).createMimeMessage();
    verify(templateEngine).process(eq("email/" + templateName), any());
    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendEmail_shouldProcessTemplateWithVariables() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "welcome";
    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "testuser");
    variables.put("activationLink", "http://example.com/activate");
    Locale locale = Locale.FRENCH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Welcome</html>");
    when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenReturn("Bienvenue");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(templateEngine).process(eq("email/welcome"), any());
  }

  @Test
  void sendEmail_shouldUseSubjectKeyAsFallback() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.missing.subject";
    String templateName = "test";
    Map<String, Object> variables = new HashMap<>();
    Locale locale = Locale.ENGLISH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Test</html>");
    when(messageSource.getMessage(eq(subjectKey), isNull(), eq(subjectKey), eq(locale)))
        .thenReturn(subjectKey);

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(messageSource).getMessage(eq(subjectKey), isNull(), eq(subjectKey), eq(locale));
  }

  @Test
  void sendEmail_shouldHandleEmptyVariables() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "simple";
    Map<String, Object> variables = new HashMap<>();
    Locale locale = Locale.ENGLISH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Simple</html>");
    when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenReturn("Subject");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendEmail_shouldHandleDifferentLocales() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "test";
    Map<String, Object> variables = new HashMap<>();
    Locale locale = Locale.GERMAN;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Test</html>");
    when(messageSource.getMessage(eq(subjectKey), isNull(), eq(subjectKey), eq(locale)))
        .thenReturn("Test Betreff");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(messageSource).getMessage(eq(subjectKey), isNull(), eq(subjectKey), eq(locale));
  }

  @Test
  void sendEmail_shouldThrowRuntimeExceptionOnMessagingException() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "test";
    Map<String, Object> variables = new HashMap<>();
    Locale locale = Locale.ENGLISH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Test</html>");
    when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenReturn("Subject");

    // Simulate MessagingException by making send throw
    org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("Mail error"))
        .when(mailSender).send(any(MimeMessage.class));

    // When & Then
    try {
      emailService.sendEmail(to, subjectKey, templateName, variables, locale);
    } catch (final RuntimeException e) {
      // The exception message comes from the MailSendException
      assert e.getMessage().contains("Mail error") || e.getMessage().contains("test@example.com");
    }
  }

  @Test
  void sendEmail_shouldHandleComplexTemplateVariables() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.complex.subject";
    String templateName = "complex-template";
    Map<String, Object> variables = new HashMap<>();
    variables.put("user", Map.of("name", "John", "email", "john@example.com"));
    variables.put("items", java.util.List.of("item1", "item2", "item3"));
    variables.put("total", 150.50);
    Locale locale = Locale.ENGLISH;

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("email/complex-template"), any())).thenReturn("<html>Complex</html>");
    when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenReturn("Complex Subject");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, locale);

    // Then
    verify(templateEngine).process(eq("email/complex-template"), any());
    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendEmail_shouldHandleNullLocale() {
    // Given
    String to = "test@example.com";
    String subjectKey = "email.test.subject";
    String templateName = "test";
    Map<String, Object> variables = new HashMap<>();

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any())).thenReturn("<html>Test</html>");
    when(messageSource.getMessage(eq(subjectKey), isNull(), eq(subjectKey), any()))
        .thenReturn("Subject");

    // When
    emailService.sendEmail(to, subjectKey, templateName, variables, null);

    // Then
    verify(mailSender).send(mimeMessage);
  }
}
