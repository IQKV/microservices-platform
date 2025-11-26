package com.iqscaffold.gatewayservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void setup() {
    classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.iqscaffold.gatewayservice");
  }

  @Test
  void servicesShouldBeAnnotatedWithService() {
    ArchRule rule = classes()
        .that().resideInAPackage("..service..")
        .and().haveSimpleNameEndingWith("Service")
        .should().beAnnotatedWith(Service.class);

    rule.check(classes);
  }

  @Test
  void controllersShouldResideInPresentationPackage() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(RestController.class)
        .should().resideInAPackage("..presentation..");

    rule.check(classes);
  }

  @Test
  void layeredArchitectureShouldBeRespected() {
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Presentation").definedBy("..presentation..")
        .layer("Service").definedBy("..service..")
        .layer("Filter").definedBy("..filter..")
        .layer("Config").definedBy("..config..")
        .layer("Security").definedBy("..security..")
        .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Presentation", "Filter", "Config")
        .check(classes);
  }
}
