package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.common.GatewayConstants;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantExtractionFilter Tests")
class TenantExtractionFilterTest {

    @Mock(lenient = true)
    private GatewayFilterChain filterChain;

    private TenantExtractionFilter tenantExtractionFilter;

    @BeforeEach
    void setUp() {
        tenantExtractionFilter = new TenantExtractionFilter();
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should extract tenant ID from header")
    void shouldExtractTenantIdFromHeader() {
        var tenantId = "tenant-123";
        var request = MockServerHttpRequest.get("/api/test")
            .header(GatewayConstants.Headers.X_TENANT_ID, tenantId)
            .build();
        var exchange = MockServerWebExchange.from(request);

        tenantExtractionFilter.filter(exchange, filterChain).block();

        var tenantContext = (TenantExtractionFilter.TenantContext)
            exchange.getAttributes().get(GatewayConstants.Attributes.TENANT_CONTEXT);
        assertThat(tenantContext).isNotNull();
        assertThat(tenantContext.tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should extract tenant ID from query parameter")
    void shouldExtractTenantIdFromQueryParameter() {
        var tenantId = "tenant-456";
        var request = MockServerHttpRequest.get("/api/test?tenantId=" + tenantId).build();
        var exchange = MockServerWebExchange.from(request);

        tenantExtractionFilter.filter(exchange, filterChain).block();

        var tenantContext = (TenantExtractionFilter.TenantContext)
            exchange.getAttributes().get(GatewayConstants.Attributes.TENANT_CONTEXT);
        assertThat(tenantContext).isNotNull();
        assertThat(tenantContext.tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("TenantContext should check if present")
    void tenantContextShouldCheckIfPresent() {
        var tenantContext = new TenantExtractionFilter.TenantContext("tenant-123");
        assertThat(tenantContext.isPresent()).isTrue();

        var emptyContext = new TenantExtractionFilter.TenantContext(null);
        assertThat(emptyContext.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should have correct filter order")
    void shouldHaveCorrectFilterOrder() {
        assertThat(tenantExtractionFilter.getOrder())
            .isEqualTo(GatewayConstants.FilterOrder.TENANT_EXTRACTION_FILTER);
    }
}
