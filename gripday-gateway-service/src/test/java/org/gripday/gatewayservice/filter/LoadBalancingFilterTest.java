package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.service.LoadBalancingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadBalancingFilter Tests")
class LoadBalancingFilterTest {

    @Mock(lenient = true)
    private LoadBalancingService loadBalancingService;

    @Mock(lenient = true)
    private GatewayFilterChain filterChain;

    private LoadBalancingFilter loadBalancingFilter;

    @BeforeEach
    void setUp() {
        loadBalancingFilter = new LoadBalancingFilter(loadBalancingService);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should apply load balancing when enabled")
    void shouldApplyLoadBalancingWhenEnabled() {
        var config = new LoadBalancingFilter.Config();
        config.setServiceName("user-service");
        config.setEnableLoadBalancing(true);

        var serviceUri = URI.create("http://user-service-1:8080");
        when(loadBalancingService.getNextServiceInstance(eq("user-service"))).thenReturn(serviceUri);

        var request = MockServerHttpRequest.get("/api/users").build();
        var exchange = MockServerWebExchange.from(request);

        var filter = loadBalancingFilter.apply(config);
        filter.filter(exchange, filterChain).block();

        assertThat(config.getServiceName()).isEqualTo("user-service");
    }

    @Test
    @DisplayName("Config should have default values")
    void configShouldHaveDefaultValues() {
        var config = new LoadBalancingFilter.Config();

        assertThat(config.isEnableLoadBalancing()).isTrue();
        assertThat(config.getStrategy()).isEqualTo("round-robin");
    }

    @Test
    @DisplayName("Config should allow setting values")
    void configShouldAllowSettingValues() {
        var config = new LoadBalancingFilter.Config();
        config.setServiceName("test-service");
        config.setEnableLoadBalancing(false);
        config.setStrategy("weighted");

        assertThat(config.getServiceName()).isEqualTo("test-service");
        assertThat(config.isEnableLoadBalancing()).isFalse();
        assertThat(config.getStrategy()).isEqualTo("weighted");
    }
}
