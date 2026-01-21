package com.iqscaffold.userservice.usermanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.iqscaffold.userservice.shared.ConfigurableFeatureService;
import com.iqscaffold.userservice.shared.MicroserviceAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified REST controller for managing composable feature access.
 *
 * <p>This controller provides a unified API for managing access to all composable
 * features (CRM, Billing, API, etc.) rather than having separate controllers for
 * each feature. This approach provides:
 * <ul>
 *   <li>Consistent API patterns across all features</li>
 *   <li>Simplified client integration</li>
 *   <li>Centralized feature access management</li>
 *   <li>Scalable architecture for new features</li>
 * </ul>
 *
 * <h3>Supported Features</h3>
 * <ul>
 *   <li><strong>crm</strong> - Customer Relationship Management</li>
 *   <li><strong>billing</strong> - Payments and subscriptions</li>
 *   <li><strong>api</strong> - Platform API access</li>
 * </ul>
 *
 * <h3>Security Requirements</h3>
 * <ul>
 *   <li>All endpoints require ADMIN or SUPER_ADMIN authority</li>
 *   <li>Feature access management is restricted to administrators</li>
 *   <li>Operations are logged for audit purposes</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users/features")
@Tag(name = "Feature Access Management", description = "Unified management of composable feature access")
public class FeatureAccessController {

  private static final Logger logger = LoggerFactory.getLogger(FeatureAccessController.class);

  private final ConfigurableFeatureService configurableFeatureService;
  private final MicroserviceAccessService microserviceAccessService;
  private final UserRepository userRepository;

  public FeatureAccessController(final ConfigurableFeatureService configurableFeatureService,
                                 final MicroserviceAccessService microserviceAccessService,
                                 final UserRepository userRepository) {
    this.configurableFeatureService = configurableFeatureService;
    this.microserviceAccessService = microserviceAccessService;
    this.userRepository = userRepository;
  }

  @Operation(
      summary = "Enable a feature for a user",
      description = "Grants access to the specified feature by adding the required authorities"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Feature enabled successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "400", description = "Invalid feature or feature not composable"),
      @ApiResponse(responseCode = "409", description = "User already has feature access"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @PostMapping("/{userId}/{featureCode}/enable")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<FeatureAccessResponse> enableFeature(
      @Parameter(description = "User ID to enable feature for", required = true)
      @PathVariable Long userId,
      @Parameter(description = "Feature code (crm, billing, api)", required = true)
      @PathVariable String featureCode) {

    logger.info("Enabling feature {} for user ID: {}", featureCode, userId);

    // Validate feature
    var featureDefinition = configurableFeatureService.getFeatureDefinition(featureCode);
    if (featureDefinition == null) {
      return ResponseEntity.badRequest().body(new FeatureAccessResponse(
          userId, null, featureCode, false, "Invalid feature code: " + featureCode
      ));
    }

    if (!featureDefinition.composable()) {
      return ResponseEntity.badRequest().body(new FeatureAccessResponse(
          userId, null, featureCode, false, "Feature is not composable: " + featureDefinition.displayName()
      ));
    }

    // Validate user
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var enabled = configurableFeatureService.enableFeature(userId, featureCode);
    if (!enabled) {
      return ResponseEntity.status(409).body(new FeatureAccessResponse(
          userId, user.get().getUsername(), featureCode, true,
          "User already has access to " + featureDefinition.displayName()
      ));
    }

    return ResponseEntity.ok(new FeatureAccessResponse(
        userId, user.get().getUsername(), featureCode, true,
        featureDefinition.displayName() + " access enabled successfully"
    ));
  }

  @Operation(
      summary = "Disable a feature for a user",
      description = "Removes access to the specified feature by removing associated authorities"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Feature disabled successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "400", description = "Invalid feature or feature not composable"),
      @ApiResponse(responseCode = "409", description = "User doesn't have feature access"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @DeleteMapping("/{userId}/{featureCode}/disable")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<FeatureAccessResponse> disableFeature(
      @Parameter(description = "User ID to disable feature for", required = true)
      @PathVariable Long userId,
      @Parameter(description = "Feature code (crm, billing, api)", required = true)
      @PathVariable String featureCode) {

    logger.info("Disabling feature {} for user ID: {}", featureCode, userId);

    // Validate feature
    var featureDefinition = configurableFeatureService.getFeatureDefinition(featureCode);
    if (featureDefinition == null) {
      return ResponseEntity.badRequest().body(new FeatureAccessResponse(
          userId, null, featureCode, false, "Invalid feature code: " + featureCode
      ));
    }

    if (!featureDefinition.composable()) {
      return ResponseEntity.badRequest().body(new FeatureAccessResponse(
          userId, null, featureCode, false, "Feature is not composable: " + featureDefinition.displayName()
      ));
    }

    // Validate user
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var disabled = configurableFeatureService.disableFeature(userId, featureCode);
    if (!disabled) {
      return ResponseEntity.status(409).body(new FeatureAccessResponse(
          userId, user.get().getUsername(), featureCode, false,
          "User doesn't have access to " + featureDefinition.displayName() + " to disable"
      ));
    }

    return ResponseEntity.ok(new FeatureAccessResponse(
        userId, user.get().getUsername(), featureCode, false,
        featureDefinition.displayName() + " access disabled successfully"
    ));
  }

  @Operation(
      summary = "Check if a user has access to a feature",
      description = "Verifies whether the specified user has access to the given feature"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Feature access status retrieved"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "400", description = "Invalid feature code"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/{userId}/{featureCode}/check")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<FeatureAccessResponse> checkFeatureAccess(
      @Parameter(description = "User ID to check feature access for", required = true)
      @PathVariable Long userId,
      @Parameter(description = "Feature code (crm, billing, api)", required = true)
      @PathVariable String featureCode) {

    // Validate feature
    var featureDefinition = configurableFeatureService.getFeatureDefinition(featureCode);
    if (featureDefinition == null) {
      return ResponseEntity.badRequest().body(new FeatureAccessResponse(
          userId, null, featureCode, false, "Invalid feature code: " + featureCode
      ));
    }

    // Validate user
    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var hasAccess = configurableFeatureService.hasFeatureAccess(userId, featureCode);
    var message = hasAccess
        ? "User has access to " + featureDefinition.displayName()
        : "User does not have access to " + featureDefinition.displayName();

    return ResponseEntity.ok(new FeatureAccessResponse(
        userId, user.get().getUsername(), featureCode, hasAccess, message
    ));
  }

  @Operation(
      summary = "Get all features for a user",
      description = "Retrieves a list of all features the user has access to"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User features retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/{userId}")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<UserFeaturesResponse> getUserFeatures(
      @Parameter(description = "User ID to get features for", required = true)
      @PathVariable Long userId) {

    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var userFeatureCodes = configurableFeatureService.getUserFeatures(userId);
    var featureSummaries = userFeatureCodes.stream()
        .map(featureCode -> {
          var featureDefinition = configurableFeatureService.getFeatureDefinition(featureCode);
          return new FeatureSummary(
              featureCode,
              featureDefinition.displayName(),
              featureDefinition.description(),
              true
          );
        })
        .toList();

    return ResponseEntity.ok(new UserFeaturesResponse(
        userId,
        user.get().getUsername(),
        featureSummaries.size(),
        featureSummaries
    ));
  }

  @Operation(
      summary = "Update user features (bulk operation)",
      description = "Enable or disable multiple features for a user in a single operation"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Features updated successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "400", description = "Invalid request or feature codes"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @PutMapping("/{userId}")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<BulkFeatureUpdateResponse> updateUserFeatures(
      @Parameter(description = "User ID to update features for", required = true)
      @PathVariable Long userId,
      @Valid @RequestBody BulkFeatureUpdateRequest request) {

    logger.info("Bulk updating features for user ID: {} - enable: {}, disable: {}",
        userId, request.enableFeatures(), request.disableFeatures());

    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Validate feature codes
    var invalidFeatures = new java.util.ArrayList<String>();
    invalidFeatures.addAll(request.enableFeatures().stream()
        .filter(code -> configurableFeatureService.getFeatureDefinition(code) == null)
        .toList());
    invalidFeatures.addAll(request.disableFeatures().stream()
        .filter(code -> configurableFeatureService.getFeatureDefinition(code) == null)
        .toList());

    if (!invalidFeatures.isEmpty()) {
      return ResponseEntity.badRequest().body(new BulkFeatureUpdateResponse(
          userId, user.get().getUsername(), 0, 0,
          "Invalid feature codes: " + String.join(", ", invalidFeatures)
      ));
    }

    // Check if features are composable
    var nonComposableFeatures = new java.util.ArrayList<String>();
    nonComposableFeatures.addAll(request.enableFeatures().stream()
        .filter(code -> {
          var def = configurableFeatureService.getFeatureDefinition(code);
          return def != null && !def.composable();
        })
        .toList());
    nonComposableFeatures.addAll(request.disableFeatures().stream()
        .filter(code -> {
          var def = configurableFeatureService.getFeatureDefinition(code);
          return def != null && !def.composable();
        })
        .toList());

    if (!nonComposableFeatures.isEmpty()) {
      return ResponseEntity.badRequest().body(new BulkFeatureUpdateResponse(
          userId, user.get().getUsername(), 0, 0,
          "Non-composable features: " + String.join(", ", nonComposableFeatures)
      ));
    }

    // Perform bulk operations
    var enabledCount = 0;
    var disabledCount = 0;

    for (final var featureCode : request.enableFeatures()) {
      if (configurableFeatureService.enableFeature(userId, featureCode)) {
        enabledCount++;
      }
    }

    for (final var featureCode : request.disableFeatures()) {
      if (configurableFeatureService.disableFeature(userId, featureCode)) {
        disabledCount++;
      }
    }

    var message = String.format("Enabled %d features, disabled %d features",
        enabledCount, disabledCount);

    return ResponseEntity.ok(new BulkFeatureUpdateResponse(
        userId, user.get().getUsername(), enabledCount, disabledCount, message
    ));
  }

  @Operation(
      summary = "List all available features",
      description = "Retrieves information about all composable features available in the platform"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Available features retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/available")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<AvailableFeaturesResponse> getAvailableFeatures() {

    var composableFeatures = configurableFeatureService.getComposableFeatures();
    var featureDetails = composableFeatures.entrySet().stream()
        .map(entry -> new FeatureDetail(
            entry.getKey(),
            entry.getValue().displayName(),
            entry.getValue().description(),
            entry.getValue().composable(),
            entry.getValue().requiredAuthorities(),
            entry.getValue().dependencies()
        ))
        .toList();

    return ResponseEntity.ok(new AvailableFeaturesResponse(
        featureDetails.size(),
        featureDetails
    ));
  }

  @Operation(
      summary = "Get microservice access for a user",
      description = "Retrieves microservice access information including accessible services and routes"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Microservice access retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/{userId}/microservices")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<MicroserviceAccessResponse> getUserMicroserviceAccess(
      @Parameter(description = "User ID to get microservice access for", required = true)
      @PathVariable Long userId) {

    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var accessSummary = microserviceAccessService.getMicroserviceAccessSummary(userId);

    return ResponseEntity.ok(new MicroserviceAccessResponse(
        accessSummary.userId(),
        accessSummary.username(),
        accessSummary.features().size(),
        accessSummary.accessibleMicroservices().size(),
        accessSummary.accessibleRoutes().size(),
        accessSummary.features(),
        accessSummary.accessibleMicroservices().stream().toList(),
        accessSummary.accessibleRoutes().stream().toList()
    ));
  }

  @Operation(
      summary = "Validate microservice access",
      description = "Validates if a user can access a specific microservice endpoint"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access validation completed"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/{userId}/microservices/{microservice}/validate")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<MicroserviceValidationResponse> validateMicroserviceAccess(
      @Parameter(description = "User ID to validate access for", required = true)
      @PathVariable Long userId,
      @Parameter(description = "Microservice name", required = true)
      @PathVariable String microservice,
      @Parameter(description = "Endpoint path to validate", required = false)
      @RequestParam(required = false) String endpoint) {

    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var validation = microserviceAccessService.validateAccess(
        userId, microservice, endpoint != null ? endpoint : "/"
    );

    return ResponseEntity.ok(new MicroserviceValidationResponse(
        userId,
        user.get().getUsername(),
        microservice,
        endpoint,
        validation.hasAccess(),
        validation.message(),
        validation.requiredFeature()
    ));
  }

  @Operation(
      summary = "Get users with microservice access",
      description = "Retrieves all users who have access to a specific microservice"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Microservice users retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/microservices/{microservice}/users")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<MicroserviceUsersResponse> getMicroserviceUsers(
      @Parameter(description = "Microservice name", required = true)
      @PathVariable String microservice) {

    var users = microserviceAccessService.getUsersWithMicroserviceAccess(microservice);
    var userSummaries = users.stream()
        .map(user -> new MicroserviceUserSummary(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.isActive()
        ))
        .toList();

    return ResponseEntity.ok(new MicroserviceUsersResponse(
        microservice,
        userSummaries.size(),
        userSummaries
    ));
  }

  @Operation(
      summary = "Get microservice usage statistics",
      description = "Retrieves usage statistics for all microservices"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Microservice statistics retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/microservices/statistics")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<MicroserviceStatisticsResponse> getMicroserviceStatistics() {

    var statistics = microserviceAccessService.getMicroserviceUsageStatistics();
    var microserviceStats = statistics.entrySet().stream()
        .map(entry -> new MicroserviceStatistic(
            entry.getKey(),
            entry.getValue(),
            configurableFeatureService.getFeaturesForMicroservice(entry.getKey())
        ))
        .toList();

    return ResponseEntity.ok(new MicroserviceStatisticsResponse(
        microserviceStats.size(),
        microserviceStats.stream().mapToLong(MicroserviceStatistic::userCount).sum(),
        microserviceStats
    ));
  }

  @Operation(
      summary = "Get users with access to a feature",
      description = "Retrieves a list of all users who have access to the specified feature"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Feature users retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid feature code"),
      @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  @GetMapping("/{featureCode}/users")
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<FeatureUsersResponse> getFeatureUsers(
      @Parameter(description = "Feature code (crm, billing, api)", required = true)
      @PathVariable String featureCode) {

    var featureDefinition = configurableFeatureService.getFeatureDefinition(featureCode);
    if (featureDefinition == null) {
      return ResponseEntity.badRequest().build();
    }

    var featureUsers = configurableFeatureService.getUsersWithFeature(featureCode);

    var userSummaries = featureUsers.stream()
        .map(user -> new FeatureUserSummary(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.isActive()
        ))
        .toList();

    return ResponseEntity.ok(new FeatureUsersResponse(
        featureCode,
        featureDefinition.displayName(),
        userSummaries.size(),
        userSummaries
    ));
  }

  // DTOs

  public record FeatureAccessResponse(
      Long userId,
      String username,
      String featureCode,
      boolean hasAccess,
      String message
  ) {
  }

  public record UserFeaturesResponse(
      Long userId,
      String username,
      int featureCount,
      List<FeatureSummary> features
  ) {
  }

  public record FeatureSummary(
      String code,
      String displayName,
      String description,
      boolean enabled
  ) {
  }

  public record BulkFeatureUpdateRequest(
      @NotNull @NotEmpty List<String> enableFeatures,
      @NotNull @NotEmpty List<String> disableFeatures
  ) {
  }

  public record BulkFeatureUpdateResponse(
      Long userId,
      String username,
      int featuresEnabled,
      int featuresDisabled,
      String message
  ) {
  }

  public record AvailableFeaturesResponse(
      int totalCount,
      List<FeatureDetail> features
  ) {
  }

  public record FeatureDetail(
      String code,
      String displayName,
      String description,
      boolean composable,
      List<String> requiredAuthorities,
      List<String> dependencies
  ) {
  }

  public record FeatureUsersResponse(
      String featureCode,
      String featureName,
      int totalCount,
      List<FeatureUserSummary> users
  ) {
  }

  public record FeatureUserSummary(
      Long id,
      String username,
      String email,
      String fullName,
      boolean active
  ) {
  }

  // Microservice Access DTOs

  public record MicroserviceAccessResponse(
      Long userId,
      String username,
      int featureCount,
      int microserviceCount,
      int routeCount,
      List<MicroserviceAccessService.FeatureMicroserviceSummary> features,
      List<String> accessibleMicroservices,
      List<String> accessibleRoutes
  ) {
  }

  public record MicroserviceValidationResponse(
      Long userId,
      String username,
      String microservice,
      String endpoint,
      boolean hasAccess,
      String message,
      String requiredFeature
  ) {
  }

  public record MicroserviceUsersResponse(
      String microservice,
      int totalCount,
      List<MicroserviceUserSummary> users
  ) {
  }

  public record MicroserviceUserSummary(
      Long id,
      String username,
      String email,
      String fullName,
      boolean active
  ) {
  }

  public record MicroserviceStatisticsResponse(
      int totalMicroservices,
      long totalUsers,
      List<MicroserviceStatistic> statistics
  ) {
  }

  public record MicroserviceStatistic(
      String microservice,
      long userCount,
      List<String> features
  ) {
  }
}
