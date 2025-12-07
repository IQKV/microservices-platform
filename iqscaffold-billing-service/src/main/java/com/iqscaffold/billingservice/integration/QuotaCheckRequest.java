package com.iqscaffold.billingservice.integration;

import com.iqscaffold.billingservice.usage.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for checking quota availability.
 * 
 * <p>Used by business microservices to verify if a tenant has quota available
 * before performing an operation. Supports common metrics:
 * <ul>
 *   <li>API_CALLS - API requests across all services</li>
 *   <li>STORAGE_GB - File storage</li>
 *   <li>EMAIL_SENDS - Email sender service</li>
 *   <li>CAMPAIGN_EXECUTIONS - Campaign service</li>
 *   <li>SCORING_REQUESTS - Scoring service</li>
 *   <li>ACTIVE_USERS - User seats</li>
 *   <li>CUSTOM_DOMAINS - Custom domain names</li>
 *   <li>DATA_EXPORTS - Data export operations</li>
 * </ul>
 * 
 * @param metricType type of metric to check
 * @param requestedQuantity amount being requested
 */
@Schema(description = "Request to check if tenant has quota available for an operation")
public record QuotaCheckRequest(
    @Schema(
        description = "Type of metric to check",
        example = "API_CALLS",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Metric type is required")
    MetricType metricType,
    
    @Schema(
        description = "Amount of quota being requested",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1")
    Long requestedQuantity
) {}
