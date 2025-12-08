package com.iqscaffold.gatewayservice.service;

/**
 * Request DTO for checking feature access in billing service.
 */
public record FeatureCheckRequest(
    String featureCode
) {
}
