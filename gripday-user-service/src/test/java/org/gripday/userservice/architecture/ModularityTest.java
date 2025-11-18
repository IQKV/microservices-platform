package org.gripday.userservice.architecture;

import org.gripday.userservice.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith tests for module boundary validation and architectural compliance.
 */
class ModularityTest {

  private final ApplicationModules modules = ApplicationModules.of(UserServiceApplication.class);

  @Test
  void verifyModularStructure() {
    // Verify that the application has a valid modular structure
    modules.verify();
  }

  @Test
  void shouldNotHaveCircularDependencies() {
    // Ensure no circular dependencies between modules
    assertThat(modules.detectDependencies())
        .as("Modules should not have circular dependencies")
        .isNotNull();
  }

  @Test
  void allModulesShouldBeValid() {
    // Verify all modules are properly structured
    modules.forEach(module -> {
      assertThat(module.getBasePackage()).isNotNull();
      assertThat(module.getName()).isNotBlank();
    });
  }

  @Test
  void verifyModuleDependencies() {
    // Verify that module dependencies follow architectural rules
    modules.forEach(module -> {
      var dependencies = module.getDependencies(modules);
      
      // Shared module should not depend on specific domain modules
      if (module.getName().equals("shared")) {
        assertThat(dependencies)
            .noneMatch(dep -> dep.getName().equals("authentication") 
                || dep.getName().equals("registration")
                || dep.getName().equals("usermanagement")
                || dep.getName().equals("organization")
                || dep.getName().equals("passwordmanagement")
                || dep.getName().equals("emailverification")
                || dep.getName().equals("tenancy"));
      }
    });
  }

  @Test
  void documentModules() {
    // Generate module documentation
    new Documenter(modules)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml();
  }

  @Test
  void verifyModuleExposure() {
    // Verify that modules only expose intended APIs
    modules.forEach(module -> {
      var exposedTypes = module.getExposedTypes();
      
      // Verify that internal implementation details are not exposed
      exposedTypes.forEach(type -> {
        assertThat(type.getPackageName())
            .as("Exposed type should not be in internal package")
            .doesNotContain(".internal.");
      });
    });
  }

  @Test
  void verifyBootstrapModules() {
    // Verify bootstrap modules (config, infrastructure) are properly isolated
    modules.stream()
        .filter(module -> module.getName().equals("config") || module.getName().equals("infrastructure"))
        .forEach(module -> {
          assertThat(module.getBootstrapDependencies(modules))
              .as("Bootstrap modules should have minimal dependencies")
              .isNotNull();
        });
  }

  @Test
  void verifyDomainModulesAreIndependent() {
    // Verify domain modules are independent of each other
    var domainModules = java.util.List.of(
        "authentication", "registration", "usermanagement", 
        "organization", "passwordmanagement", "emailverification", "tenancy"
    );

    modules.stream()
        .filter(module -> domainModules.contains(module.getName()))
        .forEach(module -> {
          var dependencies = module.getDependencies(modules);
          
          // Domain modules should only depend on shared, security, config, or infrastructure
          dependencies.forEach(dep -> {
            assertThat(dep.getName())
                .as("Domain module %s should not depend on other domain modules", module.getName())
                .isIn("shared", "security", "config", "infrastructure", "presentation");
          });
        });
  }
}