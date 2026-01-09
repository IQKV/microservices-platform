package com.iqscaffold.userservice.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class OrganizationDtoTest {

  @Test
  void shouldCreateOrganizationDto() {
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();

    var dto = new OrganizationDto(
        1L,                           // id
        "Acme Corporation",           // name
        "A leading technology company", // description
        "Technology",                 // industry
        "https://acme.com",          // website
        "+1-555-0100",               // phone
        "123 Main Street",           // address
        "San Francisco",             // city
        "USA",                       // country
        true,                        // enabled
        "tenant-1",                  // tenantId
        10L,                         // ownerUserId
        "billing@acme.com",          // billingEmail
        "acct_123",                  // stripeAccountId
        true,                        // chargesEnabled
        true,                        // payoutsEnabled
        "active",                    // subscriptionStatus
        "pro",                       // subscriptionPlan
        100,                         // maxUsers
        createdAt,                   // createdAt
        updatedAt,                   // updatedAt
        "admin"                      // createdBy
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
    assertEquals("tenant-1", dto.tenantId());
    assertEquals(10L, dto.ownerUserId());
    assertEquals("billing@acme.com", dto.billingEmail());
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        10L,
        "billing@acme.com",
        "acct_123",
        true,
        true,
        "active",
        "pro",
        100,
        createdAt,
        updatedAt,
        "admin"
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
        "tenant-1",
        10L,
        "billing@acme.com",
        "acct_123",
        true,
        true,
        "active",
        "pro",
        100,
        createdAt,
        updatedAt,
        "admin"
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
        "tenant-1",
        10L,
        "billing@acme.com",
        "acct_123",
        true,
        true,
        "active",
        "pro",
        100,
        createdAt,
        updatedAt,
        "admin"
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
        "tenant-1",
        10L,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
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
        "tenant-1",
        20L,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
    );

    assertNotEquals(dto1.ownerUserId(), dto2.ownerUserId());
  }
}
