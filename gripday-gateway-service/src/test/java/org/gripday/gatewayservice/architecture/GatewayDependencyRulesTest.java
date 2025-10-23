package org.gripday.gatewayservice.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural tests for Gateway Service dependency rules and component isolation.
 * 
 * Validates:
 * - Proper reactive architecture patterns
 * - Component isolation and dependency rules
 * - Filter and service layer separation
 * - Configuration and security component organization
 * - Prevention of inappropriate dependencies
 */
@AnalyzeClasses(packages = "org.gripday.gatewayservice")
class GatewayDependencyRulesTest {

    /**
     * Ensures that filters don't directly access external services inappropriately.
     * Validates proper abstraction in gateway filter implementations.
     */
    @ArchTest
    static final ArchRule filters_should_use_proper_abstractions = 
        classes().that().resideInAPackage("..filter..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "reactor.core..",
            "org.springframework.cloud.gateway..",
            "..service..",
            "..security..",
            "io.jsonwebtoken.."
        );

    /**
     * Validates that security components are properly isolated.
     * Ensures security logic doesn't create inappropriate dependencies.
     */
    @ArchTest
    static final ArchRule security_components_should_be_isolated = 
        classes().that().resideInAPackage("..security..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "reactor.core..",
            "..security..",
            "io.jsonwebtoken.."
        );

    /**
     * Ensures that service classes follow proper dependency patterns.
     * Validates that services don't create circular dependencies.
     */
    @ArchTest
    static final ArchRule services_should_follow_dependency_patterns = 
        classes().that().resideInAPackage("..service..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "reactor.core..",
            "..service..",
            "..security..",
            "io.jsonwebtoken.."
        );

    /**
     * Validates that configuration classes don't contain business logic.
     * Ensures proper separation between configuration and business concerns.
     */
    @ArchTest
    static final ArchRule configuration_should_not_contain_business_logic = 
        classes().that().resideInAPackage("..config..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "reactor.core..",
            "..config..",
            "..filter..",
            "..security..",
            "..service..",
            "io.jsonwebtoken..",
            "org.springframework.cloud.gateway.."
        );

    /**
     * Ensures that reactive components don't use blocking operations.
     * Validates proper reactive programming patterns.
     */
    @ArchTest
    static final ArchRule reactive_components_should_not_block = 
        noClasses().that().resideInAnyPackage("..filter..", "..service..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("java.util.concurrent.CompletableFuture", "java.lang.Thread");

    /**
     * Validates that filters implement proper reactive patterns.
     * Ensures gateway filters follow Spring Cloud Gateway conventions.
     */
    @ArchTest
    static final ArchRule filters_should_be_reactive = 
        classes().that().haveNameMatching(".*Filter")
        .should().dependOnClassesThat()
        .resideInAnyPackage("reactor.core..", "org.springframework.cloud.gateway..");

    /**
     * Ensures that REST controllers (if any) follow proper naming conventions.
     * Validates consistency with platform-wide standards.
     */
    @ArchTest
    static final ArchRule rest_controllers_should_follow_conventions = 
        classes().that().areAnnotatedWith(RestController.class)
        .should().haveSimpleNameEndingWith("Resource")
        .andShould().resideInAPackage("..web..");

    /**
     * Validates that components don't create inappropriate cross-cutting dependencies.
     * Ensures clean separation of concerns.
     */
    @ArchTest
    static final ArchRule should_not_have_inappropriate_cross_dependencies = 
        noClasses().that().resideInAPackage("..filter..")
        .should().dependOnClassesThat().resideInAPackage("..config..");

    /**
     * Ensures that security filters are properly organized.
     * Validates that security-related filters follow proper patterns.
     */
    @ArchTest
    static final ArchRule security_filters_should_be_properly_organized = 
        classes().that().haveNameMatching(".*(Security|Auth|Jwt).*Filter")
        .should().resideInAnyPackage("..filter..", "..security..");

    /**
     * Validates that utility classes don't create inappropriate dependencies.
     * Ensures utility components are properly isolated.
     */
    @ArchTest
    static final ArchRule utility_classes_should_be_isolated = 
        classes().that().haveNameMatching(".*Util.*")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "org.springframework..", "org.slf4j..");

    /**
     * Ensures that exception handling is properly implemented.
     * Validates that error handling doesn't create inappropriate dependencies.
     */
    @ArchTest
    static final ArchRule exception_handling_should_be_proper = 
        classes().that().haveNameMatching(".*Exception.*")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "org.springframework..");

    /**
     * Validates that monitoring and observability components are properly isolated.
     * Ensures observability concerns don't leak into business logic.
     */
    @ArchTest
    static final ArchRule observability_should_be_isolated = 
        classes().that().haveNameMatching(".*(Metric|Monitor|Trace).*")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..", 
            "org.springframework..", 
            "org.slf4j..",
            "io.micrometer..",
            "io.opentelemetry.."
        );
}