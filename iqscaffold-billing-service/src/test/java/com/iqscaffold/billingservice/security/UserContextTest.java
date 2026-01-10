package com.iqscaffold.billingservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserContextTest {

  @Test
  void hasAuthority_shouldReturnTrueWhenAuthorityExists() {
    // Given
    UserContext context = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("READ", "WRITE"),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertTrue(context.hasAuthority("READ"));
    assertTrue(context.hasAuthority("WRITE"));
  }

  @Test
  void hasAuthority_shouldReturnFalseWhenAuthorityDoesNotExist() {
    // Given
    UserContext context = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("READ"),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertFalse(context.hasAuthority("DELETE"));
    assertFalse(context.hasAuthority("ADMIN"));
  }

  @Test
  void isAdmin_shouldReturnTrueWhenUserHasRoleAdmin() {
    // Given
    UserContext context = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ROLE_ADMIN", "READ", "WRITE"),
        "tenant1",
        null,
        "Admin",
        "User"
    );

    // When & Then
    assertTrue(context.isAdmin());
  }

  @Test
  void isAdmin_shouldReturnTrueWhenUserHasAdminAuthority() {
    // Given
    UserContext context = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN", "READ"),
        "tenant1",
        null,
        "Admin",
        "User"
    );

    // When & Then
    assertTrue(context.isAdmin());
  }

  @Test
  void isAdmin_shouldReturnFalseWhenUserIsNotAdmin() {
    // Given
    UserContext context = new UserContext(
        1L,
        "user",
        "user@example.com",
        Set.of("READ", "WRITE"),
        "tenant1",
        null,
        "Regular",
        "User"
    );

    // When & Then
    assertFalse(context.isAdmin());
  }

  @Test
  void userContext_shouldStoreAllFields() {
    // Given
    Long userId = 123L;
    String username = "testuser";
    String email = "test@example.com";
    Set<String> authorities = Set.of("READ", "WRITE");
    String tenantId = "tenant123";
    Long organizationId = 456L;
    String firstName = "John";
    String lastName = "Doe";

    // When
    UserContext context = new UserContext(
        userId, username, email, authorities, tenantId, organizationId, firstName, lastName
    );

    // Then
    assertEquals(userId, context.userId());
    assertEquals(username, context.username());
    assertEquals(email, context.email());
    assertEquals(authorities, context.authorities());
    assertEquals(tenantId, context.tenantId());
    assertEquals(organizationId, context.organizationId());
    assertEquals(firstName, context.firstName());
    assertEquals(lastName, context.lastName());
  }

  @Test
  void userContext_shouldHandleEmptyAuthorities() {
    // Given
    UserContext context = new UserContext(
        1L,
        "user",
        "user@example.com",
        Set.of(),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertFalse(context.hasAuthority("READ"));
    assertFalse(context.isAdmin());
  }

  @Test
  void userContext_shouldHandleNullValues() {
    // Given
    UserContext context = new UserContext(
        null,
        null,
        null,
        Set.of("READ"),
        null,
        null,
        null,
        null
    );

    // When & Then
    assertNull(context.userId());
    assertNull(context.username());
    assertNull(context.email());
    assertNull(context.tenantId());
    assertNull(context.organizationId());
    assertNull(context.firstName());
    assertNull(context.lastName());
    assertTrue(context.hasAuthority("READ"));
  }

  @Test
  void hasBillingAccess_shouldReturnTrueForSuperAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "superadmin", "super@example.com",
        Set.of("SUPER_ADMIN"), "tenant1", null, "Super", "Admin"
    );

    // When & Then
    assertTrue(context.hasBillingAccess());
  }

  @Test
  void hasBillingAccess_shouldReturnTrueForTenantOwner() {
    // Given
    UserContext context = new UserContext(
        1L, "owner", "owner@example.com",
        Set.of("TENANT_OWNER"), "tenant1", null, "Tenant", "Owner"
    );

    // When & Then
    assertTrue(context.hasBillingAccess());
  }

  @Test
  void hasBillingAccess_shouldReturnTrueForBillingAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "billingadmin", "billing@example.com",
        Set.of("BILLING_ADMIN"), "tenant1", null, "Billing", "Admin"
    );

    // When & Then
    assertTrue(context.hasBillingAccess());
  }

  @Test
  void hasBillingAccess_shouldReturnTrueForFinanceViewer() {
    // Given
    UserContext context = new UserContext(
        1L, "viewer", "viewer@example.com",
        Set.of("FINANCE_VIEWER"), "tenant1", null, "Finance", "Viewer"
    );

    // When & Then
    assertTrue(context.hasBillingAccess());
  }

  @Test
  void hasBillingAccess_shouldReturnFalseForRegularAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "admin", "admin@example.com",
        Set.of("ADMIN"), "tenant1", null, "Regular", "Admin"
    );

    // When & Then
    assertFalse(context.hasBillingAccess());
  }

  @Test
  void hasBillingAccess_shouldReturnFalseForRegularUser() {
    // Given
    UserContext context = new UserContext(
        1L, "user", "user@example.com",
        Set.of("USER"), "tenant1", null, "Regular", "User"
    );

    // When & Then
    assertFalse(context.hasBillingAccess());
  }

  @Test
  void canModifyBilling_shouldReturnTrueForSuperAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "superadmin", "super@example.com",
        Set.of("SUPER_ADMIN"), "tenant1", null, "Super", "Admin"
    );

    // When & Then
    assertTrue(context.canModifyBilling());
  }

  @Test
  void canModifyBilling_shouldReturnTrueForTenantOwner() {
    // Given
    UserContext context = new UserContext(
        1L, "owner", "owner@example.com",
        Set.of("TENANT_OWNER"), "tenant1", null, "Tenant", "Owner"
    );

    // When & Then
    assertTrue(context.canModifyBilling());
  }

  @Test
  void canModifyBilling_shouldReturnTrueForBillingAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "billingadmin", "billing@example.com",
        Set.of("BILLING_ADMIN"), "tenant1", null, "Billing", "Admin"
    );

    // When & Then
    assertTrue(context.canModifyBilling());
  }

  @Test
  void canModifyBilling_shouldReturnFalseForFinanceViewer() {
    // Given
    UserContext context = new UserContext(
        1L, "viewer", "viewer@example.com",
        Set.of("FINANCE_VIEWER"), "tenant1", null, "Finance", "Viewer"
    );

    // When & Then
    assertFalse(context.canModifyBilling());
  }

  @Test
  void canModifyBilling_shouldReturnFalseForRegularAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "admin", "admin@example.com",
        Set.of("ADMIN"), "tenant1", null, "Regular", "Admin"
    );

    // When & Then
    assertFalse(context.canModifyBilling());
  }

  @Test
  void hasReadOnlyBillingAccess_shouldReturnTrueForFinanceViewer() {
    // Given
    UserContext context = new UserContext(
        1L, "viewer", "viewer@example.com",
        Set.of("FINANCE_VIEWER"), "tenant1", null, "Finance", "Viewer"
    );

    // When & Then
    assertTrue(context.hasReadOnlyBillingAccess());
  }

  @Test
  void hasReadOnlyBillingAccess_shouldReturnFalseForBillingAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "billingadmin", "billing@example.com",
        Set.of("BILLING_ADMIN"), "tenant1", null, "Billing", "Admin"
    );

    // When & Then
    assertFalse(context.hasReadOnlyBillingAccess());
  }

  @Test
  void hasReadOnlyBillingAccess_shouldReturnFalseForRegularUser() {
    // Given
    UserContext context = new UserContext(
        1L, "user", "user@example.com",
        Set.of("USER"), "tenant1", null, "Regular", "User"
    );

    // When & Then
    assertFalse(context.hasReadOnlyBillingAccess());
  }

  @Test
  void isSuperAdmin_shouldReturnTrueForSuperAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "superadmin", "super@example.com",
        Set.of("SUPER_ADMIN"), "tenant1", null, "Super", "Admin"
    );

    // When & Then
    assertTrue(context.isSuperAdmin());
  }

  @Test
  void isSuperAdmin_shouldReturnFalseForTenantOwner() {
    // Given
    UserContext context = new UserContext(
        1L, "owner", "owner@example.com",
        Set.of("TENANT_OWNER"), "tenant1", null, "Tenant", "Owner"
    );

    // When & Then
    assertFalse(context.isSuperAdmin());
  }

  @Test
  void isSuperAdmin_shouldReturnFalseForRegularAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "admin", "admin@example.com",
        Set.of("ADMIN"), "tenant1", null, "Regular", "Admin"
    );

    // When & Then
    assertFalse(context.isSuperAdmin());
  }

  @Test
  void isTenantOwner_shouldReturnTrueForTenantOwner() {
    // Given
    UserContext context = new UserContext(
        1L, "owner", "owner@example.com",
        Set.of("TENANT_OWNER"), "tenant1", null, "Tenant", "Owner"
    );

    // When & Then
    assertTrue(context.isTenantOwner());
  }

  @Test
  void isTenantOwner_shouldReturnFalseForSuperAdmin() {
    // Given
    UserContext context = new UserContext(
        1L, "superadmin", "super@example.com",
        Set.of("SUPER_ADMIN"), "tenant1", null, "Super", "Admin"
    );

    // When & Then
    assertFalse(context.isTenantOwner());
  }

  @Test
  void isTenantOwner_shouldReturnFalseForRegularUser() {
    // Given
    UserContext context = new UserContext(
        1L, "user", "user@example.com",
        Set.of("USER"), "tenant1", null, "Regular", "User"
    );

    // When & Then
    assertFalse(context.isTenantOwner());
  }

  @Test
  void billingAccessMethods_shouldWorkWithMultipleAuthorities() {
    // Given - User with multiple authorities
    UserContext context = new UserContext(
        1L, "poweruser", "power@example.com",
        Set.of("BILLING_ADMIN", "FINANCE_VIEWER", "USER"), "tenant1", null, "Power", "User"
    );

    // When & Then
    assertTrue(context.hasBillingAccess());
    assertTrue(context.canModifyBilling());
    assertTrue(context.hasReadOnlyBillingAccess());
    assertFalse(context.isSuperAdmin());
    assertFalse(context.isTenantOwner());
  }
}
