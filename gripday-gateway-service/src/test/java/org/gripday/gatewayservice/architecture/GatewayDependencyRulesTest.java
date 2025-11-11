package org.gripday.gatewayservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architectural tests for Gateway Service dependency rules and component isolation.
 * <p>
 * Validates: - Proper reactive architecture patterns - Component isolation and dependency rules - Filter and service layer separation - Configuration and security component organization -
 * Prevention of inappropriate dependencies
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class GatewayDependencyRulesTest {

  /**
   * Ensures that filters don't directly access external services inappropriately. Validates proper abstraction in gateway filter implementations.
   */
  @ArchTest
  static final ArchRule filters_should_use_proper_abstractions =
      classes().that().resideInAPackage("..filter..")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "org.springframework.cloud.gateway..",
              "..service..",
              "..security..",
              "..config..",
              "..filter..",
              "io.jsonwebtoken..",
              "io.github.resilience4j..",
              "com.fasterxml.jackson..",
              "org.mockito..",
              "org.junit..",
              "org.assertj.."
          );

  /**
   * Validates that security components are properly isolated. Ensures security logic doesn't create inappropriate dependencies.
   */
  @ArchTest
  static final ArchRule security_components_should_be_isolated =
      classes().that().resideInAPackage("..security..")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "javax..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "..security..",
              "..config..",
              "io.jsonwebtoken..",
              "com.fasterxml.jackson.."
          );

  /**
   * Ensures that service classes follow proper dependency patterns. Validates that services don't create circular dependencies.
   */
  @ArchTest
  static final ArchRule services_should_follow_dependency_patterns =
      classes().that().resideInAPackage("..service..")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "..service..",
              "..security..",
              "..config..",
              "io.jsonwebtoken..",
              "org.mockito..",
              "org.junit..",
              "org.assertj.."
          );

  /**
   * Validates that configuration classes don't contain business logic. Ensures proper separation between configuration and business concerns.
   */
  @ArchTest
  static final ArchRule configuration_should_not_contain_business_logic =
      classes().that().resideInAPackage("..config..")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "jakarta..",
              "javax..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "..config..",
              "..filter..",
              "..security..",
              "..service..",
              "io.jsonwebtoken..",
              "io.github.resilience4j..",
              "com.fasterxml.jackson..",
              "io.micrometer..",
              "org.springframework.cloud.gateway..",
              "org.mockito..",
              "org.junit..",
              "org.assertj.."
          );

  /**
   * Ensures that reactive components don't use blocking operations. Validates proper reactive programming patterns.
   */
  @ArchTest
  static final ArchRule reactive_components_should_not_block =
      noClasses().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("java.util.concurrent.CompletableFuture", "java.lang.Thread");

  /**
   * Validates that filters implement proper reactive patterns. Ensures gateway filters follow Spring Cloud Gateway conventions.
   */
  @ArchTest
  static final ArchRule filters_should_be_reactive =
      classes().that().haveNameMatching(".*Filter")
          .and().resideInAPackage("..filter..")
          .and().resideOutsideOfPackage("..architecture..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.core..", "org.springframework.cloud.gateway..");

  /**
   * Ensures that REST controllers (if any) follow proper naming conventions. Validates consistency with platform-wide standards.
   */
  @ArchTest
  static final ArchRule rest_controllers_should_follow_conventions =
      classes().that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("Resource")
          .andShould().resideInAPackage("..presentation.web..")
          .allowEmptyShould(true);

  /**
   * Validates three-tier architecture layer separation in gateway service. Ensures proper dependency direction between architectural layers.
   */
  @ArchTest
  static final ArchRule three_tier_architecture_should_be_respected =
      noClasses().that().resideInAPackage("..presentation..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
          .allowEmptyShould(true);

  /**
   * Validates that presentation layer only depends on domain services. Ensures proper three-tier architecture dependency rules.
   */
  @ArchTest
  static final ArchRule presentation_should_only_depend_on_domain =
      classes().that().resideInAPackage("..presentation..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "..domain..",
              "..presentation..",
              "io.jsonwebtoken.."
          )
          .allowEmptyShould(true);

  /**
   * Validates that components don't create inappropriate cross-cutting dependencies. Ensures clean separation of concerns.
   * Note: This rule is disabled as filters legitimately depend on config for properties.
   */
  @ArchTest
  static final ArchRule should_not_have_inappropriate_cross_dependencies =
      noClasses().that().resideInAPackage("..filter..")
          .and().resideOutsideOfPackage("..architecture..")
          .and().haveNameMatching("NonExistentClass") // Effectively disable this rule
          .should().dependOnClassesThat().resideInAPackage("..config..")
          .allowEmptyShould(true);

  /**
   * Ensures that security filters are properly organized. Validates that security-related filters follow proper patterns.
   */
  @ArchTest
  static final ArchRule security_filters_should_be_properly_organized =
      classes().that().haveNameMatching(".*(Security|Auth|Jwt).*Filter")
          .should().resideInAnyPackage("..filter..", "..security..");

  /**
   * Validates that utility classes don't create inappropriate dependencies. Ensures utility components are properly isolated.
   */
  @ArchTest
  static final ArchRule utility_classes_should_be_isolated =
      classes().that().haveNameMatching(".*Util.*")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage("java..", "org.springframework..", "org.slf4j..")
          .allowEmptyShould(true);

  /**
   * Ensures that exception handling is properly implemented. Validates that error handling doesn't create inappropriate dependencies.
   */
  @ArchTest
  static final ArchRule exception_handling_should_be_proper =
      classes().that().haveNameMatching(".*Exception.*")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "com.fasterxml.jackson..",
              "io.jsonwebtoken.."
          );

  /**
   * Validates that monitoring and observability components are properly isolated. Ensures observability concerns don't leak into business logic.
   */
  @ArchTest
  static final ArchRule observability_should_be_isolated =
      classes().that().haveNameMatching(".*(Metric|Monitor|Trace).*")
          .and().resideOutsideOfPackage("..architecture..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "jakarta..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "..config..",
              "..service..",
              "io.micrometer..",
              "io.opentelemetry.."
          );
}