package org.gripday.bookstore.shared;

import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class TestJwtConfiguration {

  @Bean
  @Primary
  public JwtDecoder testJwtDecoder() {
    return mock(JwtDecoder.class);
  }

  @Bean
  @Primary
  public UserContextExtractor testUserContextExtractor() {
    return new UserContextExtractor(testJwtDecoder()) {
      @Override
      public UserContext extractFromJwt(String jwtToken) {
        // Return a mock admin user for integration tests
        return new UserContext(
            1L,
            "test-admin",
            "test@example.com",
            Set.of("ADMIN"),
            Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE"),
            "Test Department",
            "TEST_ORG",
            Map.of()
        );
      }
    };
  }
}
