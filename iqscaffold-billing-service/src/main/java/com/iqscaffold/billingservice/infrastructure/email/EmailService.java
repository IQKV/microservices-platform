package com.iqscaffold.billingservice.infrastructure.email;

import com.iqscaffold.billingservice.config.BillingProperties;
import com.iqscaffold.billingservice.shared.MessageService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails using templates and i18n.
 * 
 * <p>Features:
 * <ul>
 *   <li>Thymeleaf template rendering with i18n support</li>
 *   <li>Integration with external Email Service</li>
 *   <li>Circuit breaker for resilience</li>
 *   <li>Automatic locale resolution</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final MessageService messageService;
    private final BillingProperties billingProperties;
    private final RestTemplate restTemplate;

    /**
     * Sends an email using a template with i18n support.
     * 
     * @param recipient email address of the recipient
     * @param templateName name of the Thymeleaf template (without .html extension)
     * @param templateData data to be used in the template
     * @param locale locale for i18n messages
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    public void sendEmail(
        String recipient,
        String templateName,
        Map<String, Object> templateData,
        Locale locale
    ) {
        log.info("Sending email: recipient={}, template={}, locale={}", 
            recipient, templateName, locale);

        try {
            // Render HTML content using Thymeleaf
            var htmlContent = renderTemplate(templateName, templateData, locale);
            
            // Get subject from i18n messages
            var subject = getSubject(templateName, templateData, locale);
            
            // Send email via Email Service API
            sendViaEmailService(recipient, subject, htmlContent);
            
            log.info("Successfully sent email: recipient={}, template={}", recipient, templateName);
            
        } catch (Exception e) {
            log.error("Failed to send email: recipient={}, template={}", 
                recipient, templateName, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * Renders a Thymeleaf template with i18n support.
     */
    private String renderTemplate(
        String templateName,
        Map<String, Object> templateData,
        Locale locale
    ) {
        var context = new Context(locale);
        context.setVariables(templateData);
        context.setVariable("locale", locale.getLanguage());
        
        // Add common URLs
        context.setVariable("supportUrl", "https://support.iqscaffold.com");
        context.setVariable("unsubscribeUrl", "https://iqscaffold.com/unsubscribe");
        
        var templatePath = "email/" + templateName;
        return templateEngine.process(templatePath, context);
    }

    /**
     * Gets the email subject from i18n messages.
     */
    private String getSubject(
        String templateName,
        Map<String, Object> templateData,
        Locale locale
    ) {
        var subjectKey = "email." + templateName.replace("_", ".") + ".subject";
        
        // Extract parameters for subject (if any)
        var params = extractSubjectParams(templateName, templateData);
        
        return messageService.getMessage(subjectKey, params, locale);
    }

    /**
     * Extracts parameters for subject line based on template name.
     */
    private Object[] extractSubjectParams(String templateName, Map<String, Object> templateData) {
        return switch (templateName) {
            case "subscription_created" -> new Object[]{templateData.get("planName")};
            case "invoice_generated" -> new Object[]{templateData.get("invoiceNumber")};
            default -> new Object[0];
        };
    }

    /**
     * Sends email via external Email Service API.
     */
    private void sendViaEmailService(String recipient, String subject, String htmlContent) {
        var emailServiceUrl = billingProperties.integration().emailService().url();
        
        var request = Map.of(
            "to", recipient,
            "subject", subject,
            "html", htmlContent,
            "from", "noreply@iqscaffold.com"
        );
        
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        var entity = new HttpEntity<>(request, headers);
        
        // TODO: Replace with actual Email Service API call
        // For now, just log the email details
        log.info("Would send email via Email Service: to={}, subject={}", recipient, subject);
        
        // In production, uncomment this:
        // restTemplate.postForEntity(emailServiceUrl + "/send", entity, Void.class);
    }

    /**
     * Fallback method when email service is unavailable.
     */
    private void sendEmailFallback(
        String recipient,
        String templateName,
        Map<String, Object> templateData,
        Locale locale,
        Exception e
    ) {
        log.error("Email service circuit breaker activated. Email not sent: recipient={}, template={}", 
            recipient, templateName, e);
        
        // In production, you might want to:
        // 1. Store failed emails in a database for retry
        // 2. Send to a dead letter queue
        // 3. Alert operations team
    }

    /**
     * Custom exception for email sending failures.
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
