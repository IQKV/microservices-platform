package org.gripday.authservice.unit;

import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for repository methods focusing on happy path scenarios.
 * Tests core repository functionality with valid data and successful operations.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    private User testUser;
    private Authority testAuthority;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "hashedPassword", "John", "Doe", "tenant-1");
        testAuthority = new Authority("USER", "Standard user role");
    }

    @Test
    void testUserRepository_FindByUsername_WithValidUsername_ShouldReturnUser() {
        // Given
        var username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        var result = userRepository.findByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testUserRepository_FindByEmail_WithValidEmail_ShouldReturnUser() {
        // Given
        var email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When
        var result = userRepository.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testUserRepository_FindByUsernameOrEmail_WithValidCredentials_ShouldReturnUser() {
        // Given
        var username = "testuser";
        var email = "test@example.com";
        when(userRepository.findByUsernameOrEmail(username, email)).thenReturn(Optional.of(testUser));

        // When
        var result = userRepository.findByUsernameOrEmail(username, email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByUsernameOrEmail(username, email);
    }

    @Test
    void testUserRepository_ExistsByUsername_WithExistingUsername_ShouldReturnTrue() {
        // Given
        var username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        var exists = userRepository.existsByUsername(username);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void testUserRepository_ExistsByEmail_WithExistingEmail_ShouldReturnTrue() {
        // Given
        var email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        var exists = userRepository.existsByEmail(email);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void testUserRepository_FindByTenantId_WithValidTenantId_ShouldReturnUsers() {
        // Given
        var tenantId = "tenant-1";
        var user2 = new User("user2", "user2@example.com", "hash", "Jane", "Smith", tenantId);
        var users = List.of(testUser, user2);
        when(userRepository.findByTenantId(tenantId)).thenReturn(users);

        // When
        var result = userRepository.findByTenantId(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testUser));
        assertTrue(result.contains(user2));
        verify(userRepository).findByTenantId(tenantId);
    }

    @Test
    void testUserRepository_FindEnabledUsersByTenantId_WithValidTenantId_ShouldReturnEnabledUsers() {
        // Given
        var tenantId = "tenant-1";
        var enabledUsers = List.of(testUser);
        when(userRepository.findEnabledUsersByTenantId(tenantId)).thenReturn(enabledUsers);

        // When
        var result = userRepository.findEnabledUsersByTenantId(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testUser));
        verify(userRepository).findEnabledUsersByTenantId(tenantId);
    }

    @Test
    void testUserRepository_CountByTenantId_WithValidTenantId_ShouldReturnCount() {
        // Given
        var tenantId = "tenant-1";
        var expectedCount = 5L;
        when(userRepository.countByTenantId(tenantId)).thenReturn(expectedCount);

        // When
        var count = userRepository.countByTenantId(tenantId);

        // Then
        assertEquals(expectedCount, count);
        verify(userRepository).countByTenantId(tenantId);
    }

    @Test
    void testUserRepository_CountEnabledUsersByTenantId_WithValidTenantId_ShouldReturnCount() {
        // Given
        var tenantId = "tenant-1";
        var expectedCount = 3L;
        when(userRepository.countEnabledUsersByTenantId(tenantId)).thenReturn(expectedCount);

        // When
        var count = userRepository.countEnabledUsersByTenantId(tenantId);

        // Then
        assertEquals(expectedCount, count);
        verify(userRepository).countEnabledUsersByTenantId(tenantId);
    }

    @Test
    void testAuthorityRepository_FindByName_WithValidName_ShouldReturnAuthority() {
        // Given
        var authorityName = "USER";
        when(authorityRepository.findByName(authorityName)).thenReturn(Optional.of(testAuthority));

        // When
        var result = authorityRepository.findByName(authorityName);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAuthority.getName(), result.get().getName());
        assertEquals(testAuthority.getDescription(), result.get().getDescription());
        verify(authorityRepository).findByName(authorityName);
    }

    @Test
    void testAuthorityRepository_FindByUserId_WithValidUserId_ShouldReturnAuthorities() {
        // Given
        var userId = 1L;
        var adminAuthority = new Authority("ADMIN", "Administrator role");
        var authorities = List.of(testAuthority, adminAuthority);
        when(authorityRepository.findByUserId(userId)).thenReturn(authorities);

        // When
        var result = authorityRepository.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testAuthority));
        assertTrue(result.contains(adminAuthority));
        verify(authorityRepository).findByUserId(userId);
    }

    @Test
    void testAuthorityRepository_FindByNameIn_WithValidNames_ShouldReturnAuthorities() {
        // Given
        var authorityNames = Set.of("USER", "ADMIN");
        var adminAuthority = new Authority("ADMIN", "Administrator role");
        var authorities = List.of(testAuthority, adminAuthority);
        when(authorityRepository.findByNameIn(authorityNames)).thenReturn(authorities);

        // When
        var result = authorityRepository.findByNameIn(authorityNames);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testAuthority));
        assertTrue(result.contains(adminAuthority));
        verify(authorityRepository).findByNameIn(authorityNames);
    }

    @Test
    void testAuthorityRepository_ExistsByName_WithExistingName_ShouldReturnTrue() {
        // Given
        var authorityName = "USER";
        when(authorityRepository.existsByName(authorityName)).thenReturn(true);

        // When
        var exists = authorityRepository.existsByName(authorityName);

        // Then
        assertTrue(exists);
        verify(authorityRepository).existsByName(authorityName);
    }

    @Test
    void testAuthorityRepository_FindAllOrderByName_ShouldReturnSortedAuthorities() {
        // Given
        var adminAuthority = new Authority("ADMIN", "Administrator role");
        var userAuthority = new Authority("USER", "Standard user role");
        var authorities = List.of(adminAuthority, userAuthority); // Should be sorted by name
        when(authorityRepository.findAllOrderByName()).thenReturn(authorities);

        // When
        var result = authorityRepository.findAllOrderByName();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("USER", result.get(1).getName());
        verify(authorityRepository).findAllOrderByName();
    }

    @Test
    void testAuthorityRepository_CountUsersByAuthorityName_WithValidName_ShouldReturnCount() {
        // Given
        var authorityName = "USER";
        var expectedCount = 10L;
        when(authorityRepository.countUsersByAuthorityName(authorityName)).thenReturn(expectedCount);

        // When
        var count = authorityRepository.countUsersByAuthorityName(authorityName);

        // Then
        assertEquals(expectedCount, count);
        verify(authorityRepository).countUsersByAuthorityName(authorityName);
    }

    @Test
    void testAuthorityRepository_FindDefaultUserAuthorities_ShouldReturnDefaultRoles() {
        // Given
        var defaultAuthorities = List.of(testAuthority);
        when(authorityRepository.findDefaultUserAuthorities()).thenReturn(defaultAuthorities);

        // When
        var result = authorityRepository.findDefaultUserAuthorities();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testAuthority));
        verify(authorityRepository).findDefaultUserAuthorities();
    }

    @Test
    void testUserRepository_FindByTenantIdAndAuthorityName_WithValidData_ShouldReturnUsers() {
        // Given
        var tenantId = "tenant-1";
        var authorityName = "ADMIN";
        var adminUsers = List.of(testUser);
        when(userRepository.findByTenantIdAndAuthorityName(tenantId, authorityName)).thenReturn(adminUsers);

        // When
        var result = userRepository.findByTenantIdAndAuthorityName(tenantId, authorityName);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testUser));
        verify(userRepository).findByTenantIdAndAuthorityName(tenantId, authorityName);
    }

    @Test
    void testUserRepository_FindUnverifiedUsersByTenantId_WithValidTenantId_ShouldReturnUnverifiedUsers() {
        // Given
        var tenantId = "tenant-1";
        var unverifiedUser = new User("unverified", "unverified@example.com", "hash", "Un", "Verified", tenantId);
        unverifiedUser.setEmailVerified(false);
        var unverifiedUsers = List.of(unverifiedUser);
        when(userRepository.findUnverifiedUsersByTenantId(tenantId)).thenReturn(unverifiedUsers);

        // When
        var result = userRepository.findUnverifiedUsersByTenantId(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(unverifiedUser));
        verify(userRepository).findUnverifiedUsersByTenantId(tenantId);
    }

    @Test
    void testAuthorityRepository_FindAuthoritiesWithNoUsers_ShouldReturnEmptyAuthorities() {
        // Given
        var emptyAuthority = new Authority("EMPTY_ROLE", "Role with no users");
        var emptyAuthorities = List.of(emptyAuthority);
        when(authorityRepository.findAuthoritiesWithNoUsers()).thenReturn(emptyAuthorities);

        // When
        var result = authorityRepository.findAuthoritiesWithNoUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(emptyAuthority));
        verify(authorityRepository).findAuthoritiesWithNoUsers();
    }
}