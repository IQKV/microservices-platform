package org.gripday.gatewayservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("MissingTenantContextException Tests")
class MissingTenantContextExceptionTest {

  @Test
  @DisplayName("Should create exception with path and reason")
  void shouldCreateExceptionWithPathAndReason() {
    // Arrange
    var path = "/api/v1/tenants/data";
    var reason = "Tenant ID is required for this endpoint";

    // Act
    var exception = new MissingTenantContextException(path, reason);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).contains("Missing tenant context");
    assertThat(exception.getReason()).contains(path);
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
  }

  @Test
  @DisplayName("Should create exception with path only using default reason")
  void shouldCreateExceptionWithPathOnlyUsingDefaultReason() {
    // Arrange
    var path = "/api/v1/admin/users";

    // Act
    var exception = new MissingTenantContextException(path);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).contains("Missing tenant context");
    assertThat(exception.getReason()).contains(path);
    assertThat(exception.getReason()).contains("X-Tenant-ID header");
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getReasonDetail()).contains("X-Tenant-ID header");
  }

  @Test
  @DisplayName("Should create exception with custom reason message")
  void shouldCreateExceptionWithCustomReasonMessage() {
    // Arrange
    var path = "/api/v1/bookstore/books";
    var reason = "Multi-tenant endpoint requires tenant identification";

    // Act
    var exception = new MissingTenantContextException(path, reason);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
  }
}
