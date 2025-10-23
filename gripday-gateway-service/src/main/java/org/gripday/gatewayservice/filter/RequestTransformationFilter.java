package org.gripday.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gateway filter for transforming incoming requests before forwarding to downstream services.
 * Handles header enrichment, user context propagation, and request modification.
 */
@Component
public class RequestTransformationFilter extends AbstractGatewayFilterFactory<RequestTransformationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RequestTransformationFilter.class);

    public RequestTransformationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            var correlationId = MDC.get("correlationId");
            var tenantId = MDC.get("tenantId");
            var userId = MDC.get("userId");

            // Build transformed request with enriched headers
            var transformedRequest = request.mutate()
                .headers(headers -> {
                    // Add correlation ID for distributed tracing
                    if (correlationId != null) {
                        headers.set("X-Correlation-ID", correlationId);
                    }
                    
                    // Add tenant context for multi-tenant support
                    if (tenantId != null) {
                        headers.set("X-Tenant-ID", tenantId);
                    }
                    
                    // Add user context for authorization
                    if (userId != null) {
                        headers.set("X-User-ID", userId);
                    }
                    
                    // Add service identification
                    headers.set("X-Gateway-Service", "gripday-gateway");
                    headers.set("X-Request-Source", "gateway");
                    
                    // Remove sensitive headers that shouldn't be forwarded
                    headers.remove("Authorization-Internal");
                    headers.remove("X-Internal-Token");
                    
                    // Add request timestamp for monitoring
                    headers.set("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));
                    
                    logger.debug("Request headers enriched for path: {}", request.getPath());
                })
                .build();

            // Continue with transformed request
            return chain.filter(exchange.mutate().request(transformedRequest).build());
        };
    }

    /**
     * Configuration class for request transformation filter.
     */
    public static class Config {
        private boolean enableHeaderEnrichment = true;
        private boolean enableUserContextPropagation = true;
        private boolean enableTenantContextPropagation = true;
        private List<String> headersToRemove = List.of("Authorization-Internal", "X-Internal-Token");
        private Map<String, String> additionalHeaders = Map.of();

        public boolean isEnableHeaderEnrichment() {
            return enableHeaderEnrichment;
        }

        public void setEnableHeaderEnrichment(boolean enableHeaderEnrichment) {
            this.enableHeaderEnrichment = enableHeaderEnrichment;
        }

        public boolean isEnableUserContextPropagation() {
            return enableUserContextPropagation;
        }

        public void setEnableUserContextPropagation(boolean enableUserContextPropagation) {
            this.enableUserContextPropagation = enableUserContextPropagation;
        }

        public boolean isEnableTenantContextPropagation() {
            return enableTenantContextPropagation;
        }

        public void setEnableTenantContextPropagation(boolean enableTenantContextPropagation) {
            this.enableTenantContextPropagation = enableTenantContextPropagation;
        }

        public List<String> getHeadersToRemove() {
            return headersToRemove;
        }

        public void setHeadersToRemove(List<String> headersToRemove) {
            this.headersToRemove = headersToRemove;
        }

        public Map<String, String> getAdditionalHeaders() {
            return additionalHeaders;
        }

        public void setAdditionalHeaders(Map<String, String> additionalHeaders) {
            this.additionalHeaders = additionalHeaders;
        }
    }
}