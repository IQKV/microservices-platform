package com.iqscaffold.gatewayservice.service;

/**
 * Response DTO for feature access check from billing service.
 */
public record FeatureCheckResponse(
    boolean available,
    String featureCode,
    String planTier,
    String message
) {
}
