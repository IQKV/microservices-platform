package org.gripday.gatewayservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("UnsupportedApiVersionException Tests")
class UnsupportedApiVersionExceptionTest {

  @Test
  @DisplayName("Should create exception with requested and supported versions")
  void shouldCreateExceptionWithRequestedAndSupportedVersions() {
    // Arrange
    var requestedVersion = "v3";
    var supportedVersions = List.of("v1", "v2");

    // Act
    var exception = new UnsupportedApiVersionException(requestedVersion, supportedVersions);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).contains("Unsupported API version");
    assertThat(exception.getReason()).contains(requestedVersion);
    assertThat(exception.getReason()).contains("v1, v2");
    assertThat(exception.getRequestedVersion()).isEqualTo(requestedVersion);
    assertThat(exception.getSupportedVersions()).containsExactlyElementsOf(supportedVersions);
  }

  @Test
  @DisplayName("Should create exception with single supported version")
  void shouldCreateExceptionWithSingleSupportedVersion() {
    // Arrange
    var requestedVersion = "v5";
    var supportedVersions = List.of("v1");

    // Act
    var exception = new UnsupportedApiVersionException(requestedVersion, supportedVersions);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).contains(requestedVersion);
    assertThat(exception.getReason()).contains("v1");
    assertThat(exception.getRequestedVersion()).isEqualTo(requestedVersion);
    assertThat(exception.getSupportedVersions()).hasSize(1);
  }

  @Test
  @DisplayName("Should create exception with empty supported versions list")
  void shouldCreateExceptionWithEmptySupportedVersionsList() {
    // Arrange
    var requestedVersion = "v10";
    var supportedVersions = List.<String>of();

    // Act
    var exception = new UnsupportedApiVersionException(requestedVersion, supportedVersions);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getRequestedVersion()).isEqualTo(requestedVersion);
    assertThat(exception.getSupportedVersions()).isEmpty();
  }
}
