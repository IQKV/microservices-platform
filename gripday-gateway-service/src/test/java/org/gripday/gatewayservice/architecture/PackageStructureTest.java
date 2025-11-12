package org.gripday.gatewayservice.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architectural tests for package structure and naming conventions in Gateway Service.
 * <p>
 * Validates: - Proper package organization and naming - Component placement in appropriate packages - Three-tier architecture package structure - Gateway-specific package conventions - REST
 * controller placement and naming (Resource suffix)
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class PackageStructureTest {

  /**
   * Validates that all classes are in the proper base package. Ensures consistent package naming across the gateway service.
   */
  @ArchTest
  static final ArchRule all_classes_should_be_in_proper_base_package =
      classes().should().resideInAPackage("org.gripday.gatewayservice..");

  /**
   * Validates that configuration classes are in config package. Ensures proper organization of configuration components.
   */
  @ArchTest
  static final ArchRule configuration_classes_should_be_in_config_package =
      classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
          .should().resideInAPackage("..config..");

  /**
   * Validates that filter classes are in filter package. Ensures proper organization of gateway filter components.
   * Note: Inner classes in config are allowed for global filters.
   */
  @ArchTest
  static final ArchRule filter_classes_should_be_in_filter_package =
      classes().that().haveNameMatching(".*Filter")
          .and().areNotMemberClasses()
          .should().resideInAPackage("..filter..");

  /**
   * Validates that service classes are in service package. Ensures proper organization of business logic components.
   */
  @ArchTest
  static final ArchRule service_classes_should_be_in_service_package =
      classes().that().areAnnotatedWith(Service.class)
          .should().resideInAPackage("..service..");

  /**
   * Validates that security classes are in security package. Ensures proper organization of security-related components.
   */
  @ArchTest
  static final ArchRule security_classes_should_be_in_security_package =
      classes().that().haveNameMatching(".*(Security|Auth|Jwt).*")
          .and().areNotInterfaces()
          .should().resideInAnyPackage("..security..", "..config..");

  /**
   * Validates that REST controllers (if any) are in presentation.web package. Ensures consistency with three-tier architecture standards.
   */
  @ArchTest
  static final ArchRule rest_controllers_should_be_in_presentation_web_package =
      classes().that().areAnnotatedWith(RestController.class)
          .should().resideInAPackage("..presentation.web..")
          .allowEmptyShould(true);

  /**
   * Validates that REST controllers have Resource suffix. Ensures consistency with platform-wide naming conventions.
   */
  @ArchTest
  static final ArchRule rest_controllers_should_have_resource_suffix =
      classes().that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("Resource")
          .allowEmptyShould(true);

  /**
   * Validates that component classes follow proper naming conventions. Ensures consistent naming patterns across gateway components.
   */
  @ArchTest
  static final ArchRule components_should_follow_naming_conventions =
      classes().that().areAnnotatedWith(Component.class)
          .and().resideInAPackage("..filter..")
          .should().haveSimpleNameEndingWith("Filter");

  /**
   * Validates that service classes follow proper naming conventions. Ensures consistent service naming patterns.
   * Note: Allows for utility service classes that may not end with "Service".
   */
  @ArchTest
  static final ArchRule services_should_follow_naming_conventions =
      classes().that().areAnnotatedWith(Service.class)
          .and().doNotHaveSimpleName("ApiVersionExtractor")
          .should().haveSimpleNameEndingWith("Service");

  /**
   * Validates that utility classes are properly organized. Ensures utility components don't pollute main packages.
   */
  @ArchTest
  static final ArchRule utility_classes_should_be_properly_organized =
      classes().that().haveNameMatching(".*Util.*")
          .should().resideInAnyPackage("..util..", "..config..", "..security..")
          .allowEmptyShould(true);

  /**
   * Validates that exception classes are properly organized. Ensures error handling components are in appropriate packages.
   */
  @ArchTest
  static final ArchRule exception_classes_should_be_properly_organized =
      classes().that().haveNameMatching(".*Exception.*")
          .should().resideInAnyPackage("..exception..", "..config..", "..security..");

  /**
   * Validates that the main application class is in the root package. Ensures proper Spring Boot application structure.
   */
  @Test
  void main_application_class_should_be_in_root_package() {
    var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");

    var applicationClasses = classes.stream()
        .filter(clazz -> clazz.isAnnotatedWith(org.springframework.boot.autoconfigure.SpringBootApplication.class))
        .toList();

    assertThat(applicationClasses).hasSize(1);

    var applicationClass = applicationClasses.get(0);
    assertThat(applicationClass.getPackageName()).isEqualTo("org.gripday.gatewayservice");
    assertThat(applicationClass.getSimpleName()).endsWith("Application");
  }

  /**
   * Validates that package structure follows gateway service conventions. Ensures proper organization of gateway-specific components.
   */
  @Test
  void should_have_proper_gateway_package_structure() {
    var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");

    // Verify expected packages exist and contain appropriate classes
    var configClasses = classes.that(resideInAPackage("..config.."));
    var filterClasses = classes.that(resideInAPackage("..filter.."));

    // Validate that each package contains appropriate components
    assertThat(configClasses).isNotEmpty();
    assertThat(filterClasses).isNotEmpty();

    // Service and security packages may be empty in a pure gateway service
    // but if they exist, they should follow proper conventions
  }

  /**
   * Validates that no classes are in inappropriate packages. Ensures clean package organization without misplaced components.
   */
  @Test
  void should_not_have_classes_in_inappropriate_packages() {
    var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");

    // Verify no classes are in unexpected packages
    var unexpectedPackages = classes.that(resideOutsideOfPackages(
        "org.gripday.gatewayservice",
        "org.gripday.gatewayservice.config..",
        "org.gripday.gatewayservice.filter..",
        "org.gripday.gatewayservice.security..",
        "org.gripday.gatewayservice.service..",
        "org.gripday.gatewayservice.presentation..",
        "org.gripday.gatewayservice.domain..",
        "org.gripday.gatewayservice.infrastructure..",
        "org.gripday.gatewayservice.util..",
        "org.gripday.gatewayservice.exception.."
    ));

    assertThat(unexpectedPackages).isEmpty();
  }


}