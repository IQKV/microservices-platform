package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtConfiguration Tests")
class JwtConfigurationTest {

  @Test
  @DisplayName("Should create JwtConfiguration instance")
  void shouldCreateJwtConfigurationInstance() {
    // Arrange & Act
    var config = new JwtConfiguration();

    // Assert
    assertThat(config).isNotNull();
  }

  @Test
  @DisplayName("Should create JwtDecoder with JWK set URI")
  void shouldCreateJwtDecoderWithJwkSetUri() {
    // Arrange
    var config = new JwtConfiguration();
    var jwkSetUri = "http://localhost:8080/.well-known/jwks.json";

    // Act - Using reflection to set the private field for testing
    try {
      var field = JwtConfiguration.class.getDeclaredField("jwkSetUri");
      field.setAccessible(true);
      field.set(config, jwkSetUri);

      var jwtDecoder = config.jwtDecoder();

      // Assert
      assertThat(jwtDecoder).isNotNull();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to test JwtDecoder creation", e);
    }
  }
}
