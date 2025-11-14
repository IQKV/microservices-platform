package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for entity validation and relationships focusing on happy path scenarios. Tests core entity functionality with valid inputs and successful operations.
 */
class EntityValidationTest {

  @Test
  void testUserEntityCreation_WithValidData_ShouldSucceed() {
    // Given
    var username = "testuser";
    var email = "test@example.com";
    var passwordHash = "hashedPassword123";
    var firstName = "John";
    var lastName = "Doe";
    var tenantId = "tenant-1";

    // When
    var user = new User(username, email, passwordHash, firstName, lastName, tenantId);

    // Then
    assertNotNull(user);
    assertEquals(username, user.getUsername());
    assertEquals(email, user.getEmail());
    assertEquals(passwordHash, user.getPasswordHash());
    assertEquals(firstName, user.getFirstName());
    assertEquals(lastName, user.getLastName());
    assertEquals(tenantId, user.getTenantId());
    assertTrue(user.getEnabled());
    assertFalse(user.getEmailVerified());
    assertNotNull(user.getAuthorities());
    assertTrue(user.getAuthorities().isEmpty());
  }

  @Test
  void testAuthorityEntityCreation_WithValidData_ShouldSucceed() {
    // Given
    var authorityName = "USER";
    var description = "Standard user role";

    // When
    var authority = new Authority(authorityName, description);

    // Then
    assertNotNull(authority);
    assertEquals(authorityName, authority.getName());
    assertEquals(description, authority.getDescription());
    assertNotNull(authority.getUsers());
    assertTrue(authority.getUsers().isEmpty());
  }

  @Test
  void testUserAuthorityRelationship_AddAuthority_ShouldSucceed() {
    // Given
    var user = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");
    var authority = new Authority("USER", "Standard user role");

    // When
    user.addAuthority(authority);

    // Then
    assertTrue(user.getAuthorities().contains(authority));
    assertTrue(authority.getUsers().contains(user));
    assertTrue(user.hasAuthority("USER"));
    assertEquals(1, user.getAuthorities().size());
    assertEquals(1, authority.getUsers().size());
  }

  @Test
  void testUserAuthorityRelationship_RemoveAuthority_ShouldSucceed() {
    // Given
    var user = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");
    var authority = new Authority("USER", "Standard user role");
    user.addAuthority(authority);

    // When
    user.removeAuthority(authority);

    // Then
    assertFalse(user.getAuthorities().contains(authority));
    assertFalse(authority.getUsers().contains(user));
    assertFalse(user.hasAuthority("USER"));
    assertTrue(user.getAuthorities().isEmpty());
    assertTrue(authority.getUsers().isEmpty());
  }

  @Test
  void testUserAuthorityRelationship_MultipleAuthorities_ShouldSucceed() {
    // Given
    var user = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");
    var userRole = new Authority("USER", "Standard user role");
    var adminRole = new Authority("ADMIN", "Administrator role");

    // When
    user.addAuthority(userRole);
    user.addAuthority(adminRole);

    // Then
    assertEquals(2, user.getAuthorities().size());
    assertTrue(user.hasAuthority("USER"));
    assertTrue(user.hasAuthority("ADMIN"));
    assertTrue(userRole.getUsers().contains(user));
    assertTrue(adminRole.getUsers().contains(user));
  }

  @Test
  void testUserUtilityMethods_WithValidData_ShouldReturnCorrectValues() {
    // Given
    var user = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");
    user.setEmailVerified(true);

    // When & Then
    assertEquals("John Doe", user.getFullName());
    assertTrue(user.isActive()); // enabled=true and emailVerified=true

    // Test with disabled user
    user.setEnabled(false);
    assertFalse(user.isActive());

    // Test with unverified email
    user.setEnabled(true);
    user.setEmailVerified(false);
    assertFalse(user.isActive());
  }

  @Test
  void testAuthorityUtilityMethods_WithValidData_ShouldReturnCorrectValues() {
    // Given
    var authority = new Authority("ADMIN", "Administrator role");
    var user1 = new User("user1", "user1@example.com", "hash", "User", "One", "tenant-1");
    var user2 = new User("user2", "user2@example.com", "hash", "User", "Two", "tenant-1");

    // When
    authority.addUser(user1);
    authority.addUser(user2);

    // Then
    assertEquals(2, authority.getUserCount());
    assertTrue(authority.hasUser(user1));
    assertTrue(authority.hasUser(user2));

    // Test remove user
    authority.removeUser(user1);
    assertEquals(1, authority.getUserCount());
    assertFalse(authority.hasUser(user1));
    assertTrue(authority.hasUser(user2));
  }

  @Test
  void testUserEqualsAndHashCode_WithSameData_ShouldBeEqual() {
    // Given
    var user1 = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");
    var user2 = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");

    // When & Then
    assertEquals(user1, user2);
    assertEquals(user1.hashCode(), user2.hashCode());
  }

  @Test
  void testAuthorityEqualsAndHashCode_WithSameData_ShouldBeEqual() {
    // Given
    var authority1 = new Authority("USER", "Standard user role");
    var authority2 = new Authority("USER", "Standard user role");

    // When & Then
    assertEquals(authority1, authority2);
    assertEquals(authority1.hashCode(), authority2.hashCode());
  }

  @Test
  void testUserToString_ShouldContainKeyInformation() {
    // Given
    var user = new User("testuser", "test@example.com", "hash", "John", "Doe", "tenant-1");

    // When
    var userString = user.toString();

    // Then
    assertNotNull(userString);
    assertTrue(userString.contains("testuser"));
    assertTrue(userString.contains("test@example.com"));
    assertTrue(userString.contains("John"));
    assertTrue(userString.contains("Doe"));
    assertTrue(userString.contains("tenant-1"));
  }

  @Test
  void testAuthorityToString_ShouldContainKeyInformation() {
    // Given
    var authority = new Authority("USER", "Standard user role");

    // When
    var authorityString = authority.toString();

    // Then
    assertNotNull(authorityString);
    assertTrue(authorityString.contains("USER"));
    assertTrue(authorityString.contains("Standard user role"));
  }

  @Test
  void testUserTenantIsolation_WithDifferentTenants_ShouldMaintainSeparation() {
    // Given
    var user1 = new User("user1", "user1@example.com", "hash", "User", "One", "tenant-1");
    var user2 = new User("user2", "user2@example.com", "hash", "User", "Two", "tenant-2");

    // When & Then
    assertEquals("tenant-1", user1.getTenantId());
    assertEquals("tenant-2", user2.getTenantId());
    assertNotEquals(user1.getTenantId(), user2.getTenantId());
  }
}