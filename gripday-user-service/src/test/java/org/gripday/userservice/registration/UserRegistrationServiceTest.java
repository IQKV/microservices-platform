package org.gripday.userservice.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.gripday.userservice.emailverification.EmailVerificationService;
import org.gripday.userservice.security.InputSanitizer;
import org.gripday.userservice.security.SecurityAuditService;
import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.shared.AuthorityRepository;
import org.gripday.userservice.usermanagement.User;
import org.gripday.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for UserRegistrationService.
 */
@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthorityRepository authorityRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private SecurityAuditService securityAuditService;

  @Mock
  private InputSanitizer inputSanitizer;

  @Mock
  private EmailVerificationService emailVerificationService;

  private UserRegistrationService service;
  private SignupRequest signupRequest;
  private Authority userRole;

  @BeforeEach
  void setUp() {
    service = new UserRegistrationService(
        userRepository,
        authorityRepository,
        passwordEncoder,
        securityAuditService,
        inputSanitizer,
        emailVerificationService
    );

    signupRequest = new SignupRequest(
        "testuser",
        "test@example.com",
        "password123",
        "Test",
        "User",
        "tenant-123"
    );

    userRole = new Authority("USER", "Default user role");

    // Setup default mocks
    lenient().when(inputSanitizer.sanitizeUsername(anyString())).thenAnswer(i -> i.getArgument(0));
    lenient().when(inputSanitizer.sanitizeEmail(anyString())).thenAnswer(i -> i.getArgument(0));
    lenient().when(inputSanitizer.sanitizeName(anyString())).thenAnswer(i -> i.getArgument(0));
    lenient().when(inputSanitizer.sanitizeInput(anyString())).thenAnswer(i -> i.getArgument(0));
    lenient().when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
    lenient().when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);
  }

  @Test
  @DisplayName("Should register user successfully")
  void shouldRegisterUserSuccessfully() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    var response = service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.username()).isEqualTo("testuser");
    assertThat(response.email()).isEqualTo("test@example.com");
    assertThat(response.firstName()).isEqualTo("Test");
    assertThat(response.lastName()).isEqualTo("User");
    assertThat(response.emailVerified()).isFalse();
    assertThat(response.message()).contains("registered successfully");

    verify(userRepository).save(any(User.class));
    verify(securityAuditService).logUserRegistration("testuser", "test@example.com", "127.0.0.1", "Mozilla/5.0");
    verify(emailVerificationService).generateVerificationToken(any(User.class));
  }

  @Test
  @DisplayName("Should throw exception for unsafe input")
  void shouldThrowExceptionForUnsafeInput() {
    // Arrange
    when(inputSanitizer.isInputSafe("testuser")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(UserRegistrationService.UserRegistrationException.class)
        .hasMessageContaining("Invalid input detected");

    verify(securityAuditService).logSuspiciousActivity(
        eq("testuser"),
        eq("Potential XSS/injection attempt in registration"),
        eq("127.0.0.1"),
        eq("Mozilla/5.0")
    );
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for SQL injection attempt")
  void shouldThrowExceptionForSqlInjection() {
    // Arrange
    when(inputSanitizer.containsSqlInjection("testuser")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(UserRegistrationService.UserRegistrationException.class)
        .hasMessageContaining("Invalid input detected");

    verify(securityAuditService).logSuspiciousActivity(
        eq("testuser"),
        eq("SQL injection attempt in registration"),
        eq("127.0.0.1"),
        eq("Mozilla/5.0")
    );
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for duplicate username")
  void shouldThrowExceptionForDuplicateUsername() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(UserRegistrationService.UserRegistrationException.class)
        .hasMessageContaining("Username already exists");

    verify(securityAuditService).logFailedAuthentication(
        "testuser",
        "Registration failed - username exists",
        "127.0.0.1",
        "Mozilla/5.0"
    );
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for duplicate email")
  void shouldThrowExceptionForDuplicateEmail() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0"))
        .isInstanceOf(UserRegistrationService.UserRegistrationException.class)
        .hasMessageContaining("Email already exists");

    verify(securityAuditService).logFailedAuthentication(
        "test@example.com",
        "Registration failed - email exists",
        "127.0.0.1",
        "Mozilla/5.0"
    );
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should create USER role if it doesn't exist")
  void shouldCreateUserRoleIfNotExists() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.empty());
    when(authorityRepository.save(any(Authority.class))).thenReturn(userRole);

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    var response = service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    assertThat(response).isNotNull();
    verify(authorityRepository).save(any(Authority.class));
  }

  @Test
  @DisplayName("Should continue registration even if email verification fails")
  void shouldContinueRegistrationIfEmailVerificationFails() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(emailVerificationService.generateVerificationToken(any(User.class)))
        .thenThrow(new RuntimeException("Email service unavailable"));

    // Act
    var response = service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert - registration should still succeed
    assertThat(response).isNotNull();
    assertThat(response.username()).isEqualTo("testuser");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("Should sanitize all input fields")
  void shouldSanitizeAllInputFields() {
    // Arrange
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    verify(inputSanitizer).sanitizeUsername("testuser");
    verify(inputSanitizer).sanitizeEmail("test@example.com");
    verify(inputSanitizer).sanitizeName("Test");
    verify(inputSanitizer).sanitizeName("User");
  }

  @Test
  @DisplayName("Should set email verified to false for new users")
  void shouldSetEmailVerifiedToFalse() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    savedUser.setEmailVerified(false);
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    var response = service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    assertThat(response.emailVerified()).isFalse();
  }

  @Test
  @DisplayName("Should hash password before saving")
  void shouldHashPasswordBeforeSaving() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));

    var savedUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    service.registerUser(signupRequest, "127.0.0.1", "Mozilla/5.0");

    // Assert
    verify(passwordEncoder).encode("password123");
  }
}
