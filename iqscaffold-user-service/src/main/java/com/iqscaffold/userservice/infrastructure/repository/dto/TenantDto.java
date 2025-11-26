package com.iqscaffold.userservice.infrastructure.repository.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Data Transfer Objects for Tenant operations using Java 21 records. Provides immutable data structures for tenant management.
 */
public final class TenantDto {

  /**
   * Request DTO for creating a new tenant.
   */
  public record CreateTenantRequest(
      @NotBlank(message = "Tenant ID is required")
      @Size(min = 3, max = 100, message = "Tenant ID must be between 3 and 100 characters")
      String tenantId,

      @NotBlank(message = "Tenant name is required")
      @Size(min = 2, max = 255, message = "Tenant name must be between 2 and 255 characters")
      String name,

      @Size(max = 500, message = "Description cannot exceed 500 characters")
      String description,

      @Size(max = 255, message = "Domain cannot exceed 255 characters")
      String domain,

      @Min(value = 1, message = "Max users must be at least 1")
      Integer maxUsers,

      @Min(value = 1, message = "Storage quota must be at least 1 GB")
      Integer storageQuotaGb,

      @Min(value = 1, message = "API rate limit must be at least 1 request per minute")
      Integer apiRateLimitPerMinute
  ) {

  }

  /**
   * Request DTO for updating an existing tenant.
   */
  public record UpdateTenantRequest(
      @Size(min = 2, max = 255, message = "Tenant name must be between 2 and 255 characters")
      String name,

      @Size(max = 500, message = "Description cannot exceed 500 characters")
      String description,

      @Size(max = 255, message = "Domain cannot exceed 255 characters")
      String domain,

      @Min(value = 1, message = "Max users must be at least 1")
      Integer maxUsers,

      @Min(value = 1, message = "Storage quota must be at least 1 GB")
      Integer storageQuotaGb,

      @Min(value = 1, message = "API rate limit must be at least 1 request per minute")
      Integer apiRateLimitPerMinute,

      Boolean enabled
  ) {

  }

  /**
   * Response DTO for tenant information.
   */
  public record TenantResponse(
      Long id,
      String tenantId,
      String name,
      String description,
      Boolean enabled,
      String domain,
      Integer maxUsers,
      Integer storageQuotaGb,
      Integer apiRateLimitPerMinute,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy
  ) {

  }

  /**
   * Summary DTO for tenant listing.
   */
  public record TenantSummary(
      String tenantId,
      String name,
      Boolean enabled,
      Long userCount,
      Integer maxUsers,
      LocalDateTime createdAt
  ) {

  }

  /**
   * Statistics DTO for tenant analytics.
   */
  public record TenantStatistics(
      String tenantId,
      String name,
      Boolean enabled,
      Long userCount,
      Integer maxUsers,
      Double userQuotaUtilization,
      LocalDateTime createdAt
  ) {

    /**
     * Calculate user quota utilization percentage.
     *
     * @param userCount current user count
     * @param maxUsers  maximum allowed users
     * @return utilization percentage or null if no quota set
     */
    public static Double calculateUtilization(Long userCount, Integer maxUsers) {
      if (maxUsers == null || maxUsers <= 0 || userCount == null) {
        return null;
      }
      return (userCount.doubleValue() / maxUsers.doubleValue()) * 100.0;
    }
  }

  /**
   * Configuration DTO for tenant settings.
   */
  public record TenantConfiguration(
      String tenantId,
      Integer maxUsers,
      Integer storageQuotaGb,
      Integer apiRateLimitPerMinute,
      Boolean enabled
  ) {

  }

  /**
   * Projection interface for tenant queries.
   */
  public interface TenantProjection {

    String getTenantId();

    String getName();

    Boolean getEnabled();

    LocalDateTime getCreatedAt();
  }

  /**
   * Domain extraction result for tenant resolution.
   */
  public record TenantResolutionResult(
      String tenantId,
      String resolutionMethod,
      String sourceValue,
      Boolean isValid
  ) {

  }
}