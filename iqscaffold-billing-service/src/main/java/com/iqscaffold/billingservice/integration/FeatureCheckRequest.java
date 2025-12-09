package com.iqscaffold.billingservice.integration;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for checking feature access.
 *
 * <p>Used by business microservices (CRM, Campaign, Email, Scoring, etc.) to verify
 * if a tenant's subscription plan includes a specific feature.
 *
 * <p>Feature codes follow a hierarchical naming convention:
 * <ul>
 *   <li>SERVICE.FEATURE format (e.g., "CRM.BULK_IMPORT", "EMAIL.CUSTOM_TEMPLATES")</li>
 *   <li>Allows fine-grained feature control per service</li>
 *   <li>Supports feature flags and A/B testing</li>
 * </ul>
 *
 * @param featureCode hierarchical feature identifier (e.g., "CRM.BULK_IMPORT")
 */
@Schema(description = "Request to check if a tenant has access to a specific feature")
public record FeatureCheckRequest(
    @Schema(
        description = "Feature code to check (hierarchical format: SERVICE.FEATURE)",
        example = "CRM.BULK_IMPORT",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Feature code is required")
    String featureCode
) {
}
