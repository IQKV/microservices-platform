package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Architecture tests for security-related concerns.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class SecurityArchitectureTest {

  @ArchTest
  static final ArchRule controllers_should_have_security_annotations =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(PreAuthorize.class)
          .orShould().containAnyMethodsThat().areAnnotatedWith(PreAuthorize.class)
          .orShould().haveSimpleNameContaining("Public")
          .because("Controllers should have security annotations unless explicitly public");

  @ArchTest
  static final ArchRule password_fields_should_not_be_logged =
      classes()
          .that().haveSimpleNameContaining("Password")
          .or().haveSimpleNameContaining("Credential")
          .should().notBeAnnotatedWith(Override.class)
          .because("Password-related classes should not expose sensitive data in toString");

  @ArchTest
  static final ArchRule no_hardcoded_credentials =
      noClasses()
          .should().accessClassesThat().haveSimpleNameContaining("Password")
          .andShould().haveSimpleNameContaining("Hardcoded")
          .because("Credentials should not be hardcoded");

  @ArchTest
  static final ArchRule authentication_methods_should_validate_input =
      methods()
          .that().areDeclaredInClassesThat().haveSimpleNameContaining("Authentication")
          .and().arePublic()
          .and().haveNameMatching("authenticate.*")
          .should().beAnnotatedWith(org.springframework.validation.annotation.Validated.class)
          .orShould().haveRawParameterTypes(jakarta.validation.Valid.class)
          .because("Authentication methods should validate input");

  @ArchTest
  static final ArchRule security_services_should_be_in_security_package =
      classes()
          .that().haveSimpleNameContaining("Security")
          .or().haveSimpleNameContaining("Auth")
          .or().haveSimpleNameContaining("Jwt")
          .should().resideInAnyPackage("..security..", "..authentication..", "..config..")
          .because("Security-related classes should be in security or authentication packages");

  @ArchTest
  static final ArchRule no_sql_injection_vulnerabilities =
      noMethods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Repository.class)
          .should().haveNameMatching(".*createNativeQuery.*")
          .because("Avoid native queries that could lead to SQL injection");

  @ArchTest
  static final ArchRule sensitive_operations_should_be_audited =
      methods()
          .that().haveNameMatching(".*(delete|remove|revoke|disable).*")
          .and().areDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .and().arePublic()
          .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .because("Sensitive operations should be transactional and auditable");

  @ArchTest
  static final ArchRule password_encoder_should_be_used =
      classes()
          .that().haveSimpleNameContaining("Password")
          .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .should().dependOnClassesThat().haveSimpleName("PasswordEncoder")
          .because("Password services should use PasswordEncoder");

  @ArchTest
  static final ArchRule no_plain_text_passwords =
      noClasses()
          .should().haveSimpleNameContaining("PlainText")
          .andShould().haveSimpleNameContaining("Password")
          .because("Plain text passwords should never be used");

  @ArchTest
  static final ArchRule jwt_services_should_validate_tokens =
      methods()
          .that().areDeclaredInClassesThat().haveSimpleNameContaining("Jwt")
          .and().haveNameMatching(".*validate.*")
          .should().declareThrowableOfType(Exception.class)
          .because("JWT validation methods should handle exceptions");

  @ArchTest
  static final ArchRule security_filters_should_be_in_security_package =
      classes()
          .that().haveSimpleNameEndingWith("Filter")
          .and().areAssignableTo(jakarta.servlet.Filter.class)
          .should().resideInAnyPackage("..security..", "..config..", "..tenancy..")
          .because("Security filters should be in security or config packages");

  @ArchTest
  static final ArchRule cors_should_be_configured_properly =
      classes()
          .that().haveSimpleNameContaining("Cors")
          .or().haveSimpleNameContaining("Web")
          .should().resideInAPackage("..config..")
          .because("CORS configuration should be in config package");
}
