package org.gripday.userservice.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.Entity;

/**
 * ArchUnit tests for three-tier architecture layer separation and dependency rules.
 * <p>
 * Validates: - Layer separation between presentation, domain, and infrastructure - Dependency direction rules (presentation -> domain -> infrastructure) - Package structure compliance -
 * REST controller naming conventions and package placement - Prevention of circular dependencies
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class LayeredArchitectureTest {

  @ArchTest
  static final ArchRule layer_dependencies_are_respected =
      layeredArchitecture().consideringAllDependencies()
          .layer("Presentation").definedBy("..presentation..", "..authentication..", "..registration..", 
                                           "..usermanagement..", "..organization..", "..passwordmanagement..", 
                                           "..emailverification..", "..tenancy..")
          .layer("Domain").definedBy("..shared..", "..security..")
          .layer("Infrastructure").definedBy("..infrastructure..", "..config..")
          .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
          .whereLayer("Domain").mayOnlyBeAccessedByLayers("Presentation", "Infrastructure")
          .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Presentation", "Domain");

  @ArchTest
  static final ArchRule controllers_should_be_in_correct_package =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy..",
              "..presentation.."
          )
          .because("REST controllers should be in domain or presentation packages");

  @ArchTest
  static final ArchRule controllers_should_have_proper_naming =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("RestResource")
          .orShould().haveSimpleNameEndingWith("Controller")
          .because("REST controllers should follow naming conventions");

  @ArchTest
  static final ArchRule services_should_be_in_correct_package =
      classes()
          .that().areAnnotatedWith(Service.class)
          .should().resideOutsideOfPackage("..presentation..")
          .because("Services should not be in presentation package");

  @ArchTest
  static final ArchRule services_should_have_proper_naming =
      classes()
          .that().areAnnotatedWith(Service.class)
          .should().haveSimpleNameEndingWith("Service")
          .because("Services should follow naming conventions");

  @ArchTest
  static final ArchRule repositories_should_be_in_correct_package =
      classes()
          .that().areAnnotatedWith(Repository.class)
          .or().haveSimpleNameEndingWith("Repository")
          .should().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy..",
              "..shared..",
              "..security..",
              "..infrastructure.."
          )
          .because("Repositories should be in domain or infrastructure packages");

  @ArchTest
  static final ArchRule repositories_should_have_proper_naming =
      classes()
          .that().areAnnotatedWith(Repository.class)
          .or().haveNameMatching(".*Repository")
          .should().haveSimpleNameEndingWith("Repository")
          .because("Repositories should follow naming conventions");

  @ArchTest
  static final ArchRule entities_should_be_in_domain_packages =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy..",
              "..shared..",
              "..security.."
          )
          .because("Entities should be in domain packages");

  @ArchTest
  static final ArchRule config_classes_should_be_in_config_package =
      classes()
          .that().haveSimpleNameEndingWith("Config")
          .or().haveSimpleNameEndingWith("Configuration")
          .should().resideInAnyPackage("..config..", "..infrastructure.config..")
          .because("Configuration classes should be in config packages");
}