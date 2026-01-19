package com.iqscaffold.billingservice.feature.dto;

import com.iqscaffold.billingservice.feature.FeatureDefinition;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for feature information exposed to frontend applications.
 */
@Schema(description = "Feature information for frontend applications")
public record FeatureDto(
    @Schema(description = "Feature unique code", example = "advanced_analytics")
    String code,

    @Schema(description = "Human-readable feature name", example = "Advanced Analytics")
    String name,

    @Schema(description = "Feature description", example = "Access to advanced analytics and reporting")
    String description,

    @Schema(description = "Feature category", example = "ANALYTICS")
    String category,

    @Schema(description = "Whether the feature is enabled for the user", example = "true")
    boolean enabled,

    @Schema(description = "Usage limit for the feature (null if unlimited)", example = "1000")
    Integer usageLimit,

    @Schema(description = "Current usage count", example = "150")
    Integer currentUsage
) {

  /**
   * Creates a FeatureDto from a FeatureDefinition entity.
   */
  public static FeatureDto fromEntity(FeatureDefinition feature, boolean enabled, Integer usageLimit, Integer currentUsage) {
    return new FeatureDto(
        feature.getFeatureKey(),
        feature.getDisplayName(),
        feature.getDescription(),
        feature.getCategory(),
        enabled,
        usageLimit,
        currentUsage != null ? currentUsage : 0
    );
  }

  /**
   * Creates a FeatureDto for an enabled feature without usage tracking.
   */
  public static FeatureDto fromEntity(FeatureDefinition feature, boolean enabled) {
    return fromEntity(feature, enabled, null, 0);
  }
}
