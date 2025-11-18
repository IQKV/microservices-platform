package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.Entity;

/**
 * Comprehensive platform architecture tests that validate cross-cutting concerns and platform-wide architectural standards.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class PlatformArchitectureTest {

  @ArchTest
  static final ArchRule no_generic_exceptions =
      NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
          .because("Generic exceptions should not be thrown");

  @ArchTest
  static final ArchRule no_java_util_logging =
      NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
          .because("Use SLF4J for logging instead of java.util.logging");

  @ArchTest
  static final ArchRule no_jodatime =
      NO_CLASSES_SHOULD_USE_JODATIME
          .because("Use Java Time API instead of Joda Time");

  @ArchTest
  static final ArchRule no_field_injection =
      NO_CLASSES_SHOULD_USE_FIELD_INJECTION
          .because("Use constructor injection instead of field injection");

  @ArchTest
  static final ArchRule services_should_be_transactional =
      classes()
          .that().areAnnotatedWith(Service.class)
          .and().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy.."
          )
          .should().beAnnotatedWith(Transactional.class)
          .because("Domain services should be transactional");

  @ArchTest
  static final ArchRule repositories_should_extend_spring_data =
      classes()
          .that().haveSimpleNameEndingWith("Repository")
          .and().areInterfaces()
          .should().beAssignableTo(org.springframework.data.repository.Repository.class)
          .because("Repositories should extend Spring Data Repository");

  @ArchTest
  static final ArchRule entities_should_have_no_arg_constructor =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().haveOnlyPrivateConstructors()
          .orShould().haveOnlyPackagePrivateConstructors()
          .because("JPA entities should have protected or private no-arg constructor");

  @ArchTest
  static final ArchRule controllers_should_not_use_implementation_types =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().arePublic()
          .should().notHaveRawReturnType(java.util.ArrayList.class)
          .andShould().notHaveRawReturnType(java.util.HashSet.class)
          .andShould().notHaveRawReturnType(java.util.HashMap.class)
          .because("Controllers should return interface types, not implementations");

  @ArchTest
  static final ArchRule services_should_not_depend_on_jakarta_servlet =
      noClasses()
          .that().areAnnotatedWith(Service.class)
          .should().dependOnClassesThat().resideInAPackage("jakarta.servlet..")
          .because("Services should not depend on servlet API");

  @ArchTest
  static final ArchRule no_classes_should_access_standard_streams =
      noClasses()
          .that().resideOutsideOfPackage("..config..")
          .should().accessClassesThat().belongToAnyOf(System.class)
          .because("Use proper logging instead of System.out/err");

  @ArchTest
  static final ArchRule dtos_should_be_records_or_have_proper_naming =
      classes()
          .that().haveSimpleNameEndingWith("Request")
          .or().haveSimpleNameEndingWith("Response")
          .or().haveSimpleNameEndingWith("Dto")
          .should().beRecords()
          .orShould().beTopLevelClasses()
          .because("DTOs should be records or properly named classes");

  @ArchTest
  static final ArchRule security_annotations_should_be_on_controllers =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
          .orShould().containAnyMethodsThat().areAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
          .because("Controllers should have security annotations");

  @ArchTest
  static final ArchRule validated_annotations_on_request_objects =
      classes()
          .that().haveSimpleNameEndingWith("Request")
          .and().areRecords()
          .should().beAnnotatedWith(jakarta.validation.Valid.class)
          .orShould().containAnyFieldsThat().areAnnotatedWith(jakarta.validation.constraints.NotNull.class)
          .orShould().containAnyFieldsThat().areAnnotatedWith(jakarta.validation.constraints.NotBlank.class)
          .because("Request objects should have validation annotations");

  @ArchTest
  static final ArchRule no_public_fields_in_entities =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().haveOnlyPrivateFields()
          .orShould().haveOnlyPackagePrivateFields()
          .because("Entity fields should be private or package-private");

  @ArchTest
  static final ArchRule configuration_classes_should_not_be_final =
      classes()
          .that().haveSimpleNameEndingWith("Config")
          .or().haveSimpleNameEndingWith("Configuration")
          .should().notBeAnnotatedWith(org.springframework.context.annotation.Configuration.class)
          .orShould().notBeFinal()
          .because("Spring configuration classes should not be final");

  @ArchTest
  static final ArchRule rest_controllers_should_have_request_mapping =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(org.springframework.web.bind.annotation.RequestMapping.class)
          .because("REST controllers should have @RequestMapping at class level");

  @ArchTest
  static final ArchRule services_should_have_single_public_constructor =
      classes()
          .that().areAnnotatedWith(Service.class)
          .should().haveOnlyOneConstructor()
          .because("Services should have a single constructor for dependency injection");
}