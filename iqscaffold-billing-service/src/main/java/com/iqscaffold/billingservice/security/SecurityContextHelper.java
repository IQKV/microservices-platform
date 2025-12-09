package com.iqscaffold.billingservice.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper class for accessing user context from Spring Security context.
 *
 * <p>Provides convenient methods to retrieve UserContext in application services
 * without directly accessing HttpServletRequest or SecurityContextHolder.
 */
public final class SecurityContextHelper {

  private SecurityContextHelper() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Get the current UserContext from the request.
   *
   * <p>The UserContext is set by JwtAuthenticationFilter as a request attribute.
   *
   * @return the current UserContext, or null if not authenticated
   */
  public static UserContext getCurrentUserContext() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes != null) {
      HttpServletRequest request = attributes.getRequest();
      Object userContext = request.getAttribute("userContext");

      if (userContext instanceof UserContext) {
        return (UserContext) userContext;
      }
    }

    // Fallback: extract from SecurityContext if not in request attributes
    return extractUserContextFromSecurityContext();
  }

  /**
   * Get the current UserContext, throwing an exception if not authenticated.
   *
   * @return the current UserContext
   * @throws IllegalStateException if no user context is available
   */
  public static UserContext getCurrentUserContextOrThrow() {
    UserContext userContext = getCurrentUserContext();

    if (userContext == null) {
      throw new IllegalStateException("No user context available - user not authenticated");
    }

    return userContext;
  }

  /**
   * Check if a user is currently authenticated.
   *
   * @return true if a user is authenticated, false otherwise
   */
  public static boolean isAuthenticated() {
    return getCurrentUserContext() != null;
  }

  /**
   * Get the current user's ID.
   *
   * @return the user ID, or null if not authenticated
   */
  public static Long getCurrentUserId() {
    UserContext userContext = getCurrentUserContext();
    return userContext != null ? userContext.userId() : null;
  }

  /**
   * Get the current user's username.
   *
   * @return the username, or null if not authenticated
   */
  public static String getCurrentUsername() {
    UserContext userContext = getCurrentUserContext();
    return userContext != null ? userContext.username() : null;
  }

  /**
   * Get the current user's tenant ID.
   *
   * @return the tenant ID, or null if not authenticated
   */
  public static String getCurrentTenantId() {
    UserContext userContext = getCurrentUserContext();
    return userContext != null ? userContext.tenantId() : null;
  }

  /**
   * Check if the current user has a specific authority.
   *
   * @param authority the authority to check
   * @return true if the user has the authority, false otherwise
   */
  public static boolean hasAuthority(String authority) {
    UserContext userContext = getCurrentUserContext();
    return userContext != null && userContext.hasAuthority(authority);
  }

  /**
   * Check if the current user is an admin.
   *
   * @return true if the user is an admin, false otherwise
   */
  public static boolean isAdmin() {
    UserContext userContext = getCurrentUserContext();
    return userContext != null && userContext.isAdmin();
  }

  /**
   * Extract UserContext from Spring Security context.
   * This is a fallback method when UserContext is not in request attributes.
   *
   * @return the UserContext, or null if not available
   */
  private static UserContext extractUserContextFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
      Jwt jwt = jwtAuthToken.getToken();

      // Extract claims
      Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
      String username = jwt.getClaim(JwtClaimNames.USERNAME);
      String email = jwt.getClaim(JwtClaimNames.EMAIL);
      String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
      String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
      String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

      // Extract authorities
      Object rolesObj = jwt.getClaim(JwtClaimNames.ROLES);
      java.util.Set<String> authorities = java.util.Collections.emptySet();

      if (rolesObj instanceof java.util.List<?> list) {
        authorities = new java.util.HashSet<>(list.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList());
      } else if (rolesObj instanceof java.util.Set<?> set) {
        authorities = new java.util.HashSet<>(set.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList());
      }

      return new UserContext(userId, username, email, authorities, tenantId, firstName, lastName);
    }

    return null;
  }

  /**
   * Extract Long value from JWT claim.
   *
   * @param value the claim value
   * @return the Long value, or null if not a valid number
   */
  private static Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (final NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }
}
