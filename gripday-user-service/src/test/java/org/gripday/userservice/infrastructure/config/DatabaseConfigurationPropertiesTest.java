package org.gripday.userservice.infrastructure.config;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DatabaseConfigurationPropertiesTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidDatabaseConfiguration() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    var config = new DatabaseConfigurationProperties(
        "jdbc:postgresql://localhost:5432/userdb",
        "dbuser",
        "dbpass",
        pool,
        migration
    );

    assertEquals("jdbc:postgresql://localhost:5432/userdb", config.url());
    assertEquals("dbuser", config.username());
    assertEquals("dbpass", config.password());
    assertNotNull(config.pool());
    assertNotNull(config.migration());
  }

  @Test
  void shouldValidateDatabaseUrlStartsWithJdbc() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties(
            "postgresql://localhost:5432/userdb",
            "dbuser",
            "dbpass",
            pool,
            migration
        )
    );
  }

  @Test
  void shouldValidateUrlNotBlank() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    // Empty string will fail the "jdbc:" validation before constraint validation
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties(
            "",
            "dbuser",
            "dbpass",
            pool,
            migration
        )
    );
  }

  @Test
  void shouldValidateUsernameNotBlank() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    var config = new DatabaseConfigurationProperties(
        "jdbc:postgresql://localhost:5432/userdb",
        "",
        "dbpass",
        pool,
        migration
    );

    Set<ConstraintViolation<DatabaseConfigurationProperties>> violations = validator.validate(config);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePasswordNotBlank() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    var config = new DatabaseConfigurationProperties(
        "jdbc:postgresql://localhost:5432/userdb",
        "dbuser",
        "",
        pool,
        migration
    );

    Set<ConstraintViolation<DatabaseConfigurationProperties>> violations = validator.validate(config);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePoolMaximumSize() {
    // Maximum size of 0 with minimum idle of 5 will fail the validation in constructor
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties.PoolProperties(
            0,
            5,
            Duration.ofSeconds(30),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30)
        )
    );
  }

  @Test
  void shouldValidateMinimumIdleNotGreaterThanMaximumSize() {
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties.PoolProperties(
            10,
            20,
            Duration.ofSeconds(30),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30)
        )
    );
  }

  @Test
  void shouldValidateConnectionTimeoutNotNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties.PoolProperties(
            10,
            5,
            Duration.ofSeconds(-1),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30)
        )
    );
  }

  @Test
  void shouldValidateIdleTimeoutNotNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties.PoolProperties(
            10,
            5,
            Duration.ofSeconds(30),
            Duration.ofMinutes(-1),
            Duration.ofMinutes(30)
        )
    );
  }

  @Test
  void shouldValidateMaxLifetimeNotNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        new DatabaseConfigurationProperties.PoolProperties(
            10,
            5,
            Duration.ofSeconds(30),
            Duration.ofMinutes(10),
            Duration.ofMinutes(-1)
        )
    );
  }

  @Test
  void shouldAllowMinimumIdleEqualToMaximumSize() {
    var pool = new DatabaseConfigurationProperties.PoolProperties(
        10,
        10,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10),
        Duration.ofMinutes(30)
    );

    assertEquals(10, pool.maximumSize());
    assertEquals(10, pool.minimumIdle());
  }

  @Test
  void shouldValidateMigrationContextsNotBlank() {
    var migration = new DatabaseConfigurationProperties.MigrationProperties(
        true,
        "",
        true
    );

    Set<ConstraintViolation<DatabaseConfigurationProperties.MigrationProperties>> violations = validator.validate(migration);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldCreateDisabledMigration() {
    var migration = new DatabaseConfigurationProperties.MigrationProperties(
        false,
        "production",
        false
    );

    assertFalse(migration.enabled());
    assertFalse(migration.validateOnMigrate());
  }

  @Test
  void shouldAcceptVariousJdbcUrls() {
    var pool = createValidPoolProperties();
    var migration = createValidMigrationProperties();

    var urls = new String[] {
        "jdbc:postgresql://localhost:5432/db",
        "jdbc:mysql://localhost:3306/db",
        "jdbc:h2:mem:testdb",
        "jdbc:oracle:thin:@localhost:1521:db"
    };

    for (final var url : urls) {
      var config = new DatabaseConfigurationProperties(url, "user", "pass", pool, migration);
      assertEquals(url, config.url());
    }
  }

  private DatabaseConfigurationProperties.PoolProperties createValidPoolProperties() {
    return new DatabaseConfigurationProperties.PoolProperties(
        20,
        5,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10),
        Duration.ofMinutes(30)
    );
  }

  private DatabaseConfigurationProperties.MigrationProperties createValidMigrationProperties() {
    return new DatabaseConfigurationProperties.MigrationProperties(
        true,
        "production",
        true
    );
  }
}
