package org.gripday.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gateway filter for transforming outgoing responses before returning to clients.
 * Handles header enrichment, security headers, and response modification.
 */
@Component
public class ResponseTransformationFilter extends AbstractGatewayFilterFactory<ResponseTransformationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ResponseTransformationFilter.class);

    public ResponseTransformationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                var response = exchange.getResponse();
                var correlationId = MDC.get("correlationId");
                var requestId = MDC.get("requestId");

                // Add security headers
                addSecurityHeaders(response);
                
                // Add correlation and request tracking headers
                if (correlationId != null) {
                    response.getHeaders().set("X-Correlation-ID", correlationId);
                }
                
                if (requestId != null) {
                    response.getHeaders().set("X-Request-ID", requestId);
                }
                
                // Add gateway identification
                response.getHeaders().set("X-Gateway-Service", "gripday-gateway");
                response.getHeaders().set("X-Response-Source", "gateway");
                
                // Add response timestamp for monitoring
                response.getHeaders().set("X-Response-Timestamp", String.valueOf(System.currentTimeMillis()));
                
                // Remove internal headers that shouldn't be exposed
                removeInternalHeaders(response);
                
                logger.debug("Response headers enriched for status: {}", response.getStatusCode());
            }));
        };
    }

    /**
     * Add security headers to the response.
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        var headers = response.getHeaders();
        
        // Content Security Policy
        headers.set("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
        
        // X-Frame-Options to prevent clickjacking
        headers.set("X-Frame-Options", "DENY");
        
        // X-Content-Type-Options to prevent MIME sniffing
        headers.set("X-Content-Type-Options", "nosniff");
        
        // X-XSS-Protection
        headers.set("X-XSS-Protection", "1; mode=block");
        
        // Referrer Policy
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Strict Transport Security (HTTPS only)
        if (isHttpsRequest()) {
            headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        
        // Cache control for sensitive endpoints
        if (isSensitiveEndpoint()) {
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");
        }
    }

    /**
     * Remove internal headers that shouldn't be exposed to clients.
     */
    private void removeInternalHeaders(ServerHttpResponse response) {
        var headers = response.getHeaders();
        
        // Remove internal service headers
        headers.remove("X-Internal-Service");
        headers.remove("X-Internal-Version");
        headers.remove("X-Database-Query-Time");
        headers.remove("X-Cache-Status-Internal");
        
        // Remove server information for security
        headers.remove("Server");
        headers.remove("X-Powered-By");
    }

    /**
     * Check if the current request is over HTTPS.
     */
    private boolean isHttpsRequest() {
        // In a real implementation, this would check the actual request scheme
        // For now, return false as we're in development mode
        return false;
    }

    /**
     * Check if the current endpoint is sensitive and requires strict caching headers.
     */
    private boolean isSensitiveEndpoint() {
        // Check if the endpoint contains sensitive data
        var path = MDC.get("requestPath");
        if (path != null) {
            return path.contains("/auth/") || 
                   path.contains("/users/") || 
                   path.contains("/admin/");
        }
        return false;
    }

    /**
     * Configuration class for response transformation filter.
     */
    public static class Config {
        private boolean enableSecurityHeaders = true;
        private boolean enableCorrelationHeaders = true;
        private boolean removeInternalHeaders = true;
        private List<String> additionalHeadersToRemove = List.of();
        private Map<String, String> additionalHeaders = Map.of();

        public boolean isEnableSecurityHeaders() {
            return enableSecurityHeaders;
        }

        public void setEnableSecurityHeaders(boolean enableSecurityHeaders) {
            this.enableSecurityHeaders = enableSecurityHeaders;
        }

        public boolean isEnableCorrelationHeaders() {
            return enableCorrelationHeaders;
        }

        public void setEnableCorrelationHeaders(boolean enableCorrelationHeaders) {
            this.enableCorrelationHeaders = enableCorrelationHeaders;
        }

        public boolean isRemoveInternalHeaders() {
            return removeInternalHeaders;
        }

        public void setRemoveInternalHeaders(boolean removeInternalHeaders) {
            this.removeInternalHeaders = removeInternalHeaders;
        }

        public List<String> getAdditionalHeadersToRemove() {
            return additionalHeadersToRemove;
        }

        public void setAdditionalHeadersToRemove(List<String> additionalHeadersToRemove) {
            this.additionalHeadersToRemove = additionalHeadersToRemove;
        }

        public Map<String, String> getAdditionalHeaders() {
            return additionalHeaders;
        }

        public void setAdditionalHeaders(Map<String, String> additionalHeaders) {
            this.additionalHeaders = additionalHeaders;
        }
    }
}