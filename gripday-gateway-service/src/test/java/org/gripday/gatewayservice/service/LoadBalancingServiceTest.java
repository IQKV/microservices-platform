package org.gripday.gatewayservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LoadBalancingService.
 * Tests successful load balancing scenarios.
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
            URI.create("http://localhost:8081"),
            URI.create("http://localhost:8082")
        );
        loadBalancingService.registerServiceInstances(serviceName, instances);

        // When
        var firstInstance = loadBalancingService.getNextServiceInstance(serviceName);
        var secondInstance = loadBalancingService.getNextServiceInstance(serviceName);
        var thirdInstance = loadBalancingService.getNextServiceInstance(serviceName);

        // Then
        assertThat(firstInstance).isEqualTo(URI.create("http://localhost:8081"));
        assertThat(secondInstance).isEqualTo(URI.create("http://localhost:8082"));
        assertThat(thirdInstance).isEqualTo(URI.create("http://localhost:8081")); // Round-robin back to first
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
            URI.create("http://localhost:8081"),
            URI.create("http://localhost:8082")
        );
        loadBalancingService.registerServiceInstances(serviceName, instances);
        
        // Mark first instance as unhealthy
        loadBalancingService.markInstanceUnhealthy(serviceName, URI.create("http://localhost:8081"));

        // When
        var firstInstance = loadBalancingService.getNextServiceInstance(serviceName);
        var secondInstance = loadBalancingService.getNextServiceInstance(serviceName);

        // Then
        assertThat(firstInstance).isEqualTo(URI.create("http://localhost:8082"));
        assertThat(secondInstance).isEqualTo(URI.create("http://localhost:8082"));
    }

    @Test
    void shouldRegisterServiceInstancesSuccessfully() {
        // Given
        var serviceName = "auth-service";
        var instances = List.of(URI.create("http://localhost:8081"));

        // When
        loadBalancingService.registerServiceInstances(serviceName, instances);
        var selectedInstance = loadBalancingService.getNextServiceInstance(serviceName);

        // Then
        assertThat(selectedInstance).isEqualTo(URI.create("http://localhost:8081"));
    }
}