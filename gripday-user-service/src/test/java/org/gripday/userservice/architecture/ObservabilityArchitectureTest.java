package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests for observability concerns (logging, metrics, tracing).
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class ObservabilityArchitectureTest {

  @ArchTest
  static final ArchRule services_should_use_slf4j_logging =
      classes()
          .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .should().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.Logger")
          .orShould().haveSimpleNameContaining("Metrics")
          .because("Services should use SLF4J for logging");

  @ArchTest
  static final ArchRule no_system_out_or_err =
      noClasses()
          .that().resideOutsideOfPackage("..config..")
          .and().resideOutsideOfPackage("..test..")
          .should().callMethod(System.class, "out")
          .orShould().callMethod(System.class, "err")
          .because("Use proper logging instead of System.out/err");

  @ArchTest
  static final ArchRule no_printStackTrace =
      noClasses()
          .should().callMethod(Throwable.class, "printStackTrace")
          .because("Use proper logging instead of printStackTrace");

  @ArchTest
  static final ArchRule metrics_services_should_use_micrometer =
      classes()
          .that().haveSimpleNameContaining("Metrics")
          .should().dependOnClassesThat().resideInAPackage("io.micrometer..")
          .because("Metrics services should use Micrometer");

  @ArchTest
  static final ArchRule audit_services_should_log_events =
      classes()
          .that().haveSimpleNameContaining("Audit")
          .should().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.Logger")
          .because("Audit services should log events");

  @ArchTest
  static final ArchRule critical_operations_should_be_logged =
      methods()
          .that().haveNameMatching(".*(create|update|delete|authenticate|authorize).*")
          .and().areDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .and().arePublic()
          .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .orShould().haveNameMatching(".*get.*")
          .orShould().haveNameMatching(".*find.*")
          .because("Critical operations should be transactional and logged");

  @ArchTest
  static final ArchRule observability_config_should_be_in_config_package =
      classes()
          .that().haveSimpleNameContaining("Observability")
          .or().haveSimpleNameContaining("Metrics")
          .or().haveSimpleNameContaining("Tracing")
          .should().resideInAPackage("..config..")
          .because("Observability configuration should be in config package");

  @ArchTest
  static final ArchRule correlation_id_should_be_used =
      classes()
          .that().haveSimpleNameContaining("CorrelationId")
          .should().resideInAPackage("..config..")
          .orShould().beAssignableTo(jakarta.servlet.Filter.class)
          .because("Correlation ID handling should be in config or filter");

  @ArchTest
  static final ArchRule structured_logging_should_be_configured =
      classes()
          .that().haveSimpleNameContaining("StructuredLogging")
          .or().haveSimpleNameContaining("LoggingConfig")
          .should().resideInAPackage("..config..")
          .because("Structured logging configuration should be in config package");

  @ArchTest
  static final ArchRule health_checks_should_be_in_config =
      classes()
          .that().haveSimpleNameContaining("Health")
          .should().resideInAPackage("..config..")
          .orShould().beAssignableTo(org.springframework.boot.actuate.health.HealthIndicator.class)
          .because("Health check configuration should be in config package");

  @ArchTest
  static final ArchRule actuator_endpoints_should_be_secured =
      classes()
          .that().resideInAPackage("..config..")
          .and().haveSimpleNameContaining("Actuator")
          .should().dependOnClassesThat().resideInAPackage("org.springframework.security..")
          .because("Actuator endpoints should be secured");
}
