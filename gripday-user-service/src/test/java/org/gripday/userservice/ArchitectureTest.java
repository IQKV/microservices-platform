package org.gripday.userservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit tests to enforce architectural rules.
 * These tests verify naming conventions and basic dependency rules.
 */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void setup() {
    classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.gripday.userservice");
  }

  @Test
  void servicesShouldNotDependOnRestResources() {
    ArchRule rule = noClasses()
        .that().haveSimpleNameEndingWith("Service")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("RestResource");

    rule.check(classes);
  }

  @Test
  void servicesShouldHaveServiceAnnotation() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Service")
        .and().areNotInterfaces()
        .and().doNotHaveSimpleName("UserServiceApplication")
        .and().areNotMemberClasses() // Exclude inner classes like TenantAwareRedisService
        .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

    rule.check(classes);
  }

  @Test
  void restResourcesShouldHaveRestControllerAnnotation() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("RestResource")
        .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

    rule.check(classes);
  }

  @Test
  void repositoriesShouldBeInterfaces() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .should().beInterfaces();

    rule.check(classes);
  }

  @Test
  void entitiesShouldResideInCorrectPackages() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(jakarta.persistence.Entity.class)
        .should().resideInAnyPackage(
            "..authentication..",
            "..emailverification..",
            "..organization..",
            "..security..",
            "..shared..",
            "..tenancy..",
            "..usermanagement.."
        );

    rule.check(classes);
  }
}
