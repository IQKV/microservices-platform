package org.gripday.gatewayservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitExceededException Tests")
class RateLimitExceededExceptionTest {

  @Test
  @DisplayName("Should create exception with global rate limit type")
  void shouldCreateExceptionWithGlobalRateLimitType() {
    // Arrange
    var tenantId = "tenant-123";
    var path = "/api/v1/users";
    var type = RateLimitExceededException.RateLimitType.GLOBAL;
    var retryAfter = 60;

    // Act
    var exception = new RateLimitExceededException(tenantId, path, type, retryAfter);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exception.getReason()).isEqualTo("Global rate limit exceeded");
    assertThat(exception.getTenantId()).isEqualTo(tenantId);
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfter);
  }

  @Test
  @DisplayName("Should create exception with tenant rate limit type")
  void shouldCreateExceptionWithTenantRateLimitType() {
    // Arrange
    var tenantId = "tenant-456";
    var path = "/api/v1/books";
    var type = RateLimitExceededException.RateLimitType.TENANT;
    var retryAfter = 120;

    // Act
    var exception = new RateLimitExceededException(tenantId, path, type, retryAfter);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exception.getReason()).contains("Tenant rate limit exceeded");
    assertThat(exception.getReason()).contains(tenantId);
    assertThat(exception.getTenantId()).isEqualTo(tenantId);
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfter);
  }

  @Test
  @DisplayName("Should create exception with endpoint rate limit type")
  void shouldCreateExceptionWithEndpointRateLimitType() {
    // Arrange
    var tenantId = "tenant-789";
    var path = "/api/v1/auth/login";
    var type = RateLimitExceededException.RateLimitType.ENDPOINT;
    var retryAfter = 30;

    // Act
    var exception = new RateLimitExceededException(tenantId, path, type, retryAfter);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exception.getReason()).contains("Endpoint rate limit exceeded");
    assertThat(exception.getReason()).contains(tenantId);
    assertThat(exception.getTenantId()).isEqualTo(tenantId);
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfter);
  }

  @Test
  @DisplayName("Should handle null tenant ID gracefully")
  void shouldHandleNullTenantIdGracefully() {
    // Arrange
    String tenantId = null;
    var path = "/api/v1/public";
    var type = RateLimitExceededException.RateLimitType.GLOBAL;
    var retryAfter = 60;

    // Act
    var exception = new RateLimitExceededException(tenantId, path, type, retryAfter);

    // Assert
    assertThat(exception.getTenantId()).isNull();
    assertThat(exception.getPath()).isEqualTo(path);
    assertThat(exception.getType()).isEqualTo(type);
  }
}
