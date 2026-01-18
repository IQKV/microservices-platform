package com.iqscaffold.gatewayservice.config.module;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("billing")
public class BillingOpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingOpenApiConfig.class);
    private final IqScaffoldProperties properties;

    public BillingOpenApiConfig(final IqScaffoldProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RouteLocator billingOpenApiRoutes(final RouteLocatorBuilder builder) {
        var routeBuilder = builder.routes();
        var serviceName = "billing-service";
        var serviceConfig = properties.gateway().routing().services().get(serviceName);

        if (serviceConfig == null || !serviceConfig.enabled()) {
            return routeBuilder.build();
        }

        var openApiConfig = serviceConfig.openapi();
        if (openApiConfig == null || !openApiConfig.enabled()) {
            return routeBuilder.build();
        }

        var contextPath = openApiConfig.contextPath();
        if (contextPath == null || contextPath.isBlank()) {
            contextPath = serviceName;
        }

        if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        }

        var serviceUri = serviceConfig.uri();
        var billingGroups = List.of("billing-payments", "billing-webhooks", "billing-admin");

        for (final var groupName : billingGroups) {
            var groupApiDocsRouteId = serviceName + "-" + groupName + "-api-docs";
            var groupApiDocsPath = "/" + contextPath + "/api-docs/" + groupName;
            var finalContextPath = contextPath;

            routeBuilder.route(groupApiDocsRouteId, r -> r
                    .path(groupApiDocsPath)
                    .filters(f -> f.rewritePath(
                            "/" + finalContextPath + "/api-docs/" + groupName,
                            "/api-docs/" + groupName))
                    .uri(serviceUri));

            log.info("Created specialized API Docs route for '{}' group '{}': {} -> {}/api-docs/{}",
                    serviceName, groupName, groupApiDocsPath, serviceUri, groupName);
        }

        return routeBuilder.build();
    }
}
