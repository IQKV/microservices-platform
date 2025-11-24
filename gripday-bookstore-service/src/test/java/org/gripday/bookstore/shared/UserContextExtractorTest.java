package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextExtractor Tests")
class UserContextExtractorTest {

  @Mock
  private JwtDecoder jwtDecoder;

  private UserContextExtractor userContextExtractor;

  @BeforeEach
  void setUp() {
    userContextExtractor = new UserContextExtractor(jwtDecoder);
  }

  @Test
  @DisplayName("Should extract user context from JWT with all claims")
  void shouldExtractUserContextFromJwtWithAllClaims() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "email", "john@example.com",
        "roles", List.of("USER", "ADMIN"),
        "permissions", List.of("read:profile", "update:profile"),
        "department", "Engineering",
        "organizationId", "org-123",
        "customField", "customValue"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.userId()).isEqualTo(1L);
    assertThat(userContext.username()).isEqualTo("johndoe");
    assertThat(userContext.email()).isEqualTo("john@example.com");
    assertThat(userContext.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    assertThat(userContext.permissions()).containsExactlyInAnyOrder("read:profile", "update:profile");
    assertThat(userContext.department()).isEqualTo("Engineering");
    assertThat(userContext.organizationId()).isEqualTo("org-123");
    assertThat(userContext.customClaims()).containsEntry("customField", "customValue");
  }

  @Test
  @DisplayName("Should extract userId from sub claim when userId is missing")
  void shouldExtractUserIdFromSubClaimWhenUserIdIsMissing() {
    // Arrange
    var claims = Map.<String, Object>of(
        "sub", "42",
        "username", "johndoe"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.userId()).isEqualTo(42L);
  }

  @Test
  @DisplayName("Should extract userId as Long from Number")
  void shouldExtractUserIdAsLongFromNumber() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 123,
        "username", "johndoe"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.userId()).isEqualTo(123L);
  }

  @Test
  @DisplayName("Should extract userId from string")
  void shouldExtractUserIdFromString() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", "456",
        "username", "johndoe"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.userId()).isEqualTo(456L);
  }

  @Test
  @DisplayName("Should return null userId when parsing fails")
  void shouldReturnNullUserIdWhenParsingFails() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", "invalid",
        "username", "johndoe"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.userId()).isNull();
  }

  @Test
  @DisplayName("Should extract username from preferred_username when username is missing")
  void shouldExtractUsernameFromPreferredUsernameWhenUsernameIsMissing() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "preferred_username", "johndoe"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.username()).isEqualTo("johndoe");
  }

  @Test
  @DisplayName("Should extract username from sub when both username and preferred_username are missing")
  void shouldExtractUsernameFromSubWhenBothUsernameAndPreferredUsernameAreMissing() {
    // Arrange
    var claims = Map.<String, Object>of(
        "sub", "johndoe",
        "userId", 1L
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.username()).isEqualTo("johndoe");
  }

  @Test
  @DisplayName("Should extract roles from authorities when roles is missing")
  void shouldExtractRolesFromAuthoritiesWhenRolesIsMissing() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "authorities", List.of("ROLE_USER", "ROLE_ADMIN")
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  @DisplayName("Should extract roles from realm_access for Keycloak")
  void shouldExtractRolesFromRealmAccessForKeycloak() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "realm_access", Map.of("roles", List.of("user", "admin"))
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.roles()).containsExactlyInAnyOrder("user", "admin");
  }

  @Test
  @DisplayName("Should return empty set when roles extraction fails")
  void shouldReturnEmptySetWhenRolesExtractionFails() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "roles", "invalid"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.roles()).isEmpty();
  }

  @Test
  @DisplayName("Should extract permissions from scope when permissions is missing")
  void shouldExtractPermissionsFromScopeWhenPermissionsIsMissing() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "scope", "read:profile update:profile"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.permissions()).containsExactlyInAnyOrder("read:profile", "update:profile");
  }

  @Test
  @DisplayName("Should extract permissions from iterable")
  void shouldExtractPermissionsFromIterable() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "permissions", List.of("read:books", "write:books")
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.permissions()).containsExactlyInAnyOrder("read:books", "write:books");
  }

  @Test
  @DisplayName("Should return empty set when permissions extraction fails")
  void shouldReturnEmptySetWhenPermissionsExtractionFails() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "permissions", 123
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.permissions()).isEmpty();
  }

  @Test
  @DisplayName("Should extract custom claims excluding standard claims")
  void shouldExtractCustomClaimsExcludingStandardClaims() {
    // Arrange
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "email", "john@example.com",
        "customField1", "value1",
        "customField2", "value2",
        "iss", "issuer",
        "sub", "subject"
    );

    var jwt = createJwt(claims);

    // Act
    var userContext = userContextExtractor.extractFromJwt(jwt);

    // Assert
    assertThat(userContext.customClaims())
        .containsEntry("customField1", "value1")
        .containsEntry("customField2", "value2")
        .doesNotContainKeys("userId", "username", "email", "iss", "sub");
  }

  @Test
  @DisplayName("Should throw exception when JWT token is invalid")
  void shouldThrowExceptionWhenJwtTokenIsInvalid() {
    // Arrange
    var invalidToken = "invalid.jwt.token";
    when(jwtDecoder.decode(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

    // Act & Assert
    assertThatThrownBy(() -> userContextExtractor.extractFromJwt(invalidToken))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid JWT token")
        .hasCauseInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should extract user context from JWT token string")
  void shouldExtractUserContextFromJwtTokenString() {
    // Arrange
    var token = "valid.jwt.token";
    var claims = Map.<String, Object>of(
        "userId", 1L,
        "username", "johndoe",
        "email", "john@example.com"
    );

    var jwt = createJwt(claims);
    when(jwtDecoder.decode(token)).thenReturn(jwt);

    // Act
    var userContext = userContextExtractor.extractFromJwt(token);

    // Assert
    assertThat(userContext.userId()).isEqualTo(1L);
    assertThat(userContext.username()).isEqualTo("johndoe");
    assertThat(userContext.email()).isEqualTo("john@example.com");
  }

  private Jwt createJwt(Map<String, Object> claims) {
    return new Jwt(
        "token",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        Map.of("alg", "RS256"),
        claims
    );
  }
}
