package com.iqscaffold.userservice.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for ValidUsername annotation.
 * Tests annotation properties and configuration.
 */
class ValidUsernameTest {

  @Test
  void annotation_shouldHaveCorrectProperties() throws Exception {
    var annotation = TestClass.class.getDeclaredField("username").getAnnotation(ValidUsername.class);

    assertNotNull(annotation);
    assertEquals("Username must contain only alphanumeric characters, underscores, and hyphens, and cannot start or end with special characters",
        annotation.message());
    assertEquals(0, annotation.groups().length);
    assertEquals(0, annotation.payload().length);
  }

  @Test
  void annotation_shouldBeDocumented() {
    assertTrue(ValidUsername.class.isAnnotationPresent(Documented.class));
  }

  @Test
  void annotation_shouldHaveCorrectTarget() {
    var target = ValidUsername.class.getAnnotation(Target.class);
    assertNotNull(target);
    assertArrayEquals(new ElementType[] {ElementType.FIELD, ElementType.PARAMETER}, target.value());
  }

  @Test
  void annotation_shouldHaveRuntimeRetention() {
    var retention = ValidUsername.class.getAnnotation(Retention.class);
    assertNotNull(retention);
    assertEquals(RetentionPolicy.RUNTIME, retention.value());
  }

  @Test
  void annotation_shouldBeValidatedByUsernameValidator() {
    var constraint = ValidUsername.class.getAnnotation(jakarta.validation.Constraint.class);
    assertNotNull(constraint);
    assertArrayEquals(new Class[] {UsernameValidator.class}, constraint.validatedBy());
  }

  // Test class to verify annotation usage
  private static class TestClass {
    @ValidUsername
    private String username;
  }
}

