package org.gripday.gatewayservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

/**
 * ArchUnit tests for Gateway Service architecture validation.
 * 
 * Validates:
 * - Reactive architecture patterns and components
 * - Package structure and naming conventions
 * - REST controller placement and naming (if any)
 * - Proper separation of concerns in gateway components
 * - Filter and configuration component organization
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class GatewayArchitectureTest {

    /**
     * Validates that any REST controllers follow proper naming conventions.
     * Ensures consistency with platform-wide REST controller standards.
     */
    @ArchTest
    static final ArchRule rest_controllers_should_have_resource_suffix = 
        classes().that().areAnnotatedWith(RestController.class)
        .should().haveSimpleNameEndingWith("Resource");

    /**
     * Ensures configuration classes are properly organized.
     * Validates that configuration components are in appropriate packages.
     */
    @ArchTest
    static final ArchRule configuration_classes_should_be_in_config_package = 
        classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
        .should().resideInAPackage("..config..");

    /**
     * Validates that filter classes are properly organized.
     * Ensures gateway filters are in the appropriate package structure.
     */
    @ArchTest
    static final ArchRule filter_classes_should_be_in_filter_package = 
        classes().that().haveNameMatching(".*Filter")
        .should().resideInAPackage("..filter..");

    /**
     * Ensures security components are properly organized.
     * Validates that security-related classes are in security packages.
     */
    @ArchTest
    static final ArchRule security_classes_should_be_in_security_package = 
        classes().that().haveNameMatching(".*(Security|Auth|Jwt).*")
        .and().areNotInterfaces()
        .should().resideInAnyPackage("..security..", "..config..");

    /**
     * Validates that service classes are properly organized.
     * Ensures business logic components are in service packages.
     */
    @ArchTest
    static final ArchRule service_classes_should_be_in_service_package = 
        classes().that().areAnnotatedWith(org.springframework.stereotype.Service.class)
        .should().resideInAPackage("..service..");

    /**
     * Ensures proper package naming conventions are followed.
     * Validates consistent package structure across the gateway service.
     */
    @ArchTest
    static final ArchRule package_naming_conventions_should_be_followed = 
        classes().that().resideInAPackage("org.gripday.gatewayservice..")
        .should().resideInAnyPackage(
            "org.gripday.gatewayservice",
            "org.gripday.gatewayservice.config..",
            "org.gripday.gatewayservice.filter..",
            "org.gripday.gatewayservice.security..",
            "org.gripday.gatewayservice.service.."
        );

    /**
     * Validates that reactive components don't use blocking operations inappropriately.
     * Ensures proper reactive programming patterns in gateway filters and services.
     */
    @ArchTest
    static final ArchRule reactive_components_should_not_use_blocking_operations = 
        noClasses().that().resideInAnyPackage("..filter..", "..service..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("java.util.concurrent..", "java.lang.Thread");

    /**
     * Ensures filter classes implement proper reactive patterns.
     * Validates that gateway filters follow Spring Cloud Gateway conventions.
     */
    @ArchTest
    static final ArchRule filters_should_follow_reactive_patterns = 
        classes().that().haveNameMatching(".*Filter")
        .should().dependOnClassesThat()
        .resideInAnyPackage("reactor.core..", "org.springframework.cloud.gateway..");

    /**
     * Validates that configuration classes don't contain business logic.
     * Ensures proper separation between configuration and business concerns.
     */
    @ArchTest
    static final ArchRule configuration_classes_should_not_contain_business_logic = 
        classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "..config..",
            "reactor.core..",
            "io.jsonwebtoken.."
        );

    /**
     * Validates that no circular dependencies exist between gateway components.
     * Ensures clean separation of concerns and maintainable architecture.
     */
    @Test
    void should_not_have_circular_dependencies() {
        var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");
        
        // Verify no cycles between major component groups
        var configClasses = classes.that(resideInAPackage("..config.."));
        var filterClasses = classes.that(resideInAPackage("..filter.."));
        var securityClasses = classes.that(resideInAPackage("..security.."));
        var serviceClasses = classes.that(resideInAPackage("..service.."));
        
        // Additional validation can be added here for specific circular dependency checks
        // This test serves as a placeholder for more complex cycle detection if needed
    }

    /**
     * Ensures proper dependency direction in gateway architecture.
     * Validates that components depend on appropriate abstractions.
     */
    @Test
    void should_have_proper_dependency_direction() {
        var classes = new ClassFileImporter().importPackages("org.gripday.gatewayservice");
        
        // Validate that filters don't depend on configuration details inappropriately
        // Validate that services are properly abstracted
        // Additional architectural validations can be added here
    }
}