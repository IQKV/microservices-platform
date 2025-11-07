package org.gripday.gatewayservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.cloud.gateway.filter.GatewayFilter;
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
   * Validates that reactive methods return appropriate Reactor types. Ensures proper reactive programming patterns.
   */
  @ArchTest
  static final ArchRule reactive_methods_should_return_reactor_types =
      methods().that().areDeclaredInClassesThat().resideInAnyPackage("..filter..", "..service..")
          .and().arePublic()
          .should().haveRawReturnType(Mono.class)
          .orShould().haveRawReturnType(Flux.class)
          .orShould().haveRawReturnType(GatewayFilter.class)
          .orShould().haveRawReturnType(void.class);

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
   * Validates that reactive services are properly annotated. Ensures service components follow Spring reactive conventions.
   */
  @ArchTest
  static final ArchRule reactive_services_should_be_properly_annotated =
      classes().that().resideInAPackage("..service..")
          .and().areNotInterfaces()
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
   * Validates that security components follow reactive patterns. Ensures security filters and services are properly reactive.
   */
  @ArchTest
  static final ArchRule security_components_should_be_reactive =
      classes().that().haveNameMatching(".*(Security|Auth|Jwt).*")
          .and().resideInAnyPackage("..filter..", "..security..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("reactor.core..", "org.springframework.security.web.server..");

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
   * Validates that reactive filters don't create inappropriate dependencies. Ensures filters maintain proper abstraction levels.
   */
  @ArchTest
  static final ArchRule reactive_filters_should_maintain_abstraction =
      classes().that().haveNameMatching(".*Filter")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "reactor.core..",
              "org.springframework.cloud.gateway..",
              "..service..",
              "..security..",
              "io.jsonwebtoken.."
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
   * Validates that reactive services follow proper naming conventions. Ensures consistent service naming patterns.
   */
  @ArchTest
  static final ArchRule reactive_services_should_follow_naming_conventions =
      classes().that().resideInAPackage("..service..")
          .and().areNotInterfaces()
          .should().haveSimpleNameEndingWith("Service");
}