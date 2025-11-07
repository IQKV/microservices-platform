package org.gripday.authservice.unit;

import org.gripday.authservice.infrastructure.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for email template rendering.
 * Tests that email templates render correctly with user data.
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailTemplateRenderingTest {

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // For testing

        templateEngine = new SpringTemplateEngine();
        ((SpringTemplateEngine) templateEngine).setTemplateResolver(templateResolver);
    }

    @Test
    void shouldRenderVerificationEmailTemplate() {
        // Given
        var user = createTestUser();
        var verificationUrl = "https://test.pynity.com/verify?token=test-token-123";
        var fromName = "Gripday Platform";

        var context = new Context(Locale.getDefault());
        context.setVariable("user", user);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("fromName", fromName);

        // When
        var htmlContent = templateEngine.process("email/verification", context);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).isNotEmpty();
        
        // Verify user data is included
        assertThat(htmlContent).contains("John");
        assertThat(htmlContent).contains("testuser");
        
        // Verify verification URL is included
        assertThat(htmlContent).contains(verificationUrl);
        
        // Verify from name is included
        assertThat(htmlContent).contains(fromName);
        
        // Verify basic HTML structure
        assertThat(htmlContent).contains("<html");
        assertThat(htmlContent).contains("</html>");
        assertThat(htmlContent).contains("<body");
        assertThat(htmlContent).contains("</body>");
        
        // Verify email-specific content
        assertThat(htmlContent).contains("verify");
        assertThat(htmlContent).contains("account");
    }

    @Test
    void shouldHandleUserWithNullFirstName() {
        // Given
        var user = createTestUser();
        user.setFirstName(null);
        
        var verificationUrl = "https://test.pynity.com/verify?token=test-token-123";
        var fromName = "Gripday Platform";

        var context = new Context(Locale.getDefault());
        context.setVariable("user", user);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("fromName", fromName);

        // When
        var htmlContent = templateEngine.process("email/verification", context);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).isNotEmpty();
        
        // Should still contain username
        assertThat(htmlContent).contains("testuser");
        
        // Should contain verification URL
        assertThat(htmlContent).contains(verificationUrl);
    }

    @Test
    void shouldHandleSpecialCharactersInUserData() {
        // Given
        var user = createTestUser();
        user.setFirstName("José");
        user.setLastName("García-López");
        user.setUsername("jose.garcia@example.com");
        
        var verificationUrl = "https://test.pynity.com/verify?token=test-token-123&user=josé";
        var fromName = "Gripday Platform";

        var context = new Context(Locale.getDefault());
        context.setVariable("user", user);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("fromName", fromName);

        // When
        var htmlContent = templateEngine.process("email/verification", context);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).isNotEmpty();
        
        // Should properly handle special characters
        assertThat(htmlContent).contains("José");
        assertThat(htmlContent).contains("García-López");
        
        // Should contain verification URL with special characters
        assertThat(htmlContent).contains(verificationUrl);
    }

    @Test
    void shouldRenderWithMinimalUserData() {
        // Given
        var user = new User("minimal-user", "minimal@example.com", "password-hash",
                           "Min", "User", "test-tenant");
        
        var verificationUrl = "https://test.pynity.com/verify?token=minimal-token";
        var fromName = "Test Platform";

        var context = new Context(Locale.getDefault());
        context.setVariable("user", user);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("fromName", fromName);

        // When
        var htmlContent = templateEngine.process("email/verification", context);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).isNotEmpty();
        
        // Should contain minimal user data
        assertThat(htmlContent).contains("minimal-user");
        assertThat(htmlContent).contains(verificationUrl);
        assertThat(htmlContent).contains("Test Platform");
    }

    private User createTestUser() {
        return new User("testuser", "test@example.com", "password-hash", 
                       "John", "Doe", "test-tenant");
    }
}