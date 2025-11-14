package org.gripday.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LoadBalancingService. Tests successful load balancing scenarios.
 */
class LoadBalancingServiceTest {

  private LoadBalancingService loadBalancingService;

  @BeforeEach
  void setUp() {
    loadBalancingService = new LoadBalancingService();
  }

  @Test
  void shouldReturnNextInstanceUsingRoundRobin() {
    // Given
    var serviceName = "test-service";
    var instances = List.of(
        URI.create("http://localhost:8080"),
        URI.create("http://localhost:8080")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // When
    var firstInstance = loadBalancingService.getNextServiceInstance(serviceName);
    var secondInstance = loadBalancingService.getNextServiceInstance(serviceName);
    var thirdInstance = loadBalancingService.getNextServiceInstance(serviceName);

    // Then
    assertThat(firstInstance).isEqualTo(URI.create("http://localhost:8080"));
    assertThat(secondInstance).isEqualTo(URI.create("http://localhost:8080"));
    assertThat(thirdInstance).isEqualTo(URI.create("http://localhost:8080")); // Round-robin back to first
  }

  @Test
  void shouldReturnNullWhenNoInstancesAvailable() {
    // Given
    var serviceName = "non-existent-service";

    // When
    var instance = loadBalancingService.getNextServiceInstance(serviceName);

    // Then
    assertThat(instance).isNull();
  }

  @Test
  void shouldSkipUnhealthyInstances() {
    // Given
    var serviceName = "test-service";
    var instances = List.of(
        URI.create("http://localhost:8080"),
        URI.create("http://localhost:8080")
    );
    loadBalancingService.registerServiceInstances(serviceName, instances);

    // Mark first instance as unhealthy
    loadBalancingService.markInstanceUnhealthy(serviceName, URI.create("http://localhost:8080"));

    // When
    var firstInstance = loadBalancingService.getNextServiceInstance(serviceName);
    var secondInstance = loadBalancingService.getNextServiceInstance(serviceName);

    // Then
    assertThat(firstInstance).isEqualTo(URI.create("http://localhost:8080"));
    assertThat(secondInstance).isEqualTo(URI.create("http://localhost:8080"));
  }

  @Test
  void shouldRegisterServiceInstancesSuccessfully() {
    // Given
    var serviceName = "user-service";
    var instances = List.of(URI.create("http://localhost:8080"));

    // When
    loadBalancingService.registerServiceInstances(serviceName, instances);
    var selectedInstance = loadBalancingService.getNextServiceInstance(serviceName);

    // Then
    assertThat(selectedInstance).isEqualTo(URI.create("http://localhost:8080"));
  }
}