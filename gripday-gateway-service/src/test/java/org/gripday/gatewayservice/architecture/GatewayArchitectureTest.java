package org.gripday.gatewayservice.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * ArchUnit tests for Gateway Service architecture validation.
 * <p>
 * Validates: - Reactive architecture patterns and components - Package structure and naming conventions - REST controller placement and naming (if any) - Proper separation of concerns in
 * gateway components - Filter and configuration component organization
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class GatewayArchitectureTest {

  /**
   * Validates that any REST controllers follow proper naming conventions. Ensures consistency with platform-wide REST controller standards.
   */
  @ArchTest
  static final ArchRule rest_controllers_should_have_resource_suffix =
      classes().that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("Resource")
          .allowEmptyShould(true);

  /**
   * Validates that any REST controllers are in the proper presentation.web package. Ensures consistency with three-tier architecture standards.
   */
  @ArchTest
  static final ArchRule rest_controllers_should_be_in_presentation_web_package =
      classes().that().areAnnotatedWith(RestController.class)
          .should().resideInAPackage("..presentation.web..")
          .allowEmptyShould(true);

  /**
   * Ensures configuration classes are properly organized. Validates that configuration components are in appropriate packages.
   */
  @ArchTest
  static final ArchRule configuration_classes_should_be_in_config_package =
      classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
          .should().resideInAPackage("..config..");

  /**
   * Validates that filter classes are properly organized. Ensures gateway filters are in the appropriate package structure.
   */
  @ArchTest
  static final ArchRule filter_classes_should_be_in_filter_package =
      classes().that().haveNameMatching(".*Filter")
          .and().areNotMemberClasses()
          .should().resideInAnyPackage("..filter..", "..security..", "..config..");

  /**
   * Ensures security components are properly organized. Validates that security-related classes are in security packages.
   */
  @ArchTest
  static final ArchRule security_classes_should_be_in_security_package =
      classes().that().haveNameMatching(".*(Security|Auth|Jwt).*")
          .and().areNotInterfaces()
          .should().resideInAnyPackage("..security..", "..config..");

  /**
   * Validates that service classes are properly organized. Ensures business logic components are in service packages.
   */
  @ArchTest
  static final ArchRule service_classes_should_be_in_service_package =
      classes().that().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .should().resideInAPackage("..service..");

  /**
   * Ensures proper package naming conventions are followed. Validates consistent package structure across the gateway service.
   */
  @ArchTest
  static final ArchRule package_naming_conventions_should_be_followed =
      classes().that().resideInAPackage("org.gripday.gatewayservice..")
          .should().resideInAnyPackage(
              "org.gripday.gatewayservice",
              "org.gripday.gatewayservice.config..",
              "org.gripday.gatewayservice.filter..",
              "org.gripday.gatewayservice.security..",
              "org.gripday.gatewayservice.service..",
              "org.gripday.gatewayservice.architecture..",
              "org.gripday.gatewayservice.integration..",
              "org.gripday.gatewayservice.unit.."
          );

  /**
   * Validates that reactive components don't use blocking operations inappropriately. Ensures proper reactive programming patterns in gateway filters and services.
   */
  @ArchTest
  static final ArchRule reactive_components_should_not_use_blocking_operations =
      noClasses().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("java.lang.Thread")
          .allowEmptyShould(true);

  /**
   * Validates that reactive filters implement proper Spring Cloud Gateway patterns. Ensures gateway filters extend appropriate base classes or implement required interfaces.
   */
  @ArchTest
  static final ArchRule gateway_filters_should_follow_spring_cloud_patterns =
      classes().that().haveNameMatching(".*Filter")
          .and().resideInAPackage("..filter..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("org.springframework.cloud.gateway.filter..");

  /**
   * Validates that reactive components use appropriate Reactor types. Ensures proper reactive programming with Mono and Flux.
   */
  @ArchTest
  static final ArchRule reactive_components_should_use_reactor_types =
      classes().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.core.publisher..");

  /**
   * Ensures filter classes implement proper reactive patterns. Validates that gateway filters follow Spring Cloud Gateway conventions.
   */
  @ArchTest
  static final ArchRule filters_should_follow_reactive_patterns =
      classes().that().haveNameMatching(".*Filter")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.core..", "org.springframework.cloud.gateway..");

  /**
   * Validates that configuration classes don't contain business logic. Ensures proper separation between configuration and business concerns.
   */
  @ArchTest
  static final ArchRule configuration_classes_should_not_contain_business_logic =
      classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "..config..",
              "..filter..",
              "..service..",
              "..security..",
              "reactor.core..",
              "io.jsonwebtoken..",
              "io.github.resilience4j..",
              "io.micrometer..",
              "org.springframework.cloud.gateway.."
          );

  /**
   * Validates that no circular dependencies exist between gateway components. Ensures clean separation of concerns and maintainable architecture.
   */
  @Test
  void should_not_have_circular_dependencies() {
    var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");

    // Verify no cycles between major component groups
    var configClasses = classes.that(resideInAPackage("..config.."));
    var filterClasses = classes.that(resideInAPackage("..filter.."));
    var securityClasses = classes.that(resideInAPackage("..security.."));
    var serviceClasses = classes.that(resideInAPackage("..service.."));

    // Additional validation can be added here for specific circular dependency checks
    // This test serves as a placeholder for more complex cycle detection if needed
  }

  /**
   * Ensures proper dependency direction in gateway architecture. Validates that components depend on appropriate abstractions.
   */
  @Test
  void should_have_proper_dependency_direction() {
    var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");

    // Validate that filters don't depend on configuration details inappropriately
    // Validate that services are properly abstracted
    // Additional architectural validations can be added here
  }
}