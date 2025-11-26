package com.iqscaffold.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LoggingConfiguration Tests")
class LoggingConfigurationTest {

  @Autowired
  private Environment environment;

  @Autowired
  private LoggingConfiguration loggingConfiguration;

  @Test
  @DisplayName("Should create LoggingConfiguration bean")
  void shouldCreateLoggingConfigurationBean() {
    // Assert
    assertThat(loggingConfiguration).isNotNull();
  }

  @Test
  @DisplayName("Should have test profile active")
  void shouldHaveTestProfileActive() {
    // Assert
    assertThat(environment.getActiveProfiles()).contains("test");
  }
}
