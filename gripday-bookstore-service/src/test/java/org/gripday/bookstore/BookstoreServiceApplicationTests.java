package org.gripday.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BookstoreServiceApplicationTests {

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
    assertThat(applicationContext.containsBean("bookstoreServiceApplication")).isTrue();
  }

  @Test
  void allBeansAreLoaded() {
    // Verify that beans are loaded
    var beanDefinitionNames = applicationContext.getBeanDefinitionNames();
    assertThat(beanDefinitionNames).isNotEmpty();
    assertThat(beanDefinitionNames.length).isGreaterThan(0);
  }
}