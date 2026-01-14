package com.iqscaffold.billingservice.infrastructure.client;

import java.util.Optional;

import com.iqscaffold.billingservice.admin.dto.OrganizationDto;
import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * REST client for communicating with the User Service.
 * Provides methods to query and update organization data.
 */
@Service
public class UserServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

  private final RestTemplate restTemplate;
  private final IqScaffoldProperties properties;

  public UserServiceClient(final RestTemplate restTemplate, final IqScaffoldProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  /**
   * Get organization by ID.
   */
  public Optional<OrganizationDto> getOrganization(Long organizationId) {
    try {
      var url = properties.userServiceUrl() + "/api/v1/admin/organizations/" + organizationId;
      logger.debug("Fetching organization {} from user service: {}", organizationId, url);

      var response = restTemplate.getForEntity(url, OrganizationDto.class);
      return Optional.ofNullable(response.getBody());
    } catch (final HttpClientErrorException.NotFound e) {
      logger.warn("Organization not found: {}", organizationId);
      return Optional.empty();
    } catch (final Exception e) {
      logger.error("Error fetching organization {}: {}", organizationId, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Get organization by tenant ID.
   */
  public Optional<OrganizationDto> getOrganizationByTenantId(String tenantId) {
    try {
      var url = properties.userServiceUrl() + "/api/v1/admin/organizations/tenant/" + tenantId;
      logger.debug("Fetching organization for tenant {} from user service: {}", tenantId, url);

      var response = restTemplate.getForEntity(url, OrganizationDto.class);
      return Optional.ofNullable(response.getBody());
    } catch (final HttpClientErrorException.NotFound e) {
      logger.warn("Organization not found for tenant: {}", tenantId);
      return Optional.empty();
    } catch (final Exception e) {
      logger.error("Error fetching organization for tenant {}: {}", tenantId, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Update organization's Stripe account ID.
   * This is called after successful merchant onboarding.
   */
  public void updateOrganizationStripeAccount(Long organizationId, String stripeAccountId) {
    try {
      var url = properties.userServiceUrl() + "/api/v1/admin/organizations/" + organizationId;
      logger.info("Updating organization {} with Stripe account {}", organizationId, stripeAccountId);

      // Create update request with only stripe_account_id
      var request = new UpdateOrganizationStripeAccountRequest(stripeAccountId);
      restTemplate.patchForObject(url, request, Void.class);

      logger.info("Successfully updated organization {} with Stripe account", organizationId);
    } catch (final HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        logger.error("Organization not found: {}", organizationId);
      } else {
        logger.error("Error updating organization {}: {} - {}",
            organizationId, e.getStatusCode(), e.getMessage());
      }
      throw new UserServiceException("Failed to update organization Stripe account", e);
    } catch (final Exception e) {
      logger.error("Unexpected error updating organization {}: {}", organizationId, e.getMessage(), e);
      throw new UserServiceException("Failed to update organization Stripe account", e);
    }
  }

  /**
   * Request DTO for updating Stripe account ID.
   */
  private record UpdateOrganizationStripeAccountRequest(String stripeAccountId) {
  }

  /**
   * Exception thrown when user service communication fails.
   */
  public static class UserServiceException extends RuntimeException {
    public UserServiceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
