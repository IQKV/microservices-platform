package org.gripday.authservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.authservice.AuthServiceApplication;
import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.LoginRequest;
import org.gripday.authservice.presentation.dto.RefreshTokenRequest;
import org.gripday.authservice.presentation.dto.SignupRequest;
import org.gripday.authservice.presentation.dto.TokenResponse;
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
 * Integration tests for authentication endpoints focusing on happy path scenarios. Tests complete authentication flows with valid credentials and tenant isolation.
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
class AuthenticationIntegrationTests {

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

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Create default USER authority if it doesn't exist
    if (authorityRepository.findByName("USER").isEmpty()) {
      var userAuthority = new Authority("USER", "Standard user role");
      authorityRepository.save(userAuthority);
    }
  }

  @Test
  void signup_WithValidRequest_ShouldReturn201Created() throws Exception {
    // Given
    var signupRequest = new SignupRequest(
        "newuser",
        "newuser@example.com",
        "ValidPass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupRequest)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value("newuser"))
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.emailVerified").value(false))
        .andExpect(jsonPath("$.message").value("User registered successfully. Please verify your email."));

    // Verify user was created in database
    var savedUser = userRepository.findByUsername("newuser");
    assertTrue(savedUser.isPresent());
    assertEquals("newuser@example.com", savedUser.get().getEmail());
    assertEquals("tenant-1", savedUser.get().getTenantId());
    assertTrue(savedUser.get().hasAuthority("USER"));
  }

  @Test
  void login_WithValidCredentials_ShouldReturn200WithToken() throws Exception {
    // Given - Create a test user
    createTestUser("testuser", "test@example.com", "ValidPass123!", "tenant-1");

    var loginRequest = new LoginRequest("testuser", "ValidPass123!", false);

    // When & Then
    var result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900)) // 15 minutes
        .andExpect(jsonPath("$.user.username").value("testuser"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"))
        .andExpect(jsonPath("$.user.tenantId").value("tenant-1"))
        .andExpect(jsonPath("$.user.roles").isArray())
        .andReturn();

    // Parse response to verify token structure
    var responseContent = result.getResponse().getContentAsString();
    var tokenResponse = objectMapper.readValue(responseContent, TokenResponse.class);

    assertNotNull(tokenResponse.accessToken());
    assertNotNull(tokenResponse.refreshToken());
    assertNotNull(tokenResponse.user());
    assertEquals("testuser", tokenResponse.user().username());
    assertEquals("tenant-1", tokenResponse.user().tenantId());
    assertTrue(tokenResponse.user().hasRole("USER"));
  }

  @Test
  void login_WithEmailAsUsername_ShouldReturn200WithToken() throws Exception {
    // Given - Create a test user
    createTestUser("emailuser", "email@example.com", "ValidPass123!", "tenant-1");

    var loginRequest = new LoginRequest("email@example.com", "ValidPass123!", false);

    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.username").value("emailuser"))
        .andExpect(jsonPath("$.user.email").value("email@example.com"))
        .andExpect(jsonPath("$.user.tenantId").value("tenant-1"));
  }

  @Test
  void login_WithRememberMe_ShouldReturnLongerExpiry() throws Exception {
    // Given - Create a test user
    createTestUser("rememberuser", "remember@example.com", "ValidPass123!", "tenant-1");

    var loginRequest = new LoginRequest("rememberuser", "ValidPass123!", true);

    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiresIn").value(604800)) // 7 days
        .andExpect(jsonPath("$.user.username").value("rememberuser"));
  }

  @Test
  void refreshToken_WithValidRefreshToken_ShouldReturn200WithNewToken() throws Exception {
    // Given - Create user and get initial tokens
    createTestUser("refreshuser", "refresh@example.com", "ValidPass123!", "tenant-1");

    // First, login to get refresh token
    var loginRequest = new LoginRequest("refreshuser", "ValidPass123!", false);
    var loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    var loginResponse = objectMapper.readValue(
        loginResult.getResponse().getContentAsString(),
        TokenResponse.class
    );

    var refreshRequest = new RefreshTokenRequest(loginResponse.refreshToken());

    // When & Then
    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").value(loginResponse.refreshToken())) // Same refresh token
        .andExpect(jsonPath("$.expiresIn").value(900)) // 15 minutes
        .andExpect(jsonPath("$.user.username").value("refreshuser"));
  }

  @Test
  void logout_WithValidToken_ShouldReturn200() throws Exception {
    // Given - Create user and login
    createTestUser("logoutuser", "logout@example.com", "ValidPass123!", "tenant-1");

    var loginRequest = new LoginRequest("logoutuser", "ValidPass123!", false);
    var loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    var loginResponse = objectMapper.readValue(
        loginResult.getResponse().getContentAsString(),
        TokenResponse.class
    );

    // When & Then
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + loginResponse.accessToken()))
        .andExpect(status().isOk());
  }

  @Test
  void authenticationFlow_WithTenantIsolation_ShouldMaintainSeparation() throws Exception {
    // Given - Create users in different tenants with same username
    createTestUser("sameuser", "user1@example.com", "ValidPass123!", "tenant-1");
    createTestUser("sameuser2", "user2@example.com", "ValidPass123!", "tenant-2");

    // When - Login as user from tenant-1
    var loginRequest1 = new LoginRequest("sameuser", "ValidPass123!", false);
    var result1 = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest1)))
        .andExpect(status().isOk())
        .andReturn();

    var tokenResponse1 = objectMapper.readValue(
        result1.getResponse().getContentAsString(),
        TokenResponse.class
    );

    // When - Login as user from tenant-2
    var loginRequest2 = new LoginRequest("sameuser2", "ValidPass123!", false);
    var result2 = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest2)))
        .andExpect(status().isOk())
        .andReturn();

    var tokenResponse2 = objectMapper.readValue(
        result2.getResponse().getContentAsString(),
        TokenResponse.class
    );

    // Then - Verify tenant isolation
    assertEquals("tenant-1", tokenResponse1.user().tenantId());
    assertEquals("tenant-2", tokenResponse2.user().tenantId());
    assertEquals("user1@example.com", tokenResponse1.user().email());
    assertEquals("user2@example.com", tokenResponse2.user().email());
    assertNotEquals(tokenResponse1.accessToken(), tokenResponse2.accessToken());
  }

  @Test
  void signup_WithMultiTenantScenario_ShouldAllowSameUsernameInDifferentTenants() throws Exception {
    // Given - Two signup requests with same username but different tenants
    var signupRequest1 = new SignupRequest(
        "multiuser",
        "multi1@example.com",
        "ValidPass123!",
        "Multi",
        "User1",
        "tenant-1"
    );

    var signupRequest2 = new SignupRequest(
        "multiuser",
        "multi2@example.com",
        "ValidPass123!",
        "Multi",
        "User2",
        "tenant-2"
    );

    // When & Then - Both signups should succeed
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupRequest1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("multiuser"))
        .andExpect(jsonPath("$.email").value("multi1@example.com"));

    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupRequest2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("multiuser"))
        .andExpect(jsonPath("$.email").value("multi2@example.com"));

    // Verify both users exist with different tenant IDs
    var users = userRepository.findAll();
    var tenant1Users = users.stream()
        .filter(u -> "tenant-1".equals(u.getTenantId()) && "multiuser".equals(u.getUsername()))
        .toList();
    var tenant2Users = users.stream()
        .filter(u -> "tenant-2".equals(u.getTenantId()) && "multiuser".equals(u.getUsername()))
        .toList();

    assertEquals(1, tenant1Users.size());
    assertEquals(1, tenant2Users.size());
    assertEquals("multi1@example.com", tenant1Users.get(0).getEmail());
    assertEquals("multi2@example.com", tenant2Users.get(0).getEmail());
  }

  @Test
  void completeAuthenticationFlow_WithValidCredentials_ShouldSucceed() throws Exception {
    // Given - Complete flow: signup -> login -> refresh -> logout
    var signupRequest = new SignupRequest(
        "flowuser",
        "flow@example.com",
        "ValidPass123!",
        "Flow",
        "User",
        "tenant-1"
    );

    // Step 1: Signup
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupRequest)))
        .andExpect(status().isCreated());

    // Step 2: Login
    var loginRequest = new LoginRequest("flowuser", "ValidPass123!", false);
    var loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    var loginResponse = objectMapper.readValue(
        loginResult.getResponse().getContentAsString(),
        TokenResponse.class
    );

    // Step 3: Refresh token
    var refreshRequest = new RefreshTokenRequest(loginResponse.refreshToken());
    var refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest)))
        .andExpect(status().isOk())
        .andReturn();

    var refreshResponse = objectMapper.readValue(
        refreshResult.getResponse().getContentAsString(),
        TokenResponse.class
    );

    // Step 4: Logout
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + refreshResponse.accessToken()))
        .andExpect(status().isOk());

    // Verify user context is maintained throughout the flow
    assertEquals("flowuser", loginResponse.user().username());
    assertEquals("flowuser", refreshResponse.user().username());
    assertEquals("tenant-1", loginResponse.user().tenantId());
    assertEquals("tenant-1", refreshResponse.user().tenantId());
  }

  private User createTestUser(String username, String email, String password, String tenantId) {
    var hashedPassword = passwordEncoder.encode(password);
    var user = new User(username, email, hashedPassword, "Test", "User", tenantId);

    // Add USER authority
    var userAuthority = authorityRepository.findByName("USER")
        .orElseGet(() -> authorityRepository.save(new Authority("USER", "Standard user role")));
    user.addAuthority(userAuthority);

    return userRepository.save(user);
  }
}