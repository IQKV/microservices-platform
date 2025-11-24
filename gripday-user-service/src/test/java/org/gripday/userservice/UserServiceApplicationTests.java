package org.gripday.userservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.gripday.userservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class UserServiceApplicationTests extends AbstractIntegrationTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    // Verify that Spring context loads successfully
    assertThat(applicationContext).isNotNull();
  }

  @Test
  void applicationContextContainsExpectedBeans() {
    // Verify main application bean is present
    assertThat(applicationContext.containsBean("userServiceApplication")).isTrue();
  }

  @Test
  void allBeansAreLoaded() {
    // Verify that beans are loaded
    var beanDefinitionNames = applicationContext.getBeanDefinitionNames();
    assertThat(beanDefinitionNames).isNotEmpty();
    assertThat(beanDefinitionNames.length).isGreaterThan(0);
  }
}
