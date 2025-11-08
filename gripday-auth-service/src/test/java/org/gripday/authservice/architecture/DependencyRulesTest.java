package org.gripday.authservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architectural tests focused on dependency rules and layer isolation.
 * <p>
 * Validates: - REST controllers cannot directly access repositories - Proper dependency direction between layers - Prevention of circular dependencies - Enforcement of three-tier
 * architecture principles - Proper abstraction and encapsulation
 */
@AnalyzeClasses(packages = "org.gripday.authservice")
class DependencyRulesTest {

  /**
   * Critical rule: REST controllers must NOT directly access repositories. This enforces the three-tier architecture by requiring controllers to go through domain services for data access.
   */
  @ArchTest
  static final ArchRule controllers_should_not_directly_access_repositories =
      noClasses().that().areAnnotatedWith(RestController.class)
          .should().dependOnClassesThat().areAnnotatedWith(Repository.class);

  /**
   * Validates that controllers don't directly access repository packages. Provides additional protection against bypassing the service layer.
   */
  @ArchTest
  static final ArchRule controllers_should_not_access_repository_packages =
      noClasses().that().resideInAPackage("..presentation.web..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure.repository..");

  /**
   * Ensures that controllers don't directly access entity classes. Prevents tight coupling between presentation and data layers.
   */
  @ArchTest
  static final ArchRule controllers_should_not_directly_access_entities =
      noClasses().that().areAnnotatedWith(RestController.class)
          .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class);

  /**
   * Validates that presentation web layer doesn't access infrastructure directly. Allows DTOs to be shared but prevents direct repository access.
   */
  @ArchTest
  static final ArchRule presentation_web_should_not_access_infrastructure_directly =
      noClasses().that().resideInAPackage("..presentation.web..")
          .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.repository..", "..infrastructure.entity..")
          .because("Presentation web layer should only access domain layer");

  /**
   * Ensures that domain services act as the proper intermediary.
   * Validates that repositories are only accessed by domain services.
   * Allows config and test packages for setup and testing purposes.
   */
  @ArchTest
  static final ArchRule repositories_should_only_be_accessed_by_domain_services =
      classes().that().areAnnotatedWith(Repository.class)
          .should().onlyBeAccessed().byAnyPackage(
              "..domain.service..",
              "..infrastructure..",
              "..config..",
              "..unit..",
              "..integration.."
          );

  /**
   * Validates that infrastructure layer doesn't depend on presentation layer. Allows limited domain dependencies for tenant context.
   */
  @ArchTest
  static final ArchRule infrastructure_should_not_depend_on_presentation =
      noClasses().that().resideInAPackage("..infrastructure..")
          .should().dependOnClassesThat().resideInAnyPackage("..presentation.web..", "..presentation.exception..");

  /**
   * Ensures that entities are not exposed outside infrastructure layer. Validates proper encapsulation of data access concerns.
   */
  @ArchTest
  static final ArchRule entities_should_not_be_used_outside_infrastructure =
      classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
          .should().onlyBeAccessed().byAnyPackage("..infrastructure..", "..domain.service..");

  /**
   * Validates that DTOs are used for data transfer between layers.
   * Ensures proper abstraction between presentation and domain layers.
   * Allows infrastructure and test packages for data mapping and testing.
   */
  @ArchTest
  static final ArchRule dtos_should_be_used_for_layer_communication =
      classes().that().resideInAPackage("..presentation.dto..")
          .should().onlyBeAccessed().byAnyPackage(
              "..presentation..",
              "..domain.service..",
              "..infrastructure..",
              "..unit..",
              "..integration..",
              "..config.."
          );

  /**
   * Ensures that validation logic is properly encapsulated. Validates that validation classes are only used by presentation layer.
   */
  @ArchTest
  static final ArchRule validation_should_be_presentation_concern =
      classes().that().resideInAPackage("..presentation.validation..")
          .should().onlyBeAccessed().byAnyPackage("..presentation..");

  /**
   * Validates that exception handling is properly layered. Ensures exceptions are handled at appropriate layers.
   */
  @ArchTest
  static final ArchRule exception_handling_should_be_layered =
      classes().that().resideInAPackage("..presentation.exception..")
          .should().onlyBeAccessed().byAnyPackage("..presentation..");

  /**
   * Ensures that configuration classes don't create inappropriate dependencies. Validates that configuration is properly isolated.
   */
  @ArchTest
  static final ArchRule configuration_should_not_create_layer_violations =
      classes().that().resideInAPackage("..config..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "..config..",
              "..infrastructure..",
              "..domain..",
              "io.jsonwebtoken..",
              "liquibase..",
              "org.hibernate.."
          );

  /**
   * Validates that service interfaces are properly abstracted. Ensures that services depend on abstractions, not implementations.
   */
  @ArchTest
  static final ArchRule services_should_depend_on_abstractions =
      classes().that().resideInAPackage("..domain.service..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "org.springframework..",
              "org.slf4j..",
              "..domain..",
              "..infrastructure.repository..",
              "..infrastructure.entity..",
              "..presentation.dto..",
              "jakarta.persistence..",
              "jakarta.validation.."
          );
}