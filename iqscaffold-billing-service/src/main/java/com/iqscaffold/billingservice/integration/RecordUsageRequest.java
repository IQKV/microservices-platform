package com.iqscaffold.billingservice.integration;

import com.iqscaffold.billingservice.usage.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Request DTO for recording usage.
 * 
 * <p>Used by business microservices to record usage for billing and quota tracking.
 * Supports both real-time and batch usage recording.
 * 
 * <p>Usage recording is typically asynchronous (published to RabbitMQ) to handle
 * high volume (10,000+ records/sec) without blocking service operations.
 * 
 * @param metricType type of metric being recorded
 * @param quantity amount of usage
 * @param unit unit of measurement (optional, e.g., "requests", "GB", "emails")
 * @param recordedAt when the usage occurred (optional, defaults to now)
 */
@Schema(description = "Request to record usage for billing and quota tracking")
public record RecordUsageRequest(
    @Schema(
        description = "Type of metric being recorded",
        example = "API_CALLS",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Metric type is required")
    MetricType metricType,
    
    @Schema(
        description = "Amount of usage",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Long quantity,
    
    @Schema(
        description = "Unit of measurement",
        example = "requests"
    )
    String unit,
    
    @Schema(
        description = "When the usage occurred (defaults to now)",
        example = "2024-01-15T10:30:00"
    )
    LocalDateTime recordedAt
) {}
