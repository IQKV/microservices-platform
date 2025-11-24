package org.gripday.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import org.gripday.gatewayservice.exception.NoHealthyInstancesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoadBalancingService Tests")
class LoadBalancingServiceTest {

  private LoadBalancingService loadBalancingService;

  @BeforeEach
  void setUp() {
    loadBalancingService = new LoadBalancingService();
  }

  @Test
  @DisplayName("Should throw NoHealthyInstancesException when no instances registered")
  void shouldThrowExceptionWhenNoInstancesRegistered() {
    // Arrange
    var serviceName = "user-service";

    // Act & Assert
    assertThatThrownBy(() -> loadBalancingService.getNextServiceInstance(serviceName))
        .isInstanceOf(NoHealthyInstancesException.class)
        .hasMessageContaining("No healthy instances available")
        .hasMessageContaining(serviceName)
        .satisfies(ex -> {
          var exception = (NoHealthyInstancesException) ex;
          assertThat(exception.getServiceName()).isEqualTo(serviceName);
          assertThat(exception.getTotalInstances()).isZero();
        });
  }

  @Test
  @DisplayName("Should return instance using round-robin strategy")
  void shouldReturnInstanceUsingRoundRobinStrategy() {
    // Arrange
    var serviceName = "user-service";
    var instances = List.of(
        URI.create("http://localhost:8081"),
        URI.create("http://localhost:8082"),
        URI.create("http://localhost:8083")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Act
    var instance1 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance2 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance3 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance4 = loadBalancingService.getNextServiceInstance(serviceName);

    // Assert
    assertThat(instance1).isEqualTo(instances.get(0));
    assertThat(instance2).isEqualTo(instances.get(1));
    assertThat(instance3).isEqualTo(instances.get(2));
    assertThat(instance4).isEqualTo(instances.get(0)); // Round-robin back to first
  }

  @Test
  @DisplayName("Should skip unhealthy instances")
  void shouldSkipUnhealthyInstances() {
    // Arrange
    var serviceName = "bookstore-service";
    var instances = List.of(
        URI.create("http://localhost:9001"),
        URI.create("http://localhost:9002"),
        URI.create("http://localhost:9003")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Mark second instance as unhealthy
    loadBalancingService.markInstanceUnhealthy(serviceName, instances.get(1));

    // Act
    var instance1 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance2 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance3 = loadBalancingService.getNextServiceInstance(serviceName);

    // Assert
    assertThat(instance1).isEqualTo(instances.get(0));
    assertThat(instance2).isEqualTo(instances.get(2)); // Skips unhealthy instance
    assertThat(instance3).isEqualTo(instances.get(0)); // Round-robin continues
  }

  @Test
  @DisplayName("Should throw NoHealthyInstancesException when all instances unhealthy")
  void shouldThrowExceptionWhenAllInstancesUnhealthy() {
    // Arrange
    var serviceName = "gateway-service";
    var instances = List.of(
        URI.create("http://localhost:7001"),
        URI.create("http://localhost:7002")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Mark all instances as unhealthy
    loadBalancingService.markInstanceUnhealthy(serviceName, instances.get(0));
    loadBalancingService.markInstanceUnhealthy(serviceName, instances.get(1));

    // Act & Assert
    assertThatThrownBy(() -> loadBalancingService.getNextServiceInstance(serviceName))
        .isInstanceOf(NoHealthyInstancesException.class)
        .hasMessageContaining("No healthy instances available")
        .satisfies(ex -> {
          var exception = (NoHealthyInstancesException) ex;
          assertThat(exception.getServiceName()).isEqualTo(serviceName);
          assertThat(exception.getTotalInstances()).isEqualTo(2);
        });
  }

  @Test
  @DisplayName("Should register service instances successfully")
  void shouldRegisterServiceInstancesSuccessfully() {
    // Arrange
    var serviceName = "user-service";
    var instances = List.of(
        URI.create("http://localhost:8081"),
        URI.create("http://localhost:8082")
    );

    // Act
    loadBalancingService.registerServiceInstances(serviceName, instances);
    var selectedInstance = loadBalancingService.getNextServiceInstance(serviceName);

    // Assert
    assertThat(selectedInstance).isIn(instances);
  }

  @Test
  @DisplayName("Should handle single instance service")
  void shouldHandleSingleInstanceService() {
    // Arrange
    var serviceName = "single-service";
    var instances = List.of(URI.create("http://localhost:5000"));
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Act
    var instance1 = loadBalancingService.getNextServiceInstance(serviceName);
    var instance2 = loadBalancingService.getNextServiceInstance(serviceName);

    // Assert
    assertThat(instance1).isEqualTo(instances.get(0));
    assertThat(instance2).isEqualTo(instances.get(0)); // Same instance
  }

  @Test
  @DisplayName("Should mark instance as unhealthy")
  void shouldMarkInstanceAsUnhealthy() {
    // Arrange
    var serviceName = "test-service";
    var instances = List.of(
        URI.create("http://localhost:6001"),
        URI.create("http://localhost:6002")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Act
    loadBalancingService.markInstanceUnhealthy(serviceName, instances.get(0));
    var selectedInstance = loadBalancingService.getNextServiceInstance(serviceName);

    // Assert
    assertThat(selectedInstance).isEqualTo(instances.get(1)); // Only healthy instance
  }
}
