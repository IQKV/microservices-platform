package org.gripday.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for User Service configuration. Validates that the application context loads successfully with all configurations.
 * 
 * Note: This test requires Redis to be running. Use docker-compose to start Redis:
 * docker-compose up -d redis
 */
@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Redis to be running - enable when Redis is available")
class UserServiceConfigTest {

  @Test
  void contextLoads() {
    // This test validates that the Spring application context loads successfully
    // with all the configuration classes and properties we've set up
  }
}