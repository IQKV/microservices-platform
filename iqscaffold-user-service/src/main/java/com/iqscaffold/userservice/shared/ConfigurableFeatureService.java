package com.iqscaffold.userservice.shared;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.iqscaffold.userservice.config.PlatformConfigurationProperties;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration-driven feature service that uses YAML properties instead of hardcoded enums.
 * 
 * <p>This service provides feature management based entirely on configuration properties,
 * making the platform extensible without code changes. All feature definitions, authorities,
 * microservice mappings, and route patterns are defined in application.yml.
 * 
 * <h3>Configuration-Driven Architecture</h3>
 * <ul>
 *   <li><strong>No Hardcoded Features</strong> - All features defined in YAML</li>
 *   <li><strong>Dynamic Authority Lists</strong> - Authority hierarchies in configuration</li>
 *   <li><strong>Flexible Route Mapping</strong> - URL patterns mapped via properties</li>
 *   <li><strong>Microservice Integration</strong> - Service mappings in configuration</li>
 * </ul>
 * 
 * <h3>Extensibility Benefits</h3>
 * <ul>
 *   <li>Add new features without code changes</li>
 *   <li>Modify authority hierarchies via configuration</li>
 *   <li>Update route patterns without recompilation</li>
 *   <li>Environment-specific feature sets</li>
 * </ul>
 */
@Service
@Transactional
public class ConfigurableFeatureService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurableFeatureService.class);

  private final PlatformConfigurationProperties platformConfig;
  private final AuthorityRepository authorityRepository;
  private final UserRepository userRepository;

  public ConfigurableFeatureService(final PlatformConfigurationProperties platformConfig,
                                    final AuthorityRepository authorityRepository,
                                    final UserRepository userRepository) {
    this.platformConfig = platformConfig;
    this.authorityRepository = authorityRepository;
    this.userRepository = userRepository;
  }

  /**
   * Check if a user has access to a specific feature based on configuration.
   * 
   * @param userId the user ID to check
   * @param featureCode the feature code from configuration
   * @return true if user has access to the feature
   */
  public boolean hasFeatureAccess(Long userId, String featureCode) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return false;
    }
    
    return hasFeatureAccess(user.get(), featureCode);
  }

  /**
   * Check if a user has access to a specific feature based on configuration.
   * 
   * @param user the user to check
   * @param featureCode the feature code from configuration
   * @return true if user has access to the feature
   */
  public boolean hasFeatureAccess(User user, String featureCode) {
    var featureDefinition = platformConfig.features().getFeature(featureCode);
    if (featureDefinition == null || !featureDefinition.enabled()) {
      return false;
    }
    
    var userAuthorities = user.getAuthorities().stream()
        .map(Authority::getName)
        .collect(Collectors.toSet());
    
    return hasRequiredAuthorities(userAuthorities, featureDefinition.requiredAuthorities());
  }

  /**
   * Enable a feature for a user by granting the required authorities.
   * 
   * @param userId the user ID to enable the feature for
   * @param featureCode the feature code from configuration
   * @return true if the feature was enabled, false if already enabled or user not found
   */
  public boolean enableFeature(Long userId, String featureCode) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      logger.warn("Cannot enable feature {}: user not found with ID {}", featureCode, userId);
      return false;
    }
    
    return enableFeature(user.get(), featureCode);
  }

  /**
   * Enable a feature for a user by granting the required authorities.
   * 
   * @param user the user to enable the feature for
   * @param featureCode the feature code from configuration
   * @return true if the feature was enabled, false if already enabled
   */
  public boolean enableFeature(User user, String featureCode) {
    var featureDefinition = platformConfig.features().getFeature(featureCode);
    if (featureDefinition == null) {
      throw new IllegalArgumentException("Unknown feature code: " + featureCode);
    }
    
    if (!featureDefinition.composable()) {
      throw new IllegalArgumentException("Feature is not composable: " + featureCode);
    }
    
    if (hasFeatureAccess(user, featureCode)) {
      logger.debug("User {} already has access to feature {}", user.getUsername(), featureCode);
      return false;
    }
    
    // Enable dependencies first
    for (final var dependency : featureDefinition.dependencies()) {
      if (!hasFeatureAccess(user, dependency)) {
        logger.info("Enabling dependency {} for feature {} for user {}", 
            dependency, featureCode, user.getUsername());
        enableFeature(user, dependency);
      }
    }
    
    // Grant primary access authority
    var primaryAuthority = featureDefinition.getPrimaryAuthority();
    if (primaryAuthority != null) {
      var authority = findOrCreateAuthority(primaryAuthority);
      user.addAuthority(authority);
      userRepository.save(user);
      
      logger.info("Enabled feature {} for user: {} (granted authority: {})", 
          featureCode, user.getUsername(), primaryAuthority);
      return true;
    }
    
    return false;
  }

  /**
   * Disable a feature for a user by removing associated authorities.
   * 
   * @param userId the user ID to disable the feature for
   * @param featureCode the feature code from configuration
   * @return true if the feature was disabled, false if not enabled or user not found
   */
  public boolean disableFeature(Long userId, String featureCode) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      logger.warn("Cannot disable feature {}: user not found with ID {}", featureCode, userId);
      return false;
    }
    
    return disableFeature(user.get(), featureCode);
  }

  /**
   * Disable a feature for a user by removing associated authorities.
   * 
   * @param user the user to disable the feature for
   * @param featureCode the feature code from configuration
   * @return true if the feature was disabled, false if not enabled
   */
  public boolean disableFeature(User user, String featureCode) {
    var featureDefinition = platformConfig.features().getFeature(featureCode);
    if (featureDefinition == null) {
      throw new IllegalArgumentException("Unknown feature code: " + featureCode);
    }
    
    if (!featureDefinition.composable()) {
      throw new IllegalArgumentException("Feature is not composable: " + featureCode);
    }
    
    if (!hasFeatureAccess(user, featureCode)) {
      logger.debug("User {} doesn't have access to feature {} to disable", 
          user.getUsername(), featureCode);
      return false;
    }
    
    // Remove all feature authorities
    var featureAuthorityNames = Set.copyOf(featureDefinition.allAuthorities());
    
    var authoritiesToRemove = user.getAuthorities().stream()
        .filter(authority -> featureAuthorityNames.contains(authority.getName()))
        .toList();
    
    for (final var authority : authoritiesToRemove) {
      user.removeAuthority(authority);
    }
    
    userRepository.save(user);
    
    logger.info("Disabled feature {} for user: {} (removed {} authorities)", 
        featureCode, user.getUsername(), authoritiesToRemove.size());
    return true;
  }

  /**
   * Get all features that a user has access to based on configuration.
   * 
   * @param userId the user ID to check
   * @return list of feature codes the user has access to
   */
  public List<String> getUserFeatures(Long userId) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return List.of();
    }
    
    return getUserFeatures(user.get());
  }

  /**
   * Get all features that a user has access to based on configuration.
   * 
   * @param user the user to check
   * @return list of feature codes the user has access to
   */
  public List<String> getUserFeatures(User user) {
    return platformConfig.features().getComposableFeatures().entrySet().stream()
        .filter(entry -> hasFeatureAccess(user, entry.getKey()))
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * Get all users who have access to a specific feature.
   * 
   * @param featureCode the feature code from configuration
   * @return list of users with access to the feature
   */
  public List<User> getUsersWithFeature(String featureCode) {
    var featureDefinition = platformConfig.features().getFeature(featureCode);
    if (featureDefinition == null) {
      return List.of();
    }
    
    var authorityNames = List.<String>of(featureDefinition.allAuthorities().toArray(new String[0]));
    var allAuthorityNames = new java.util.ArrayList<>(authorityNames);
    allAuthorityNames.addAll(platformConfig.authorities().adminAuthorities());
    
    return userRepository.findUsersWithAnyAuthority(allAuthorityNames);
  }

  /**
   * Get feature definition from configuration.
   * 
   * @param featureCode the feature code
   * @return feature definition or null if not found
   */
  public PlatformConfigurationProperties.FeatureDefinition getFeatureDefinition(String featureCode) {
    return platformConfig.features().getFeature(featureCode);
  }

  /**
   * Get all available features from configuration.
   * 
   * @return map of feature codes to definitions
   */
  public java.util.Map<String, PlatformConfigurationProperties.FeatureDefinition> getAvailableFeatures() {
    return platformConfig.features().definitions();
  }

  /**
   * Get composable features from configuration.
   * 
   * @return map of composable feature codes to definitions
   */
  public java.util.Map<String, PlatformConfigurationProperties.FeatureDefinition> getComposableFeatures() {
    return platformConfig.features().getComposableFeatures();
  }

  /**
   * Find feature by route pattern.
   * 
   * @param route the route to match
   * @return feature code that matches the route, or null if none found
   */
  public String findFeatureByRoute(String route) {
    return platformConfig.features().definitions().entrySet().stream()
        .filter(entry -> entry.getValue().matchesRoute(route))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  /**
   * Get features that use a specific microservice.
   * 
   * @param microservice the microservice name
   * @return list of feature codes that use the microservice
   */
  public List<String> getFeaturesForMicroservice(String microservice) {
    return platformConfig.features().definitions().entrySet().stream()
        .filter(entry -> entry.getValue().usesMicroservice(microservice))
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * Check if user authorities include required authorities (with admin override).
   */
  private boolean hasRequiredAuthorities(Set<String> userAuthorities, List<String> requiredAuthorities) {
    // Check for admin authorities (implicit access)
    if (userAuthorities.stream().anyMatch(platformConfig.authorities()::isAdminAuthority)) {
      return true;
    }
    
    // Check for required authorities
    return userAuthorities.stream()
        .anyMatch(requiredAuthorities::contains);
  }

  /**
   * Find or create an authority by name.
   */
  private Authority findOrCreateAuthority(String authorityName) {
    var existingAuthority = authorityRepository.findByName(authorityName);
    
    if (existingAuthority.isPresent()) {
      return existingAuthority.get();
    }
    
    // Get description from configuration
    var authorityDefinition = platformConfig.authorities().getAuthority(authorityName);
    var description = authorityDefinition != null 
        ? authorityDefinition.description() 
        : "Configurable authority: " + authorityName;
    
    var newAuthority = new Authority(authorityName, description);
    var savedAuthority = authorityRepository.save(newAuthority);
    
    logger.info("Created authority: {} - {}", authorityName, description);
    return savedAuthority;
  }
}