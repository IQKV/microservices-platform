package org.gripday.authservice.unit;

import org.gripday.authservice.domain.service.SecurityAuditService;
import org.gripday.authservice.domain.service.UserRegistrationService;
import org.gripday.authservice.infrastructure.entity.Authority;
import org.gripday.authservice.infrastructure.entity.User;
import org.gripday.authservice.infrastructure.repository.AuthorityRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.SignupRequest;
import org.gripday.authservice.presentation.validation.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserRegistrationService focusing on happy path scenarios.
 * Tests successful user registration with valid inputs and proper security measures.
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
    private org.gripday.authservice.domain.service.EmailVerificationService emailVerificationService;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(
            userRepository, authorityRepository, passwordEncoder, securityAuditService, inputSanitizer, emailVerificationService
        );
    }

    @Test
    void registerUser_WithValidRequest_ShouldSucceed() {
        // Given
        var request = new SignupRequest(
            "newuser", "new@example.com", "ValidPass123!", 
            "John", "Doe", "tenant-1"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization - all inputs are safe
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.userId());
        assertEquals("newuser", result.username());
        assertEquals("new@example.com", result.email());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertFalse(result.emailVerified());
        assertEquals("User registered successfully. Please verify your email.", result.message());

        // Verify interactions
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("ValidPass123!");
        verify(userRepository).save(argThat(user -> !user.getEmailVerified())); // Verify emailVerified is false
        verify(securityAuditService).logUserRegistration(eq("newuser"), eq("new@example.com"), eq(ipAddress), eq(userAgent));
        verify(emailVerificationService).generateVerificationToken(any(User.class)); // Verify email verification is triggered
    }

    @Test
    void registerUser_WithSanitizedInputs_ShouldUseSanitizedValues() {
        // Given - SignupRequest already trims inputs in its compact constructor
        var request = new SignupRequest(
            "  newuser  ", "  NEW@EXAMPLE.COM  ", "ValidPass123!", 
            "  John  ", "  Doe  ", "tenant-1"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization - inputs are already trimmed by SignupRequest
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls with sanitized values
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.username());
        assertEquals("new@example.com", result.email());

        // Verify sanitization was called with already-trimmed values
        verify(inputSanitizer).sanitizeUsername("newuser");
        verify(inputSanitizer).sanitizeEmail("new@example.com");
        verify(inputSanitizer).sanitizeName("John");
        verify(inputSanitizer).sanitizeName("Doe");
        verify(emailVerificationService).generateVerificationToken(any(User.class));
    }

    @Test
    void registerUser_WithNewUserRole_ShouldCreateDefaultRole() {
        // Given
        var request = new SignupRequest(
            "newuser", "new@example.com", "ValidPass123!", 
            "John", "Doe", "tenant-1"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var newUserRole = new Authority("USER", "Default user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls - USER role doesn't exist initially
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.empty());
        when(authorityRepository.save(any(Authority.class))).thenReturn(newUserRole);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.username());

        // Verify that a new USER role was created
        verify(authorityRepository).findByName("USER");
        verify(authorityRepository).save(any(Authority.class));
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).generateVerificationToken(any(User.class));
    }

    @Test
    void registerUser_WithDefaultTenantId_ShouldUseProvidedTenant() {
        // Given
        var request = new SignupRequest(
            "newuser", "new@example.com", "ValidPass123!", 
            "John", "Doe", "custom-tenant"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            var user = (User) invocation.getArgument(0);
            assertEquals("custom-tenant", user.getTenantId());
            return savedUser;
        });

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        verify(userRepository).save(argThat(user -> "custom-tenant".equals(user.getTenantId())));
        verify(emailVerificationService).generateVerificationToken(any(User.class));
    }

    @Test
    void registerUser_WithValidMultiTenantScenario_ShouldIsolateTenants() {
        // Given - Two users with same username but different tenants
        var request1 = new SignupRequest(
            "sameuser", "user1@example.com", "ValidPass123!", 
            "User", "One", "tenant-1"
        );
        var request2 = new SignupRequest(
            "sameuser", "user2@example.com", "ValidPass123!", 
            "User", "Two", "tenant-2"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser1 = createTestUser(1L, "sameuser", "user1@example.com");
        var savedUser2 = createTestUser(2L, "sameuser", "user2@example.com");

        // Mock input sanitization for both requests
        when(inputSanitizer.sanitizeUsername("sameuser")).thenReturn("sameuser");
        when(inputSanitizer.sanitizeEmail(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inputSanitizer.sanitizeName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls - usernames don't conflict across tenants
        when(userRepository.existsByUsername("sameuser")).thenReturn(false);
        when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("user2@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser1)
            .thenReturn(savedUser2);

        // When
        var result1 = userRegistrationService.registerUser(request1, ipAddress, userAgent);
        var result2 = userRegistrationService.registerUser(request2, ipAddress, userAgent);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("sameuser", result1.username());
        assertEquals("sameuser", result2.username());
        assertEquals("user1@example.com", result1.email());
        assertEquals("user2@example.com", result2.email());

        // Verify both users were saved (tenant isolation allows same username)
        verify(userRepository, times(2)).save(any(User.class));
        verify(emailVerificationService, times(2)).generateVerificationToken(any(User.class));
    }

    @Test
    void registerUser_ShouldTriggerEmailVerificationAfterRegistration() {
        // Given
        var request = new SignupRequest(
            "newuser", "new@example.com", "ValidPass123!", 
            "John", "Doe", "tenant-1"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Mock email verification service
        when(emailVerificationService.generateVerificationToken(any(User.class))).thenReturn("verification-token");

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertFalse(result.emailVerified());

        // Verify email verification was triggered
        verify(emailVerificationService).generateVerificationToken(argThat(user -> 
            "newuser".equals(user.getUsername()) && 
            "new@example.com".equals(user.getEmail()) &&
            !user.getEmailVerified()
        ));
    }

    @Test
    void registerUser_WhenEmailVerificationFails_ShouldStillCompleteRegistration() {
        // Given
        var request = new SignupRequest(
            "newuser", "new@example.com", "ValidPass123!", 
            "John", "Doe", "tenant-1"
        );
        var ipAddress = "192.168.1.1";
        var userAgent = "Mozilla/5.0";

        var userRole = new Authority("USER", "Standard user role");
        var savedUser = createTestUser(1L, "newuser", "new@example.com");

        // Mock input sanitization
        when(inputSanitizer.sanitizeUsername("newuser")).thenReturn("newuser");
        when(inputSanitizer.sanitizeEmail("new@example.com")).thenReturn("new@example.com");
        when(inputSanitizer.sanitizeName("John")).thenReturn("John");
        when(inputSanitizer.sanitizeName("Doe")).thenReturn("Doe");
        when(inputSanitizer.isInputSafe(anyString())).thenReturn(true);
        when(inputSanitizer.containsSqlInjection(anyString())).thenReturn(false);

        // Mock repository calls
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Mock email verification service to throw exception
        when(emailVerificationService.generateVerificationToken(any(User.class)))
            .thenThrow(new RuntimeException("Email service unavailable"));

        // When
        var result = userRegistrationService.registerUser(request, ipAddress, userAgent);

        // Then - Registration should still succeed even if email verification fails
        assertNotNull(result);
        assertEquals("newuser", result.username());
        assertEquals("new@example.com", result.email());
        assertFalse(result.emailVerified());

        // Verify user was still saved
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).generateVerificationToken(any(User.class));
    }

    private User createTestUser(Long id, String username, String email) {
        var user = new User(username, email, "hashedPassword", "John", "Doe", "tenant-1");
        
        // Use reflection to set the ID for testing
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            // Ignore for test purposes
        }
        
        return user;
    }
}