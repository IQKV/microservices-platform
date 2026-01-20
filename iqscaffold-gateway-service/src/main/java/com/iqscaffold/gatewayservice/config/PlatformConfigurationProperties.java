package com.iqscaffold.gatewayservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Platform configuration properties for the gateway service.
 * 
 * <p>This configuration allows the gateway to enforce access control based on
 * configurable route patterns and authority requirements, making the platform
 * extensible without code changes.
 */
@ConfigurationProperties(prefix = "iqscaffold.platform")
@Validated
public record PlatformConfigurationProperties(
    @Valid @NotNull Security security
) {

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