package org.gripday.authservice.domain.service;

import org.gripday.authservice.config.GripdayProperties;
import org.gripday.authservice.infrastructure.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

/**
 * Service for sending verification emails.
 * Uses Spring Boot Mail with SMTP integration and Thymeleaf templates.
 */
@Service
public class EmailService implements EmailOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final GripdayProperties gripdayProperties;
    
    public EmailService(JavaMailSender mailSender, 
                       TemplateEngine templateEngine,
                       GripdayProperties gripdayProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.gripdayProperties = gripdayProperties;
    }
    
    @Override
    public void sendVerificationEmail(User user, String token) {
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            var emailConfig = gripdayProperties.email();
            var verificationConfig = emailConfig.verification();
            var templatesConfig = emailConfig.templates();
            
            // Set email properties
            helper.setFrom(verificationConfig.fromEmail(), verificationConfig.fromName());
            helper.setTo(user.getEmail());
            helper.setSubject(templatesConfig.verificationSubject());
            
            // Build verification URL
            var verificationUrl = buildVerificationUrl(token);
            
            // Create template context
            var context = new Context(Locale.getDefault());
            context.setVariable("user", user);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("fromName", verificationConfig.fromName());
            
            // Process HTML template
            var htmlContent = templateEngine.process(templatesConfig.verificationTemplate(), context);
            helper.setText(htmlContent, true);
            
            // Send email
            mailSender.send(mimeMessage);
            
            logger.info("Verification email sent successfully to user: {} ({})", 
                       user.getUsername(), user.getEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to create verification email for user: {} ({})", 
                        user.getUsername(), user.getEmail(), e);
            throw new EmailServiceException("Failed to create verification email", e);
        } catch (MailException e) {
            logger.error("Failed to send verification email to user: {} ({})", 
                        user.getUsername(), user.getEmail(), e);
            throw new EmailServiceException("Failed to send verification email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending verification email to user: {} ({})", 
                        user.getUsername(), user.getEmail(), e);
            throw new EmailServiceException("Unexpected error sending verification email", e);
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
     * Custom exception for email service errors.
     */
    public static class EmailServiceException extends RuntimeException {
        public EmailServiceException(String message) {
            super(message);
        }
        
        public EmailServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}