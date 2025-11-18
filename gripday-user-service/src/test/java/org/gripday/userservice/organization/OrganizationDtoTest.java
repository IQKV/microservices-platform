package org.gripday.userservice.organization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class OrganizationDtoTest {

  @Test
  void shouldCreateOrganizationDto() {
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();

    var dto = new OrganizationDto(
        1L,
        "Acme Corporation",
        "A leading technology company",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        10L,
        "john.doe",
        "tenant-1",
        createdAt,
        updatedAt
    );

    assertEquals(1L, dto.id());
    assertEquals("Acme Corporation", dto.name());
    assertEquals("A leading technology company", dto.description());
    assertEquals("Technology", dto.industry());
    assertEquals("https://acme.com", dto.website());
    assertEquals("+1-555-0100", dto.phone());
    assertEquals("123 Main Street", dto.address());
    assertEquals("San Francisco", dto.city());
    assertEquals("USA", dto.country());
    assertTrue(dto.enabled());
    assertEquals(10L, dto.ownerId());
    assertEquals("john.doe", dto.ownerUsername());
    assertEquals("tenant-1", dto.tenantId());
    assertEquals(createdAt, dto.createdAt());
    assertEquals(updatedAt, dto.updatedAt());
  }

  @Test
  void shouldCheckIfOrganizationIsActive() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertTrue(dto.isActive());
  }

  @Test
  void shouldCheckIfOrganizationIsInactive() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertFalse(dto.isActive());
  }

  @Test
  void shouldHandleNullEnabledStatus() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertFalse(dto.isActive());
  }

  @Test
  void shouldGetLocationWithCityAndCountry() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        "San Francisco",
        "USA",
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertEquals("San Francisco, USA", dto.getLocation());
  }

  @Test
  void shouldGetLocationWithOnlyCity() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        "San Francisco",
        null,
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertEquals("San Francisco", dto.getLocation());
  }

  @Test
  void shouldGetLocationWithOnlyCountry() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        "USA",
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertEquals("USA", dto.getLocation());
  }

  @Test
  void shouldGetEmptyLocationWhenBothNull() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertEquals("", dto.getLocation());
  }

  @Test
  void shouldCreateMinimalDto() {
    var dto = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null,
        null,
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertEquals("Acme Corp", dto.name());
    assertNull(dto.description());
    assertNull(dto.industry());
  }

  @Test
  void shouldHandleCompleteOrganizationData() {
    var createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    var updatedAt = LocalDateTime.of(2024, 1, 15, 14, 30);

    var dto = new OrganizationDto(
        1L,
        "Acme Corporation",
        "Leading tech company",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street, Suite 100",
        "San Francisco",
        "USA",
        true,
        10L,
        "john.doe",
        "tenant-1",
        createdAt,
        updatedAt
    );

    assertTrue(dto.isActive());
    assertEquals("San Francisco, USA", dto.getLocation());
    assertEquals(createdAt, dto.createdAt());
    assertEquals(updatedAt, dto.updatedAt());
  }

  @Test
  void shouldCompareOrganizationDtos() {
    var createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    var updatedAt = LocalDateTime.of(2024, 1, 15, 14, 30);

    var dto1 = new OrganizationDto(
        1L,
        "Acme Corp",
        "Description",
        "Tech",
        "https://acme.com",
        "+1-555-0100",
        "123 Main St",
        "SF",
        "USA",
        true,
        10L,
        "john.doe",
        "tenant-1",
        createdAt,
        updatedAt
    );

    var dto2 = new OrganizationDto(
        1L,
        "Acme Corp",
        "Description",
        "Tech",
        "https://acme.com",
        "+1-555-0100",
        "123 Main St",
        "SF",
        "USA",
        true,
        10L,
        "john.doe",
        "tenant-1",
        createdAt,
        updatedAt
    );

    assertEquals(dto1, dto2);
    assertEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  void shouldHandleDifferentOwners() {
    var dto1 = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        10L,
        "john.doe",
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    var dto2 = new OrganizationDto(
        1L,
        "Acme Corp",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        20L,
        "jane.doe",
        "tenant-1",
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    assertNotEquals(dto1.ownerId(), dto2.ownerId());
    assertNotEquals(dto1.ownerUsername(), dto2.ownerUsername());
  }
}
