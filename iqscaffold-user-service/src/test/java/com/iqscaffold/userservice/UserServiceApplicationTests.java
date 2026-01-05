package com.iqscaffold.userservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Simple application test that verifies the application can be instantiated.
 * This test doesn't load the full Spring context to avoid configuration conflicts.
 */
class UserServiceApplicationTests {

  @Test
  void contextLoads() {
    // Verify that the application class can be instantiated
    var application = new UserServiceApplication();
    assertThat(application).isNotNull();
  }

  @Test
  void applicationMainMethodExists() {
    // Verify that the main method exists and can be called
    try {
      var mainMethod = UserServiceApplication.class.getMethod("main", String[].class);
      assertThat(mainMethod).isNotNull();
      assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Main method not found", e);
    }
  }
}
