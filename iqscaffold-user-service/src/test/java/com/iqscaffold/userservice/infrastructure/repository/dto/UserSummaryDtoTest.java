package com.iqscaffold.userservice.infrastructure.repository.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserSummaryDtoTest {

  @Test
  void shouldCreateUserSummaryDto() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        LocalDateTime.now()
    );

    assertEquals(1L, dto.id());
    assertEquals("john.doe", dto.username());
    assertEquals("john.doe@example.com", dto.email());
    assertEquals("John Doe", dto.displayName());
    assertTrue(dto.enabled());
    assertEquals("tenant-1", dto.tenantId());
  }

  @Test
  void shouldCreateUserSummaryDtoWithFactoryMethod() {
    var dto = UserSummaryDto.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        "tenant-1"
    );

    assertEquals("John Doe", dto.displayName());
    assertNull(dto.lastActivity());
  }

  @Test
  void shouldCreateDisplayNameFromFirstAndLastName() {
    var dto = UserSummaryDto.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        "tenant-1"
    );

    assertEquals("John Doe", dto.displayName());
  }

  @Test
  void shouldFallbackToUsernameWhenNamesAreNull() {
    var dto = UserSummaryDto.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        null,
        null,
        true,
        "tenant-1"
    );

    assertEquals("john.doe", dto.displayName());
  }

  @Test
  void shouldFallbackToUsernameWhenNamesAreBlank() {
    var dto = UserSummaryDto.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "",
        "",
        true,
        "tenant-1"
    );

    assertEquals("john.doe", dto.displayName());
  }

  @Test
  void shouldCreateWithActivity() {
    var lastActivity = LocalDateTime.now().minusDays(5);
    var dto = UserSummaryDto.withActivity(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        "tenant-1",
        lastActivity
    );

    assertEquals(lastActivity, dto.lastActivity());
  }

  @Test
  void shouldCheckIfUserIsEnabled() {
    var enabledDto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        null
    );

    assertTrue(enabledDto.isEnabled());
  }

  @Test
  void shouldCheckIfUserIsDisabled() {
    var disabledDto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        false,
        "tenant-1",
        null
    );

    assertFalse(disabledDto.isEnabled());
  }

  @Test
  void shouldHandleNullEnabledStatus() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        null,
        "tenant-1",
        null
    );

    assertFalse(dto.isEnabled());
  }

  @Test
  void shouldReturnActiveStatus() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        null
    );

    assertEquals("ACTIVE", dto.status());
  }

  @Test
  void shouldReturnInactiveStatus() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        false,
        "tenant-1",
        null
    );

    assertEquals("INACTIVE", dto.status());
  }

  @Test
  void shouldCheckRecentActivity() {
    var recentActivity = LocalDateTime.now().minusDays(10);
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        recentActivity
    );

    assertTrue(dto.hasRecentActivity());
  }

  @Test
  void shouldCheckNoRecentActivity() {
    var oldActivity = LocalDateTime.now().minusDays(60);
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        oldActivity
    );

    assertFalse(dto.hasRecentActivity());
  }

  @Test
  void shouldHandleNullLastActivity() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        null
    );

    assertFalse(dto.hasRecentActivity());
  }

  @Test
  void shouldReturnRecentlyActiveStatus() {
    var recentActivity = LocalDateTime.now().minusDays(5);
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        recentActivity
    );

    assertEquals("Recently active", dto.activityStatus());
  }

  @Test
  void shouldReturnInactiveActivityStatus() {
    var oldActivity = LocalDateTime.now().minusDays(60);
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        oldActivity
    );

    assertEquals("Inactive", dto.activityStatus());
  }

  @Test
  void shouldReturnNoActivityRecordedStatus() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        null
    );

    assertEquals("No activity recorded", dto.activityStatus());
  }

  @Test
  void shouldGenerateLogString() {
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        null
    );

    var logString = dto.toLogString();
    assertTrue(logString.contains("id=1"));
    assertTrue(logString.contains("username=john.doe"));
    assertTrue(logString.contains("tenant=tenant-1"));
    assertTrue(logString.contains("enabled=true"));
  }

  @Test
  void shouldCheckActivityAtBoundary() {
    var exactlyThirtyDaysAgo = LocalDateTime.now().minusDays(30).minusSeconds(1);
    var dto = new UserSummaryDto(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John Doe",
        true,
        "tenant-1",
        exactlyThirtyDaysAgo
    );

    assertFalse(dto.hasRecentActivity());
  }
}
