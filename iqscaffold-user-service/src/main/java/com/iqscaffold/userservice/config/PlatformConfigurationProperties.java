package com.iqscaffold.userservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Platform configuration properties for extensible feature and microservice management.
 *
 * <p>This configuration allows the platform to be extended with new features, authorities,
 * and microservices without code changes. All platform behavior is driven by configuration.
 *
 * <h3>Extensibility Features</h3>
 * <ul>
 *   <li><strong>Dynamic Features</strong> - Features defined in configuration</li>
 *   <li><strong>Configurable Authorities</strong> - Authority hierarchies in YAML</li>
 *   <li><strong>Route Mappings</strong> - URL patterns mapped to features</li>
 *   <li><strong>Microservice Integration</strong> - Service-to-feature mappings</li>
 * </ul>
 *
 * <h3>Configuration Structure</h3>
 * <pre>
 * iqscaffold:
 *   platform:
 *     features:
 *       crm:
 *         displayName: "Customer Relationship Management"
 *         authorities: ["CRM_ACCESS", "CRM_LEAD_MANAGER"]
 *         microservices: ["contact-service", "lead-service"]
 *         routes: ["/api/&#42;/crm/&#42;&#42;"]
 * </pre>
 */
@ConfigurationProperties(prefix = "iqscaffold.platform")
@Validated
public record PlatformConfigurationProperties(
    @Valid @NotNull Features features,
    @Valid @NotNull Authorities authorities,
    @Valid @NotNull Security security
) {

  /**
   * Feature definitions with their associated authorities, microservices, and routes.
   */
  public record Features(
      @NotNull Map<@NotBlank String, @Valid FeatureDefinition> definitions
  ) {

    /**
     * Get a feature definition by code.
     */
    public FeatureDefinition getFeature(String code) {
      return definitions.get(code);
    }

    /**
     * Check if a feature exists.
     */
    public boolean hasFeature(String code) {
      return definitions.containsKey(code);
    }

    /**
     * Get all feature codes.
     */
    public Set<String> getFeatureCodes() {
      return definitions.keySet();
    }

    /**
     * Get all composable features.
     */
    public Map<String, FeatureDefinition> getComposableFeatures() {
      return definitions.entrySet().stream()
          .filter(entry -> entry.getValue().composable())
          .collect(java.util.stream.Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue
          ));
    }
  }

  /**
   * Individual feature definition.
   */
  public record FeatureDefinition(
      @NotBlank String displayName,
      @NotBlank String description,
      @NotEmpty List<@NotBlank String> requiredAuthorities,
      @NotNull List<@NotBlank String> allAuthorities,
      @NotNull List<@NotBlank String> microservices,
      @NotNull List<@NotBlank String> routePatterns,
      @NotNull List<@NotBlank String> dependencies,
      boolean composable,
      boolean enabled
  ) {

    /**
     * Get the primary authority for this feature.
     */
    public String getPrimaryAuthority() {
      return requiredAuthorities.isEmpty() ? null : requiredAuthorities.get(0);
    }

    /**
     * Check if this feature has a specific authority.
     */
    public boolean hasAuthority(String authority) {
      return allAuthorities.contains(authority);
    }

    /**
     * Check if this feature uses a specific microservice.
     */
    public boolean usesMicroservice(String microservice) {
      return microservices.contains(microservice);
    }

    /**
     * Check if a route matches this feature's patterns.
     */
    public boolean matchesRoute(String route) {
      return routePatterns.stream()
          .anyMatch(pattern -> matchesPattern(route, pattern));
    }

    /**
     * Check if a path matches a route pattern.
     */
    private boolean matchesPattern(String path, String pattern) {
      if (pattern.endsWith("/**")) {
        var prefix = pattern.substring(0, pattern.length() - 3);
        // Handle wildcard in the middle (e.g., /api/*/crm/**)
        if (prefix.contains("*")) {
          var regex = prefix.replace("*", "[^/]+");
          return path.matches(regex + ".*");
        }
        return path.startsWith(prefix);
      }
      return path.equals(pattern);
    }
  }

  /**
   * Authority definitions and hierarchies.
   */
  public record Authorities(
      @NotNull Map<@NotBlank String, @Valid AuthorityDefinition> definitions,
      @NotNull List<@NotBlank String> adminAuthorities,
      @NotNull List<@NotBlank String> defaultAuthorities
  ) {

    /**
     * Get an authority definition by name.
     */
    public AuthorityDefinition getAuthority(String name) {
      return definitions.get(name);
    }

    /**
     * Check if an authority exists.
     */
    public boolean hasAuthority(String name) {
      return definitions.containsKey(name);
    }

    /**
     * Check if an authority is an admin authority.
     */
    public boolean isAdminAuthority(String authority) {
      return adminAuthorities.contains(authority);
    }

    /**
     * Check if an authority is a default authority.
     */
    public boolean isDefaultAuthority(String authority) {
      return defaultAuthorities.contains(authority);
    }

    /**
     * Get all authority names.
     */
    public Set<String> getAuthorityNames() {
      return definitions.keySet();
    }
  }

  /**
   * Individual authority definition.
   */
  public record AuthorityDefinition(
      @NotBlank String displayName,
      @NotBlank String description,
      @NotBlank String category,
      boolean systemAuthority,
      int priority
  ) {

    /**
     * Check if this is a system-level authority.
     */
    public boolean isSystemAuthority() {
      return systemAuthority;
    }

    /**
     * Get the authority priority (higher number = higher priority).
     */
    public int getPriority() {
      return priority;
    }
  }

  /**
   * Security configuration for access control.
   */
  public record Security(
      @Valid @NotNull RouteProtection routeProtection,
      @Valid @NotNull AccessControl accessControl
  ) {

    /**
     * Route protection configuration.
     */
    public record RouteProtection(
        @NotNull List<@NotBlank String> publicPaths,
        @NotNull Map<@NotBlank String, @NotEmpty List<@NotBlank String>> protectedRoutes
    ) {

      /**
       * Check if a path is public.
       */
      public boolean isPublicPath(String path) {
        return publicPaths.stream()
            .anyMatch(publicPath -> {
              if (publicPath.endsWith("/**")) {
                var prefix = publicPath.substring(0, publicPath.length() - 3);
                return path.startsWith(prefix);
              }
              return path.equals(publicPath);
            });
      }

      /**
       * Get required authorities for a route.
       */
      public List<String> getRequiredAuthorities(String route) {
        return protectedRoutes.entrySet().stream()
            .filter(entry -> matchesPattern(route, entry.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(List.of());
      }

      /**
       * Check if a path matches a route pattern.
       */
      private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
          var prefix = pattern.substring(0, pattern.length() - 3);
          if (prefix.contains("*")) {
            var regex = prefix.replace("*", "[^/]+");
            return path.matches(regex + ".*");
          }
          return path.startsWith(prefix);
        }
        return path.equals(pattern);
      }
    }

    /**
     * Access control configuration.
     */
    public record AccessControl(
        boolean enableFeatureValidation,
        boolean enableMicroserviceValidation,
        boolean enableRouteValidation,
        @NotNull List<@NotBlank String> bypassAuthorities
    ) {

      /**
       * Check if an authority can bypass access controls.
       */
      public boolean canBypassAccess(String authority) {
        return bypassAuthorities.contains(authority);
      }
    }
  }
}
