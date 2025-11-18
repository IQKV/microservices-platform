package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture tests for REST API design and conventions.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class ApiArchitectureTest {

  @ArchTest
  static final ArchRule controllers_should_return_response_entity =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().arePublic()
          .and().areAnnotatedWith(GetMapping.class)
          .or().areAnnotatedWith(PostMapping.class)
          .or().areAnnotatedWith(PutMapping.class)
          .or().areAnnotatedWith(DeleteMapping.class)
          .or().areAnnotatedWith(PatchMapping.class)
          .should().haveRawReturnType(ResponseEntity.class)
          .orShould().haveRawReturnType(void.class)
          .because("Controller methods should return ResponseEntity for proper HTTP response handling");

  @ArchTest
  static final ArchRule controllers_should_have_request_mapping =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(RequestMapping.class)
          .because("REST controllers should have @RequestMapping at class level");

  @ArchTest
  static final ArchRule post_methods_should_accept_request_body =
      methods()
          .that().areAnnotatedWith(PostMapping.class)
          .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .should().haveRawParameterTypes(RequestBody.class)
          .orShould().haveNameMatching(".*logout.*")
          .orShould().haveNameMatching(".*refresh.*")
          .because("POST methods should typically accept @RequestBody");

  @ArchTest
  static final ArchRule put_methods_should_accept_request_body =
      methods()
          .that().areAnnotatedWith(PutMapping.class)
          .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .should().haveRawParameterTypes(RequestBody.class)
          .because("PUT methods should accept @RequestBody");

  @ArchTest
  static final ArchRule get_methods_should_not_accept_request_body =
      noMethods()
          .that().areAnnotatedWith(GetMapping.class)
          .should().haveRawParameterTypes(RequestBody.class)
          .because("GET methods should not accept @RequestBody");

  @ArchTest
  static final ArchRule delete_methods_should_not_accept_request_body =
      noMethods()
          .that().areAnnotatedWith(DeleteMapping.class)
          .should().haveRawParameterTypes(RequestBody.class)
          .because("DELETE methods should not accept @RequestBody");

  @ArchTest
  static final ArchRule controllers_should_validate_input =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().arePublic()
          .and().haveRawParameterTypes(RequestBody.class)
          .should().haveRawParameterTypes(jakarta.validation.Valid.class)
          .because("Controller methods should validate request body with @Valid");

  @ArchTest
  static final ArchRule api_versioning_should_be_consistent =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(RequestMapping.class)
          .because("REST controllers should use consistent API versioning");

  @ArchTest
  static final ArchRule controllers_should_not_throw_generic_exceptions =
      noMethods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .should().declareThrowableOfType(Exception.class)
          .because("Controllers should throw specific exceptions, not generic Exception");

  @ArchTest
  static final ArchRule response_dtos_should_be_immutable =
      classes()
          .that().haveSimpleNameEndingWith("Response")
          .should().beRecords()
          .orShould().haveOnlyFinalFields()
          .because("Response DTOs should be immutable");

  @ArchTest
  static final ArchRule request_dtos_should_be_immutable =
      classes()
          .that().haveSimpleNameEndingWith("Request")
          .should().beRecords()
          .orShould().haveOnlyFinalFields()
          .because("Request DTOs should be immutable");

  @ArchTest
  static final ArchRule controllers_should_use_proper_http_methods =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().arePublic()
          .should().beAnnotatedWith(GetMapping.class)
          .orShould().beAnnotatedWith(PostMapping.class)
          .orShould().beAnnotatedWith(PutMapping.class)
          .orShould().beAnnotatedWith(DeleteMapping.class)
          .orShould().beAnnotatedWith(PatchMapping.class)
          .orShould().beAnnotatedWith(RequestMapping.class)
          .because("Controller methods should use proper HTTP method annotations");

  @ArchTest
  static final ArchRule controllers_should_not_use_implementation_classes =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().arePublic()
          .should().notHaveRawReturnType(java.util.ArrayList.class)
          .andShould().notHaveRawReturnType(java.util.HashSet.class)
          .andShould().notHaveRawReturnType(java.util.HashMap.class)
          .because("Controllers should return interface types");

  @ArchTest
  static final ArchRule path_variables_should_be_validated =
      methods()
          .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
          .and().haveRawParameterTypes(PathVariable.class)
          .should().beAnnotatedWith(org.springframework.validation.annotation.Validated.class)
          .orShould().haveRawParameterTypes(jakarta.validation.constraints.NotNull.class)
          .because("Path variables should be validated");

  @ArchTest
  static final ArchRule controllers_should_handle_exceptions =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .should().beAnnotatedWith(org.springframework.web.bind.annotation.ExceptionHandler.class)
          .orShould().resideInAPackage("..presentation..")
          .because("Controllers should handle exceptions or rely on global exception handler");
}
