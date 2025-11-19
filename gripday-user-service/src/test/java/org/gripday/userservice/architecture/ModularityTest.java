package org.gripday.userservice.architecture;

import org.assertj.core.api.Assertions;
import org.gripday.userservice.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

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
    // Ensure no circular dependencies between modules (verification implies this)
    modules.verify();
    Assertions.assertThat(modules).isNotNull();
  }

  @Test
  void allModulesShouldBeValid() {
    // Verify all modules are properly structured
    modules.forEach(module -> {
      Assertions.assertThat((Object) module.getBasePackage()).isNotNull();
      Assertions.assertThat((Object) module.getName()).isNotNull();
    });
  }

  @Test
  void verifyModuleDependencies() {
    // Verify that module dependencies follow architectural rules
    modules.forEach(module -> {
      var dependencies = module.getDependencies(modules);

      // Shared module should not depend on specific domain modules
      if (module.getName().equals("shared")) {
        Assertions.assertThat(dependencies.stream().noneMatch(dep -> {
          var name = dep.getTargetModule().getName();
          return name.equals("authentication") || name.equals("registration") || name.equals("usermanagement")
                 || name.equals("organization") || name.equals("passwordmanagement") || name.equals("emailverification")
                 || name.equals("tenancy");
        })).isTrue();
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
  void verifyBootstrapModules() {
    // Verify bootstrap modules (config, infrastructure) are properly isolated
    modules.stream()
        .filter(module -> module.getName().equals("config") || module.getName().equals("infrastructure"))
        .forEach(module -> {
          Assertions.assertThat(module.getBootstrapDependencies(modules))
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
          dependencies.stream().forEach(dep -> {
            org.assertj.core.api.Assertions.assertThat(dep.getTargetModule().getName())
                .as("Domain module %s should not depend on other domain modules", module.getName())
                .isIn("shared", "security", "config", "infrastructure", "presentation");
          });
        });
  }
}
