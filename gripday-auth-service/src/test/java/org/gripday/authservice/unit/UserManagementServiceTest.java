package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gripday.authservice.domain.service.UserManagementService;
import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserAuditLogRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.CreateUserRequest;
import org.gripday.authservice.presentation.dto.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for UserManagementService focusing on happy path scenarios. Tests core functionality with valid inputs and successful operations.
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthorityRepository authorityRepository;

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UserManagementService userManagementService;
  private UserContext adminUser;
  private UserContext superAdminUser;

  @BeforeEach
  void setUp() {
    userManagementService = new UserManagementService(
        userRepository, authorityRepository, auditLogRepository, passwordEncoder
    );

    // Create test admin user context
    adminUser = new UserContext(
        1L, "admin", "admin@test.com",
        Set.of("ADMIN"), Set.of("USER_MANAGEMENT"),
        "Admin", "User", "tenant-1", Map.of()
    );

    // Create test super admin user context
    superAdminUser = new UserContext(
        2L, "superadmin", "superadmin@test.com",
        Set.of("SUPER_ADMIN"), Set.of("USER_MANAGEMENT"),
        "Super", "Admin", "tenant-1", Map.of()
    );
  }

  @Test
  void getAllUsers_WithAdminUser_ShouldReturnFilteredUsers() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var user1 = createTestUser(3L, "user1", "user1@test.com", Set.of("USER"));
    var user2 = createTestUser(4L, "user2", "user2@test.com", Set.of("ADMIN"));
    var users = List.of(user1, user2);

    when(userRepository.findByTenantId("tenant-1")).thenReturn(users);

    // When
    var result = userManagementService.getAllUsers(pageable, adminUser);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getContent().size());
    verify(userRepository).findByTenantId("tenant-1");
    verify(auditLogRepository).save(any());
  }

  @Test
  void getUserById_WithValidId_ShouldReturnUser() {
    // Given
    var userId = 3L;
    var user = createTestUser(userId, "testuser", "test@test.com", Set.of("USER"));

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // When
    var result = userManagementService.getUserById(userId, adminUser);

    // Then
    assertNotNull(result);
    assertEquals("testuser", result.username());
    assertEquals("test@test.com", result.email());
    assertEquals("tenant-1", result.tenantId());
    verify(auditLogRepository).save(any());
  }

  @Test
  void createUser_WithValidRequest_ShouldCreateUser() {
    // Given
    var request = new CreateUserRequest(
        "newuser", "new@test.com", "password123",
        "New", "User", true, false, Set.of("USER")
    );

    var authority = new Authority("USER");
    var savedUser = createTestUser(5L, "newuser", "new@test.com", Set.of("USER"));

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
    when(authorityRepository.findByNameIn(Set.of("USER"))).thenReturn(List.of(authority));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // When
    var result = userManagementService.createUser(request, adminUser);

    // Then
    assertNotNull(result);
    assertEquals("newuser", result.username());
    assertEquals("new@test.com", result.email());
    assertTrue(result.roles().contains("USER"));
    verify(userRepository).save(any(User.class));
    verify(auditLogRepository).save(any());
  }

  @Test
  void deleteUser_WithValidId_ShouldDeleteUser() {
    // Given
    var userId = 3L;
    var user = createTestUser(userId, "testuser", "test@test.com", Set.of("USER"));

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // When
    assertDoesNotThrow(() -> userManagementService.deleteUser(userId, adminUser));

    // Then
    verify(userRepository).delete(user);
    verify(auditLogRepository).save(any());
  }

  @Test
  void createUser_AdminCannotAssignSuperAdminRole_ShouldThrowException() {
    // Given
    var request = new CreateUserRequest(
        "newuser", "new@test.com", "password123",
        "New", "User", true, false, Set.of("SUPER_ADMIN")
    );

    var authority = new Authority("SUPER_ADMIN");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
    when(authorityRepository.findByNameIn(Set.of("SUPER_ADMIN"))).thenReturn(List.of(authority));

    // When & Then
    assertThrows(org.springframework.security.access.AccessDeniedException.class,
        () -> userManagementService.createUser(request, adminUser));
  }

  @Test
  void createUser_SuperAdminCanAssignSuperAdminRole_ShouldSucceed() {
    // Given
    var request = new CreateUserRequest(
        "newuser", "new@test.com", "password123",
        "New", "User", true, false, Set.of("SUPER_ADMIN")
    );

    var authority = new Authority("SUPER_ADMIN");
    var savedUser = createTestUser(5L, "newuser", "new@test.com", Set.of("SUPER_ADMIN"));

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
    when(authorityRepository.findByNameIn(Set.of("SUPER_ADMIN"))).thenReturn(List.of(authority));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // When
    var result = userManagementService.createUser(request, superAdminUser);

    // Then
    assertNotNull(result);
    assertTrue(result.roles().contains("SUPER_ADMIN"));
    verify(userRepository).save(any(User.class));
  }

  private User createTestUser(Long id, String username, String email, Set<String> roleNames) {
    var user = new User(username, email, "hashedPassword", "First", "Last", "tenant-1");

    // Use reflection to set the ID for testing
    try {
      var idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (Exception e) {
      // Ignore for test purposes
    }

    // Create authorities
    var authorities = roleNames.stream()
        .map(Authority::new)
        .collect(java.util.stream.Collectors.toSet());
    user.setAuthorities(authorities);

    return user;
  }
}