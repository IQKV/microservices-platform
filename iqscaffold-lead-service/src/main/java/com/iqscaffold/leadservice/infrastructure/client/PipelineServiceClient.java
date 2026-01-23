package com.iqscaffold.leadservice.infrastructure.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iqscaffold.leadservice.tenancy.TenantContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client for communicating with the Pipeline Service.
 * <p>
 * Handles HTTP requests to the Pipeline Service REST API with:
 * <ul>
 *   <li>Automatic tenant context propagation</li>
 *   <li>JWT token forwarding</li>
 *   <li>Error handling and logging</li>
 * </ul>
 */
@Component
public class PipelineServiceClient {

  private static final Logger log = LoggerFactory.getLogger(PipelineServiceClient.class);

  private final WebClient pipelineServiceWebClient;

  public PipelineServiceClient(final WebClient pipelineServiceWebClient) {
    this.pipelineServiceWebClient = pipelineServiceWebClient;
  }

  /**
   * Creates a pipeline item in the Pipeline Service.
   *
   * @param request     The pipeline item creation request
   * @param bearerToken JWT bearer token for authentication
   * @return The created pipeline item response
   * @throws PipelineServiceException if the request fails
   */
  @CircuitBreaker(name = "pipelineService", fallbackMethod = "createPipelineItemFallback")
  @Retry(name = "pipelineService")
  @TimeLimiter(name = "pipelineService")
  public PipelineItemResponse createPipelineItem(
      final CreatePipelineItemRequest request,
      final String bearerToken) {
    String tenantId = TenantContext.getCurrentTenant();

    log.debug("Creating pipeline item in Pipeline Service for tenant: {}", tenantId);

    try {
      return pipelineServiceWebClient
          .post()
          .uri("/api/v1/pipeline/items")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
          .header("X-Tenant-ID", tenantId)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(PipelineItemResponse.class)
          .block();
    } catch (final WebClientResponseException e) {
      log.error("Failed to create pipeline item in Pipeline Service: {} - {}",
          e.getStatusCode(), e.getResponseBodyAsString());
      throw new PipelineServiceException(
          "Failed to create pipeline item: " + e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Unexpected error calling Pipeline Service", e);
      throw new PipelineServiceException(
          "Unexpected error creating pipeline item: " + e.getMessage(), e);
    }
  }

  /**
   * Fallback method for pipeline item creation when circuit breaker is open or retries exhausted.
   *
   * @param request     The pipeline item creation request
   * @param bearerToken JWT bearer token for authentication
   * @param throwable   The exception that triggered the fallback
   * @return Never returns, always throws exception
   * @throws PipelineServiceException with circuit breaker information
   */
  private PipelineItemResponse createPipelineItemFallback(
      final CreatePipelineItemRequest request,
      final String bearerToken,
      final Throwable throwable) {
    log.error("Pipeline Service is unavailable or circuit breaker is open. Fallback triggered.", throwable);
    throw new PipelineServiceException(
        "Pipeline Service is currently unavailable. Please try again later. "
        + "Reason: " + throwable.getMessage(),
        throwable
    );
  }

  /**
   * Deletes a pipeline item in the Pipeline Service (used for rollback).
   *
   * @param pipelineItemId The ID of the pipeline item to delete
   * @param bearerToken    JWT bearer token for authentication
   * @throws PipelineServiceException if the request fails
   */
  @CircuitBreaker(name = "pipelineService", fallbackMethod = "deletePipelineItemFallback")
  @Retry(name = "pipelineService")
  @TimeLimiter(name = "pipelineService")
  public void deletePipelineItem(final Long pipelineItemId, final String bearerToken) {
    String tenantId = TenantContext.getCurrentTenant();

    log.debug("Deleting pipeline item {} in Pipeline Service for tenant: {}", pipelineItemId, tenantId);

    try {
      pipelineServiceWebClient
          .delete()
          .uri("/api/v1/pipeline/items/{id}", pipelineItemId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
          .header("X-Tenant-ID", tenantId)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
      log.info("Successfully deleted pipeline item {} for rollback", pipelineItemId);
    } catch (final WebClientResponseException e) {
      log.error("Failed to delete pipeline item {} in Pipeline Service: {} - {}",
          pipelineItemId, e.getStatusCode(), e.getResponseBodyAsString());
      throw new PipelineServiceException(
          "Failed to delete pipeline item for rollback: " + e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Unexpected error deleting pipeline item {} in Pipeline Service", pipelineItemId, e);
      throw new PipelineServiceException(
          "Unexpected error deleting pipeline item for rollback: " + e.getMessage(), e);
    }
  }

  /**
   * Fallback method for pipeline item deletion when circuit breaker is open or retries exhausted.
   *
   * @param pipelineItemId The ID of the pipeline item to delete
   * @param bearerToken    JWT bearer token for authentication
   * @param throwable      The exception that triggered the fallback
   * @throws PipelineServiceException with circuit breaker information
   */
  private void deletePipelineItemFallback(
      final Long pipelineItemId,
      final String bearerToken,
      final Throwable throwable) {
    log.error("Pipeline Service is unavailable or circuit breaker is open during rollback. "
              + "Pipeline item {} may need manual cleanup.", pipelineItemId, throwable);
    throw new PipelineServiceException(
        "Pipeline Service is currently unavailable for rollback. Manual cleanup may be required for pipeline item "
        + pipelineItemId + ". Reason: " + throwable.getMessage(),
        throwable
    );
  }

  /**
   * Request DTO for creating a pipeline item.
   */
  public record CreatePipelineItemRequest(
      Long leadId,
      Long stageId,
      BigDecimal expectedValue,
      BigDecimal probability,
      String createdBy
  ) {
  }

  /**
   * Response DTO from Pipeline Service.
   */
  public record PipelineItemResponse(
      Long id,
      Long leadId,
      Long stageId,
      BigDecimal expectedValue,
      BigDecimal probability,
      LocalDateTime enteredStageAt,
      Integer daysInStage,
      LocalDateTime convertedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy
  ) {
  }

  /**
   * Exception thrown when Pipeline Service communication fails.
   */
  public static class PipelineServiceException extends RuntimeException {
    public PipelineServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}