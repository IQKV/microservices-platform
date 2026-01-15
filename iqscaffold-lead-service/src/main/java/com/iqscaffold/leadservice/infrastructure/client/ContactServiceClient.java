package com.iqscaffold.leadservice.infrastructure.client;

import java.time.LocalDateTime;

import com.iqscaffold.leadservice.tenancy.TenantContext;
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
