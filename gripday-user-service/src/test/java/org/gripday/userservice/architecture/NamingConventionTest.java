package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture tests for naming conventions across the application.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class NamingConventionTest {

  @ArchTest
  static final ArchRule services_should_be_named_correctly =
      classes()
          .that().areAnnotatedWith(Service.class)
          .should().haveSimpleNameEndingWith("Service")
          .because("Service classes should end with 'Service'");

  @ArchTest
  static final ArchRule repositories_should_be_named_correctly =
      classes()
          .that().areAnnotatedWith(Repository.class)
          .or().areAssignableTo(org.springframework.data.repository.Repository.class)
          .should().haveSimpleNameEndingWith("Repository")
          .because("Repository interfaces should end with 'Repository'");

  @ArchTest
  static final ArchRule controllers_should_be_named_correctly =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("RestResource")
          .orShould().haveSimpleNameEndingWith("Controller")
          .because("REST controllers should end with 'RestResource' or 'Controller'");

  @ArchTest
  static final ArchRule configuration_classes_should_be_named_correctly =
      classes()
          .that().areAnnotatedWith(Configuration.class)
          .should().haveSimpleNameEndingWith("Config")
          .orShould().haveSimpleNameEndingWith("Configuration")
          .because("Configuration classes should end with 'Config' or 'Configuration'");

  @ArchTest
  static final ArchRule request_dtos_should_be_named_correctly =
      classes()
          .that().haveSimpleNameEndingWith("Request")
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
          .because("Request DTOs should be in domain or presentation packages");

  @ArchTest
  static final ArchRule response_dtos_should_be_named_correctly =
      classes()
          .that().haveSimpleNameEndingWith("Response")
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
          .because("Response DTOs should be in domain or presentation packages");

  @ArchTest
  static final ArchRule exception_classes_should_be_named_correctly =
      classes()
          .that().areAssignableTo(Exception.class)
          .and().resideInAPackage("org.gripday.userservice..")
          .should().haveSimpleNameEndingWith("Exception")
          .because("Exception classes should end with 'Exception'");

  @ArchTest
  static final ArchRule test_classes_should_be_named_correctly =
      classes()
          .that().resideInAPackage("..test..")
          .and().haveSimpleNameNotEndingWith("Test")
          .and().haveSimpleNameNotEndingWith("Tests")
          .should().beInterfaces()
          .orShould().beEnums()
          .orShould().haveSimpleNameEndingWith("Config")
          .orShould().haveSimpleNameEndingWith("Application")
          .because("Test classes should end with 'Test' or 'Tests'");

  @ArchTest
  static final ArchRule interface_names_should_not_start_with_I =
      classes()
          .that().areInterfaces()
          .should().haveSimpleNameNotStartingWith("I")
          .because("Interface names should not start with 'I' prefix");

  @ArchTest
  static final ArchRule package_names_should_be_lowercase =
      classes()
          .should().resideInAPackage("..userservice..")
          .because("Package names should be lowercase");
}
