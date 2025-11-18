package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import jakarta.persistence.Entity;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architectural tests focused on dependency rules and layer isolation.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class DependencyRulesTest {

  @ArchTest
  static final ArchRule services_should_not_depend_on_controllers =
      noClasses()
          .that().resideInAPackage("..service..")
          .or().areAnnotatedWith(Service.class)
          .should().dependOnClassesThat().areAnnotatedWith(RestController.class)
          .because("Services should not depend on REST controllers");

  @ArchTest
  static final ArchRule repositories_should_not_depend_on_services =
      noClasses()
          .that().resideInAPackage("..repository..")
          .or().areAnnotatedWith(Repository.class)
          .should().dependOnClassesThat().areAnnotatedWith(Service.class)
          .because("Repositories should not depend on services");

  @ArchTest
  static final ArchRule entities_should_not_depend_on_services =
      noClasses()
          .that().areAnnotatedWith(Entity.class)
          .should().dependOnClassesThat().areAnnotatedWith(Service.class)
          .because("Entities should not depend on services");

  @ArchTest
  static final ArchRule entities_should_not_depend_on_controllers =
      noClasses()
          .that().areAnnotatedWith(Entity.class)
          .should().dependOnClassesThat().areAnnotatedWith(RestController.class)
          .because("Entities should not depend on controllers");

  @ArchTest
  static final ArchRule no_cycles_in_domain_modules =
      slices()
          .matching("org.gripday.userservice.(*)..")
          .should().beFreeOfCycles()
          .because("Domain modules should not have circular dependencies");

  @ArchTest
  static final ArchRule config_should_not_depend_on_presentation =
      noClasses()
          .that().resideInAPackage("..config..")
          .should().dependOnClassesThat().resideInAPackage("..presentation..")
          .because("Configuration should not depend on presentation layer");

  @ArchTest
  static final ArchRule shared_should_not_depend_on_specific_modules =
      noClasses()
          .that().resideInAPackage("..shared..")
          .should().dependOnClassesThat().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy.."
          )
          .because("Shared components should not depend on specific domain modules");

  @ArchTest
  static final ArchRule infrastructure_should_not_depend_on_domain_services =
      noClasses()
          .that().resideInAPackage("..infrastructure..")
          .should().dependOnClassesThat().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification.."
          )
          .andShould().dependOnClassesThat().areAnnotatedWith(Service.class)
          .because("Infrastructure should not depend on domain services");
}
