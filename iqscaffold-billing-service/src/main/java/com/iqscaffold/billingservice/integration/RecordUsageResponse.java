package com.iqscaffold.billingservice.integration;

import com.iqscaffold.billingservice.usage.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Response DTO for usage recording.
 * 
 * <p>Confirms that usage has been recorded (or queued for async processing).
 * Includes updated quota information to help services make decisions.
 * 
 * @param recorded whether usage was successfully recorded
 * @param metricType type of metric recorded
 * @param quantity amount recorded
 * @param currentUsage updated current usage in billing period
 * @param limit quota limit for billing period
 * @param remainingQuota remaining quota after this recording
 * @param message human-readable confirmation message
 */
@Schema(description = "Response confirming usage recording")
public record RecordUsageResponse(
    @Schema(description = "Whether usage was successfully recorded", example = "true")
    boolean recorded,
    
    @Schema(description = "Type of metric recorded", example = "API_CALLS")
    MetricType metricType,
    
    @Schema(description = "Amount recorded", example = "1")
    long quantity,
    
    @Schema(description = "Updated current usage in billing period", example = "751")
    long currentUsage,
    
    @Schema(description = "Quota limit for billing period", example = "1000")
    long limit,
    
    @Schema(description = "Remaining quota after recording", example = "249")
    long remainingQuota,
    
    @Schema(description = "Confirmation message", example = "Usage recorded successfully. 249 API calls remaining.")
    String message
) {}
