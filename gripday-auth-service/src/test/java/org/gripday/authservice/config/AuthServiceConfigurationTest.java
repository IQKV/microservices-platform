package org.gripday.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for Auth Service configuration.
 * Validates that the application context loads successfully with all configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceConfigurationTest {

    @Test
    void contextLoads() {
        // This test validates that the Spring application context loads successfully
        // with all the configuration classes and properties we've set up
    }
}