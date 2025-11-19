package org.gripday.userservice.organization;

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

class CreateOrganizationRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidOrganizationRequest() {
    var request = new CreateOrganizationRequest(
        "Acme Corporation",
        "A leading technology company",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    assertEquals("Acme Corporation", request.name());
    assertEquals("A leading technology company", request.description());
    assertEquals("Technology", request.industry());
    assertTrue(request.enabled());
    assertEquals(1L, request.ownerId());
  }

  @Test
  void shouldValidateNameNotBlank() {
    var request = new CreateOrganizationRequest(
        "",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Organization name is required")));
  }

  @Test
  void shouldValidateNameMinLength() {
    var request = new CreateOrganizationRequest(
        "A",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 2 and 255 characters")));
  }

  @Test
  void shouldValidateNameMaxLength() {
    var longName = "A".repeat(256);
    var request = new CreateOrganizationRequest(
        longName,
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateDescriptionMaxLength() {
    var longDescription = "A".repeat(1001);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        longDescription,
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("must not exceed 1000 characters")));
  }

  @Test
  void shouldValidateIndustryMaxLength() {
    var longIndustry = "A".repeat(101);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        longIndustry,
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateWebsiteMaxLength() {
    var longWebsite = "https://" + "a".repeat(250) + ".com";
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        longWebsite,
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePhoneMaxLength() {
    var longPhone = "1".repeat(51);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        "https://acme.com",
        longPhone,
        "123 Main Street",
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateAddressMaxLength() {
    var longAddress = "A".repeat(501);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        longAddress,
        "San Francisco",
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateCityMaxLength() {
    var longCity = "A".repeat(101);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        longCity,
        "USA",
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateCountryMaxLength() {
    var longCountry = "A".repeat(101);
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        longCountry,
        true,
        1L
    );

    Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAllowNullOptionalFields() {
    var request = new CreateOrganizationRequest(
        "Acme Corp",
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
  }

  @Test
  void shouldCreateMinimalRequest() {
    var request = new CreateOrganizationRequest(
        "Acme",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    assertEquals("Acme", request.name());
    assertTrue(request.enabled());
    assertNull(request.description());
  }

  @Test
  void shouldHandleDisabledOrganization() {
    var request = new CreateOrganizationRequest(
        "Acme Corp",
        "Description",
        "Technology",
        "https://acme.com",
        "+1-555-0100",
        "123 Main Street",
        "San Francisco",
        "USA",
        false,
        1L
    );

    assertFalse(request.enabled());
  }

  @Test
  void shouldAcceptVariousPhoneFormats() {
    var phoneFormats = new String[] {
        "+1-555-0100",
        "(555) 123-4567",
        "555.123.4567",
        "+44 20 7946 0958"
    };

    for (final var phone : phoneFormats) {
      var request = new CreateOrganizationRequest(
          "Acme Corp",
          null,
          null,
          null,
          phone,
          null,
          null,
          null,
          true,
          null
      );
      assertTrue(validator.validate(request).isEmpty(), "Phone format should be valid: " + phone);
    }
  }

  @Test
  void shouldAcceptVariousWebsiteFormats() {
    var websiteFormats = new String[] {
        "https://acme.com",
        "http://www.acme.com",
        "www.acme.com",
        "acme.com"
    };

    for (final var website : websiteFormats) {
      var request = new CreateOrganizationRequest(
          "Acme Corp",
          null,
          null,
          website,
          null,
          null,
          null,
          null,
          true,
          null
      );
      assertTrue(validator.validate(request).isEmpty(), "Website format should be valid: " + website);
    }
  }
}
