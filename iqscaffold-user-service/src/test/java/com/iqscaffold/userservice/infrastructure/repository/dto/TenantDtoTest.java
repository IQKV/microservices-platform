package com.iqscaffold.userservice.infrastructure.repository.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TenantDtoTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidCreateTenantRequest() {
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "Acme Corp",
        "A great company",
        "acme.com",
        100,
        50,
        1000
    );

    assertEquals("tenant-123", request.tenantId());
    assertEquals("Acme Corp", request.name());
    assertEquals("A great company", request.description());
  }

  @Test
  void shouldValidateTenantIdNotBlank() {
    var request = new TenantDto.CreateTenantRequest(
        "",
        "Acme Corp",
        "Description",
        "acme.com",
        100,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateTenantIdLength() {
    var request = new TenantDto.CreateTenantRequest(
        "ab",
        "Acme Corp",
        "Description",
        "acme.com",
        100,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNameNotBlank() {
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "",
        "Description",
        "acme.com",
        100,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateMaxUsersMinimum() {
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "Acme Corp",
        "Description",
        "acme.com",
        0,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldCreateValidUpdateTenantRequest() {
    var request = new TenantDto.UpdateTenantRequest(
        "Updated Name",
        "Updated description",
        "newdomain.com",
        200,
        100,
        2000,
        true
    );

    assertEquals("Updated Name", request.name());
    assertTrue(request.enabled());
  }

  @Test
  void shouldAllowNullFieldsInUpdateRequest() {
    var request = new TenantDto.UpdateTenantRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    assertNull(request.name());
    assertNull(request.enabled());
  }

  @Test
  void shouldCreateTenantResponse() {
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();

    var response = new TenantDto.TenantResponse(
        1L,
        "tenant-123",
        "Acme Corp",
        "A great company",
        true,
        "acme.com",
        100,
        50,
        1000,
        null,  // subscriptionId
        null,  // subscriptionStatus
        null,  // subscriptionPlanCode
        createdAt,
        updatedAt,
        "admin"
    );

    assertEquals(1L, response.id());
    assertEquals("tenant-123", response.tenantId());
    assertEquals("Acme Corp", response.name());
    assertTrue(response.enabled());
  }

  @Test
  void shouldCreateTenantSummary() {
    var createdAt = LocalDateTime.now();

    var summary = new TenantDto.TenantSummary(
        "tenant-123",
        "Acme Corp",
        true,
        75L,
        100,
        createdAt
    );

    assertEquals("tenant-123", summary.tenantId());
    assertEquals(75L, summary.userCount());
    assertEquals(100, summary.maxUsers());
  }

  @Test
  void shouldCalculateUtilization() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(75L, 100);

    assertEquals(75.0, utilization);
  }

  @Test
  void shouldCalculateFullUtilization() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(100L, 100);

    assertEquals(100.0, utilization);
  }

  @Test
  void shouldCalculateOverUtilization() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(150L, 100);

    assertEquals(150.0, utilization);
  }

  @Test
  void shouldReturnNullUtilizationWhenMaxUsersIsNull() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(75L, null);

    assertNull(utilization);
  }

  @Test
  void shouldReturnNullUtilizationWhenMaxUsersIsZero() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(75L, 0);

    assertNull(utilization);
  }

  @Test
  void shouldReturnNullUtilizationWhenUserCountIsNull() {
    var utilization = TenantDto.TenantStatistics.calculateUtilization(null, 100);

    assertNull(utilization);
  }

  @Test
  void shouldCreateTenantStatistics() {
    var createdAt = LocalDateTime.now();

    var statistics = new TenantDto.TenantStatistics(
        "tenant-123",
        "Acme Corp",
        true,
        75L,
        100,
        75.0,
        createdAt
    );

    assertEquals("tenant-123", statistics.tenantId());
    assertEquals(75.0, statistics.userQuotaUtilization());
  }

  @Test
  void shouldCreateTenantConfiguration() {
    var config = new TenantDto.TenantConfiguration(
        "tenant-123",
        100,
        50,
        1000,
        true
    );

    assertEquals("tenant-123", config.tenantId());
    assertEquals(100, config.maxUsers());
    assertEquals(50, config.storageQuotaGb());
    assertEquals(1000, config.apiRateLimitPerMinute());
    assertTrue(config.enabled());
  }

  @Test
  void shouldCreateTenantResolutionResult() {
    var result = new TenantDto.TenantResolutionResult(
        "tenant-123",
        "JWT",
        "tenant-123",
        true
    );

    assertEquals("tenant-123", result.tenantId());
    assertEquals("JWT", result.resolutionMethod());
    assertEquals("tenant-123", result.sourceValue());
    assertTrue(result.isValid());
  }

  @Test
  void shouldCreateInvalidTenantResolutionResult() {
    var result = new TenantDto.TenantResolutionResult(
        null,
        "header",
        "invalid-tenant",
        false
    );

    assertNull(result.tenantId());
    assertFalse(result.isValid());
  }

  @Test
  void shouldValidateDescriptionLength() {
    var longDescription = "a".repeat(501);
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "Acme Corp",
        longDescription,
        "acme.com",
        100,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateDomainLength() {
    var longDomain = "a".repeat(256);
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "Acme Corp",
        "Description",
        longDomain,
        100,
        50,
        1000
    );

    Set<ConstraintViolation<TenantDto.CreateTenantRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAllowNullOptionalFields() {
    var request = new TenantDto.CreateTenantRequest(
        "tenant-123",
        "Acme Corp",
        null,
        null,
        null,
        null,
        null
    );

    assertTrue(validator.validate(request).isEmpty());
  }
}
