package com.iqscaffold.billingservice.admin;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.admin.dto.GatewayConfigDtos;
import com.iqscaffold.billingservice.security.GatewayConfigEncryptionService;
import com.iqscaffold.billingservice.security.SecurityContextHelper;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing payment gateway configurations per tenant.
 * <p>
 * Handles CRUD operations, encryption/decryption of sensitive data,
 * and business logic for gateway activation and primary selection.
 * </p>
 */
@Service
public class PaymentGatewayConfigService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayConfigService.class);

  private final PaymentGatewayConfigRepository repository;
  private final GatewayConfigEncryptionService encryptionService;
  private final ObjectMapper objectMapper;

  public PaymentGatewayConfigService(
      final PaymentGatewayConfigRepository repository,
      final GatewayConfigEncryptionService encryptionService,
      final ObjectMapper objectMapper) {
    this.repository = repository;
    this.encryptionService = encryptionService;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a new gateway configuration for the current tenant.
   *
   * @param request The configuration request
   * @return Response with created configuration details
   * @throws IllegalStateException if configuration already exists
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", key = "#tenantId + ':' + #request.gatewayProvider()")
  public GatewayConfigDtos.GatewayConfigResponse createGatewayConfig(GatewayConfigDtos.CreateGatewayConfigRequest request) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();
    
    logger.info("Creating gateway configuration for tenant {} and provider {}", tenantId, request.gatewayProvider());

    // Check if configuration already exists
    if (repository.existsByTenantIdAndGatewayProvider(tenantId, request.gatewayProvider())) {
      throw new IllegalStateException("Gateway configuration already exists for " + request.gatewayProvider());
    }

    // Validate and serialize configuration data
    String configJson = serializeConfigData(request.configData());

    // Encrypt configuration data
    String encryptedConfig = encryptionService.encrypt(configJson, tenantId);

    // Create entity
    PaymentGatewayConfig config = new PaymentGatewayConfig();
    config.setTenantId(tenantId);
    config.setGatewayProvider(request.gatewayProvider());
    config.setConfigData(encryptedConfig);
    config.setMode(request.mode());
    config.setActive(request.isActive());
    config.setDisplayName(request.displayName());
    config.setDescription(request.description());

    // Handle primary designation
    if (request.isPrimary()) {
      // Unset other primary gateways
      unsetOtherPrimaryGateways(tenantId, request.gatewayProvider());
      config.setPrimary(true);
    } else {
      config.setPrimary(false);
    }

    PaymentGatewayConfig savedConfig = repository.save(config);
    logger.info("Successfully created gateway configuration {} for tenant {}", savedConfig.getId(), tenantId);

    return toResponse(savedConfig);
  }

  /**
   * Updates an existing gateway configuration.
   *
   * @param provider The gateway provider to update
   * @param request  The update request
   * @return Response with updated configuration
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", key = "#tenantId + ':' + #provider")
  public GatewayConfigDtos.GatewayConfigResponse updateGatewayConfig(
      PaymentGatewayProvider provider,
      GatewayConfigDtos.UpdateGatewayConfigRequest request) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.info("Updating gateway configuration for tenant {} and provider {}", tenantId, provider);

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    // Update configuration data if provided
    if (request.configData() != null) {
      String configJson = serializeConfigData(request.configData());
      String encryptedConfig = encryptionService.encrypt(configJson, tenantId);
      config.setConfigData(encryptedConfig);
    }

    // Update other fields if provided
    if (request.mode() != null) {
      config.setMode(request.mode());
    }
    if (request.isActive() != null) {
      config.setActive(request.isActive());
    }
    if (request.displayName() != null) {
      config.setDisplayName(request.displayName());
    }
    if (request.description() != null) {
      config.setDescription(request.description());
    }

    // Handle primary designation
    if (request.isPrimary() != null && request.isPrimary()) {
      unsetOtherPrimaryGateways(tenantId, provider);
      config.setPrimary(true);
    } else if (request.isPrimary() != null && !request.isPrimary()) {
      config.setPrimary(false);
    }

    PaymentGatewayConfig updatedConfig = repository.save(config);
    logger.info("Successfully updated gateway configuration for tenant {} and provider {}", tenantId, provider);

    return toResponse(updatedConfig);
  }

  /**
   * Retrieves a gateway configuration for the current tenant.
   *
   * @param provider The gateway provider
   * @return Response with configuration details
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "gatewayConfigs", key = "#tenantId + ':' + #provider")
  public GatewayConfigDtos.GatewayConfigResponse getGatewayConfig(PaymentGatewayProvider provider) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    return toResponse(config);
  }

  /**
   * Retrieves decrypted gateway configuration data.
   * This should be used internally only and never exposed via REST API.
   *
   * @param tenantId The tenant ID
   * @param provider The gateway provider
   * @return Decrypted configuration data
   */
  @Transactional(readOnly = true)
  public <T extends GatewayConfigDtos.GatewayConfigData> T getDecryptedGatewayConfig(
      String tenantId, PaymentGatewayProvider provider, Class<T> configClass) {
    
    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException(
            "Gateway configuration not found for tenant " + tenantId + " and provider " + provider));

    if (!config.isActive()) {
      throw new IllegalStateException("Gateway " + provider + " is not active for tenant " + tenantId);
    }

    // Decrypt configuration data
    String decryptedJson = encryptionService.decrypt(config.getConfigData(), tenantId);

    // Deserialize to specific type
    try {
      return objectMapper.readValue(decryptedJson, configClass);
    } catch (final JsonProcessingException e) {
      logger.error("Failed to deserialize gateway config for tenant {} and provider {}: {}", 
          tenantId, provider, e.getMessage(), e);
      throw new RuntimeException("Failed to deserialize gateway configuration", e);
    }
  }

  /**
   * Lists all gateway configurations for the current tenant.
   *
   * @return List of configuration summaries
   */
  @Transactional(readOnly = true)
  public List<GatewayConfigDtos.GatewayConfigSummary> listGatewayConfigs() {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    return repository.findByTenantId(tenantId).stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  /**
   * Lists all active gateway configurations for the current tenant.
   *
   * @return List of active configuration summaries
   */
  @Transactional(readOnly = true)
  public List<GatewayConfigDtos.GatewayConfigSummary> listActiveGatewayConfigs() {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    return repository.findByTenantIdAndIsActive(tenantId, true).stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  /**
   * Gets the primary gateway configuration for the current tenant.
   *
   * @return Primary gateway configuration
   */
  @Transactional(readOnly = true)
  public Optional<GatewayConfigDtos.GatewayConfigResponse> getPrimaryGatewayConfig() {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    return repository.findByTenantIdAndIsPrimary(tenantId, true)
        .map(this::toResponse);
  }

  /**
   * Activates a gateway for the current tenant.
   *
   * @param provider The gateway provider to activate
   * @return Status response
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", key = "#tenantId + ':' + #provider")
  public GatewayConfigDtos.GatewayStatusResponse activateGateway(PaymentGatewayProvider provider) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.info("Activating gateway {} for tenant {}", provider, tenantId);

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    config.setActive(true);
    repository.save(config);

    return new GatewayConfigDtos.GatewayStatusResponse(
        config.getId(),
        config.getGatewayProvider(),
        true,
        config.isPrimary(),
        "Gateway activated successfully"
    );
  }

  /**
   * Deactivates a gateway for the current tenant.
   *
   * @param provider The gateway provider to deactivate
   * @return Status response
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", key = "#tenantId + ':' + #provider")
  public GatewayConfigDtos.GatewayStatusResponse deactivateGateway(PaymentGatewayProvider provider) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.info("Deactivating gateway {} for tenant {}", provider, tenantId);

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    // Prevent deactivating the only active gateway
    long activeCount = repository.countByTenantIdAndIsActive(tenantId, true);
    if (activeCount == 1 && config.isActive()) {
      throw new IllegalStateException("Cannot deactivate the only active gateway");
    }

    config.setActive(false);
    // Also remove primary designation if it was primary
    if (config.isPrimary()) {
      config.setPrimary(false);
    }
    repository.save(config);

    return new GatewayConfigDtos.GatewayStatusResponse(
        config.getId(),
        config.getGatewayProvider(),
        false,
        config.isPrimary(),
        "Gateway deactivated successfully"
    );
  }

  /**
   * Sets a gateway as the primary payment method for the tenant.
   *
   * @param provider The gateway provider to set as primary
   * @return Status response
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", allEntries = true)
  public GatewayConfigDtos.GatewayStatusResponse setPrimaryGateway(PaymentGatewayProvider provider) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.info("Setting gateway {} as primary for tenant {}", provider, tenantId);

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    if (!config.isActive()) {
      throw new IllegalStateException("Cannot set an inactive gateway as primary. Activate it first.");
    }

    // Unset other primary gateways
    unsetOtherPrimaryGateways(tenantId, provider);

    config.setPrimary(true);
    repository.save(config);

    return new GatewayConfigDtos.GatewayStatusResponse(
        config.getId(),
        config.getGatewayProvider(),
        config.isActive(),
        true,
        "Gateway set as primary successfully"
    );
  }

  /**
   * Deletes a gateway configuration.
   *
   * @param provider The gateway provider to delete
   */
  @Transactional
  @CacheEvict(value = "gatewayConfigs", key = "#tenantId + ':' + #provider")
  public void deleteGatewayConfig(PaymentGatewayProvider provider) {
    String tenantId = SecurityContextHelper.getCurrentTenantId();

    logger.info("Deleting gateway configuration for tenant {} and provider {}", tenantId, provider);

    PaymentGatewayConfig config = repository.findByTenantIdAndGatewayProvider(tenantId, provider)
        .orElseThrow(() -> new IllegalArgumentException("Gateway configuration not found for " + provider));

    // Prevent deleting the only active gateway
    if (config.isActive()) {
      long activeCount = repository.countByTenantIdAndIsActive(tenantId, true);
      if (activeCount == 1) {
        throw new IllegalStateException("Cannot delete the only active gateway");
      }
    }

    repository.delete(config);
    logger.info("Successfully deleted gateway configuration for tenant {} and provider {}", tenantId, provider);
  }

  // Helper methods

  private void unsetOtherPrimaryGateways(String tenantId, PaymentGatewayProvider excludeProvider) {
    List<PaymentGatewayConfig> otherPrimaryGateways = repository.findOtherPrimaryGateways(tenantId, excludeProvider);
    otherPrimaryGateways.forEach(config -> {
      config.setPrimary(false);
      repository.save(config);
      logger.debug("Unset primary flag for gateway {} for tenant {}", config.getGatewayProvider(), tenantId);
    });
  }

  private String serializeConfigData(GatewayConfigDtos.GatewayConfigData configData) {
    try {
      return objectMapper.writeValueAsString(configData);
    } catch (final JsonProcessingException e) {
      logger.error("Failed to serialize gateway config data: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to serialize configuration data", e);
    }
  }

  private GatewayConfigDtos.GatewayConfigResponse toResponse(PaymentGatewayConfig config) {
    // Extract last 4 chars of a key for masking (we'll need to decrypt first to get it)
    String lastFourChars = "";
    try {
      String decryptedJson = encryptionService.decrypt(config.getConfigData(), config.getTenantId());
      // Simple extraction - in production you might want to parse JSON properly
      if (decryptedJson.contains("apiKey")) {
        lastFourChars = "****";
      }
    } catch (final Exception e) {
      logger.debug("Could not extract last four chars for masking");
    }

    GatewayConfigDtos.MaskedConfigData maskedData = new GatewayConfigDtos.MaskedConfigData(
        config.getGatewayProvider(),
        true,
        lastFourChars
    );

    return new GatewayConfigDtos.GatewayConfigResponse(
        config.getId(),
        config.getTenantId(),
        config.getGatewayProvider(),
        config.isActive(),
        config.isPrimary(),
        config.getMode(),
        config.getDisplayName(),
        config.getDescription(),
        maskedData,
        config.getCreatedAt(),
        config.getUpdatedAt()
    );
  }

  private GatewayConfigDtos.GatewayConfigSummary toSummary(PaymentGatewayConfig config) {
    return new GatewayConfigDtos.GatewayConfigSummary(
        config.getId(),
        config.getGatewayProvider(),
        config.isActive(),
        config.isPrimary(),
        config.getMode(),
        config.getDisplayName(),
        config.getUpdatedAt()
    );
  }
}
