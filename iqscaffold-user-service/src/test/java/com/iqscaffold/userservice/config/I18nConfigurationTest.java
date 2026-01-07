package com.iqscaffold.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for I18n configuration properties.
 */
@SpringBootTest
@ActiveProfiles({"test", "i18n"})
@TestPropertySource(properties = {
    "iqscaffold.i18n.supported-locales[0]=en",
    "iqscaffold.i18n.supported-locales[1]=es", 
    "iqscaffold.i18n.supported-locales[2]=fr",
    "iqscaffold.i18n.default-locale=en",
    "iqscaffold.i18n.message-basename=i18n/messages",
    "iqscaffold.i18n.message-cache-duration=PT1H",
    "iqscaffold.i18n.fallback-to-system-locale=false",
    "iqscaffold.i18n.use-code-as-default-message=true"
})
class I18nConfigurationTest {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully with i18n configuration
        assertThat(true).isTrue();
    }
}