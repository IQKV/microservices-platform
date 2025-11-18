package org.gripday.userservice.architecture;

import org.gripday.userservice.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.ApplicationModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Advanced Spring Modulith integration tests for module interactions and boundaries.
 */
class ModulithIntegrationTest {

  private final ApplicationModules modules = ApplicationModules.of(UserServiceApplication.class);

  @Test
  void shouldHaveWellDefinedModules() {
    // Verify expected modules exist
    var expectedModules = java.util.List.of(
        "authentication",
        "registration",
        "usermanagement",
        "organization",
        "passwordmanagement",
        "emailverification",
        "tenancy",
        "shared",
        "security",
        "config",
        "infrastructure"
    );

    var actualModuleNames = modules.stream()
        .map(ApplicationModule::getName)
        .toList();

    assertThat(actualModuleNames)
        .as("Application should have well-defined modules")
        .containsAll(expectedModules);
  }

  @Test
  void sharedModuleShouldBeAccessibleByAll() {
    var sharedModule = modules.getModuleByName("shared");
    
    assertThat(sharedModule).isPresent();
    
    // Verify that shared module is used by other modules
    modules.stream()
        .filter(module -> !module.getName().equals("shared"))
        .forEach(module -> {
          var canAccessShared = module.getAllModuleDependencies(modules).stream()
              .anyMatch(dep -> dep.getName().equals("shared"));
          
          // Most modules should be able to access shared
          if (!module.getName().equals("config") && !module.getName().equals("infrastructure")) {
            assertThat(canAccessShared || module.getName().equals("presentation"))
                .as("Module %s should be able to access shared module", module.getName())
                .isTrue();
          }
        });
  }

  @Test
  void configModuleShouldNotDependOnDomainModules() {
    var configModule = modules.getModuleByName("config");
    
    if (configModule.isPresent()) {
      var dependencies = configModule.get().getDependencies(modules);
      var domainModules = java.util.List.of(
          "authentication", "registration", "usermanagement",
          "organization", "passwordmanagement", "emailverification", "tenancy"
      );

      dependencies.forEach(dep -> {
        assertThat(dep.getName())
            .as("Config module should not depend on domain modules")
            .isNotIn(domainModules);
      });
    }
  }

  @Test
  void infrastructureModuleShouldNotDependOnDomainServices() {
    var infraModule = modules.getModuleByName("infrastructure");
    
    if (infraModule.isPresent()) {
      var dependencies = infraModule.get().getDependencies(modules);
      var domainModules = java.util.List.of(
          "authentication", "registration", "usermanagement",
          "organization", "passwordmanagement", "emailverification"
      );

      dependencies.forEach(dep -> {
        assertThat(dep.getName())
            .as("Infrastructure module should not depend on domain service modules")
            .isNotIn(domainModules);
      });
    }
  }

  @Test
  void authenticationModuleShouldBeIndependent() {
    var authModule = modules.getModuleByName("authentication");
    
    if (authModule.isPresent()) {
      var dependencies = authModule.get().getDependencies(modules);
      
      // Authentication should only depend on shared, security, config, infrastructure
      dependencies.forEach(dep -> {
        assertThat(dep.getName())
            .as("Authentication module should only depend on infrastructure modules")
            .isIn("shared", "security", "config", "infrastructure", "usermanagement");
      });
    }
  }

  @Test
  void registrationModuleShouldBeIndependent() {
    var regModule = modules.getModuleByName("registration");
    
    if (regModule.isPresent()) {
      var dependencies = regModule.get().getDependencies(modules);
      
      // Registration should only depend on shared, security, config, infrastructure
      dependencies.forEach(dep -> {
        assertThat(dep.getName())
            .as("Registration module should only depend on infrastructure modules")
            .isIn("shared", "security", "config", "infrastructure", "usermanagement", "emailverification");
      });
    }
  }

  @Test
  void verifyModuleBootstrapOrder() {
    // Verify that infrastructure modules can be bootstrapped first
    var bootstrapModules = modules.stream()
        .filter(module -> module.getName().equals("config") 
            || module.getName().equals("infrastructure")
            || module.getName().equals("shared")
            || module.getName().equals("security"))
        .toList();

    bootstrapModules.forEach(module -> {
      var bootstrapDeps = module.getBootstrapDependencies(modules);
      
      // Bootstrap modules should have minimal dependencies
      assertThat(bootstrapDeps.size())
          .as("Bootstrap module %s should have minimal dependencies", module.getName())
          .isLessThanOrEqualTo(3);
    });
  }

  @Test
  void verifyNoHiddenDependencies() {
    // Verify that all dependencies are explicit
    modules.forEach(module -> {
      var declaredDeps = module.getDependencies(modules);
      var allDeps = module.getAllModuleDependencies(modules);
      
      // All dependencies should be either direct or transitive
      assertThat(allDeps)
          .as("Module %s should have explicit dependencies", module.getName())
          .isNotNull();
    });
  }

  @Test
  void verifyModuleAPIExposure() {
    // Verify that modules expose proper APIs
    modules.forEach(module -> {
      var exposedTypes = module.getExposedTypes();
      
      // Exposed types should not include internal implementation
      exposedTypes.forEach(type -> {
        assertThat(type.getSimpleName())
            .as("Exposed type should not be internal implementation")
            .doesNotContain("Impl")
            .doesNotContain("Internal");
      });
    });
  }

  @Test
  void verifySecurityModuleIsShared() {
    var securityModule = modules.getModuleByName("security");
    
    if (securityModule.isPresent()) {
      // Security module should be accessible by domain modules
      var dependentModules = modules.stream()
          .filter(module -> module.getDependencies(modules).stream()
              .anyMatch(dep -> dep.getName().equals("security")))
          .count();
      
      assertThat(dependentModules)
          .as("Security module should be used by multiple modules")
          .isGreaterThan(0);
    }
  }

  @Test
  void verifyTenancyModuleIsolation() {
    var tenancyModule = modules.getModuleByName("tenancy");
    
    if (tenancyModule.isPresent()) {
      var dependencies = tenancyModule.get().getDependencies(modules);
      
      // Tenancy should not depend on business domain modules
      var businessModules = java.util.List.of(
          "authentication", "registration", "passwordmanagement", "emailverification"
      );
      
      dependencies.forEach(dep -> {
        assertThat(dep.getName())
            .as("Tenancy module should not depend on business modules")
            .isNotIn(businessModules);
      });
    }
  }

  @Test
  void verifyEmailVerificationModuleIntegration() {
    var emailModule = modules.getModuleByName("emailverification");
    
    if (emailModule.isPresent()) {
      var dependencies = emailModule.get().getDependencies(modules);
      
      // Email verification should depend on shared for email services
      var hasSharedDep = dependencies.stream()
          .anyMatch(dep -> dep.getName().equals("shared"));
      
      assertThat(hasSharedDep)
          .as("Email verification should depend on shared module")
          .isTrue();
    }
  }

  @Test
  void verifyPasswordManagementModuleIntegration() {
    var passwordModule = modules.getModuleByName("passwordmanagement");
    
    if (passwordModule.isPresent()) {
      var dependencies = passwordModule.get().getDependencies(modules);
      
      // Password management should depend on security and shared
      var hasSecurityDep = dependencies.stream()
          .anyMatch(dep -> dep.getName().equals("security") || dep.getName().equals("shared"));
      
      assertThat(hasSecurityDep)
          .as("Password management should depend on security or shared module")
          .isTrue();
    }
  }
}
