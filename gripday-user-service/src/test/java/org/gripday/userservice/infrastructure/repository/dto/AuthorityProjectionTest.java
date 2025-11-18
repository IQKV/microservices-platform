package org.gripday.userservice.infrastructure.repository.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AuthorityProjectionTest {

  @Test
  void shouldCreateAuthorityProjection() {
    var createdAt = LocalDateTime.now();
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Administrator role",
        createdAt,
        10L
    );

    assertEquals(1L, projection.id());
    assertEquals("ADMIN", projection.name());
    assertEquals("Administrator role", projection.description());
    assertEquals(createdAt, projection.createdAt());
    assertEquals(10L, projection.userCount());
  }

  @Test
  void shouldCreateWithoutUserCount() {
    var createdAt = LocalDateTime.now();
    var projection = AuthorityProjection.of(
        1L,
        "USER",
        "Standard user role",
        createdAt
    );

    assertEquals(0L, projection.userCount());
  }

  @Test
  void shouldCreateWithUserCount() {
    var createdAt = LocalDateTime.now();
    var projection = AuthorityProjection.withUserCount(
        1L,
        "ADMIN",
        "Administrator role",
        createdAt,
        25L
    );

    assertEquals(25L, projection.userCount());
  }

  @Test
  void shouldCheckIfHasUsers() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Admin",
        LocalDateTime.now(),
        5L
    );

    assertTrue(projection.hasUsers());
  }

  @Test
  void shouldCheckIfHasNoUsers() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Admin",
        LocalDateTime.now(),
        0L
    );

    assertFalse(projection.hasUsers());
  }

  @Test
  void shouldHandleNullUserCount() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Admin",
        LocalDateTime.now(),
        null
    );

    assertFalse(projection.hasUsers());
  }

  @Test
  void shouldGenerateDisplayNameWithDescription() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Administrator role",
        LocalDateTime.now(),
        10L
    );

    assertEquals("ADMIN - Administrator role", projection.displayName());
  }

  @Test
  void shouldGenerateDisplayNameWithoutDescription() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        null,
        LocalDateTime.now(),
        10L
    );

    assertEquals("ADMIN", projection.displayName());
  }

  @Test
  void shouldGenerateDisplayNameWithBlankDescription() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "",
        LocalDateTime.now(),
        10L
    );

    assertEquals("ADMIN", projection.displayName());
  }

  @Test
  void shouldIdentifyAdminAuthority() {
    var adminProjection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Admin",
        LocalDateTime.now(),
        5L
    );

    assertTrue(adminProjection.isAdminAuthority());
  }

  @Test
  void shouldIdentifySuperAdminAuthority() {
    var superAdminProjection = new AuthorityProjection(
        1L,
        "SUPER_ADMIN",
        "Super Admin",
        LocalDateTime.now(),
        2L
    );

    assertTrue(superAdminProjection.isAdminAuthority());
  }

  @Test
  void shouldNotIdentifyUserAsAdmin() {
    var userProjection = new AuthorityProjection(
        1L,
        "USER",
        "User",
        LocalDateTime.now(),
        100L
    );

    assertFalse(userProjection.isAdminAuthority());
  }

  @Test
  void shouldIdentifyDefaultAuthority() {
    var authorities = new String[]{"USER", "BASIC_USER", "STANDARD_USER"};

    for (var authority : authorities) {
      var projection = new AuthorityProjection(
          1L,
          authority,
          "Default",
          LocalDateTime.now(),
          50L
      );
      assertTrue(projection.isDefaultAuthority(), authority + " should be default");
    }
  }

  @Test
  void shouldNotIdentifyAdminAsDefault() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Admin",
        LocalDateTime.now(),
        5L
    );

    assertFalse(projection.isDefaultAuthority());
  }

  @Test
  void shouldReturnCorrectAuthorityLevel() {
    var superAdmin = new AuthorityProjection(1L, "SUPER_ADMIN", "", LocalDateTime.now(), 0L);
    var admin = new AuthorityProjection(2L, "ADMIN", "", LocalDateTime.now(), 0L);
    var user = new AuthorityProjection(3L, "USER", "", LocalDateTime.now(), 0L);
    var custom = new AuthorityProjection(4L, "CUSTOM", "", LocalDateTime.now(), 0L);

    assertEquals(100, superAdmin.authorityLevel());
    assertEquals(50, admin.authorityLevel());
    assertEquals(10, user.authorityLevel());
    assertEquals(1, custom.authorityLevel());
  }

  @Test
  void shouldReturnLevelForBasicUser() {
    var basicUser = new AuthorityProjection(1L, "BASIC_USER", "", LocalDateTime.now(), 0L);
    assertEquals(10, basicUser.authorityLevel());
  }

  @Test
  void shouldReturnLevelForStandardUser() {
    var standardUser = new AuthorityProjection(1L, "STANDARD_USER", "", LocalDateTime.now(), 0L);
    assertEquals(10, standardUser.authorityLevel());
  }

  @Test
  void shouldGenerateLogString() {
    var projection = new AuthorityProjection(
        1L,
        "ADMIN",
        "Administrator",
        LocalDateTime.now(),
        15L
    );

    var logString = projection.toLogString();
    assertTrue(logString.contains("id=1"));
    assertTrue(logString.contains("name=ADMIN"));
    assertTrue(logString.contains("userCount=15"));
  }

  @Test
  void shouldCompareAuthorityLevels() {
    var superAdmin = new AuthorityProjection(1L, "SUPER_ADMIN", "", LocalDateTime.now(), 0L);
    var admin = new AuthorityProjection(2L, "ADMIN", "", LocalDateTime.now(), 0L);
    var user = new AuthorityProjection(3L, "USER", "", LocalDateTime.now(), 0L);

    assertTrue(superAdmin.authorityLevel() > admin.authorityLevel());
    assertTrue(admin.authorityLevel() > user.authorityLevel());
  }
}
