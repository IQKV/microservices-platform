package org.gripday.authservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * Comprehensive platform architecture tests that validate cross-cutting concerns and platform-wide architectural standards.
 * <p>
 * Validates: - Consistent naming conventions across all components - Proper annotation usage and placement - Platform-wide architectural patterns - Security and validation standards -
 * Integration patterns and dependencies
 */
@AnalyzeClasses(packages = "org.gripday.authservice")
class PlatformArchitectureTest {

  /**
   * Validates that all service classes are annotated with @Service. Ensures proper Spring component detection and dependency injection.
   */
  @ArchTest
  static final ArchRule service_classes_should_be_annotated =
      classes().that().resideInAPackage("..domain.service..")
          .and().haveSimpleNameEndingWith("Service")
          .should().beAnnotatedWith(Service.class);

  /**
   * Validates that all repository interfaces are annotated with @Repository. Ensures proper Spring Data JPA integration and exception translation.
   */
  @ArchTest
  static final ArchRule repository_interfaces_should_be_annotated =
      classes().that().resideInAPackage("..infrastructure.repository..")
          .and().haveSimpleNameEndingWith("Repository")
          .should().beAnnotatedWith(Repository.class);

  /**
   * Ensures that entity classes are properly placed in infrastructure layer. Validates proper separation of data access concerns.
   */
  @ArchTest
  static final ArchRule entity_classes_should_be_in_infrastructure =
      classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
          .should().resideInAPackage("..infrastructure.entity..");

  /**
   * Validates that DTO classes are properly placed in presentation layer. Ensures proper separation of data transfer concerns.
   */
  @ArchTest
  static final ArchRule dto_classes_should_be_in_presentation =
      classes().that().haveNameMatching(".*(Dto|Request|Response)")
          .and().resideInAPackage("org.gripday.authservice..")
          .should().resideInAPackage("..presentation.dto..");

  /**
   * Ensures that validation classes are properly organized. Validates that validation logic is in appropriate packages.
   */
  @ArchTest
  static final ArchRule validation_classes_should_be_in_validation_package =
      classes().that().haveNameMatching(".*(Validator|Validation)")
          .should().resideInAPackage("..presentation.validation..");

  /**
   * Validates that exception handling is centralized. Ensures consistent error handling across the service.
   */
  @ArchTest
  static final ArchRule exception_handlers_should_be_in_exception_package =
      classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestControllerAdvice.class)
          .should().resideInAPackage("..presentation.exception..");

  /**
   * Ensures that configuration classes follow proper naming conventions. Validates consistent configuration organization.
   */
  @ArchTest
  static final ArchRule configuration_classes_should_have_config_suffix =
      classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
          .should().haveSimpleNameEndingWith("Config");

  /**
   * Validates that no classes use deprecated Java features. Ensures modern Java practices are followed.
   */
  @ArchTest
  static final ArchRule should_not_use_deprecated_features =
      noClasses().should().dependOnClassesThat()
          .resideInAnyPackage("java.util.Vector", "java.util.Hashtable", "java.util.Stack");

  /**
   * Ensures proper logging practices are followed. Validates that SLF4J is used consistently for logging.
   */
  @ArchTest
  static final ArchRule should_use_slf4j_for_logging =
      classes().that().resideInAPackage("org.gripday.authservice..")
          .should().onlyDependOnClassesThat()
          .resideOutsideOfPackages("java.util.logging..", "org.apache.commons.logging..")
          .orShould().dependOnClassesThat()
          .resideInAPackage("org.slf4j..");

  /**
   * Validates that security annotations are used appropriately. Ensures proper security implementation across the service.
   */
  @ArchTest
  static final ArchRule security_annotations_should_be_on_appropriate_classes =
      classes().that().areAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
          .should().resideInAnyPackage("..presentation.web..", "..domain.service..");

  /**
   * Ensures that transactional annotations are used appropriately. Validates proper transaction management in service layer.
   */
  @ArchTest
  static final ArchRule transactional_annotations_should_be_on_services =
      classes().that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .should().resideInAPackage("..domain.service..");

  /**
   * Validates that caching annotations are used appropriately. Ensures proper caching implementation in service layer.
   */
  @ArchTest
  static final ArchRule caching_annotations_should_be_on_services =
      classes().that().areAnnotatedWith(org.springframework.cache.annotation.Cacheable.class)
          .or().areAnnotatedWith(org.springframework.cache.annotation.CacheEvict.class)
          .or().areAnnotatedWith(org.springframework.cache.annotation.CachePut.class)
          .should().resideInAPackage("..domain.service..");

  /**
   * Ensures that validation annotations are used consistently. Validates proper input validation across DTOs and entities.
   */
  @ArchTest
  static final ArchRule validation_annotations_should_be_on_dtos_and_entities =
      classes().that().areAnnotatedWith(jakarta.validation.Valid.class)
          .or().areAnnotatedWith(jakarta.validation.constraints.NotNull.class)
          .or().areAnnotatedWith(jakarta.validation.constraints.NotBlank.class)
          .should().resideInAnyPackage("..presentation.dto..", "..infrastructure.entity..");

  /**
   * Validates that OpenAPI annotations are used consistently. Ensures proper API documentation across REST controllers.
   */
  @ArchTest
  static final ArchRule openapi_annotations_should_be_on_controllers =
      classes().that().areAnnotatedWith(io.swagger.v3.oas.annotations.Operation.class)
          .or().areAnnotatedWith(io.swagger.v3.oas.annotations.tags.Tag.class)
          .should().resideInAPackage("..presentation.web..");
}