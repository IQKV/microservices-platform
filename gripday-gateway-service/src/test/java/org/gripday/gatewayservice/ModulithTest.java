package org.gripday.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithTest {

  ApplicationModules modules = ApplicationModules.of(GatewayServiceApplication.class);

  @Test
  void createModuleDocumentation() {
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
  }
}
