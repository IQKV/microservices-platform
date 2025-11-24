package org.gripday.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.gripday.userservice.security.UserAuditLogRepository;
import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.shared.AuthorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for UserManagementService.
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

  @InjectMocks
  private UserManagementService service;

  private UserContext adminUser;
  private UserContext superAdminUser;
  private UserContext regularUser;
  private User testUser;
  private User superAdminTestUser;
  private Authority userAuthority;
  private Authority adminAuthority;
  private Authority superAdminAuthority;

  @BeforeEach
  void setUp() {
    adminUser = new UserContext(
        1L,
        "admin",
        "admin@test.com",
        Set.of("ADMIN"),
        Set.of(),
        "Admin",
        "User",
        "tenant-123",
        null
    );

    superAdminUser = new UserContext(
        2L,
        "superadmin",
        "superadmin@test.com",
        Set.of("SUPER_ADMIN"),
        Set.of(),
        "Super",
        "Admin",
        "tenant-123",
        null
    );

    regularUser = new UserContext(
        3L,
        "user",
        "user@test.com",
        Set.of("USER"),
        Set.of(),
        "Regular",
        "User",
        "tenant-123",
        null
    );

    testUser = new User("testuser", "test@example.com", "hashedpass", "Test", "User", "tenant-123");
    superAdminTestUser = new User("superuser", "super@example.com", "hashedpass", "Super", "User", "tenant-123");

    userAuthority = new Authority("USER");
    adminAuthority = new Authority("ADMIN");
    superAdminAuthority = new Authority("SUPER_ADMIN");
  }

  @Test
  @DisplayName("Should create user successfully")
  void shouldCreateUser() {
    // Arrange
    var request = new CreateUserRequest(
        "newuser",
        "new@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("USER")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
    when(authorityRepository.findByNameIn(anySet())).thenReturn(List.of(userAuthority));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // Act
    var result = service.createUser(request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(passwordEncoder).encode("password123");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("Should throw exception when creating user with duplicate username")
  void shouldThrowExceptionWhenCreatingDuplicateUsername() {
    // Arrange
    var request = new CreateUserRequest(
        "existinguser",
        "new@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("USER")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createUser(request, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Username already exists");
  }

  @Test
  @DisplayName("Should throw exception when creating user with duplicate email")
  void shouldThrowExceptionWhenCreatingDuplicateEmail() {
    // Arrange
    var request = new CreateUserRequest(
        "newuser",
        "existing@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("USER")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createUser(request, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Email already exists");
  }

  @Test
  @DisplayName("Should retrieve user by ID")
  void shouldGetUserById() {
    // Arrange
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

    // Act
    var result = service.getUserById(1L, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.username()).isEqualTo("testuser");
    verify(userRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getUserById(1L, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  @DisplayName("Should update user successfully")
  void shouldUpdateUser() {
    // Arrange
    var request = new UpdateUserRequest(
        "updateduser",
        "updated@example.com",
        "newpassword",
        "Updated",
        "User",
        true,
        true,
        Set.of("USER")
    );

    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("newhashedpassword");
    when(authorityRepository.findByNameIn(anySet())).thenReturn(List.of(userAuthority));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // Act
    var result = service.updateUser(1L, request, adminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(passwordEncoder).encode("newpassword");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("Should throw exception when updating to duplicate username")
  void shouldThrowExceptionWhenUpdatingToDuplicateUsername() {
    // Arrange
    var request = new UpdateUserRequest(
        "existinguser",
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByUsername(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.updateUser(1L, request, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Username already exists");
  }

  @Test
  @DisplayName("Should throw exception when updating to duplicate email")
  void shouldThrowExceptionWhenUpdatingToDuplicateEmail() {
    // Arrange
    var request = new UpdateUserRequest(
        null,
        "existing@example.com",
        null,
        null,
        null,
        null,
        null,
        null
    );

    when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.updateUser(1L, request, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Email already exists");
  }

  @Test
  @DisplayName("Should list all users")
  void shouldGetAllUsers() {
    // Arrange
    var pageable = PageRequest.of(0, 20);
    when(userRepository.findAll()).thenReturn(List.of(testUser));

    // Act
    var result = service.getAllUsers(pageable, adminUser);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(userRepository).findAll();
  }

  @Test
  @DisplayName("Should delete user successfully")
  void shouldDeleteUser() throws Exception {
    // Arrange
    var userToDelete = new User("deleteuser", "delete@test.com", "hash", "Delete", "User", "tenant-123");
    var idField = User.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(userToDelete, 2L);
    
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(userToDelete));

    // Act
    service.deleteUser(2L, adminUser);

    // Assert
    verify(userRepository).delete(userToDelete);
  }

  @Test
  @DisplayName("Should prevent self-deletion")
  void shouldPreventSelfDeletion() throws Exception {
    // Arrange
    var selfUser = new User("admin", "admin@test.com", "hash", "Admin", "User", "tenant-123");
    var idField = User.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(selfUser, 1L);
    
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(selfUser));

    // Act & Assert
    assertThatThrownBy(() -> service.deleteUser(1L, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Cannot delete your own account");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for create operation")
  void shouldDenyAccessToNonAdminForCreate() {
    // Arrange
    var request = new CreateUserRequest(
        "newuser",
        "new@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("USER")
    );

    // Act & Assert
    assertThatThrownBy(() -> service.createUser(request, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for read operation")
  void shouldDenyAccessToNonAdminForRead() {
    // Act & Assert
    assertThatThrownBy(() -> service.getUserById(1L, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for update operation")
  void shouldDenyAccessToNonAdminForUpdate() {
    // Arrange
    var request = new UpdateUserRequest(
        "updateduser",
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    // Act & Assert
    assertThatThrownBy(() -> service.updateUser(1L, request, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for delete operation")
  void shouldDenyAccessToNonAdminForDelete() {
    // Act & Assert
    assertThatThrownBy(() -> service.deleteUser(1L, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should deny access to non-admin user for list operation")
  void shouldDenyAccessToNonAdminForList() {
    // Arrange
    var pageable = PageRequest.of(0, 20);

    // Act & Assert
    assertThatThrownBy(() -> service.getAllUsers(pageable, regularUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient permissions");
  }

  @Test
  @DisplayName("Should prevent admin from accessing super admin user")
  void shouldPreventAdminFromAccessingSuperAdminUser() {
    // Arrange
    superAdminTestUser.setAuthorities(Set.of(superAdminAuthority));
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(superAdminTestUser));

    // Act & Assert
    assertThatThrownBy(() -> service.getUserById(1L, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Cannot access super admin user");
  }

  @Test
  @DisplayName("Should prevent admin from assigning super admin role")
  void shouldPreventAdminFromAssigningSuperAdminRole() {
    // Arrange
    var request = new CreateUserRequest(
        "newuser",
        "new@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("SUPER_ADMIN")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
    when(authorityRepository.findByNameIn(anySet())).thenReturn(List.of(superAdminAuthority));

    // Act & Assert
    assertThatThrownBy(() -> service.createUser(request, adminUser))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("super admin");
  }

  @Test
  @DisplayName("Should allow super admin to create super admin user")
  void shouldAllowSuperAdminToCreateSuperAdminUser() {
    // Arrange
    var request = new CreateUserRequest(
        "newsuper",
        "newsuper@example.com",
        "password123",
        "New",
        "Super",
        true,
        false,
        Set.of("SUPER_ADMIN")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
    when(authorityRepository.findByNameIn(anySet())).thenReturn(List.of(superAdminAuthority));
    when(userRepository.save(any(User.class))).thenReturn(superAdminTestUser);

    // Act
    var result = service.createUser(request, superAdminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("Should allow super admin to access all users")
  void shouldAllowSuperAdminToAccessAllUsers() {
    // Arrange
    superAdminTestUser.setAuthorities(Set.of(superAdminAuthority));
    when(userRepository.findById(anyLong())).thenReturn(Optional.of(superAdminTestUser));

    // Act
    var result = service.getUserById(1L, superAdminUser);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw exception when role not found")
  void shouldThrowExceptionWhenRoleNotFound() {
    // Arrange
    var request = new CreateUserRequest(
        "newuser",
        "new@example.com",
        "password123",
        "New",
        "User",
        true,
        false,
        Set.of("INVALID_ROLE")
    );

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
    when(authorityRepository.findByNameIn(anySet())).thenReturn(List.of());

    // Act & Assert
    assertThatThrownBy(() -> service.createUser(request, adminUser))
        .isInstanceOf(UserManagementService.UserManagementException.class)
        .hasMessageContaining("Unknown roles");
  }
}
