package org.gripday.userservice.usermanagement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Unit tests for UserDto class.
 * Tests record creation and utility methods.
 */
class UserDtoTest {

    @Test
    void shouldCreateValidUserDto() {
        var createdAt = LocalDateTime.of(2023, 1, 1, 10, 0);
        var updatedAt = LocalDateTime.of(2023, 1, 2, 15, 30);

        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            "John",
            "Doe",
            true,
            true,
            Set.of("USER", "ADMIN"),
            "tenant-123",
            createdAt,
            updatedAt
        );

        assertEquals(1L, userDto.id());
        assertEquals("johndoe", userDto.username());
        assertEquals("john@example.com", userDto.email());
        assertEquals("John", userDto.firstName());
        assertEquals("Doe", userDto.lastName());
        assertTrue(userDto.enabled());
        assertTrue(userDto.emailVerified());
        assertEquals(Set.of("USER", "ADMIN"), userDto.roles());
        assertEquals("tenant-123", userDto.tenantId());
        assertEquals(createdAt, userDto.createdAt());
        assertEquals(updatedAt, userDto.updatedAt());
    }

    @Test
    void shouldGetFullName() {
        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            "John",
            "Doe",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals("John Doe", userDto.getFullName());
    }

    @Test
    void shouldGetFullNameWithNullFirstName() {
        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            null,
            "Doe",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals("Doe", userDto.getFullName());
    }

    @Test
    void shouldGetFullNameWithNullLastName() {
        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            "John",
            null,
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals("John", userDto.getFullName());
    }

    @Test
    void shouldGetFullNameWithBothNamesNull() {
        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            null,
            null,
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals("", userDto.getFullName());
    }

    @Test
    void shouldGetFullNameWithEmptyNames() {
        var userDto = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            "",
            "",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals(" ", userDto.getFullName());
    }

    @Test
    void shouldGetFullNameWithMixedNullAndEmpty() {
        var userDto1 = new UserDto(
            1L,
            "johndoe",
            "john@example.com",
            "",
            "Doe",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var userDto2 = new UserDto(
            2L,
            "janedoe",
            "jane@example.com",
            "Jane",
            "",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertEquals(" Doe", userDto1.getFullName());
        assertEquals("Jane ", userDto2.getFullName());
    }

    @Test
    void shouldCheckIfUserIsActive() {
        var activeUser = new UserDto(
            1L,
            "activeuser",
            "active@example.com",
            "Active",
            "User",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var inactiveUser1 = new UserDto(
            2L,
            "inactiveuser1",
            "inactive1@example.com",
            "Inactive",
            "User1",
            false,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var inactiveUser2 = new UserDto(
            3L,
            "inactiveuser2",
            "inactive2@example.com",
            "Inactive",
            "User2",
            true,
            false,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var inactiveUser3 = new UserDto(
            4L,
            "inactiveuser3",
            "inactive3@example.com",
            "Inactive",
            "User3",
            false,
            false,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertTrue(activeUser.isActive());
        assertFalse(inactiveUser1.isActive());
        assertFalse(inactiveUser2.isActive());
        assertFalse(inactiveUser3.isActive());
    }

    @Test
    void shouldCheckIfUserIsActiveWithNullValues() {
        var userWithNullEnabled = new UserDto(
            1L,
            "user1",
            "user1@example.com",
            "User",
            "One",
            null,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var userWithNullEmailVerified = new UserDto(
            2L,
            "user2",
            "user2@example.com",
            "User",
            "Two",
            true,
            null,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        var userWithBothNull = new UserDto(
            3L,
            "user3",
            "user3@example.com",
            "User",
            "Three",
            null,
            null,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertFalse(userWithNullEnabled.isActive());
        assertFalse(userWithNullEmailVerified.isActive());
        assertFalse(userWithBothNull.isActive());
    }

    @Test
    void shouldCheckIfUserIsAdmin() {
        var adminUser = new UserDto(
            1L,
            "admin",
            "admin@example.com",
            "Admin",
            "User",
            true,
            true,
            Set.of("ADMIN"),
            "tenant-123",
            null,
            null
        );

        var superAdminUser = new UserDto(
            2L,
            "superadmin",
            "superadmin@example.com",
            "Super",
            "Admin",
            true,
            true,
            Set.of("SUPER_ADMIN"),
            "tenant-123",
            null,
            null
        );

        var userWithMultipleRoles = new UserDto(
            3L,
            "multirole",
            "multirole@example.com",
            "Multi",
            "Role",
            true,
            true,
            Set.of("USER", "ADMIN"),
            "tenant-123",
            null,
            null
        );

        var regularUser = new UserDto(
            4L,
            "user",
            "user@example.com",
            "Regular",
            "User",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            null,
            null
        );

        assertTrue(adminUser.isAdmin());
        assertTrue(superAdminUser.isAdmin());
        assertTrue(userWithMultipleRoles.isAdmin());
        assertFalse(regularUser.isAdmin());
    }

    @Test
    void shouldCheckIfUserIsAdminWithNullRoles() {
        var userWithNullRoles = new UserDto(
            1L,
            "user",
            "user@example.com",
            "User",
            "One",
            true,
            true,
            null,
            "tenant-123",
            null,
            null
        );

        var userWithEmptyRoles = new UserDto(
            2L,
            "user2",
            "user2@example.com",
            "User",
            "Two",
            true,
            true,
            Set.of(),
            "tenant-123",
            null,
            null
        );

        assertFalse(userWithNullRoles.isAdmin());
        assertFalse(userWithEmptyRoles.isAdmin());
    }

    @Test
    void shouldHandleComplexRoleSets() {
        var userWithManyRoles = new UserDto(
            1L,
            "superuser",
            "superuser@example.com",
            "Super",
            "User",
            true,
            true,
            Set.of("USER", "ADMIN", "SUPER_ADMIN", "MANAGER", "VIEWER", "EDITOR"),
            "tenant-123",
            null,
            null
        );

        assertTrue(userWithManyRoles.isAdmin());
    }

    @Test
    void shouldHandleTimestamps() {
        var now = LocalDateTime.now();
        var userDto = new UserDto(
            1L,
            "user",
            "user@example.com",
            "User",
            "One",
            true,
            true,
            Set.of("USER"),
            "tenant-123",
            now,
            now.plusDays(1)
        );

        assertEquals(now, userDto.createdAt());
        assertEquals(now.plusDays(1), userDto.updatedAt());
    }
}
