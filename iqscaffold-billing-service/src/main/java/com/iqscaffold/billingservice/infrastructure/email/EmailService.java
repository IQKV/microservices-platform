package com.iqscaffold.billingservice.infrastructure.email;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final SpringTemplateEngine templateEngine;
  private final IqScaffoldProperties iqScaffoldProperties;
  private final MessageSource messageSource;

  public EmailService(
      JavaMailSender mailSender,
      SpringTemplateEngine templateEngine,
      IqScaffoldProperties iqScaffoldProperties,
      MessageSource messageSource
  ) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.iqScaffoldProperties = iqScaffoldProperties;
    this.messageSource = messageSource;
  }

  @Async
  public void sendEmail(String to, String subjectKey, String templateName, Map<String, Object> variables, Locale locale) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
          message,
          MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
          StandardCharsets.UTF_8.name()
      );

      Context context = new Context(locale);
      context.setVariables(variables);
      
      String html = templateEngine.process("email/" + templateName, context);
      
      String subject = messageSource.getMessage(subjectKey, null, subjectKey, locale);

      helper.setTo(to);
      assert subject != null;
      helper.setSubject(subject);
      helper.setText(html, true);
      helper.setFrom(iqScaffoldProperties.email().sender().fromEmail(), iqScaffoldProperties.email().sender().fromName());

      mailSender.send(message);
    } catch (MessagingException | UnsupportedEncodingException e) {
      // proper logging would go here
      throw new RuntimeException("Failed to send email to " + to, e);
    }
  }
}
