package com.iqscaffold.userservice.infrastructure.messaging;

import com.iqscaffold.userservice.config.RabbitMQConfig;
import com.iqscaffold.userservice.organization.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for billing service events with payment gateway abstraction.
 * Clean greenfield implementation without backward compatibility.
 */
@Component
public class BillingEventListener {

  private static final Logger log = LoggerFactory.getLogger(BillingEventListener.class);

  private final OrganizationRepository organizationRepository;

  public BillingEventListener(final OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  /**
   * Handle merchant onboarded event from billing service.
   * Syncs payment gateway account ID and capabilities to organization.
   */
  @RabbitListener(queues = RabbitMQConfig.BILLING_EVENTS_QUEUE)
  @Transactional
  public void handleMerchantOnboarded(MerchantOnboardedEvent event) {
    try {
      log.info("Received merchant onboarded event for organization: {}, gateway: {}, provider: {}",
          event.organizationId(), event.gatewayAccountId(), event.gatewayProvider());

      var organization = organizationRepository.findById(event.organizationId())
          .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + event.organizationId()));

      // Validate tenant matches
      if (!organization.getTenantId().equals(event.tenantId())) {
        log.error("Tenant mismatch for organization {}: expected {}, got {}",
            event.organizationId(), organization.getTenantId(), event.tenantId());
        throw new IllegalArgumentException("Tenant mismatch for organization: " + event.organizationId());
      }

      // Update payment gateway account and capabilities
      organization.setPaymentGatewayAccountId(event.gatewayAccountId());
      organization.setPaymentGatewayProvider(event.gatewayProvider());
      organization.setChargesEnabled(event.chargesEnabled());
      organization.setPayoutsEnabled(event.payoutsEnabled());

      organizationRepository.save(organization);

      log.info("Successfully synced merchant onboarding for organization: {}, gateway: {}, provider: {}",
          event.organizationId(), event.gatewayAccountId(), event.gatewayProvider());
    } catch (final Exception e) {
      log.error("Error processing merchant onboarded event for organization: {}",
          event.organizationId(), e);
      throw e; // Re-throw to trigger retry or DLQ routing
    }
  }

  /**
   * Handle merchant capabilities updated event from billing service.
   * Syncs capability status to organization.
   */
  @RabbitListener(queues = RabbitMQConfig.BILLING_EVENTS_QUEUE)
  @Transactional
  public void handleMerchantCapabilitiesUpdated(MerchantCapabilitiesUpdatedEvent event) {
    try {
      log.info("Received merchant capabilities updated event for organization: {}, charges: {}, payouts: {}",
          event.organizationId(), event.chargesEnabled(), event.payoutsEnabled());

      var organization = organizationRepository.findById(event.organizationId())
          .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + event.organizationId()));

      // Validate tenant matches
      if (!organization.getTenantId().equals(event.tenantId())) {
        log.error("Tenant mismatch for organization {}: expected {}, got {}",
            event.organizationId(), organization.getTenantId(), event.tenantId());
        throw new IllegalArgumentException("Tenant mismatch for organization: " + event.organizationId());
      }

      // Validate payment gateway account matches
      if (!event.gatewayAccountId().equals(organization.getPaymentGatewayAccountId())) {
        log.error("Payment gateway account mismatch for organization {}: expected {}, got {}",
            event.organizationId(), organization.getPaymentGatewayAccountId(), event.gatewayAccountId());
        throw new IllegalArgumentException("Payment gateway account mismatch for organization: " + event.organizationId());
      }

      // Validate provider matches
      if (event.gatewayProvider() != organization.getPaymentGatewayProvider()) {
        log.error("Payment gateway provider mismatch for organization {}: expected {}, got {}",
            event.organizationId(), organization.getPaymentGatewayProvider(), event.gatewayProvider());
        throw new IllegalArgumentException("Payment gateway provider mismatch for organization: " + event.organizationId());
      }

      // Update capabilities
      organization.setChargesEnabled(event.chargesEnabled());
      organization.setPayoutsEnabled(event.payoutsEnabled());

      organizationRepository.save(organization);

      log.info("Successfully synced merchant capabilities for organization: {}, charges: {}, payouts: {}",
          event.organizationId(), event.chargesEnabled(), event.payoutsEnabled());
    } catch (final Exception e) {
      log.error("Error processing merchant capabilities updated event for organization: {}",
          event.organizationId(), e);
      throw e; // Re-throw to trigger retry or DLQ routing
    }
  }
}
