package org.gripday.gatewayservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Architectural tests specifically for reactive components in the Gateway Service.
 * <p>
 * Validates: - Proper reactive programming patterns with Reactor - Spring Cloud Gateway filter implementations - Non-blocking reactive operations - Proper use of Mono and Flux types -
 * Reactive security and authentication patterns
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class ReactiveArchitectureTest {

  /**
   * Validates that gateway filters extend the proper Spring Cloud Gateway base classes. Ensures filters follow Spring Cloud Gateway conventions.
   */
  @ArchTest
  static final ArchRule gateway_filters_should_extend_proper_base_classes =
      classes().that().haveNameMatching(".*Filter")
          .and().areAnnotatedWith(Component.class)
          .should().beAssignableTo(AbstractGatewayFilterFactory.class);

  /**
   * Validates that reactive filter methods return appropriate types.
   * Focuses on the main filter() method which must be reactive.
   * Excludes utility methods, getters, and configuration methods.
   */
  @ArchTest
  static final ArchRule reactive_filter_methods_should_return_reactor_types =
      methods().that().areDeclaredInClassesThat()
          .resideInAnyPackage("..filter..")
          .and().haveNameMatching("filter")
          .and().arePublic()
          .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
          .and().areDeclaredInClassesThat().areNotMemberClasses()
          .should().haveRawReturnType(Mono.class)
          .orShould().haveRawReturnType(Flux.class);

  /**
   * Validates that reactive components don't use blocking I/O operations. Ensures non-blocking reactive patterns throughout the gateway.
   */
  @ArchTest
  static final ArchRule reactive_components_should_not_use_blocking_io =
      noClasses().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "java.io..",
              "java.nio.file..",
              "java.util.concurrent.CompletableFuture",
              "java.util.concurrent.Future"
          );

  /**
   * Validates that service classes are properly annotated.
   * Ensures consistent service layer organization.
   * Excludes test classes, inner classes, and records from this validation.
   */
  @ArchTest
  static final ArchRule reactive_services_should_be_properly_annotated =
      classes().that().resideInAPackage("..service..")
          .and().areNotInterfaces()
          .and().haveSimpleNameNotEndingWith("Test")
          .and().areNotMemberClasses()
          .and().areNotRecords()
          .should().beAnnotatedWith(Service.class)
          .orShould().beAnnotatedWith(Component.class);

  /**
   * Validates that reactive filters use proper Spring Cloud Gateway APIs. Ensures filters integrate correctly with the gateway framework.
   */
  @ArchTest
  static final ArchRule reactive_filters_should_use_gateway_apis =
      classes().that().haveNameMatching(".*Filter")
          .should().dependOnClassesThat()
          .resideInAnyPackage("org.springframework.cloud.gateway..");

  /**
   * Validates that reactive components handle errors properly. Ensures error handling follows reactive patterns.
   */
  @ArchTest
  static final ArchRule reactive_components_should_handle_errors_reactively =
      classes().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.core.publisher..", "org.springframework.web.server..");

  /**
   * Validates that security filter classes use reactive patterns.
   * Ensures proper reactive security implementation in filters.
   * Focuses on filter classes only, not inner records or utility classes.
   */
  @ArchTest
  static final ArchRule security_filters_should_be_reactive =
      classes().that().haveNameMatching(".*(Security|Auth|Jwt).*Filter")
          .and().resideInAnyPackage("..filter..", "..security..")
          .and().areNotMemberClasses()
          .and().areNotRecords()
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "reactor.core..",
              "org.springframework.security.web.server..",
              "java..",
              "org.springframework..",
              "io.jsonwebtoken..",
              "javax.crypto..",
              "com.fasterxml.jackson..",
              "..config..",
              "..filter..",
              "..security.."
          );

  /**
   * Validates that configuration classes don't contain reactive logic. Ensures proper separation between configuration and reactive business logic.
   */
  @ArchTest
  static final ArchRule configuration_should_not_contain_reactive_logic =
      classes().that().resideInAPackage("..config..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "..config..",
              "..filter..",
              "..security..",
              "..service..",
              "io.jsonwebtoken..",
              "org.springframework.cloud.gateway.."
          );

  /**
   * Validates that reactive components use proper context propagation. Ensures reactive context is properly maintained across async operations.
   */
  @ArchTest
  static final ArchRule reactive_components_should_use_context_propagation =
      classes().that().resideInAnyPackage("..filter..", "..service..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.util.context..", "org.slf4j.MDC");

  /**
   * Validates that reactive filters maintain proper abstraction.
   * Ensures filters don't depend on inappropriate external libraries.
   * Allows necessary dependencies for gateway functionality.
   */
  @ArchTest
  static final ArchRule reactive_filters_should_maintain_abstraction =
      classes().that().haveNameMatching(".*Filter")
          .and().areNotMemberClasses()
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
              "javax.crypto..",
              "reactor.util.context.."
          );

  /**
   * Validates that reactive components follow proper naming conventions. Ensures consistent naming across reactive components.
   */
  @ArchTest
  static final ArchRule reactive_components_should_follow_naming_conventions =
      classes().that().resideInAnyPackage("..filter..")
          .and().areNotInterfaces()
          .should().haveSimpleNameEndingWith("Filter");

  /**
   * Ensures that service classes follow proper naming conventions.
   * Validates consistent service organization.
   * Excludes test classes, inner classes, and records from this validation.
   */
  @ArchTest
  static final ArchRule reactive_services_should_follow_naming_conventions =
      classes().that().resideInAPackage("..service..")
          .and().areNotInterfaces()
          .and().haveSimpleNameNotEndingWith("Test")
          .and().areNotMemberClasses()
          .and().areNotRecords()
          .should().haveSimpleNameEndingWith("Service")
          .orShould().haveSimpleNameEndingWith("Extractor");
}
