package org.gripday.userservice.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for UsernameValidator class.
 * Tests username format validation and security restrictions.
 */
class UsernameValidatorTest {

    private UsernameValidator usernameValidator;
    
    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usernameValidator = new UsernameValidator();
        usernameValidator.initialize(null);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void isValid_shouldReturnFalseForNullUsername() {
        assertFalse(usernameValidator.isValid(null, context));
    }

    @Test
    void isValid_shouldReturnFalseForEmptyUsername() {
        assertFalse(usernameValidator.isValid("", context));
        assertFalse(usernameValidator.isValid("   ", context));
    }

    @Test
    void isValid_shouldReturnFalseForInvalidFormats() {
        assertFalse(usernameValidator.isValid("_username", context)); // Starts with underscore
        assertFalse(usernameValidator.isValid("-username", context)); // Starts with hyphen
        assertFalse(usernameValidator.isValid("username_", context)); // Ends with underscore
        assertFalse(usernameValidator.isValid("username-", context)); // Ends with hyphen
        assertFalse(usernameValidator.isValid("user@name", context)); // Contains invalid character
        assertFalse(usernameValidator.isValid("user.name", context)); // Contains invalid character
        assertFalse(usernameValidator.isValid("user name", context)); // Contains space
    }

    @Test
    void isValid_shouldReturnFalseForReservedUsernames() {
        assertFalse(usernameValidator.isValid("admin", context));
        assertFalse(usernameValidator.isValid("ADMIN", context));
        assertFalse(usernameValidator.isValid("root", context));
        assertFalse(usernameValidator.isValid("system", context));
        assertFalse(usernameValidator.isValid("user", context));
        assertFalse(usernameValidator.isValid("guest", context));
        assertFalse(usernameValidator.isValid("test", context));
        assertFalse(usernameValidator.isValid("demo", context));
        assertFalse(usernameValidator.isValid("api", context));
        assertFalse(usernameValidator.isValid("www", context));
        assertFalse(usernameValidator.isValid("mail", context));
        assertFalse(usernameValidator.isValid("email", context));
        assertFalse(usernameValidator.isValid("support", context));
        assertFalse(usernameValidator.isValid("help", context));
        assertFalse(usernameValidator.isValid("info", context));
        assertFalse(usernameValidator.isValid("contact", context));
        assertFalse(usernameValidator.isValid("null", context));
        assertFalse(usernameValidator.isValid("undefined", context));
        assertFalse(usernameValidator.isValid("anonymous", context));
        assertFalse(usernameValidator.isValid("public", context));
        assertFalse(usernameValidator.isValid("private", context));
        assertFalse(usernameValidator.isValid("internal", context));
    }

    @Test
    void isValid_shouldReturnFalseForConsecutiveSpecialCharacters() {
        assertFalse(usernameValidator.isValid("user__name", context));
        assertFalse(usernameValidator.isValid("user--name", context));
        assertFalse(usernameValidator.isValid("user_-_name", context));
        assertFalse(usernameValidator.isValid("user-_-name", context));
    }

    @Test
    void isValid_shouldReturnTrueForValidUsernames() {
        assertTrue(usernameValidator.isValid("username", context));
        assertTrue(usernameValidator.isValid("user123", context));
        assertTrue(usernameValidator.isValid("user_name", context));
        assertTrue(usernameValidator.isValid("user-name", context));
        assertTrue(usernameValidator.isValid("user_name123", context));
        assertTrue(usernameValidator.isValid("user-name123", context));
        assertTrue(usernameValidator.isValid("123user", context));
        assertTrue(usernameValidator.isValid("user123name", context));
        assertTrue(usernameValidator.isValid("a", context)); // Single character
        assertTrue(usernameValidator.isValid("u", context)); // Single character
    }

    @Test
    void isValid_shouldHandleMixedCaseUsernames() {
        assertTrue(usernameValidator.isValid("UserName", context));
        assertTrue(usernameValidator.isValid("USER_NAME", context));
        assertTrue(usernameValidator.isValid("User-Name", context));
    }

    @Test
    void isValid_shouldHandleUsernamesWithNumbers() {
        assertTrue(usernameValidator.isValid("user123", context));
        assertTrue(usernameValidator.isValid("123user", context));
        assertTrue(usernameValidator.isValid("user1name2", context));
        assertTrue(usernameValidator.isValid("user123name", context));
    }

    @Test
    void isValid_shouldHandleSingleCharacterUsernames() {
        assertTrue(usernameValidator.isValid("a", context));
        assertTrue(usernameValidator.isValid("A", context));
        assertTrue(usernameValidator.isValid("1", context));
    }

    @Test
    void isValid_shouldHandleUsernamesWithSingleSpecialChar() {
        assertTrue(usernameValidator.isValid("user_name", context));
        assertTrue(usernameValidator.isValid("user-name", context));
        assertTrue(usernameValidator.isValid("user_name_123", context));
        assertTrue(usernameValidator.isValid("user-name-123", context));
    }

    @Test
    void isValid_shouldBeCaseInsensitiveForReservedUsernames() {
        assertFalse(usernameValidator.isValid("ADMIN", context));
        assertFalse(usernameValidator.isValid("Admin", context));
        assertFalse(usernameValidator.isValid("ADMINISTRATOR", context));
        assertFalse(usernameValidator.isValid("Administrator", context));
    }
}
