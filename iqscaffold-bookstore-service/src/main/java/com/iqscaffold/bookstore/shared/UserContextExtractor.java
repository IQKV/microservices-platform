package com.iqscaffold.bookstore.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Extracts user context from JWT tokens issued by IQ Scaffold User Service.
 *
 * <p>This component parses JWT claims and converts them into a structured UserContext
 * object that can be used throughout the application for authorization and audit logging.
 *
 * <p><b>JWT Token Structure (from User Service):</b>
 * <pre>
 * {
 *   "iss": "iqscaffold-user-service",
 *   "sub": "1",
 *   "userId": 1,
 *   "username": "johndoe",
 *   "email": "john@example.com",
 *   "roles": ["USER", "ADMIN"],
 *   "permissions": ["read:profile", "update:profile"],
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "tenantId": "default",
 *   "department": "Engineering",
 *   "organizationId": "org-123"
 * }
 * </pre>
 *
 * <p><b>Extracted UserContext Fields:</b>
 * <ul>
 *   <li><b>userId</b> - Unique user identifier</li>
 *   <li><b>username</b> - User's login name</li>
 *   <li><b>email</b> - User's email address</li>
 *   <li><b>roles</b> - User's roles (e.g., USER, ADMIN, SUPERADMIN)</li>
 *   <li><b>permissions</b> - Fine-grained permissions</li>
 *   <li><b>department</b> - User's department</li>
 *   <li><b>organizationId</b> - User's organization</li>
 *   <li><b>customClaims</b> - Additional custom claims</li>
 * </ul>
 *
 * <p><b>Usage in Controllers:</b>
 * <pre>
 * {@code @PostMapping}
 * public ResponseEntity<BookDto> createBook(
 *     {@code @RequestBody} CreateBookRequest request,
 *     {@code @RequestAttribute("userContext")} UserContext userContext) {
 *   // userContext is automatically extracted by JwtAuthenticationFilter
 *   return CatalogService.createBook(request, userContext);
 * }
 * </pre>
 *
 * @see UserContext
 * @see JwtAuthenticationFilter
 */
@Component
public class UserContextExtractor {

  private static final Logger logger = LoggerFactory.getLogger(UserContextExtractor.class);

  private final JwtDecoder jwtDecoder;

  public UserContextExtractor(final JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  public UserContext extractFromJwt(String jwtToken) {
    try {
      var jwt = jwtDecoder.decode(jwtToken);
      return extractFromJwt(jwt);
    } catch (final Exception e) {
      logger.error("Failed to decode JWT token", e);
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public UserContext extractFromJwt(Jwt jwt) {
    var claims = jwt.getClaims();

    var userId = extractUserId(claims);
    var username = extractUsername(claims);
    var email = extractEmail(claims);
    var roles = extractRoles(claims);
    var permissions = extractPermissions(claims);
    var department = extractDepartment(claims);
    var organizationId = extractOrganizationId(claims);
    var customClaims = extractCustomClaims(claims);

    logger.debug("Extracted user context for user: {} with roles: {}", username, roles);

    return new UserContext(
        userId,
        username,
        email,
        roles,
        permissions,
        department,
        organizationId,
        customClaims
    );
  }

  private Long extractUserId(Map<String, Object> claims) {
    var userIdClaim = claims.get(BookstoreConstants.JwtClaims.USER_ID);
    if (userIdClaim == null) {
      userIdClaim = claims.get(BookstoreConstants.JwtClaims.SUBJECT);
    }

    if (userIdClaim instanceof Number number) {
      return number.longValue();
    } else if (userIdClaim instanceof String str) {
      try {
        return Long.parseLong(str);
      } catch (final NumberFormatException e) {
        logger.warn("Could not parse userId from string: {}", str);
        return null;
      }
    }

    return null;
  }

  private String extractUsername(Map<String, Object> claims) {
    var username = (String) claims.get(BookstoreConstants.JwtClaims.USERNAME);
    if (username == null) {
      username = (String) claims.get(BookstoreConstants.JwtClaims.PREFERRED_USERNAME);
    }
    if (username == null) {
      username = (String) claims.get(BookstoreConstants.JwtClaims.SUBJECT);
    }
    return username;
  }

  private String extractEmail(Map<String, Object> claims) {
    return (String) claims.get(BookstoreConstants.JwtClaims.EMAIL);
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractRoles(Map<String, Object> claims) {
    // Try different claim names for roles
    var rolesObj = claims.get(BookstoreConstants.JwtClaims.ROLES);
    if (rolesObj == null) {
      rolesObj = claims.get(BookstoreConstants.JwtClaims.AUTHORITIES);
    }
    if (rolesObj == null) {
      // Check realm_access for Keycloak
      var realmAccess = (Map<String, Object>) claims.get(BookstoreConstants.JwtClaims.REALM_ACCESS);
      if (realmAccess != null) {
        rolesObj = realmAccess.get(BookstoreConstants.JwtClaims.ROLES);
      }
    }

    try {
      if (rolesObj instanceof Iterable<?> roles) {
        return StreamSupport.stream(((Iterable<Object>) roles).spliterator(), false)
            .map(Object::toString)
            .collect(Collectors.toSet());
      }
    } catch (final Exception e) {
      logger.warn("Error extracting roles from claims", e);
    }

    return Set.of();
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractPermissions(Map<String, Object> claims) {
    var permissionsObj = claims.get(BookstoreConstants.JwtClaims.PERMISSIONS);
    if (permissionsObj == null) {
      permissionsObj = claims.get(BookstoreConstants.JwtClaims.SCOPE);
    }

    if (permissionsObj instanceof String scopeString) {
      return Set.of(scopeString.split(" "));
    }

    try {
      if (permissionsObj instanceof Iterable<?> permissions) {
        return StreamSupport.stream(((Iterable<Object>) permissions).spliterator(), false)
            .map(Object::toString)
            .collect(Collectors.toSet());
      }
    } catch (final Exception e) {
      logger.warn("Error extracting permissions from claims", e);
    }

    return Set.of();
  }

  private String extractDepartment(Map<String, Object> claims) {
    return (String) claims.get(BookstoreConstants.JwtClaims.DEPARTMENT);
  }

  private String extractOrganizationId(Map<String, Object> claims) {
    return (String) claims.get(BookstoreConstants.JwtClaims.ORGANIZATION_ID);
  }

  private Map<String, Object> extractCustomClaims(Map<String, Object> claims) {
    var customClaims = new HashMap<String, Object>();

    // Standard JWT claims to exclude
    var standardClaims = Set.of(
        BookstoreConstants.JwtClaims.ISSUER,
        BookstoreConstants.JwtClaims.SUBJECT,
        BookstoreConstants.JwtClaims.AUDIENCE,
        BookstoreConstants.JwtClaims.EXPIRATION,
        BookstoreConstants.JwtClaims.NOT_BEFORE,
        BookstoreConstants.JwtClaims.ISSUED_AT,
        BookstoreConstants.JwtClaims.JWT_ID,
        BookstoreConstants.JwtClaims.USER_ID,
        BookstoreConstants.JwtClaims.USERNAME,
        BookstoreConstants.JwtClaims.PREFERRED_USERNAME,
        BookstoreConstants.JwtClaims.EMAIL,
        BookstoreConstants.JwtClaims.ROLES,
        BookstoreConstants.JwtClaims.AUTHORITIES,
        BookstoreConstants.JwtClaims.PERMISSIONS,
        BookstoreConstants.JwtClaims.SCOPE,
        BookstoreConstants.JwtClaims.DEPARTMENT,
        BookstoreConstants.JwtClaims.ORGANIZATION_ID,
        BookstoreConstants.JwtClaims.REALM_ACCESS
    );

    claims.entrySet().stream()
        .filter(entry -> !standardClaims.contains(entry.getKey()))
        .forEach(entry -> customClaims.put(entry.getKey(), entry.getValue()));

    return customClaims;
  }
}
