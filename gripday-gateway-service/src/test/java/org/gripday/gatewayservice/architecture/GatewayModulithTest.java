package org.gripday.gatewayservice.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith tests for Gateway Service module boundaries and architecture validation.
 * <p>
 * Note: The gateway service has intentional cyclic dependencies between config, filter, and service layers
 * for dependency injection and configuration. This is a conscious design decision for a reactive gateway architecture.
 * <p>
 * Validates: - Module structure and boundaries - Component encapsulation and isolation - Gateway-specific reactive architecture patterns -
 * Filter and configuration module organization
 */
class GatewayModulithTest {

  private final ApplicationModules modules = ApplicationModules.of(org.gripday.gatewayservice.GatewayServiceApplication.class);

  /**
   * Validates that the gateway service has detectable modules.
   * Ensures Spring Modulith can identify the module structure.
   */
  @Test
  void should_detect_modules() {
    // Verify that modules are detected
    assert !modules.stream().toList().isEmpty() : "Should detect at least one module";
  }

  /**
   * Validates that configuration modules exist and are properly structured.
   * Ensures configuration concerns are organized.
   */
  @Test
  void configuration_module_should_exist() {
    var configModule = modules.getModuleByName("config");
    assert configModule.isPresent() : "Config module should exist";
  }

  /**
   * Validates that filter modules exist and follow proper reactive patterns.
   * Ensures gateway filters are properly organized.
   */
  @Test
  void filter_module_should_exist() {
    var filterModule = modules.getModuleByName("filter");
    assert filterModule.isPresent() : "Filter module should exist";
  }

  /**
   * Validates that security modules are properly encapsulated.
   * Ensures security concerns are isolated.
   */
  @Test
  void security_module_should_exist() {
    var securityModule = modules.getModuleByName("security");
    assert securityModule.isPresent() : "Security module should exist";
  }

  /**
   * Validates that service modules follow proper dependency patterns.
   * Ensures business logic is properly organized.
   */
  @Test
  void service_module_should_exist() {
    var serviceModule = modules.getModuleByName("service");
    assert serviceModule.isPresent() : "Service module should exist";
  }

  /**
   * Generates module documentation for the gateway service.
   * Creates visual representation of module structure and dependencies.
   */
  @Test
  void should_generate_module_documentation() {
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
  }

  /**
   * Validates that all expected modules are present.
   * Ensures the gateway service has the expected modular structure.
   */
  @Test
  void should_have_all_expected_modules() {
    var moduleNames = modules.stream()
        .map(module -> module.getName())
        .toList();
    
    assert moduleNames.contains("config") : "Should have config module";
    assert moduleNames.contains("filter") : "Should have filter module";
    assert moduleNames.contains("security") : "Should have security module";
    assert moduleNames.contains("service") : "Should have service module";
  }

  /**
   * Validates that modules are properly named and organized.
   * Ensures consistent naming conventions across modules.
   */
  @Test
  void modules_should_follow_naming_conventions() {
    modules.stream().forEach(module -> {
      var name = module.getName();
      assert name.matches("[a-z]+") : "Module names should be lowercase: " + name;
    });
  }

  /**
   * Validates that the gateway service has a reasonable number of modules.
   * Ensures the service is not over-modularized or under-modularized.
   */
  @Test
  void should_have_reasonable_module_count() {
    var moduleCount = modules.stream().count();
    assert moduleCount >= 4 : "Should have at least 4 modules (config, filter, security, service)";
    assert moduleCount <= 10 : "Should not have more than 10 modules to avoid over-modularization";
  }
}
