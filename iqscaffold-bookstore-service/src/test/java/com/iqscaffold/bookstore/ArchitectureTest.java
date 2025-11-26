package com.iqscaffold.bookstore;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.iqscaffold.bookstore");
  }

  @Test
  void servicesShouldBeAnnotatedWithServiceAnnotation() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Service")
        .and().resideInAPackage("..catalog..")
        .and().areNotInterfaces()
        .should().beAnnotatedWith(Service.class);

    rule.check(importedClasses);
  }

  @Test
  void repositoriesShouldBeAnnotatedWithRepositoryAnnotation() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .should().beAnnotatedWith(Repository.class);

    rule.check(importedClasses);
  }

  @Test
  void resourcesShouldBeAnnotatedWithRestController() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Resource")
        .should().beAnnotatedWith(RestController.class);

    rule.check(importedClasses);
  }

  @Test
  void servicesShouldNotDependOnResources() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..catalog..")
        .and().haveSimpleNameEndingWith("Service")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("Resource");

    rule.check(importedClasses);
  }

  @Test
  void repositoriesShouldNotDependOnServices() {
    ArchRule rule = noClasses()
        .that().haveSimpleNameEndingWith("Repository")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("Service");

    rule.check(importedClasses);
  }
}
