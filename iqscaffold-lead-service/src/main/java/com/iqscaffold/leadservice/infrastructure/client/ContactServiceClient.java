package com.iqscaffold.leadservice.infrastructure.client;

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
 * Client for communicating with the Contact Service.
 * <p>
 * Handles HTTP requests to the Contact Service REST API with:
 * <ul>
 *   <li>Automatic tenant context propagation</li>
 *   <li>JWT token forwarding</li>
 *   <li>Error handling and logging</li>
 * </ul>
 */
@Component
public class ContactServiceClient {

  private static final Logger log = LoggerFactory.getLogger(ContactServiceClient.class);

  private final WebClient contactServiceWebClient;

  public ContactServiceClient(final WebClient contactServiceWebClient) {
    this.contactServiceWebClient = contactServiceWebClient;
  }

  /**
   * Creates a contact in the Contact Service.
   *
   * @param request     The contact creation request
   * @param bearerToken JWT bearer token for authentication
   * @return The created contact response
   * @throws ContactServiceException if the request fails
   */
  @CircuitBreaker(name = "contactService", fallbackMethod = "createContactFallback")
  @Retry(name = "contactService")
  @TimeLimiter(name = "contactService")
  public ContactResponse createContact(
      final CreateContactRequest request,
      final String bearerToken) {
    String tenantId = TenantContext.getCurrentTenant();

    log.debug("Creating contact in Contact Service for tenant: {}", tenantId);

    try {
      return contactServiceWebClient
          .post()
          .uri("/api/v1/contacts")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
          .header("X-Tenant-ID", tenantId)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(ContactResponse.class)
          .block();
    } catch (final WebClientResponseException e) {
      log.error("Failed to create contact in Contact Service: {} - {}",
          e.getStatusCode(), e.getResponseBodyAsString());
      throw new ContactServiceException(
          "Failed to create contact: " + e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Unexpected error calling Contact Service", e);
      throw new ContactServiceException(
          "Unexpected error creating contact: " + e.getMessage(), e);
    }
  }

  /**
   * Fallback method for contact creation when circuit breaker is open or retries exhausted.
   *
   * @param request     The contact creation request
   * @param bearerToken JWT bearer token for authentication
   * @param throwable   The exception that triggered the fallback
   * @return Never returns, always throws exception
   * @throws ContactServiceException with circuit breaker information
   */
  private ContactResponse createContactFallback(
      final CreateContactRequest request,
      final String bearerToken,
      final Throwable throwable) {
    log.error("Contact Service is unavailable or circuit breaker is open. Fallback triggered.", throwable);
    throw new ContactServiceException(
        "Contact Service is currently unavailable. Please try again later. "
            + "Reason: " + throwable.getMessage(),
        throwable
    );
  }

  /**
   * Deletes a contact in the Contact Service (used for rollback).
   *
   * @param contactId   The ID of the contact to delete
   * @param bearerToken JWT bearer token for authentication
   * @throws ContactServiceException if the request fails
   */
  @CircuitBreaker(name = "contactService", fallbackMethod = "deleteContactFallback")
  @Retry(name = "contactService")
  @TimeLimiter(name = "contactService")
  public void deleteContact(final Long contactId, final String bearerToken) {
    String tenantId = TenantContext.getCurrentTenant();

    log.debug("Deleting contact {} in Contact Service for tenant: {}", contactId, tenantId);

    try {
      contactServiceWebClient
          .delete()
          .uri("/api/v1/contacts/{id}", contactId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
          .header("X-Tenant-ID", tenantId)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
      log.info("Successfully deleted contact {} for rollback", contactId);
    } catch (final WebClientResponseException e) {
      log.error("Failed to delete contact {} in Contact Service: {} - {}",
          contactId, e.getStatusCode(), e.getResponseBodyAsString());
      throw new ContactServiceException(
          "Failed to delete contact for rollback: " + e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Unexpected error deleting contact {} in Contact Service", contactId, e);
      throw new ContactServiceException(
          "Unexpected error deleting contact for rollback: " + e.getMessage(), e);
    }
  }

  /**
   * Fallback method for contact deletion when circuit breaker is open or retries exhausted.
   *
   * @param contactId   The ID of the contact to delete
   * @param bearerToken JWT bearer token for authentication
   * @param throwable   The exception that triggered the fallback
   * @throws ContactServiceException with circuit breaker information
   */
  private void deleteContactFallback(
      final Long contactId,
      final String bearerToken,
      final Throwable throwable) {
    log.error("Contact Service is unavailable or circuit breaker is open during rollback. "
        + "Contact {} may need manual cleanup.", contactId, throwable);
    throw new ContactServiceException(
        "Contact Service is currently unavailable for rollback. Manual cleanup may be required for contact "
            + contactId + ". Reason: " + throwable.getMessage(),
        throwable
    );
  }

  /**
   * Request DTO for creating a contact.
   */
  public record CreateContactRequest(
      String firstName,
      String lastName,
      String email,
      String phone,
      String jobTitle,
      Long companyId,
      String status,
      Integer leadScore,
      String notes
  ) {
  }

  /**
   * Response DTO from Contact Service.
   */
  public record ContactResponse(
      Long id,
      String firstName,
      String lastName,
      String email,
      String phone,
      String jobTitle,
      Long companyId,
      String status,
      Integer leadScore,
      String notes,
      Long convertedFromLeadId,
      LocalDateTime convertedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String createdBy,
      String updatedBy
  ) {
  }

  /**
   * Exception thrown when Contact Service communication fails.
   */
  public static class ContactServiceException extends RuntimeException {
    public ContactServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
