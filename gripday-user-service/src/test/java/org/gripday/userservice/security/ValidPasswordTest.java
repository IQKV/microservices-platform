package org.gripday.userservice.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Unit tests for ValidPassword annotation.
 * Tests annotation properties and configuration.
 */
class ValidPasswordTest {

    @Test
    void annotation_shouldHaveCorrectProperties() throws Exception {
        var annotation = TestClass.class.getDeclaredField("password").getAnnotation(ValidPassword.class);
        
        assertNotNull(annotation);
        assertEquals("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character", 
                    annotation.message());
        assertEquals(0, annotation.groups().length);
        assertEquals(0, annotation.payload().length);
    }

    @Test
    void annotation_shouldBeDocumented() {
        assertTrue(ValidPassword.class.isAnnotationPresent(Documented.class));
    }

    @Test
    void annotation_shouldHaveCorrectTarget() {
        var target = ValidPassword.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.FIELD, ElementType.PARAMETER}, target.value());
    }

    @Test
    void annotation_shouldHaveRuntimeRetention() {
        var retention = ValidPassword.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void annotation_shouldBeValidatedByPasswordValidator() {
        var constraint = ValidPassword.class.getAnnotation(jakarta.validation.Constraint.class);
        assertNotNull(constraint);
        assertEquals(1, constraint.validatedBy().length);
        assertEquals(PasswordValidator.class, constraint.validatedBy()[0]);
    }

    // Test class to verify annotation usage
    private static class TestClass {
        @ValidPassword
        private String password;
    }
}
