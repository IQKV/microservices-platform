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
    private final EmailVerificationMetricsService metricsService;
    
    public EmailService(JavaMailSender mailSender, 
                       TemplateEngine templateEngine,
                       GripdayProperties gripdayProperties,
                       EmailVerificationMetricsService metricsService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.gripdayProperties = gripdayProperties;
        this.metricsService = metricsService;
    }
    
    @Override
    public void sendVerificationEmail(User user, String token) {
        var timerSample = metricsService.startEmailSendTimer();
        
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
            
            // Record successful send
            metricsService.recordEmailSent();
            
            logger.info("Verification email sent successfully to user: {} ({})", 
                       user.getUsername(), user.getEmail());
            
        } catch (MessagingException e) {
            metricsService.recordEmailSendFailed();
            logger.error("Failed to create verification email for user: {} ({})", 
                        user.getUsername(), user.getEmail(), e);
            throw new EmailServiceException("Failed to create verification email", e);
        } catch (MailException e) {
            metricsService.recordEmailSendFailed();
            logger.error("Failed to send verification email to user: {} ({})", 
                        user.getUsername(), user.getEmail(), e);
            throw new EmailServiceException("Failed to send verification email", e);
        } catch (Exception e) {
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

            helper.setFrom(verificationConfig.fromEmail(), verificationConfig.fromName());
            helper.setTo(user.getEmail());
            var templatesConfig = emailConfig.templates();
            var subject = templatesConfig.passwordResetSubject() != null && !templatesConfig.passwordResetSubject().isBlank()
                ? templatesConfig.passwordResetSubject()
                : "Password reset request";
            helper.setSubject(subject);

            var resetUrl = buildPasswordResetUrl(token);
            // Use Thymeleaf template for HTML email rendering
            var context = new Context(Locale.getDefault());
            context.setVariable("user", user);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("fromName", verificationConfig.fromName());

            var templateName = templatesConfig.passwordResetTemplate() != null && !templatesConfig.passwordResetTemplate().isBlank()
                ? templatesConfig.passwordResetTemplate()
                : "password-reset";
            var htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new EmailServiceException("Failed to send password reset email", e);
        }
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
        public EmailServiceException(String message) {
            super(message);
        }
        
        public EmailServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}