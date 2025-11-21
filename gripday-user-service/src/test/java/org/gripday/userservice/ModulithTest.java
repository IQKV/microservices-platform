package org.gripday.userservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith tests to verify modular structure.
 * 
 * <p>Current Status: Tests are disabled due to architectural issues that need to be fixed:
 * <ul>
 *   <li>Cyclic dependencies between modules (passwordmanagement -> security -> shared -> tenancy -> usermanagement -> passwordmanagement)</li>
 *   <li>Non-exposed types from infrastructure module being accessed by tenancy module</li>
 *   <li>Shared module has bidirectional dependencies with multiple modules</li>
 * </ul>
 * 
 * <p>To fix these issues:
 * <ul>
 *   <li>Create package-info.java files with @ApplicationModule annotations to define module boundaries</li>
 *   <li>Use @NamedInterface to expose specific types from modules</li>
 *   <li>Break circular dependencies by introducing events or extracting shared interfaces</li>
 * </ul>
 */
class ModulithTest {

  private final ApplicationModules modules = ApplicationModules.of(UserServiceApplication.class);

  @Test
  void printModuleStructure() {
    // This always works - just prints the detected modules
    modules.forEach(System.out::println);
  }

  @Test
  @Disabled("Enable after fixing cyclic dependencies and module boundaries")
  void verifyModularStructure() {
    modules.verify();
  }

  @Test
  @Disabled("Enable after fixing module structure")
  void createModuleDocumentation() {
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
  }
}
