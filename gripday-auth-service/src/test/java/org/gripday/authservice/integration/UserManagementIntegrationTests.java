package org.gripday.authservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.authservice.AuthServiceApplication;
import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.CreateUserRequest;
import org.gripday.authservice.presentation.dto.LoginRequest;
import org.gripday.authservice.presentation.dto.TokenResponse;
import org.gripday.authservice.presentation.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for user management endpoints focusing on happy path scenarios. Tests admin-only CRUD operations with proper role-based access control and tenant isolation.
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "gripday.auth.jwt.secret=test-secret-key-for-integration-tests-that-is-long-enough",
    "gripday.cache.redis.enabled=false"
})
@Transactional
class UserManagementIntegrationTests {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AuthorityRepository authorityRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;
  private String adminToken;
  private String superAdminToken;
  private String userToken;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Create authorities
    createAuthorities();

    // Create test users and get tokens
    createTestUsersAndTokens();
  }

  @Test
  void getAllUsers_WithAdminToken_ShouldReturn200WithUsers() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3)) // admin, superadmin, regular user
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void getAllUsers_WithRegularUserToken_ShouldReturn403() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/users")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void createUser_WithAdminToken_ShouldReturn201Created() throws Exception {
    // Given
    var createRequest = new CreateUserRequest(
        "newuser",
        "newuser@example.com",
        "ValidPass123!",
        "New",
        "User",
        true,
        false,
        Set.of("USER")
    );

    // When & Then
    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("newuser"))
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.firstName").value("New"))
        .andExpect(jsonPath("$.lastName").value("User"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.roles").isArray())
        .andExpect(jsonPath("$.roles[0]").value("USER"));

    // Verify user was created in database
    var savedUser = userRepository.findByUsername("newuser");
    assertTrue(savedUser.isPresent());
    assertEquals("tenant-1", savedUser.get().getTenantId());
    assertTrue(savedUser.get().hasAuthority("USER"));
  }

  @Test
  void createUser_AdminCannotAssignSuperAdminRole_ShouldReturn403() throws Exception {
    // Given
    var createRequest = new CreateUserRequest(
        "newsuperadmin",
        "newsuperadmin@example.com",
        "ValidPass123!",
        "New",
        "SuperAdmin",
        true,
        false,
        Set.of("SUPER_ADMIN")
    );

    // When & Then
    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createUser_SuperAdminCanAssignSuperAdminRole_ShouldReturn201Created() throws Exception {
    // Given
    var createRequest = new CreateUserRequest(
        "newsuperadmin",
        "newsuperadmin@example.com",
        "ValidPass123!",
        "New",
        "SuperAdmin",
        true,
        false,
        Set.of("SUPER_ADMIN")
    );

    // When & Then
    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("newsuperadmin"))
        .andExpect(jsonPath("$.roles[0]").value("SUPER_ADMIN"));

    // Verify user was created with SUPER_ADMIN role
    var savedUser = userRepository.findByUsername("newsuperadmin");
    assertTrue(savedUser.isPresent());
    assertTrue(savedUser.get().hasAuthority("SUPER_ADMIN"));
  }

  @Test
  void getUserById_WithAdminToken_ShouldReturn200WithUser() throws Exception {
    // Given - Find a user to get by ID
    var testUser = userRepository.findByUsername("testuser").orElseThrow();

    // When & Then
    mockMvc.perform(get("/api/v1/users/{id}", testUser.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.email").value("testuser@example.com"))
        .andExpect(jsonPath("$.tenantId").value("tenant-1"));
  }

  @Test
  void updateUser_WithAdminToken_ShouldReturn200WithUpdatedUser() throws Exception {
    // Given - Find a user to update
    var testUser = userRepository.findByUsername("testuser").orElseThrow();

    var updateRequest = new UpdateUserRequest(
        "testuser",
        "updated@example.com",
        null, // password not being updated
        "Updated",
        "User",
        true,
        true,
        Set.of("USER", "ADMIN")
    );

    // When & Then
    mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("updated@example.com"))
        .andExpect(jsonPath("$.firstName").value("Updated"))
        .andExpect(jsonPath("$.lastName").value("User"))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.roles").isArray());

    // Verify user was updated in database
    var updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertEquals("updated@example.com", updatedUser.getEmail());
    assertEquals("Updated", updatedUser.getFirstName());
    assertTrue(updatedUser.getEmailVerified());
  }

  @Test
  void deleteUser_WithAdminToken_ShouldReturn204() throws Exception {
    // Given - Create a user to delete
    var userToDelete = createTestUser("deleteuser", "delete@example.com", "tenant-1", Set.of("USER"));

    // When & Then
    mockMvc.perform(delete("/api/v1/users/{id}", userToDelete.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    // Verify user was deleted from database
    var deletedUser = userRepository.findById(userToDelete.getId());
    assertTrue(deletedUser.isEmpty());
  }

  @Test
  void userManagement_WithTenantIsolation_ShouldOnlyAccessSameTenant() throws Exception {
    // Given - Create users in different tenants
    createTestUser("tenant1user", "tenant1@example.com", "tenant-1", Set.of("USER"));
    createTestUser("tenant2user", "tenant2@example.com", "tenant-2", Set.of("USER"));

    // When - Admin from tenant-1 tries to access users
    var result = mockMvc.perform(get("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andReturn();

    // Then - Should only see users from tenant-1
    var responseContent = result.getResponse().getContentAsString();
    assertTrue(responseContent.contains("tenant1user"));
    assertFalse(responseContent.contains("tenant2user"));

    // Verify tenant isolation in database query
    var tenant1Users = userRepository.findByTenantId("tenant-1");
    var tenant2Users = userRepository.findByTenantId("tenant-2");

    assertTrue(tenant1Users.stream().anyMatch(u -> "tenant1user".equals(u.getUsername())));
    assertTrue(tenant2Users.stream().anyMatch(u -> "tenant2user".equals(u.getUsername())));
    assertFalse(tenant1Users.stream().anyMatch(u -> "tenant2user".equals(u.getUsername())));
    assertFalse(tenant2Users.stream().anyMatch(u -> "tenant1user".equals(u.getUsername())));
  }

  @Test
  void createUser_WithTenantIsolation_ShouldCreateInCorrectTenant() throws Exception {
    // Given
    var createRequest = new CreateUserRequest(
        "tenantuser",
        "tenantuser@example.com",
        "ValidPass123!",
        "Tenant",
        "User",
        true,
        false,
        Set.of("USER")
    );

    // When & Then
    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tenantId").value("tenant-1"));

    // Verify user was created in correct tenant
    var savedUser = userRepository.findByUsername("tenantuser");
    assertTrue(savedUser.isPresent());
    assertEquals("tenant-1", savedUser.get().getTenantId());
  }

  @Test
  void userManagementCRUD_CompleteFlow_ShouldSucceed() throws Exception {
    // Step 1: Create user
    var createRequest = new CreateUserRequest(
        "cruduser",
        "crud@example.com",
        "ValidPass123!",
        "CRUD",
        "User",
        true,
        false,
        Set.of("USER")
    );

    var createResult = mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    var createdUser = objectMapper.readValue(
        createResult.getResponse().getContentAsString(),
        org.gripday.authservice.presentation.dto.UserDto.class
    );

    // Step 2: Read user
    mockMvc.perform(get("/api/v1/users/{id}", createdUser.id())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("cruduser"));

    // Step 3: Update user
    var updateRequest = new UpdateUserRequest(
        "cruduser",
        "updated-crud@example.com",
        null, // password not being updated
        "Updated",
        "CRUD",
        true,
        true,
        Set.of("USER", "ADMIN")
    );

    mockMvc.perform(put("/api/v1/users/{id}", createdUser.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("updated-crud@example.com"));

    // Step 4: Delete user
    mockMvc.perform(delete("/api/v1/users/{id}", createdUser.id())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    // Verify user is deleted
    var deletedUser = userRepository.findById(createdUser.id());
    assertTrue(deletedUser.isEmpty());
  }

  private void createAuthorities() {
    if (authorityRepository.findByName("USER").isEmpty()) {
      authorityRepository.save(new Authority("USER", "Standard user role"));
    }
    if (authorityRepository.findByName("ADMIN").isEmpty()) {
      authorityRepository.save(new Authority("ADMIN", "Administrator role"));
    }
    if (authorityRepository.findByName("SUPER_ADMIN").isEmpty()) {
      authorityRepository.save(new Authority("SUPER_ADMIN", "Super administrator role"));
    }
  }

  private void createTestUsersAndTokens() throws Exception {
    // Create admin user
    createTestUser("admin", "admin@example.com", "tenant-1", Set.of("ADMIN"));
    adminToken = getTokenForUser("admin", "ValidPass123!");

    // Create super admin user
    createTestUser("superadmin", "superadmin@example.com", "tenant-1", Set.of("SUPER_ADMIN"));
    superAdminToken = getTokenForUser("superadmin", "ValidPass123!");

    // Create regular user
    createTestUser("testuser", "testuser@example.com", "tenant-1", Set.of("USER"));
    userToken = getTokenForUser("testuser", "ValidPass123!");
  }

  private User createTestUser(String username, String email, String tenantId, Set<String> roleNames) {
    var hashedPassword = passwordEncoder.encode("ValidPass123!");
    var user = new User(username, email, hashedPassword, "Test", "User", tenantId);

    // Add authorities
    for (var roleName : roleNames) {
      var authority = authorityRepository.findByName(roleName).orElseThrow();
      user.addAuthority(authority);
    }

    return userRepository.save(user);
  }

  private String getTokenForUser(String username, String password) throws Exception {
    var loginRequest = new LoginRequest(username, password, false);

    var result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    var tokenResponse = objectMapper.readValue(
        result.getResponse().getContentAsString(),
        TokenResponse.class
    );

    return tokenResponse.accessToken();
  }
}