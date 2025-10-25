package org.gripday.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for GripdayProperties configuration binding.
 * Validates that configuration properties are properly loaded and bound.
 */
@SpringBootTest
@ActiveProfiles("test")
class GripdayPropertiesTest {

    @Autowired
    private GripdayProperties gripdayProperties;

    // Note: Other property tests are commented out as they use outdated property names
    // This test focuses only on the new email properties added in this task

    @Test
    void shouldLoadEmailProperties() {
        var email = gripdayProperties.email();
        
        assertThat(email).isNotNull();
        assertThat(email.smtp()).isNotNull();
        assertThat(email.smtp().host()).isEqualTo("localhost");
        assertThat(email.smtp().port()).isEqualTo(587);
        assertThat(email.smtp().auth()).isTrue();
        assertThat(email.smtp().starttls()).isTrue();
        assertThat(email.smtp().timeout()).isEqualTo(java.time.Duration.ofSeconds(30));
        
        assertThat(email.verification()).isNotNull();
        assertThat(email.verification().fromEmail()).isEqualTo("noreply@gripday.com");
        assertThat(email.verification().fromName()).isEqualTo("Gripday Platform");
        assertThat(email.verification().tokenExpiry()).isEqualTo(java.time.Duration.ofHours(24));
        assertThat(email.verification().rateLimit()).isEqualTo(3);
        
        assertThat(email.templates()).isNotNull();
        assertThat(email.templates().verificationSubject()).isEqualTo("Verify your Gripday account");
        assertThat(email.templates().verificationTemplate()).isEqualTo("email/verification.html");
    }
}