package com.iqscaffold.userservice.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateOrganizationRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidUpdateRequest() {
    var request = new UpdateOrganizationRequest(
        "Updated Acme Corporation",
        "Updated description",
        "Technology",
        "https://newacme.com",
        "+1-555-0200",
        "456 New Street",
        "New York",
        "USA",
        true,
        2L
    );

    assertEquals("Updated Acme Corporation", request.name());
    assertEquals("Updated description", request.description());
    assertTrue(request.enabled());
    assertEquals(2L, request.ownerId());
  }

  @Test
  void shouldAllowAllNullFields() {
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    assertTrue(validator.validate(request).isEmpty());
    assertNull(request.name());
    assertNull(request.enabled());
  }

  @Test
  void shouldValidateNameMinLength() {
    var request = new UpdateOrganizationRequest(
        "A",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 2 and 255 characters")));
  }

  @Test
  void shouldValidateNameMaxLength() {
    var longName = "A".repeat(256);
    var request = new UpdateOrganizationRequest(
        longName,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateDescriptionMaxLength() {
    var longDescription = "A".repeat(1001);
    var request = new UpdateOrganizationRequest(
        null,
        longDescription,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateIndustryMaxLength() {
    var longIndustry = "A".repeat(101);
    var request = new UpdateOrganizationRequest(
        null,
        null,
        longIndustry,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateWebsiteMaxLength() {
    var longWebsite = "https://" + "a".repeat(250) + ".com";
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        longWebsite,
        null,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePhoneMaxLength() {
    var longPhone = "1".repeat(51);
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        longPhone,
        null,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateAddressMaxLength() {
    var longAddress = "A".repeat(501);
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        longAddress,
        null,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateCityMaxLength() {
    var longCity = "A".repeat(101);
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        longCity,
        null,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateCountryMaxLength() {
    var longCountry = "A".repeat(101);
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        longCountry,
        null,
        null
    );

    Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldUpdateOnlyName() {
    var request = new UpdateOrganizationRequest(
        "New Name",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    assertEquals("New Name", request.name());
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldUpdateOnlyEnabled() {
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null
    );

    assertFalse(request.enabled());
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldUpdateOnlyOwnerId() {
    var request = new UpdateOrganizationRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        99L
    );

    assertEquals(99L, request.ownerId());
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldUpdateMultipleFields() {
    var request = new UpdateOrganizationRequest(
        "New Name",
        "New Description",
        "New Industry",
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    assertEquals("New Name", request.name());
    assertEquals("New Description", request.description());
    assertEquals("New Industry", request.industry());
    assertTrue(request.enabled());
  }
}
