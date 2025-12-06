package com.iqscaffold.billingservice.usage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for UsageRecord.
 */
@Schema(description = "Usage record data transfer object")
public record UsageDto(
  @Schema(description = "Usage record unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
