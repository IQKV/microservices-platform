package com.iqscaffold.leadservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.iqscaffold.leadservice.config")
public class LeadServiceApplication {

  public static void main(final String[] args) {
    SpringApplication.run(LeadServiceApplication.class, args);
  }
}
