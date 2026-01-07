package com.iqscaffold.userservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.iqscaffold.userservice.config.IqScaffoldProperties;
import com.iqscaffold.userservice.config.RedisConfig.TenantAwareSessionService;
import com.iqscaffold.userservice.security.AccountLockoutService;
import com.iqscaffold.userservice.security.InputSanitizer;
import com.iqscaffold.userservice.security.SecurityAuditService;
import com.iqscaffold.userservice.shared.Authority;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests for user locale update functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Update User Locale Tests")
class UpdateUserLocaleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private InputSanitizer inputSanitizer;

    @Mock
    private TenantAwareSessionService sessionService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private IqScaffoldProperties iqScaffoldProperties;

    private AuthenticationService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup i18n configuration mock
        var i18nConfig = new IqScaffoldProperties.I18n(
            java.util.List.of("en", "es", "fr"),
            "en",
            "i18n/messages",
            java.time.Duration.ofHours(1),
            false,
            true
        );
        lenient().when(iqScaffoldProperties.i18n()).thenReturn(i18nConfig);

        service = new AuthenticationServiceImpl(
            userRepository,
            passwordEncoder,
            jwtService,
            accountLockoutService,
            securityAuditService,
            inputSanitizer,
            sessionService,
            meterRegistry,
            iqScaffoldProperties
        );

        // Setup test user
        testUser = new User("testuser", "test@example.com", "hashed-password", "Test", "User", "tenant-123");
        setUserId(testUser, 1L);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setPreferredLocale("en");

        var authority = new Authority("ROLE_USER", "User role");
        testUser.setAuthorities(java.util.Set.of(authority));
    }

    @Test
    @DisplayName("Should update user locale successfully")
    void shouldUpdateUserLocaleSuccessfully() {
        // Given
        Long userId = 1L;
        String newLocale = "es";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        service.updateUserLocale(userId, newLocale);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPreferredLocale()).isEqualTo("es");
        
        verify(securityAuditService).logUserLocaleChange(userId, "testuser", newLocale);
    }

    @Test
    @DisplayName("Should throw exception for unsupported locale")
    void shouldThrowExceptionForUnsupportedLocale() {
        // Given
        Long userId = 1L;
        String unsupportedLocale = "de";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> service.updateUserLocale(userId, unsupportedLocale))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Unsupported locale: de")
            .hasMessageContaining("Supported locales: [en, es, fr]");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        Long userId = 999L;
        String locale = "es";
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.updateUserLocale(userId, locale))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("User not found");
    }

    /**
     * Helper method to set user ID using reflection (since it's generated)
     */
    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID", e);
        }
    }
}