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
 * Service for managing microservice access control across the platform.
 * 
 * <p>This service provides unified management of microservice access based on
 * feature authorities. It handles:
 * <ul>
 *   <li>Determining which microservices a user can access</li>
 *   <li>Validating access to specific microservice endpoints</li>
 *   <li>Managing feature-to-microservice mappings</li>
 *   <li>Providing access context for service-to-service communication</li>
 * </ul>
 * 
 * <h3>Microservice Access Model</h3>
 * <ul>
 *   <li><strong>Feature-Based</strong> - Access determined by enabled features</li>
 *   <li><strong>Route-Aware</strong> - Validates access to specific endpoints</li>
 *   <li><strong>Authority-Driven</strong> - Uses existing authority system</li>
 *   <li><strong>Admin Override</strong> - Admin authorities provide universal access</li>
 * </ul>
 * 
 * <h3>Integration Points</h3>
 * <ul>
 *   <li><strong>Gateway</strong> - Route-based access control</li>
 *   <li><strong>Services</strong> - Endpoint-level validation</li>
 *   <li><strong>JWT</strong> - Access context propagation</li>
 *   <li><strong>Audit</strong> - Access logging and monitoring</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MicroserviceAccessService {

  private static final Logger logger = LoggerFactory.getLogger(MicroserviceAccessService.class);

  private final UserRepository userRepository;
  private final ConfigurableFeatureService configurableFeatureService;
  private final PlatformConfigurationProperties platformConfig;

  public MicroserviceAccessService(final UserRepository userRepository,
                                   final ConfigurableFeatureService configurableFeatureService,
                                   final PlatformConfigurationProperties platformConfig) {
    this.userRepository = userRepository;
    this.configurableFeatureService = configurableFeatureService;
    this.platformConfig = platformConfig;
  }

  /**
   * Get all microservices that a user has access to.
   * 
   * @param userId the user ID to check
   * @return set of microservice names the user can access
   */
  public Set<String> getUserMicroservices(Long userId) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return Set.of();
    }
    
    return getUserMicroservices(user.get());
  }

  /**
   * Get all microservices that a user has access to.
   * 
   * @param user the user to check
   * @return set of microservice names the user can access
   */
  public Set<String> getUserMicroservices(User user) {
    var userFeatureCodes = configurableFeatureService.getUserFeatures(user);
    
    return userFeatureCodes.stream()
        .map(featureCode -> platformConfig.features().getFeature(featureCode))
        .filter(featureDefinition -> featureDefinition != null)
        .flatMap(featureDefinition -> featureDefinition.microservices().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Check if a user has access to a specific microservice.
   * 
   * @param userId the user ID to check
   * @param microservice the microservice name
   * @return true if user has access to the microservice
   */
  public boolean hasMicroserviceAccess(Long userId, String microservice) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return false;
    }
    
    return hasMicroserviceAccess(user.get(), microservice);
  }

  /**
   * Check if a user has access to a specific microservice.
   * 
   * @param user the user to check
   * @param microservice the microservice name
   * @return true if user has access to the microservice
   */
  public boolean hasMicroserviceAccess(User user, String microservice) {
    var accessibleServices = getUserMicroservices(user);
    return accessibleServices.contains(microservice);
  }

  /**
   * Check if a user has access to a specific route/endpoint.
   * 
   * @param userId the user ID to check
   * @param route the route/endpoint path
   * @return true if user has access to the route
   */
  public boolean hasRouteAccess(Long userId, String route) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return false;
    }
    
    return hasRouteAccess(user.get(), route);
  }

  /**
   * Check if a user has access to a specific route/endpoint.
   * 
   * @param user the user to check
   * @param route the route/endpoint path
   * @return true if user has access to the route
   */
  public boolean hasRouteAccess(User user, String route) {
    // Find the feature that protects this route
    var featureCode = configurableFeatureService.findFeatureByRoute(route);
    if (featureCode == null) {
      // Route not protected by any feature - allow access
      return true;
    }
    
    return configurableFeatureService.hasFeatureAccess(user, featureCode);
  }

  /**
   * Get the feature that protects a specific route.
   * 
   * @param route the route/endpoint path
   * @return the feature code that protects the route, or null if not protected
   */
  public String getRouteFeature(String route) {
    return configurableFeatureService.findFeatureByRoute(route);
  }

  /**
   * Get all routes that a user has access to.
   * 
   * @param userId the user ID to check
   * @return set of route patterns the user can access
   */
  public Set<String> getUserRoutes(Long userId) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return Set.of();
    }
    
    return getUserRoutes(user.get());
  }

  /**
   * Get all routes that a user has access to.
   * 
   * @param user the user to check
   * @return set of route patterns the user can access
   */
  public Set<String> getUserRoutes(User user) {
    var userFeatureCodes = configurableFeatureService.getUserFeatures(user);
    
    return userFeatureCodes.stream()
        .map(featureCode -> platformConfig.features().getFeature(featureCode))
        .filter(featureDefinition -> featureDefinition != null)
        .flatMap(featureDefinition -> featureDefinition.routePatterns().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Get microservice access summary for a user.
   * 
   * @param userId the user ID to get summary for
   * @return access summary with features, microservices, and routes
   */
  public MicroserviceAccessSummary getMicroserviceAccessSummary(Long userId) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return new MicroserviceAccessSummary(userId, null, List.of(), Set.of(), Set.of());
    }
    
    return getMicroserviceAccessSummary(user.get());
  }

  /**
   * Get microservice access summary for a user.
   * 
   * @param user the user to get summary for
   * @return access summary with features, microservices, and routes
   */
  public MicroserviceAccessSummary getMicroserviceAccessSummary(User user) {
    var userFeatureCodes = configurableFeatureService.getUserFeatures(user);
    var microservices = getUserMicroservices(user);
    var routes = getUserRoutes(user);
    
    var featureSummaries = userFeatureCodes.stream()
        .map(featureCode -> {
          var featureDefinition = platformConfig.features().getFeature(featureCode);
          return new FeatureMicroserviceSummary(
              featureCode,
              featureDefinition.displayName(),
              featureDefinition.microservices(),
              featureDefinition.routePatterns()
          );
        })
        .toList();
    
    return new MicroserviceAccessSummary(
        user.getId(),
        user.getUsername(),
        featureSummaries,
        microservices,
        routes
    );
  }

  /**
   * Get all users who have access to a specific microservice.
   * 
   * @param microservice the microservice name
   * @return list of users with access to the microservice
   */
  public List<User> getUsersWithMicroserviceAccess(String microservice) {
    var featuresForService = configurableFeatureService.getFeaturesForMicroservice(microservice);
    
    if (featuresForService.isEmpty()) {
      logger.warn("No features found for microservice: {}", microservice);
      return List.of();
    }
    
    // Get users who have access to any of the features that use this microservice
    return featuresForService.stream()
        .flatMap(featureCode -> configurableFeatureService.getUsersWithFeature(featureCode).stream())
        .distinct()
        .toList();
  }

  /**
   * Get microservice usage statistics.
   * 
   * @return map of microservice names to user counts
   */
  public Map<String, Long> getMicroserviceUsageStatistics() {
    return platformConfig.features().getComposableFeatures().entrySet().stream()
        .flatMap(entry -> entry.getValue().microservices().stream())
        .distinct()
        .collect(Collectors.toMap(
            microservice -> microservice,
            microservice -> (long) getUsersWithMicroserviceAccess(microservice).size()
        ));
  }

  /**
   * Validate that a user can access a specific microservice endpoint.
   * This method provides detailed validation with logging for audit purposes.
   * 
   * @param userId the user ID to validate
   * @param microservice the microservice name
   * @param endpoint the specific endpoint path
   * @return validation result with details
   */
  public MicroserviceAccessValidation validateAccess(Long userId, String microservice, String endpoint) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return new MicroserviceAccessValidation(
          false, "User not found", null, null, null
      );
    }
    
    return validateAccess(user.get(), microservice, endpoint);
  }

  /**
   * Validate that a user can access a specific microservice endpoint.
   * 
   * @param user the user to validate
   * @param microservice the microservice name
   * @param endpoint the specific endpoint path
   * @return validation result with details
   */
  public MicroserviceAccessValidation validateAccess(User user, String microservice, String endpoint) {
    // Check microservice access
    if (!hasMicroserviceAccess(user, microservice)) {
      return new MicroserviceAccessValidation(
          false, 
          "User does not have access to microservice: " + microservice,
          null, microservice, endpoint
      );
    }
    
    // Check route access
    if (!hasRouteAccess(user, endpoint)) {
      var featureCode = getRouteFeature(endpoint);
      
      return new MicroserviceAccessValidation(
          false,
          "User does not have access to endpoint: " + endpoint,
          featureCode, microservice, endpoint
      );
    }
    
    // Access granted
    var featureCode = getRouteFeature(endpoint);
    
    logger.debug("Access granted for user {} to {}{}", 
        user.getUsername(), microservice, endpoint);
    
    return new MicroserviceAccessValidation(
        true, "Access granted", featureCode, microservice, endpoint
    );
  }

  /**
   * Summary of microservice access for a user.
   */
  public record MicroserviceAccessSummary(
      Long userId,
      String username,
      List<FeatureMicroserviceSummary> features,
      Set<String> accessibleMicroservices,
      Set<String> accessibleRoutes
  ) {}

  /**
   * Summary of a feature's microservice mappings.
   */
  public record FeatureMicroserviceSummary(
      String featureCode,
      String featureName,
      List<String> microservices,
      List<String> routePatterns
  ) {}

  /**
   * Result of microservice access validation.
   */
  public record MicroserviceAccessValidation(
      boolean hasAccess,
      String message,
      String requiredFeature,
      String microservice,
      String endpoint
  ) {}
}