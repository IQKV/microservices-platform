package org.gripday.gatewayservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("NoHealthyInstancesException Tests")
class NoHealthyInstancesExceptionTest {

  @Test
  @DisplayName("Should create exception with service name and total instances")
  void shouldCreateExceptionWithServiceNameAndTotalInstances() {
    // Arrange
    var serviceName = "user-service";
    var totalInstances = 3;

    // Act
    var exception = new NoHealthyInstancesException(serviceName, totalInstances);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getReason()).contains("No healthy instances available");
    assertThat(exception.getReason()).contains(serviceName);
    assertThat(exception.getReason()).contains(String.valueOf(totalInstances));
    assertThat(exception.getServiceName()).isEqualTo(serviceName);
    assertThat(exception.getTotalInstances()).isEqualTo(totalInstances);
  }

  @Test
  @DisplayName("Should create exception with service name only")
  void shouldCreateExceptionWithServiceNameOnly() {
    // Arrange
    var serviceName = "bookstore-service";

    // Act
    var exception = new NoHealthyInstancesException(serviceName);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getReason()).contains("No healthy instances available");
    assertThat(exception.getReason()).contains(serviceName);
    assertThat(exception.getServiceName()).isEqualTo(serviceName);
    assertThat(exception.getTotalInstances()).isZero();
  }

  @Test
  @DisplayName("Should create exception with zero total instances")
  void shouldCreateExceptionWithZeroTotalInstances() {
    // Arrange
    var serviceName = "gateway-service";
    var totalInstances = 0;

    // Act
    var exception = new NoHealthyInstancesException(serviceName, totalInstances);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getServiceName()).isEqualTo(serviceName);
    assertThat(exception.getTotalInstances()).isZero();
  }
}
